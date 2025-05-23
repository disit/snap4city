"""SNAP4 Origin Destination Server (OD-Server).
   Copyright (C) 2022 DISIT Lab http://www.disit.org - University of Florence
   This program is free software: you can redistribute it and/or modify
   it under the terms of the GNU Affero General Public License as
   published by the Free Software Foundation, either version 3 of the
   License, or (at your option) any later version.
   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU Affero General Public License for more details.
   You should have received a copy of the GNU Affero General Public License
   along with this program.  If not, see <http://www.gnu.org/licenses/>.
"""
import psycopg2
import psycopg2.extras
import mgrs
import matplotlib.pyplot as plt
import geopy
import geopy.distance as distance
# import shapely Polygon as PolygonShapely to avoid collision with geojson's Polygon
from shapely.geometry import Polygon as PolygonShapely 
from shapely.ops import transform
from shapely import wkb
import shapely.wkt
import pyproj
from pyproj import Transformer, CRS
from geojson import Feature, Polygon, FeatureCollection
from flask import Flask, request, g, jsonify
from flask_restful import reqparse, Resource, Api
from flask_cors import CORS
from waitress import serve
import json
import pandas as pd
import numpy as np
import pyutm
import mysql.connector
import json
import yaml
import os
import math
import traceback
import sys
from db_connection import psgConnect, sqlConnect
from auth import get_token, is_valid_token
from ownership import check_ownership_by_id

script_dir = os.path.dirname(os.path.realpath(__file__))

with open(os.path.join(script_dir,'config.yaml'), 'r') as file:
    config = yaml.load(file, Loader=yaml.FullLoader)

app = Flask(__name__)
api = Api(app)

parser = reqparse.RequestParser()
parser.add_argument('longitude', type=str, required=True)
parser.add_argument('latitude', type=str, required=True)
parser.add_argument('precision', type=str, required=True)
parser.add_argument('from_date', type=str, required=True)
parser.add_argument('organization', type=str, required=True)
parser.add_argument('inflow', type=str, required=True)
parser.add_argument('od_id', type=str, required=False) # new field (2022/04/21)
parser.add_argument('perc', type=str, required=False) # new field (2022/05/13)

parser_mgrs_polygon = reqparse.RequestParser()
parser_mgrs_polygon.add_argument('longitude', type=str, required=True)
parser_mgrs_polygon.add_argument('latitude', type=str, required=True)
parser_mgrs_polygon.add_argument('precision', type=str, required=True)

parser_polygon = reqparse.RequestParser()
parser_polygon.add_argument('longitude', type=str, required=True)
parser_polygon.add_argument('latitude', type=str, required=True)
parser_polygon.add_argument('type', type=str, required=True) # new field (2022/04/21)
parser_polygon.add_argument('organization', type=str, required=True) # new field (2022/04/21)
parser_polygon.add_argument('od_id', type=str, required=False) # new field (2025/04/14)

parser_color = reqparse.RequestParser()
parser_color.add_argument('metric_name', type=str, required=True)

# new parser to get statistics for a given poligon ID stored in a given OD matrix 
parser_get_stats = reqparse.RequestParser()
parser_get_stats.add_argument('od_id', type=str, required=True)
parser_get_stats.add_argument('poly_id', type=str, required=True)
parser_get_stats.add_argument('from_date', type=str, required=True)
parser_get_stats.add_argument('invalid_id', type=str, required=True)
parser_get_stats.add_argument('invalid_label', type=str, required=True)
parser_get_stats.add_argument('organization', type=str, required=True) # new field (2025/04/11)

# new parser to retrieve all the polygon shapes included into the visible map
parser_get_all_polygons = reqparse.RequestParser()
parser_get_all_polygons.add_argument('latitude_ne', type=str, required=True)
parser_get_all_polygons.add_argument('longitude_ne', type=str, required=True)
parser_get_all_polygons.add_argument('latitude_sw', type=str, required=True)
parser_get_all_polygons.add_argument('longitude_sw', type=str, required=True)
parser_get_all_polygons.add_argument('type', type=str, required=True)
parser_get_all_polygons.add_argument('organization', type=str, required=True)
parser_get_all_polygons.add_argument('od_id', type=str, required=False) # new field (2025/04/14)

#before request parser
before_parser = reqparse.RequestParser()
before_parser.add_argument('contextbroker', type=str, required=True)




# get MGRS precision from precision in meters
def getMGRSprecision(precision):
    if precision == 1:
        return 5
    elif precision == 10:
        return 4
    elif precision == 100:
        return 3
    elif precision == 1000:
        return 2
    elif precision == 10000:
        return 1
    elif precision == 100000:
        return 0
    else:
        return -1

# get MGRS corners coordinates from the lower left corner's coordinates (southwest) and reference
'''
https://stackoverflow.com/questions/26187164/getting-the-coordinates-of-the-corners-of-cells-in-the-mgrs-coordinate-system
https://dida.do/blog/understanding-mgrs-coordinates

mgrs will convert to latitude and longitude. 
The grid reference is the southwest corner. 
To get the other three just add one to the easting and/or northing

Example:

southwest = 18SUH 67890 43210 <- output of m.toMGRS()
northwest = 18SUH 67890 43211
southeast = 18SUH 67891 43210
northeast = 18SUH 67891 43211
'''

def getMGRScorners(lon, lat, precision):
    m = mgrs.MGRS()
    
    # get MGRS precision from precision in meters
    mgrs_precision = getMGRSprecision(float(precision))
    
    # get MGRS grid reference
    reference = m.toMGRS(float(lat), float(lon), MGRSPrecision=mgrs_precision)
    
    # get substring cut point
    s = int(len(reference[5:])/2)
    
    # get the grid references (format numbers with s digits and trailing zeros)
    southwest = [reference[:5], reference[5:5+s], reference[-s:]]
    #northwest = [reference[:5], reference[5:5+s], str(int(reference[-s:])+1)]
    #southeast = [reference[:5], str(int(reference[5:5+s])+1), reference[-s:]]
    #northeast = [reference[:5], str(int(reference[5:5+s])+1), str(int(reference[-s:])+1)]
    northwest = [reference[:5], reference[5:5+s], ("{:0" + str(s) + "d}").format(int(reference[-s:])+1)]
    southeast = [reference[:5], ("{:0" + str(s) + "d}").format(int(reference[5:5+s])+1), reference[-s:]]
    northeast = [reference[:5], ("{:0" + str(s) + "d}").format(int(reference[5:5+s])+1), ("{:0" + str(s) + "d}").format(int(reference[-s:])+1)]

    # flatten the grid reference arrays
    southwest = ''.join(southwest)
    northwest = ''.join(northwest)
    southeast = ''.join(southeast)
    northeast = ''.join(northeast)

    # convert the grid references to coordinates
    southwest = m.toLatLon(southwest)
    northwest = m.toLatLon(northwest)
    southeast = m.toLatLon(southeast)
    northeast = m.toLatLon(northeast)
    
    return southwest, northwest, southeast, northeast, reference

# get the MGRS shapely polygon built and reference from the bottom left coordinate clockwise from precision in meters
# https://stackoverflow.com/questions/66921642/create-a-square-polygon-from-lower-left-corner-coordinates
# https://gis.stackexchange.com/questions/354273/flipping-coordinates-with-shapely
def getMGRSpolygonShapelyOld(lon, lat, precision):
    m = mgrs.MGRS()
    
    # get MGRS precision from precision in meters
    mgrs_precision = getMGRSprecision(float(precision))
    
    # get MGRS grid reference
    reference = m.toMGRS(float(lat), float(lon), MGRSPrecision=mgrs_precision)
    
    # assume each site is 100 m
    d = distance.distance(kilometers=float(precision)/1000)

    # going clockwise, from lower-left to upper-left, upper-right...
    p1 = geopy.Point((float(lat), float(lon)))
    p2 = d.destination(point=p1, bearing=0)
    p3 = d.destination(point=p2, bearing=90)
    p4 = d.destination(point=p3, bearing=180)

    points = [(p.latitude, p.longitude) for p in [p1,p2,p3,p4]]
    polygon = PolygonShapely(points)
    #plt.plot(*polygon.exterior.xy)
    #print(polygon.wkt)
    # return polygon with flipped coordinates (lon, lat)
    return transform(lambda x, y: (y, x), polygon), reference

# get the MGRS shapely polygon built and reference from the bottom left coordinate clockwise from precision in meters
# https://stackoverflow.com/questions/66921642/create-a-square-polygon-from-lower-left-corner-coordinates
# https://gis.stackexchange.com/questions/354273/flipping-coordinates-with-shapely
def getMGRSpolygonShapely(lon, lat, precision):
    m = mgrs.MGRS()
    
    # get MGRS precision from precision in meters
    mgrs_precision = getMGRSprecision(float(precision))
    
    # get MGRS grid reference
    reference = m.toMGRS(float(lat), float(lon), MGRSPrecision=mgrs_precision)
    
    lat_min, lon_min = m.toLatLon(reference)

    # transform EPSG:4326 coordinates in EPGS:3857
    transformer = Transformer.from_crs("epsg:4326", "epsg:3857", always_xy=True)
    x_min, y_min = transformer.transform(lon_min, lat_min)
    
    y_max = y_min + float(precision)
    x_max = x_min + float(precision)
    
    # transform EPSG:3857 coordinates in EPSG:4326
    transformer = Transformer.from_crs("epsg:3857", "epsg:4326", always_xy=True)
    lat_min, lon_min = transformer.transform(x_min, y_min)
    lat_max, lon_max = transformer.transform(x_max, y_max)
    
    southwest = (lat_min, lon_min)
    northwest = (lat_max, lon_min)
    southeast = (lat_min, lon_max)
    northeast = (lat_max, lon_max)
    
    #return lat_min, lat_max, lon_min, lon_max
    #return southwest, northwest, southeast, northeast, reference
    polygon = PolygonShapely([southwest,
         southeast,
         northeast,
         northwest
        ])
    #plt.plot(*polygon.exterior.xy)
    #return polygon.wkt
    # return polygon's wkt with flipped coordinates (lon, lat)
    return transform(lambda x, y: (y, x), polygon), reference

# get the MGRS shapely polygon built and reference from the bottom left coordinate clockwise from precision in meters
# https://stackoverflow.com/questions/66921642/create-a-square-polygon-from-lower-left-corner-coordinates
# https://gis.stackexchange.com/questions/354273/flipping-coordinates-with-shapely
# USE THIS
def get_MGRS_Polygon_Shapely(lon, lat, precision):
    m = mgrs.MGRS()
    
    # get MGRS precision from precision in meters
    mgrs_precision = getMGRSprecision(float(precision))
    
    # get MGRS grid reference
    reference = m.toMGRS(float(lat), float(lon), MGRSPrecision=mgrs_precision)

    # convert MGRS reference to UTM
    # https://www.stellman-greene.com/mgrs_to_utm (alternative C implementation)
    z, n, easting, northing = m.MGRSToUTM(reference)

    eastingMin = easting
    eastingMax = easting + float(precision)
    northingMin = northing
    northingMax = northing + float(precision)

    # get CRS
    # https://gis.stackexchange.com/questions/365584/convert-utm-zone-into-epsg-code
    # use PROJ string, assuming a default WGS84
    #crs = CRS.from_string('+proj=utm +zone=' + str(z) + (' +south' if n == 'S' else '+north'))
    # or dictionary
    crs = CRS.from_dict({'proj': 'utm', 'zone': z, 'south': True if n == 'S' else False})
    epsg, epsg_code = crs.to_authority()
    # transform EPSG:x coordinates in EPSG:4326
    transformer = Transformer.from_crs('epsg:' + epsg_code, 'epsg:4326', always_xy=True) 

    southwest = transformer.transform(eastingMin, northingMin)
    northwest = transformer.transform(eastingMin, northingMax)
    southeast = transformer.transform(eastingMax, northingMin)
    northeast = transformer.transform(eastingMax, northingMax)
    
    #return lat_min, lat_max, lon_min, lon_max
    #return southwest, northwest, southeast, northeast, reference
    polygon = PolygonShapely([southwest,
                              southeast,
                              northeast,
                              northwest
                             ])
    #plt.plot(*polygon.exterior.xy)
    #return polygon.wkt
    # return polygon's wkt with flipped coordinates (lon, lat)
    #return transform(lambda x, y: (y, x), polygon), reference
    return polygon, reference

# get the MGRS shapely polygon built and reference from the bottom left coordinate clockwise from precision in meters
# https://stackoverflow.com/questions/66921642/create-a-square-polygon-from-lower-left-corner-coordinates
# https://gis.stackexchange.com/questions/354273/flipping-coordinates-with-shapely
def getMGRSpolygonShapely1(lon, lat, precision):
    southwest, northwest, southeast, northeast, reference = getMGRScorners(lon, lat, precision)
    
    #return lat_min, lat_max, lon_min, lon_max
    #return southwest, northwest, southeast, northeast, reference
    polygon = PolygonShapely([southwest,
         southeast,
         northeast,
         northwest
        ])
    #plt.plot(*polygon.exterior.xy)
    #return polygon.wkt
    # return polygon's wkt with flipped coordinates (lon, lat)
    #return transform(lambda x, y: (y, x), polygon), reference
    return polygon, reference

# get MGRS corners coordinates and reference from the lower left corner's coordinates (southwest)
# precision = mgrs_tile_edge_size
# https://dida.do/blog/understanding-mgrs-coordinates
def getMGRSpolygonShapely_alternative(lon, lat, precision):
    m = mgrs.MGRS()
    
    # get MGRS precision from precision in meters
    mgrs_precision = getMGRSprecision(float(precision))
    
    # get MGRS grid reference
    reference = m.toMGRS(float(lat), float(lon), MGRSPrecision=mgrs_precision)
    
    geod = pyproj.Geod(ellps='WGS84')
    lat_min, lon_min = m.toLatLon(reference)
    x_var = geod.line_length([lon_min, lon_min], [lat_min, lat_min + 1])
    y_var = geod.line_length([lon_min, lon_min + 1], [lat_min, lat_min])
    lat_max = lat_min + float(precision) / x_var
    lon_max = lon_min + float(precision) / y_var
    
    southwest = (lat_min, lon_min)
    northwest = (lat_max, lon_min)
    southeast = (lat_min, lon_max)
    northeast = (lat_max, lon_max)
    
    #return lat_min, lat_max, lon_min, lon_max
    #return southwest, northwest, southeast, northeast, reference
    polygon = PolygonShapely([southwest,
         southeast,
         northeast,
         northwest
        ])
    #plt.plot(*polygon.exterior.xy)
    #return polygon.wkt
    # return polygon's wkt with flipped coordinates (lon, lat)
    return transform(lambda x, y: (y, x), polygon), reference

def getMGRSpolygonShapelyCenter(lon, lat, precision):
    m = mgrs.MGRS()
    
    # get MGRS precision from precision in meters
    mgrs_precision = getMGRSprecision(float(precision))
    
    # get MGRS grid reference
    reference = m.toMGRS(float(lat), float(lon), MGRSPrecision=mgrs_precision)
    
    lat_min, lon_min = m.toLatLon(reference)

    # transform EPSG:4326 coordinates in EPGS:3857
    transformer = Transformer.from_crs("epsg:4326", "epsg:3857", always_xy=True)
    x_min, y_min = transformer.transform(lon_min, lat_min)
    
    y_c = y_min + float(precision) / 2
    x_c = x_min + float(precision) / 2
    
    # transform EPSG:3857 coordinates in EPSG:4326
    transformer = Transformer.from_crs("epsg:3857", "epsg:4326", always_xy=True)
    lat_c, lon_c = transformer.transform(x_c, y_c)
    
    return str(lat_c) + ', ' + str(lon_c)

# NOT USED
def getOD(precision, from_date, organization):
    feature_collection = []
    try:
        connection = psgConnect(config)
        cursor = connection.cursor(cursor_factory=psycopg2.extras.DictCursor)
    
        # get the geometry data with SRID 3003
        #query = '''
        #SELECT DISTINCT(a.idstazione), a.value, ST_AsText(ST_Transform(c.geom, 3003)) AS geom FROM 
        #(SELECT idstazione, SUM(value) AS value FROM public.pluviometers_data WHERE date(date) = '%s' 
        #GROUP BY idstazione) a
        #JOIN public.stations c
        #ON a.idstazione = c.idstazione
        #WHERE c.strumento = 'pluviometro'
        #'''
        #cursor.execute(query, date)
        
        query = '''
        SELECT * FROM public.od_data a 
        LEFT JOIN public.od_metadata c
        ON a.od_id = c.od_id 
        WHERE precision = %s
        AND from_date = %s
        AND organization = %s 
        '''
        # use a comma after date to make it a tuple if it contains a single element (precision,)
        cursor.execute(query, (precision, from_date, organization))
        
        results = cursor.fetchall()

        print('[getOD] query: ', query)
        print('[getOD] query result: ', results)
        
        features = []

        for row in results:
            # get shapely geometry from WKT
            #geom = wkt.loads(row['geom'])
            
            # get MGRS corners (lat-lon tuples array) of origin and destination
            #orig_sw, orig_nw, orig_se, orig_ne, reference = getMGRScorners(row['x_orig'], 
            #                                                               row['y_orig'], 
            #                                                               precision)
            dest_sw, dest_nw, dest_se, dest_ne, reference = getMGRScorners(row['x_dest'], 
                                                                           row['y_dest'], 
                                                                           precision)
            
            # get polygon (lon-lat tuples' array)
            #polygon = [(orig_sw[1], orig_sw[0]), 
            #           (orig_nw[1], orig_nw[0]), 
            #           (orig_ne[1], orig_ne[0]), 
            #           (orig_se[1], orig_se[0]),
            #           (orig_sw[1], orig_sw[0])]
            
            # get polygon (lon-lat tuples' array)
            polygon = [[(dest_sw[1], dest_sw[0]), 
                       (dest_nw[1], dest_nw[0]), 
                       (dest_ne[1], dest_ne[0]), 
                       (dest_se[1], dest_se[0]),
                       (dest_sw[1], dest_sw[0])]]
                        
            feature = Feature(geometry=Polygon(polygon),
                              id = reference,
                              properties={'name': reference, 'density': int(row['value'])})

            # append feature to features' array
            features.append(feature)
            
        # create features' collection from features array
        feature_collection = FeatureCollection(features)

    except (Exception, psycopg2.Error) as error:
        print("Error while fetching data from PostgreSQL", error)

    finally:
        # closing database connection
        if(connection):
            cursor.close()
            connection.close()
            #return feature_collection
            #return json.loads(str(feature_collection))
            return 'var = statesData = ' + str(feature_collection)

# check if a string is numeric
# https://stackoverflow.com/questions/354038/how-do-i-check-if-a-string-is-a-number-float
def is_number(s):
    try:
        n = float(s)
        # check if float is finite (not nan and not inf)
        # since 'nan' and 'inf' are recognized as valid numbers
        #if not math.isnan(n) and not math.isinf(n):
        if math.isfinite(n):
            return True
        else:
            return False
    except ValueError:
        return False
    
# get MGRS inflows/outflows
def getMGRSflows(lon, lat, precision, from_date, organization, inFlow):
    feature_collection = []
    result = None
    try:
        connection = psgConnect(config)
        cursor = connection.cursor(cursor_factory=psycopg2.extras.DictCursor)
        query = ''
        df = None
        
        # Matteo 31/03/2025 -> correction in query line 492 ('orig_geom' if inFlow != 'True' else 'dest_geom') => ('orig_geom' if inFlow == 'True' else 'dest_geom')
        query = '''
        SELECT a.od_id, a.from_date, c.organization, a.precision, a.value, 
        ''' + ('x_orig AS lon, y_orig AS lat' if inFlow == 'True' else 'x_dest AS lon, y_dest AS lat') + '''
        FROM public.od_data_mgrs a
        LEFT JOIN public.od_metadata c
        ON a.od_id = c.od_id
        WHERE ST_CONTAINS(''' + ('orig_geom' if inFlow == 'True' else 'dest_geom') + ''',  
        ST_GEOMFROMEWKT('SRID=4326;POINT(''' + lon + ' ' + lat + ''')'))
        AND from_date = %(from_date)s
        AND organization = %(organization)s
        AND "precision" = %(precision)s
        AND ST_AsText(dest_geom) <> ST_AsText(orig_geom)
        '''
        
        #print(query)
        #print([from_date, organization, precision])

        # fetch results as dataframe
        df = pd.read_sql_query(query, connection, 
                               params={'from_date': from_date, 
                                       'organization': organization,
                                       'precision': precision})
        
            
        # get dictionary of flows grouped by od_id
        #od_id_flows = df.groupby(['od_id'])['value'].agg('sum').to_dict()
        
        # get total sum of values
        total = df['value'].sum()
        
        # remove duplicates rows by od_id, keeping the last
        #df = df.drop_duplicates(subset='od_id', keep='last')
        
        features = []

        for index, row in df.iterrows():
            # get MGRS corners (lat-lon tuples array) of origin and destination
            #sw, nw, se, ne, reference = getMGRScorners(row['lon'], 
            #                                           row['lat'], 
            #                                           precision)
            
            # get polygon (lon-lat tuples' array)
            #polygon = [[(sw[1], sw[0]), 
            #           (nw[1], nw[0]), 
            #           (ne[1], ne[0]), 
            #           (se[1], se[0]),
            #           (sw[1], sw[0])]]
            
            # get flow (%)
            #perc = od_id_flows[row['od_id']] / total
            perc = int(row['value']) / total
            
            # build geojson feature
            # Matteo 03/04/2025 -> in feature return even od_id and organization to check the ownership
            polygon, reference = get_MGRS_Polygon_Shapely(float(row['lon']), float(row['lat']), float(precision))
            feature = Feature(geometry=shapely.wkt.loads(polygon.wkt),
                              id = reference,
                              properties={'name': reference, 'density': perc, organization:row['organization']},
                              od_id=row['od_id']) 
            
            # append feature to features' array
            features.append(feature)
            
        # create features' collection from features array
        feature_collection = FeatureCollection(features)

        print('[getMGRSflows] query: ', query)
        print('[getMGRSflows] query result: ', feature_collection)

    except (Exception, psycopg2.Error) as error:
        print("[getMGRSflows] Error while fetching data from PostgreSQL", error)

    finally:
        # closing database connection
        if(connection):
            cursor.close()
            connection.close()
        return feature_collection


def is_json(myjson):
    if(myjson[0] in {'{', '['} and myjson[-1] in {'}', ']'} ):
        try:
            json.loads(myjson)
        except ValueError as e:
            return False
        return True
    else:
        return False

# get inflows/outflows
# >>>>>>>>>> modified function to handle the new source parameted in od_metadata table,
#            used to select the table to be used to retrieve the geometry data
def getFlows(lon, lat, precision, from_date, organization, inFlow, od_id, get_perc):

    feature_collection = []
    result = None
    try:
        connection = psgConnect(config)
        cursor = connection.cursor(cursor_factory=psycopg2.extras.DictCursor)
        query = ''
        df = None

        if(od_id != ''):
            query = '''
                    SELECT od_id, source
                    FROM public.od_metadata
                    WHERE od_id = %(od_id)s  
                '''
            df = pd.read_sql_query(query, connection, params={'od_id': od_id})

            if(len(df['source']) != 1):
                raise Exception("Unable to retrieve source table for od_id: %s" % od_id)
            source = df['source'][0]
        else:
            source = ''


        # initial query to assess the geom table to use. If 'od_id' is empty,
        # then the defauls 'gadm36' table is selected
        if(not(source) or source == ''): 
            source = 'gadm36'
            # get query
            query = '''
            SELECT a.od_id, value, c.geom AS orig_commune, c.uid AS orig_id, d.geom AS dest_commune, d.uid AS dest_id, from_date, to_date
            FROM public.od_data a
            LEFT JOIN
            public.''' + source + ''' c
            ON a.orig_commune = c.uid
            LEFT JOIN
            public.''' + source + ''' d
            ON a.dest_commune = d.uid
            LEFT JOIN public.od_metadata e
            ON a.od_id = e.od_id
            WHERE ST_CONTAINS(''' + ('c.geom' if inFlow == 'True' else 'd.geom') + ''', 
            ST_GEOMFROMEWKT('SRID=4326;POINT(''' + lon + ' ' + lat + ''')'))
            AND from_date = %(from_date)s
            AND organization = %(organization)s
            AND ST_AsText(d.geom) <> ST_AsText(c.geom)
            '''
            
            # print(query)

            # fetch results as dataframe
            df = pd.read_sql_query(query, connection, 
                                params={'from_date': from_date, 
                                        'organization': organization})
                
            # get dictionary of flows grouped by od_id
            #od_id_flows = df.groupby(['od_id'])['value'].agg('sum').to_dict()
            
            # get total sum of values
            # total = df['value'].sum()

            # Matteo 31/03/2025 -> ensures that the query result is not empty
            if not df.empty:
                if(is_json(df.at[0,'value'])):
                    data0 = json.loads(df.at[0,'value'])
                    keys = list(data0.keys())
                    values = np.zeros((df.shape[0], len(keys)), dtype=float)                
                    for index, row in df.iterrows():
                        data = json.loads(row['value'])
                        for k in range(len(keys)):
                            # print(data[keys[k]])
                            values[index,k] = data[keys[k]]
                    # print(values)
                    total = np.sum(values, axis=0).tolist()
                    # print(total)
                    # print(keys)
                else:
                    df['value'] = df['value'].astype(float)
                    total = df['value'].sum()

                # remove duplicates rows by od_id, keeping the last
                #df = df.drop_duplicates(subset='od_id', keep='last')

                # inspected polygon
                # name0 = df.at[0,'dest_name'] if inFlow == 'True' else df.at[0,'orig_name']
                # geom0 = df.at[0,'dest_commune'] if inFlow == 'True' else df.at[0,'orig_commune']
            
            features = []

            for index, row in df.iterrows():
                if(isinstance(total, list)):
                    data = json.loads(row['value'])
                    perc = {}
                    for t in range(len(total)):
                        if(total[t] <= 1): # i valori salvati sono già percentuali. nulla da fare
                            perc_ = float(data[keys[t]])
                        else: # i valori salvati sono assoluti
                            if(get_perc == 'True'): # posso calcolare le percentuali, se get_perc = True
                                #print('is true')
                                perc_ = float(data[keys[t]])/ total[t]
                            else: # o mandare i valori assoluti
                                #print('is false')
                                perc_ = float(data[keys[t]])
                        perc[keys[t]] = perc_
                    # print(perc)
                    json_object = json.dumps(perc, indent = 4) 
                    # print(json_object)
                else:
                    if(total <= 1):
                        perc = float(row['value'])
                    else: 
                        if(get_perc == 'True'):
                            perc = int(row['value']) / total
                        else:
                            perc = int(row['value']) 
                # build geojson feature
                # Matteo 03/04/2025 -> in feature return even od_id and organization to check the ownership
                polygon = wkb.loads(row['dest_commune'], hex=True) if inFlow != 'True' else wkb.loads(row['orig_commune'], hex=True)
                feature = Feature(geometry=shapely.wkt.loads(polygon.wkt),
                                id = row['dest_id'] if inFlow != 'True' else row['orig_id'],
                                properties={
                                    'name': row['dest_id'] if inFlow != 'True' else row['orig_id'], 
                                    'txt_name': '', 
                                    'density': perc,
                                    'od_id':row['od_id'],
                                    'organization':organization,
                                })
                # print(feature)
                
                # append feature to features' array
                features.append(feature)

            # create features' collection from features array
            feature_collection = FeatureCollection(features)

        else:  # new code using the source parameter
            
            query = '''
            SELECT 
                a.od_id, value, 
                c.geom AS orig_commune, c.uid AS orig_id, c.name AS orig_name,
                d.geom AS dest_commune, d.uid AS dest_id, d.name AS dest_name,
                from_date, to_date
            FROM public.od_data a
            LEFT JOIN
            public.''' + source + ''' c
            ON a.orig_commune = c.uid
            LEFT JOIN
            public.''' + source + ''' d
            ON a.dest_commune = d.uid
            LEFT JOIN public.od_metadata e
            ON a.od_id = e.od_id
            WHERE ST_CONTAINS(''' + ('c.geom' if inFlow == 'True' else 'd.geom') + ''', 
            ST_GEOMFROMEWKT('SRID=4326;POINT(''' + lon + ' ' + lat + ''')'))
            AND from_date = %(from_date)s
            AND organization = %(organization)s
            AND a.od_id = %(od_id)s
            '''
            # AND ST_AsText(d.geom) <> ST_AsText(c.geom)
            # '''

            #print(query)
            # print(from_date)
            # print(organization)
            # print(od_id)
            
            # fetch results as dataframe
            df = pd.read_sql_query(query, connection, 
                                params={'from_date': from_date, 
                                        'organization': organization,
                                        'od_id': od_id})

            # print("query DONE!")

            # Matteo 03/04/2025 -> ensures that the query result is not empty
            if not df.empty:
                # get total sum of values            
                if(is_json(df.at[0,'value'])):
                    data0 = json.loads(df.at[0,'value'])
                    keys = list(data0.keys())
                    values = np.zeros((df.shape[0], len(keys)), dtype=float)                
                    for index, row in df.iterrows():
                        data = json.loads(row['value'])
                        for k in range(len(keys)):
                            # print(data[keys[k]])
                            values[index,k] = data[keys[k]]
                    # print(values)
                    total = np.sum(values, axis=0).tolist()
                    # print(total)
                    # print(keys)
                else:
                    df['value'] = df['value'].astype(float)
                    total = df['value'].sum()

                # print("SUM DONE")
            
            # remove duplicates rows by od_id, keeping the last
            #df = df.drop_duplicates(subset='od_id', keep='last')

            # inspected polygon
            # name0 = df.at[0,'dest_name'] if inFlow == 'True' else df.at[0,'orig_name']
            # geom0 = df.at[0,'dest_commune'] if inFlow == 'True' else df.at[0,'orig_commune']
            
            features = []

            for index, row in df.iterrows():
                # get MGRS corners (lat-lon tuples array) of origin and destination
                #sw, nw, se, ne, reference = getMGRScorners(row['lon'], 
                #                                           row['lat'], 
                #                                           precision)
                
                # get polygon (lon-lat tuples' array)
                #polygon = [[(sw[1], sw[0]), 
                #           (nw[1], nw[0]), 
                #           (ne[1], ne[0]), 
                #           (se[1], se[0]),
                #           (sw[1], sw[0])]]
                
                # get flow (%)
                #perc = od_id_flows[row['od_id']] / total

                if(isinstance(total, list)):
                    data = json.loads(row['value'])
                    perc = {}
                    for t in range(len(total)):
                        if(total[t] <= 1): # i valori salvati sono già percentuali. nulla da fare
                            perc_ = float(data[keys[t]])
                        else: # i valori salvati sono assoluti
                            if(get_perc == 'True'): # posso calcolare le percentuali, se get_perc = True
                                # print('is true')
                                perc_ = float(data[keys[t]])/ total[t]
                            else: # o mandare i valori assoluti
                                # print('is false')
                                perc_ = float(data[keys[t]])
                        perc[keys[t]] = perc_
                    # print(perc)
                    json_object = json.dumps(perc, indent = 4) 
                    # print(json_object)
                else:
                    if(total <= 1):
                        perc = float(row['value'])
                    else: 
                        if(get_perc == 'True'):
                            perc = int(row['value']) / total
                        else:
                            perc = int(row['value']) 
                # build geojson feature
                
                # print([name0, row['dest_name'], row['orig_name']])


                polygon = wkb.loads(row['dest_commune'], hex=True) if inFlow != 'True' else wkb.loads(row['orig_commune'], hex=True)
                feature = Feature(geometry=shapely.wkt.loads(polygon.wkt),
                                id = row['dest_id'] if inFlow != 'True' else row['orig_id'],
                                properties={
                                    'name': row['dest_id'] if inFlow != 'True' else row['orig_id'], 
                                    'txt_name': row['dest_name'] if inFlow != 'True' else row['orig_name'], 
                                    'density': perc,
                                    'od_id':row['od_id'],
                                    'organization':organization,
                                })
                # print(feature)
                
                # append feature to features' array
                features.append(feature)
                
            # create features' collection from features array
            feature_collection = FeatureCollection(features)

        print('[getFlows] query: ', query)
        print('[getFlows] query result: ', feature_collection)

    except (Exception, psycopg2.Error) as error:
        print("[GET FLOW] Error while fetching data from PostgreSQL", error)

    finally:
        # closing database connection
        if(connection):
            cursor.close()
            connection.close()
        return feature_collection
    
# get MGRS polygon array of coordinates [[lat1, lon1], [lat2, lon2], [lat3, lon3]..., [latn, lonn]]
def getMGRSPolygon(lon, lat, precision):
    # get left bottom corner's coordinates of MGRS zone containing (latitude, longitude)
    #m = mgrs.MGRS()
        
    # get MGRS precision from precision in meters
    #mgrs_precision = getMGRSprecision(float(precision))
        
    # get MGRS grid reference
    #reference = m.toMGRS(float(lat), float(lon), MGRSPrecision=mgrs_precision)
    #latitude, longitude = m.toLatLon(reference)
        
    # get MGRS polygon
    #polygon, reference = getMGRSpolygonShapely(lon, lat, precision)
    #if precision == '100000':
    #    polygon, reference = getMGRSpolygonShapely(lon, lat, precision)
    #else:
    #    polygon, reference = getMGRSpolygonShapely1(lon, lat, precision)
    polygon, reference = get_MGRS_Polygon_Shapely(lon, lat, precision)
        
    # flip polygon coordinates, since they are already flipped by getMGRSpolygonShapely
    # this will return coordinates in (lat, lon) order
    polygon = transform(lambda x, y: (y, x), polygon).wkt
    #polygon = polygon.wkt
        
    # build polygon array
    pol = []
    for i in polygon.split('((')[1].split('))')[0].split(','):
        lat_lon = i.strip().split(' ')
        pol.append([float(lat_lon[0]), float(lat_lon[1])])
        
    return pol

# get polygon array of coordinates [[lat1, lon1], [lat2, lon2], [lat3, lon3]..., [latn, lonn]]
# 2022/04/21 modified function that use the new parameter 'type': if empty the legacy 
#            behavior is used, otherwise 'type' is used to select the type of area 
#            where seach for the point(lat, lon). The 'type' can be {'region', 
#            'province', 'municipalty', 'ace', 'section', 'poi'} and work only for 
#            query in the new 'italy_epgs4326' table
def getPolygon(lon, lat, type, organization, od_id):
    pol = []
    try:
        connection = psgConnect(config)
        cursor = connection.cursor(cursor_factory=psycopg2.extras.DictCursor)
        
        if(type==''):
            query = '''
            SELECT uid, geom FROM public.gadm36
            WHERE ST_Intersects(geom, ST_GeomFromText('POINT(%s %s)', 4326))
            ''' % (lon, lat)
        else:
            # Matteo 11/04/2025 -> change query to filter by od_id
            query = '''
            SELECT g.*, d.od_id
            FROM public.od_data d
            JOIN public.italy_epgs4326 g
            ON g.uid = d.orig_commune or g.uid = d.dest_commune
            WHERE ST_Intersects(geom, ST_GeomFromText('POINT(%s %s)', 4326)) AND
            ''' % (lon, lat)
            if type == 'region':
                query = query + 'cod_reg<>\'NULL\' AND ' + \
                                'cod_prov=\'NULL\' AND ' + \
                                'cod_com=\'NULL\'  AND ' + \
                                'cod_ace=\'NULL\'  AND ' + \
                                'cod_sez=\'NULL\'  AND ' + \
                                'poi_id=\'NULL\''
            elif type == 'province':
                query = query + 'cod_reg<>\'NULL\' AND ' + \
                                'cod_prov<>\'NULL\' AND ' + \
                                'cod_com=\'NULL\'  AND ' + \
                                'cod_ace=\'NULL\'  AND ' + \
                                'cod_sez=\'NULL\'  AND ' + \
                                'poi_id=\'NULL\'' 
            elif type == 'municipality':
                query = query + 'cod_reg<>\'NULL\' AND ' + \
                                'cod_prov<>\'NULL\' AND ' + \
                                'cod_com<>\'NULL\'  AND ' + \
                                'cod_ace=\'NULL\'  AND ' + \
                                'cod_sez=\'NULL\'  AND ' + \
                                'poi_id=\'NULL\''   
            elif type == 'ace':
                query = query + 'cod_reg<>\'NULL\' AND ' + \
                                'cod_prov<>\'NULL\' AND ' + \
                                'cod_com<>\'NULL\'  AND ' + \
                                'cod_ace<>\'NULL\'  AND ' + \
                                'cod_sez=\'NULL\'  AND ' + \
                                'poi_id=\'NULL\''  
            elif type == 'section':
                query = query + 'cod_reg<>\'NULL\' AND ' + \
                                'cod_prov<>\'NULL\' AND ' + \
                                'cod_com<>\'NULL\'  AND ' + \
                                'cod_ace<>\'NULL\'  AND ' + \
                                'cod_sez<>\'NULL\'  AND ' + \
                                'poi_id=\'NULL\''  
            elif type == 'poi':   
                query = query + '(poi_id LIKE \'%' + organization + '%\' or poi_id <> \'NULL\')'
                if od_id is not None:
                    query = query + ' AND d.od_id = \'' + od_id +'\''

        # print(query)
        # fetch results
        cursor.execute(query)
        results = cursor.fetchall()
        
        features = []
        for row in results:
            # MODIFIED 2022-05-05: get same behavior of get flow
            #     to support multi-polygons
            try:
                txt_name = row['name']
            except:
                txt_name = ''
            polygon = wkb.loads(row['geom'], hex=True)
            feature = Feature(geometry=shapely.wkt.loads(polygon.wkt),
                            id = row['uid'],
                            properties={
                                'name': row['uid'], 
                                'txt_name': txt_name,
                                'custom': row['poi_id'] != 'NULL',
                                'od_id': row['od_id'] if 'od_id' in row else None,
                                'organization':organization,
                            })
            features.append(feature)
        pol = FeatureCollection(features)
            # ORIGINAL CODE (2022-05-05)
            # polygon = wkb.loads(row['geom'], hex=True)
            # # flip polygon coordinates
            # # this will return coordinates in (lat, lon) order
            # polygon = transform(lambda x, y: (y, x), polygon).wkt
        
            # # build polygon array 
            # for i in polygon.split('(((')[1].split('))')[0].split(','):
            #     lat_lon = i.strip().split(' ')
            #     pol.append([float(lat_lon[0]), float(lat_lon[1])])
        print('[getPolygon] query: ', query)
        print('[getPolygon] query result: ', pol)
    except (Exception, psycopg2.Error) as error:
        print("[GET POLYGON] Error while fetching data from PostgreSQL", error)

    finally:
        # closing database connection
        if(connection):
            cursor.close()
            connection.close()
        return pol
    
# get polygon array of coordinates [[lat1, lon1], [lat2, lon2], [lat3, lon3]..., [latn, lonn]]
def getPolygonShapely(lon, lat):
    polygon = None
    try:
        connection = psgConnect(config)
        cursor = connection.cursor(cursor_factory=psycopg2.extras.DictCursor)
        
        query = '''
        SELECT geom FROM public.gadm36
        WHERE ST_Intersects(geom, ST_GeomFromText('POINT(%s %s)', 4326))
        ''' % (lon, lat)
        
        # fetch results
        cursor.execute(query)
        results = cursor.fetchall()
        
        for row in results:
            polygon = wkb.loads(row['geom'], hex=True)
            # flip polygon coordinates
            # this will return coordinates in (lat, lon) order
            polygon = transform(lambda x, y: (y, x), polygon)
        print('[getPolygonShapely] query: ', query)
        print('[getPolygonShapely] query result: ', results)
    except (Exception, psycopg2.Error) as error:
        print("Error while fetching data from PostgreSQL", error)

    finally:
        # closing database connection
        if(connection):
            cursor.close()
            connection.close()
        return polygon
    
# convert rgb color tuple to hex color string
def rgb2hex(rgb):
    return '#%02x%02x%02x' % rgb

# convert rgb color string to hex color string
def convertColor(rgb):
    t = json.loads(rgb)
    t = tuple(t)
    return rgb2hex(t)
    
# get color map as array
def getColorMap(metric_name):
    out = None
    try:
        # setup MySQL connection
        cnx = sqlConnect(config)
        # get the total number of rows
        df = pd.read_sql('SELECT * FROM colors WHERE metric_name = %s', 
                        params=(metric_name,), 
                        con=cnx)
        
        # close MySQL connection
        cnx.close()

        #Matteo 03/04/2025 -> if df is empty return empty list

        if df.empty:
            return [], 200
        
        # create hex color column
        df['hex'] = df.apply(lambda x: convertColor(x['rgb']), axis=1)
        
        # replace nans with empty strings
        df = df.replace(np.nan, '', regex=True)
        
        out = df[['min', 'max', 'hex']].to_numpy().tolist()
        status = 200
    except Exception as e:
        exc_type, exc_obj, exc_tb = sys.exc_info()
        fname = os.path.split(exc_tb.tb_frame.f_code.co_filename)[1]
        print(exc_type, fname, exc_tb.tb_lineno)
        out = 'Error: ' + str(exc_type) + ' ' + str(fname) + ' ' + str(exc_tb.tb_lineno) + '\n' + traceback.format_exc()
        status = 500

    return out, status

# New function to get statistics stored in a OD matrix (i.e., od_id) for a given polygon (i.e., dest_id)
# - invalid_id is used to identify dest_id not previsously defined (e.g., -9999)
# - invalid_label is used to fill the txt name filed of the response for invalid_id found
# NOTE: at this moment, it only works if italy_epgs4326 table is used as source for the OD storing the statistics!!!
# Matteo 03/04/2025 -> add organization parameter
def getPolygonStatistics(od_id, organization, dest_id, from_date, invalid_id, invalid_label):
    feature_collection = []
    result = None
    try:
        connection = psgConnect(config)
        cursor = connection.cursor(cursor_factory=psycopg2.extras.DictCursor)
        query = ''
        df = None
        
        query = '''
            SELECT od_id, source
            FROM public.od_metadata
            WHERE od_id = %(od_id)s  
        '''
        df = pd.read_sql_query(query, connection, params={'od_id': od_id})
        if(len(df['source']) != 1):
            raise Exception("Unable to retrieve source table for od_id: %s" % od_id)
        source = df['source'][0]

        # get query
        query = '''
        SELECT 
            od_id, orig_commune, dest_commune, value, from_date, to_date
        FROM public.od_data
        WHERE dest_commune = \'''' + str(dest_id) + '''\'
        AND from_date = \'''' + from_date + '''\'
        AND od_id = \'''' + od_id + '''\''''

        # print(query)
        
        # fetch results as dataframe
        df = pd.read_sql_query(query, connection)

        # print(invalid_id)
        valid_orig_id = []
        invalid_orig_id = []
        valid_orig_id_values = []
        invalid_orig_id_values = []
        valid_orig_id_date = []
        invalid_orig_id_date = []
        
        # Matteo 03/04/2025 -> if df is empty return empty feature collection
        if not df.empty:
            for index, row in df.iterrows():
                if str(row['orig_commune']) == str(invalid_id):
                    invalid_orig_id.append(row['orig_commune'])
                    invalid_orig_id_values.append(row['value'])
                    invalid_orig_id_date.append(str(row['from_date']))
                else:
                    valid_orig_id.append(row['orig_commune'])
                    valid_orig_id_values.append(row['value'])
                    valid_orig_id_date.append(str(row['from_date']))

            # print(valid_orig_id)
            # print(invalid_orig_id)

            # print(valid_orig_id_date)
            # print(invalid_orig_id_date)

            query2= '''
                SELECT ids.orig_id, s.uid, s.name, s.cod_reg, s.cod_prov, s.cod_com, s.cod_ace, s.cod_sez, s.poi_id 
                FROM (VALUES '''

            for i, id in enumerate(valid_orig_id):
                query2 = query2 + '(' + str(i) + ',' + str(id) + '),'

            query2 = query2[:-1] + ''') 
                AS ids(rid, orig_id) 
                LEFT JOIN public.''' + source + ''' AS s 
                ON ids.orig_id=s.uid 
                ORDER BY ids.rid;'''

            # print(query2)

            df = pd.read_sql_query(query2, connection)

            # print(df)
                
        features = []

        for index, row in df.iterrows():            
            feature = Feature(
                            id = row['orig_id'],
                            properties={
                                'id': row['orig_id'], 
                                'orig': row['uid'], 
                                'txt_name': row['name'], 
                                'date': valid_orig_id_date[index],
                                'density': valid_orig_id_values[index],
                                'cod_reg': row['cod_reg'],
                                'cod_prov': row['cod_prov'],
                                'cod_com': row['cod_com'],
                                'cod_ace': row['cod_ace'],
                                'cod_sez': row['cod_sez'],
                                'poi_id': row['poi_id'],
                                'od_id': od_id,
                                'organization': organization,
                            })
            features.append(feature)

        for index, val in enumerate(invalid_orig_id_values):
            
            feature = Feature(
                            id = int(invalid_id),
                            properties={
                                'id': int(invalid_id), 
                                'orig': int(invalid_id), 
                                'txt_name': invalid_label,
                                'date': invalid_orig_id_date[index], 
                                'density': val,
                                'cod_reg': '',
                                'cod_prov': '',
                                'cod_com': '',
                                'cod_ace': '',
                                'cod_sez': '',
                                'poi_id': ''
                            })
            features.append(feature)
            
        # print(features)
        # create features' collection from features array
        feature_collection = FeatureCollection(features)
        # print(feature_collection)
        print('[getPolygonStatistics] query: ', query)
        print('[getPolygonStatistics] query result: ', feature_collection)
    except (Exception, psycopg2.Error) as error:
        print("[GET POLYGON STATS] Error while fetching data from PostgreSQL", error)

    finally:
        # closing database connection
        if(connection):
            cursor.close()
            connection.close()
        return feature_collection

# New function to retrieve all polygon shapes included in the map bounding box (< latitude_ne, longitude_ne, latitude_sw, longitude_sw >)
# NOTE: at this moment it only works for polygon shapes stored into the italy_epgs4326!!!
def getAllPoly(latitude_ne, longitude_ne, latitude_sw, longitude_sw, type, od_id, organization):

    feature_collection = []
    result = None
    try:
        connection = psgConnect(config)
        cursor = connection.cursor(cursor_factory=psycopg2.extras.DictCursor)
        query = ''
        df = None

        if(type == 'poi' or type == 'ace' or type == 'municipality' or type == 'province' or type == 'region'):
            source = 'italy_epgs4326'
        elif(type == 'communes'):
            source = 'gadm36' # TODO: CONTROLLA BENE!!!!
        else:
            source = 'mgrs' # TODO: CONTROLLA BENE !!!!!

        #   p0 ------ p1
        #    |         |
        #   p3 ------ p2
        x1 = longitude_ne
        y1 = latitude_ne
        x3 = longitude_sw
        y3 = latitude_sw
        x0 = x3
        y0 = y1
        x2 = x1
        y2 = y3
        aoi = "" + \
            "ST_GeomFromEWKT(" + \
            "'SRID=4326;POLYGON((" + \
            str(x0) + " " + str(y0) + "," + \
            str(x1) + " " + str(y1) + "," + \
            str(x2) + " " + str(y2) + "," + \
            str(x3) + " " + str(y3) + "," + \
            str(x0) + " " + str(y0) + "))'" + \
            ")"

        # get query
        if(source == 'italy_epgs4326'):
            query = '''
            SELECT 
                g.uid, g.text_uid, g.name, g.poi_id, g.geom, d.od_id
            FROM public.od_data d
            JOIN public.''' + source + ''' g
            ON g.uid = d.orig_commune or g.uid = d.dest_commune
            WHERE 
                ST_Intersects(geom, ''' + aoi + ''') AND
            '''
            if type == 'region':
                query = query + 'cod_reg<>\'NULL\' AND ' + \
                                'cod_prov=\'NULL\' AND ' + \
                                'cod_com=\'NULL\'  AND ' + \
                                'cod_ace=\'NULL\'  AND ' + \
                                'cod_sez=\'NULL\'  AND ' + \
                                'poi_id=\'NULL\''
            elif type == 'province':
                query = query + 'cod_reg<>\'NULL\' AND ' + \
                                'cod_prov<>\'NULL\' AND ' + \
                                'cod_com=\'NULL\'  AND ' + \
                                'cod_ace=\'NULL\'  AND ' + \
                                'cod_sez=\'NULL\'  AND ' + \
                                'poi_id=\'NULL\'' 
            elif type == 'municipality':
                query = query + 'cod_reg<>\'NULL\' AND ' + \
                                'cod_prov<>\'NULL\' AND ' + \
                                'cod_com<>\'NULL\'  AND ' + \
                                'cod_ace=\'NULL\'  AND ' + \
                                'cod_sez=\'NULL\'  AND ' + \
                                'poi_id=\'NULL\''   
            elif type == 'ace':
                query = query + 'cod_reg<>\'NULL\' AND ' + \
                                'cod_prov<>\'NULL\' AND ' + \
                                'cod_com<>\'NULL\'  AND ' + \
                                'cod_ace<>\'NULL\'  AND ' + \
                                'cod_sez=\'NULL\'  AND ' + \
                                'poi_id=\'NULL\''  
            elif type == 'section':
                query = query + 'cod_reg<>\'NULL\' AND ' + \
                                'cod_prov<>\'NULL\' AND ' + \
                                'cod_com<>\'NULL\'  AND ' + \
                                'cod_ace<>\'NULL\'  AND ' + \
                                'cod_sez<>\'NULL\'  AND ' + \
                                'poi_id=\'NULL\''  
            elif type == 'poi':  
                query = query + '(poi_id LIKE \'%' + organization + '%\' or poi_id <> \'NULL\')'
                if od_id is not None:
                    query = query + ' AND d.od_id = \'' + od_id +'\''
            
            # fetch results as dataframe
            df = pd.read_sql_query(query, connection)
            # print(df)            
                    
            features = []

            for index, row in df.iterrows():            
                polygon = wkb.loads(row['geom'], hex=True)
                feature = Feature(geometry=shapely.wkt.loads(polygon.wkt),
                                id = row['uid'],
                                properties={
                                    'name': row['uid'], 
                                    'txt_name': row['name'],
                                    'custom': row['poi_id'] != 'NULL',
                                    'od_id': row['od_id'] if 'od_id' in row else None,
                                    'organization':organization,
                                })
                features.append(feature)
    
            # print(features)
            # create features' collection from features array
            feature_collection = FeatureCollection(features)
            # print(feature_collection)

        elif (source == 'gadm36'):
            query = '''
            SELECT 
                uid, name_3, geom
            FROM public.''' + source + '''
            WHERE 
                ST_Intersects(geom, ''' + aoi + ''')
            '''
            # print(query)

            # fetch results as dataframe
            df = pd.read_sql_query(query, connection)
            # print(df)            
                    
            features = []

            for index, row in df.iterrows():            
                polygon = wkb.loads(row['geom'], hex=True)
                feature = Feature(geometry=shapely.wkt.loads(polygon.wkt),
                                id = row['uid'],
                                properties={
                                    'name': row['uid'], 
                                    'txt_name': row['name_3']
                                })
                features.append(feature)
    
            # print(features)
            # create features' collection from features array
            feature_collection = FeatureCollection(features)

        elif (source == 'mgrs'):
            print('TODO!!!') # TODO cases for gadm and msgr !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
        print('[getAllPoly] query: ', query)
        print('[getAllPoly] query result: ', feature_collection)
    except (Exception, psycopg2.Error) as error:
        print("[GET ALL POLYGONS] Error while fetching data from PostgreSQL", error)

    finally:
        # closing database connection
        if(connection):
            cursor.close()
            connection.close()
        return feature_collection




########################################################################################################################################################

# these endopints requires only the auth control, not the owneship
EXCLUDED_ENDPOINTS = ['getmgrspolygon', 'getmgrspolygoncenter', 'getpolygon', 'color', 'getallpolygons']
# these endpoints requires a late ownership control
POLYGON_ENDPOINTS = ['getpolygon', 'getallpolygons']
    

# check authentication/ownership (when possible) before each request
@app.before_request
def before_request():
    #auth
    token, message, status = get_token(request)
    if token is not None:
        message, status = is_valid_token(config, token)
        if status == None or status != 200:
            resp = jsonify({'message':message, 'status':status})
            resp.status_code = status
            return resp
    
    if request.endpoint not in EXCLUDED_ENDPOINTS or request.endpoint in POLYGON_ENDPOINTS:
        before_args = before_parser.parse_args()
        g.contextbroker = before_args['contextbroker']
        g.token = token
    
    if request.endpoint not in EXCLUDED_ENDPOINTS: 
        # check ownership of the resource
        args = parser.parse_args() if request.endpoint != 'getpolystats' else parser_get_stats.parse_args()
        organization = args['organization']
        od_id = None
        if(args['od_id']):
            od_id = args['od_id']

        if od_id != None: # if the request has the od_id parameter => query by serviceUri
            message, status = check_ownership_by_id(config, token, od_id, organization, before_args['contextbroker'])
            if status == None or status != 200:
                resp = jsonify({'message':message, 'status':status})
                resp.status_code = status
                return resp

def create_feature_collection(response, token, contextbroker):
    # parse response
    owned_features = []
    checked_od_id = []
    passed_od_id = []
    json_response = response.get_json()
    features = json_response['features']
    if len(features) > 0:
        for feature in features:
            od_id = feature['properties']['od_id']
            organization = feature['properties']['organization']
            # check ownership by serviceUri
            if od_id not in checked_od_id:
                checked_od_id.append(od_id)
                message, status = check_ownership_by_id(config, token, od_id, organization, contextbroker)
                if status == 200 and len(message['Service']['features'])>0:
                    passed_od_id.append(od_id)
                    owned_features.append(feature)
            else:
                if od_id in passed_od_id:
                    owned_features.append(feature)
    checked_od_id.clear()
    passed_od_id.clear()
    feature_collection = FeatureCollection(owned_features)
    feature_collection_json = json.dumps(feature_collection)
    response.set_data(feature_collection_json)
    response.content_type = "application/json"
    return response

# check ownership when od_id is not provided from the beginning
@app.after_request
def after_request(response):

    if response.status_code != 200:
        return response
    
    if request.endpoint not in EXCLUDED_ENDPOINTS or request.endpoint in POLYGON_ENDPOINTS:
        token = getattr(g, "token", None)
        contextbroker = getattr(g, "contextbroker", None)
        if contextbroker is None:
            resp = jsonify({'message':'contextbroker is None', 'status':500})
            resp.status_code = 500
            return resp
        response = create_feature_collection(response, token, contextbroker)

    #clear g
    g.token = None
    g.contextbroker = None

    return response


class ODMGRS(Resource):
    def get(self):

        print('[ODMGRS]')
        #print(self)

        #print(parser.parse_args())
        # parse arguments
        args = parser.parse_args()

        #print(args)
        
        longitude = args['longitude']
        latitude = args['latitude']
        precision = args['precision']
        from_date = args['from_date']
        organization = args['organization']
        inFlow = args['inflow']
        
        #print('read')
        
        return getMGRSflows(longitude, latitude, precision, from_date, organization, inFlow)

# >>>>>>>>>> modified to handle the new optional parameter 'od_id'. If missing it is 
#            set to an empty string and the legacy functionality is kept.
#            Similar behaviour is imlemented for the additional new parameter 'perc'
class OD(Resource):
    def get(self):
        # parse arguments
        args = parser.parse_args()
        #print(args)
        
        longitude = args['longitude']
        latitude = args['latitude']
        precision = args['precision']
        from_date = args['from_date']
        organization = args['organization']
        inFlow = args['inflow']
        if(args['od_id']):
            od_id = args['od_id']
        else:
            od_id = ''
        if(args['perc']):
            perc = args['perc']
        else:
            perc = 'True'
        
        return getFlows(longitude, latitude, precision, from_date, organization, inFlow, od_id, perc)
    
class GetMGRSPolygon(Resource):
    def get(self):
        # parse arguments
        args = parser_mgrs_polygon.parse_args()
        
        longitude = args['longitude']
        latitude = args['latitude']
        precision = args['precision']
        
        return getMGRSPolygon(longitude, latitude, precision)

class GetMGRSPolygonCenter(Resource):
    def get(self):
        # parse arguments
        args = parser_mgrs_polygon.parse_args()
        
        longitude = args['longitude']
        latitude = args['latitude']
        precision = args['precision']
        
        return getMGRSpolygonShapelyCenter(longitude, latitude, precision)

# 2022/04/21 modified to handle the new optional parameter 'type'. If missing it is 
#            set to an empty string and the legacy functionality is kept
class GetPolygon(Resource):
    def get(self):
        # parse arguments
        args = parser_polygon.parse_args()
        
        longitude = args['longitude']
        latitude = args['latitude']
        type = args['type']
        organization = args['organization']
        od_id = args['od_id']
        
        return getPolygon(longitude, latitude, type, organization, od_id)
    
class Color(Resource):
    def get(self):
        # parse arguments
        args = parser_color.parse_args()
        metric_name = args['metric_name']
        
        return getColorMap(metric_name)

class GetPolyStats(Resource):
    def get(self):
        # parse arguments
        args = parser_get_stats.parse_args()
        od_id = args['od_id']
        organization = args['organization']
        dest_id = args['poly_id']
        from_date = args['from_date']
        invalid_id = args['invalid_id']
        invalid_label = args['invalid_label']       
        return getPolygonStatistics(od_id, organization, dest_id, from_date, invalid_id, invalid_label)

class GetAllPolygons(Resource):
    def get(self):
        args = parser_get_all_polygons.parse_args()
        latitude_ne = args['latitude_ne']
        longitude_ne = args['longitude_ne']
        latitude_sw = args['latitude_sw']
        longitude_sw = args['longitude_sw']
        type = args['type']
        od_id = args['od_id']
        organization = args['organization']
        return getAllPoly(latitude_ne, longitude_ne, latitude_sw, longitude_sw, type, od_id, organization)

        
api.add_resource(OD, '/get')
api.add_resource(ODMGRS, '/get_mgrs')
api.add_resource(GetMGRSPolygon, '/mgrs_polygon')
api.add_resource(GetMGRSPolygonCenter, '/mgrs_polygon_center')
api.add_resource(GetPolygon, '/polygon')
api.add_resource(Color, '/color')
api.add_resource(GetPolyStats, '/get_stats')
api.add_resource(GetAllPolygons, '/get_all_polygons')

# enable CORS
CORS(app, resources={r'/*': {'origins': '*'}})

if __name__ == '__main__':
    #app.run(debug=True)
    #app.run(host="0.0.0.0", port=3200, debug=True)
    '''
    avoid 'WARNING: This is a development server. Do not use it in a production deployment.'
    when running Flask, use waitress instead to serve
    https://stackoverflow.com/questions/51025893/flask-at-first-run-do-not-use-the-development-server-in-a-production-environmen
    '''
    print('OD-API_GET :: Listening on port 3200')
    serve(app, host='0.0.0.0', port=3200)
    
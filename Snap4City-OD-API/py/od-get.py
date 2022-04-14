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
from flask import Flask, request
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

with open(r'config.yaml') as file:
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

parser_mgrs_polygon = reqparse.RequestParser()
parser_mgrs_polygon.add_argument('longitude', type=str, required=True)
parser_mgrs_polygon.add_argument('latitude', type=str, required=True)
parser_mgrs_polygon.add_argument('precision', type=str, required=True)

parser_polygon = reqparse.RequestParser()
parser_polygon.add_argument('longitude', type=str, required=True)
parser_polygon.add_argument('latitude', type=str, required=True)

parser_color = reqparse.RequestParser()
parser_color.add_argument('metric_name', type=str, required=True)

def psgConnect(conf):
    conn = psycopg2.connect(user=conf['user_psg'],
                            password=conf['password_psg'],
                            host=conf['host_psg'],
                            port=conf['port_psg'],
                            database=conf['database_psg'])
    return conn

def sqlConnect(conf):
    conn = mysql.connector.connect(user=conf['user_sql'],
                                   password=conf['password_sql'],
                                   host=conf['host_sql'],
                                   database=conf['database_sql'])
    return conn

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
        
        query = '''
        SELECT a.od_id, a.from_date, c.organization, a.precision, a.value, 
        ''' + ('x_orig AS lon, y_orig AS lat' if inFlow == 'True' else 'x_dest AS lon, y_dest AS lat') + '''
        FROM public.od_data_mgrs a
        LEFT JOIN public.od_metadata c
        ON a.od_id = c.od_id
        WHERE ST_CONTAINS(''' + ('orig_geom' if inFlow != 'True' else 'dest_geom') + ''', 
        ST_GEOMFROMEWKT('SRID=4326;POINT(''' + lon + ' ' + lat + ''')'))
        AND from_date = %(from_date)s
        AND organization = %(organization)s
        AND "precision" = %(precision)s
        AND ST_AsText(dest_geom) <> ST_AsText(orig_geom)
        '''
        
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
            polygon, reference = get_MGRS_Polygon_Shapely(float(row['lon']), float(row['lat']), float(precision))
            feature = Feature(geometry=shapely.wkt.loads(polygon.wkt),
                              id = reference,
                              properties={'name': reference, 'density': perc})
            
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
        return feature_collection

# get inflows/outflows
def getFlows(lon, lat, precision, from_date, organization, inFlow):
    feature_collection = []
    result = None
    try:
        connection = psgConnect(config)
        cursor = connection.cursor(cursor_factory=psycopg2.extras.DictCursor)
        query = ''
        df = None
        
        query = '''
        SELECT a.od_id, value, c.geom AS orig_commune, c.uid AS orig_id, d.geom AS dest_commune, d.uid AS dest_id, from_date, to_date
        FROM public.od_data a
        LEFT JOIN
        public.gadm36 c
        ON a.orig_commune = c.uid
        LEFT JOIN
        public.gadm36 d
        ON a.dest_commune = d.uid
        LEFT JOIN public.od_metadata e
        ON a.od_id = e.od_id
        WHERE ST_CONTAINS(''' + ('c.geom' if inFlow != 'True' else 'd.geom') + ''', 
        ST_GEOMFROMEWKT('SRID=4326;POINT(''' + lon + ' ' + lat + ''')'))
        AND from_date = %(from_date)s
        AND organization = %(organization)s
        AND ST_AsText(d.geom) <> ST_AsText(c.geom)
        '''
        
        # fetch results as dataframe
        df = pd.read_sql_query(query, connection, 
                               params={'from_date': from_date, 
                                       'organization': organization})
            
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
            polygon = wkb.loads(row['dest_commune'], hex=True) if inFlow != 'True' else wkb.loads(row['orig_commune'], hex=True)
            feature = Feature(geometry=shapely.wkt.loads(polygon.wkt),
                              id = row['dest_id'] if inFlow != 'True' else row['orig_id'],
                              properties={'name': row['dest_id'] if inFlow != 'True' else row['orig_id'], 'density': perc})
            
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
def getPolygon(lon, lat):
    pol = []
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
            polygon = transform(lambda x, y: (y, x), polygon).wkt
        
            # build polygon array 
            for i in polygon.split('(((')[1].split('))')[0].split(','):
                lat_lon = i.strip().split(' ')
                pol.append([float(lat_lon[0]), float(lat_lon[1])])

    except (Exception, psycopg2.Error) as error:
        print("Error while fetching data from PostgreSQL", error)

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
    # setup MySQL connection
    cnx = sqlConnect(config)
    # get the total number of rows
    df = pd.read_sql('SELECT * FROM colors WHERE metric_name = %s', 
                     params=(metric_name,), 
                     con=cnx)
    
    # close MySQL connection
    cnx.close()
    
    # create hex color column
    df['hex'] = df.apply(lambda x: convertColor(x['rgb']), axis=1)
    
    # replace nans with empty strings
    df = df.replace(np.nan, '', regex=True)
    
    return df[['min', 'max', 'hex']].to_numpy().tolist()

class ODMGRS(Resource):
    def get(self):
        # parse arguments
        args = parser.parse_args()
        
        longitude = args['longitude']
        latitude = args['latitude']
        precision = args['precision']
        from_date = args['from_date']
        organization = args['organization']
        inFlow = args['inflow']
        
        return getMGRSflows(longitude, latitude, precision, from_date, organization, inFlow)
    
class OD(Resource):
    def get(self):
        # parse arguments
        args = parser.parse_args()
        
        longitude = args['longitude']
        latitude = args['latitude']
        precision = args['precision']
        from_date = args['from_date']
        organization = args['organization']
        inFlow = args['inflow']
        
        return getFlows(longitude, latitude, precision, from_date, organization, inFlow)
    
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
    
class GetPolygon(Resource):
    def get(self):
        # parse arguments
        args = parser_polygon.parse_args()
        
        longitude = args['longitude']
        latitude = args['latitude']
        
        return getPolygon(longitude, latitude)
    
class Color(Resource):
    def get(self):
        # parse arguments
        args = parser_color.parse_args()
        metric_name = args['metric_name']
        
        return getColorMap(metric_name)
        
api.add_resource(OD, '/get')
api.add_resource(ODMGRS, '/get_mgrs')
api.add_resource(GetMGRSPolygon, '/mgrs_polygon')
api.add_resource(GetMGRSPolygonCenter, '/mgrs_polygon_center')
api.add_resource(GetPolygon, '/polygon')
api.add_resource(Color, '/color')

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
    serve(app, host='0.0.0.0', port=3200)

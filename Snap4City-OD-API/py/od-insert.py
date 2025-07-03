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

# https://stackoverflow.com/questions/60079115/create-origin-destination-matrix-from-a-data-frame-in-python
# https://legallandconverter.com/p50.html
# https://github.com/hobu/mgrs
# https://github.com/FREAC/pyutm

'''
PostgreSQL tables

public.od_data

CREATE TABLE public.od_data (
    od_id varchar NULL,
    x_orig numeric NULL,
    y_orig numeric NULL,
    x_dest numeric NULL,
    y_dest numeric NULL,
    "precision" int8 NULL,
    value numeric NULL,                         # MODIFIED TO text TO ACCEPT JSON DATA
    orig_geom geometry(POLYGON, 4326) NULL,
    dest_geom geometry(POLYGON, 4326) NULL,
    from_date timestamp(0) NULL,
    to_date timestamp(0) NULL
);
CREATE INDEX dest_geom ON public.od_data USING gist (dest_geom);
CREATE INDEX orig_geom ON public.od_data USING gist (orig_geom);

public.od_metadata

CREATE TABLE public.od_metadata (
    od_id varchar NULL,
    value_type varchar NULL,
    value_unit varchar NULL,
    description varchar NULL,
    organization varchar NULL,
    kind varchar NULL,
    "mode" varchar NULL,
    transport varchar NULL,
    purpose varchar NULL,
    source text NULL                            # ADDED NEW FILED TO SPECIFY SOURCE GEOM TABLE
);
CREATE UNIQUE INDEX od_metadata_od_id_idx ON public.od_metadata USING btree (od_id);

'''

'''
example usage:

POST (json in the body)
insert job with bounding box
http://hostname:3100/insert

curl http://hostname:3100/insert -H 'Content-Type:application/json' 
-d '{"x_orig": [1,2,3], 
"y_orig": [2,3,4], 
"x_dest": [3,4,5], 
"y_dest": [4,5,6], 
"from_date": "2021-04-01 00:00:00",
"to_date": "2021-04-01 23:59:59", 
"precision": 100, 
"values": [8,9,10], 
"value_type": "test", 
"value_unit": "test", 
"uid": "test", 
"description": "test", 
"organization": "test", 
"kind": "test", 
"mode": "test", 
"transport": "test", 
"purpose": "test"}' -X POST -v
'''

# print to console
# import sys
# print('text', file=sys.stderr)

# https://stackoverflow.com/questions/38080310/how-to-add-custom-http-response-header-in-flask-restful <- custom response header

# http://spyne.io <- flask soap

from flask import Flask, request, g, make_response, jsonify
from flask_restful import reqparse, Resource, Api
from flask_cors import CORS
from waitress import serve
import json
import pandas as pd
import numpy as np
import pyutm
import mgrs
import hashlib
import mgrs
import pyproj
from pyproj import Transformer, CRS
import geopy
import geopy.distance as distance
# import shapely Polygon as PolygonShapely to avoid collision with geojson's Polygon
from shapely.geometry import Polygon as PolygonShapely, shape
from shapely.ops import transform
import psycopg2
import psycopg2.extras
import yaml
import os
from shapely.wkt import loads
from shapely.ops import unary_union
import geojson
from auth import basic_auth
from device import create_device, insert_data
from ownership import check_ownership_by_id
from db_connection import psgConnect

script_dir = os.path.dirname(os.path.realpath(__file__))

with open(os.path.join(script_dir,'config.yaml'), 'r') as file:
    config = yaml.load(file, Loader=yaml.FullLoader)

app = Flask(__name__)
api = Api(app)

# snap4city device parser
parser = reqparse.RequestParser()
parser.add_argument('model', type=str, required=True)
parser.add_argument('type', type=str, required=True)
parser.add_argument('contextbroker', type=str, required=True)
parser.add_argument('producer', type=str, required=False)
parser.add_argument('subnature', type=str, required=True)
parser.add_argument('organization', type=str, required=True)

#parser = reqparse.RequestParser()
#parser.add_argument('data', type=str, required=True)
'''
parser.add_argument('x_orig', type=str, required=True)
parser.add_argument('y_orig', type=str, required=True)
parser.add_argument('x_dest', type=str, required=True)
parser.add_argument('y_dest', type=str, required=True)
parser.add_argument('from_date', type=str, required=True)
parser.add_argument('to_date', type=str, required=True)
parser.add_argument('precision', type=str, required=True)
parser.add_argument('values', type=str, required=True)
parser.add_argument('value_type', type=str, required=True)
parser.add_argument('value_unit',  type=str, required=True)
parser.add_argument('description', type=str, required=True)
parser.add_argument('organization', type=str, required=True)
parser.add_argument('kind', type=str, required=True)
parser.add_argument('mode', type=str, required=True)
parser.add_argument('transport', type=str, required=True)
parser.add_argument('purpose', type=str, required=True)
'''

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

# get MGRS corners coordinates and reference from the lower left corner's coordinates (southwest)
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

# DON'T USE THIS, WRONG ORDER OF POLYGON COORDINATES
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

# get MGRS corners coordinates and reference from the lower left corner's coordinates (southwest)
# precision = mgrs_tile_edge_size
# https://dida.do/blog/understanding-mgrs-coordinates
def getMGRSpolygon_alternative(lon, lat, precision):
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
    polygon = Polygon([southwest,
         southeast,
         northeast,
         northwest
        ])
    #plt.plot(*polygon.exterior.xy)
    #return polygon.wkt
    # return polygon's wkt with flipped coordinates (lon, lat)
    return transform(lambda x, y: (y, x), polygon).wkt

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
    return polygon.wkt

# DON'T USE THIS, WRONG ORDER OF POLYGON COORDINATES
# bounding box surrounding the point at given coordinates,
# assuming local approximation of Earth surface as a sphere
# of radius given by WGS84
# https://stackoverflow.com/questions/238260/how-to-calculate-the-bounding-box-for-a-given-lat-lng-location
def boundingBox(latitudeInDegrees, longitudeInDegrees, halfSideInKm):
    lat = deg2rad(latitudeInDegrees)
    lon = deg2rad(longitudeInDegrees)
    halfSide = 1000 * halfSideInKm

    # Radius of Earth at given latitude
    radius = WGS84EarthRadius(lat)
    # Radius of the parallel at given latitude
    pradius = radius * math.cos(lat)

    latMin = lat - halfSide / radius
    latMax = lat + halfSide / radius
    lonMin = lon - halfSide / pradius
    lonMax = lon + halfSide / pradius
    
    southwest = (rad2deg(latMin), rad2deg(lonMin))
    northwest = (rad2deg(latMax), rad2deg(lonMin))
    southeast = (rad2deg(latMin), rad2deg(lonMax))
    northeast = (rad2deg(latMax), rad2deg(lonMax))
    
    return southwest, northwest, southeast, northeast
    #return (rad2deg(latMin), rad2deg(lonMin), rad2deg(latMax), rad2deg(lonMax))
    
# get the wkt of MGRS polygon built from the bottom left coordinate clockwise from precision in meters
# https://stackoverflow.com/questions/66921642/create-a-square-polygon-from-lower-left-corner-coordinates
# https://gis.stackexchange.com/questions/354273/flipping-coordinates-with-shapely
def getMGRSpolygon(lon, lat, precision):
    # assume each site is precision in meters
    d = distance.distance(kilometers=precision/1000)

    # going clockwise, from lower-left to upper-left, upper-right...
    p1 = geopy.Point((lat, lon))
    p2 = d.destination(point=p1, bearing=0)
    p3 = d.destination(point=p2, bearing=90)
    p4 = d.destination(point=p3, bearing=180)

    points = [(p.latitude, p.longitude) for p in [p1,p2,p3,p4]]
    polygon = Polygon(points)
    #plt.plot(*polygon.exterior.xy)
    #return polygon.wkt
    # return polygon's wkt with flipped coordinates (lon, lat)
    return transform(lambda x, y: (y, x), polygon).wkt


# Naldi 01/04/2025 -> separate data creation from insertion
def buildOD_MGRS(od_id, x_orig, y_orig, x_dest, y_dest, from_date, to_date, precision, values, value_type, value_unit, description, organization, kind, mode, transport, purpose):
    tuples_data = []
    tuples_metadata = []

    # for each item
    for i in range (len(x_orig)):
        # get MGRS corners for origin and destination
        # prepare the data
        #orig_polygon = getMGRSpolygon(x_orig[i], y_orig[i], precision)
        #dest_polygon = getMGRSpolygon(x_dest[i], y_dest[i], precision)
        #orig_polygon = getMGRSpolygon_alternative(x_orig[i], y_orig[i], precision)
        #dest_polygon = getMGRSpolygon_alternative(x_dest[i], y_dest[i], precision)
        orig_polygon = get_MGRS_Polygon_Shapely(x_orig[i], y_orig[i], precision)
        dest_polygon = get_MGRS_Polygon_Shapely(x_dest[i], y_dest[i], precision)
        t = [od_id, x_orig[i], y_orig[i], x_dest[i], y_dest[i], values[i], 
             precision, from_date, to_date, orig_polygon, dest_polygon]
        
        # append data as tuple
        tuples_data.append(tuple(t))
        
        # prepare metadata
        t = [od_id, value_type, value_unit, description, organization, kind, mode, transport, purpose,
            od_id, value_type, value_unit, description, organization, kind, mode, transport, purpose]
        
        # append metadata as tuple
        tuples_metadata.append(tuple(t))
    return tuples_data, tuples_metadata
    
# insert OD data (MGRS) into PostgreSQL
def insertOD_MGRS(tuples_data, tuples_metadata):
    result = False
    # calculate OD's id (SHA1 of x_orig + y_orig + x_dest + y_dest + precision + from_date + to_date)
    #od_id = str(x_orig) + str(y_orig) + str(x_dest) + str(y_dest) + str(precision) + from_date + to_date
    #od_id = hashlib.sha1(od_id.encode()).hexdigest()
    try:
        connection = psgConnect(config)
        
        #insert_data = '''
        #INSERT INTO public.od_data (od_id, x_orig, y_orig, x_dest, y_dest, value, 
        #precision, from_date, to_date, orig_geom, dest_geom) 
        #VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s,
        #ST_GeomFromText('POINT(' || %s || ' ' || %s || ')', 4326),
        #ST_GeomFromText('POINT(' || %s || ' ' || %s || ')', 4326))
        #'''
        
        polygon = 'POLYGON((%s %s, %s %s, %s %s, %s %s, %s %s))'
        
        insert_data = '''
        INSERT INTO public.od_data_mgrs (od_id, x_orig, y_orig, x_dest, y_dest, value, 
        precision, from_date, to_date, orig_geom, dest_geom) 
        VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, ST_GeomFromText(%s, 4326), ST_GeomFromText(%s, 4326))
        '''
        
        '''
        ST_GeomFromText('POLYGON((' || %s || ' ' || %s || ', ' || %s || ' ' || %s || ', ' || %s || ' ' || %s || ', ' || %s || ' ' || %s || ', ' || %s || ' ' ||  %s || '))', 4326),
        ST_GeomFromText('POLYGON((' || %s || ' ' || %s || ', ' || %s || ' ' || %s || ', ' || %s || ' ' || %s || ', ' || %s || ' ' || %s || ', ' || %s || ' ' ||  %s || '))', 4326))
        '''

        insert_metadata = '''
        INSERT INTO public.od_metadata (od_id, 
        value_type, value_unit, description, organization, kind, mode, transport, purpose) 
        VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s)
        ON CONFLICT(od_id)
        DO UPDATE SET od_id = %s, 
        value_type = %s, value_unit = %s, description = %s, organization = %s, 
        kind = %s, mode = %s, transport = %s, purpose = %s
        '''
                
        # get the cursor
        cursor = connection.cursor(cursor_factory=psycopg2.extras.DictCursor)

        #print('[insertOD_MGRS] query: ', insert_data)
        #print('[insertOD_MGRS] query: ', insert_metadata)
            
        # insert
        if len(tuples_data) > 0:
            cursor.executemany(insert_metadata, tuples_metadata)
            cursor.executemany(insert_data, tuples_data)
            connection.commit()
            result = True

    except (Exception, psycopg2.Error) as error:
        print("Error while inserting data to PostgreSQL", error)

    finally:
        # closing database connection
        if(connection):
            cursor.close()
            connection.close()
        return result
    

# Naldi 02/04/2025
def get_geometry_by_communes_id(orig_communes, dest_communes, source):
    try:
        connection = psgConnect(config)
        # get the cursor
        cursor = connection.cursor(cursor_factory=psycopg2.extras.DictCursor)
        result = []

        if orig_communes is None or orig_communes == '':
            raise Exception("Unable to retrieve source table for orig_communes: %s" % orig_communes)
        if dest_communes is None or dest_communes == '':
            raise Exception("Unable to retrieve source table for dest_communes: %s" % dest_communes)
        
        if source is None or source == '':
            source = 'gadm36'
        
        #query
        query = '''
        SELECT ST_AsText(geom) as geometry 
        FROM public.''' + source + ''' 
        WHERE uid IN (''' + ", ".join(f"'{item}'" for item in orig_communes) + ', ' + ", ".join(f"'{item}'" for item in dest_communes) + ''') 
        '''

        # fetch results as dataframe
        df = pd.read_sql_query(query, connection)
        if not df.empty:
            result = df['geometry'].tolist()

        #print('[get_geometry_by_communes_id] query: ', query)
        #print('[get_geometry_by_communes_id] query result: ', result)
        

    except (Exception, psycopg2.Error) as error:
        print("Error while inserting data to PostgreSQL", error)

    finally:
        # closing database connection
        if(connection):
            cursor.close()
            connection.close()
        return result
    
    

# Naldi 02/04/2025 -> separate data creation from insertion
def buildOD_Communes(od_id, orig_communes, dest_communes, from_date, to_date, values, value_type, value_unit, description, organization, kind, mode, transport, purpose, source):
    tuples_data = []
    tuples_metadata = []

    # for each item
    for i in range (len(orig_communes)):
        # prepare the data
        t = [od_id, orig_communes[i], dest_communes[i], values[i], from_date, to_date]
        
        # append data as tuple
        tuples_data.append(tuple(t))
        
        # prepare metadata
        t = [od_id, value_type, value_unit, description, organization, kind, mode, transport, purpose, source,
             od_id, value_type, value_unit, description, organization, kind, mode, transport, purpose, source]
        
        # append metadata as tuple
        tuples_metadata.append(tuple(t))

    return tuples_data, tuples_metadata



# insert OD data (Communes) into PostgreSQL
# >>>>>>>>>> modified to handle insertion of data from the new table 'italy_epgs4326':
#            - in od_metadata added new column 'source' to specify the table to which UID refers  
def insertOD_Communes(tuples_data, tuples_metadata):
    result = False
    # calculate OD's id (SHA1 of x_orig + y_orig + x_dest + y_dest + precision + from_date + to_date)
    #od_id = str(x_orig) + str(y_orig) + str(x_dest) + str(y_dest) + str(precision) + from_date + to_date
    #od_id = hashlib.sha1(od_id.encode()).hexdigest()
    try:
        connection = psgConnect(config)

        #insert_data = '''
        #INSERT INTO public.od_data (od_id, x_orig, y_orig, x_dest, y_dest, value, 
        #precision, from_date, to_date, orig_geom, dest_geom) 
        #VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s,
        #ST_GeomFromText('POINT(' || %s || ' ' || %s || ')', 4326),
        #ST_GeomFromText('POINT(' || %s || ' ' || %s || ')', 4326))
        #'''
        
        polygon = 'POLYGON((%s %s, %s %s, %s %s, %s %s, %s %s))'
        
        insert_data = '''
        INSERT INTO public.od_data (od_id, orig_commune, dest_commune, value, 
        from_date, to_date) 
        VALUES (%s, %s, %s, %s, %s, %s)
        '''

        insert_metadata = '''
        INSERT INTO public.od_metadata (od_id, 
        value_type, value_unit, description, organization, kind, mode, transport, purpose, source) 
        VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
        ON CONFLICT(od_id)
        DO UPDATE SET od_id = %s, 
        value_type = %s, value_unit = %s, description = %s, organization = %s, 
        kind = %s, mode = %s, transport = %s, purpose = %s, source = %s
        '''
                
        # get the cursor
        cursor = connection.cursor(cursor_factory=psycopg2.extras.DictCursor)

        #print('[insertOD_Communes] query: ', insert_data)
        #print('[insertOD_Communes] query: ', insert_metadata)
            
        # insert
        if len(tuples_data) > 0:
            cursor.executemany(insert_metadata, tuples_metadata)
            cursor.executemany(insert_data, tuples_data)
            connection.commit()
            result = True

    except (Exception, psycopg2.Error) as error:
        print("Error while inserting data to PostgreSQL", error)

    finally:
        # closing database connection
        if(connection):
            cursor.close()
            connection.close()
        return result
    

# get the wkt of the bounding box of the polygons    
def get_wkt_box(polygons_wkt):
    polygons = [loads(polygon_wkt) for polygon_wkt in polygons_wkt]
    union_polygon = unary_union(polygons)
    bounding_box = union_polygon.bounds
    xmin, ymin, xmax, ymax = bounding_box
    return f"POLYGON(({xmin} {ymin}, {xmax} {ymin}, {xmax} {ymax}, {xmin} {ymax}, {xmin} {ymin}))"


# convert wkt to geojson
def wtkToGeoJSON(polygon_wkt):
    polygon = loads(polygon_wkt)
    geojson_data = geojson.Feature(geometry=polygon, properties={})
    return geojson_data['geometry']

# check authentication before each request
INSERT_AREAS_ENDPOINTS = ['od_circular_custom_area', 'od_custom_area']

@app.before_request
def before_request():
    if request.endpoint not in INSERT_AREAS_ENDPOINTS:
        args = parser.parse_args()
        g.model = args['model']
        g.device_type = args['type']
        g.contextbroker = args['contextbroker']
        g.producer = args['producer']
        g.subnature = args['subnature']
        g.organization = args['organization']

    #auth
    token, message, status = basic_auth(config, request)
    if status == None or status != 200:
        resp = jsonify({'message':message, 'status':status})
        resp.status_code = status
        return resp
    g.token = token

# after inserting data, clear flask global variable
@app.after_request
def after_request(response):
    # clear g
    g.token = None
    if request.endpoint not in INSERT_AREAS_ENDPOINTS:
        g.model = None
        g.device_type = None
        g.contextbroker = None
        g.producer = None
        g.subnature = None
        g.organization = None

    return response


def try_insert_data_in_device(content, coords, poly, device, token, model, device_type, contextbroker, producer, subnature):
    if device == 'not_exists':

        #create device
        message, status = create_device(config, token, content['od_id'], model, producer, subnature, coords, poly)
        if status == None or status != 200:
            return {'message':message, 'status':status}, status
        # insert into device
        data = {
            'description': content['description'],
            'precision': content['precision'] if 'precision' in content and content['precision'] is not None else None,
            'kind': content['kind'],
            'mode': content['mode'],
            'transport': content['transport'],
            'purpose': content['purpose'],
            'instances': 0,
            'from_date': content['from_date'],
            'to_date': content['to_date'],
            'geometry': wtkToGeoJSON(poly),
            'colormap_name': content['colormap_name'],
            'representation': content['representation']
        }
        message, status = insert_data(config, token, content['od_id'], device_type, contextbroker, data)
        if status == None or (status != 204 and status != 200):
            return {'message':message, 'status':status}, status
    else:
        try:
            old_data = device['realtime']['results']['bindings'][0]
        except KeyError:
            return {'message': 'Please retry', status:503}, 503
        # insert into device
        data = {
            'description': content['description'] if content['description'] is not None else old_data['description']['value'],
            'precision': content['precision'] if 'precision' in content and content['precision'] is not None else old_data.get('precision', {}).get('value'),
            'kind': content['kind'] if content['kind'] is not None else old_data['kind']['value'],
            'mode': content['mode'] if content['mode'] is not None else old_data['mode']['value'],
            'transport': content['transport'] if content['transport'] is not None else old_data['transport']['value'],
            'purpose': content['purpose'] if content['purpose'] is not None else old_data['purpose']['value'],
            'instances': int(old_data['instances']['value']) + 1,
            'from_date': content['from_date'] if content['from_date'] is not None else old_data['fromDate']['value'],
            'to_date': content['to_date'] if content['to_date'] is not None else old_data['toDate']['value'],
            'colormap_name': content['colormap_name'] if content['colormap_name'] is not None else old_data['colormapName']['value'],
            'representation': content['representation'] if content['representation'] is not None else old_data['representation']['value'],
            'geometry': json.loads(old_data['geometry']['value'])
        }
        message, status = insert_data(config, token, content['od_id'], device_type, contextbroker, data)
        if status == None or (status != 204 and status != 200):
            return {'message':message, 'status':status}, status
    return None, 200


#Naldi 02/07/2025 -> check if area insert is unique by searching is poi_id
def check_poi_id_uniqueness(poi_id):
    result = False
    err = None
    try:
        connection = psgConnect(config)

        query = '''
        SELECT *
        FROM public.italy_epgs4326 
        WHERE poi_id = \'''' + poi_id + '''\'
        '''
        print(query)
        # fetch results as dataframe
        df = pd.read_sql_query(query, connection)
        result = df.empty


    except (Exception, psycopg2.Error) as error:
        print("Error while searching data into PostgreSQL", error)
        result = False
        err = "PostgreSQL Error. See logs for more info"

    finally:
        # closing database connection
        if(connection):
            connection.close()
        return result, err

#Naldi 02/07/2025 -> insert into postgis db a custom circular area
def insert_circular_area(poi_id, name, shape_area, shape_leng, latitude, longitude, radius):
    result = False
    err = None
    try:
        connection = psgConnect(config)
        cursor = connection.cursor(cursor_factory=psycopg2.extras.DictCursor)

        #get last uid
        query = '''
        SELECT MAX(uid) as uid
        FROM public.italy_epgs4326
        '''
        print(query)
        # fetch results as dataframe
        df = pd.read_sql_query(query, connection)
        if df.empty:
            uid = 1
        else:
            uid = int(df['uid'][0])+1

        query = '''
        INSERT INTO public.italy_epgs4326 (
            uid, text_uid, cod_reg, cod_prov, cod_prov_storico, cod_com, cod_ace, cod_sez, 
            poi_id, name, nuts1, nuts2, nuts3, shape_area, shape_leng, geom
        )
        VALUES (%s, %s, 'NULL', 'NULL', 'NULL', 'NULL', 'NULL', 'NULL', %s, %s, 'NULL', 'NULL', 'NULL', %s, %s,
        ST_Buffer(ST_SetSRID(ST_MakePoint(%s, %s),4326)::geography, %s)::geometry);
        '''

        # fetch results as dataframe
        cursor.execute(query, (uid, "POI_"+poi_id, poi_id, name, shape_area, shape_leng, longitude, latitude, radius))
        connection.commit()
        result = True

    except (Exception, psycopg2.Error) as error:
        print("Error while inserting data to PostgreSQL", error)
        result = False
        err = "PostgreSQL Error. See logs for more info"

    finally:
        # closing database connection
        if(connection):
            cursor.close()
            connection.close()
        return result, err

#Naldi 02/07/2025 -> get surface in m2 and length in m     
def get_surface_length_custom_area(custom_area):
    if isinstance(custom_area, str):
        custom_area = json.loads(custom_area)

    geom = shape(custom_area)

    # Project to an equal-area projection for area calculation
    crs_wgs84 = CRS('EPSG:4326')
    crs_equal_area = CRS('EPSG:6933')  # Cylindrical Equal Area projection

    project = Transformer.from_crs(crs_wgs84, crs_equal_area, always_xy=True).transform
    geom_proj = transform(project, geom)

    area_m2 = geom_proj.area
    length_m = geom_proj.length

    return area_m2, length_m


#Naldi 02/07/2025 -> insert into postgis db a custom area
def insert_custom_area(poi_id, name, shape_area, shape_leng, custom_area):
    result = False
    err = None
    try:
        connection = psgConnect(config)
        cursor = connection.cursor(cursor_factory=psycopg2.extras.DictCursor)

        #get last uid
        query = '''
        SELECT MAX(uid) as uid
        FROM public.italy_epgs4326
        '''
        print(query)
        # fetch results as dataframe
        df = pd.read_sql_query(query, connection)
        if df.empty:
            uid = 1
        else:
            uid = int(df['uid'][0])+1

        query = '''
        INSERT INTO public.italy_epgs4326 (
            uid, text_uid, cod_reg, cod_prov, cod_prov_storico, cod_com, cod_ace, cod_sez, 
            poi_id, name, nuts1, nuts2, nuts3, shape_area, shape_leng, geom
        )
        VALUES (%s, %s, 'NULL', 'NULL', 'NULL', 'NULL', 'NULL', 'NULL', %s, %s, 'NULL', 'NULL', 'NULL', %s, %s,
        ST_SetSRID(ST_GeomFromGeoJSON(%s),4326));
        '''

        # fetch results as dataframe
        cursor.execute(query, (uid, "POI_"+poi_id, poi_id, name, shape_area, shape_leng, custom_area))
        connection.commit()
        result = True

    except (Exception, psycopg2.Error) as error:
        print("Error while inserting data to PostgreSQL", error)
        result = False
        err = "PostgreSQL Error. See logs for more info"

    finally:
        # closing database connection
        if(connection):
            cursor.close()
            connection.close()
        return result, err

    
    
class OD_MGRS(Resource):
    def post(self):
        # parse arguments
        #args = parser.parse_args()
        
        #data = json.loads(args['data'])
        '''
        x_orig = json.loads(args['x_orig']) #json.loads(request.args.post('x_orig'))
        y_orig = json.loads(args['y_orig']) #json.loads(request.args.post('y_orig'))
        x_dest = json.loads(args['x_dest']) #json.loads(request.args.post('x_dest'))
        y_dest = json.loads(args['y_dest']) #json.loads(request.args.post('y_dest'))
        from_date = args['from_date'] #request.args.post('from_date')
        to_date = args['to_date'] #request.args.post('to_date')
        precision = int(args['precision']) #json.loads(request.args.post('precision'))
        values = json.loads(args['values']) #json.loads(request.args.post('values'))
        value_type = args['value_type'] #request.args.post('value_type')
        value_unit = args['value_unit'] #request.args.post('value_unit')
        description = args['description'] #request.args.post('description')
        organization = args['organization'] #request.args.post('organization')
        kind = args['kind'] #request.args.post('kind')
        mode = args['mode'] #request.args.post('mode')
        transport = args['transport'] #request.args.post('transport')
        purpose = args['purpose'] #request.args.post('purpose')
        '''

        content = request.get_json()

        missing_params = []

        required_params = [
            'od_id', 'x_orig', 'y_orig', 'x_dest', 'y_dest',
            'from_date', 'to_date', 'precision', 'values',
            'value_type', 'value_unit', 'description', 'organization',
            'kind', 'mode', 'transport', 'purpose',
            'colormap_name', 'representation'
        ]
        
        for param in required_params:
            if param not in content:
                missing_params.append(param)
            elif param == 'x_orig' and not content.get('x_orig'):  # also handles empty list or None
                missing_params.append('x_orig (empty)')
        
        if missing_params:
            resp = jsonify({
                'message': 'Missing or invalid data',
                'missing': missing_params,
                'status': 400
            })
            resp.status_code = 400
            return resp
            
        
        #check device ownership/existence before inserting data
        token = getattr(g, "token", None)
        model = getattr(g, "model", None)
        device_type = getattr(g, "device_type", None)
        contextbroker = getattr(g, "contextbroker", None)
        producer = getattr(g, "producer", None)
        subnature = getattr(g, "subnature", None)
        organization = getattr(g, "organization", None)

        required_fields = {
            'token': token,
            'model': model,
            'device_type': device_type,
            'contextbroker': contextbroker,
            'subnature': subnature,
            'organization': organization,
        }

        for field_name, value in required_fields.items():
            if value is None:
                resp = jsonify({'message': f'{field_name} is None', 'status': 400})
                resp.status_code = 400
                return resp
            
        #check od_id: must be name-without-underscore_organization_precision
        splits = content['od_id'].split("_")
        if len(splits) != 3:
            resp = jsonify({'message': 'Invalid OD id, must be name-without-underscore_<organization>_<precision>', 'reason': 'too much undescores', 'status': 400})
            resp.status_code = 400
            return resp

        if splits[1] != organization:
            resp = jsonify({'message': 'Invalid OD id, must be name-without-underscore_<organization>_<precision>', 'reason': 'organization mismatch', 'status': 400})
            resp.status_code = 400
            return resp
        
        if splits[2] != str(content['precision']):
            resp = jsonify({'message': 'Invalid OD id, must be name-without-underscore_<organization>_<precision>', 'reason': "precision mismatch", 'status': 400})
            resp.status_code = 400
            return resp
        
        #check if device already exists
        device, status = check_ownership_by_id(config, token, content['od_id'], organization, contextbroker)
        if status == None or status != 200:
            device = 'not_exists'

        tuples_data, tuples_metadata = buildOD_MGRS(
            content['od_id'], content['x_orig'], content['y_orig'], content['x_dest'], content['y_dest'], 
            content['from_date'], content['to_date'], content['precision'], content['values'], content['value_type'], 
            content['value_unit'], content['description'], content['organization'],content['kind'], content['mode'], 
            content['transport'], content['purpose']
        )

        # get wkt
        polygons_wkt = [poly for t in tuples_data for poly in t[-2:]]
        poly = get_wkt_box(polygons_wkt)
        centroid = loads(poly).centroid
        coords = {'lat':float(centroid.y), 'lng':float(centroid.x)}

        result, status = try_insert_data_in_device(content, coords, poly, device, token, model, device_type, contextbroker, producer, subnature)
        if status != 200:
            resp = jsonify({'message':result, 'status':status})
            resp.status_code = status
            return resp
        
        #insert data into PostgreSQL
        result = insertOD_MGRS(tuples_data, tuples_metadata)
        if result:
            resp = jsonify({'message':'data inserted successfully', 'status':200})
            resp.status_code = 200
            return resp




# 2022/04/21 modified to accept the new 'source' parameter. To keep retrocompatibility
#            the function check if 'source' is in 'content', and if missing 'gadm36' is
#            selected as default.
class OD_Communes(Resource):
    def post(self):
        # parse arguments
        content = request.get_json()
        if('source' not in content):
            content['source'] = ''

        missing_params = []

        required_params = [
            'od_id', 'orig_communes', 'dest_communes', 'from_date', 'to_date',
            'values', 'value_type', 'value_unit', 'description', 'organization',
            'kind', 'mode', 'transport', 'purpose', 'source',
            'colormap_name', 'representation'
        ]
        
        for param in required_params:
            if param not in content or (param == 'orig_communes' and len(content.get('orig_communes', [])) == 0):
                missing_params.append(param)
        
        if missing_params:
            resp = jsonify({
                'message': 'Missing data',
                'missing': missing_params,
                'status': 400
            })
            resp.status_code = 400
            return resp
        
        #check device ownership/existence before inserting data
        token = getattr(g, "token", None)
        model = getattr(g, "model", None)
        device_type = getattr(g, "device_type", None)
        contextbroker = getattr(g, "contextbroker", None)
        producer = getattr(g, "producer", None)
        subnature = getattr(g, "subnature", None)
        organization = getattr(g, "organization", None)

        required_fields = {
            'token': token,
            'model': model,
            'device_type': device_type,
            'contextbroker': contextbroker,
            'subnature': subnature,
            'organization': organization,
        }

        for field_name, value in required_fields.items():
            if value is None:
                resp = jsonify({'message': f'{field_name} is None', 'status': 400})
                resp.status_code = 400
                return resp
            
        #check od_id: must be name-without-underscore_organization_precision
        splits = content['od_id'].split("_")
        if len(splits) != 3:
            resp = jsonify({'message': 'Invalid OD id, must be name-without-underscore_<organization>_<precision>', 'reason': 'too much undescores', 'status': 400})
            resp.status_code = 400
            return resp

        if splits[1] != organization:
            resp = jsonify({'message': 'Invalid OD id, must be name-without-underscore_<organization>_<precision>', 'reason': 'organization mismatch', 'status': 400})
            resp.status_code = 400
            return resp
        
        #check if device already exists
        device, status = check_ownership_by_id(config, token, content['od_id'], organization, contextbroker)
        if status == None or status != 200:
            device = 'not_exists'

        tuples_data, tuples_metadata = buildOD_Communes(content['od_id'], content['orig_communes'], content['dest_communes'],
            content['from_date'], content['to_date'], content['values'], content['value_type'], 
            content['value_unit'], content['description'], content['organization'],
            content['kind'], content['mode'], content['transport'], content['purpose'], content['source']
        )
        
        # get wkt
        polygons_wkt = get_geometry_by_communes_id(content['orig_communes'], content['dest_communes'], content['source'])
        poly = get_wkt_box(polygons_wkt)
        centroid = loads(poly).centroid
        coords = {'lat':float(centroid.y), 'lng':float(centroid.x)}
        result, status = try_insert_data_in_device(content, coords, poly, device, token, model, device_type, contextbroker, producer, subnature)
        if status != 200:
            resp = jsonify({'message':result, 'status':status})
            resp.status_code = status
            return resp
        
        #insert data into PostgreSQL
        result = insertOD_Communes(tuples_data, tuples_metadata)
        if result:
            resp = jsonify({'message':'data inserted successfully', 'status':200})
            resp.status_code = status
            return resp
        
# Naldi 02/07/2025 -> add new endpoint to insert into postgis db custom circular area starting from lat, lng, rad
class OD_Circular_Custom_Area(Resource):
    def post(self):
        content = request.get_json()

        missing_params = []

        required_params = [
            'poi_id', 'name', 'latitude', 'longitude', 'radius'
        ]
        
        for param in required_params:
            if param not in content:
                missing_params.append(param)
        
        if missing_params:
            resp = jsonify({
                'message': 'Missing data',
                'missing': missing_params,
                'status': 400
            })
            resp.status_code = 400
            return resp
        
        poi_id = content['poi_id']
        radius = content['radius']
        
        #check if poi_id already exists
        result, err = check_poi_id_uniqueness(poi_id)
        if not result:
            m = err if err is not None else 'POI id passed is already in use, please choose another one'
            resp = jsonify({'message': m,'status': 400})
            resp.status_code = 400
            return resp
        #calcuate area things
        shape_area = np.pi * radius * radius
        shape_leng = 2 * np.pi * radius
        #insert into db and return ok/error
        result, err = insert_circular_area(poi_id, content['name'], shape_area, shape_leng, content['latitude'], content['longitude'], content['radius'])
        if result:
            resp = jsonify({'message': 'Custom area insert succeed','status': 200})
            resp.status_code = 200
        else:
            resp = jsonify({'message': 'Something went wrong with data inserting','status': 400, 'err': err})
            resp.status_code = 400
        return resp

# Naldi 02/07/2025 -> add new endpoint to insert into postgis db custom area starting from geoJSON
class OD_Custom_Area(Resource):
    def post(self):
        content = request.get_json()

        missing_params = []

        required_params = [
            'poi_id', 'name', 'custom_area'
        ]
        
        for param in required_params:
            if param not in content:
                missing_params.append(param)
        
        if missing_params:
            resp = jsonify({
                'message': 'Missing data',
                'missing': missing_params,
                'status': 400
            })
            resp.status_code = 400
            return resp
        
        poi_id = content['poi_id']
        
        
        #check if poi_id already exists
        result, err = check_poi_id_uniqueness(poi_id)
        if not result:
            m = err if err is not None else 'POI id passed is already in use, please choose another one'
            resp = jsonify({'message': m,'status': 400})
            resp.status_code = 400
            return resp
        #calcuate area things
        shape_leng, shape_area = get_surface_length_custom_area(content['custom_area'])
        #insert into db and return ok/error
        result, err = insert_custom_area(poi_id, content['name'], shape_area, shape_leng, content['custom_area'])
        if result:
            resp = jsonify({'message': 'Custom area insert succeed','status': 200})
            resp.status_code = 200
        else:
            resp = jsonify({'message': 'Something went wrong with data inserting','status': 400, 'err': err})
            resp.status_code = 400
        return resp                                        
        
api.add_resource(OD_MGRS, '/insert')
api.add_resource(OD_Communes, '/insertcommunes')
api.add_resource(OD_Circular_Custom_Area, '/insertcirculararea')
api.add_resource(OD_Custom_Area, '/insertcustomarea')

# enable CORS
CORS(app, resources={r'/*': {'origins': '*'}})

if __name__ == '__main__':
    #app.run(debug=True)
    #app.run(host="0.0.0.0", port=3000, debug=True)
    '''
    avoid 'WARNING: This is a development server. Do not use it in a production deployment.'
    when running Flask, use waitress instead to serve
    https://stackoverflow.com/questions/51025893/flask-at-first-run-do-not-use-the-development-server-in-a-production-environmen
    '''
    #print(config)
    print("[OD-INSERT API] Accepting connections on port 3100")
    serve(app, host='0.0.0.0', port=3100)
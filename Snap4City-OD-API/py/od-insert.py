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
    value numeric NULL,
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
    purpose varchar NULL
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

from flask import Flask, request
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
from shapely.geometry import Polygon as PolygonShapely
from shapely.ops import transform
import psycopg2
import psycopg2.extras
import yaml

with open(r'config.yaml') as file:
    config = yaml.load(file, Loader=yaml.FullLoader)

app = Flask(__name__)
api = Api(app)

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

def psgConnect(conf):
    conn = psycopg2.connect(user=conf['user_psg'],
                            password=conf['password_psg'],
                            host=conf['host_psg'],
                            port=conf['port_psg'],
                            database=conf['database_psg'])
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
    
# insert OD data (MGRS) into PostgreSQL
def insertOD_MGRS(od_id, x_orig, y_orig, x_dest, y_dest, from_date, to_date, precision, values, value_type, value_unit, description, organization, kind, mode, transport, purpose):
    result = False
    # calculate OD's id (SHA1 of x_orig + y_orig + x_dest + y_dest + precision + from_date + to_date)
    #od_id = str(x_orig) + str(y_orig) + str(x_dest) + str(y_dest) + str(precision) + from_date + to_date
    #od_id = hashlib.sha1(od_id.encode()).hexdigest()
    try:
        tuples_data = []
        tuples_metadata = []
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
            
        # insert
        if len(tuples_data) > 0:
            cursor.executemany(insert_metadata, tuples_metadata)
            cursor.executemany(insert_data, tuples_data)
            connection.commit()
            tuples_data = []
            tuples_metadata = []
            result = True

    except (Exception, psycopg2.Error) as error:
        print("Error while inserting data to PostgreSQL", error)

    finally:
        # closing database connection
        if(connection):
            cursor.close()
            connection.close()
        return result

# insert OD data (Communes) into PostgreSQL
def insertOD_Communes(od_id, orig_communes, dest_communes, from_date, to_date, values, value_type, value_unit, description, organization, kind, mode, transport, purpose):
    result = False
    # calculate OD's id (SHA1 of x_orig + y_orig + x_dest + y_dest + precision + from_date + to_date)
    #od_id = str(x_orig) + str(y_orig) + str(x_dest) + str(y_dest) + str(precision) + from_date + to_date
    #od_id = hashlib.sha1(od_id.encode()).hexdigest()
    try:
        tuples_data = []
        tuples_metadata = []
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
        value_type, value_unit, description, organization, kind, mode, transport, purpose) 
        VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s)
        ON CONFLICT(od_id)
        DO UPDATE SET od_id = %s, 
        value_type = %s, value_unit = %s, description = %s, organization = %s, 
        kind = %s, mode = %s, transport = %s, purpose = %s
        '''
                
        # get the cursor
        cursor = connection.cursor(cursor_factory=psycopg2.extras.DictCursor)
        # for each item
        for i in range (len(orig_communes)):
            # prepare the data
            t = [od_id, orig_communes[i], dest_communes[i], values[i], from_date, to_date]
            
            # append data as tuple
            tuples_data.append(tuple(t))
            
            # prepare metadata
            t = [od_id, value_type, value_unit, description, organization, kind, mode, transport, purpose,
                od_id, value_type, value_unit, description, organization, kind, mode, transport, purpose]
            
            # append metadata as tuple
            tuples_metadata.append(tuple(t))
            
        # insert
        if len(tuples_data) > 0:
            cursor.executemany(insert_metadata, tuples_metadata)
            cursor.executemany(insert_data, tuples_data)
            connection.commit()
            tuples_data = []
            tuples_metadata = []
            result = True

    except (Exception, psycopg2.Error) as error:
        print("Error while inserting data to PostgreSQL", error)

    finally:
        # closing database connection
        if(connection):
            cursor.close()
            connection.close()
        return result
    
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
        
        if ('od_id' in content and
            'x_orig' in content and 
            'y_orig' in content and
            'x_dest' in content and
            'y_dest' in content and
            'from_date' in content and
            'to_date' in content and
            'precision' in content and
            'values' in content and
            'value_type' in content and
            'value_unit' in content and
            'description' in content and
            'organization' in content and
            'kind' in content and
            'mode' in content and
            'transport' in content and
            'purpose' in content and
            len(content['x_orig']) > 0):
            return insertOD_MGRS(content['od_id'], content['x_orig'], content['y_orig'], content['x_dest'], content['y_dest'], content['from_date'], 
                            content['to_date'], content['precision'], content['values'], content['value_type'], 
                            content['value_unit'], content['description'], content['organization'],
                            content['kind'], content['mode'], content['transport'], content['purpose'])

class OD_Communes(Resource):
    def post(self):
        # parse arguments
        content = request.get_json()
        
        if ('od_id' in content and
            'orig_communes' in content and 
            'dest_communes' in content and
            'from_date' in content and
            'to_date' in content and
            'values' in content and
            'value_type' in content and
            'value_unit' in content and
            'description' in content and
            'organization' in content and
            'kind' in content and
            'mode' in content and
            'transport' in content and
            'purpose' in content and
            len(content['orig_communes']) > 0):
            return insertOD_Communes(content['od_id'], content['orig_communes'], content['dest_communes'],
                            content['from_date'], content['to_date'], content['values'], content['value_type'], 
                            content['value_unit'], content['description'], content['organization'],
                            content['kind'], content['mode'], content['transport'], content['purpose'])
        
api.add_resource(OD_MGRS, '/insert')
api.add_resource(OD_Communes, '/insertcommunes')

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
    print(config)
    print("accepting connections on port 3100")
    serve(app, host='0.0.0.0', port=3100)

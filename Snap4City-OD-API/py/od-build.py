"""SNAP4 Origin Destination Server (OBF).
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
from flask import Flask, request
from flask_restful import reqparse, Resource, Api
from flask_cors import CORS
from shapely import wkb
from waitress import serve
import json
import pandas as pd
import numpy as np
import psycopg2
import psycopg2.extras
import io
import pyutm
import mgrs
import yaml
import os
import ast
from auth import basic_auth

script_dir = os.path.dirname(os.path.realpath(__file__))

with open(os.path.join(script_dir,'config.yaml'), 'r') as file:
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
parser.add_argument('precision', type=str, required=True)
'''

def psgConnect(conf):
    conn = psycopg2.connect(user=os.getenv('POSTGRES_USER', conf['POSTGRES_USER']),
                            password=os.getenv('POSTGRES_PASSWORD', conf['POSTGRES_PASSWORD']),
                            host=os.getenv('POSTGRES_HOST', conf['POSTGRES_HOST']),
                            port=os.getenv('POSTGRES_PORT', conf['POSTGRES_PORT']),
                            database=os.getenv('POSTGRES_DATABASE', conf['POSTGRES_DATABASE']))
    return conn

# build OD MGRS matrix with precision (m)
# https://stackoverflow.com/questions/56520616/is-it-possible-from-dataframe-transform-to-matrix
def buildOD_mgrs(x_orig, y_orig, x_dest, y_dest, precision):
    # get MGRS grid references for origins and destinations, with precision (m)
    #lonlat = [(x_orig[i], y_orig[i]) for i in range(len(x_orig))]
    #orig = getReference(lonlat, precision)
    #orig_ref = [orig[i][2] for i in range(len(orig))]
    #lonlat = [(x_dest[i], y_dest[i]) for i in range(len(x_dest))]
    #dest = getReference(lonlat, precision)
    #dest_ref = [dest[i][2] for i in range(len(dest))]
    m = mgrs.MGRS()
    mgrs_precision = getMGRSprecision(precision)
    orig_ref = [m.toMGRS(y_orig[i], x_orig[i], MGRSPrecision=mgrs_precision) for i in range(len(x_orig))]
    dest_ref = [m.toMGRS(y_dest[i], x_dest[i], MGRSPrecision=mgrs_precision) for i in range(len(x_dest))]
        
    # merge arrays element-wise
    data = mergeArrays(orig_ref, dest_ref)
    
    # build dataframe
    df = pd.DataFrame({'OD': data})
    places = df['OD'].unique()
    places.sort()
    od_df = pd.DataFrame(df['OD'].values.reshape((-1, 2)), columns=['O', 'D'])
    #od_matrix = od_df.groupby(["O", "D"]).size().unstack().reindex(index=places, columns=places)
    od_matrix = pd.pivot_table(od_df, index='O', columns='D', aggfunc='size').reindex(index=places, columns=places)
    od_matrix.fillna(0, downcast='infer', inplace=True)
    
    return od_matrix

# build OD matrix with precision (m)
# https://stackoverflow.com/questions/56520616/is-it-possible-from-dataframe-transform-to-matrix
def buildOD(x_orig, y_orig, x_dest, y_dest, precision):
    # build OD MGRS matrix with precision (m)
    print("Building prec. ", precision)
    od = buildOD_mgrs(x_orig, y_orig, x_dest, y_dest, precision)
    results = []

    # get the squares' coordinates from MGRS references (columns)
    lat_lon_columns = [getCoordinates(str(column)) for column in od.columns]

    # iterate the OD matrix
    for k, v in od.iterrows():
        # get the square's coordinates from MGRS references (rows)
        lat_lon_rows = getCoordinates(str(k))
        data = v.tolist()
        for i in range(len(data)):
            if data[i] > 0:
                results.append({'x_orig': lat_lon_rows[1], 
                                'y_orig': lat_lon_rows[0], 
                                'x_dest': lat_lon_columns[i][1],
                                'y_dest': lat_lon_columns[i][0],
                                'value': data[i]})
                
    return results

# merge arrays element-wise
# https://stackoverflow.com/questions/17619415/how-do-i-combine-two-numpy-arrays-element-wise-in-python
def mergeArrays(a, b):
    A = np.array(a)
    B = np.array(b)
    return list(np.insert(B, np.arange(len(A)), A))

# get MGRS grid references with precision (m), don't use this, slow implementation
# use 
# m = mgrs.MGRS()
# c = m.toMGRS(latitude, longitude, MGRSPrecision=5)
# MGRSPrecision
# 0: 100 km
# 1: 10 km
# 2: 1 km
# 3: 100 m
# 4: 10 m
# 5: 1 m
def getReference(lonlat, precision):
    #lonlat = [(16.776031, -3.005612), (16.772291, -3.007136)]
    grid_from_list = pyutm.Grid(data=lonlat, epsg=4326)
    return grid_from_list.write_refs(precision=precision)

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
    
# get MGRS coordinates from grid reference
def getCoordinates(grid_reference):
    m = mgrs.MGRS()
    return m.toLatLon(grid_reference)

# get geometry's code for commune
def getGeometryCode(latitude, longitude):
    result = None
    try:
        connection = psgConnect(config)
        cursor = connection.cursor(cursor_factory=psycopg2.extras.DictCursor)
        
        query = '''
        SELECT comm_id
        FROM public.comm_rg_01m_2016_4326
        WHERE ST_Intersects(geom, ST_GeomFromText('POINT(%s %s)', 4326))
        ''' % (longitude, latitude)
        
        # fetch results
        cursor.execute(query)
        results = cursor.fetchall()
        
        for row in results:
            result = row['comm_id']

    except (Exception, psycopg2.Error) as error:
        print("Error while fetching data from PostgreSQL", error)

    finally:
        # closing database connection
        if(connection):
            cursor.close()
            connection.close()
        return result

# get geometries' codes for Tuscany's municipalities using GADM dataset
# https://gadm.org/download_world.html
def getGeometryCodes(latitudes, longitudes):
    result = []
    geometries = ''
    try:
        connection = psgConnect(config)
        cursor = connection.cursor(cursor_factory=psycopg2.extras.DictCursor)
        
        '''
        SELECT comm_id
            FROM public.comm_rg_01m_2016_4326
            WHERE ST_Intersects(geom, 
            ST_Collect(ST_GeomFromText('POINT(10 40)', 4326), ST_GeomFromText('POINT(11 40)', 4326)))
        '''
        # build query with constant table to get comm_id for each (latitude, longitude)
        # https://www.enterprisedb.com/postgres-tutorials/how-create-constant-table-postgresql
        for i in range(len(latitudes)):
            geometries += '''(ST_GeomFromText('POINT(%s %s)', 4326))%s
            ''' % (longitudes[i], latitudes[i], ',' if i < len(latitudes)-1 else '')
        #query = ('''SELECT p, COALESCE(comm_rg_01m_2016_4326.comm_id, '') AS comm_id FROM (values ''' +
        #geometries +
        #''') AS points (p) LEFT JOIN comm_rg_01m_2016_4326 ON ST_Intersects(geom, p)''')
        query = ('''SELECT p, COALESCE(gadm36.uid, -1) AS comm_id FROM (values ''' +
        geometries +
        ''') AS points (p) LEFT JOIN gadm36 ON ST_Intersects(geom, p)''')
        
        if geometries != '':
            # fetch results
            cursor.execute(query)
            results = cursor.fetchall()
        
            for row in results:
                result.append(row['comm_id'])

    except (Exception, psycopg2.Error) as error:
        print("Error while fetching data from PostgreSQL", error)

    finally:
        # closing database connection
        if(connection):
            cursor.close()
            connection.close()
        return result
    
# get geometry's centroid coordinates (latitude, longitude) of a commune
def getGeometryCoordinates(codcom):
    result = None
    try:
        connection = psgConnect(config)
        cursor = connection.cursor(cursor_factory=psycopg2.extras.DictCursor)
        
        # get centroid's geometry
        query = '''
        SELECT ST_Centroid(geom) AS centroid
        FROM public.comm_rg_01m_2016_4326
        WHERE comm_id = %s
        '''
        
        # fetch results
        cursor.execute(query, (codcom,))
        results = cursor.fetchall() 
   
        for row in results:
            # get geometry's coordinates
            result = list(wkb.loads(row['centroid'], hex=True).coords)
            
    except (Exception, psycopg2.Error) as error:
        print("Error while fetching data from PostgreSQL", error)

    finally:
        # closing database connection
        if(connection):
            #cursor.close()
            connection.close()
        # return (latitude, longitude) tuple, reversing result's tuple
        return result[0][::-1]

# build OD matrix from geometries with precision (m) using GADM dataset
# https://stackoverflow.com/questions/56520616/is-it-possible-from-dataframe-transform-to-matrix
# https://gadm.org/download_world.html
def buildOD_geom(x_orig, y_orig, x_dest, y_dest):
    o = getGeometryCodes(y_orig, x_orig)
    d = getGeometryCodes(y_dest, x_dest)
    orig_ref = []
    dest_ref = []
    for i in range(len(o)):
        #if o[i] != '' and d[i] != '':
        if o[i] != -1 and d[i] != -1:
            orig_ref.append(o[i])
            dest_ref.append(d[i])
            
    del(o)
    del(d)
    
    # merge arrays element-wise
    data = mergeArrays(orig_ref, dest_ref)
    
    # build dataframe
    df = pd.DataFrame({'OD': data})
    places = df['OD'].unique()
    places.sort()
    od_df = pd.DataFrame(df['OD'].values.reshape((-1, 2)), columns=['O', 'D'])
    #od_matrix = od_df.groupby(["O", "D"]).size().unstack().reindex(index=places, columns=places)
    od_matrix = pd.pivot_table(od_df, index='O', columns='D', aggfunc='size').reindex(index=places, columns=places)
    od_matrix.fillna(0, downcast='infer', inplace=True)
    
    results = []

    # get the squares' coordinates from MGRS references (columns)
    lat_lon_columns = [str(column) for column in od_matrix.columns]

    # iterate the OD matrix
    for k, v in od_matrix.iterrows():
        # get the square's coordinates from MGRS references (rows)
        lat_lon_rows = str(k)
        data = v.tolist()
        for i in range(len(data)):
            if data[i] > 0:
                results.append({'orig_commune': lat_lon_rows, 
                                'dest_commune': lat_lon_columns[i], 
                                'value': data[i]})
                
    return results


# 2022/04/21 new function to get geometry UID from italy_epgs4326 dataset using zone ID 
#            (i.e., region, province, municipality, ACE, section, POI).
#           Note: 
#               - to select a region, only region code is required
#               - to select a province, region and province codes are required
#               - to select a municipality, region, province, and municipality codes are required
#               - to select a ACE, region, province, municipality, and ACE codes are required
#               - to select a POI, only poi ID is required
def getGeometryCodesItaly(content,direction):
    result = []
    try:
        connection = psgConnect(config)
        cursor = connection.cursor(cursor_factory=psycopg2.extras.DictCursor)
        
        '''
            SELECT idx1, COALESCE(public.italy_epgs4326.uid, -1) AS comm_id
            FROM (VALUES ('EnelX-1','15'), ('EnelX-2','15'), ('EnelX-3','15')) idxs(idx1,idx2)
            LEFT JOIN public.italy_epgs4326 ON idxs.idx1=italy_epgs4326.poi_id AND idxs.idx2=italy_epgs4326.cod_prov
        '''
        keys = [k for k in content.keys()]
        numval = len(content[keys[0]])
        tuples = [('(' + str(n) + ', \'') for n in range(numval)] # n is used as index in the VALUES
        valid = False
        if direction + '_region_id' in keys:
            for i in range(numval):
                tuples[i] = tuples[i] + 'IT_R' + content[direction + '_region_id'][i]
                valid = True
        if direction + '_province_id' in keys:
            for i in range(numval):
                tuples[i] = tuples[i] + '_P' + content[direction + '_province_id'][i]
        if direction + '_municipality_id' in keys:
            for i in range(numval):
                tuples[i] = tuples[i] + '_C' + content[direction + '_municipality_id'][i]
        if direction + '_ACE_id' in keys:
            for i in range(numval):
                tuples[i] = tuples[i] + '_ACE' + content[direction + '_ACE_id'][i]
        if direction + '_section_id' in keys:
            for i in range(numval):
                tuples[i] = tuples[i] + '_S' + content[direction + '_section_id'][i]
        if direction + '_poi_id' in keys:
            for i in range(numval):
                tuples[i] = tuples[i] + 'POI_' + content[direction + '_poi_id'][i]
                valid = True
        for i in range(numval):
            tuples[i] = tuples[i] + '\')'

        query = '''
            SELECT idxs.text_uid, COALESCE(public.italy_epgs4326.uid, -1) AS comm_id 
            FROM (VALUES '''
        for s in tuples:
            query = query + s + ','
        query = query[:-1] + ''') 
            AS idxs(rid, text_uid) 
            LEFT JOIN public.italy_epgs4326 
            ON idxs.text_uid=public.italy_epgs4326.text_uid 
            ORDER BY idxs.rid;'''

        if valid:
            # fetch results
            cursor.execute(query)
            results = cursor.fetchall()
        
            for row in results:
                result.append(row['comm_id'])

    except (Exception, psycopg2.Error) as error:
        print("Error while fetching data from PostgreSQL", error)

    finally:
        # closing database connection
        if(connection):
            cursor.close()
            connection.close()
        return result

# 2022/04/21 new function to build OD matrix from geometries using 
#            the new 'italy_epgs4325' table
def buildOD_geom_italy(content):
    print("DEBUG: in buildOD_geom_italy")
    

    o = getGeometryCodesItaly(content,'orig')
    d = getGeometryCodesItaly(content,'dest')    

    results = []
    for i in range(len(o)):
        results.append({'orig_commune': o[i], 
                        'dest_commune': d[i], 
                        'value': 1})
    # print(results)
    return results


# build OD matrix from geometries with precision (m)
# https://stackoverflow.com/questions/56520616/is-it-possible-from-dataframe-transform-to-matrix
# return OD matrix with origin and destination centroids
def buildOD_geom_old(x_orig, y_orig, x_dest, y_dest):
    orig_ref = [getGeometryCode(y_orig[i], x_orig[i]) for i in range(len(x_orig))]
    dest_ref = [getGeometryCode(y_dest[i], x_dest[i]) for i in range(len(x_dest))]
    
    # merge arrays element-wise
    data = mergeArrays(orig_ref, dest_ref)
    
    # build dataframe
    df = pd.DataFrame({'OD': data})
    places = df['OD'].unique()
    places.sort()
    od_df = pd.DataFrame(df['OD'].values.reshape((-1, 2)), columns=['O', 'D'])
    #od_matrix = od_df.groupby(["O", "D"]).size().unstack().reindex(index=places, columns=places)
    od_matrix = pd.pivot_table(od_df, index='O', columns='D', aggfunc='size').reindex(index=places, columns=places)
    od_matrix.fillna(0, downcast='infer', inplace=True)
    
    results = []

    # get the squares' coordinates from MGRS references (columns)
    lat_lon_columns = [getGeometryCoordinates(str(column)) for column in od_matrix.columns]

    # iterate the OD matrix
    for k, v in od_matrix.iterrows():
        # get the square's coordinates from MGRS references (rows)
        lat_lon_rows = getGeometryCoordinates(str(k))
        data = v.tolist()
        for i in range(len(data)):
            if data[i] > 0:
                results.append({'x_orig': lat_lon_rows[1], 
                                'y_orig': lat_lon_rows[0], 
                                'x_dest': lat_lon_columns[i][1],
                                'y_dest': lat_lon_columns[i][0],
                                'value': data[i]})
                
    return results


# check authentication before each request
@app.before_request
def before_request():
    #auth
    token, message, status = basic_auth(config, request)
    if status == None or status != 200:
        return {'message':message, 'status':status}, status


class OD(Resource):
    def post(self):
        #x_orig = request.form.getlist('x_orig[]')
        #y_orig = request.form.getlist('y_orig[]')
        #x_dest = request.form.getlist('x_dest[]')
        #y_dest = request.form.getlist('y_dest[]')
        
        # parse arguments
        #args = parser.parse_args()
        
        #x_orig = json.loads(args['x_orig']) #json.loads(request.args.get('x_orig'))
        #y_orig = json.loads(args['y_orig']) #json.loads(request.args.get('y_orig'))
        #x_dest = json.loads(args['x_dest']) #json.loads(request.args.get('x_dest'))
        #y_dest = json.loads(args['y_dest']) #json.loads(request.args.get('y_dest'))
        #precision = int(args['']) #int(request.args.get('precision'))
        
        #data = json.loads(args['data'])
        
        content = request.get_json()
        
        if ('x_orig' in content and
            'y_orig' in content and
            'x_dest' in content and
            'y_dest' in content and
            'precision' in content and
            getMGRSprecision(content['precision']) >= 0 and
            getMGRSprecision(content['precision']) <= 5):
            return buildOD(content['x_orig'], 
                           content['y_orig'], 
                           content['x_dest'], 
                           content['y_dest'], 
                           content['precision'])

class ODCompressed(Resource):
    def post(self):
        # decompress data
        content = np.load(io.BytesIO(request.get_data()))
        
        if ('precision' in content and
            getMGRSprecision(content['precision']) >= 0 and
            getMGRSprecision(content['precision']) <= 5):                    
            return buildOD(content['x_orig'], 
                           content['y_orig'], 
                           content['x_dest'], 
                           content['y_dest'], 
                           content['precision'])

# 2022/04/21 modified to accept request that will use the new table 'italy_epgs4326'.
#            To maintain retrocompatibility, if content contains 'x_orig', 'y_orig',
#            'x_dest', and 'y_dest', the previous function 'buildOD_geom' (working with
#            gadm36 table) is called, otherwise the new function 'buildOD_geom_italy'
#            is called. Note that the new function do not use 'x_orig', etc. 
class ODCompressedCommunes(Resource):
    def post(self):
        print('[buildcommunes]::Processing POST request')
        # decompress data
        content = np.load(io.BytesIO(request.get_data()))
        # print(content)
        keys = [k for k in content.keys()]
        # print(keys)

        if(
            'x_orig' in keys and 'y_orig' in keys and
            'x_dest' in keys and 'y_dest' in keys
        ):
            return buildOD_geom(content['x_orig'], 
                                content['y_orig'], 
                                content['x_dest'], 
                                content['y_dest'])
        else:
            if(isinstance(content[keys[0]], (bytes, bytearray))):
                c2 = dict.fromkeys(keys, [])
                for k in keys:
                    tmp = ast.literal_eval(content[k].decode())
                    c2[k] = tmp
                content = c2
            return buildOD_geom_italy(content)
            
api.add_resource(OD, '/build')
api.add_resource(ODCompressed, '/buildcompressed')
api.add_resource(ODCompressedCommunes, '/buildcommunes')

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
    # print(config)
    print("[BUILD-OD API] Accepting connections on port 3000")
    serve(app, host='0.0.0.0', port=3000)
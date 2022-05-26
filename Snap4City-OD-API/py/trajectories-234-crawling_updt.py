#!/usr/bin/env python
# coding: utf-8

# In[ ]:


from IPython.core.display import display, HTML
display(HTML("<style>.container { width:100% !important; }</style>"))


# In[ ]:


# save sensors data to csv file
import itertools
import mysql.connector
import pandas as pd

date_from = "2022-03-01";
date_to = "2022-03-03";

# https://stackoverflow.com/questions/61306931/retrieve-large-data-from-mysql-db-with-chunks-and-save-them-dataframe-pandas
def getData(chunksize):
    # setup MySQL connection
    cnx = mysql.connector.connect(user='root', password='kodekode',
                                  host='192.168.0.234',
                                  database='sensors')
    df = None
    
    count = 0
    
    for offset in itertools.count(step=chunksize):
    #for offset in range(0, 1000, chunksize):
        #print('Reading chunk %d' % offset)
    #    query = 'SELECT * FROM user_eval WHERE latitude != 0 ORDER BY user_eval_id LIMIT %d OFFSET %d' % (chunksize, offset)
        query = 'SELECT * FROM user_eval WHERE latitude != 0 AND (date <= "%s" AND date >= "%s") ORDER BY user_eval_id LIMIT %d OFFSET %d' % (date_to, date_from, chunksize, offset)
        
        chunk_df = pd.read_sql(query, cnx)
        if len(chunk_df) == 0:
            # no data in new chunk, so we probably have it all
            break
        else:
            chunk_df.to_csv('sensors.csv', mode='a', header=False)
        
        count += len(chunk_df)
        
        print(count)

    # close MySQL connection
    cnx.close()

getData(100000)

headerList = ['id', 'user_eval_id', 'date', 'device_id', 'latitude', 'longitude', 'cc_x', 'cc_y', 'speed', 'altitude', 'provider', 'accuracy', 'heading', 'lin_acc_x', 'lin_acc_y', 'lin_acc_z', 'avg_lin_acc_magn', 'avg_speed', 'lat_pre_scan', 'long_pre_scan', 'date_pre_scan', 'prev_status', 'curr_status', 'curr_status_new', 'curr_status_time_new', 'lat_centroid', 'lon_centroid', 'last_status_row', 'appID', 'version', 'lang', 'uid2', 'profile', 'insert_datetime']
# read contents of csv file
df = pd.read_csv("sensors.csv", names=headerList)
df.to_csv("sensors_h.csv", index=False)
# converting data frame to csv
# df.to_csv("sensors_h.csv", header=headerList, index=False)

# In[ ]:


# convert csv dataset to HDF5 by generating intermediate HDF5 files and merging them
import vaex

df = vaex.from_csv('sensors_h.csv',
                   convert=True,
                   chunk_size=5_000_000)

stop_flag = 1
df.export_hdf5('out.csv.hdf5')

# In[ ]:


# open dataset from HDF5 file
# import vaex

df = vaex.open('out.csv.hdf5')


# In[ ]:


# prepare vaex dataframe
import numpy as np
import pickle
from datetime import datetime

def convert_to_datetime(date_string):
    return np.datetime64(datetime.strptime(str(date_string), "%Y-%m-%d %H:%M:%S"))

# drop nan values in datetime
df = df.dropna(column_names=['date'])

# create a datetime column from date
#df['datetime'] = df.date.astype('datetime64[ns]')
#df['datetime'] = df.date.astype('datetime64[Y]')
df['datetime']  = df.date.apply(convert_to_datetime)

# sort the dataframe by datetime 
# https://github.com/vaexio/vaex/pull/463
#df = df.sort(by=['device_id', 'datetime'])
df = df.sort(by=['datetime'])

# save sorted dataframe to file
#df.export_hdf5('sensors_sorted.hdf5')

# check if column is of type datetime
#df.data_type('datetime').is_datetime

# drop nan values in datetime
#df = df.dropna(column_names=['datetime'])

# group by datetime
# https://github.com/vaexio/vaex/issues/408
#group_per_day = df.groupby(by=vaex.BinnerTime(df.datetime, resolution='D'), 
#                           agg={'count': 'count', 'mean_x': vaex.agg.mean(df.user_eval_id)})

# convert vaex dataframe to pandas dataframe
#df_pandas = df.to_pandas_df(list(df.columns))

# create a datetime column
#df_pandas['datetime']  = df_pandas.date.apply(convert_to_datetime)

# sort the dataframe by device_id, datetime
#df_pandas = df_pandas.sort_values(['device_id', 'datetime'], ascending = (True, True))

# save dataframe to file
#with open('df_pandas.pck', 'wb') as fp:
#    pickle.dump(df_pandas, fp)
    
# load dataframe from file
#with open('df_pandas.pck', 'rb') as fp:
#    df_pandas = pickle.load(fp)

# In[ ]:


# save sorted dataframe to file
# df.export_hdf5('sensors_sorted.hdf5')


# In[ ]:


# open sorted dataset from HDF5 file
import vaex

# df = vaex.open('sensors_sorted.hdf5')


# In[ ]:
print(repr(df.datetime))

# build MGRS OD flows
import requests
import json
import itertools
import io
import datetime
import sys
import numpy as np

# prepare OD data to be inserted
def buildData(organization, precision, value_type, value_unit, description, kind, mode, transport, purpose, date_s):
    od = {}
    od['od_id'] = 'od_' + organization + '_' + str(precision)
    od['from_date'] = date_s + ' 00:00:00'
    od['to_date'] = date_s + ' 23:59:59'
    od['organization'] = organization
    od['precision'] = precision
    od['value_type'] = value_type
    od['value_unit'] = value_unit
    od['description'] = description
    od['kind'] = kind
    od['mode'] = mode
    od['transport'] = transport
    od['purpose'] = purpose
    
    return od  

organization = 'Tuscany'
value_type = 'count'
value_unit = '#'
description = 'people count'
kind = 'demand' #offer
mode = 'any' #pedestrian, vehicle, bike
transport = 'both' #pub, priv, both
purpose = 'both' #tourism, commuter, both
precisions = [10000] # 100 m, 1 km, 10 km, (100 km not working since refers to 5 digits MGRS id)
a = []
for precision in precisions:
    x_orig = []
    y_orig = []
    x_dest = []
    y_dest = []
    od_data = None
    prev_date = None
    user_data = {}
    for i, row in df.iterrows(): #df[6680000:]
        #print(row['datetime'].date(), datetime.date(2017, 12, 18))
        #if row['datetime'].date() < datetime.date(2017, 12, 18):
        #    continue
        if (prev_date is not None and
            (row['datetime'].year != prev_date.year or 
            row['datetime'].month != prev_date.month or 
            row['datetime'].day != prev_date.day)):
            # build OD data
            for device_id in user_data:
                # build all combinations of coordinates from start to end
                # https://stackoverflow.com/questions/104420/how-to-generate-all-permutations-of-a-list
                # e.g. [11.10, 11.11, 11.12] => [(11.1, 11.11), (11.1, 11.12), (11.11, 11.12)]
                x_combinations = list(itertools.combinations(user_data[device_id]['x'], 2))
                y_combinations = list(itertools.combinations(user_data[device_id]['y'], 2))
                n = 0
                # append the coordinates (if source and destination are different)
                for j in range(len(x_combinations)):
                    if (x_combinations[j][0] != x_combinations[j][1] or 
                        y_combinations[j][0] != y_combinations[j][1]):
                        n += 1
                        x_orig.append(x_combinations[j][0])
                        x_dest.append(x_combinations[j][1])
                        y_orig.append(y_combinations[j][0])
                        y_dest.append(y_combinations[j][1])
                    
                # empty arrays
                del(x_combinations)
                del(y_combinations)
                
            if len(x_orig) > 0:
                # create the buffer
                buf = io.BytesIO()
                #d = {}
                #d['x_orig'] = x_orig
                #d['y_orig'] = y_orig
                #d['x_dest'] = x_dest
                #d['y_dest'] = y_dest
                #d['precision'] = precision
                
                # pass the buffer as you would an open file object
                np.savez_compressed(buf, 
                                    x_orig=x_orig, 
                                    y_orig=y_orig, 
                                    x_dest=x_dest, 
                                    y_dest=y_dest,
                                    precision=precision)

                # this simulates closing the file and re-opening it
                # otherwise the cursor will already be at the end of the
                # file when flask tries to read the contents, and it will
                # think the file is empty
                buf.seek(0)

                # send the data to the API as compressed binary
                response = requests.post('http://192.168.1.105:3000/buildcompressed', data=buf)
                od_data = json.loads(response.text)
    
                # prepare OD data
                x_o = []
                y_o = []
                x_d = []
                y_d = []
                v = []
                for data in od_data:
                    x_o.append(data['x_orig'])
                    y_o.append(data['y_orig'])
                    x_d.append(data['x_dest'])
                    y_d.append(data['y_dest'])
                    v.append(data['value'])
            
                # insert OD matrix
                date_s = str(prev_date.year) + '-' + str(prev_date.month) + '-' + str(prev_date.day)
                od = buildData(organization, precision, value_type, value_unit, description, kind, mode, transport, purpose, date_s)
                od['x_orig'] = x_o
                od['y_orig'] = y_o
                od['x_dest'] = x_d
                od['y_dest'] = y_d
                od['values'] = v
                response = requests.post('http://192.168.1.105:3100/insert', json=od)
                del(od)
                del(x_o)
                del(y_o)
                del(x_d)
                del(y_d)
                del(v)
                
                print('inserted', date_s, precision)
                
            # empty dictionary and arrays
            user_data = {}
            x_orig = []
            y_orig = []
            x_dest = []
            y_dest = []              
        
        prev_date = row['datetime']
        if row['device_id'] not in user_data:
            user_data[row['device_id']] = {}
            user_data[row['device_id']]['x'] = []
            user_data[row['device_id']]['y'] = []
        user_data[row['device_id']]['x'].append(row['longitude'])
        user_data[row['device_id']]['y'].append(row['latitude'])


# In[ ]:


# build communes OD flows
import requests
import json
import itertools
import io
import datetime
import sys
import numpy as np

# prepare OD data to be inserted
def buildDataCommunes(organization, value_type, value_unit, description, kind, mode, transport, purpose, date_s):
    od = {}
    od['od_id'] = 'od_' + organization + "_communes"
    od['from_date'] = date_s + ' 00:00:00'
    od['to_date'] = date_s + ' 23:59:59'
    od['organization'] = organization
    od['value_type'] = value_type
    od['value_unit'] = value_unit
    od['description'] = description
    od['kind'] = kind
    od['mode'] = mode
    od['transport'] = transport
    od['purpose'] = purpose
    
    return od  

organization = 'Tuscany'
value_type = 'count'
value_unit = '#'
description = 'people count'
kind = 'demand' #offer
mode = 'any' #pedestrian, vehicle, bike
transport = 'both' #pub, priv, both
purpose = 'both' #tourism, commuter, both
a = []
x_orig = []
y_orig = []
x_dest = []
y_dest = []
od_data = None
prev_date = None
user_data = {}
for i, row in df[6817000:].iterrows():
    if (prev_date is not None and
        (row['datetime'].year != prev_date.year or 
        row['datetime'].month != prev_date.month or 
        row['datetime'].day != prev_date.day)):
        # build OD data
        for device_id in user_data:
            # build all combinations of coordinates from start to end
            # https://stackoverflow.com/questions/104420/how-to-generate-all-permutations-of-a-list
            # e.g. [11.10, 11.11, 11.12] => [(11.1, 11.11), (11.1, 11.12), (11.11, 11.12)]
            x_combinations = list(itertools.combinations(user_data[device_id]['x'], 2))
            y_combinations = list(itertools.combinations(user_data[device_id]['y'], 2))
            n = 0
            # append the coordinates (if source and destination are different)
            for j in range(len(x_combinations)):
                if (x_combinations[j][0] != x_combinations[j][1] or 
                    y_combinations[j][0] != y_combinations[j][1]):
                    n += 1
                    x_orig.append(x_combinations[j][0])
                    x_dest.append(x_combinations[j][1])
                    y_orig.append(y_combinations[j][0])
                    y_dest.append(y_combinations[j][1])
            print('prepared arrays for', device_id, len(x_orig), prev_date)
                    
            # empty arrays
            del(x_combinations)
            del(y_combinations)
                
        if len(x_orig) > 0:
            # create the buffer
            buf = io.BytesIO()
            #d = {}
            #d['x_orig'] = x_orig
            #d['y_orig'] = y_orig
            #d['x_dest'] = x_dest
            #d['y_dest'] = y_dest
            #d['precision'] = precision
                
            # pass the buffer as you would an open file object
            np.savez_compressed(buf, 
                                x_orig=x_orig, 
                                y_orig=y_orig, 
                                x_dest=x_dest, 
                                y_dest=y_dest)

            # this simulates closing the file and re-opening it
            # otherwise the cursor will already be at the end of the
            # file when flask tries to read the contents, and it will
            # think the file is empty
            buf.seek(0)

            # send the data to the API as compressed binary
            print('sending data to API (build)')
            response = requests.post('http://192.168.1.105:3000/buildcommunes', data=buf)
            od_data = json.loads(response.text)
    
            # prepare OD data
            orig_communes = []
            dest_communes = []
            v = []
            for data in od_data:
                orig_communes.append(data['orig_commune'])
                dest_communes.append(data['dest_commune'])
                v.append(data['value'])
            
            # insert OD matrix
            od = None
            if len(orig_communes) > 0:
                date_s = str(prev_date.year) + '-' + str(prev_date.month) + '-' + str(prev_date.day)
                od = buildDataCommunes(organization, value_type, value_unit, description, kind, mode, transport, purpose, date_s)
                od['orig_communes'] = orig_communes
                od['dest_communes'] = dest_communes
                od['values'] = v
                response = requests.post('http://192.168.1.105:3100/insertcommunes', json=od)
                print('inserted data', date_s)
                
            del(od)
            del(orig_communes)
            del(dest_communes)
            del(v)
                
        # empty dictionary and arrays
        user_data = {}
        x_orig = []
        y_orig = []
        x_dest = []
        y_dest = []             
        
    prev_date = row['datetime']
    if row['device_id'] not in user_data:
        user_data[row['device_id']] = {}
        user_data[row['device_id']]['x'] = []
        user_data[row['device_id']]['y'] = []
    user_data[row['device_id']]['x'].append(row['longitude'])
    user_data[row['device_id']]['y'].append(row['latitude'])


# In[ ]:


buf = io.BytesIO()
# pass the buffer as you would an open file object
np.savez_compressed(buf, 
                    x_orig=x_orig, 
                    y_orig=y_orig, 
                    x_dest=x_dest, 
                    y_dest=y_dest,
                    precision=precision)

# this simulates closing the file and re-opening it
# otherwise the cursor will already be at the end of the
# file when flask tries to read the contents, and it will
# think the file is empty
buf.seek(0) 
            
# send the data to the API as compressed binary
response = requests.post('http://192.168.1.105:3000/buildcompressed', data=buf)
json.loads(response.text)                


# In[ ]:


# https://stackoverflow.com/questions/51356287/python-flask-api-post-and-receive-bytearray-and-metadata/51361847
# https://stackoverflow.com/questions/44463768/python3-requests-post-ignoring-filename-when-using-bytesio

n = 1000
d = {}
#d['x_orig'] = list(np.random.uniform(low=11, high=12, size=(n,)))#x_orig
#d['y_orig'] = list(np.random.uniform(low=43, high=44, size=(n,)))#y_orig
#d['x_dest'] = list(np.random.uniform(low=11, high=12, size=(n,)))#x_dest
#d['y_dest'] = list(np.random.uniform(low=43, high=44, size=(n,)))#y_dest

buf = io.BytesIO() #create our buffer

#pass the buffer as you would an open file object
np.savez_compressed(buf, x_orig=list(np.random.uniform(low=11, high=12, size=(n,))), 
                    y_orig=list(np.random.uniform(low=43, high=44, size=(n,))), 
                    x_dest=list(np.random.uniform(low=11, high=12, size=(n,))), 
                    y_dest=list(np.random.uniform(low=43, high=44, size=(n,))),
                    precision=precision)

buf.seek(0) #This simulates closing the file and re-opening it.
            #  Otherwise the cursor will already be at the end of the
            #  file when flask tries to read the contents, and it will
            #  think the file is empty.
            
#print(len(x_orig), len(y_orig), len(x_dest), len(y_dest), device_id, precision)
#response = requests.post('http://192.168.1.105:3000/buildcompressed', files=action, json=d)
#response = requests.post('http://192.168.1.105:3000/buildcompressed', files={'data': buf})
#rawData = io.BytesIO(b'Some data: \x00\x01')
#response = requests.post('http://192.168.1.105:3000/buildcompressed', files={'data': rawData})
#response = requests.post('http://192.168.1.105:3000/buildcompressed', files={'data': buf.getvalue()})
response = requests.post('http://192.168.1.105:3000/buildcompressed', data=buf)#, headers={'Content-Type': 'application/octet-stream'})
json.loads(response.text)


# In[ ]:


import numpy as np
import io

myarray_1 = list(np.random.uniform(low=11, high=12, size=(n,)))#np.arange(10) #dummy data
myarray_2 = np.eye(5)

buf = io.BytesIO() #create our buffer
#pass the buffer as you would an open file object
np.savez_compressed(buf, myarray_1, myarray_2, #etc...
         )

buf.seek(0) #This simulates closing the file and re-opening it.
            #  Otherwise the cursor will already be at the end of the
            #  file when flask tries to read the contents, and it will
            #  think the file is empty.
# print(bio.getbuffer().nbytes, len(myarray_1))
print(io.getbuffer().nbytes, len(myarray_1))
#flask.sendfile(buf)

#client receives buf
npzfile = np.load(buf)
print(npzfile['arr_0']) #default names are given unless you use keywords to name your arrays
print(npzfile['arr_1']) #  such as: np.savez(buf, x = myarray_1, y = myarray_2 ... (see the docs)


# In[ ]:


bio = io.BytesIO()
print(bio.getbuffer().nbytes)
bio.write(b'here is some data')
print(bio.getbuffer().nbytes)


# In[ ]:


x_combinations = list(itertools.combinations([11.10, 11.11, 11.12], 2))


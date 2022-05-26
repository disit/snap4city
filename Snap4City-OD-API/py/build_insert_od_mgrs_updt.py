import numpy as np
import vaex
import datetime

# open sorted dataset from HDF5 file
# df = vaex.open('sensors_sorted.hdf5')
df = vaex.open('sensors_batch.hdf5')
dates_ko = ['2017-12-19', '2018-06-15', '2018-07-04', '2018-07-25', '2019-01-07', '2019-09-15', '2019-09-19',
            '2019-10-07', '2019-10-28', '2019-11-08']
'''
df1 = vaex.open('sensors_sorted.hdf5')
df = df1[df1.datetime >= np.datetime64('2018-02-04')]
df[['id', 'user_eval_id', 'date', 'device_id', 'latitude', 'longitude', 'cc_x', 'cc_y', 'speed', 'altitude', 'provider', 'accuracy', 'heading', 'lin_acc_x', 'lin_acc_y', 'lin_acc_z', 'avg_lin_acc_magn', 'avg_speed', 'lat_pre_scan', 'long_pre_scan', 'date_pre_scan', 'prev_status', 'curr_status', 'curr_status_new', 'curr_status_time_new', 'lat_centroid', 'lon_centroid', 'last_status_row', 'appID', 'version', 'lang', 'uid2', 'profile', 'insert_datetime', 'datetime']]
'''
print(repr(df.datetime))

# build MGRS OD flows
import requests
import json
import itertools
import io
import sys


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
kind = 'demand'  # offer
mode = 'any'  # pedestrian, vehicle, bike
transport = 'both'  # pub, priv, both
purpose = 'both'  # tourism, commuter, both
# precisions = [1000, 10000, 100000]  # 100 m, 1 km, 10 km, (100 km not working since refers to 5 digits MGRS id)
# precisions = [1000, 5000, 10000, 50000]
precisions = [1000, 10000]
a = []
for precision in precisions:
    x_orig = []
    y_orig = []
    x_dest = []
    y_dest = []
    od_data = None
    prev_date = None
    user_data = {}
    for i, row in df.iterrows():  # df[6680000:]
        # print(row['datetime'].date(), datetime.date(2017, 12, 18))
        # if row['datetime'].date() < datetime.date(2017, 12, 18):
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
                del (x_combinations)
                del (y_combinations)
            if len(x_orig) > 0:
                # create the buffer
                buf = io.BytesIO()
                # d = {}
                # d['x_orig'] = x_orig
                # d['y_orig'] = y_orig
                # d['x_dest'] = x_dest
                # d['y_dest'] = y_dest
                # d['precision'] = precision
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
                print('Build...')
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
                if date_s not in dates_ko:
                    od = buildData(organization, precision, value_type, value_unit, description, kind, mode, transport,
                                   purpose, date_s)
                    od['x_orig'] = x_o
                    od['y_orig'] = y_o
                    od['x_dest'] = x_d
                    od['y_dest'] = y_d
                    od['values'] = v
                    print('Insert...')
                    response = requests.post('http://192.168.1.105:3100/insert', json=od)
                    del (od)
                    del (x_o)
                    del (y_o)
                    del (x_d)
                    del (y_d)
                    del (v)
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

        if i == (df.__len__() - 1):
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
                del (x_combinations)
                del (y_combinations)
            if len(x_orig) > 0:
                # create the buffer
                buf = io.BytesIO()
                # d = {}
                # d['x_orig'] = x_orig
                # d['y_orig'] = y_orig
                # d['x_dest'] = x_dest
                # d['y_dest'] = y_dest
                # d['precision'] = precision
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
                print('Build...')
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
                if date_s not in dates_ko:
                    od = buildData(organization, precision, value_type, value_unit, description, kind, mode, transport,
                                   purpose, date_s)
                    od['x_orig'] = x_o
                    od['y_orig'] = y_o
                    od['x_dest'] = x_d
                    od['y_dest'] = y_d
                    od['values'] = v
                    print('Insert...')
                    response = requests.post('http://192.168.1.105:3100/insert', json=od)
                    del (od)
                    del (x_o)
                    del (y_o)
                    del (x_d)
                    del (y_d)
                    del (v)
                    print('inserted', date_s, precision)

            # empty dictionary and arrays
            user_data = {}
            x_orig = []
            y_orig = []
            x_dest = []
            y_dest = []

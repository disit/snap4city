#!/usr/bin/env python
# coding: utf-8

# In[181]:


from datetime import timezone
import datetime
import math
import json
import requests
import pandas as pd

# get unix timestamp UTC (ms) from local datetime
def getTimestampUTC(date):
    dt = datetime.datetime.strptime(date, '%Y-%m-%d %H:%M:%S')
    return dt.replace(tzinfo=timezone.utc).timestamp() * 1000

# get local datetime from unix timestamp UTC (ms)
def getDateTime(timestamp):
    return str(datetime.datetime.fromtimestamp(timestamp))

# get the access token
def getAccessToken(conf):
    data = {
        'grant_type': conf["AccessTokenGrantType"],
        'client_id': conf["AccessTokenClientID"],
        'username': conf["AccessTokenUsername"],
        'password': conf["AccessTokenPassword"]
    }
    r = requests.post(
        conf["AccessTokenUrl"],
        data=data)
    token = json.loads(r.text)
    token = token["access_token"]
    return token

def getUrlResponse(url, accessToken):
    header = {'Authorization': 'Bearer ' + accessToken}
    response = requests.get(url, headers=header)
    return response.text

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
    
# get flows with Smart City API
def getFlows(conf, from_date, to_date, value_type, value_unit, 
             description, kind, mode, transport, purpose):
    data = []
    start = int(getTimestampUTC(from_date))
    end = int(getTimestampUTC(to_date))

    token = getAccessToken(conf)

    # get the kpi data
    url = 'https://www.snap4city.org/mypersonaldata/api/v1/kpidata?sourceRequest=test&searchKey=s4c'
    results = getUrlResponse(url, token)
    results = json.loads(results)
    for r in results:
        url = 'https://www.snap4city.org/mypersonaldata/api/v1/kpidata/%s/values?sourceRequest=test&from=%s&to=%s' % (r['id'], start, end)
        sub_results = getUrlResponse(url, token)
        sub_results = json.loads(sub_results)
        if len(sub_results) > 0:
            # build dataFrame from results
            dataFrame = pd.DataFrame(sub_results)
            # sort dataTime (ascending)
            dataFrame = dataFrame.sort_values(by=['dataTime'], ascending=True)
            # create a date column (datetime) from dataTime (timestamp)
            dataFrame['date'] = pd.to_datetime(dataFrame.dataTime, utc=True, unit='ms')
            # convert date column to local datetime
            dataFrame['date'] = dataFrame['date'].dt.tz_convert('Europe/Rome')

            # split dataframe into hourly dataframes
            # https://stackoverflow.com/questions/39609391/pandas-how-to-split-dataframe-by-column-by-interval
            for j, g in dataFrame.groupby([pd.to_datetime(dataFrame.date).dt.hour]):
                df = g.reset_index(drop=True)
                # append OD flows to arrays
                if len(df) > 1 and 'latitude' in df.columns and 'longitude' in df.columns:
                    d = {}
                    #d['from_date'] = getDateTime(df.iloc[i]['dataTime']/1000).split(':')[0] + ':00:00'
                    #d['to_date'] = getDateTime(df.iloc[i]['dataTime']/1000).split(':')[0] + ':59:59'
                    d['from_date'] = str(df.iloc[0]['date']).split('+')[0].split(':')[0] + ':00:00'
                    d['to_date'] = str(df.iloc[0]['date']).split('+')[0].split(':')[0] + ':59:59'
                    d['value_type'] = value_type
                    d['value_unit'] = value_unit
                    d['uid'] = str(r['id'])
                    d['description'] = description
                    d['organization'] = r['organizations'].split(',')[0].split('=')[1]
                    d['kind'] = kind
                    d['mode'] = mode
                    d['transport'] = transport
                    d['purpose'] = purpose
                    x_orig = []
                    y_orig = []
                    x_dest = []
                    y_dest = []
                    for i in range(len(df)-1):
                        if (is_number(str(df.iloc[i]['longitude'])) and 
                            is_number(str(df.iloc[i]['latitude'])) and
                            is_number(str(df.iloc[i+1]['longitude'])) and 
                            is_number(str(df.iloc[i+1]['latitude']))):
                            x_orig.append(float(df.iloc[i]['longitude']))
                            y_orig.append(float(df.iloc[i]['latitude']))
                            x_dest.append(float(df.iloc[i+1]['longitude']))
                            y_dest.append(float(df.iloc[i+1]['latitude']))
                    d['x_orig'] = x_orig
                    d['y_orig'] = y_orig
                    d['x_dest'] = x_dest
                    d['y_dest'] = y_dest
                    if len(x_orig) > 0:
                        data.append(d)
                
    return data

# create date range
#s = datetime.datetime.today()
#s = '2019-03-15 00:00:00'
#s = datetime.datetime.strptime(s, '%Y-%m-%d %H:%M:%S')
#numdays = 1
#date_list = [str(s + datetime.timedelta(days=x)) for x in range(numdays)]
# date_list = pd.date_range('2019-03-15', '2021-04-22').tolist()
# date_list = pd.date_range('2019-04-19', '2021-05-09').tolist()    # PREV
date_list = pd.date_range('2018-04-01', '2018-04-30').tolist()  # Modified GP

precisions = [1000, 10000] # 100 m, 1 km, 10 km, (100 km not working since refers to 5 digits MGRS id)
value_type = 'count'
value_unit = '#'
description = 'people count'
kind = 'demand' #offer
mode = 'any' #pedestrian, vehicle, bike
transport = 'both' #pub, priv, both
purpose = 'both' #tourism, commuter, both

# get the access token
with open('config.json', 'r') as file:
    #conf = file.read().replace('\n', '')
    conf = json.loads(file.read())
        
for fromDate in date_list:
    print(str(fromDate))
    # get from and to dates in utc timestamps (ms)
    #fromDate = str(getTimestampUTC('2021-04-01 00:00:00'))
    #toDate = str(getTimestampUTC('2021-04-06 23:59:59'))
    toDate = str(fromDate).split(' ')[0] + ' 23:59:59'
    
    data = getFlows(conf, str(fromDate), toDate, value_type, value_unit, description, kind, mode, transport, purpose)

    # insert data into database with API
    for precision in precisions:
        orig_dest_dict = {}
        od_data = None
        d = None
        for i in range(len(data)):
            # set precision
            d = data[i].copy()
            d['precision'] = precision
            # build OD matrix
            print("Call od-build API")
            response = requests.post('http://192.168.1.105:3000/build', json=d)
            od_data = json.loads(response.text)
            #print(od_data)
            #print(data[i]['from_date'], data[i]['to_date'])
            #print('')
            
            # cumulate results
            for od_d in od_data:
                xorig = str(od_d['x_orig'])
                yorig = str(od_d['y_orig'])
                xdest = str(od_d['x_dest'])
                ydest = str(od_d['y_dest'])
                from_date = data[i]['from_date']
                organization = data[i]['organization']
                                    
                if organization not in orig_dest_dict:
                    orig_dest_dict[organization] = {}
                if from_date not in orig_dest_dict[organization]:
                    orig_dest_dict[organization][from_date] = {}
                if xorig not in orig_dest_dict[organization][from_date]:
                    orig_dest_dict[organization][from_date][xorig] = {}
                if yorig not in orig_dest_dict[organization][from_date][xorig]:
                    orig_dest_dict[organization][from_date][xorig][yorig] = {}
                if xdest not in orig_dest_dict[organization][from_date][xorig][yorig]:
                    orig_dest_dict[organization][from_date][xorig][yorig][xdest] = {}
                if ydest not in orig_dest_dict[organization][from_date][xorig][yorig][xdest]:
                    orig_dest_dict[organization][from_date][xorig][yorig][xdest][ydest] = 0
                    
                orig_dest_dict[organization][from_date][xorig][yorig][xdest][ydest] += od_d['value']
               
        # build data array
        for organization, v1 in orig_dest_dict.items():
            for date_s, v2 in v1.items():
                # prepare the data
                x_orig_array = []
                y_orig_array = []
                x_dest_array = []
                y_dest_array = []
                values_array = []
                od_id_array = []
                organization_array = []
                od = {}
                #od['od_id'] = data[i]['uid']
                od['od_id'] = 'mobile_' + organization + '_' + str(precision)
                od['from_date'] = date_s
                od['to_date'] = date_s.split('+')[0].split(':')[0] + ':59:59'
                od['organization'] = organization
                od['precision'] = precision
                od['value_type'] = value_type
                od['value_unit'] = value_unit
                od['description'] = description
                od['kind'] = kind
                od['mode'] = mode
                od['transport'] = transport
                od['purpose'] = purpose
            
                for xorig, v3 in v2.items():
                    for yorig, v4 in v3.items():
                        for xdest, v5 in v4.items():
                            for ydest, value in v5.items():  
                                x_orig_array.append(xorig)
                                y_orig_array.append(yorig)
                                x_dest_array.append(xdest)
                                y_dest_array.append(ydest)
                                values_array.append(value)                         

                # insert OD matrix
                od['x_orig'] = x_orig_array
                od['y_orig'] = y_orig_array
                od['x_dest'] = x_dest_array
                od['y_dest'] = y_dest_array
                od['values'] = values_array
                response = requests.post('http://192.168.1.105:3100/insert', json=od)
                del(od)
                
        del(od_data)
        del(d)


# In[129]:

'''
value_type = 'count'
value_unit = '#'
description = 'people count'
kind = 'demand' #offer
mode = 'any' #pedestrian, vehicle, bike
transport = 'both' #pub, priv, both
purpose = 'both' #tourism, commuter, both
from_date = '2019-04-19'
to_date = '2019-04-19'
data = getFlows(conf, str(fromDate), toDate, value_type, value_unit, description, kind, mode, transport, purpose)


# In[ ]:


import json

# S4CHelsinkiTrackerLocation
# S4CAntwerpTrackerLocation
# S4CTuscanyTrackerLocation
# SiiMTuscanyTrackerLocation
# S4CCovid19TrackerLocation

with open('config.json', 'r') as file:
    #conf = file.read().replace('\n', '')
    conf = json.loads(file.read())


# In[3]:


data = {"x_orig":[1,2], "y_orig": [2,3], "x_dest": [3,4], "y_dest": [4,5], "precision": 100}
response = requests.post('http://192.168.1.105:3000/build', json=data)
response.text


# In[93]:


data = {'x_orig': [-122.08457807999915, -122.08457807999915, -122.08457807999915, -122.04613559838288, -122.04613559838288, 1.9984335825148123, 11.248329278032418, 11.248329278032418, 11.248363019578681, 11.248363019578681, 11.248396762689925, 11.248396762689925, 11.249537431666289, 11.249537431666289, 11.249638712103797, 11.249672475381368, 11.249672475381368, 11.249706240224954, 11.249706240224954, 11.249706240224954, 11.249706240224954, 11.25077932371587, 11.250948225276117, 11.250948225276117, 11.2509820102887, 11.2509820102887, 11.252257817559023, 11.252393053973078, 11.252393053973078, 11.252393053973078, 11.252426866997233, 11.252426866997233, 11.252426866997233, 11.252426866997233, 11.252426866997233, 11.252426866997233, 11.252426866997233, 11.252426866997233, 11.252426866997233, 11.252426866997233, 11.252426866997233, 11.252426866997233, 11.25246068158982, 11.25246068158982, 11.25246068158982, 11.25246068158982, 11.25246068158982, 11.252494497750924, 11.252494497750924, 11.252528315480637, 11.25326310311459, 11.253601317455887, 11.253601317455887, 11.25363514751873, 11.25363514751873, 11.25363514751873, 11.253668979150781, 11.253668979150781, 11.253668979150781, 11.253668979150781, 11.253668979150781, 11.253668979150781, 11.253668979150781, 11.253668979150781, 11.253702812352124, 11.253702812352124, 11.253702812352124, 11.253702812352124, 11.253736647122844, 11.254944941545046, 11.254944941545046, 11.25954034045738, 11.25954034045738, 11.272894372620728, 11.275275430361097, 11.276482953109397, 11.276517103412598, 11.278863704365259, 11.281244240537806, 11.28490030389475, 11.2873832885235, 11.301005082438051, 11.309729863036253, 11.315937152706228, 11.317178605812336, 11.320937761932042, 11.322179227164915, 11.439522825005838, 11.45927459893968, 11.461495316774888, 11.462072441130774, 11.46312720042775, 11.46345811839543, 11.463678806998633, 11.46673222908593, 11.469098553356345, 11.471464664962156, 11.472629195957364, 11.472629195957364, 11.472666086526278, 11.476233189353614, 11.7494576743548, 11.7494576743548, 11.7494576743548, 11.480963987973311, 11.486894832587971, 11.494025071040078, 11.502316648760564, 11.511843149218512, 11.518929858409477, 11.523690851924282, 11.526052272418116, 11.526052272418116, 11.52608988584508, 11.52965049782023, 11.534372162971852, 11.541528716761514, 11.548607912310988, 11.554562784795209, 11.55804388001838, 11.560364269781019, 11.562760596149115, 11.563882462367095, 11.563882462367095, 11.563920582469096, 11.563920582469096, 11.56395870433406, 11.56395870433406, 11.565118637422994, 11.566240351013674, 11.568750770457152, 11.569986901795511, 11.571184806587196, 11.572420916063566, 11.576090937560798, 11.580996895364544, 11.58462822568842, 11.589533790960923, 11.591928722167367, 11.594323506818697, 11.596640970284035, 11.596640970284035, 11.596640970284035, 11.597915409037856, 24.938262775754882, 24.938262775754882, 24.945641751498954, 24.94750009603803, 24.947444049045146, 24.947444049045146, 24.947444049045146, 24.947444049045146, 24.947444049045146, 24.947444049045146, 24.947444049045146, 24.947444049045146, 24.947387998489727, 24.947387998489727, 24.94924635015387, 24.94924635015387, 24.949190348730987, 24.949190348730987, 24.998348536483938, 24.998348536483938, 27.0, 27.0, 27.0], 
        'y_orig': [37.42126898438473, 37.42126898438473, 37.42126898438473, 37.33353386126535, 37.33353386126535, 60.569896930539684, 43.78814707119837, 43.78814707119837, 43.78904676120772, 43.78904676120772, 43.78994645105413, 43.78994645105413, 43.787222931925456, 43.787222931925456, 43.789921999663335, 43.79082168858336, 43.79082168858336, 43.791721377340366, 43.791721377340366, 43.791721377340366, 43.791721377340366, 43.78719846934131, 43.791696910937006, 43.791696910937006, 43.792596598767155, 43.792596598767155, 43.79347180501663, 43.797070550997425, 43.797070550997425, 43.797070550997425, 43.79797023708503, 43.79797023708503, 43.79797023708503, 43.79797023708503, 43.79797023708503, 43.79797023708503, 43.79797023708503, 43.79797023708503, 43.79797023708503, 43.79797023708503, 43.79797023708503, 43.79797023708503, 43.7988699230096, 43.7988699230096, 43.7988699230096, 43.7988699230096, 43.7988699230096, 43.79976960877113, 43.79976960877113, 43.80066929436963, 43.787149503722546, 43.79614636754828, 43.79614636754828, 43.79704605303408, 43.79704605303408, 43.79704605303408, 43.797945738356795, 43.797945738356795, 43.797945738356795, 43.797945738356795, 43.797945738356795, 43.797945738356795, 43.797945738356795, 43.797945738356795, 43.798845423516454, 43.798845423516454, 43.798845423516454, 43.798845423516454, 43.799745108513065, 43.79882091053444, 43.79882091053444, 43.788826220127184, 43.788826220127184, 43.78045810884233, 43.77770964438472, 43.77678523207519, 43.777684906381694, 43.77403669791765, 43.771288117662486, 43.769414337086445, 43.76936464591718, 43.768190720850534, 43.768915000097266, 43.76878932375582, 43.76876414806824, 43.76958818770348, 43.769562957336525, 43.69148725942401, 43.65865069493947, 43.6523003114374, 43.63608127344132, 43.631556697712625, 43.63965287331644, 43.64505031620796, 43.62877781292106, 43.62602559099422, 43.62327332398564, 43.62144738811478, 43.62144738811478, 43.62234695900372, 43.61866822382496, 43.24956496319167, 43.24956496319167, 43.24956496319167, 43.613163372110115, 43.60673183609513, 43.59937344183517, 43.590188424492325, 43.580975655713786, 43.57271627514095, 43.56810934187842, 43.565356034673734, 43.565356034673734, 43.56625557773783, 43.56257530178501, 43.55706841533621, 43.55060682016595, 43.54234574600862, 43.53681050258032, 43.53133035582612, 43.52767687179196, 43.525822393285765, 43.52309608994414, 43.52309608994414, 43.52399561323057, 43.52399561323057, 43.524895136348185, 43.524895136348185, 43.52306834520262, 43.520342021437806, 43.521185990729705, 43.52115819432084, 43.520230865973915, 43.52020304373414, 43.51921998163951, 43.51820889051724, 43.5163260494061, 43.51531460061894, 43.51345954135009, 43.5116044350257, 43.50795027691113, 43.50795027691113, 43.50795027691113, 43.50882168733152, 60.16883457658139, 60.16883457658139, 60.19499679495099, 60.19412738926569, 60.19502471258456, 60.19502471258456, 60.19502471258456, 60.19502471258456, 60.19502471258456, 60.19502471258456, 60.19502471258456, 60.19502471258456, 60.19592203574962, 60.19592203574962, 60.19505260573321, 60.19505260573321, 60.1959499299061, 60.1959499299061, 60.09969756873113, 60.09969756873113, 59.99989951879679, 59.99989951879679, 60.56916013917843], 
        'x_dest': [-122.08457807999915, 11.253668979150781, 27.0, -122.04613559838288, 11.252426866997233, 27.0, 11.248329278032418, 11.249537431666289, 11.248329278032418, 11.248363019578681, 11.248363019578681, 11.248396762689925, 11.249537431666289, 11.25077932371587, 11.248396762689925, 11.249638712103797, 11.249672475381368, 11.249672475381368, 11.249706240224954, 11.250948225276117, 11.7494576743548, 11.25326310311459, 11.249706240224954, 11.250948225276117, 11.249706240224954, 11.2509820102887, 11.2509820102887, 11.252393053973078, 11.252426866997233, 11.25363514751873, -122.08457807999915, 11.250948225276117, 11.252393053973078, 11.252426866997233, 11.25246068158982, 11.252528315480637, 11.253668979150781, 11.253702812352124, 11.254944941545046, 11.7494576743548, 11.596640970284035, 24.938262775754882, 11.252393053973078, 11.252426866997233, 11.25246068158982, 11.253668979150781, 11.253702812352124, 11.252426866997233, 11.253668979150781, 11.253736647122844, 11.25954034045738, 11.252257817559023, 11.253601317455887, 11.253601317455887, 11.25363514751873, 11.253668979150781, 11.252426866997233, 11.25246068158982, 11.252494497750924, 11.25363514751873, 11.253668979150781, 11.253702812352124, 11.596640970284035, 24.947444049045146, 11.252426866997233, 11.25246068158982, 11.253668979150781, 11.253702812352124, 11.252494497750924, 11.253702812352124, 11.254944941545046, 11.25954034045738, 11.272894372620728, 11.275275430361097, 11.276482953109397, 11.276517103412598, 11.278863704365259, 11.281244240537806, 11.28490030389475, 11.2873832885235, 11.301005082438051, 11.309729863036253, 11.315937152706228, 11.317178605812336, 11.320937761932042, 11.322179227164915, 11.439522825005838, 11.45927459893968, 11.461495316774888, 11.463678806998633, 11.46312720042775, 11.46673222908593, 11.462072441130774, 11.46345811839543, 11.469098553356345, 11.471464664962156, 11.472666086526278, 11.472666086526278, 11.476233189353614, 11.472629195957364, 11.480963987973311, -122.04613559838288, 11.252426866997233, 11.7494576743548, 11.486894832587971, 11.494025071040078, 11.502316648760564, 11.511843149218512, 11.518929858409477, 11.523690851924282, 11.52608988584508, 11.526052272418116, 11.52965049782023, 11.526052272418116, 11.534372162971852, 11.541528716761514, 11.548607912310988, 11.554562784795209, 11.55804388001838, 11.560364269781019, 11.562760596149115, 11.56395870433406, 11.563882462367095, 11.565118637422994, 11.563882462367095, 11.563920582469096, 11.563920582469096, 11.56395870433406, 11.566240351013674, 11.568750770457152, 11.569986901795511, 11.571184806587196, 11.572420916063566, 11.576090937560798, 11.580996895364544, 11.58462822568842, 11.589533790960923, 11.591928722167367, 11.594323506818697, 11.597915409037856, 11.252426866997233, 11.253668979150781, 11.596640970284035, 11.596640970284035, 24.938262775754882, 24.947444049045146, 24.947444049045146, 24.947444049045146, 11.252426866997233, 11.253668979150781, 24.945641751498954, 24.94750009603803, 24.947444049045146, 24.947387998489727, 24.94924635015387, 24.949190348730987, 24.947444049045146, 24.947387998489727, 24.947444049045146, 24.94924635015387, 24.947444049045146, 24.947387998489727, 11.252426866997233, 24.998348536483938, 1.9984335825148123, 27.0, 24.998348536483938], 
        'y_dest': [37.42126898438473, 43.797945738356795, 59.99989951879679, 37.33353386126535, 43.79797023708503, 60.56916013917843, 43.78814707119837, 43.787222931925456, 43.78814707119837, 43.78904676120772, 43.78904676120772, 43.78994645105413, 43.787222931925456, 43.78719846934131, 43.78994645105413, 43.789921999663335, 43.79082168858336, 43.79082168858336, 43.791721377340366, 43.791696910937006, 43.24956496319167, 43.787149503722546, 43.791721377340366, 43.791696910937006, 43.791721377340366, 43.792596598767155, 43.792596598767155, 43.797070550997425, 43.79797023708503, 43.79704605303408, 37.42126898438473, 43.791696910937006, 43.797070550997425, 43.79797023708503, 43.7988699230096, 43.80066929436963, 43.797945738356795, 43.798845423516454, 43.79882091053444, 43.24956496319167, 43.50795027691113, 60.16883457658139, 43.797070550997425, 43.79797023708503, 43.7988699230096, 43.797945738356795, 43.798845423516454, 43.79797023708503, 43.797945738356795, 43.799745108513065, 43.788826220127184, 43.79347180501663, 43.79614636754828, 43.79614636754828, 43.79704605303408, 43.797945738356795, 43.79797023708503, 43.7988699230096, 43.79976960877113, 43.79704605303408, 43.797945738356795, 43.798845423516454, 43.50795027691113, 60.19502471258456, 43.79797023708503, 43.7988699230096, 43.797945738356795, 43.798845423516454, 43.79976960877113, 43.798845423516454, 43.79882091053444, 43.788826220127184, 43.78045810884233, 43.77770964438472, 43.77678523207519, 43.777684906381694, 43.77403669791765, 43.771288117662486, 43.769414337086445, 43.76936464591718, 43.768190720850534, 43.768915000097266, 43.76878932375582, 43.76876414806824, 43.76958818770348, 43.769562957336525, 43.69148725942401, 43.65865069493947, 43.6523003114374, 43.64505031620796, 43.631556697712625, 43.62877781292106, 43.63608127344132, 43.63965287331644, 43.62602559099422, 43.62327332398564, 43.62234695900372, 43.62234695900372, 43.61866822382496, 43.62144738811478, 43.613163372110115, 37.33353386126535, 43.79797023708503, 43.24956496319167, 43.60673183609513, 43.59937344183517, 43.590188424492325, 43.580975655713786, 43.57271627514095, 43.56810934187842, 43.56625557773783, 43.565356034673734, 43.56257530178501, 43.565356034673734, 43.55706841533621, 43.55060682016595, 43.54234574600862, 43.53681050258032, 43.53133035582612, 43.52767687179196, 43.525822393285765, 43.524895136348185, 43.52309608994414, 43.52306834520262, 43.52309608994414, 43.52399561323057, 43.52399561323057, 43.524895136348185, 43.520342021437806, 43.521185990729705, 43.52115819432084, 43.520230865973915, 43.52020304373414, 43.51921998163951, 43.51820889051724, 43.5163260494061, 43.51531460061894, 43.51345954135009, 43.5116044350257, 43.50882168733152, 43.79797023708503, 43.797945738356795, 43.50795027691113, 43.50795027691113, 60.16883457658139, 60.19502471258456, 60.19502471258456, 60.19502471258456, 43.79797023708503, 43.797945738356795, 60.19499679495099, 60.19412738926569, 60.19502471258456, 60.19592203574962, 60.19505260573321, 60.1959499299061, 60.19502471258456, 60.19592203574962, 60.19502471258456, 60.19505260573321, 60.19502471258456, 60.19592203574962, 43.79797023708503, 60.09969756873113, 60.569896930539684, 59.99989951879679, 60.09969756873113], 
        'values': [104, 1, 1, 44, 1, 1, 2, 1, 1, 1, 1, 1, 12, 1, 1, 1, 2, 1, 2, 1, 1, 1, 2, 2, 1, 1, 1, 1, 1, 1, 2, 1, 1, 4395, 32, 1, 42, 10, 1, 3, 1, 1, 1, 31, 83, 4, 2, 1, 1, 1, 1, 1, 1, 1, 2, 1, 45, 3, 1, 1, 201, 3, 1, 1, 9, 3, 4, 29, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 1, 1, 3, 69, 1, 1, 1, 1, 1, 1, 1, 2, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 3, 1, 1, 1, 1, 3, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 1, 14, 1, 56, 1, 2, 1, 1, 1, 2, 1, 1778, 33, 1, 2, 34, 245, 1, 2, 1, 1, 1, 269, 1, 1, 1], 
        'from_date': '2019-03-15 08:00:00', 
        'to_date': '2019-03-15 08:59:59', 
        'value_type': 'count', 
        'value_unit': '#', 
        'od_id': '17055992', 
        'description': 'people count', 
        'organization': 'DISIT', 
        'kind': 'demand', 
        'mode': 'any', 
        'precision': '1',
        'transport': 'both', 
        'purpose': 'both'}
response = requests.post('http://192.168.1.105:3100/insert', json=data)
response.text
'''

# In[78]:


import mgrs
import pyproj
    
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
 
# precision = mgrs_tile_edge_size
# https://dida.do/blog/understanding-mgrs-coordinates
def getMGRScorners_old(lon, lat, precision):
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
    return southwest, northwest, southeast, northeast, reference

getMGRScorners('11.596640970284035', '43.50795027691113', '1')
#getMGRS_corners('11.596640970284035', '43.50795027691113', '1')
#print(getMGRScorners('-93.00000000000001', '41.999997975128', '100'))
#print('')
#print(getMGRS_corners('-93.00000000000001', '41.999997975128', '100'))
#m = mgrs.MGRS()
#southeast = m.toLatLon('32TQP990120500')
#print(southeast)


# In[109]:


#32TQP0990020500
#32TQP0990020501
#32TQP990120500
#32TQP990120501

import mgrs
m = mgrs.MGRS()
d = m.toLatLon('32TQP0990120500'.encode())
print(d)
m.toMGRS(59.49062559948277, 23.46739795948797, MGRSPrecision=1)


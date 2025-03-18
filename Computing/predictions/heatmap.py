''' Snap4city Computing HEATMAP.
   Copyright (C) 2024 DISIT Lab http://www.disit.org - University of Florence

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
'''

import json
import time
import os
from datetime import datetime, timedelta
from urllib.parse import quote

import geopandas as gpd
import numpy as np
import pandas as pd
import pyproj
import requests
from flask import jsonify, make_response
from geopy.distance import geodesic
from pyproj import Proj, Transformer
from scipy.interpolate import griddata
from shapely.geometry import Point

from helper import write_log, parse_from_date

pd.set_option('future.no_silent_downcasting', True)


def safe_float_conversion(x):
    if isinstance(x, dict) and 'value' in x and x['value'] != '':
        return float(x['value'])
    return np.nan  # Return NaN if the condition is not met


def round_time(time_str):
    time_obj = datetime.strptime(time_str, '%H:%M')
    minute = (time_obj.minute // 10 + 1) * 10
    if minute == 60:
        time_obj += timedelta(hours=1)
        minute = 0
    return time_obj.replace(minute=minute).strftime('%H:%M')


def fetch_sensor_data(service_uri: str, from_date_time: str, to_date_time: str, access_token: str):
    parsed_from_date = parse_from_date(from_date_time, to_date_time)
    if not parsed_from_date:
        parsed_from_date = from_date_time
    base_url = os.getenv("BASE_URL","https://www.snap4city.org")
    api_url = (
        f"{base_url}/superservicemap/api/v1/?serviceUri={service_uri}"
        f"&fromTime={parsed_from_date}&toTime={to_date_time}&accessToken={access_token}"
    )
    header = {
      "Authorization": f"Bearer {access_token}"
    }
    try:
        res = requests.get(api_url,headers=header)
        sensor_data = res.json()

        write_log({
            "url": api_url,
            "response_status": res.status_code
        })

        if 'realtime' in sensor_data and 'results' in sensor_data['realtime']:
            results = sensor_data['realtime']['results']['bindings']

            if len(results) < 0:
                return None

            # Extract sensor information
            sensor_info = sensor_data['Service']['features'][0]
            coordinates = sensor_info['geometry']['coordinates']
            name = sensor_info['properties']['name'] or f"{service_uri}_sensor"

            # Process realtime data
            results = sensor_data['realtime']['results']['bindings']
            df = pd.DataFrame(results)

            if 'measuredTime' not in df.columns:
                return None

            variable_names = [col for col in df.columns if col != 'measuredTime']
            temp_df = df.copy()

            # Update variable names
            temp_df = temp_df.rename(columns={var: var for var in variable_names})

            # Convert measuredTime to datetime and adjust time slots
            temp_df['measuredTime'] = temp_df['measuredTime'].apply(lambda x: x['value'] if isinstance(x, dict) and 'value' in x else None)

            temp_df['measuredTime'] = pd.to_datetime(temp_df['measuredTime'], utc=True, errors='coerce')
            temp_df = temp_df.dropna(subset=['measuredTime'])
            temp_df['time'] = temp_df['measuredTime'].dt.strftime('%H:%M')
            temp_df['date'] = temp_df['measuredTime'].dt.strftime('%Y-%m-%d')
            temp_df['day'] = temp_df['measuredTime'].dt.day_name()

            # Round time slots every 10 minutes

            temp_df['time'] = temp_df['time'].apply(round_time)

            # Adjust date and time columns
            temp_df['dateTime'] = temp_df.apply(lambda row: f"{row['date']}T{row['time']}:00", axis=1)
            temp_df = temp_df.sort_values(by='measuredTime')
            temp_df['sensorName'] = name

            return {
                'sensorCoordinates': coordinates,
                'sensorName': name,
                'sensorRealtimeData': temp_df
            }

        return None

    except Exception as e:
        print(f"Error fetching data for ServiceUri: {service_uri}, Error: {e}")
        write_log({ "exception": f"Error fetching data for ServiceUri: {service_uri}, Error: {e}" })

def heatmapIDW(city: str, long_min: float, long_max: float, lat_min: float, lat_max: float, epsg_projection: float, subnature: str, value_types: list,
               from_date_time: str, to_date_time: str, scenario: str, color_map: str, broker: str, token: str, heat_map_model_name: str, clustered: int,
               file: int):
    # ------------------------------------------------

    # Via nodered the valueType parameter is not entered as an array but as a string.
    # Splitting of the elements and creation of the array:

    if isinstance(value_types, str):
        value_types = [vt.strip() for vt in value_types.split(",")]

    heatmap_name = f"{scenario}_" + "_".join(value_types)
    device_name = heatmap_name
    print(heatmap_name)
    print(value_types)

    metric_name = color_map
    sensor_category = subnature

    print("--------- CHECK ON PARAMETERS END ---------")
    print(datetime.now())
    print("------------------------------------------------")

    # -------------------------------------------------------
    # UPLOAD All Service Uris (sensor stations) In the Area
    # -------------------------------------------------------
    print("--------- UPLOAD ALL SENSOR STATIONS IN THE AREA OF INTEREST - START---------")
    print(datetime.now())

    base_url = os.getenv("BASE_URL","https://www.snap4city.org")
    query = (f"{base_url}/superservicemap/api/v1/?selection="
             f"{lat_min};{long_min};{lat_max};{long_max}"
             f"&categories={sensor_category}"
             f"&maxResults=100&maxDists=5&format=json")
    header = {
        "Authorization": f"Bearer {token}",
    }

    response = requests.get(query,headers=header)

    write_log({
        "url": query,
        "token": token,
        "response": response.json(),
        "response_status": response.status_code
    })

    if response.status_code != 200:
        return make_response(jsonify({
            "response": "error",
            "error": "Error fetching data from the service"

        }), response.status_code)

    sensor_category_json = response.json()

    service_uris = [service['properties']['serviceUri'] for service in sensor_category_json.get('Services', {}).get('features', [])]

    write_log({
        "uris": service_uris
    })

    if len(service_uris) == 0:
        return make_response(jsonify({
            "response": "error",
            "error": "No Sensor Station in the selected area. Please, correct the coordinates"

        }), 400)

    print("--------- UPLOAD ALL SENSOR STATIONS IN THE AREA OF INTEREST - END---------")
    print(datetime.now())
    print("------------------------------------------------")

    sensor_data = []

    for service_uri in service_uris:
        data = fetch_sensor_data(service_uri, from_date_time, to_date_time, token)

        if data:
            sensor_data.append(data)

    print("--------- SensorData List Creation Completed ---------")
    print("--------- UPLOAD DATA FOR EACH SENSOR STATION - END ---------")
    print(datetime.now())
    print("------------------------------------------------")

    info_heatmap = {
        "heatmapName": heatmap_name,
        "dateTime": to_date_time,
        "message": []
    }

    print("--------- DATA MANIPULATION -- START ---------")
    print(datetime.now())

    # -------------------------------------------------------------------
    # drop empty suri
    # -------------------------------------------------------------------
    print("--------- 1. Drop empty ServiceUris ---------")

    indices_to_remove = []

    for i, entry in enumerate(sensor_data):
        if entry['sensorRealtimeData'].isnull().all().all():
            indices_to_remove.append(i)

    sensor_data = [entry for i, entry in enumerate(sensor_data) if i not in indices_to_remove]

    if not sensor_data:
        return make_response(jsonify({
            "response": "error",
            "error": f"No Available Data for {value_types}"

        }), 400)

        # -------------------------------------------------------------------
    # CONTROL on the names of airQuality variables
    # -------------------------------------------------------------------

    print("--------- 2. Check on ValueType ---------")
    val_type_index = []
    var_name_list = []
    sensor_data_index = []

    for i, entry in enumerate(sensor_data):
        dat_temp = entry['sensorRealtimeData']

        for value in value_types:
            if value in dat_temp.columns:
                sensor_data_index.append(i)
                val_type_index.append(dat_temp.columns.get_loc(value))  # Get the index 
                var_name_list.append(value)

    names_matrix = pd.DataFrame({
        'sensorDataIndex': sensor_data_index,
        'varNameList': var_name_list,
        'valTypeIndex': val_type_index
    })

    if not var_name_list:
        return make_response(jsonify({
            "response": "error",
            "error": f"The valueType is incorrect. Please, change the parameter and inject the node again."

        }), 400)

    print("--------- 3. Average Values Matrix creation and null values editing ---------")
    data = pd.DataFrame(columns=["lat", "long", "value"], index=range(len(sensor_data)))

    for j, entry in enumerate(sensor_data):
        temp = entry['sensorRealtimeData']
        temp = temp.drop(columns=["measuredTime", "time", "date", "dateObserved",
                                  "reliability", "source", "day", "sensorName"], errors='ignore')

        temp_new = {}
        var_name = None
        print("-----------------------------------------------------")
        print(names_matrix)
        if not names_matrix[names_matrix['sensorDataIndex'] == j].empty:
            print("Entrato")
            var_name = names_matrix.loc[names_matrix['sensorDataIndex'] == j, 'varNameList'].values[0]
            if "dateTime" in temp.columns:
                temp_new = temp.loc[:, [var_name, "dateTime"]]
            else:
                temp_new = temp[[var_name]]

            if var_name:
                temp_new[var_name] = temp_new[var_name].apply(safe_float_conversion)

        # Update the entry in sensor_data_list with the new dataframe
        sensor_data[j]['indexValues'] = temp_new

        # Handle missing values
        print("var_name:",var_name)
        if var_name and var_name in temp_new.columns:
            if temp_new[var_name].isna().all() :
                print(f"No Available Measures for the Variable '{var_name}' in ServiceUri '{entry['sensorName']}'")
                data.at[j, "value"] = np.nan
            else:
                # Calculate mean, skipping NaN values
                data.at[j, "value"] = temp_new[var_name].mean(skipna=True)

        # Assign latitude and longitude
        data.at[j, "lat"] = entry['sensorCoordinates'][1]
        data.at[j, "long"] = entry['sensorCoordinates'][0]

    if data['value'].isna().all():
        info_heatmap["message"].append(f"No Available Data for {value_types}: All ServiceUris are empty")
    else:
        data['value'] = data['value'].replace([-9999, 9999], np.nan)
        data = data.infer_objects(copy=False)

    print("--------- DATA MANIPULATION -- END ---------")
    print(datetime.now())
    print("------------------------------------------------")

    # --------------------------------------------------------------------------------------------------------------------------------------#
    print("--------- LAT-LONG BBOX CONVERSION TO UTM - START---------")

    long_min = float(long_min)
    long_max = float(long_max)
    lat_min = float(lat_min)
    lat_max = float(lat_max)

    bbox_coordinates = pd.DataFrame({
        'X': [long_min, long_max],
        'Y': [lat_min, lat_max]
    })

    print("bbox_coordinates")
    print(bbox_coordinates)

    # Define projections
    wgs84 = pyproj.CRS("EPSG:4326")  # WGS84 Lat/Lon
    utm = pyproj.CRS(f"EPSG:{epsg_projection}")  # UTM with the given EPSG code

    # Transform coordinates
    project = pyproj.Transformer.from_crs(wgs84, utm, always_xy=True).transform

    # Transform the bounding box coordinates to UTM
    utm_bbox_coordinates = bbox_coordinates.apply(
        lambda row: project(row['X'], row['Y']),
        axis=1
    )

    utm_bbox_coordinates = pd.DataFrame(utm_bbox_coordinates.tolist(), columns=['X', 'Y'])

    print("utm_bbox_coordinates")
    print(utm_bbox_coordinates)

    # Prepare coordinates for SpatialPoints
    x = [utm_bbox_coordinates['X'][1], utm_bbox_coordinates['X'][0]]
    y = [utm_bbox_coordinates['Y'][1], utm_bbox_coordinates['Y'][0]]
    xy = list(zip(x, y))

    # Create SpatialPoints object
    city_bbox = [Point(coord) for coord in xy]

    print("city_bbox")
    print(city_bbox)

    print("--------- LAT-LONG BBOX CONVERSION TO UTM - END ---------")
    print("------------------------------------------------")

    print("--------- DATA INTERPOLATION - START ---------")

    if len(data) >= 3:
        data = data.dropna().drop_duplicates().reset_index(drop=True)
        values = data['value'].astype(float).to_frame(name='value')
        coordinates = data[['long', 'lat']]

        # Transform to UTM coordinates
        wgs84 = Proj("EPSG:4326")  # WGS84 Lat/Lon
        utm = Proj(f"EPSG:{epsg_projection}")  # UTM with the given EPSG code
        transformer = Transformer.from_proj(wgs84, utm)

        coordinates['X'], coordinates['Y'] = transformer.transform(coordinates['lat'].values, coordinates['long'].values)

        # Create a GeoDataFrame
        geometry = [Point(xy) for xy in zip(coordinates['X'], coordinates['Y'])]
        gdf = gpd.GeoDataFrame(values, geometry=geometry, crs=f"EPSG:{epsg_projection}")

        # Ensure that the bounding box is aligned with the grid
        city_bbox = gpd.GeoSeries(city_bbox, crs=f"EPSG:{epsg_projection}")

        xmin, ymin, xmax, ymax = city_bbox.total_bounds

        step_size_meters = 100
        x_step_count = int((xmax - xmin) / step_size_meters)
        y_step_count = int((ymax - ymin) / step_size_meters)

        x_values = np.linspace(xmin, xmax, x_step_count + 1)
        y_values = np.linspace(ymin, ymax, y_step_count + 1)

        grid_x, grid_y = np.meshgrid(x_values, y_values)

        print(f"Data Bounds: {coordinates[['X', 'Y']].min().values} to {coordinates[['X', 'Y']].max().values}")
        print(f"Grid Bounds: {xmin}, {ymin} to {xmax}, {ymax}")

        grid_z = griddata(
            (coordinates['X'], coordinates['Y']), values['value'],
            (grid_x, grid_y), method='nearest'
        )

        interpolated_data = pd.DataFrame({
            'X': grid_x.flatten(),
            'Y': grid_y.flatten(),
            'V3': grid_z.flatten()
        }).dropna()

    else:
        return make_response(jsonify({
            "response": "error",
            "error": "No sufficient number of data for interpolation"

        }), 400)

    print(f"interpolatedData dim: {len(interpolated_data)}")
    print("--------- DATA INTERPOLATION - END ---------")

    print("--------- INTERPOLATED DATA LIST CREATION - START ---------")
    print(datetime.now())
    interpolated_heatmap = {
        'attributes': [],
        'saveStatus': []
    }

    id = 1
    for i, row in interpolated_data.iterrows():
        x = row['X']
        y = row['Y']
        mean_obs = row['V3']

        # Create a dictionary for the current record
        list_attrib_temp = {
            'id': id,
            'mapName': heatmap_name,
            'metricName': metric_name,
            'description': f"Average from {from_date_time} to {to_date_time}",
            'clustered': clustered,
            'latitude': x,
            'longitude': y,
            'value': mean_obs,
            'date': f"{to_date_time}Z",
            'xLength': step_size_meters,
            'yLength': step_size_meters,
            'projection': int(epsg_projection),
            'file': file,
            'org': 'DISIT'
        }
        id += 1

        # Append the dictionary to the attributes list
        interpolated_heatmap['attributes'].append(list_attrib_temp)

    # Print the length of the attributes list
    print(f"interpolatedHeatmap list length: {len(interpolated_heatmap['attributes'])}")
    print("--------- INTERPOLATED DATA LIST CREATION - END ---------")
    print(datetime.now())
    print("------------------------------------------------")

    wkt = (
        f"POLYGON(({long_min} {lat_min}, {long_max} {lat_min}, "
        f"{long_max} {lat_max}, {long_min} {lat_max}, {long_min} {lat_min}))"
    )

    wkt_geo_gson = {
        "type": "Polygon",
        "coordinates": [
            [
                [long_min, lat_min],
                [long_max, lat_min],
                [long_max, lat_max],
                [long_min, lat_max],
                [long_min, lat_min]
            ]
        ]
    }

    base_url = os.getenv("BASE_URL","https://www.snap4city.org")
    orionfilter_base_url = os.getenv("ORIONFILTER_BASE_URL","https://www.snap4city.org/orionfilter")
    if(broker is None):
        broker = os.getenv("DEFAULT_BROKER", "orionUNIFI")

    config = {
        "token": token,
        "model": {
            "model_name": heat_map_model_name,
            "model_type": "Heatmap",
            "model_kind": "sensor",
            "model_frequency": "600",
            "model_kgenerator": "normal",
            "model_contextbroker": broker,
            "model_protocol": "ngsi",
            "model_format": "json",
            "model_hc": "refresh_rate",
            "model_hv": "300",
            "model_subnature": subnature,
            "model_url": f"{base_url}/iot-directory/api/model.php?",
        },
        "producer": "DISIT",
        "k1": "cdfc46e7-75fd-46c5-b11e-04e231b08f37",
        "k2": "24f146b3-f2e8-43b8-b29f-0f9f1dd6cac5",
        'hlt': "Heatmap",
        "device_name": device_name,
        "heatmap_name": heatmap_name,
        "url": base_url,
        "device": {
            "device_url": f"{base_url}/iot-directory/api/device.php?"
        },
        "long": coordinates['long'].values[0],
        "lat": coordinates['lat'].values[0],
        "patch": orionfilter_base_url + "/" + broker +"/v2/entities/",
        "usernamedelegated": "",
        "passworddelegated": "",
        "color_map": color_map,
        "wkt": wkt,
        "bouding_box": wkt_geo_gson,
        "size": len(interpolated_data),
        "date_observed": f"{to_date_time}Z",
        "description": f"Average from {from_date_time} to {to_date_time}",
        "maximum_date": to_date_time,
        "minimum_date": from_date_time

    }
    print(json.dumps(config))
    r = create_heatmap_device(config)

    if r['status'] == 'ko' and r['error_msg']:
        print(f"ERROR! \n {r['error_msg']}")
        info_heatmap['device'] = {
            'POSTStatus': "Error in creating device",
            'error': r['error_msg']
        }
    else:
        info_heatmap['device'] = {
            'POSTStatus': f"Stato creazione device {config['device_name']} : " + r['status'],
        }
        print(f"\nStato creazione device {config['device_name']} : " + r['status'])

    print("Insert data in Device")

    time.sleep(5)

    response = send_heatmap_device_data(config)
    print(response.status_code)
    if response.status_code == 204:
        info_heatmap['device_data'] = {
            'POSTStatus': "Inserimento riuscito",
        }
        print("\nInserimento riuscito")
    else:
        info_heatmap['device_data'] = {
            'POSTStatus': f"Inserimento fallito",
            'error': response.text
        }
        print("\nInserimento fallito")
        print(response.text)

    # #--------------------------------------------------------------------------------------------------------------------------------------#
    # #--------------------------------------------------------------------------------------------------------------------------------------#   
    # #------------------------------------------------ SAVE INTERP RESULTS -----------------------------------------------------------------# 
    # #--------------------------------------------------------------------------------------------------------------------------------------#  
    # #--------------------------------------------------------------------------------------------------------------------------------------#   
    # print("--------- SAVING INTERPOLATED DATA LIST - START ---------")
    # print(datetime.now())

    print(interpolated_heatmap['attributes'][:5])
    # Convert the data to JSON format
    request_body_json = json.dumps(interpolated_heatmap['attributes'], indent=4)

    # Define the URL for the POST request
    heatmap_insert_base_url = os.getenv("HEATMAP_INSERT_BASE_URL","http://192.168.0.59:8000")
    heatmap_setmap_url = os.getenv("HEATMAP_SETMAP_URL","http://192.168.0.59/setMap.php")
    url = heatmap_insert_base_url + "/insertArray"
    print("Sending POST to ",url)
    # Send the POST request
    headers = {'Content-Type': 'application/json'}
    try:
        response = requests.post(url, data=request_body_json, headers=headers)

        write_log({
            "url": url,
            "header": headers,
            "params": request_body_json,
            "response_status": response.status_code
        })
        print(response)
        save_status = response.status_code
    except requests.RequestException as e:
        print(f"Request failed: {e}")
        save_status = None

    # Update the save status
    interpolated_data['saveStatus'] = save_status

    if save_status == 200:
        print("StatusCode 200")
        completed = 1
    else:
        print("Completed Status = 0")
        completed = 0

    # Construct the final API URL for GET request
    api_final = f"{heatmap_setmap_url}?mapName={heatmap_name}&metricName={metric_name}&date={to_date_time}Z&completed={completed}"
    print(api_final)
    # Send the GET request
    try:
        response = requests.get(api_final)
        api_status_code = response.status_code

        write_log({
            "url": api_final,
            'status': api_status_code
        })

    except requests.RequestException as e:
        print(f"Request failed: {e}")
        api_status_code = 0

    print("Return SensorHeatmap List")

    print("--------- SAVING INTERPOLATED DATA LIST - END ---------")
    print(datetime.now())
    print("------------------------------------------------")

    # Initialize infoHeatmap dictionary   
    # 

    if api_status_code == 200:
        info_heatmap['interpolation'] = {
            "POSTstatus": "Interpolated data saved correctly"
        }
        print("Interpolated data saved correctly")
    else:
        info_heatmap['interpolation'] = {
            "POSTstatus": "Problems on saving interpolated data",
            "error": response.text
        }
        print("Problems on saving interpolated data")
        print(response.text)

    return info_heatmap


def create_heatmap_device(conf: dict):
    print("Create Device")
    token = conf['token']
    device_name = conf['device_name']
    device_url = conf["device"]["device_url"]
    type = conf['model']['model_type']
    kind = conf['model']['model_kind']
    context_broker = conf['model']['model_contextbroker']
    format = conf['model']['model_format']
    model = conf['model']['model_name']
    producer = conf['producer']
    lat = conf['lat']
    long = conf['long']
    frequency = conf['model']['model_frequency']
    k1 = conf['k1']
    k2 = conf['k2']
    subnature = conf['model']['model_subnature']
    hlt = conf['hlt']
    wkt = conf['wkt']

    attributes = quote(json.dumps([{"value_name": "dateObserved", "data_type": "string", "value_type": "timestamp", "editable": "0",
                                    "value_unit": "timestamp", "healthiness_criteria": "refresh_rate", "healthiness_value": "300",
                                    "real_time_flag": "false"},
                                   {"value_name": "mapName", "data_type": "string", "value_type": "Identifier", "editable": "0", "value_unit": "ID",
                                    "healthiness_criteria": "refresh_rate", "healthiness_value": "300", "real_time_flag": "false"},
                                   {"value_name": "colorMap", "data_type": "string", "value_type": "Identifier", "editable": "0", "value_unit": "ID",
                                    "healthiness_criteria": "refresh_rate", "healthiness_value": "300", "real_time_flag": "false"},
                                   {"value_name": "minimumDate", "data_type": "string", "value_type": "time", "editable": "0", "value_unit": "s",
                                    "healthiness_criteria": "refresh_rate", "healthiness_value": "300", "real_time_flag": "false"},
                                   {"value_name": "maximumDate", "data_type": "string", "value_type": "time", "editable": "0", "value_unit": "s",
                                    "healthiness_criteria": "refresh_rate", "healthiness_value": "300", "real_time_flag": "false"},
                                   {"value_name": "instances", "data_type": "integer", "value_type": "Count", "editable": "0", "value_unit": "#",
                                    "healthiness_criteria": "refresh_rate", "healthiness_value": "300", "real_time_flag": "false"},
                                   {"value_name": "description", "data_type": "string", "value_type": "status", "editable": "0",
                                    "value_unit": "status", "healthiness_criteria": "refresh_rate", "healthiness_value": "300",
                                    "real_time_flag": "false"},
                                   {"value_name": "boundingBox", "data_type": "json", "value_type": "Geometry", "editable": "0", "value_unit": "text",
                                    "healthiness_criteria": "refresh_rate", "healthiness_value": "300", "real_time_flag": "false"},
                                   {"value_name": "size", "data_type": "integer", "value_type": "status", "editable": "0", "value_unit": "status",
                                    "healthiness_criteria": "refresh_rate", "healthiness_value": "300", "real_time_flag": "false"}]))

    header = {
        "Content-Type": "application/json",
        "Accept": "application/x-www-form-urlencoded",
        "Authorization": f"Bearer {token}",
    }

    url = device_url + f"action=insert&attributes={attributes}&id={device_name}&type={type}&kind={kind}&contextbroker={context_broker}&format={format}&mac=&model={model}&producer={producer}&latitude={lat}&longitude={long}&visibility=&frequency={frequency}&accessToken={token}&k1={k1}&k2={k2}&edgegateway_type=&edgegateway_uri=&subnature={subnature}&static_attributes=&servicePath=&nodered=false&hlt={hlt}&wktGeometry={wkt}&nodered=false"

    response = requests.request("PATCH", url, headers=header)

    write_log({
        "url": url,
        "header": header,
        "response_status": response.status_code
    })

    r = response.text
    r = json.loads(r)
    time.sleep(2)

    return r


def send_heatmap_device_data(conf: dict):
    token = conf['token']
    heatmap_name = conf['heatmap_name']
    device_name = conf['device_name']
    payload = {
        "boundingBox": {"value": conf['bouding_box'], "type": "json"},
        "colorMap": {"value": conf['color_map'], "type": "string"},
        "dateObserved": {"value": conf['date_observed'], "type": "string"},
        "description": {"value": conf['description'], "type": "string"},
        "instances": {"value": 1, "type": "integer"},
        "mapName": {"value": heatmap_name, "type": "string"},
        "maximumDate": {"value": conf['maximum_date'], "type": "string"},
        "minimumDate": {"value": conf['minimum_date'], "type": "string"},
        "size": {"value": conf['size'], "type": "integer"}
    }

    header = {
        "Content-Type": "application/json",
        "Accept": "application/json",
        "Authorization": f"Bearer {token}",
    }

    # timestamp = datetime.now().isoformat()
    # timestamp = timestamp[0:20] + "000Z"

    url = conf["patch"] + device_name + '/attrs?elementid=' + device_name + '&type=' + conf['model']['model_type']
    response = requests.request("PATCH", url, data=json.dumps(payload), headers=header)

    write_log({
        "url": url,
        "header": header,
        "params": payload,
        "response_status": response.status_code
    })
    return response

# test file for build endpoint
# convention: test_FUNCTION-NAME_RESULT-EXPECTED

################################################### DATA ###################################################

import json
import requests
import os
import sys
import numpy as np
import io

config = None
script_dir = os.path.dirname(os.path.realpath(__file__))

# Read and parse the JSON file
try:
    with open(os.path.join(script_dir,'config.json'), 'r') as file:
        config = json.load(file)
    print("Configuration loaded successfully:", config)
except json.JSONDecodeError as e:
    print(f"Error: Failed to parse JSON - {e}")
    sys.exit(1)  # Exit if JSON is invalid
except FileNotFoundError:
    print("Error: Configuration file not found.")
    sys.exit(1)
except Exception as e:
    print(f"Error: {e}")

#MGRS
x_orig_mgrs = [11.240716442245125, 11.271746328150613, 11.241751267083647]
y_orig_mgrs = [43.778027114249724, 43.7668719117573, 43.760050269085774]
x_dest_mgrs = [11.248737408508978, 11.248737408508978, 11.248737408508978]
y_dest_mgrs = [43.79163071922525, 43.79163071922525, 43.79163071922525]
precision = 1000
#GADM
x_orig_gadm = [11.254224622517292, 11.167166955936286, 11.29326591697936]
y_orig_gadm = [43.68395500620326, 43.8648446568991, 43.806420216210135]
x_dest_gadm = [11.253283529320194, 11.253283529320194, 11.253283529320194]
y_dest_gadm = [43.78755386935608, 43.78755386935608, 43.78755386935608]
#ISTAT
orig_reg = ['9', '9', '9']
orig_prov = ['48', '48', '48']
orig_comm = ['48017', '48017', '48017']
dest_reg = ['9', '9', '9']
dest_prov = ['48', '48', '48']
dest_comm = ['48001', '48043', '48041']

token = config['token']
second_user_token = config['second_user_token']
IP = config['ip']
PORT_GET = config['port_get']
BASE_URL = config['base_url']
URL_BUILD =  BASE_URL+config['url_build']
URL_INSERT =  BASE_URL+config['url_insert']
URL_GET = BASE_URL+config['url_get']
header = {
    "Authorization": f"Bearer {token}"
}
insert_header = {
    "Content-Type": "application/json",
    "Authorization": f"Bearer {token}"
}
second_insert_header = {
    "Content-Type": "application/json",
    "Authorization": f"Bearer {second_user_token}"
}
contextbroker='orion-1'
organization='Organization'
model = 'ODMModel'
type = 'ODMModel'
producer = 'DISIT'
subnature = ''

################################################### FUNCTIONS ###################################################

def compressed_build_mgrs(header):
    buf = io.BytesIO()
    np.savez_compressed(buf, 
        x_orig=x_orig_gadm, 
        y_orig=y_orig_gadm, 
        x_dest=x_dest_gadm, 
        y_dest=y_dest_gadm,
        precision=precision
    )
    buf.seek(0)
    #response = requests.post('http://' + IP + ':' + PORT_BUILD + '/buildcompressed', data=buf, headers=header)
    response = requests.post(URL_BUILD + '/buildcompressed', data=buf, headers=header)
    dd_data = json.loads(response.text)
    return dd_data

def compressed_build_gadm(header):
    buf = io.BytesIO()
    np.savez_compressed(buf, 
        x_orig=x_orig_gadm, 
        y_orig=y_orig_gadm, 
        x_dest=x_dest_gadm, 
        y_dest=y_dest_gadm
    )
    buf.seek(0)
    #response = requests.post('http://' + IP + ':' + PORT_BUILD + '/buildcommunes', data=buf, headers=header)
    response = requests.post(URL_BUILD + '/buildcommunes', data=buf, headers=header)
    dd_data = json.loads(response.text)
    return dd_data

def compressed_build_istat(header):
    buf = io.BytesIO()
    np.savez_compressed(buf, 
        orig_region_id = orig_reg, 
        orig_province_id = orig_prov, 
        orig_municipality_id = orig_comm, 
        dest_region_id = dest_reg, 
        dest_province_id = dest_prov, 
        dest_municipality_id = dest_comm
    )
    buf.seek(0)
    #response = requests.post('http://' + IP + ':' + PORT_BUILD + '/buildcommunes', data=buf, headers=header)
    response = requests.post(URL_BUILD + '/buildcommunes', data=buf, headers=header)
    dd_data = json.loads(response.text)
    return dd_data

def insert_mgrs(header, data_insert):
    #return requests.post('http://' + IP + ':' + PORT_INSERT + '/insert?' + f"model={model}&type={type}&contextbroker={contextbroker}&organization={organization}&producer={producer}&subnature={subnature}", data=json.dumps(data_insert), headers=header)
    return requests.post(URL_INSERT + '/insert?' + f"model={model}&type={type}&contextbroker={contextbroker}&organization={organization}&producer={producer}&subnature={subnature}", data=json.dumps(data_insert), headers=header)

def insert_gadm_istat(header, data_insert):
    #return requests.post('http://' + IP + ':' + PORT_INSERT + '/insertcommunes?' + f"model={model}&type={type}&contextbroker={contextbroker}&organization={organization}&producer={producer}&subnature={subnature}", data=json.dumps(data_insert), headers=header)
    return requests.post(URL_INSERT + '/insertcommunes?' + f"model={model}&type={type}&contextbroker={contextbroker}&organization={organization}&producer={producer}&subnature={subnature}", data=json.dumps(data_insert), headers=header)

def get_mgrs(header, data):
    #response = requests.get('http://' + IP + ':' + PORT_GET + '/get_mgrs?' + f"contextbroker={contextbroker}", params=data , headers=header)
    response = requests.get(URL_GET + '/get_mgrs?' + f"contextbroker={contextbroker}", params=data , headers=header)
    return response.json(), response.status_code

def get_gadm_istat(header, data):
    #response = requests.get('http://' + IP + ':' + PORT_GET + '/get?' + f"contextbroker={contextbroker}", params=data , headers=header)
    response = requests.get(URL_GET + '/get?' + f"contextbroker={contextbroker}", params=data , headers=header)
    return response.json(), response.status_code

def get_mgrs_polygon(header, data):
    #response = requests.get('http://' + IP + ':' + PORT_GET + '/mgrs_polygon', params=data , headers=header)
    response = requests.get(URL_GET + '/mgrs_polygon', params=data , headers=header)
    return response.json(), response.status_code

def get_mgrs_polygon_center(header, data):
    #response = requests.get('http://' + IP + ':' + PORT_GET + '/mgrs_polygon_center', params=data , headers=header)
    response = requests.get(URL_GET + '/mgrs_polygon_center', params=data , headers=header)
    return response.json(), response.status_code\

def get_polygon(header, data):
    #response = requests.get('http://' + IP + ':' + PORT_GET + '/polygon?' + f"contextbroker={contextbroker}", params=data , headers=header)
    response = requests.get(URL_GET + '/polygon?' + f"contextbroker={contextbroker}", params=data , headers=header)
    return response.json(), response.status_code

def get_polygon_stats(header, data):
    #response = requests.get('http://' + IP + ':' + PORT_GET + '/get_stats?' + f"contextbroker={contextbroker}", params=data , headers=header)
    response = requests.get(URL_GET + '/get_stats?' + f"contextbroker={contextbroker}", params=data , headers=header)
    return response.json(), response.status_code

def get_all_polygon(header, data):
    #response = requests.get('http://' + IP + ':' + PORT_GET + '/get_all_polygons?' + f"contextbroker={contextbroker}", params=data , headers=header)
    response = requests.get(URL_GET + '/get_all_polygons?' + f"contextbroker={contextbroker}", params=data , headers=header)
    return response.json(), response.status_code

def get_color(header, data):
    #response = requests.get('http://' + IP + ':' + PORT_GET + '/color', params=data , headers=header)
    response = requests.get(URL_GET + '/color', params=data , headers=header)
    return response.json(), response.status_code


def is_feature_collection(data):
    return (
        isinstance(data, dict) and
        data.get("type") == "FeatureCollection" and
        "features" in data and isinstance(data["features"], list)
    )

def insert_mgrs_data(header, od_name, from_date):
    dd_data = compressed_build_mgrs(header)
    od_id = od_name + "_" + organization + "_" + str(precision)
    data_insert = {
        "od_id": od_id,
        "x_orig": [item['x_orig'] for item in dd_data], 
        "y_orig": [item['y_orig'] for item in dd_data], 
        "x_dest": [item['x_dest'] for item in dd_data], 
        "y_dest": [item['y_dest'] for item in dd_data], 
        "from_date": from_date,
        "to_date": "2024-01-01 09:59:59", 
        "precision": precision, 
        "values": [item['value'] for item in dd_data], 
        "value_type": "test", 
        "value_unit": "test",  
        "description": "test", 
        "organization": organization, 
        "kind": "test", 
        "mode": "test", 
        "transport": "test", 
        "purpose": "test",
        "colormap_name": "colomapshapevalue",
        "representation": "MGRS"
    }
    return insert_mgrs(header, data_insert)

def insert_gadm_data(header, od_name, from_date):
    dd_data = compressed_build_gadm(header)
    od_id = od_name + "_" + organization + "_" + str(precision)
    data_insert = {
        "od_id": od_id,
        "orig_communes": [item['orig_commune'] for item in dd_data], 
        "dest_communes": [item['dest_commune'] for item in dd_data], 
        "from_date": from_date,
        "to_date": "2024-01-01 09:59:59", 
        "values": [item['value'] for item in dd_data], 
        "value_type": "test", 
        "value_unit": "test",  
        "description": "test", 
        "organization": organization, 
        "kind": "test", 
        "mode": "test", 
        "transport": "test", 
        "purpose": "test",
        "colormap_name": "colomapshapevalue",
        "representation": "GADM"
    }
    return insert_gadm_istat(header, data_insert)

def insert_istat_data(header, od_id, from_date):
    dd_data = compressed_build_istat(header)
    data_insert = {
        "od_id": od_id,
        "orig_communes": [item['orig_commune'] for item in dd_data], 
        "dest_communes": [item['dest_commune'] for item in dd_data], 
        "from_date": from_date,
        "to_date": "2024-01-01 09:59:59", 
        "values": [item['value'] for item in dd_data], 
        "value_type": "test", 
        "value_unit": "test",  
        "description": "test", 
        "organization": organization, 
        "kind": "test", 
        "mode": "test", 
        "transport": "test", 
        "purpose": "test",
        "source": "italy_epgs4326",
        "colormap_name": "colomapshapevalue",
        "representation": "ISTAT"
    }
    return insert_gadm_istat(header, data_insert)


################################################### TESTS ###################################################

def test_get_mgrs_success():
    from_date = "2046-01-01 09:00:00"
    res = insert_mgrs_data(insert_header, 'GT_MGRS', from_date)
    assert res.status_code == 200
    data = {
        "organization": organization,
        "precision": precision,
        "from_date": from_date,
        "latitude": '43.867960356353514',
        "longitude": '11.171681272693329',
        "inflow": True
    }
    result, status = get_mgrs(header, data)
    assert is_feature_collection(result) == True
    assert len(result['features']) > 0
    assert status == 200


def test_get_mgrs_resource_no_ownership():
    from_date = "2000-01-01 09:00:00"
    res = insert_mgrs_data(second_insert_header, 'GT_MGRS_NO', from_date)
    assert res.status_code == 200
    data = {
        "organization": organization,
        "precision": precision,
        "from_date": from_date,
        "latitude": '43.7586',
        "longitude": '11.2423',
        "inflow": True
    }
    result, status = get_mgrs(header, data)
    assert is_feature_collection(result) == True
    assert len(result['features']) == 0
    assert status == 200


def test_get_mgrs_resource_not_exists():
    data = {
        "organization": organization,
        "precision": precision,
        "from_date": "2023-01-01 09:00:00",
        "latitude": '43.7586',
        "longitude": '11.2423',
        "inflow": True
    }
    result, status = get_mgrs(header, data)
    assert is_feature_collection(result) == True
    assert len(result['features']) == 0
    assert status == 200


def test_get_mgrs_missing_parameter():
    data = {
        "organization": organization,
        "precision": precision,
        "from_date": "2023-01-01 09:00:00",
        "latitude": '43.7586',
    }
    result, status = get_mgrs(header, data)
    assert status == 400


def test_get_gadm_success():
    from_date = "2046-01-01 09:00:00"
    res = insert_gadm_data(insert_header, 'GT_GADM', from_date)
    assert res.status_code == 200
    data = {
        "organization": organization,
        "precision": precision,
        "from_date": from_date,
        "latitude": '43.6977',
        "longitude": '11.2470',
        "inflow": True
    }
    result, status = get_gadm_istat(header, data)
    assert is_feature_collection(result) == True
    assert len(result['features']) > 0
    assert status == 200


def test_get_gadm_resource_no_ownership():
    from_date = "2000-01-01 09:00:00"
    res = insert_gadm_data(second_insert_header, 'GT_GADM_NO', from_date)
    assert res.status_code == 200
    data = {
        "organization": organization,
        "precision": precision,
        "from_date": from_date,
        "latitude": '43.7586',
        "longitude": '11.2423',
        "inflow": True
    }
    result, status = get_gadm_istat(header, data)
    assert is_feature_collection(result) == True
    assert len(result['features']) == 0
    assert status == 200


def test_get_gadm_resource_not_exists():
    data = {
        "organization": organization,
        "precision": precision,
        "from_date": "2023-01-01 09:00:00",
        "latitude": '43.7586',
        "longitude": '11.2423',
        "inflow": True
    }
    result, status = get_gadm_istat(header, data)
    assert is_feature_collection(result) == True
    assert len(result['features']) == 0
    assert status == 200


def test_get_gadm_missing_parameter():
    data = {
        "organization": organization,
        "precision": precision,
        "from_date": "2023-01-01 09:00:00",
        "latitude": '43.7586',
    }
    result, status = get_gadm_istat(header, data)
    assert status == 400


def test_get_istat_success():
    from_date = "2046-01-01 09:00:00"
    od_id = 'GT_ISTAT' + "_" + organization + "_" + str(precision)
    res = insert_istat_data(insert_header, od_id, from_date)
    assert res.status_code == 200
    data = {
        "organization": organization,
        "precision": precision,
        "from_date": from_date,
        "latitude": '43.77996790468745',
        "longitude": '11.244942607789538',
        "inflow": True,
        "od_id": od_id
    }
    result, status = get_gadm_istat(header, data)
    assert is_feature_collection(result) == True
    assert len(result['features']) > 0
    assert status == 200


def test_get_istat_resource_no_ownership():
    from_date = "2046-01-01 09:00:00"
    od_id = 'GT_ISTAT_NO' + "_" + organization + "_" + str(precision)
    res = insert_istat_data(second_insert_header, od_id, from_date)
    assert res.status_code == 200
    data = {
        "organization": organization,
        "precision": precision,
        "from_date": from_date,
        "latitude": '43.7586',
        "longitude": '11.2423',
        "inflow": True,
        "od_id": od_id
    }
    result, status = get_gadm_istat(header, data)
    assert status == 403


def test_get_istat_resource_not_exists():
    data = {
        "organization": organization,
        "precision": precision,
        "from_date": "2023-01-01 09:00:00",
        "latitude": '43.7586',
        "longitude": '11.2423',
        "inflow": True,
        "od_id": "od_id"
    }
    result, status = get_gadm_istat(header, data)
    assert status == 400


def test_get_istat_missing_parameter():
    data = {
        "organization": organization,
        "precision": 1000,
        "from_date": "2023-01-01 09:00:00",
        "latitude": '43.7586',
    }
    result, status = get_gadm_istat(header, data)
    assert status == 400


def test_get_mgrs_polygon_success():
    data = {
        "precision": precision,
        "latitude": '43.7586',
        "longitude": '11.2423',
    }
    result, status = get_mgrs_polygon(header, data)
    assert len(result) > 0
    assert status == 200


def test_get_mgrs_polygon_missing_parameter():
    data = {
        "precision": precision,
        "latitude": '43.7586',
    }
    result, status = get_mgrs_polygon(header, data)
    assert status == 400


def test_get_mgrs_polygon_center_success():
    data = {
        "precision": precision,
        "latitude": '43.7586',
        "longitude": '11.2423',
    }
    result, status = get_mgrs_polygon_center(header, data)
    assert len(result) > 0
    assert status == 200


def test_get_mgrs_polygon_center_missing_parameter():
    data = {
        "precision": precision,
        "latitude": '43.7586',
    }
    result, status = get_mgrs_polygon_center(header, data)
    assert status == 400


def test_get_gadm_polygon_success():
    data = {
        "type": "",
        "latitude": '43.7586',
        "longitude": '11.2423',
        "organization": organization
    }
    result, status = get_polygon(header, data)
    assert is_feature_collection(result) == True
    assert len(result['features']) > 0
    assert status == 200


def test_get_gadm_polygon_not_exists():
    data = {
        "type": "",
        "latitude": '0',
        "longitude": '0',
        "organization": organization
    }
    result, status = get_polygon(header, data)
    assert is_feature_collection(result) == True
    assert len(result['features']) == 0
    assert status == 200


def test_get_gadm_polygon_missing_parameter():
    data = {
        "latitude": '43.7586',
    }
    result, status = get_polygon(header, data)
    assert status == 400


def test_get_istat_polygon_success():
    data = {
        "type": "municipality", # in the test db there are only municipalities
        "latitude": '43.7586',
        "longitude": '11.2423',
        "organization": organization
    }
    result, status = get_polygon(header, data)
    assert is_feature_collection(result) == True
    assert len(result['features']) > 0
    assert status == 200


def test_get_istat_polygon_not_exists():
    data = {
        "type": "municipality",
        "latitude": '0',
        "longitude": '0',
        "organization": organization
    }
    result, status = get_polygon(header, data)
    assert is_feature_collection(result) == True
    assert len(result['features']) == 0
    assert status == 200


def test_get_istat_polygon_missing_parameter():
    data = {
        "latitude": '43.7586',
    }
    result, status = get_polygon(header, data)
    assert status == 400


def test_get_polygon_stats_success():
    data = {
        "od_id": "GT_ISTAT_Organization_1000",
        "organization": organization,
        "poly_id": "4777",
        "from_date": "2046-01-01 09:00:00",
        "invalid_id": "",
        "invalid_label": ""
    }
    result, status = get_polygon_stats(header, data)
    assert is_feature_collection(result) == True
    assert len(result['features']) > 0
    assert status == 200


def test_get_polygon_stats_resource_no_ownership():
    data = {
        "od_id": "GT_ISTAT_NO_Organization_1000",
        "poly_id": "4777",
        "from_date": "2025-01-01 09:00:00",
        "invalid_id": "",
        "invalid_label": ""
    }
    result, status = get_polygon_stats(header, data)
    assert is_feature_collection(result) == True
    assert len(result['features']) == 0
    assert status == 200


def test_get_polygon_stats_resource_not_exists():
    data = {
        "od_id": "od_id_Organization_1000",
        "organization": organization,
        "poly_id": "4777",
        "from_date": "3024-01-01 09:00:00",
        "invalid_id": "",
        "invalid_label": ""
    }
    result, status = get_polygon_stats(header, data)
    assert status == 400


def test_get_polygon_stats_missing_parameter():
    data = {
        "od_id": "GT_ISTAT_Organization_1000",
        "poly_id": "4777",
        "from_date": "3024-01-01 09:00:00",
    }
    result, status = get_polygon_stats(header, data)
    assert status == 400


def test_get_all_polygon_success():
    data = {
        "latitude_ne": "43.7616",
        "longitude_ne": "11.2473",
        "latitude_sw": "43.7536",
        "longitude_sw": "11.2373",
        "organization": organization,
        "type": "municipality" # in the test db there are only municipalities
    }
    result, status = get_all_polygon(header, data)
    assert is_feature_collection(result) == True
    assert len(result['features']) > 0
    assert status == 200


def test_get_all_polygon_resource_not_exists():
    data = {
        "latitude_ne": "0.7616",
        "longitude_ne": "0.2473",
        "latitude_sw": "0.7536",
        "longitude_sw": "0.2373",
        "organization": organization,
        "type": "municipality" # in the test db there are only municipalities
    }
    result, status = get_all_polygon(header, data)
    assert is_feature_collection(result) == True
    assert len(result['features']) == 0
    assert status == 200


def test_get_all_polygon_missing_parameter():
    data = {
        "latitude_ne": "0.7616",
        "longitude_ne": "0.2473",
        "latitude_sw": "0.7536",
        "type": "municipality" # in the test db there are only municipalities
    }
    result, status = get_all_polygon(header, data)
    assert status == 400


def test_get_color_success():
    data = {
        "metric_name":"noiseLA"
    }
    result, status = get_color(header, data)
    assert len(result) > 0
    assert status == 200


def test_get_color_resource_not_exists():
    data = {
        "metric_name":"LAnoise"
    }
    result, status = get_color(header, data)
    assert len(result) == 0
    assert status == 200


def test_get_color_missing_parameter():
    data = {
    }
    result, status = get_color(header, data)
    assert status == 400
'''

def test_get_poi_polygon_without_filter_success():
    data = {
        "type": "poi",
        "latitude": '43.7982122',
        "longitude": '11.2534424',
        "organization": organization,
    }
    result, status = get_polygon(header, data)
    assert is_feature_collection(result) == True
    assert len(result['features']) > 0
    assert status == 200


def  test_get_poi_polygon_without_filter_not_exists():
    data = {
        "type": "poi",
        "latitude": '0',
        "longitude": '0',
        "organization": organization,
    }
    result, status = get_polygon(header, data)
    assert is_feature_collection(result) == True
    assert len(result['features']) == 0
    assert status == 200


def  test_get_poi_polygon_without_filter_missing_parameter():
    data = {
        "type": "poi",
        "latitude": '43.7982122',
    }
    result, status = get_polygon(header, data)
    assert status == 400


def test_get_poi_polygon_with_filter_success():
    data = {
        "type": "poi",
        "latitude": '43.7982122',
        "longitude": '11.2534424',
        "organization": organization,
        "od_id": "D3Xvu_Organization_1000"
    }
    result, status = get_polygon(header, data)
    assert is_feature_collection(result) == True
    assert len(result['features']) > 0
    assert status == 200


def  test_get_poi_polygon_with_filter_not_exists():
    data = {
        "type": "poi",
        "latitude": '0',
        "longitude": '0',
        "organization": organization,
        "od_id": "D3Xvu_Organization_1000"
    }
    result, status = get_polygon(header, data)
    assert is_feature_collection(result) == True
    assert len(result['features']) == 0
    assert status == 200


def  test_get_poi_polygon_with_filter_missing_parameter():
    data = {
        "type": "poi",
        "latitude": '43.7982122',
        "od_id": "D3Xvu_Organization_1000"
    }
    result, status = get_polygon(header, data)
    assert status == 400


def test_get_all_poi_polygon_without_filter_success():
    data = {
        "latitude_ne": "43.79936936",
        "longitude_ne": "11.25532783",
        "latitude_sw": "43.79679365",
        "longitude_sw": "11.25274512",
        "organization": organization,
        "type": "poi"
    }
    result, status = get_all_polygon(header, data)
    assert is_feature_collection(result) == True
    assert len(result['features']) > 0
    assert status == 200


def test_get_all_poi_polygon_without_filter_resource_not_exists():
    data = {
        "latitude_ne": "0.7616",
        "longitude_ne": "0.2473",
        "latitude_sw": "0.7536",
        "longitude_sw": "0.2373",
        "organization": organization,
        "type": "poi"
    }
    result, status = get_all_polygon(header, data)
    assert is_feature_collection(result) == True
    assert len(result['features']) == 0
    assert status == 200


def test_get_all_poi_polygon_without_filter_missing_parameter():
    data = {
        "latitude_ne": "0.7616",
        "longitude_ne": "0.2473",
        "latitude_sw": "0.7536",
        "type": "poi"
    }
    result, status = get_all_polygon(header, data)
    assert status == 400


def test_get_all_poi_polygon_with_filter_success():
    data = {
        "latitude_ne": "43.79936936",
        "longitude_ne": "11.25532783",
        "latitude_sw": "43.79679365",
        "longitude_sw": "11.25274512",
        "organization": organization,
        "type": "poi",
        "od_id": "D3Xvu_Organization_1000"
    }
    result, status = get_all_polygon(header, data)
    assert is_feature_collection(result) == True
    assert len(result['features']) > 0
    assert status == 200


def test_get_all_poi_polygon_with_filter_resource_not_exists():
    data = {
        "latitude_ne": "0.7616",
        "longitude_ne": "0.2473",
        "latitude_sw": "0.7536",
        "longitude_sw": "0.2373",
        "organization": organization,
        "type": "poi",
        "od_id": "D3Xvu_Organization_1000"
    }
    result, status = get_all_polygon(header, data)
    assert is_feature_collection(result) == True
    assert len(result['features']) == 0
    assert status == 200


def test_get_all_poi_polygon_with_filter_missing_parameter():
    data = {
        "latitude_ne": "0.7616",
        "longitude_ne": "0.2473",
        "latitude_sw": "0.7536",
        "type": "poi",
        "od_id": "D3Xvu_Organization_1000"
    }
    result, status = get_all_polygon(header, data)
    assert status == 400

def test_get_all_public_poi_polygon():
    data = {
        "latitude_ne": "43.79936936",
        "longitude_ne": "11.25532783",
        "latitude_sw": "43.79679365",
        "longitude_sw": "11.25274512",
        "type": "poi",
        "organization": organization,
        "od_id": "D3Xvu_Organization_1000"
    }
    result, status = get_all_polygon({}, data)
    assert status == 200
'''
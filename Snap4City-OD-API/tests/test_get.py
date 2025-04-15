# test file for build endpoint
# convention: test_FUNCTION-NAME_RESULT-EXPECTED

################################################### DATA ###################################################

import json
import requests
import os
import sys

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

token = config['token']
IP = config['ip']
PORT_GET = config['port_get']

header = {
    "Authorization": f"Bearer {token}"
}
contextbroker='orion-1'

################################################### FUNCTIONS ###################################################

def get_mgrs(header, data):
    response = requests.get('http://' + IP + ':' + PORT_GET + '/get_mgrs?' + f"contextbroker={contextbroker}", params=data , headers=header)
    return response.json(), response.status_code

def get_gadm_istat(header, data):
    response = requests.get('http://' + IP + ':' + PORT_GET + '/get?' + f"contextbroker={contextbroker}", params=data , headers=header)
    return response.json(), response.status_code

def get_mgrs_polygon(header, data):
    response = requests.get('http://' + IP + ':' + PORT_GET + '/mgrs_polygon', params=data , headers=header)
    return response.json(), response.status_code

def get_mgrs_polygon_center(header, data):
    response = requests.get('http://' + IP + ':' + PORT_GET + '/mgrs_polygon_center', params=data , headers=header)
    return response.json(), response.status_code\

def get_polygon(header, data):
    response = requests.get('http://' + IP + ':' + PORT_GET + '/polygon?' + f"contextbroker={contextbroker}", params=data , headers=header)
    return response.json(), response.status_code

def get_polygon_stats(header, data):
    response = requests.get('http://' + IP + ':' + PORT_GET + '/get_stats?' + f"contextbroker={contextbroker}", params=data , headers=header)
    return response.json(), response.status_code

def get_all_polygon(header, data):
    response = requests.get('http://' + IP + ':' + PORT_GET + '/get_all_polygons?' + f"contextbroker={contextbroker}", params=data , headers=header)
    return response.json(), response.status_code

def get_color(header, data):
    response = requests.get('http://' + IP + ':' + PORT_GET + '/color', params=data , headers=header)
    return response.json(), response.status_code


def is_feature_collection(data):
    return (
        isinstance(data, dict) and
        data.get("type") == "FeatureCollection" and
        "features" in data and isinstance(data["features"], list)
    )

################################################### TESTS ###################################################

def test_get_mgrs_success():
    data = {
        "organization": "Organization",
        "precision": 1000,
        "from_date": "2036-01-01 09:00:00",
        "latitude": '43.867960356353514',
        "longitude": '11.171681272693329',
        "inflow": True
    }
    result, status = get_mgrs(header, data)
    assert is_feature_collection(result) == True
    assert len(result['features']) > 0
    assert status == 200


def test_get_mgrs_resource_no_ownership():
    data = {
        "organization": "Organization",
        "precision": 1000,
        "from_date": "2025-01-01 09:00:00",
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
        "organization": "Organization",
        "precision": 1000,
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
        "organization": "Organization",
        "precision": 1000,
        "from_date": "2023-01-01 09:00:00",
        "latitude": '43.7586',
    }
    result, status = get_mgrs(header, data)
    assert status == 400


def test_get_gadm_success():
    data = {
        "organization": "Organization",
        "precision": 1000,
        "from_date": "2024-01-01 09:00:00",
        "latitude": '43.6977',
        "longitude": '11.2470',
        "inflow": True,
        "od_id": "R269k_Organization_1000"
    }
    result, status = get_gadm_istat(header, data)
    assert is_feature_collection(result) == True
    assert len(result['features']) > 0
    assert status == 200


def test_get_gadm_resource_no_ownership():
    data = {
        "organization": "Organization",
        "precision": 1000,
        "from_date": "2025-01-01 09:00:00",
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
        "organization": "Organization",
        "precision": 1000,
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
        "organization": "Organization",
        "precision": 1000,
        "from_date": "2023-01-01 09:00:00",
        "latitude": '43.7586',
    }
    result, status = get_gadm_istat(header, data)
    assert status == 400


def test_get_istat_success():
    data = {
        "organization": "Organization",
        "precision": 1000,
        "from_date": "2024-01-01 09:00:00",
        "latitude": '43.77996790468745',
        "longitude": '11.244942607789538',
        "inflow": True,
        "od_id": "SIqTI_Organization_1000",
        "source": "italy_epgs4326"
    }
    result, status = get_gadm_istat(header, data)
    assert is_feature_collection(result) == True
    assert len(result['features']) > 0
    assert status == 200


def test_get_istat_resource_no_ownership():
    data = {
        "organization": "Organization",
        "precision": 1000,
        "from_date": "2025-01-01 09:00:00",
        "latitude": '43.7586',
        "longitude": '11.2423',
        "inflow": True
    }
    result, status = get_gadm_istat(header, data)
    assert is_feature_collection(result) == True
    assert len(result['features']) == 0
    assert status == 200


def test_get_istat_resource_not_exists():
    data = {
        "organization": "Organization",
        "precision": 1000,
        "from_date": "2023-01-01 09:00:00",
        "latitude": '43.7586',
        "longitude": '11.2423',
        "inflow": True
    }
    result, status = get_gadm_istat(header, data)
    assert is_feature_collection(result) == True
    assert len(result['features']) == 0
    assert status == 200


def test_get_istat_missing_parameter():
    data = {
        "organization": "Organization",
        "precision": 1000,
        "from_date": "2023-01-01 09:00:00",
        "latitude": '43.7586',
    }
    result, status = get_gadm_istat(header, data)
    assert status == 400


def test_get_mgrs_polygon_success():
    data = {
        "precision": 1000,
        "latitude": '43.7586',
        "longitude": '11.2423',
    }
    result, status = get_mgrs_polygon(header, data)
    assert len(result) > 0
    assert status == 200


def test_get_mgrs_polygon_missing_parameter():
    data = {
        "precision": 1000,
        "latitude": '43.7586',
    }
    result, status = get_mgrs_polygon(header, data)
    assert status == 400


def test_get_mgrs_polygon_center_success():
    data = {
        "precision": 1000,
        "latitude": '43.7586',
        "longitude": '11.2423',
    }
    result, status = get_mgrs_polygon_center(header, data)
    assert len(result) > 0
    assert status == 200


def test_get_mgrs_polygon_center_missing_parameter():
    data = {
        "precision": 1000,
        "latitude": '43.7586',
    }
    result, status = get_mgrs_polygon_center(header, data)
    assert status == 400


def test_get_gadm_polygon_success():
    data = {
        "type": "",
        "latitude": '43.7586',
        "longitude": '11.2423',
        "organization": "Organization"
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
        "organization": "Organization"
    }
    result, status = get_polygon(header, data)
    assert is_feature_collection(result) == True
    assert len(result['features']) == 0
    assert status == 200


def test_get_gadm_polygon_missing_parameter():
    data = {
        "precision": 1000,
        "latitude": '43.7586',
    }
    result, status = get_polygon(header, data)
    assert status == 400


def test_get_istat_polygon_success():
    data = {
        "type": "municipality", # in the test db there are only municipalities
        "latitude": '43.7586',
        "longitude": '11.2423',
        "organization": "Organization"
    }
    result, status = get_polygon(header, data)
    assert is_feature_collection(result) == True
    assert len(result['features']) > 0
    assert status == 200


def test_get_istat_polygon_not_exists():
    data = {
        "type": "",
        "latitude": '0',
        "longitude": '0',
        "organization": "Organization"
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
        "od_id": "SIqTI_Organization_1000",
        "organization": "Organization",
        "poly_id": "4777",
        "from_date": "2024-01-01 09:00:00",
        "invalid_id": "",
        "invalid_label": ""
    }
    result, status = get_polygon_stats(header, data)
    assert is_feature_collection(result) == True
    assert len(result['features']) > 0
    assert status == 200


def test_get_polygon_stats_resource_no_ownership():
    data = {
        "od_id": "SIqTI_Organization_1000",
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
        "od_id": "SIqTI_Organization_1000",
        "poly_id": "4777",
        "from_date": "3024-01-01 09:00:00",
        "invalid_id": "",
        "invalid_label": ""
    }
    result, status = get_polygon_stats(header, data)
    assert is_feature_collection(result) == True
    assert len(result['features']) == 0
    assert status == 200


def test_get_polygon_stats_missing_parameter():
    data = {
        "od_id": "SIqTI_Organization_1000",
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
        "organization": "Organization",
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
        "organization": "Organization",
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


def test_get_poi_polygon_without_filter_success():
    data = {
        "type": "poi",
        "latitude": '43.7982122',
        "longitude": '11.2534424',
        "organization": "Organization",
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
        "organization": "Organization",
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
        "organization": "Organization",
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
        "organization": "Organization",
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
        "organization": "Organization",
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
        "organization": "Organization",
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
        "organization": "Organization",
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
        "organization": "Organization",
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
        "organization": "Organization",
        "od_id": "D3Xvu_Organization_1000"
    }
    result, status = get_all_polygon({}, data)
    assert status == 200

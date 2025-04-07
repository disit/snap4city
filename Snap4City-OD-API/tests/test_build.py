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

IP = config['ip']
PORT_BUILD = config['port_build']

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

#headers
valid_header = {
    "Content-Type": "application/json",
    "Authorization": f"Bearer {config['token']}"
}

missing_token_header = {
    "Content-Type": "application/json",
    "Authorization": "Bearer "
}

non_valid_token_header = {
    "Content-Type": "application/json",
    "Authorization": "Bearer ciao"
}

expired_token_header = {
    "Content-Type": "application/json",
    "Authorization": "Bearer EXPIRED TOKEN"
}

################################################### FUNCTIONS ###################################################

def uncompressed_build_mgrs(header):
    data = {
    "x_orig": x_orig_mgrs,
    "y_orig": y_orig_mgrs, 
    "x_dest": x_dest_mgrs, 
    "y_dest": y_dest_mgrs,
    "precision": precision
    }
    response = requests.post('http://' + IP + ':' + PORT_BUILD + '/build', data=json.dumps(data), headers=header)
    dd_data = json.loads(response.text)
    return dd_data

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
    response = requests.post('http://' + IP + ':' + PORT_BUILD + '/buildcompressed', data=buf, headers=header)
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
    response = requests.post('http://' + IP + ':' + PORT_BUILD + '/buildcommunes', data=buf, headers=header)
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
    response = requests.post('http://' + IP + ':' + PORT_BUILD + '/buildcommunes', data=buf, headers=header)
    dd_data = json.loads(response.text)
    return dd_data

################################################### TESTS ###################################################

def test_uncompressed_build_mgrs_success():
    response = uncompressed_build_mgrs(valid_header)
    assert isinstance(response, list)
    assert len(response) > 0


def test_uncompressed_build_mgrs_missing_header():
    response = uncompressed_build_mgrs(None)
    assert response['status'] == 401


def test_uncompressed_build_mgrs_missing_token():
    response = uncompressed_build_mgrs(missing_token_header)
    assert  response['status'] == 401


def test_uncompressed_build_mgrs_non_valid_token():
    response = uncompressed_build_mgrs(non_valid_token_header)
    assert  response['status'] == 401


def test_uncompressed_build_mgrs_expired_token():
    response = uncompressed_build_mgrs(expired_token_header)
    assert  response['status'] == 401


def test_compressed_build_mgrs_success():
    response = compressed_build_mgrs(valid_header)
    assert isinstance(response, list)
    assert len(response) > 0


def test_compressed_build_mgrs_missing_header():
    response = compressed_build_mgrs(None)
    assert response['status'] == 401


def test_compressed_build_gadm_missing_token():
    response = compressed_build_mgrs(missing_token_header)
    assert  response['status'] == 401


def test_compressed_build_mgrs_non_valid_token():
    response = compressed_build_mgrs(non_valid_token_header)
    assert  response['status'] == 401


def test_compressed_build_mgrs_expired_token():
    response = compressed_build_mgrs(expired_token_header)
    assert  response['status'] == 401


def test_compressed_build_gadm_success():
    response = compressed_build_gadm(valid_header)
    assert isinstance(response, list)
    assert len(response) > 0


def test_compressed_build_gadm_missing_header():
    response = compressed_build_gadm(None)
    assert response['status'] == 401


def test_compressed_build_gadm_missing_token():
    response = compressed_build_gadm(missing_token_header)
    assert  response['status'] == 401


def test_compressed_build_gadm_non_valid_token():
    response = compressed_build_gadm(non_valid_token_header)
    assert  response['status'] == 401


def test_compressed_build_gadm_expired_token():
    response = compressed_build_gadm(expired_token_header)
    assert  response['status'] == 401


def test_compressed_build_istat_success():
    response = compressed_build_istat(valid_header)
    assert isinstance(response, list)
    assert len(response) > 0


def test_compressed_build_istat_missing_header():
    response = compressed_build_istat(None)
    assert response['status'] == 401


def test_compressed_build_istat_missing_token():
    response = compressed_build_istat(missing_token_header)
    assert  response['status'] == 401


def test_compressed_build_istat_non_valid_token():
    response = compressed_build_istat(non_valid_token_header)
    assert  response['status'] == 401


def test_compressed_build_istat_expired_token():
    response = compressed_build_istat(expired_token_header)
    assert  response['status'] == 401
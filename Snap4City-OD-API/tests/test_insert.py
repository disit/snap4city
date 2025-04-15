# test file for insert endpoint
# convention: test_FUNCTION-NAME_RESULT-EXPECTED

################################################### DATA ###################################################

import json
import requests
import random
import string
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
IP = config['ip']
PORT_BUILD = config['port_build']
PORT_INSERT = config['port_insert']
header = {
    "Content-Type": "application/json",
    "Authorization": f"Bearer {token}"
}

model = 'ODMModel'
type = 'ODMModel'
contextbroker = 'orion-1'
producer = 'DISIT'
subnature = ''
organization = 'Organization'
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

def insert_mgrs(header, data_insert):
    return requests.post('http://' + IP + ':' + PORT_INSERT + '/insert?' + f"model={model}&type={type}&contextbroker={contextbroker}&organization={organization}&producer={producer}&subnature={subnature}", data=json.dumps(data_insert), headers=header)

def insert_gadm_istat(header, data_insert):
    return requests.post('http://' + IP + ':' + PORT_INSERT + '/insertcommunes?' + f"model={model}&type={type}&contextbroker={contextbroker}&organization={organization}&producer={producer}&subnature={subnature}", data=json.dumps(data_insert), headers=header)
################################################### TESTS ###################################################

def test_insert_mgrs_success():
    dd_data = compressed_build_mgrs(header)

    od_name = ''.join(random.choices(string.ascii_letters + string.digits, k=5))
    organization = 'Organization'
    od_id = od_name + "_" + organization + "_" + str(precision)

    data_insert = {
        "od_id": od_id,
        "x_orig": [item['x_orig'] for item in dd_data], 
        "y_orig": [item['y_orig'] for item in dd_data], 
        "x_dest": [item['x_dest'] for item in dd_data], 
        "y_dest": [item['y_dest'] for item in dd_data], 
        "from_date": "2036-01-01T09:00:00Z",
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

    res = insert_mgrs(header, data_insert)
    assert res.status_code == 200
    assert res.json() == {"message": "data inserted successfully", "status": 200}

def test_insert_mgrs_device_no_ownership():
    dd_data = compressed_build_mgrs(header)
    
    od_name = 'ABCDE'
    organization = 'Organization'
    od_id = od_name + "_" + organization + "_" + str(precision)

    data_insert = {
        "od_id": od_id,
        "x_orig": [item['x_orig'] for item in dd_data], 
        "y_orig": [item['y_orig'] for item in dd_data], 
        "x_dest": [item['x_dest'] for item in dd_data], 
        "y_dest": [item['y_dest'] for item in dd_data], 
        "from_date": "2024-01-01 09:00:00",
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

    assert insert_mgrs(header, data_insert).status_code == 409

def test_insert_mgrs_device_missing_parameters():
    dd_data = compressed_build_mgrs(header)

    od_name = ''.join(random.choices(string.ascii_letters + string.digits, k=5))
    organization = 'Organization'
    od_id = od_name + "_" + organization + "_" + str(precision)

    data_insert = {
        "od_id": od_id,
        "x_orig": [item['x_orig'] for item in dd_data], 
        "y_orig": [item['y_orig'] for item in dd_data], 
        "x_dest": [item['x_dest'] for item in dd_data], 
        "y_dest": [item['y_dest'] for item in dd_data], 
        "from_date": "2024-01-01 09:00:00",
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
        "purpose": "test"
    }

    assert insert_mgrs(header, data_insert).status_code == 400


def test_insert_mgrs_device_update():
    dd_data = compressed_build_mgrs(header)

    od_name = 'SIqTI'
    organization = 'Organization'
    od_id = od_name + "_" + organization + "_" + str(precision)

    data_insert = {
        "od_id": od_id,
        "x_orig": [item['x_orig'] for item in dd_data], 
        "y_orig": [item['y_orig'] for item in dd_data], 
        "x_dest": [item['x_dest'] for item in dd_data], 
        "y_dest": [item['y_dest'] for item in dd_data], 
        "from_date": "2024-01-01 09:00:00",
        "to_date": "2024-01-01 09:59:59", 
        "precision": precision, 
        "values": [item['value'] for item in dd_data], 
        "value_type": None, 
        "value_unit": None,  
        "description": "description", 
        "organization": organization, 
        "kind": None, 
        "mode": "mode", 
        "transport": "transport", 
        "purpose": "purpose",
        "colormap_name": None,
        "representation": None
    }

    assert insert_mgrs(header, data_insert).status_code == 200


def test_insert_gadm_success():
    dd_data = compressed_build_gadm(header)

    od_name = ''.join(random.choices(string.ascii_letters + string.digits, k=5))
    organization = 'Organization'
    od_id = od_name + "_" + organization + "_" + str(precision)

    data_insert = {
        "od_id": od_id,
        "orig_communes": [item['orig_commune'] for item in dd_data], 
        "dest_communes": [item['dest_commune'] for item in dd_data], 
        "from_date": "2024-01-01 09:00:00",
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

    assert insert_gadm_istat(header, data_insert).status_code == 200

def test_insert_gadm_device_no_ownership():
    dd_data = compressed_build_gadm(header)
    
    od_name = 'ABCDE'
    organization = 'Organization'
    od_id = od_name + "_" + organization + "_" + str(precision)

    data_insert = {
        "od_id": od_id,
        "orig_communes": [item['orig_commune'] for item in dd_data], 
        "dest_communes": [item['dest_commune'] for item in dd_data], 
        "from_date": "2024-01-01 09:00:00",
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

    assert insert_gadm_istat(header, data_insert).status_code == 409

def test_insert_gadm_device_missing_parameters():
    dd_data = compressed_build_gadm(header)

    od_name = ''.join(random.choices(string.ascii_letters + string.digits, k=5))
    organization = 'Organization'
    od_id = od_name + "_" + organization + "_" + str(precision)

    data_insert = {
        "od_id": od_id,
        "orig_communes": [item['orig_commune'] for item in dd_data], 
        "dest_communes": [item['dest_commune'] for item in dd_data], 
        "from_date": "2024-01-01 09:00:00",
        "to_date": "2024-01-01 09:59:59", 
        "values": [item['value'] for item in dd_data], 
        "value_type": "test", 
        "value_unit": "test",  
        "description": "test", 
        "organization": organization, 
        "kind": "test", 
        "mode": "test", 
        "transport": "test", 
        "purpose": "test"
    }

    assert insert_gadm_istat(header, data_insert).status_code == 400


def test_insert_gadm_device_update():
    dd_data = compressed_build_gadm(header)

    od_name = 'SIqTI'
    organization = 'Organization'
    od_id = od_name + "_" + organization + "_" + str(precision)

    data_insert = {
        "od_id": od_id,
        "orig_communes": [item['orig_commune'] for item in dd_data], 
        "dest_communes": [item['dest_commune'] for item in dd_data], 
        "from_date": "2024-01-01 09:00:00",
        "to_date": "2024-01-01 09:59:59", 
        "values": [item['value'] for item in dd_data], 
        "value_type": None, 
        "value_unit": None,  
        "description": "oirbvdsfhsòbv", 
        "organization": organization, 
        "kind": "vlsdkhàvoihsdàoòv", 
        "mode": None, 
        "transport": "èùogròùkvn", 
        "purpose": None,
        "colormap_name": "colomapshapevalue",
        "representation": "GADM"
    }

    assert insert_gadm_istat(header, data_insert).status_code == 200

def test_insert_istat_success():
    dd_data = compressed_build_istat(header)

    od_name = ''.join(random.choices(string.ascii_letters + string.digits, k=5))
    organization = 'Organization'
    od_id = od_name + "_" + organization + "_" + str(precision)

    data_insert = {
        "od_id": od_id,
        "orig_communes": [item['orig_commune'] for item in dd_data], 
        "dest_communes": [item['dest_commune'] for item in dd_data], 
        "from_date": "2024-01-01 09:00:00",
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

    assert insert_gadm_istat(header, data_insert).status_code == 200

def test_insert_istat_device_no_ownership():
    dd_data = compressed_build_istat(header)
    
    od_name = 'ABCDE'
    organization = 'Organization'
    od_id = od_name + "_" + organization + "_" + str(precision)

    data_insert = {
        "od_id": od_id,
        "orig_communes": [item['orig_commune'] for item in dd_data], 
        "dest_communes": [item['dest_commune'] for item in dd_data], 
        "from_date": "2024-01-01 09:00:00",
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

    assert insert_gadm_istat(header, data_insert).status_code == 409

def test_insert_istat_device_missing_parameters():
    dd_data = compressed_build_istat(header)

    od_name = ''.join(random.choices(string.ascii_letters + string.digits, k=5))
    organization = 'Organization'
    od_id = od_name + "_" + organization + "_" + str(precision)

    data_insert = {
        "od_id": od_id,
        "orig_communes": [item['orig_commune'] for item in dd_data], 
        "dest_communes": [item['dest_commune'] for item in dd_data], 
        "from_date": "2024-01-01 09:00:00",
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
        "source": "italy_epgs4326"
    }

    assert insert_gadm_istat(header, data_insert).status_code == 400


def test_insert_istat_device_update():
    dd_data = compressed_build_istat(header)

    od_name = 'SIqTI'
    organization = 'Organization'
    od_id = od_name + "_" + organization + "_" + str(precision)

    data_insert = {
        "od_id": od_id,
        "orig_communes": [item['orig_commune'] for item in dd_data], 
        "dest_communes": [item['dest_commune'] for item in dd_data], 
        "from_date": "2024-01-01 09:00:00",
        "to_date": "2024-01-01 09:59:59", 
        "values": [item['value'] for item in dd_data], 
        "value_type": None, 
        "value_unit": None,  
        "description": "oirbvdsfhsòbv", 
        "organization": organization, 
        "kind": "vlsdkhàvoihsdàoòv", 
        "mode": None, 
        "transport": "èùogròùkvn", 
        "purpose": None,
        "source": "italy_epgs4326",
        "colormap_name": "colomapshapevalue",
        "representation": "ISTAT"
    }

    assert insert_gadm_istat(header, data_insert).status_code == 200

def test_insert_custom_success():

    orig_reg = ['9']
    orig_prov = ['48']
    orig_comm = ['48017']
    dest_reg = ['9']
    dest_prov = ['48']
    dest_comm = ['48017']

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

    od_name = ''.join(random.choices(string.ascii_letters + string.digits, k=5))
    organization = 'Organization'
    od_id = od_name + "_" + organization + "_" + str(precision)
    data_insert = {
        "od_id": od_id,
        "orig_communes": [item['orig_commune'] for item in dd_data], 
        "dest_communes": [item['dest_commune'] for item in dd_data], 
        "from_date": "2222-01-01 09:00:00",
        "to_date": "2222-01-01 09:59:59", 
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

    res = insert_gadm_istat(header, data_insert)
    assert res.status_code == 200
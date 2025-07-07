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
from http_methods import get_request, patch_request
import json
from datetime import datetime, timezone
import os


#device creation
def create_device(config, token, id, model, contextbroker, organization, producer, subnature, coords, wkt):
    base_url_var = 'default_base_url'
    if 'base_url' in config:
        base_url_var = config['base_url']
    base_url = os.getenv('BASE_URL', base_url_var)

    #get model
    model_url_var = 'default_model_url'
    if 'model_url' in config:
        model_url_var = config['model_url']
    url = base_url + os.getenv('MODEL_URL', model_url_var)

    url = url + f"action=get_model&name={model}&nodered=true"
    params = {
        "action": "get_model",
        "name": model,
        "nodered": "true"
    }
    header = {'Authorization': 'Bearer '+token}

    model_result, status = get_request(url, params, header)

    if status == None or status != 200:
        return model_result, status

    dvc = {
        'action': 'insert',
        'token': token,
        'latitude': coords['lat'],
        'longitude': coords['lng'],
        'wktGeometry': wkt,
        'subnature': subnature,
        'nodered': "true",
        'model': model
    }

    device_url_var = 'default_device_url'
    if 'device_url' in config:
        device_url_var = config['device_url']
    url = base_url + os.getenv('DEVICE_URL', device_url_var)

    dvc.update(model_result['content'])
    dvc['type'] = dvc['devicetype']
    dvc['k1'] = ""
    dvc['k2'] = ""
    dvc['id'] = id
    if producer is not None:
        dvc['producer'] = producer
    if contextbroker is not None and contextbroker != '':
        dvc['contextbroker'] = contextbroker
    if organization is not None and organization != '':
        dvc['organization'] = organization
    return get_request(url, dvc)


#check if a date is in isoformat, if not convert it
def isoformat(date_str):
    #check if date is in isoformat
    try:
        datetime.strptime(date_str, "%Y-%m-%dT%H:%M:%SZ")
        return date_str
    except ValueError:
        dt = datetime.strptime(date_str, "%Y-%m-%d %H:%M:%S").replace(tzinfo=timezone.utc)
        d = dt.isoformat().replace("+00:00", "Z")
        return d
        
    
#device payload
def payload(data):
    payload = {}
    payload['dateObserved'] = {'type':'string', 'value': isoformat(datetime.now(timezone.utc).strftime("%Y-%m-%d %H:%M:%S"))}
    payload['description'] = {'type':'string', 'value': data['description']}
    payload['precision'] = {'type':'integer', 'value': data['precision']}
    payload['kind'] = {'type':'string', 'value': data['kind']}
    payload['mode'] = {'type':'string', 'value': data['mode']}
    payload['transport'] = {'type':'string', 'value': data['transport']}
    payload['purpose'] = {'type':'string', 'value': data['purpose']}
    payload['instances'] = {'type':'integer', 'value': data['instances']}
    payload['fromDate'] = {'type':'string', 'value': isoformat(data['from_date'])}
    payload['toDate'] = {'type':'string', 'value': isoformat(data['to_date'])}
    payload['geometry'] = {'type':'json', 'value': data['geometry']}
    payload['colormapName'] = {'type':'string', 'value': data['colormap_name']}
    payload['representation'] = {'type':'string', 'value': data['representation']}
    return payload
    

#device data insertion
def insert_data(config, token, id, type, contexbroker, data):
    base_url_var = 'default_base_url'
    if 'base_url' in config:
         base_url_var = config['base_url']
    base_url = os.getenv('BASE_URL', base_url_var)

    insert_url_var = 'default_insert_url'
    if 'insert_url' in config:
        insert_url_var = config['INSERT_URL']
    url = base_url + os.getenv('INSERT_URL', insert_url_var)

    url += f"{contexbroker}/v2/entities/{id}/attrs?elementid={id}&type={type}"
    header = {
        'Content-Type': 'application/json',
        'Authorization': f"Bearer {token}"
    }
    pld = payload(data)
    data = json.dumps(pld)
    return patch_request(url, header, data)
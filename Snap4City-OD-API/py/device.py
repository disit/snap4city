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

#device metadata
def device(token, id, model, type, contextbroker, producer, subnature, coords, wkt):
    return {
        'action': 'insert',
        'token': token,
        'id': id,
        'type': type,
        'contextbroker': contextbroker,
        'kind': "sensor",
        'format': "json",
        'model': model,
        'producer': producer,
        'latitude': coords['lat'],
        'longitude': coords['lng'],
        'k1': "",
        'k2': "",
        'frequency': "600",
        'nodered': "true",
        'wktGeometry': wkt,
        'highleveltype': 'OriginDestinationMatrix',
        'subnature': subnature
    }


#device attributes
def attribute (value_name, value_type = "description", value_unit = "text"):
    return {
        'value_name': value_name,
        'data_type': "string",
        'value_type': value_type,
        'editable': "0",
        'value_unit': value_unit,
        'healthiness_criteria': "refresh_rate",
        'healthiness_value': "300"
    }


#device creation
def create_device(config, token, id, model, type, contextbroker, producer, subnature, coords, wkt):
    dvc = device(token, id, model, type, contextbroker, producer, subnature, coords, wkt)
    attributes = [
        attribute("dateObserved", "datetime", "timestamp"),
        attribute("description"),
        attribute("precision", "Count", "#"),
        attribute("kind"),
        attribute("mode"),
        attribute("transport"),
        attribute("purpose"),
        attribute("instances", "Count", "#"),
        attribute("fromDate", "datetime", "timestamp"),
        attribute("toDate", "datetime", "timestamp"),
        attribute('geometry', 'datastructure', 'complex'),
        attribute("colormapName"),
        attribute("representation")
    ]
    base_url = os.getenv('BASE_URL', config['BASE_URL'])
    url = base_url + os.getenv('DEVICE_URL', config['DEVICE_URL'])
    dvc['attributes'] = json.dumps(attributes)
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
    base_url = os.getenv('BASE_URL', config['BASE_URL'])
    url = base_url + os.getenv('INSERT_URL', config['INSERT_URL'])
    url += f"{contexbroker}/v2/entities/{id}/attrs?elementid={id}&type={type}"
    header = {
        'Content-Type': 'application/json',
        'Authorization': f"Bearer {token}"
    }
    pld = payload(data)
    data = json.dumps(pld)
    return patch_request(url, header, data)
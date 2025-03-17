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

from datetime import datetime

from flask import Flask, request, jsonify, make_response

from heatmap import heatmapIDW
from helper import parse_from_date, is_date

app = Flask(__name__)


@app.route('/')
def hello_word():
    return 'hello_word'


@app.route('/heatmap', methods=['POST'])
def heat_map():
    data = request.json
    date_format = '%Y-%m-%dT%H:%M:%S'

    required_fields = {
        'city': str,
        'long_min': float,
        'long_max': float,
        'lat_min': float,
        'lat_max': float,
        'epsg_projection': int,
        'value_types': list,
        'subnature': str,
        'scenario': str,
        'color_map': str,
        'from_date_time': str,
        'to_date_time': str,
        'token': str,
        'heat_map_model_name': str, 
    }

    missing_fields = [field for field in required_fields if field not in data]
    if missing_fields:
        return make_response(jsonify({
            "response": "error",
            "error": f"Missing fields: {', '.join(missing_fields)}"
        }), 400)

    for field, field_type in required_fields.items():
        if not isinstance(data.get(field), field_type):
            return make_response(jsonify({
                "response": "error",
                "message": f"Invalid data type for {field}. Expected {field_type.__name__}."
            }), 400)

    city = data.get('city')
    long_min = float(data.get('long_min'))
    long_max = float(data.get('long_max'))
    lat_min = float(data.get('lat_min'))
    lat_max = float(data.get('lat_max'))
    epsg_projection = int(data.get('epsg_projection'))
    value_types = data.get('value_types')
    # TODO handle value unit
    value_types = [value.split('-')[0] for value in value_types]
    subnature = data.get('subnature')
    scenario = data.get('scenario')
    color_map = data.get('color_map')
    from_date_time = data.get('from_date_time')
    to_date_time = data.get('to_date_time')
    token = data.get('token')
    heat_map_model_name = data.get('heat_map_model_name')
    model = data.get('model')
    clustered = int(data.get('clustered', 0))
    file = int(data.get('file', 1))
    broker = data.get('broker')


    if not (-180 <= long_min <= 180 and -180 <= long_max <= 180):
        return make_response(jsonify({
            "response": "error",
            "message": "Longitude values (long_min, long_max) must be between -180 and 180."
        }), 400)

    if not (-90 <= lat_min <= 90 and -90 <= lat_max <= 90):
        return make_response(jsonify({
            "response": "error",
            "message": "Latitude values (lat_min, lat_max) must be between -90 and 90."
        }), 400)

    if long_max <= long_min:
        return make_response(jsonify({
            "response": "error",
            "message": "Please insert 'long_max' and 'long_min' parameters correctly. 'long_max' must be greater than 'long_min'."
        }), 400)

    if lat_max <= lat_min:
        return make_response(jsonify({
            "response": "error",
            "message": "Please insert 'lat_max' and 'lat_min' parameters correctly. 'lat_max' must be greater than 'lat_min'."
        }), 400)

    parsed_from_date = parse_from_date(from_date_time, to_date_time)

    if parsed_from_date == None and is_date(from_date_time) == False:
        return make_response(jsonify({
            "response": "error",
            "error": "Please insert 'fromDateTime' parameter correctly and inject the node again. "
                     "The parameter has to be in a string format (n-hour or n-day) or in a date and time format (yyyy-mm-ddThh:mm:ss)"
        }), 400)

    if parsed_from_date == None:
        parsed_from_date = datetime.strptime(from_date_time, "%Y-%m-%dT%H:%M:%S")

    if is_date(to_date_time) == False:
        return make_response(jsonify({
            "response": "error",
            "error": "Please insert 'to_date_time' parameter correctly and inject the node again. The parameter has to be in a date and time format (yyyy-mm-ddThh:mm:ss)"
        }), 400)

    parsed_to_date = datetime.strptime(to_date_time, date_format)

    if parsed_to_date <= parsed_from_date:
        return make_response(jsonify({
            "response": "error",
            "message": "Please insert 'from_date_time' and 'to_date_time' parameters correctly. 'to_date_time' must be later than 'from_date_time'."
        }), 400)

    if model not in ['IDW']:
        return make_response(jsonify({
            "response": "error",
            "message": "Invalid AI model. Choose either 'IDW'."
        }), 400)

    if model == 'IDW':
        response = heatmapIDW(
            city=city,
            long_min=long_min,
            long_max=long_max,
            lat_min=lat_min,
            lat_max=lat_max,
            epsg_projection=epsg_projection,
            value_types=value_types,
            subnature=subnature,
            scenario=scenario,
            color_map=color_map,
            from_date_time=from_date_time,
            to_date_time=to_date_time,
            token=token,
            heat_map_model_name=heat_map_model_name,
            clustered=clustered,
            file=file,
            broker=broker
        )

        return response


if __name__ == '__main__':
    app.run(host='0.0.0.0', port=8085)

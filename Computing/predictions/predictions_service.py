''' Snap4city Computing PREDICTIONS.
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

from flask import Flask, request, jsonify, make_response
from helper import get_sensor_real_time_data, ISO_to_datetime, date_time_to_ISO, parse_from_date, is_date
#from heatmap import heatmapIDW
from models.lstm.prediction import lstm
from prophet import Prophet
from datetime import timedelta, datetime
import pandas as pd 
import numpy as np
import os

app = Flask(__name__)

@app.route('/')
def hello_word():
    return 'hello_word'

@app.route('/predict', methods=['POST'])
def time_series():
    data = request.json

    required_fields = ['token', 'serviceUri', 'sensorType', 'aiModel', 'startDate', 'previsionRate', 'slotNumbers']
    missing_fields = [field for field in required_fields if field not in data]

    if missing_fields:
        return make_response(jsonify({
            "response": "error",
            "error": f"Missing fields: {', '.join(missing_fields)}"
        }), 400)
    
    token = data.get('token')
    service_uri = data.get('serviceUri')
    sensor_type = data.get('sensorType')
    ai_model = data.get('aiModel')
    start_date = data.get('startDate')
    prevision_rate = int(data.get('previsionRate'))
    slot_numbers = int(data.get('slotNumbers'))


    if ai_model not in ['Prophet', 'LSTM']:
        return make_response(jsonify({
            "response": "error",
            "error": "Invalid AI model. Choose either 'Prophet' or 'LSTM'."
        }), 400)

    if prevision_rate <= 0 or slot_numbers <= 0:
        return make_response(jsonify({
            "response": "error",
            "error": "previsionRate and slotNumbers must be positive integers."
        }), 400)
    

    try:
        start_date = ISO_to_datetime(start_date)
    except Exception as e:
        return make_response(jsonify({
            "response": "error",
            "error": f"Invalid startDate format: {str(e)}"
        }), 400)

    params = {
        "serviceUri" : service_uri,
        "accessToken": token,
        "fromTime": '2024-01-01T00:00:00', 
    }

    
    api_response = get_sensor_real_time_data(params)
    #print(api_response)

    if ('response' in api_response and api_response['response'] == 'error') or ('error' in api_response):
        return make_response(api_response, 400)
    
    
    bindings = api_response['realtime']['results']['bindings']
    
    date_observed = [
        item['dateObserved']['value'].replace('Z', '') for item in bindings
        if 'dateObserved' in item  and ISO_to_datetime(item['dateObserved']['value']) < start_date
    ]

    sensor_values = [
        float(item[sensor_type]['value']) for item in bindings
        if 'dateObserved' in item and ISO_to_datetime(item['dateObserved']['value']) < start_date
    ]

    predictions = []


    if ai_model == 'Prophet':
        data = {
            'date_observed': date_observed,
            'sensor_values': sensor_values
        }
        
        if len(date_observed) < 2 or len(sensor_values) < 2:
            error_response = jsonify({
                "response": "error",
                "message": "Not enough data points to make a prediction."
            })
            return make_response(error_response, 400)


        df = pd.DataFrame({'ds': data['date_observed'], 'y': data['sensor_values']})

        model = Prophet()
        model.fit(df)
        last_observed_date = pd.to_datetime(df['ds']).max()
        gap_periods = 0
        # If the start date is later than the last observed date, 
        # calculate the number of periods between them.
        if start_date > last_observed_date:
            gap_periods = int((start_date - last_observed_date).total_seconds() // (prevision_rate * 60))
           
        
        # Future dataframe should include the gap between the last observed date and start_date
        future = model.make_future_dataframe(periods=slot_numbers + gap_periods, freq=f"{prevision_rate}min")
        forecast = model.predict(future)
        
        # Select only the predictions from the start_date onwards
        result = forecast[['ds', 'yhat']].tail(slot_numbers + gap_periods).iloc[gap_periods:]
        result['ds'] = result['ds'].dt.strftime('%Y-%m-%dT%H:%M:%S%z')
        result = result.rename(columns={'ds': 'date', 'yhat': 'value'})
        predictions = result.to_dict('records')

    elif ai_model == 'LSTM':
        past_examples = len(date_observed)
        if past_examples < 100:
            error_response = jsonify({
                "response": "error",
                "error": "Not enough past examples"
            })
            return make_response(error_response, 400)

        last_date = start_date
        for _ in range(slot_numbers):
            past_data = np.array(sensor_values)[-100:]            
            predicted_value = lstm(past_data)[0]
            next_date = last_date + timedelta(minutes = prevision_rate )
            predictions.append({
                'date': date_time_to_ISO(next_date),
                'value': predicted_value
            })  
            last_date = next_date
            sensor_values.append(predicted_value)
    

    response = {
        "response" : "ok", 
        "result" : predictions
    }
    
    return jsonify(response)

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=8084)

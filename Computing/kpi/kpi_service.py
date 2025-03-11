'''
   Computing KPI
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

from flask import Flask, request, jsonify

from utils import kpi_service

app = Flask(__name__)

@app.route('/kpi', methods=['GET'])
def kpi():
    try:
        # Recupera i parametri dalla query string
        suri = request.args.get('SURI')
        date_observed_scenario = request.args.get('DATEOBSERVED_scenario')
        date_observed_ricostruzione = request.args.get('DATEOBSERVED_ricostruzione')
        access_token = request.args.get('ACCESS_TOKEN')

        # Esegui il servizio KPI
        tot_km, tot_veicoli, tot_running, consumo_tot, co2_tot, FRrs, FLrs, HErs, VHrs, funzionale_traffic_state = kpi_service(suri, date_observed_scenario, date_observed_ricostruzione, access_token)

        # Costruisci il risultato
        result = {
            'total kilometers [km]': f"{tot_km:.3f}",
            'number of vehicles [#]': f"{int(tot_veicoli) + (1 if tot_veicoli % 1 > 0 else 0)}",  # Arrotonda all'intero superiore
            'total travel time [s]': f"{int(tot_running) + (1 if tot_running % 1 > 0 else 0)}",  # Arrotonda all'intero superiore
            'Total fuel consumed [L]': f"{consumo_tot:.3f}",
            'Total CO2 emissions [ton]': f"{co2_tot / 1_000_000:.3f}",
            'Traffic state objective function [#]': f"{funzionale_traffic_state:.3f}"
        }

        # Restituisci i risultati come JSON
        return jsonify(result)

    except Exception as e:
        # Gestione di errori generici
        return jsonify({'error': 'An unexpected error occurred', 'details': str(e)}), 500

if __name__ == '__main__':
    app.run(host='0.0.0.0',port=8083)

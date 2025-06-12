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
import requests


#get request to the server
def get_request(url, params, header=None):
    try:
        if header.get('Authorization') == 'Bearer None':
            header.pop('Authorization')
        response = requests.get(url, headers=header, params=params)
        response.raise_for_status()
        json_response =  response.json()
        # 200 but not ok
        if 'status' in json_response and 'ok' not in json_response['status'].lower():
            return json_response['msg'], 409
        return json_response, 200
    except requests.exceptions.HTTPError as h_e:
        print(f"Http Error: {h_e}", h_e.response.status_code)
        return f"Http Error: {h_e}", h_e.response.status_code
    except requests.exceptions.ConnectionError as c_e:
        print(f"Error Connecting: {c_e}", 400)
        return f"Error Connecting: {c_e}", 400
    except requests.exceptions.Timeout as t_e:
        print(f"Timeout Error: {t_e}", 400)
        return f"Timeout Error: {t_e}", 400
    except requests.exceptions.RequestException as r_e:
        print(f"Some error occurred: {r_e}", 400)
        return f"Some error occurred: {r_e}", 400
    except Exception as e:
        json_response =  response.json()
        print(f"Unexpected error: {e} {json_response['msg']}", 400)
        return f"Unexpected error: {e} {json_response['msg']}", 400
    

#post request to the server
def post_request(url, header):
    try:
        response = requests.post(url, headers=header)
        response.raise_for_status()
        return response.text, response.status_code
    except requests.exceptions.HTTPError as h_e:
        print(f"Http Error: {h_e}", h_e.response.status_code)
        return f"Http Error: {h_e}", h_e.response.status_code
    except requests.exceptions.ConnectionError as c_e:
        print(f"Error Connecting: {c_e}", 400)
        return f"Error Connecting: {c_e}", 400
    except requests.exceptions.Timeout as t_e:
        print(f"Timeout Error: {t_e}", 400)
        return f"Timeout Error: {t_e}", 400
    except requests.exceptions.RequestException as r_e:
        print(f"Some error occurred: {r_e}", 400)
        return f"Some error occurred: {r_e}", 400
    except Exception as e: 
        print(f"Unexpected error: {e}", 400)
        return f"Unexpected error: {e}", 400
    

#patch request to the server
def patch_request(url, header, data):
    try:
        response = requests.patch(url, headers=header,data=data)
        response.raise_for_status()
        return response.text, response.status_code
    except requests.exceptions.HTTPError as h_e:
        print(f"Http Error: {h_e}", h_e.response.status_code)
        return f"Http Error: {h_e}", h_e.response.status_code
    except requests.exceptions.ConnectionError as c_e:
        print(f"Error Connecting: {c_e}", 400)
        return f"Error Connecting: {c_e}", 400
    except requests.exceptions.Timeout as t_e:
        print(f"Timeout Error: {t_e}", 400)
        return f"Timeout Error: {t_e}", 400
    except requests.exceptions.RequestException as r_e:
        print(f"Some error occurred: {r_e}", 400)
        return f"Some error occurred: {r_e}", 400
    except Exception as e: 
        print(f"Unexpected error: {e}", 400)
        return f"Unexpected error: {e}", 400
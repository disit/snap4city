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
from http_methods import get_request
import os


def check_ownership_by_id(config, token, id, organization, contextbroker):
    base_url_var = 'base_url'
    if 'BASE_URL' in config:
         base_url_var = config['BASE_URL']
    base_url = os.getenv('BASE_URL', base_url_var)

    sm_url_var = 'sm_url'
    if 'SERVICEMAP_URL' in config:
         sm_url_var = config['SERVICEMAP_URL']
    sm_url_var = os.getenv('SERVICEMAP_URL', base_url_var)

    url = base_url + os.getenv('SERVICEMAP_URL', sm_url_var)
    header={
        'Authorization': f"Bearer {token}",
        'Content-Type': 'application/json'
    }
    params={
        'serviceUri': os.getenv('SERVICE_URI', config['SERVICE_URI'])+f"{contextbroker}/{organization}/{id}",
    }
    return get_request(url, params, header)
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

from http_methods import post_request
import os

# get token from request
# return token, message error and status code
def get_token(request):
    try:
        bearer = request.headers.get('Authorization')
        token = bearer.split()[1]
        return token, None, 200
    except (AttributeError, IndexError):  # No token
            return None, 'Access Token not present', 401

# check if the token is a valid snap4city token
def is_valid_token(config, token):
    # build the userinfo call
    header = {
        "Content-Type": "application/json",
        "Authorization": f"Bearer {token}"
    }
    base_url_var = 'default_base_url'
    if 'base_url' in config:
         base_url_var = config['base_url']
    base_url = os.getenv('BASE_URL', base_url_var)
    
    user_info_var = 'default_user_info'
    if 'user_info' in config:
         user_info_var = config['user_info']
    url = base_url + os.getenv('USER_INFO', user_info_var)

    return post_request(url, header)

#basic authentication
def basic_auth(config, request):
    token, message, status = get_token(request)
    if status != 200:
        print('message: ',message, 'status: ', status)
        return None, message, status
    message, status = is_valid_token(config, token)
    if status != 200:
        print('message: ',message, 'status: ', status)
        return None, message, status
    return token, None, 200
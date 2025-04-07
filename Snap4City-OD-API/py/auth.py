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
def is_valid_token(url_conf, token):
    # build the userinfo call
    header = {
        "Content-Type": "application/json",
        "Authorization": f"Bearer {token}"
    }
    base_url = os.getenv('BASE_URL')
    url = base_url + url_conf['user_info']

    return post_request(url, header)

#basic authentication
def basic_auth(url_conf, request):
    token, message, status = get_token(request)
    if status != 200:
        return None, message, status
    message, status = is_valid_token(url_conf, token)
    if status != 200:
        return None, message, status
    return token, None, 200
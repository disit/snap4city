import mysql.connector
import json
import datetime
import traceback
from starlette.applications import Starlette
from starlette.responses import Response, JSONResponse
from starlette.routing import Route
import random, string, os, requests
import time
import jwt

from jwt import PyJWKClient
url = os.getenv('certpath')
jwks_client = PyJWKClient(url)
app = Starlette()


db_conn_info = {
    "user": os.getenv('userdb') ,
    "passwd": os.getenv('passworddb'),
    "host": os.getenv('hostdb'),
    "port": os.getenv('portdb'),
    "database": os.getenv('database'),
    "auth_plugin": 'mysql_native_password'
}

import requests

def check_keycloak_token(access_token_sent):
    try:
        
        signing_key = jwks_client.get_signing_key_from_jwt(access_token_sent)

        decoded = jwt.decode(access_token_sent, signing_key.key, algorithms=["RS256"], options={"verify_aud": False})

        return decoded, 200
    except:
        print(f"Request failed: {traceback.format_exc()}")
        return False, 500
    
def ip_to_int(ip_str):
    """Convert dotted IPv4 string to 32-bit int."""
    octets = [int(o) for o in ip_str.split(".")]
    return (octets[0] << 24) | (octets[1] << 16) | (octets[2] << 8) | octets[3]

def check_ip(address: str, network: str):
    if address == network: # network accepts only exactly this address, using no "/" mask
        return True
    
    if "/" not in network:
        return False # this is not exactly the intended ip

    ip_str, prefix_str = network.split("/")
    prefix_len = int(prefix_str)

    # Convert both to int
    ip_int = ip_to_int(ip_str)
    other_int = ip_to_int(address)

    # Build netmask
    netmask = (0xFFFFFFFF << (32 - prefix_len)) & 0xFFFFFFFF

    # Compare network parts
    return (ip_int & netmask) == (other_int & netmask)

async def hello(request):
    return Response("Hello World!", status_code=200)
app.routes.append(Route("/hello", hello))

async def printme(request):
    print(await request.body())
    print(request.base_url)
    print(dict(request.headers))
    return Response("k", status_code=200)
app.routes.append(Route("/printme", printme))

@app.route('/save', methods=["POST"])
async def save(request):
    with mysql.connector.connect(**db_conn_info) as conn:
        cursor = conn.cursor(buffered=True)
        body = await request.json() # Asynchronously parse JSON body
        headers = dict(request.headers)  # Convert headers to a dictionary
        thisdict = {
            "body": body,
            "headers": headers
        }
        try:
            query = '''INSERT INTO `requests` (`result`,`extracted_id`) VALUES (%s,%s);'''
            cursor.execute(query, (str(thisdict),str(thisdict["body"]["request"]["headers"]["x-checkme-id"])))
            conn.commit()
            return Response("Request saved", status_code=200)
        except Exception:
            print(traceback.format_exc())
            query = '''INSERT INTO `requests` (`result`,`extracted_id`) VALUES (%s, NULL);'''
            cursor.execute(query, (str(thisdict),))
            conn.commit()
            return Response("Request saved, no id", status_code=200)
app.routes.append(Route("/save", save, methods=["POST"]))


async def timeout(requests):
    # Simulate a long-running process
    print("Begin slow")
    time.sleep(40)
    return Response("This was long, supposedly")

app.routes.append(Route("/timeout", timeout, methods=["GET"]))
   
def format_headers(headers_str):
    headers = {}
    for line in headers_str.strip().split("\n"):
        key, value = line.split(": ", 1)
        try:
            value = int(value)  # Convert numerical values if applicable
        except ValueError:
            pass
        headers[key] = value
    return json.dumps(headers)

@app.route('/checkme')
async def checkvalidity(request):
    with mysql.connector.connect(**db_conn_info) as conn:
        cursor = conn.cursor(buffered=True)
        try:
            randid = ''.join(random.choices(string.ascii_lowercase + string.ascii_uppercase + string.digits, k=32))
            print(f"Headers: {request.headers}")
            userinfodict, code = check_keycloak_token(dict(request.headers)["authorization"][7:])
            if not userinfodict:
                query_f = "INSERT INTO timedaccess (`user`,`resource`,`result`,`extracted_id`) VALUES (%s,%s,%s,%s);"
                cursor.execute(query_f, (None,None,"{'result':'Request not forwarded with Kong', 'errors':'Failed to retrieve userinfo from token', 'headers':'"+format_headers(str(request.headers))+"', 'path':'"+request.headers['x-original-request']+"'}",randid))
                conn.commit()
                return Response("Failed to retrieve userinfo from token, ensure that the token is valid and has its userinfo available", status_code=500)
            username=""
            if "preferred_username" in userinfodict.keys():     #usually the name is here
                username = userinfodict["preferred_username"]
            elif "username" in userinfodict.keys():             #if it wasn't there, maybe it's here: this is true for tokens from nodered
                username = userinfodict["username"]
            else:                                               #at this point we assume it isn't here at all
                raise KeyError
            if username:
            
                query = '''SELECT ratelimit.*,operative_apitable.apiexternalurl FROM ratelimit JOIN operative_apitable ON ratelimit.resource = operative_apitable.idapi WHERE %s LIKE CONCAT(apiexternalurl, '%') AND user=%s AND operative_apitable.apistatus="active" ORDER BY LENGTH(apiexternalurl) DESC limit 1;'''
                cursor.execute(query, (request.headers['x-original-request'],username,))
                conn.commit()
                result = cursor.fetchone()

                if not result:
                    query_f = "INSERT INTO timedaccess (`user`,`resource`,`result`,`extracted_id`) VALUES (%s,%s,%s,%s);"
                    cursor.execute(query_f, (username,None,"{'result':'Request not forwarded with Kong', 'errors':'No rule found for user and resource', 'headers':'"+format_headers(str(request.headers))+"', 'path':'"+request.headers['x-original-request']+"'}",randid))
                    conn.commit()
                    return Response("Rule not found for this user/path", status_code=403)
                # checking origin
                try:
                    if result[-2] != "":
                        ip_to_match = request.headers['x-forwarded-for'].split(', ')[0]
                        if not check_ip(address=ip_to_match,network=result[-2]):
                            query_f = "INSERT INTO timedaccess (`user`,`resource`,`result`,`extracted_id`) VALUES (%s,%s,%s,%s);"
                            cursor.execute(query_f, (username,None,"{'result':'Request not forwarded with Kong', 'errors':'This connection is not allowed from the source ip', 'headers':'"+format_headers(str(request.headers))+"', 'path':'"+request.headers['x-original-request']+"'}",randid))
                            conn.commit()
                            return Response("This connection is not allowed from the source ip", status_code=403)
                except Exception:
                    print(f"Rule matched: {str(result)}, ip detected: {request.headers['x-forwarded-for'].split(', ')[0]}")
                    query_f = "INSERT INTO timedaccess (`user`,`resource`,`result`,`extracted_id`) VALUES (%s,%s,%s,%s);"
                    cursor.execute(query_f, (username,None,"{'result':'Request not forwarded with Kong', 'errors':'Issues while determining if origin ip is allowed.', 'headers':'"+format_headers(str(request.headers))+"', 'path':'"+request.headers['x-original-request']+"'}",randid))
                    conn.commit()
                    return Response("Issues while determining if origin ip is allowed: "+traceback.format_exc(), status_code=403)
                # end check origin
                right_now=datetime.datetime.now()
                if result[4] < right_now < result[5]:
                    pass
                else:
                    query_f = "INSERT INTO timedaccess (`user`,`resource`,`result`,`extracted_id`) VALUES (%s,%s,%s,%s);"
                    cursor.execute(query_f, (username,result[1],"{'result':'Request not forwarded with Kong', 'errors':'Rule is not valid at the current time', 'headers':'"+format_headers(str(request.headers))+"'}",randid))
                    conn.commit()
                    return Response("outside rule time validity", status_code=401)
                if list(result[2])[0] == "TotalAccesses":
                    query_2 = "SELECT sum(amount) FROM (SELECT count(*) as amount FROM timedaccess WHERE resource = %s AND user = %s AND `request_ok`= 1 UNION SELECT sum(total_requests) AS amount FROM timedaccess_summary WHERE resource = %s AND user = %s) alias;"
                    cursor.execute(query_2, (result[1],username,result[1], username))
                    conn.commit()
                    result_2 = cursor.fetchone()
                    if result_2[0] < json.loads(result[3])["amount"]:
                        query_3 = "INSERT INTO timedaccess (`user`,`resource`,`result`,`extracted_id`,`request_ok`) VALUES (%s,%s,%s,%s,1);"
                        cursor.execute(query_3, (username,result[1],"{'result':'Request forwarded with Kong', 'errors':'', 'headers':'"+format_headers(str(request.headers))+"'}",randid))
                        conn.commit()
                        return JSONResponse({"id":randid}, status_code=200)
                    else:
                        query_3 = "INSERT INTO timedaccess (`user`,`resource`,`result`,`extracted_id`) VALUES (%s,%s,%s,%s);"
                        cursor.execute(query_3, (username,result[1],"{'result':'Request not forwarded with Kong', 'errors':'Used more than "+str(json.loads(result[3])["amount"])+" requests', 'headers':'"+format_headers(str(request.headers))+"'}",randid))
                        conn.commit()
                        return Response("too many requests", status_code=429)
                elif list(result[2])[0] == "AccessesOverTime":
                    query_2_1 = "SELECT sum(amount) FROM ("
                    query_2_2_a = "SELECT count(*) as amount FROM timedaccess WHERE resource = %s AND user = %s AND `request_ok`= 1 "
                    query_2_2_b = " SELECT sum(total_requests) AS amount FROM timedaccess_summary WHERE resource = %s AND user = %s "
                    period = json.loads(result[3])["period"]
                    if period == 3:
                        query_2_3 = "AND YEAR(`beginaccess`) = YEAR(CURDATE()) "
                        query_2_3_a = "AND YEAR(`access_day`) = YEAR(CURDATE()) "
                    elif period == 2:
                        query_2_3 = "AND YEAR(`beginaccess`) = YEAR(CURDATE()) AND MONTH(`beginaccess`) = MONTH(CURDATE()) "
                        query_2_3_a = "AND YEAR(`access_day`) = YEAR(CURDATE()) AND MONTH(`access_day`) = MONTH(CURDATE()) "
                    elif period == 1:
                        query_2_3 = "AND YEAR(`beginaccess`) = YEAR(CURDATE()) AND WEEK(`beginaccess`, 1) = WEEK(CURDATE(), 1) "
                        query_2_3_a = "AND YEAR(`access_day`) = YEAR(CURDATE()) AND WEEK(`access_day`, 1) = WEEK(CURDATE(), 1) "
                    elif period == 0:
                        query_2_3 = "AND DATE(`beginaccess`) = CURDATE()"
                        query_2_3_a = "AND DATE(`access_day`) = CURDATE()"
                    else:
                        return Response("invalid interval", status_code=500)
                    query_2 = query_2_1 + query_2_2_a + query_2_3 + "UNION" + query_2_2_b + query_2_3_a + ") alias"
                    cursor.execute(query_2, (result[1],username,result[1],username,))
                    conn.commit()
                    result_2 = cursor.fetchone()
                    if result_2[0] < json.loads(result[3])["amount"]:
                        query_3 = "INSERT INTO timedaccess (`user`,`resource`,`result`,`extracted_id`,`request_ok`) VALUES (%s,%s,%s,%s,1);"
                        cursor.execute(query_3, (username,result[1],"{'result':'Request forwarded with Kong', 'errors':'', 'headers':'"+format_headers(str(request.headers))+"'}",randid))
                        conn.commit()
                        return JSONResponse({"id":randid}, status_code=200)
                    else:
                        query_3 = "INSERT INTO timedaccess (`user`,`resource`,`result`,`extracted_id`) VALUES (%s,%s,%s,%s);"
                        cursor.execute(query_3, (username,result[1],"{'result':'Request not forwarded with Kong', 'errors':'Used more than "+str(json.loads(result[3])["amount"])+" requests in given timeframe', 'headers':'"+format_headers(str(request.headers))+"'}",randid))
                        conn.commit()
                        return Response("too many requests", status_code=429)
                elif list(result[2])[0] == "ContemporaryAccess":
                    query_2 = "SELECT count(*) FROM timedaccess JOIN requests ON requests.extracted_id = timedaccess.extracted_id AND resource = %s AND user = %s AND requests.endaccess = 0 "
                    cursor.execute(query_2, (result[1],username,))
                    conn.commit()
                    result_2 = cursor.fetchone()
                    if result_2[0] < json.loads(result[3])["amount"]:
                        query_3 = "INSERT INTO timedaccess (`user`,`resource`,`result`,`extracted_id`,`request_ok`) VALUES (%s,%s,%s,%s,1);"
                        cursor.execute(query_3, (username,result[1],"{'result':'Request forwarded with Kong', 'errors':'', 'headers':'"+format_headers(str(request.headers))+"'}",randid))
                        conn.commit()
                        return JSONResponse({"id":randid}, status_code=200)
                    else:
                        query_3 = "INSERT INTO timedaccess (`user`,`resource`,`result`,`extracted_id`) VALUES (%s,%s,%s,%s);"
                        cursor.execute(query_3, (username,result[1],"{'result':'Request not forwarded with Kong', 'errors':'Used more than "+str(json.loads(result[3])["amount"])+" concurrent requests', 'headers':'"+format_headers(str(request.headers))+"'}",randid))
                        conn.commit()
                        return Response("too many requests", status_code=429)
                else:
                    query_f = "INSERT INTO timedaccess (`user`,`resource`,`result`,`extracted_id`) VALUES (%s,%s,%s,%s);"
                    cursor.execute(query_f, (username,result[1],"{'result':'Request not forwarded with Kong', 'errors':'Illegal rule', 'headers':'"+format_headers(str(request.headers))+"'}",randid))
                    conn.commit()
                    return Response("Illegal rule", status_code=503)
        # unauthenticated user
        except KeyError:
            username = "anonymous"
            query = '''SELECT ratelimit.*,operative_apitable.apiexternalurl FROM ratelimit JOIN operative_apitable ON ratelimit.resource = operative_apitable.idapi WHERE %s LIKE CONCAT(apiexternalurl, '%') AND user=%s AND operative_apitable.apistatus="active" ORDER BY LENGTH(apiexternalurl) DESC limit 1;'''
            cursor.execute(query, (request.headers['x-original-request'],username,))
            conn.commit()
            result = cursor.fetchone()
            if not result:
                query_f = "INSERT INTO timedaccess (`user`,`resource`,`result`,`extracted_id`) VALUES (%s,%s,%s,%s);"
                cursor.execute(query_f, (username,None,"{'result':'Request not forwarded with Kong', 'errors':'No rule found for user and resource', 'headers':'"+format_headers(str(request.headers))+"', 'path':'"+request.headers['x-original-request']+"'}",randid))
                conn.commit()
                return Response("Rule not found for anonymous on this path", status_code=403)
            right_now=datetime.datetime.now()
            # checking origin
            try:
                if result[-2] != "":
                    ip_to_match = request.headers['x-forwarded-for'].split(', ')[0]
                    if not check_ip(address=ip_to_match,network=result[-2]):
                        query_f = "INSERT INTO timedaccess (`user`,`resource`,`result`,`extracted_id`) VALUES (%s,%s,%s,%s);"
                        cursor.execute(query_f, (username,None,"{'result':'Request not forwarded with Kong', 'errors':'This connection is not allowed from the source ip', 'headers':'"+format_headers(str(request.headers))+"', 'path':'"+request.headers['x-original-request']+"'}",randid))
                        conn.commit()
                        return Response("This connection is not allowed from the source ip", status_code=403)
            except Exception:
                print(f"Rule matched: {str(result)}, ip detected: {request.headers['x-forwarded-for'].split(', ')[0]}")
                query_f = "INSERT INTO timedaccess (`user`,`resource`,`result`,`extracted_id`) VALUES (%s,%s,%s,%s);"
                cursor.execute(query_f, (username,None,"{'result':'Request not forwarded with Kong', 'errors':'Issues while determining if origin ip is allowed.', 'headers':'"+format_headers(str(request.headers))+"', 'path':'"+request.headers['x-original-request']+"'}",randid))
                conn.commit()
                return Response("Issues while determining if origin ip is allowed: "+str(traceback.format_exc()), status_code=403)
            # end check origin
            if result[4] < right_now < result[5]:
                pass
            else:
                query_f = "INSERT INTO timedaccess (`user`,`resource`,`result`,`extracted_id`) VALUES (%s,%s,%s,%s);"
                cursor.execute(query_f, (username,result[1],"{'result':'Request not forwarded with Kong', 'errors':'Rule is not valid at the current time', 'headers':'"+format_headers(str(request.headers))+"'}",randid))
                conn.commit()
                return Response("outside rule time validity", status_code=401)
            if list(result[2])[0] == "TotalAccesses":
                query_2 = "SELECT sum(amount) FROM (SELECT count(*) as amount FROM timedaccess WHERE resource = %s AND user = %s AND `request_ok`= 1 UNION SELECT sum(total_requests) AS amount FROM timedaccess_summary WHERE resource = %s AND user = %s) alias;"
                cursor.execute(query_2, (result[1],username,result[1],username,))
                conn.commit()
                result_2 = cursor.fetchone()
                if result_2[0] < json.loads(result[3])["amount"]:
                    query_3 = "INSERT INTO timedaccess (`user`,`resource`,`result`,`extracted_id`,`request_ok`) VALUES (%s,%s,%s,%s,1);"
                    cursor.execute(query_3, (username,result[1],"{'result':'Request forwarded with Kong', 'errors':'', 'headers':'"+format_headers(str(request.headers))+"'}",randid))
                    conn.commit()
                    return JSONResponse({"id":randid}, status_code=200)
                else:
                    query_3 = "INSERT INTO timedaccess (`user`,`resource`,`result`,`extracted_id`) VALUES (%s,%s,%s,%s);"
                    cursor.execute(query_3, (username,result[1],"{'result':'Request not forwarded with Kong', 'errors':'Used more than "+str(json.loads(result[3])["amount"])+" requests', 'headers':'"+format_headers(str(request.headers))+"'}",randid))
                    conn.commit()
                    return Response("too many requests", status_code=429)
            elif list(result[2])[0] == "AccessesOverTime":
                query_2_1 = "SELECT sum(amount) FROM ("
                query_2_2_a = "SELECT count(*) as amount FROM timedaccess WHERE resource = %s AND user = %s AND `request_ok`= 1 "
                query_2_2_b = " SELECT sum(total_requests) AS amount FROM timedaccess_summary WHERE resource = %s AND user = %s "
                period = json.loads(result[3])["period"]
                if period == 3:
                    query_2_3 = "AND YEAR(`beginaccess`) = YEAR(CURDATE()) "
                    query_2_3_a = "AND YEAR(`access_day`) = YEAR(CURDATE()) "
                elif period == 2:
                    query_2_3 = "AND YEAR(`beginaccess`) = YEAR(CURDATE()) AND MONTH(`beginaccess`) = MONTH(CURDATE()) "
                    query_2_3_a = "AND YEAR(`access_day`) = YEAR(CURDATE()) AND MONTH(`access_day`) = MONTH(CURDATE()) "
                elif period == 1:
                    query_2_3 = "AND YEAR(`beginaccess`) = YEAR(CURDATE()) AND WEEK(`beginaccess`, 1) = WEEK(CURDATE(), 1) "
                    query_2_3_a = "AND YEAR(`access_day`) = YEAR(CURDATE()) AND WEEK(`access_day`, 1) = WEEK(CURDATE(), 1) "
                elif period == 0:
                    query_2_3 = "AND DATE(`beginaccess`) = CURDATE()"
                    query_2_3_a = "AND DATE(`access_day`) = CURDATE()"
                else:
                    return Response("invalid interval", status_code=500)
                query_2 = query_2_1 + query_2_2_a + query_2_3 + "UNION" + query_2_2_b + query_2_3_a + ") alias"
                cursor.execute(query_2, (result[1],username,result[1],username,))
                conn.commit()
                result_2 = cursor.fetchone()
                if result_2[0] < json.loads(result[3])["amount"]:
                    query_3 = "INSERT INTO timedaccess (`user`,`resource`,`result`,`extracted_id`,`request_ok`) VALUES (%s,%s,%s,%s,1);"
                    cursor.execute(query_3, (username,result[1],"{'result':'Request forwarded with Kong', 'errors':'', 'headers':'"+format_headers(str(request.headers))+"'}",randid))
                    conn.commit()
                    return JSONResponse({"id":randid}, status_code=200)
                else:
                    query_3 = "INSERT INTO timedaccess (`user`,`resource`,`result`,`extracted_id`) VALUES (%s,%s,%s,%s);"
                    cursor.execute(query_3, (username,result[1],"{'result':'Request not forwarded with Kong', 'errors':'Used more than "+str(json.loads(result[3])["amount"])+" requests in given timeframe', 'headers':'"+format_headers(str(request.headers))+"'}",randid))
                    conn.commit()
                    return Response("too many requests", status_code=429)
            elif list(result[2])[0] == "ContemporaryAccess":
                query_2 = "SELECT count(*) FROM timedaccess JOIN requests ON requests.extracted_id = timedaccess.extracted_id AND resource = %s AND user = %s AND requests.endaccess = 0 AND `request_ok`= 1;"
                cursor.execute(query_2, (result[1],username,))
                conn.commit()
                result_2 = cursor.fetchone()
                if result_2[0] < json.loads(result[3])["amount"]:
                    query_3 = "INSERT INTO timedaccess (`user`,`resource`,`result`,`extracted_id`,`request_ok`) VALUES (%s,%s,%s,%s,1);"
                    cursor.execute(query_3, (username,result[1],"{'result':'Request forwarded with Kong', 'errors':'', 'headers':'"+format_headers(str(request.headers))+"'}",randid))
                    conn.commit()
                    return JSONResponse({"id":randid}, status_code=200)
                else:
                    query_3 = "INSERT INTO timedaccess (`user`,`resource`,`result`,`extracted_id`) VALUES (%s,%s,%s,%s);"
                    cursor.execute(query_3, (username,result[1],"{'result':'Request not forwarded with Kong', 'errors':'Used more than "+str(json.loads(result[3])["amount"])+" concurrent requests', 'headers':'"+format_headers(str(request.headers))+"'}",randid))
                    conn.commit()
                    return Response("too many requests", status_code=429)
            else:
                query_f = "INSERT INTO timedaccess (`user`,`resource`,`result`,`extracted_id`) VALUES (%s,%s,%s,%s);"
                cursor.execute(query_f, (username,result[1],"{'result':'Request not forwarded with Kong', 'errors':'Illegal rule', 'headers':'"+format_headers(str(request.headers))+"'}",randid))
                conn.commit()
                return Response("Illegal rule", status_code=503)
        except Exception as E:
            print("did not find a token or something broke:",str(traceback.format_exc()))
            return Response("Error in checking validity: "+str(traceback.format_exc()), status_code=500)
        

app.routes.append(Route("/checkme", checkvalidity, methods=["POST", "GET"]))
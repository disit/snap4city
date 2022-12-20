/* Snap4RTSP.
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
   along with this program.  If not, see <http://www.gnu.org/licenses/>. */
   
/*
 * (C) Copyright 2014-2015 Kurento (http://kurento.org/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

var path = require('path');
var url = require('url');
var cookieParser = require('cookie-parser')
var express = require('express');
var session = require('express-session')
var minimist = require('minimist');
var ws = require('ws');
var kurento = require('kurento-client');
var fs = require('fs');
var https = require('https');
var axios = require('axios');
var mysql = require("mysql");

var config = JSON.parse(fs.readFileSync('conf/config.json'));
var argv = minimist(process.argv.slice(2), {
    default: {
        as_uri: process.env.as_uri || 'https://localhost:8443/',
        ws_uri: process.env.ws_uri || config['ws_uri'] || 'ws://localhost:8888/kurento'
    }
});

var db_connection;

function handleDbDisconnect() {

    db_connection = mysql.createConnection(config.db); 			// Recreate the connection, since the old one cannot be reused.

    db_connection.connect(function (err) { 							// The server is either down
        if (err) { 												// or restarting (takes a while sometimes).
            console.log('error when connecting to db:', err);
            setTimeout(handleDbDisconnect, 2000); 				// We introduce a delay before attempting to reconnect,
        }                                     					// to avoid a hot loop, and to allow our node script to
    });                                     					// process asynchronous requests in the meantime.
    // If you're also serving http, display a 503 error.
    db_connection.on('error', function (err) {
        console.log('db error', err);								// Connection to the MySQL server is usually
        if (err.code === 'PROTOCOL_CONNECTION_LOST' || err.fatal) { 	// lost due to either server restart, or a
            handleDbDisconnect();                         			// connnection idle timeout (the wait_timeout
        } 															// server variable configures this)									
    });

}

handleDbDisconnect();

var options =
{
    key: fs.readFileSync('keys/server.key'),
    cert: fs.readFileSync('keys/server.crt')
};

var app = express();

/*
 * Management of sessions
 */
app.use(cookieParser());

var sessionHandler = session({
    secret: 'none',
    rolling: true,
    resave: true,
    saveUninitialized: true
});

app.use(sessionHandler);

/*
 * Definition of global variables.
 */
var sessions = {};
var candidatesQueue = {};
var kurentoClient = null;

/*
 * Server startup
 */
var asUrl = url.parse(argv.as_uri);
var port = asUrl.port;
var server = https.createServer(options, app).listen(port, function () {
    console.log('SNAP4RTSP started');
    console.log('Open ' + url.format(asUrl) + ' with a WebRTC capable browser');
    console.log('using kurento via ' + argv.ws_uri);
    console.log(config);
});

//TOBE DONE: handle multiple sessions with the same client browser

var wss = new ws.Server({
    server: server,
    path: '/ws'
});

function now() {
    return new Date().toISOString() + " ";
}
/*
 * Management of WebSocket messages
 */
wss.on('connection', function (ws, req) {
    var sessionId = null;
    var request = req;
    var response = {
        writeHead: {}
    };

    sessionHandler(request, response, function (err) {
        sessionId = request.session.id;
        console.log(now() + 'Connection received with sessionId ' + sessionId);
    });

    ws.on('error', function (error) {
        console.log(now() + 'Connection ' + sessionId + ' error');
        stop(sessionId);
    });

    ws.on('close', function () {
        console.log(now() + 'Connection ' + sessionId + ' closed');
        stop(sessionId);
    });

    ws.on('message', function (_message) {
        try {
            var message = JSON.parse(_message);
        } catch (e) {
            console.log(now() + 'Connection ' + sessionId + ' failed parsing ' + _message);
            return;
        }
        console.log(now() + 'Connection ' + sessionId + ' received message ', message);

        switch (message.id) {
            case 'start':
                sessionId = request.session.id;
                start(sessionId, message, ws, message.sdpOffer, function (error, sdpAnswer) {
                    if (error) {
                        return ws.send(JSON.stringify({
                            id: 'error',
                            message: error
                        }));
                    }
                    ws.send(JSON.stringify({
                        id: 'startResponse',
                        sdpAnswer: sdpAnswer
                    }));
                });
                break;

            case 'stop':
                stop(sessionId);
                break;

            case 'onIceCandidate':
                onIceCandidate(sessionId, message.candidate);
                break;

            default:
                ws.send(JSON.stringify({
                    id: 'error',
                    message: 'Invalid message ' + message
                }));
                break;
        }

    });
});

/*
 * Definition of functions
 */

// Recover kurentoClient for the first time.
function getKurentoClient(callback) {
    if (kurentoClient !== null) {
        return callback(null, kurentoClient);
    }

    kurento(argv.ws_uri, function (error, _kurentoClient) {
        if (error) {
            console.log("Could not find media server at address " + argv.ws_uri);
            return callback("Could not find media server at address" + argv.ws_uri
                + ". Exiting with error " + error);
        }

        kurentoClient = _kurentoClient;
        callback(null, kurentoClient);
    });
}

function retrieveRtspUrl(message, callback) {
    var opts = {};
    if (message.accessToken)
        opts = { "headers": { "Authorization": "Bearer " + message.accessToken } };
    axios.get(config.servicemapApiUrl + "?serviceUri=" + message.rtspId, opts)
        .then(response => {
            console.log(response.data);

            var rtspUrl = response.data.realtime.results.bindings[0].videoSource.value;
            var name = response.data.realtime.results.bindings[0].name.value;
            if (!rtspUrl) {
                return callback('invalid rtsp url');
            }
            if (!name) {
                return callback('invalid rtsp name');
            }

            db_connection.query("SELECT * FROM Dashboard.camdata WHERE name='" + name + "'",
                function (error, results, fields) {
                    if (error) {
                        console.log(error);
                        callback("db error");
                    } else {
                        console.log(results);
                        if (results.length > 0) {
                            let username = results[0]["username"];
                            let pwd = results[0]["password"];
                            if (username && pwd) {
                                rtspUrl = rtspUrl.replace("rtsp://", "rtsp://" + username + ":" + pwd + "@");
                            }
                            callback(null, rtspUrl);
                        } else {
                            callback("no credentials on db for " + name);
                        }
                    }
                });
        })
        .catch(error => {
            console.log(error)
            if (error.response) {
                if (error.response.status == 400)
                    return callback('wrong serviceUri');
                else if (error.response.status == 401)
                    return callback('access denied need authentication');
                else if (error.response.status == 403)
                    return callback('access denied');
                callback('failed request to servicemap ' + error.response.status);
            }
            callback('failure getting rtsp url');
        });
}

function start(sessionId, message, ws, sdpOffer, callback) {
    if (!sessionId) {
        return callback('Cannot use undefined sessionId');
    }
    retrieveRtspUrl(message, function (error, rtspUrl) {
        if (error) {
            return callback(error);
        }
        getKurentoClient(function (error, kurentoClient) {
            if (error) {
                return callback(error);
            }

            kurentoClient.create('MediaPipeline', function (error, pipeline) {
                if (error) {
                    return callback(error);
                }

                pipeline.create("PlayerEndpoint", { uri: rtspUrl }, function (error, player) {
                    if (error) {
                        pipeline.release();
                        return callback(error);
                    }
                    pipeline.create("WebRtcEndpoint", function (error, webRtcEndpoint) {
                        if (error) {
                            pipeline.release();
                            return callback(error);
                        }
                        //setIceCandidateCallbacks(webRtcEndpoint, webRtcPeer, onError);
                        if (candidatesQueue[sessionId]) {
                            while (candidatesQueue[sessionId].length) {
                                var candidate = candidatesQueue[sessionId].shift();
                                webRtcEndpoint.addIceCandidate(candidate);
                            }
                        }

                        webRtcEndpoint.on('OnIceCandidate', function (event) {
                            var candidate = kurento.getComplexType('IceCandidate')(event.candidate);
                            ws.send(JSON.stringify({
                                id: 'iceCandidate',
                                candidate: candidate
                            }));
                        });

                        webRtcEndpoint.processOffer(sdpOffer, function (error, sdpAnswer) {
                            if (error) {
                                pipeline.release();
                                return callback(error);
                            }
                            sessions[sessionId] = {
                                'pipeline': pipeline,
                                'webRtcEndpoint': webRtcEndpoint
                            }
                            //webRtcEndpoint.gatherCandidates(onError);

                            return callback(null, sdpAnswer);
                        });
                        webRtcEndpoint.gatherCandidates(function (error) {
                            if (error) {
                                pipeline.release();
                                return callback(error);
                            }
                        });

                        player.connect(webRtcEndpoint, function (error) {
                            if (error) {
                                pipeline.release();
                                return callback(error);
                            }
                            console.log(now() + "PlayerEndpoint-->WebRtcEndpoint connection established");
                            player.play(function (error) {
                                if (error) {
                                    pipeline.release();
                                    return callback(error);
                                }
                                console.log(now() + "Player playing " + rtspUrl + "...");
                            });
                        });
                    });
                });
            });
        });
    });
}

function stop(sessionId) {
    if (sessions[sessionId]) {
        var pipeline = sessions[sessionId].pipeline;
        console.info('Releasing pipeline');
        pipeline.release();

        delete sessions[sessionId];
        delete candidatesQueue[sessionId];
    }
}

function onIceCandidate(sessionId, _candidate) {
    var candidate = kurento.getComplexType('IceCandidate')(_candidate);

    if (sessions[sessionId]) {
        console.info('Sending candidate');
        var webRtcEndpoint = sessions[sessionId].webRtcEndpoint;
        webRtcEndpoint.addIceCandidate(candidate);
    }
    else {
        console.info('Queueing candidate');
        if (!candidatesQueue[sessionId]) {
            candidatesQueue[sessionId] = [];
        }
        candidatesQueue[sessionId].push(candidate);
    }
}

app.use(express.static(path.join(__dirname, 'static')));

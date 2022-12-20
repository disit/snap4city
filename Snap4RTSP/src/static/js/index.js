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
   
var ws = new WebSocket(conf.snap4rtspWsUrl);
var videoInput;
var videoOutput;
var webRtcPeer;
var state = null;
const urlParams = new URLSearchParams(window.location.search);
const rtspSRC = urlParams.get('src');

const I_CAN_START = 0;
const I_CAN_STOP = 1;
const I_AM_STARTING = 2;

setInterval(function () {
	ws.send('ping');
	console.log("ping!");
}, 40000);

var keycloak = new Keycloak(conf.keycloak);
var accessToken = "";
var username = "public";

window.onload = function () {
	videoInput = document.getElementById('videoInput');
	videoOutput = document.getElementById('videoOutput');
	setState(I_CAN_START);
	if (rtspSRC != null) {
		keycloak.init({
			onLoad: 'check-sso'
		}).success(
			function (authenticated) {
				if (authenticated) {
					console.log("AUTHENTICATED");
					accessToken = keycloak.token;
					username = keycloak.tokenParsed.preferred_username;
				} else {
					console.log("Not Authenticated..");
				}
				setTimeout(function () { start() }, 1000);
			}).error(function (err) {
				console.log(err);
			});
	}
}

window.onbeforeunload = function () {
	ws.close();
}

ws.onmessage = function (message) {
	var parsedMessage = JSON.parse(message.data);
	console.info('Received message: ' + message.data);

	switch (parsedMessage.id) {
		case 'startResponse':
			startResponse(parsedMessage);
			break;
		case 'error':
			if (state == I_AM_STARTING) {
				setState(I_CAN_START);
			}
			alert(parsedMessage.message);
			if (parsedMessage.message == "access denied need authentication")
				keycloak.login();
			onError('Error message from server: ' + parsedMessage.message);
			break;
		case 'iceCandidate':
			webRtcPeer.addIceCandidate(parsedMessage.candidate)
			break;
		default:
			if (state == I_AM_STARTING) {
				setState(I_CAN_START);
			}
			onError('Unrecognized message', parsedMessage);
	}
}

function start() {
	console.log('Starting video call ...')

	// Disable start button
	setState(I_AM_STARTING);
	showSpinner(videoOutput);

	console.log('Creating WebRtcPeer and generating local sdp offer ...');

	var options = {
		remoteVideo: videoOutput,
		onicecandidate: onIceCandidate,
		configuration: conf.iceServers
	}

	webRtcPeer = kurentoUtils.WebRtcPeer.WebRtcPeerRecvonly(options, function (error) {
		if (error) return onError(error);
		this.generateOffer(onOffer);
	});
}

function onIceCandidate(candidate) {
	console.log('Local candidate' + JSON.stringify(candidate));

	var message = {
		id: 'onIceCandidate',
		candidate: candidate
	};
	sendMessage(message);
}

function onOffer(error, offerSdp) {
	if (error) return onError(error);

	console.info('Invoking SDP offer callback function ' + location.host);
	var message = {
		id: 'start',
		sdpOffer: offerSdp,
		rtspId: rtspSRC,
		accessToken: accessToken
	}
	sendMessage(message);
}

function onError(error) {
	console.error(error);
}

function startResponse(message) {
	setState(I_CAN_STOP);
	console.log('SDP answer received from server. Processing ...');
	webRtcPeer.processAnswer(message.sdpAnswer);
}

function stop() {
	console.log('Stopping video call ...');
	setState(I_CAN_START);
	if (webRtcPeer) {
		webRtcPeer.dispose();
		webRtcPeer = null;

		var message = {
			id: 'stop'
		}
		sendMessage(message);
	}
	hideSpinner(videoOutput);
}

function setState(nextState) {
	switch (nextState) {
		case I_CAN_START:
			$('#start').attr('disabled', false);
			$('#start').attr('onclick', 'start()');
			$('#stop').attr('disabled', true);
			$('#stop').removeAttr('onclick');
			break;

		case I_CAN_STOP:
			$('#start').attr('disabled', true);
			$('#stop').attr('disabled', false);
			$('#stop').attr('onclick', 'stop()');
			break;

		case I_AM_STARTING:
			$('#start').attr('disabled', true);
			$('#start').removeAttr('onclick');
			$('#stop').attr('disabled', true);
			$('#stop').removeAttr('onclick');
			break;

		default:
			onError('Unknown state ' + nextState);
			return;
	}
	state = nextState;
}

function sendMessage(message) {
	var jsonMessage = JSON.stringify(message);
	console.log('Sending message: ' + jsonMessage);
	ws.send(jsonMessage);
}

function showSpinner() {
	for (var i = 0; i < arguments.length; i++) {
		arguments[i].poster = './img/transparent-1px.png';
		arguments[i].style.background = 'center transparent url("./img/spinner.gif") no-repeat';
	}
}

function hideSpinner() {
	for (var i = 0; i < arguments.length; i++) {
		arguments[i].src = '';
		arguments[i].poster = './img/webrtc.png';
		arguments[i].style.background = '';
	}
}

/**
 * Lightbox utility (to display media pipeline image in a modal dialog)
 */
$(document).delegate('*[data-toggle="lightbox"]', 'click', function (event) {
	event.preventDefault();
	$(this).ekkoLightbox();
});

var conf = {
snap4rtspWsUrl: ((window.location.protocol === "https:") ? "wss://" : "ws://") + window.location.host + '/snap4rtsp/ws',
keycloak:{ "realm":"master", "url":'https://www.snap4city.org/auth/', "clientId": "js-snap4city-mobile-app" },
iceServers:  [{"url":"turn:<ip>:3478","username":"change","credential":"change"}]
};

var fs = require("fs");
var schedule = require('node-schedule');
var XMLHttpRequest = require("xmlhttprequest").XMLHttpRequest;
const {
	Issuer
} = require('./lib/openid-client');
const params_scope = 'openid username profile offline_access';

var _appid;
var _ownership_endpoint;
var _redirect_base_uri;
var _refresh_token_path;
var _keycloak_base_uri;
var _keycloak_clientid;
var _keycloak_clientsecret;
var _strategy_name;

module.exports = {

	//----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
	init: function (appid, ownership_endpoint, redirect_base_uri, refresh_token_path, keycloak_base_uri, keycloak_clientid, keycloak_clientsecret, strategy_name) {
		_appid = appid;
		_ownership_endpoint = ownership_endpoint;
		_redirect_base_uri = redirect_base_uri;
		_refresh_token_path = refresh_token_path;
		_keycloak_base_uri = keycloak_base_uri;
		_keycloak_clientid = keycloak_clientid;
		_keycloak_clientsecret = keycloak_clientsecret;
		_strategy_name = strategy_name;

		var randomSec = Math.floor(Math.random() * Math.floor(59));
		var randomMin = Math.floor(Math.random() * Math.floor(59));
		var randomOra = Math.floor(Math.random() * Math.floor(23));
		var chron = randomSec + " " + randomMin + " " + randomOra + " * * *";
		console.log((new Date()).toString() + "^^Refresh Token running at " + chron);
		schedule.scheduleJob(chron, function () { //once a day
			console.log((new Date()).toString() + "^^Attempting to Refresh Token at " + encodeURI(_keycloak_base_uri + '/protocol/openid-connect/token'));
			try {
				//retrieve old Refresh Token
				var old_refresh_token = fs.readFileSync(_refresh_token_path);
				var xmlHttp = new XMLHttpRequest();
				xmlHttp.open("POST", encodeURI(_keycloak_base_uri + '/protocol/openid-connect/token'), false);
				xmlHttp.setRequestHeader("Content-Type", "application/x-www-form-urlencoded");
				xmlHttp.send("client_id=" + _keycloak_clientid + "&client_secret=" + _keycloak_clientsecret + "&grant_type=refresh_token&scope=" + params_scope + "&refresh_token=" + old_refresh_token);
				if (xmlHttp.readyState === 4 && xmlHttp.status===200 && xmlHttp.responseText != "") {
					console.log((new Date()).toString() + "^^Got " + xmlHttp.responseText);
					var response = JSON.parse(xmlHttp.responseText);
					if (response!= null && response.refresh_token!=null) {
						//write new Refresh Token
						fs.writeFileSync(_refresh_token_path, response.refresh_token);
						console.log((new Date()).toString() + "^^Token refreshed");
					}
					else {
						console.log((new Date()).toString() + "^^Not contains valid Refresh Token");
					}
				} else {
					console.log((new Date()).toString() + "^^Not a valid response for Refresh Token");
				}
			} catch (e) {
				console.log((new Date()).toString() + "^^Unknown error");
				console.log(e);
			}
		});
	},

	//----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
	auth: function () {
		//preparing data for strategy
		const issuer = new Issuer({
			issuer: _keycloak_base_uri,
			authorization_endpoint: _keycloak_base_uri + '/protocol/openid-connect/auth',
			token_endpoint: _keycloak_base_uri + '/protocol/openid-connect/token',
			userinfo_endpoint: _keycloak_base_uri + '/protocol/openid-connect/userinfo',
			jwks_uri: _keycloak_base_uri + '/protocol/openid-connect/certs'
		});
		const client = new issuer.Client({
			client_id: _keycloak_clientid,
			client_secret: _keycloak_clientsecret
		});
		//returning strategy	
		var obj = {
			type: "strategy",
			strategy: {
				name: _strategy_name,
				label: 'Enter',
				strategy: require("./lib/openid-client").Strategy,
				options: {
					client: client,
					params: {
						redirect_uri: _redirect_base_uri + _appid + '/auth/strategy/callback',
						scope: params_scope
					},
					verify: function (tokenset, userinfo, done) {
						console.log((new Date()).toString() + "^^Logged user: %j", userinfo);
						//Always store the Refresh Token, in a TEMP FILE
						fs.writeFileSync(_refresh_token_path + "-temp-" + userinfo.username, tokenset.refresh_token);
						//user is correctly recognized by keycloak, user enforcement is made below
						done(null, userinfo);
					}
				},
			},
			users: function (username) {
				return new Promise(function (resolve) {
					var isValid = false;

					try {
						if (fs.existsSync(_refresh_token_path + "-temp-" + username)){//should always exist, but enforce the existence
							//retrieve Refresh Token, from a TEMP FILE
							var old_refresh_token = fs.readFileSync(_refresh_token_path + "-temp-" + username);
							//Removing TEMP FILE in any case
							//fs.unlinkSync(_refresh_token_path + "-temp-" + username);
							var xmlHttp = new XMLHttpRequest();
							xmlHttp.open("POST", encodeURI(issuer.token_endpoint), false);
							xmlHttp.setRequestHeader("Content-Type", "application/x-www-form-urlencoded");
							xmlHttp.send("client_id=" + client.client_id + "&client_secret=" + client.client_secret + "&grant_type=refresh_token&scope=" + params_scope + "&refresh_token=" + old_refresh_token);
							if (xmlHttp.readyState === 4 && xmlHttp.status===200 && xmlHttp.responseText != "") {
								console.log((new Date()).toString() + "^^Got " + xmlHttp.responseText);
								var response = JSON.parse(xmlHttp.responseText);
								if (response!=null && response.access_token!=null && response.refresh_token!=null) {
									//retry from OWNERSHIP API the list of the APPID belonging to the logged user, using passed tokenset.refresh_token
									var xmlHttp = new XMLHttpRequest();
									xmlHttp.open("GET", encodeURI(_ownership_endpoint + "?accessToken=" + response.access_token + "&elementId=" + _appid), false);
									xmlHttp.send();
									//check against ownership
									ownerships = JSON.parse(xmlHttp.responseText);
									for (var i = 0; i < ownerships.length; i++) {
										if (ownerships[i].elementId == _appid) {
											isValid = true;
											console.log((new Date()).toString() + "^^He is a valid user");
											if (ownerships[i].username == username) {
												//write the Refresh Token just for the owner
												fs.writeFileSync(_refresh_token_path, response.refresh_token);
												console.log((new Date()).toString() + "^^He is the owner");
											}
										}
									}
								}
								else {
									console.log((new Date()).toString() + "^^Not contains valid Refresh Token");
								}							
							} else {
								console.log((new Date()).toString() + "^^Not a valid response for Refresh Token");
							}
						} 
						else {
							console.log((new Date()).toString() + "^^Refresh Token not found");
						}
					} catch (e) {
						console.log((new Date()).toString() + "^^Unknown error");
						console.log(e);
					}
					
					if (isValid) {
						var user = {
							username: username,
							permissions: "*"
						};
						resolve(user);
					} else {
						resolve(null);
					}
				});
			}
		};
		return obj;
	}
}
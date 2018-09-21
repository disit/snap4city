var fs = require("fs");
var schedule = require('node-schedule');
var XMLHttpRequest = require("xmlhttprequest").XMLHttpRequest;
const { Issuer } = require('openid-client');
const params_scope='openid username profile offline_access';

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
    init: function(appid, ownership_endpoint, redirect_base_uri, refresh_token_path, keycloak_base_uri, keycloak_clientid, keycloak_clientsecret, strategy_name){

                _appid=appid;
                _ownership_endpoint=ownership_endpoint;
                _redirect_base_uri=redirect_base_uri;
                _refresh_token_path=refresh_token_path;
                _keycloak_base_uri=keycloak_base_uri;
                _keycloak_clientid=keycloak_clientid;
                _keycloak_clientsecret=keycloak_clientsecret;
                _strategy_name=strategy_name;

                var randomMin=Math.floor(Math.random() * Math.floor(59));
                var randomSec=Math.floor(Math.random() * Math.floor(59));
                var chron=randomSec+" "+randomMin+" * * * *";
                console.log((new Date()).toString()+"^^Refresh Token running at "+chron);
                schedule.scheduleJob(chron, function(){//every hour
                    console.log((new Date()).toString()+"^^Attempting to Refresh Token at "+encodeURI(_keycloak_base_uri+'/protocol/openid-connect/token'));
                    try{
                        var old_refresh_token=fs.readFileSync(_refresh_token_path);
                        var response = "";
                        var xmlHttp = new XMLHttpRequest();
                        xmlHttp.open("POST", encodeURI(_keycloak_base_uri+'/protocol/openid-connect/token'), false);
                        xmlHttp.setRequestHeader("Content-Type", "application/x-www-form-urlencoded");
                        xmlHttp.send("client_id="+_keycloak_clientid+"&client_secret="+_keycloak_clientsecret+"&grant_type=refresh_token&scope="+params_scope+"&refresh_token="+ old_refresh_token  );
                        if (xmlHttp.responseText != ""){
                            response = JSON.parse(xmlHttp.responseText);
                        }
			console.log("GOT --"+response);
                        if ((response != "")&&(typeof response.refresh_token !== "undefined")){
                            fs.writeFile(_refresh_token_path, response.refresh_token);
                            console.log((new Date()).toString()+"^^Token refreshed");
                        }else{
                            console.log((new Date()).toString()+"^^Not a valid response for Refresh Token" );
                        }
                    }
                    catch(e){
                        console.log((new Date()).toString()+"^^Refresh Token not present or Auth server not reachable");
                        console.log(e);
                    }
                });
    },

    //----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
    auth:function(){

	//preparing data for strategy
	const issuer = new Issuer({
	    issuer: _keycloak_base_uri,
	    authorization_endpoint: _keycloak_base_uri+'/protocol/openid-connect/auth',
	    token_endpoint: _keycloak_base_uri+'/protocol/openid-connect/token',
	    userinfo_endpoint: _keycloak_base_uri+'/protocol/openid-connect/userinfo',
	    jwks_uri: _keycloak_base_uri+'/protocol/openid-connect/certs'
	});

	const client = new issuer.Client({
	    client_id: _keycloak_clientid,
	    client_secret: _keycloak_clientsecret 
	});

	//returning strategy	
	var obj={
	    type:"strategy",
            strategy: {
                name: _strategy_name,
                label: 'Enter',
                strategy: require("openid-client").Strategy,
                options: {
                        client:client,
                        params:{
				redirect_uri: _redirect_base_uri+_appid+'/auth/strategy/callback',
				scope: params_scope 
			},
                        verify: function(tokenset,userinfo,done) {
				//Always store the Refresh Token 
				fs.writeFileSync(_refresh_token_path,tokenset.refresh_token);
				//user is correctly recognized by keycloak, user enforcement is made below
                                done(null, userinfo);
                        }
                },
            },
	    users: function(username) {
		return new Promise(function(resolve) {
		    var isValid=false;

		    //retrieve Access Token from keyclock	
	            var access_token;
		    try{
                            var old_refresh_token=fs.readFileSync(_refresh_token_path);
                            var response = "";
                            var xmlHttp = new XMLHttpRequest();
                            xmlHttp.open("POST", encodeURI(issuer.token_endpoint), false);
                            xmlHttp.setRequestHeader("Content-Type", "application/x-www-form-urlencoded");
                            xmlHttp.send("client_id="+client.client_id+"&client_secret="+client.client_secret+"&grant_type=refresh_token&scope="+params_scope+"&refresh_token="+ old_refresh_token  );
                            if (xmlHttp.responseText != ""){
                                response = JSON.parse(xmlHttp.responseText);
                            }
                            if ((response != "")&&(typeof response.access_token !== "undefined")){
                                access_token=response.access_token; 
                            }else{
                                console.log((new Date()).toString()+"^^Was not possibile to retrieve a valid Access Token" );
                            }
		    }
                    catch(e){
                            console.log((new Date()).toString()+"^^Access Token not present or Auth server not reachable");
                            console.log(e);
                    }
		   
		    //retry from OWNERSHIP API the list of the APPID belonging to the logged user, using passed tokenset.refresh_token
		    try{
                            var xmlHttp = new XMLHttpRequest();
                            xmlHttp.open("GET", encodeURI(_ownership_endpoint+"?accessToken="+access_token+"&elementId="+_appid), false);
			    xmlHttp.send();
			    //console.log((new Date()).toString()+"Ownership-"+xmlHttp.responseText); 
			    if (xmlHttp.responseText.includes(_appid))
				isValid=true;    
                    }
                    catch(e){
                            console.log((new Date()).toString()+"^^Ownership not reachable");
                            console.log(e);
                    }
		    
		    if (isValid){
		    	var user = { username: username, permissions: "*" };
                    	resolve(user);
		    }else{
			resolve(null);
		    }
       		});
	     }	
	};
	return obj;	
    }
}

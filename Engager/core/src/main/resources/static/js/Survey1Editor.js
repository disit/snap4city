var Survey1Editor = {

    initialize: function (organization, questionNumber) {

    	Survey1Editor.keycloak = Keycloak({
            "realm": "master",
            "url": Authentication.url,
            "clientId": Authentication.clientId
        });        
         
         
    	Survey1Editor.keycloak.init({
             onLoad: 'login-required'
         }).success(
             function (authenticated) {
                 console.log(authenticated);
                 if (authenticated) {
                	 Survey1Tabler.renderTable(organization, questionNumber);
                 } else {
                	 Survey1Editor.keycloak.login();
                 } 
             }).error(function () {});
    },
}
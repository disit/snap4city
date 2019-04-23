var EngagerEditor = {

    initialize: function () {

         EngagerEditor.keycloak = Keycloak({
            "realm": "master",
            "url": Authentication.url,
            "clientId": Authentication.clientId
        });        
         
         
         EngagerEditor.keycloak.init({
             onLoad: 'login-required'
         }).success(
             function (authenticated) {
                 console.log(authenticated);
                 if (authenticated) {
                	 EngagerTabler.checkEngagementEnabled();//renderTable();
                 } else {
                	 EngagerEditor.keycloak.login();
                 } 
             }).error(function () {});
    },
}
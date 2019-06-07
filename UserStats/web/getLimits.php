<?php

header('Access-Control-Allow-Origin: *');
require 'sso/autoload.php';

use Jumbojett\OpenIDConnectClient;

function getToken() {
    $oidc = new OpenIDConnectClient(
            'https://www.snap4city.org', 'php-userstats', 'b941378b-4088-46e2-9e59-abaf8614ddf4'
    );

    $oidc->setVerifyHost(false);
    $oidc->setVerifyPeer(false);

    $oidc->providerConfigParam(array('authorization_endpoint' => 'https://www.snap4city.org/auth/realms/master/protocol/openid-connect/auth'));
    $oidc->providerConfigParam(array('token_endpoint' => 'https://www.snap4city.org/auth/realms/master/protocol/openid-connect/token'));
    $oidc->providerConfigParam(array('userinfo_endpoint' => 'https://www.snap4city.org/auth/realms/master/protocol/openid-connect/userinfo'));
    $oidc->providerConfigParam(array('jwks_uri' => 'https://www.snap4city.org/auth/realms/master/protocol/openid-connect/certs'));
    $oidc->providerConfigParam(array('issuer' => 'https://www.snap4city.org/auth/realms/master'));
    $oidc->providerConfigParam(array('end_session_endpoint' => 'https://www.snap4city.org/auth/realms/master/protocol/openid-connect/logout'));

    if (!isset($_SESSION['login'])) {
        $oidc->addScope(array('openid', 'username', 'profile'));
        $oidc->setRedirectURL($sso_login_redirect);
        $oidc->authenticate();
        $name = $oidc->requestUserInfo('username');
        $_SESSION['login'] = $name;
        $_SESSION['refreshToken'] = $oidc->getRefreshToken();
        $_SESSION['accessToken'] = $oidc->getAccessToken();
        //echo $_SESSION['login'] . "<br/>";
        return $_SESSION['accessToken'];
    } else {
        //echo $_SESSION['login'] . "<br/>";
        $tkn = $oidc->refreshToken($_SESSION['refreshToken']);
        // echo $tkn->access_token;
        $_SESSION['accessToken'] = $tkn->access_token;
        $_SESSION['refreshToken'] = $tkn->refresh_token;
        return $_SESSION['accessToken'];
    }
}

function getLimits($username) {
    $accessToken = getToken();
    if($username != null) {
     $json = file_get_contents("http://192.168.0.10/ownership-api/v1/limits/?accessToken=" . $accessToken . "&username=" . $username);
    }
    else {
     $json = file_get_contents("http://192.168.0.10/ownership-api/v1/limits/?accessToken=" . $accessToken);
    }
    $json = json_decode($json, true);
    return $json["limits"];
}
echo json_encode(getLimits($_REQUEST["username"]));
?>

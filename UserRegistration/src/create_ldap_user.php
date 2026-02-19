<?php
/* eShare in a Snap.
  Copyright (C) 2024 SNAP4 S.R.L. https://www.snap4.eu

  All right reserved */

/**Readme, you fools!
 * In order to create an user inside ldap, we need some credentials.
 * These credentials are hardcoded in the code here, but you will need to change them, according to whatever your configuration happens to be.
 * IF you are using the openldap server inside the docker-compose from the generator of snap4city distributions, then the password for the ldap
 * will be in the placeholder list files (also someplace else but that's as clear as it gets). It won't work until the post-setup.sh has been executed.
 * So, change it before trying to run this php. The BaseDn and the admin user are hardocoded in the ldap too.
 *
 * For the follow up email, you need to provide an email (and its additional data) as the sender.
 */
require_once './vendor/phpmailer/phpmailer/src/PHPMailer.php';
require_once './vendor/phpmailer/phpmailer/src/SMTP.php';
require_once './vendor/phpmailer/phpmailer/src/Exception.php';
error_reporting(E_ERROR | E_PARSE);
if ($_SERVER["REQUEST_METHOD"] == "POST") {
    //this monster means: Minimum twelve characters, at least one uppercase letter, one lowercase letter, one number and one special character among the following string inside quotes (not included) "@$!%*?&"
    $pattern = '/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])[A-Za-z\d@$!%*?&]{12,}$/';
    $pattern_uid = '/^[a-zA-Z_0-9]+$/';
    // Validate form data
    $uid = htmlspecialchars($_POST['user']);
    if($uid == ''){
        outputHTML("Username is empty",400);
    }
    $email = htmlspecialchars($_POST['email']);
    if($email == ''){
        outputHTML("Email is empty",400);
    }
    $password = $_POST['password'];
    if($password == ''){
        outputHTML("Password is empty",400);
    }
    $confirm_password = $_POST['confirm_password'];
    if($confirm_password == ''){
       outputHTML("Password confirmation is empty",400);
    }
    if ($password != $confirm_password) {
        outputHTML("Password confirmation failed. Please ensure both passwords are identical",400);
    }
    elseif (!preg_match($pattern, $password)) {
        outputHTML("Password is not strong enough",400);
    }
    if (!preg_match($pattern_uid, htmlspecialchars($_POST['user']))) {
        outputHTML("Username does not contain letters, digits and underscore",400);
    }
    $servername = getenv("DB_HOST") ?? "db_host";
    $username = getenv("DB_USER") ?? "db_user";
    $pass = getenv("DB_PASSWORD") ?? "db_password";
    $dbname = getenv("DB_NAME") ?? "db_name";
    try{
        $conn = new PDO("mysql:host=$servername;dbname=$dbname", $username, $pass);
        $conn->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
        $stmt = $conn->prepare("SELECT * FROM users WHERE userid = :v1;");
        $stmt->bindParam(":v1", $uid);
        $stmt->execute();
        $row = $stmt->fetch(PDO::FETCH_ASSOC);
        if ($row) {
            $now = new DateTime();
            $target = new DateTime($row["timeofregistration"]);
            $diffInSeconds = $now->getTimestamp() - $target->getTimestamp(); 
            $totalMinutes = floor($diffInSeconds / 60);
            $maxTime = (int)getenv("REGISTRATION_LINK_DURATION_IN_MIN") ?: 24*60;
            if ($totalMinutes <= $maxTime) {
                if ($row["statusofregistration"]!="unregistered") {
                    outputHTML("User is already registered.",409);
                }
                else {
                    $stmt = $conn->prepare("UPDATE `user_login`.`users` SET `statusofregistration` = 'no-devices-yet' WHERE (`userid` = :v1);");
                    $stmt->bindParam(":v1", $uid);
                    $stmt->execute();
                    register_to_ldap($uid, $email, $password);
                }
            }
            else {
                $conn->prepare("DELETE FROM users WHERE userid = :v2");
                $stmt->bindParam(':v2', $userid);
                $stmt->execute();
                $rowCount = $stmt->rowCount();
                if ($rowCount > 0) {
                    outputHTML("Link on mail is expired, please start the procedure again", 400);
                } else {
                    error_log("Error trying to remove an user ($userid): user not exists", 500);
                    outputHTML("Something went wrong. Please contact support if the problem persist", 500);
                }
            }
        } else {
            outputHTML("User does not exist",409);
        }
    }catch(PDOException $e){
        error_log("Database error: ". $e->getMessage());
        outputHTML("Something went wrong. Please contact support if the problem persist", 500);
    }
} else {
    outputHTML("Method not allowed", 405);
}
function register_to_ldap($uid, $email, $password){
    $ldapServer = getenv('LDAP_SERVER') ?: 'ldap://ldap';
    $ldapAdminUser = getenv("LDAP_ADMIN_USER") ?: 'cn=admin,dc=ldap,dc=organization,dc=com'; // The admin user with appropriate privileges
    $ldapAdminPassword = getenv("LDAP_ADMIN_PASSWORD") ?: "ldap_password"; // The password for the admin user
    $ldapBaseDn = getenv("LDAP_BASEDN") ?: 'dc=ldap,dc=organization,dc=com'; // Base DN for your LDAP directory
    $group= getenv("LDAP_GROUP") ?: $ldapBaseDn;
    $ldapRole = getenv("LDAP_ROLE") ?: 'Manager';
    $ldapOu = getenv("LDAP_OU") ?: 'Organization';
    $ldapOu = array_map('trim', explode(',', $ldapOu));
    // LDAP connection
    $ldapConn = ldap_connect($ldapServer);
    if(!$ldapConn){
        error_log("Fail to connect to LDAP server $ldapServer");
        outputHTML("Something went wrong. Please contact support if the problem persist", 500);
    }
    ldap_set_option($ldapConn, LDAP_OPT_PROTOCOL_VERSION, 3);
    ldap_set_option($ldapConn, LDAP_OPT_REFERRALS, 0);
    $ldapBind = ldap_bind($ldapConn, $ldapAdminUser, $ldapAdminPassword);
    if(!$ldapBind){
        error_log("Fail to bind to LDAP: ". ldap_error($ldapConn));
        ldap_close($ldapConn);
        outputHTML("Something went wrong. Please contact support if the problem persist", 500);
    }
    $search_filter = "(cn=$uid)";
    $search_result = ldap_search($ldapConn, $ldapBaseDn, $search_filter);
    if (!$search_result) {
        error_log("LDAP search by cn=$uid failed: " . ldap_error($ldapConn));
        ldap_close($ldapConn);
        outputHTML("Something went wrong. Please contact support if the problem persist",500);
    }
    $entries = ldap_get_entries($ldapConn, $search_result);
    if ($entries["count"] > 0) {
        ldap_close($ldapConn);
        outputHTML("Fail to register user: email already exist", 409);
    }
    $newUserDn = 'cn=' . $uid . ',' . $group;
    $encryptedUid = encryptUid($uid); 
    $newUserAttributes = [
        'cn' => $uid,
        'sn' => $uid,
        'uid' => $encryptedUid,
        'mail' => $email,
        'ou' => $ldapOu,
        'userPassword' => hashing_the_password($password),
        'objectclass' => ['top', 'inetOrgPerson']
    ];
    $addUser = ldap_add($ldapConn, $newUserDn, $newUserAttributes);           
    if (!$addUser){
        error_log("Fail to add user to LDAP: ". ldap_error($ldapConn));
        ldap_close($ldapConn);
        outputHTML("Something went wrong. Please contact support if the problem persist",500);
    }
    $organizational_role = "cn=$ldapRole,$ldapBaseDn";
    $organizational_role_entry = ['roleoccupant' => $newUserDn];
    $modifyEntry = @ldap_mod_add($ldapConn, $organizational_role, $organizational_role_entry);
    if (!$modifyEntry) {
        error_log("Fail to set user as $ldapRole: ". ldap_error($ldapConn));
        ldap_close($ldapConn);
        outputHTML("Something went wrong. Please contact support if the problem persist", 500);
    }
    $organization_entry = ['l' => $newUserDn];
    foreach($ldapOu as $ldapOrg){
        $organizational_role_2 = "ou=$ldapOrg,$ldapBaseDn";
        $modifyEntry_2 = @ldap_mod_add($ldapConn, $organizational_role_2, $organization_entry);
        if (!$modifyEntry_2) {
            error_log("Fail to add user to $ldapOrg: ". ldap_error($ldapConn));
            ldap_close($ldapConn);
            outputHTML("Something went wrong. Please contact support if the problem persist", 500);
        }
    }
    $ldap_groups = getenv("LDAP_GROUPS") ?: "";
    if($ldap_groups == ""){
        error_log("Fail to get LDAP groups. Environment variable not read");
        ldap_close($ldapConn);
        outputHTML("Something went wrong. Please contact support if the problem persist", 500);
    }
    $organizational_role_memberUid = ['memberUid' => $newUserDn];
    $group_list = explode(',', $ldap_groups);
    foreach ($group_list as $item) {
        $organizational_role_3 = "cn=$item,$ldapBaseDn";
        $modifyEntry_3 = @ldap_mod_add($ldapConn, $organizational_role_3, $organizational_role_memberUid);
        if (!$modifyEntry_3) {
            error_log("Fail to add user to $item: ". ldap_error($ldapConn));
            ldap_close($ldapConn);
            outputHTML("Something went wrong. Please contact support if the problem persist", 500);
        }
    }
    //success
    outputHTML(null, 200);
}
function outputHTML($errors, $code) {
    http_response_code($code);
    $templatesFolder = getenv('TEMPLATES_FOLDER') ?: "";
    $config = [
        "error_template" => $templatesFolder . "/error_template.html",
        "success_template" =>  $templatesFolder . "/success_template.html",
    ];
    if ($errors) {
        try{
            $html = get_template($config["error_template"]);
        }catch(Exception $ex){
            $html = errorHTML();
        }
        $html = str_replace("[[Error text]]", $errors, $html);
        echo $html;
    }else{
        try{
            $html = get_template($config["success_template"]);
        }catch(Exception $ex){
            $html = successHTML();
        }
        $html = str_replace("[[operation]]", "User registration", $html);
        echo $html;
    }
    exit();
}
function encryptUid($uid){
    $secretKey = getenv('SECRET_KEY') ?: 'secret_key';
    $secretIV = getenv('SECRET_IV') ?: 'secret_iv';
    $encryptMethod = getenv('ENCRYPT_METHOD') ?: 'encrypt_method';
    return encryptOSSL($uid, $secretKey, $secretIV, $encryptMethod);
}
function encryptOSSL($string, $secret_key, $secret_iv, $encrypt_method) {
    $output = false;

    // hash
    $key = hash('sha256', $secret_key);

    // iv - encrypt method AES-256-CBC expects 16 bytes - else you will get a warning
    $iv = substr(hash('sha256', $secret_iv), 0, 16);

    $output = openssl_encrypt($string, $encrypt_method, $key, 0, $iv);
    $output = base64_encode($output);


    return $output;
}

function successHTML() {
    return <<<STRING
    <!DOCTYPE html>
    <html lang="en">
    <head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Success!</title>
    <!-- Bootstrap CSS -->
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet" integrity="sha384-QWTKZyjpPEjISv5WaRU9OFeRpok6YctnYmDr5pNlyT2bRjXh0JMhjY6hW+ALEwIH" crossorigin="anonymous">
    </head>
    <body>
    <div class="container mt-5" style="text-align: center;">
    <div class="row">
    <div class="col-md-12">
    <div class="success-container ">
    <h1 class="success-heading text-success" style="font-size: 40px;">[[operation]] completed!</h1>
    <p class="success-message" style="font-size: 24px;">You can now close this page</p>
    </div>
    </div>
    </div>
    </div>
    </body>
    </html>
    STRING;
}

function errorHTML() {
    return <<<STRING
    <!DOCTYPE html>
    <html lang="en">
    <head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Error!</title>
    <!-- Bootstrap CSS -->
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet" integrity="sha384-QWTKZyjpPEjISv5WaRU9OFeRpok6YctnYmDr5pNlyT2bRjXh0JMhjY6hW+ALEwIH" crossorigin="anonymous">
    </head>
    <body>
    <div class="container mt-5" style="text-align: center;">
    <div class="row">
    <div class="col-md-12">
    <div class="error-container">
    <h1 class="error-heading text-danger" style="font-size: 40px;">An error occurred!</h1>
    <p class="error-message" style="font-size: 24px;">[[Error text]]</p>
    </div>
    </div>
    </div>
    </div>
    </body>
    </html>
    STRING;
}

function get_template($templateFilename) {
    $htmlBody = file_get_contents($templateFilename);
    if ($htmlBody === false) {
        throw new Exception('Could not read HTML file.');
    }
    return $htmlBody;
}
function hashing_the_password($password) {
    $salt = substr(str_shuffle(str_repeat('ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789',4)),0,4);
    $expose_just_now = '{SSHA}' . base64_encode(sha1( $password.$salt, TRUE ). $salt);
    return $expose_just_now;
    //return '{crypt}'.crypt('$password');
}
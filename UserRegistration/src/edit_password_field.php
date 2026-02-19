<?php
/* eShare in a Snap.
  Copyright (C) 2024 SNAP4 S.R.L. https://www.snap4.eu

  All right reserved */

require_once './vendor/phpmailer/phpmailer/src/PHPMailer.php';
require_once './vendor/phpmailer/phpmailer/src/SMTP.php';
require_once './vendor/phpmailer/phpmailer/src/Exception.php';
error_reporting(E_ERROR | E_PARSE);
if ($_SERVER["REQUEST_METHOD"] == "POST") {
    //this monster means: Minimum twelve characters, at least one uppercase letter, one lowercase letter, one number and one special character among the following string inside quotes (not included) "@$!%*?&"
    $pattern = '/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])[A-Za-z\d@$!%*?&]{12,}$/';
    $pattern_uid = '/^[a-zA-Z_0-9]+$/';
    // Validate form data
    $password = $_POST['password'];
    if($password == ''){
        outputHTML("Password is empty",400);
    }
    $confirm_password = $_POST['confirm_password'];
    if($confirm_password == ''){
        outputHTML("Confirmation password is empty",400);
    }
    if ($password != $confirm_password) {
        outputHTML("Password confirmation failed. Please ensure both passwords are identical",400);
    }
    elseif (!preg_match($pattern, $password)) {
        outputHTML("Password is not strong enough",400);
    }
    $servername = getenv("DB_HOST") ?: "db_host";
    $username = getenv("DB_USER") ?: "db_user";
    $pass = getenv("DB_PASSWORD") ?: "db_password";
    $dbname = getenv("DB_NAME") ?: "db_name";
    try{
        $conn = new PDO("mysql:host=$servername;dbname=$dbname", $username, $pass);
        $conn->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
        $stmt = $conn->prepare(
            "SELECT mail, password_reset.token, user, timeofreset 
            FROM password_reset join users on password_reset.user=users.userid 
            where (password_reset.token=:v2)
            union all
            SELECT mail, password_reset.token, user, timeofreset 
            FROM password_reset join operator_users on password_reset.user=operator_users.userid 
            where (password_reset.token=:v2)  
            order by timeofreset desc limit 1;"
        );
        $stmt->bindParam(":v2", $_POST['token']);
        $stmt->execute();
        $row = $stmt->fetch(PDO::FETCH_ASSOC);
        if ($row) {
            $now = new DateTime();
            $target = new DateTime($row["timeofreset"]);
            $diffInSeconds = $now->getTimestamp() - $target->getTimestamp(); 
            $totalMinutes = floor($diffInSeconds / 60);
            $maxTime = (int)getenv("RECOVER_LINK_DURATION_IN_MIN") ?: 15;
            if ($totalMinutes <= $maxTime) {
                $uid = $row["user"];
                update_password_on_ldap($uid, $password);
                $stmt = $conn->prepare("DELETE FROM `user_login`.`password_reset` WHERE (`user` = :v1);");
                $stmt->bindParam(":v1", $uid);
                $stmt->execute();
                $rowCount = $stmt->rowCount();
                if ($rowCount > 0) {
                    outputHTML(null, 200);
                } else {
                    error_log("Error trying to remove an user ($uid): user not exists", 500);
                    outputHTML("Something went wrong. Please contact support if the problem persist", 500);
                }
            } else {
                $conn->prepare("DELETE FROM `user_login`.`password_reset` WHERE user = :v2");
                $stmt->bindParam(':v2', $uid);
                $stmt->execute();
                $rowCount = $stmt->rowCount();
                if ($rowCount > 0) {
                    outputHTML("Link on mail is expired, please start the procedure again", 400);
                } else {
                    error_log("Error trying to remove an user ($uid): user not exists", 500);
                    outputHTML("Something went wrong. Please contact support if the problem persist", 500);
                }
            }
        }
        else {
            outputHTML("User not found", 500);
        }
    }catch(PDOException $e){
        error_log("Database error: ". $e->getMessage());
        outputHTML("Something went wrong. Please contact support if the problem persist", 500);
    }
} else {
    outputHTML("Method not allowed", 405);
}
function update_password_on_ldap($uid, $password){
    $ldapServer = getenv('LDAP_SERVER') ?: 'ldap://ldap';
    $ldapAdminUser = getenv("LDAP_ADMIN_USER") ?: 'cn=admin,dc=ldap,dc=organization,dc=com'; // The admin user with appropriate privileges
    $ldapAdminPassword = getenv("LDAP_ADMIN_PASSWORD") ?: "ldap_password"; // The password for the admin user
    $ldapBaseDn = getenv("LDAP_BASEDN") ?: 'dc=ldap,dc=organization,dc=com'; // Base DN for your LDAP directory
    // User login credentials
    $username_full = 'cn='.$uid.','.$ldapBaseDn; // User's username
    // Connect to LDAPS server
    $ldap_conn = ldap_connect($ldapServer);
    if (!$ldap_conn) {
        error_log("Fail to connect to LDAP server $ldapServer");
        outputHTML("Something went wrong. Please contact support if the problem persist", 500);
    }
    // Set LDAP options for SSL/TLS
    ldap_set_option($ldap_conn, LDAP_OPT_PROTOCOL_VERSION, 3);
    ldap_set_option($ldap_conn, LDAP_OPT_REFERRALS, 0);
    // Bind to LDAPS server
    $ldap_bind = ldap_bind($ldap_conn, $ldapAdminUser, $ldapAdminPassword);
    if (!$ldap_bind) {
        error_log("Fail to bind to LDAP: ". ldap_error($ldap_conn));
        ldap_close($ldap_conn);
        outputHTML("Something went wrong. Please contact support if the problem persist", 503);
    }
    $new_value=hashing_the_password($password);
    // Modify the user's attribute (e.g., email)
    $attributes = array('userPassword' => $new_value);
    $modify_result = ldap_modify($ldap_conn, $username_full, $attributes);
    if (!$modify_result){
        error_log("Fail to edit password: ". ldap_error($ldap_conn));
        ldap_close($ldap_conn);
        outputHTML("Something went wrong. Please contact support if the problem persist", 500);
    }
    ldap_close($ldap_conn);
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
        $html = str_replace("[[operation]]", "Password recovery", $html);
        echo $html;
    }
    exit();
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
    <h1 class="success-heading text-success" style="font-size: 40px;">[[operation]] successfully completed!</h1>
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
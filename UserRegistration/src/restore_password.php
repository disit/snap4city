<?php
/* eShare in a Snap.
  Copyright (C) 2024 SNAP4 S.R.L. https://www.snap4.eu

  All right reserved */
function verySimpleEncoding($string) {
    return base64_encode($string);
}
require_once './vendor/phpmailer/phpmailer/src/PHPMailer.php';
require_once './vendor/phpmailer/phpmailer/src/SMTP.php';
require_once './vendor/phpmailer/phpmailer/src/Exception.php';
error_reporting(E_ERROR | E_PARSE);
//build json message
$messageOutput['message'] = '';
$messageOutput['code'] = '';
$messageOutput['error'] = '';
if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {             
    header("Access-Control-Allow-Origin: *");                    
    header('Access-Control-Allow-Methods: POST, GET, OPTIONS'); // Allow POST, GET, and OPTIONS requests
    header('Access-Control-Max-Age: 86400'); // Cache preflight response for 1 day
    header('Content-Length: 0');
    header('Content-Type: text/plain');
    exit();
}
if ($_SERVER["REQUEST_METHOD"] == "POST") {
    $user = htmlspecialchars($_POST['user']);
    if($user == ''){
        http_response_code(409);
        $messageOutput["code"] = "409";
        $messageOutput['error'] = "Empty username";
        $messageOutput["message"] = "Username is empty";
        echo json_encode($messageOutput);
        exit();
    }

    $servername = getenv("DB_HOST") ?: "db_host";
    $username = getenv("DB_USER") ?: "db_user";
    $password = getenv("DB_PASSWORD") ?: "db_password";
    $dbname = getenv("DB_NAME") ?: "db_name";

    try{
        $conn = new PDO("mysql:host=$servername;dbname=$dbname", $username, $password);
        $conn->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
        $email = search_user_on_ldap($user);
        $datetime = new DateTime();
        $token = substr(str_shuffle(str_repeat('ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789',12)),0,12);
        $registration_time = $datetime->format(DateTimeInterface::ATOM);
        $stmt = $conn->prepare("INSERT INTO `user_login`.`password_reset` (`token`, `user`, `timeofreset`) VALUES (:v1, :v2, :v3);");
        $stmt->bindParam(':v1',$token);
        $stmt->bindParam(':v2',$user);
        $stmt->bindParam(':v3',$registration_time);
        try {
            $stmt->execute();
        }
        catch (PDOException $e) {
            error_log("User already tries to recover password: " . $e->getMessage());
            http_response_code(409);
            $messageOutput["code"] = "409";
            $messageOutput['error'] = "User already tries to recover password";
            $messageOutput["message"] = "User already tries to recover password";
            echo json_encode($messageOutput);
            exit();
        }
        send_email($user, $email, $token);
    } catch(PDOException $e){
        error_log("Database error: ". $e->getMessage());
        http_response_code(500);
        $messageOutput["code"] = "500";
        $messageOutput['error'] = "Unknown error";
        $messageOutput["message"] = "Something went wrong. Please contact support if the problem persist";
        echo json_encode($messageOutput);
        exit();
    }
} else {
    http_response_code(405);
    $messageOutput["code"] = "405";
    $messageOutput['error'] = "Method not allowed";
    $messageOutput["message"] = "Method not allowed";
    echo json_encode($messageOutput);
}
function search_user_on_ldap($user){
    $ldapServer = getenv('LDAP_SERVER') ?: 'ldap://ldap';
    $ldapAdminUser = getenv("LDAP_ADMIN_USER") ?: 'cn=admin,dc=ldap,dc=organization,dc=com'; // The admin user with appropriate privileges
    $ldapAdminPassword = getenv("LDAP_ADMIN_PASSWORD") ?: "ldap_password"; // The password for the admin user
    $ldapBaseDn = getenv("LDAP_BASEDN") ?: 'dc=ldap,dc=organization,dc=com'; // Base DN for your LDAP directory

    $ldap_conn = ldap_connect($ldapServer);
    if (!$ldap_conn) {
        error_log("Fail to connect to LDAP server $ldapServer");
        http_response_code(500);
        $messageOutput["code"] = "500";
        $messageOutput['error'] = "Unknown error";
        $messageOutput["message"] = "Something went wrong. Please contact support if the problem persist";
        echo json_encode($messageOutput);
        exit();
    }
    ldap_set_option($ldap_conn, LDAP_OPT_PROTOCOL_VERSION, 3);
    ldap_set_option($ldap_conn, LDAP_OPT_REFERRALS, 0);
    $ldap_bind = ldap_bind($ldap_conn, $ldapAdminUser, $ldapAdminPassword);

    if (!$ldap_bind) {
        error_log("Fail to bind to LDAP: ". ldap_error($ldap_conn));
        ldap_close($ldap_conn);
        http_response_code(500);
        $messageOutput["code"] = "500";
        $messageOutput['error'] = "Unknown error";
        $messageOutput["message"] = "Something went wrong. Please contact support if the problem persist";
        echo json_encode($messageOutput);
        exit();
    }
    $search_filter = "(cn=$user)";
    $search_result = ldap_search($ldap_conn, $ldapBaseDn, $search_filter);
    if (!$search_result) {
        error_log("LDAP search by cn=$user failed:" . ldap_error($ldap_conn));
        ldap_close($ldap_conn);
        http_response_code(500);
        $messageOutput["code"] = "500";
        $messageOutput['error'] = "Unknown error";
        $messageOutput["message"] = "Something went wrong. Please contact support if the problem persist";
        echo json_encode($messageOutput);
        exit();
    }
    $entries = ldap_get_entries($ldap_conn, $search_result);
    if ($entries["count"] == 0) {
        ldap_close($ldap_conn);
        http_response_code(409);
        $messageOutput["code"] = "409";
        $messageOutput['error'] = "User not found";
        $messageOutput["message"] = "User not found";
        echo json_encode($messageOutput);
        exit();
    }
    ldap_close($ldap_conn);
    return $entries[0]['mail'][0];
}
function send_email($user, $email, $token){
    // Constructing the URL with the token
    $mail = new PHPMailer\PHPMailer\PHPMailer;
    if(getenv('MAIL_DEBUG')==='true') {
        $mail->SMTPDebug = 2;
        $mail->Debugoutput = 'html';
    }
    // Set up SMTP
    $mail->isSMTP();
    $mail->Host = getenv("MAIL_HOST") ?: 'mail_host';
    $mail->SMTPAuth = (getenv('MAIL_SMTPAUTH') ?: 'true') === 'true';
    $mail->Username = getenv('MAIL_USERNAME') ?: "host_mail"; // Your email address
    $mail->Password = getenv('MAIL_PASSWORD') ?: "host_password"; // Your email password
    if (getenv('MAIL_SMTPSECURE')==='') {
        $mail->SMTPSecure = '';
    } else {
        $mail->SMTPSecure = getenv('MAIL_SMTPSECURE') ?: 'tls';
    }
    $mail->SMTPAutoTLS = (getenv('MAIL_SMTPAUTOTLS') ?: 'true') === 'true';
    $mail->Port = intval(getenv('MAIL_PORT') ?: '587');
    //read mail config file
    $config = [
        "mail_sender" => getenv('MAIL_SENDER') ?:"mail_sender",
        "mail_sender_name" => getenv('MAIL_SENDER_NAME') ?:"mail_sender_name",
        "mail_receiver_name" => getenv('MAIL_RECEIVER_NAME') ?:"mail_receiver_name",
        "mail_subject" => getenv('RECOVER_PASSWORD_MAIL_SUBJECT') ?:"Reset your password!",
        "mail_body_template" => ( getenv('TEMPLATES_FOLDER') ?: "") ."/recover_password_mail_template.html"
    ];
    $mail->setFrom($config["mail_sender"], $config["mail_sender_name"]);
    $receiverName = $config["mail_receiver_name"];
    if($receiverName == '[[user]]'){
        $receiverName = str_replace("[[user]]", $user, $receiverName);
    }
    $mail->addAddress($email, $receiverName);
    // Set email subject and body
    $mail->Subject = $config["mail_subject"];
    $url_token = (getenv("BASE_URL") ?: "base_url") . "change_password.php?token=".$token; // Example: Generating a unique token
    try{
        $email_body = get_template($config["mail_body_template"]);
    }catch(Exception $ex){
        $email_body = get_email_body();
    }
    $email_body = str_replace("[[user]]", $user, $email_body);
    $email_body = str_replace("[[url]]", $url_token, $email_body);
    $mail->Body = $email_body;
    $mail->IsHTML(true);
    if(!$mail->send()) {
        error_log("Mail error for user $user: " . $mail->ErrorInfo);
        http_response_code(400);
        $messageOutput["code"] = "400";
        $messageOutput['error'] = "Mail error";
        $messageOutput["message"] = "Process started but email is not sent.\n Link to recover: ".$url_token;
        echo json_encode($messageOutput);
        exit();
    } else {
        http_response_code(201);
        $messageOutput["code"] = "201";
        $messageOutput['error'] = "";
        $messageOutput["message"] = "We sent an email to the given address to continue password recover";
        echo json_encode($messageOutput);
    }
}
function get_email_body() {
    return <<<STRING
    <!DOCTYPE html>
    <html lang="it">
    <head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Reset password</title>
    </head>
    <body>
    <p>Dear [[user]],</p>
    <p>To recover your password, please click link below:</p>
    <p><a href="[[url]]">[[url]]</a></p>
    <p>The link is valid for 15 minutes. After that, you will need to start the process from the beginning</p>
    <p>Regards,</p>
    <p>Snap4City team.</p>
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
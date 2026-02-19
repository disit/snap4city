<?php
/* eShare in a Snap.
  Copyright (C) 2024 SNAP4 S.R.L. https://www.snap4.eu

  All right reserved */

require_once './vendor/phpmailer/phpmailer/src/PHPMailer.php';
require_once './vendor/phpmailer/phpmailer/src/SMTP.php';
require_once './vendor/phpmailer/phpmailer/src/Exception.php';
error_reporting(E_ERROR | E_PARSE);
//build json message
$messageOutput['message'] = '';
$messageOutput['code'] = '';
$messageOutput['error'] = '';
if ($_SERVER["REQUEST_METHOD"] == "POST") {
    $pattern_uid = '/^[a-zA-Z_0-9]+$/';
    //read username, email, org -> must not be empty and email is an email
    $uid = htmlspecialchars($_POST['user']);
    if($uid == ''){
        http_response_code(409);
        $messageOutput["code"] = "409";
        $messageOutput['error'] = "Empty username";
        $messageOutput["message"] = "Username is empty";
        echo json_encode($messageOutput);
        exit();
    }
    $email = htmlspecialchars($_POST['email']);
    if($email == ''){
        http_response_code(409);
        $messageOutput["code"] = "409";
        $messageOutput['error'] = "Empty email";
        $messageOutput["message"] = "Email is empty";
        echo json_encode($messageOutput);
        exit();
    }
    $org = htmlspecialchars($_POST['org']);
    if($org == ''){
        http_response_code(409);
        $messageOutput["code"] = "409";
        $messageOutput['error'] = "Empty org";
        $messageOutput["message"] = "Org is empty";
        echo json_encode($messageOutput);
        exit();
    }
    if (!preg_match($pattern_uid, $uid)) {
        http_response_code(409);
        $messageOutput["code"] = "409";
        $messageOutput['error'] = "Malformed username";
        $messageOutput["message"] = "Username does not contain letters, digits and underscore";
        echo json_encode($messageOutput);
        exit();
    }
    //connect to LDAP
    $ldapServer = getenv('LDAP_SERVER') ?: 'ldap://ldap';
    $ldapBaseDn = getenv("LDAP_BASEDN") ?: 'dc=ldap,dc=organization,dc=com'; // Base DN for your LDAP directory
    $ldapConn = ldap_connect($ldapServer);
    if (!$ldapConn) {
        error_log("Fail to connect to LDAP server $ldapServer");
        http_response_code(500);
        $messageOutput["code"] = "500";
        $messageOutput['error'] = "Unkwnon error";
        $messageOutput["message"] = "Something went wrong. Please contact support if the problem persist";
        echo json_encode($messageOutput);
        exit();
    }
    ldap_set_option($ldapConn, LDAP_OPT_PROTOCOL_VERSION, 3);
    ldap_set_option($ldapConn, LDAP_OPT_REFERRALS, 0);
    search_operator_on_ldap($ldapConn, $ldapBaseDn, $uid, $email);
    //connect to db
    $servername = getenv("DB_HOST") ?: "db_host";
    $username = getenv("DB_USER") ?: "db_user";
    $password = getenv("DB_PASSWORD") ?: "db_password";
    $dbname = getenv("DB_NAME") ?: "db_name";
    try{
        $conn = new PDO("mysql:host=$servername;dbname=$dbname", $username, $password);
        $conn->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
        //insert into db as tobedeviced
        $datetime = new DateTime();
        $registration_time = $datetime->format(DateTimeInterface::ATOM);
        $stmt = $conn->prepare("INSERT INTO operator_users (`userid`, `mail`, `timeofregistration`, `statusofregistration`, `organization`) VALUES (:v1,:v2,:v3,'no-devices-yet', :v4);");
        $stmt->bindParam(':v1',$uid);
        $stmt->bindParam(':v2',$email);
        $stmt->bindParam(':v3',$registration_time);
        $stmt->bindParam(':v4',$org);
        try {
           $stmt->execute();
        }
        catch (PDOException $e) {
            ldap_close($ldapConn);
            http_response_code(409);
            $messageOutput["code"] = "409";
            $messageOutput['error'] = "User already exists in database";
            $messageOutput["message"] = "User already registered";
            echo json_encode($messageOutput);
            exit();
        }
        register_user_on_ldap($ldapConn, $ldapBaseDn, $uid, $email, $org);
        //call recover password procedure
        include 'restore_password.php';
    }catch(PDOException $e){
        error_log("Database error: ". $e->getMessage());
        http_response_code(500);
        $messageOutput["code"] = "500";
        $messageOutput['error'] = "Unknown error";
        $messageOutput["message"] = "Something went wrong. Please contact support if the problem persist";
        echo json_encode($messageOutput);
        exit();
    }
}else {
    http_response_code(405);
    $messageOutput["code"] = "405";
    $messageOutput['error'] = "Method not allowed";
    $messageOutput["message"] = "Method not allowed";
    echo json_encode($messageOutput);
    exit();
}

function generate_strong_password(int $length): string {
    // Default minimum distribution giving "mostly lowercase"
    $defaults = [
        'lower' => 6,
        'upper' => 6,
        'digits' => 3,
        'symbols'=> 1,
    ];
    // Character sets
    $sets = [
        'lower' => 'abcdefghijklmnopqrstuvwxyz',
        'upper' => 'ABCDEFGHIJKLMNOPQRSTUVWXYZ',
        'digits' => '0123456789',
        'symbols' => '@$!%*?&',
    ];
    $passwordChars = [];
    // Helper to pick one random char from a string
    $pick = function(string $chars) {
        $i = random_int(0, strlen($chars) - 1);
        return $chars[$i];
    };
    // Add minimum required characters
    foreach ($defaults as $type => $count) {
        $pool = $sets[$type] ?? '';
        for ($i = 0; $i < $count; $i++) {
            $passwordChars[] = $pick($pool);
        }
    }
    // Fill remaining slots with mostly-lowercase characters
    $remaining = $length - count($passwordChars);
    $mostlyPool = $sets['lower']; // "mostly lower"
    for ($i = 0; $i < $remaining; $i++) {
        $passwordChars[] = $pick($mostlyPool);
    }
    // Secure Fisher–Yates shuffle using random_int
    $n = count($passwordChars);
    for ($i = $n - 1; $i > 0; $i--) {
        $j = random_int(0, $i);
        // swap
        $tmp = $passwordChars[$i];
        $passwordChars[$i] = $passwordChars[$j];
        $passwordChars[$j] = $tmp;
    }
    return implode('', $passwordChars);
}
function search_operator_on_ldap($ldapConn, $ldapBaseDn, $uid, $email){
//connect to ldap
    $ldapAdminUser = getenv("LDAP_ADMIN_USER") ?: 'cn=admin,dc=ldap,dc=organization,dc=com'; // The admin user with appropriate privileges
    $ldapAdminPassword = getenv("LDAP_ADMIN_PASSWORD") ?: "ldap_password"; // The password for the admin user
    $ldapBind = ldap_bind($ldapConn, $ldapAdminUser, $ldapAdminPassword);
    if (!$ldapBind) {
        error_log("Fail to bind to LDAP: ". ldap_error($ldapConn));
        ldap_close($ldapConn);
        http_response_code(500);
        $messageOutput["code"] = "500";
        $messageOutput['error'] = "Unknown error";
        $messageOutput["message"] = "Something went wrong. Please contact support if the problem persist";
        echo json_encode($messageOutput);
        exit();
    }
    //check if user with that email + username not already exists in ldap
    $search_filter = "(cn=$uid)";
    $search_result = ldap_search($ldapConn, $ldapBaseDn, $search_filter);
    if (!$search_result) {
        error_log("LDAP search by cn=$uid failed:" . ldap_error($ldapConn));
        ldap_close($ldapConn);
        http_response_code(500);
        $messageOutput["code"] = "500";
        $messageOutput['error'] = "Unknown error";
        $messageOutput["message"] = "Something went wrong. Please contact support if the problem persist";
        echo json_encode($messageOutput);
        exit();
    }
    $entries = ldap_get_entries($ldapConn, $search_result);
    if ($entries["count"] > 0) {
        ldap_close($ldapConn);
        http_response_code(409);
        $messageOutput["code"] = "409";
        $messageOutput['error'] = "Username already exists";
        $messageOutput["message"] = "Fail to register user: username already exist";
        echo json_encode($messageOutput);
        exit();
    }
    $search_filter = "(mail=$email)";
    $search_result = ldap_search($ldapConn, $ldapBaseDn, $search_filter);
    if (!$search_result) {
        error_log("LDAP search by mail=$email failed:" . ldap_error($ldapConn));
        ldap_close($ldapConn);
        http_response_code(500);
        $messageOutput["code"] = "500";
        $messageOutput['error'] = "Unknown error";
        $messageOutput["message"] = "Something went wrong. Please contact support if the problem persist";
        echo json_encode($messageOutput);
        exit();
    }
    $entries = ldap_get_entries($ldapConn, $search_result);
    if ($entries["count"] > 0) {
        ldap_close($ldapConn);
        http_response_code(409);
        $messageOutput["code"] = "409";
        $messageOutput['error'] = "Email already exists";
        $messageOutput["message"] = "Fail to register user: email already exist";
        echo json_encode($messageOutput);
        exit();
    }
}
function register_user_on_ldap($ldapConn, $ldapBaseDn, $uid, $email, $org){
    //create a strong rand password
    $strongPass = generate_strong_password(16);
    //register user to ldap
    $group= getenv("LDAP_GROUP") ?: $ldapBaseDn;
    $ldapRole = getenv("LDAP_OPERATOR_ROLE") ?: 'Manager';
    $ldapOu = $org;
    $newUserDn = 'cn=' . $uid . ',' . $group;
    $encryptedUid = encryptUid($uid); 
    $newUserAttributes = [
        'cn' => $uid,
        'sn' => $uid,
        'uid' => $encryptedUid,
        'mail' => $email,
        'ou' => $ldapOu,
        'userPassword' => hashing_the_password($strongPass),
        'objectclass' => ['top', 'inetOrgPerson']
    ];
    $addUser = ldap_add($ldapConn, $newUserDn, $newUserAttributes);
    if (!$addUser) {
        error_log("Fail to add user to LDAP: ". ldap_error($ldapConn));
        ldap_close($ldapConn);
        http_response_code(500);
        $messageOutput["code"] = "500";
        $messageOutput['error'] = "Unknown error";
        $messageOutput["message"] = "Something went wrong. Please contact support if the problem persist";
        echo json_encode($messageOutput);
        exit();
    }
    // using @ due to the possibility of firing a warning; the ldap user is created, it just misses some permissions, only happens in testing
    $organizational_role = "cn=$ldapRole,$ldapBaseDn";
    $organizational_role_entry = ['roleoccupant' => $newUserDn];
    $modifyEntry = @ldap_mod_add($ldapConn, $organizational_role, $organizational_role_entry);
    if (!$modifyEntry) {
        error_log("Fail to set user as $ldapRole: ". ldap_error($ldapConn));
        ldap_close($ldapConn);
        http_response_code(500);
        $messageOutput["code"] = "500";
        $messageOutput['error'] = "Unknown error";
        $messageOutput["message"] = "Something went wrong. Please contact support if the problem persist";
        echo json_encode($messageOutput);
        exit();
    }
    $organizational_role_2 = "ou=$ldapOu,$ldapBaseDn";
    $organization_entry = ['l' => $newUserDn];
    $modifyEntry_2 = @ldap_mod_add($ldapConn, $organizational_role_2, $organization_entry);
    if (!$modifyEntry_2) {
        error_log("Fail to add user to $ldapOu". ldap_error($ldapConn));
        ldap_close($ldapConn);
        http_response_code(500);
        $messageOutput["code"] = "500";
        $messageOutput['error'] = "Unknown error";
        $messageOutput["message"] = "Something went wrong. Please contact support if the problem persist";
        echo json_encode($messageOutput);
        exit();
    }
    $ldap_groups = getenv("LDAP_OPERATOR_GROUPS") ?: "";
    if($ldap_groups == ""){
        error_log("Fail to get ldap groups. Environment variable not read");
        ldap_close($ldapConn);
        http_response_code(500);
        $messageOutput["code"] = "500";
        $messageOutput['error'] = "Unknown error";
        $messageOutput["message"] = "Something went wrong. Please contact support if the problem persist";
        echo json_encode($messageOutput);
        exit();
    }
    $organizational_role_memberUid = ['memberUid' => $newUserDn];
    $group_list = array_map('trim', explode(',', $ldap_groups));
    foreach ($group_list as $item) {
        $organizational_role_3 = "cn=$item,$ldapBaseDn";
        $modifyEntry_3 = @ldap_mod_add($ldapConn, $organizational_role_3, $organizational_role_memberUid);
        if (!$modifyEntry_3) {
            error_log("Fail to add user to $item". ldap_error($ldapConn));
            ldap_close($ldapConn);
            http_response_code(500);
            $messageOutput["code"] = "500";
            $messageOutput['error'] = "Unknown error";
            $messageOutput["message"] = "Something went wrong. Please contact support if the problem persist";
            echo json_encode($messageOutput);
            exit();
        }
    }
    ldap_close($ldapConn);
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
function hashing_the_password($password) {
    $salt = substr(str_shuffle(str_repeat('ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789',4)),0,4);
    $expose_just_now = '{SSHA}' . base64_encode(sha1( $password.$salt, TRUE ). $salt);
    return $expose_just_now;
    //return '{crypt}'.crypt('$password');
}
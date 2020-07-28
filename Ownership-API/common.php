<?php
/* Snap4city Ownership API
   Copyright (C) 2018 DISIT Lab http://www.disit.org - University of Florence

   This program is free software: you can redistribute it and/or modify
   it under the terms of the GNU Affero General Public License as
   published by the Free Software Foundation, either version 3 of the
   License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU Affero General Public License for more details.

   You should have received a copy of the GNU Affero General Public License
   along with this program.  If not, see <http://www.gnu.org/licenses/>. */

function ownership_access_log($msg) {
  require 'config.php';
  
  $log=fopen($log_path.'/ownership-access.log','a');
  fwrite($log, date('c') . ' ' . @get_client_ip_server() . ' ' . join(' ', $msg) . "\n");
  fclose($log);  
}

function get_limit_user($db, $org, $username, $role, $elementType) {
  if($org==NULL)
    $org = get_user_organization($username);
  $q = "SELECT max(maxCount) FROM limits WHERE (organization='any' OR organization='".mysqli_escape_string($db, $org)."') AND ".
          "(username='any' OR username='".mysqli_escape_string($db, $username)."') AND ".
          "(role='any' OR role='".mysqli_escape_string($db, $role)."') AND ".
          "elementType='".mysqli_escape_string($db, $elementType)."'";
  $r = mysqli_query($db, $q);
  if($r && $c=mysqli_fetch_array($r))
    return array($c[0],$q);
  return array(0, $q." error ".mysqli_error($db));
}

function get_user_organization($username) {
  require 'config.php';
  
  $connection = ldap_connect($ldapServer, $ldapPort);
  ldap_set_option($connection, LDAP_OPT_PROTOCOL_VERSION, 3);
  if(isset($ldapAdminDN) && $ldapAdminDN) {
    ldap_bind($connection, $ldapAdminDN, $ldapAdminPwd);      
  } else {
    ldap_bind($connection);
  }
  
  $ldapUsername = "cn=" . $username . "," . $ldapBaseDN;  
  $result = ldap_search($connection, $ldapBaseDN, '(&(objectClass=organizationalUnit)(l=' . $ldapUsername . '))');
  $entries = ldap_get_entries($connection, $result);
  foreach ($entries as $key => $value) {
    if(is_numeric($key)) {
      if($value["ou"]["0"] != '' ) {
          return $value["ou"]["0"];
      }
    }
  }

  return 'any';
}

function get_user_role($username) {
  require 'config.php';
  
  $connection = ldap_connect($ldapServer, $ldapPort);
  ldap_set_option($connection, LDAP_OPT_PROTOCOL_VERSION, 3);
  if(isset($ldapAdminDN) && $ldapAdminDN) {
    ldap_bind($connection, $ldapAdminDN, $ldapAdminPwd);      
  } else {
    ldap_bind($connection);
  }
  
  $ldapUsername = "cn=" . $username . "," . $ldapBaseDN;  
  $roles = array("RootAdmin","ToolAdmin","AreaManager","Manager","Observer");
  
  foreach($roles as $role) {
    if(checkLdapRole($connection, $ldapUsername, $role, $ldapBaseDN))
      return $role;
  }

  return '';
}

function checkLdapRole($connection, $userDn, $role, $baseDn) 
 {
    $result = ldap_search($connection, $baseDn, '(&(objectClass=organizationalRole)(cn=' . $role . ')(roleOccupant=' . $userDn . '))');
    $entries = ldap_get_entries($connection, $result);
    foreach ($entries as $key => $value) 
    {
       if(is_numeric($key)) 
       { 
          if($value["cn"]["0"] == $role) 
          {
             return true;
          }
       }
    }

    return false;
}

function encryptOSSL($string) {
    require 'config.php';

    $output = false;

    // hash
    $key = hash('sha256', $encryptionInitKey);

    // iv - encrypt method AES-256-CBC expects 16 bytes - else you will get a warning
    $iv = substr(hash('sha256', $encryptionIvKey), 0, 16);

    $output = openssl_encrypt($string, $encryptionMethod, $key, 0, $iv);
    $output = base64_encode($output);


    return $output;
}

function getElementDelegations($elId, $elType, $accessToken) {
  require 'config.php';
  
  $r = http_get($datamanager_api_url."/v3/apps/$elId/delegator?sourceRequest=ownership&elementType=$elType",$accessToken);
  $users = array();
  $orgs = array();
  $grps = array();
  if ($r['httpcode']==200) {
    foreach ($r['result'] as $k) {
      if(isset($k['usernameDelegated'])) {
        $users[] = encryptOSSL($k['usernameDelegated']);
      } else if(isset($k['groupnameDelegated'])){
        if(substr($k['groupnameDelegated'],0,3)==="ou=") {
          $orgs[] = substr($k['groupnameDelegated'],3,strpos($k['groupnameDelegated'],',')-3);
        } elseif(substr($k['groupnameDelegated'],0,3)==="cn=") {
          $grps[] = substr($k['groupnameDelegated'],3,strpos($k['groupnameDelegated'],',')-3);
        }
      }
    }
  }
  $r = http_get($datamanager_api_url."/v1/groupelement/?sourceRequest=ownership&elementId=$elId&elementType=$elType",$accessToken);
  //var_dump($r);
  if ($r['httpcode']==200) {
    foreach ($r['result'] as $k) {
      if(isset($k['deviceGroupId'])) {
        $rr = getElementDelegations($k['deviceGroupId'], 'MyGroup', $accessToken);
        //var_dump($rr);
        $users = array_values(array_unique(array_merge($users,$rr['users'])));
        $orgs = array_values(array_unique(array_merge($orgs,$rr['orgs'])));
        $grps = array_values(array_unique(array_merge($grps,$rr['grps'])));
      }
    }
  }
  return array('users'=>$users,'orgs'=>$orgs,'grps'=>$grps);
}

function wsTunnelRegisterClient(&$app) {
  include 'config.php';
  if(isset($wstunnel_db_host)) {
    $link = mysqli_connect($wstunnel_db_host, $wstunnel_db_user, $wstunnel_db_passw, $wstunnel_database);
    if(!$link) {
      return "fail db connection to ".$wstunnel_db_host;
    }
    
    $client_id = $app->elementId;

    //check if already registered
    $r = mysqli_query($link, "SELECT code FROM client_list WHERE client_id='$client_id'");
    if($r && ($row = mysqli_fetch_array($r))) {
      $app->elementUrl = $iot_app_edge_url . $row[0];
      mysqli_close($link);
      return "OK";
    }
    
    //find a free port
    mysqli_query($link, "LOCK TABLES client_list WRITE");
    $r = mysqli_query($link,"SELECT id,code FROM client_list WHERE client_id IS NULL LIMIT 1");
    if($r && $row= mysqli_fetch_assoc($r)){
      $id = $row['id'];
      $code = $row['code'];
      //echo "update $client_id $code $id \n";

      mysqli_query($link,"UPDATE client_list SET client_id='$client_id' WHERE id=$id");
      $app->elementUrl = $iot_app_edge_url . $code;
      $result = "OK";
    } else {
      //echo 'no space for '.$client_id."\n";
      $app->elementUrl= 'failed no ports';
      $result = "failed no ports available";
    }
    mysqli_query($link, "UNLOCK TABLES");
    mysqli_close($link);
    return "OK";
  }
  return "skipped";
}

function http_get($url, $token) {
  $opts = array('http' =>
      array(
          'method'  => 'GET',
          "header" => "Accept: application/json\r\nAuthorization: Bearer $token\r\n"
      )
  );
  
  # Create the context
  $context = stream_context_create($opts);
  # Get the response (you can use this for GET)
  $result = file_get_contents($url, false, $context);
  //echo "result:$result\n";
  //var_dump($http_response_header);
  return array("httpcode"=>explode(" ",$http_response_header[0])[1],"result"=>json_decode($result, true));
}

function http_post($url, $data, $mimetype) {
  $opts = array('http' =>
      array(
          'method'  => 'POST',
          'header'  => 'Content-type: '.$mimetype,
          'content' => $data,
          'ignore_errors' => true 
      )
  );
  
  # Create the context
  $context = stream_context_create($opts);
  # Get the response (you can use this for GET)
  $result = file_get_contents($url, false, $context);
  //echo "result:$result\n";
  //var_dump($http_response_header);
  return array("httpcode"=>explode(" ",$http_response_header[0])[1],"result"=>json_decode($result, true));
}
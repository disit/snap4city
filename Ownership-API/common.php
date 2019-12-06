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

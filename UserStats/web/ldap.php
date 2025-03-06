<?php

/*
Snap4city -- ldap.php --
   Copyright (C) 2020 DISIT Lab http://www.disit.org - University of Florence
   This program is free software: you can redistribute it and/or modify
   it under the terms of the GNU Affero General Public License as
   published by the Free Software Foundation, either version 3 of the
   License, or (at your option) any later version.
   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU Affero General Public License for more details.
   You should have received a copy of the GNU Affero General Public License
   along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
define(LDAP_OPT_DIAGNOSTIC_MESSAGE, 0x0032);

include_once "./settings.php";

function check_auth_ldap($username, $password, $tool, $role) {
    global $config;
    ini_set('session.save_path', $config['session.save_path']);
    ini_set('session.gc_probability', $config['session.gc_probability']);
    $sessionTimeoutSecs = $config['sessionTimeoutSecs'];
    $ldapServer = $config['ldapServer'];
    $ldapPort = $config['ldapPort'];

    //if (!isset($_SESSION)) {
    //session_name("name");
    //session_set_cookie_params(0, '/', '.disit.org');
    session_start();
    //}

    if (!empty($_SESSION['lastactivity']) && $_SESSION['lastactivity'] > time() - $sessionTimeoutSecs && !isset($_GET['logout'])) {
        // Session is already authenticated
        $ds = ldap_connect($ldapServer, $ldapPort);
        ldap_set_option($ds, LDAP_OPT_PROTOCOL_VERSION, 3);
        $bind = ldap_bind($ds, $username, $password);
        // successfully authenticated
        if ($bind) {
            // check if the user belongs to the Recommender group
            $membership = checkMembership($ds, $username, $tool);
            $role = checkRole($ds, $username, $role);
            ldap_close($ds);
            if ($membership && $role) {
                $_SESSION['lastactivity'] = time();
                return true;
            } else
                return false;
        }
        // wrong credentials or expired session
        else {
            if (ldap_get_option($ds, LDAP_OPT_DIAGNOSTIC_MESSAGE, $extended_error)) {
                echo "Error Binding to LDAP: $extended_error";
            }
            ldap_close($ds);
            unset($_SESSION['lastactivity'], $_SESSION['username'], $_SESSION['password']);
            return false;
        }
    } else if (isset($username) && isset($password)) {
        // Handle login requests
        $ds = ldap_connect($ldapServer, $ldapPort);
        ldap_set_option($ds, LDAP_OPT_PROTOCOL_VERSION, 3);
        $bind = ldap_bind($ds, $username, $password);
        // successfully authenticated
        if ($bind) {
            $membership = checkMembership($ds, $username, $tool);
            $role = checkRole($ds, $username, $role);
            ldap_close($ds);
            if ($membership && $role) {
                $_SESSION['lastactivity'] = time();
                $_SESSION['username'] = $username;
                $_SESSION['password'] = $password;
                return true;
            } else
                return false;
        }
        // wrong credentials or expired session
        else {
            // Auth failed
            if (ldap_get_option($ds, LDAP_OPT_DIAGNOSTIC_MESSAGE, $extended_error)) {
                echo "Error Binding to LDAP: $extended_error";
            }
            ldap_close($ds);
            return false;
        }
    }
}

// check if the user belongs to the Recommender group
function checkMembership($connection, $userDn, $tool) {
    $result = ldap_search(
            $connection, 'dc=ldap,dc=disit,dc=org', // The base from which to start your search. (could be an OU too, like 'ou=restricted,dc=mycompany,dc=com')
            '(&(objectClass=posixGroup)(memberUid=' . $userDn . '))'
    );
    $entries = ldap_get_entries($connection, $result);
    foreach ($entries as $key => $value) {
        if (is_numeric($key)) {
            if ($value["cn"]["0"] == $tool) {
                return true;
            }
        }
    }
    return false;
}

// check if the user is an AreaManager
function checkRole($connection, $userDn, $role) {
    $result = ldap_search(
            $connection, 'dc=ldap,dc=disit,dc=org', // The base from which to start your search. (could be an OU too, like 'ou=restricted,dc=mycompany,dc=com')
            '(&(objectClass=organizationalRole)(cn=' . $role . ')(roleOccupant=' . $userDn . '))'
    );
    $entries = ldap_get_entries($connection, $result);
    foreach ($entries as $key => $value) {
        if (is_numeric($key)) {
            if ($value["cn"]["0"] == $role) {
                return true;
            }
        }
    }
    return false;
}

?>

<?php

// apt-get install php5-ldap nfs-commons cifs-utils
// mkdir /mnt/nfs
// in /etc/fstab
// 192.168.0.137:/var/nfs    /mnt/nfs   nfs rw,user,auto 0 0
// mount -a
// service apache2 restart

define('LDAP_OPT_DIAGNOSTIC_MESSAGE', 0x0032);

function check_auth_ldap($username, $password, $tool, $role) {
    ini_set('session.save_path', '/mnt/nfs');
    ini_set('session.gc_probability', 1);
    $sessionTimeoutSecs = 2678400;
    $ldapServer = '192.168.0.137';
    $ldapPort = 389;

    //if (!isset($_SESSION)) {
    //session_name("name");
    //session_set_cookie_params(0, '/', '.disit.org');
    session_start();
    //}

    if (!empty($_SESSION['lastactivity']) && $_SESSION['lastactivity'] > time() - $sessionTimeoutSecs && !isset($_GET['logout'])) {
        // Session is already authenticated
        $ds = ldap_connect($ldapServer, $ldapPort);
        ldap_set_option($ds, LDAP_OPT_PROTOCOL_VERSION, 3);
        $bind = ldap_bind($ds, 'cn='.$username.',dc=ldap,dc=disit,dc=org', $password);
        // successfully authenticated
        if ($bind) {
            // check if the user belongs to the Recommender group
            $membership = checkMembership($ds, 'cn=' . $username . ',dc=ldap,dc=disit,dc=org', $tool);
            $role = checkRole($ds, 'cn=' . $username . ',dc=ldap,dc=disit,dc=org', $role);
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
        $bind = ldap_bind($ds, 'cn=' . $username . ',dc=ldap,dc=disit,dc=org', $password);
        // successfully authenticated
        if ($bind) {
            $membership = checkMembership($ds, 'cn='.$username.',dc=ldap,dc=disit,dc=org', $tool);
            $role = checkRole($ds, 'cn='.$username.',dc=ldap,dc=disit,dc=org', $role);
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
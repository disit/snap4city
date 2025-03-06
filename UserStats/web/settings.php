<?php
/*
Snap4city -- settings.php --
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
$config['session.save_path'] = '/mnt/nfs';
$config['session.gc_probability'] = 1;
$config['sessionTimeoutSecs'] = 2678400;
$config['ldapServer'] = 'ldap-server';
$config['ldapPort'] = 389;
?>

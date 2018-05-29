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

include("../../session.php");
include("../../config.php");

$db=mysqli_connect($db_host,$db_user,$db_pwd,$database) or die("DB connection error ");

$filter = '';
if(isset($_REQUEST['type'])) {
  $types = explode(";", $_REQUEST['type']);
  $ctypes = array();
  foreach($types as $t)
    $ctypes[] = mysqli_escape_string($db,$t);
  $filter = "AND elementType IN ('".implode("','",$ctypes)."') ";
}

if(isset($_REQUEST['elementId'])) {
  $elIds = explode(";", $_REQUEST['elementId']);
  $ids = array();
  foreach($elIds as $id)
    $ids[] = mysqli_escape_string($db,$id);
  $filter .= "AND elementId IN ('".implode("','",$ids)."') ";
}
if($uinfo->mainRole=='RootAdmin')
  $userFilter = "1 ";
else
  $userFilter = "username='".mysqli_escape_string($db, $uinfo->username)."' ";

$r=mysqli_query($db,"SELECT * FROM ownership WHERE deleted IS NULL AND ".$userFilter.$filter) or die("query error: ".  mysqli_error($db));
echo "[\n";
$i=0;
while($o=mysqli_fetch_object($r)) {
  if($i!=0)
    echo ',';
  //unset($o->id);
  if($o->elementDetails!=null) {
    $o->elementDetails=json_decode($o->elementDetails);
  }
  echo json_encode($o)."\n";
  $i++;
}
echo "]\n";

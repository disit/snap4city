<?php
/* Snap4city IOT Application API
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


function http_get($url) {
  $opts = array('http' =>
      array(
          'method'  => 'GET',
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

function http_delete($url) {
  $opts = array('http' =>
      array(
          'method'  => 'DELETE',
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



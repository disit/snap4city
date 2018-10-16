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

$marathonTasksUrl = "http://192.168.1.187:8080/v2/tasks";
$tasks=http_get($marathonTasksUrl);

if($tasks['httpcode']==200) {
  $tsks=$tasks['result']['tasks'];

  $conf = '';
  foreach($tsks as $t) {
    $appId=$t['appId'];
    $alive=$t['healthCheckResults'][0]['alive'];
    $host = $t['host'];
    $ipAddr = $t['ipAddresses'][0];
    $port = $t['ports'][0];

    $conf .= "location  /nodered$appId/ {
          proxy_set_header Host \$http_host;
          proxy_set_header X-Real-IP \$remote_addr;
          proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
          proxy_set_header X-Forwarded-Proto \$scheme;
          proxy_http_version 1.1;
          proxy_set_header Upgrade \$http_upgrade;
          proxy_set_header Connection \"upgrade\";
          proxy_pass \"http://$host:$port/nodered$appId/\";
  }
  ";
  }

  if($argc>1) {
    $f=fopen($argv[1],"w");
    fwrite($f,$conf);
    fclose($f);
  }
  header("Content-Type: text/plain");
  echo $conf;
} else {
  echo "failed retrieval from marathon $marathonTasksUrl";
}

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
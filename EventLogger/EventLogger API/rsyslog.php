<?php

/* EventLogger API via RSyslog.
   Copyright (C) 2018 DISIT Lab https://www.disit.org - University of Florence

   This program is free software; you can redistribute it and/or
   modify it under the terms of the GNU General Public License
   as published by the Free Software Foundation; either version 2
   of the License, or (at your option) any later version.
   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.
   You should have received a copy of the GNU General Public License
   along with this program; if not, write to the Free Software
   Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA. */

if (filter_input(INPUT_GET, 'p') == "log") {
    
    /*
    if (!empty($_REQUEST["host"])) {
        $host = $_REQUEST["host"];
    }
    */
    
    /*
    if (!empty($_REQUEST["userId"])) {
        $userId = $_REQUEST["userId"];
    }
    */
    
    /*
    if (!empty($_REQUEST["dateTime"])) {
        $dateTime = $_REQUEST["dateTime"];
    }
    */
    
    /*
    if (!empty($_REQUEST["blockId"])) {
        $blockId = $_REQUEST["blockId"];
    }
    */
    
    if (!empty($_REQUEST["pid"])) {
        $pidLocal = $_REQUEST["pid"];
    }
    
    if (!empty($_REQUEST["pidDest"])) {
        $pidExt = $_REQUEST["pidDest"];
    }
    
    if (!empty($_REQUEST["tmstmp"])) {
        $tmstmp = $_REQUEST["tmstmp"];
     //   $dateTime = date("Y-m-d H:i:s", $tmstmp/1000);
     //   
     
        date_default_timezone_set("UTC");
        $dateTime = date("Y-m-d\TH:i:s", $tmstmp/1000);
        $dateTime = $dateTime.".".substr($tmstmp, -3)."Z";
        $dateTime_1sec = substr($dateTime, 0, strlen($dateTime)-5)."Z";
        $dateTime_10sec = substr($dateTime, 0, strlen($dateTime)-6)."0Z";
        $dateTime_1min = substr($dateTime, 0, strlen($dateTime)-8).":00Z";
        $dateTime_10min = substr($dateTime, 0, strlen($dateTime)-9)."0:00Z";
        $dateTime_1h = substr($dateTime, 0, strlen($dateTime)-10)."00:00Z";
        if (intval(substr($dateTime, strlen($dateTime)-13, 2)) >= 12) {
            $dateTime_12h = substr($dateTime, 0, strlen($dateTime)-13)."12:00:00Z";
        } else {
            $dateTime_12h = substr($dateTime, 0, strlen($dateTime)-13)."00:00:00Z";
        }
        $dateTime_1d = substr($dateTime, 0, strlen($dateTime)-13)."00:00:00Z";
    
    /*    
        $dateTime = date("Y-m-d H:i:s", $tmstmp/1000);
        $dateTime = $dateTime.".".substr($tmstmp, -3);
    //    $dateTime = str_replace(" ", "T", $dateTime);
        $dateTime_1sec = substr($dateTime, 0, strlen($dateTime)-4);
        $dateTime_10sec = substr($dateTime, 0, strlen($dateTime)-5)."0";
        $dateTime_1min = substr($dateTime, 0, strlen($dateTime)-7).":00";
        $dateTime_10min = substr($dateTime, 0, strlen($dateTime)-8)."0:00";
        $dateTime = substr($dateTime, 0, strlen($dateTime)-4);
        $stopFlag = 1;
     
     */
        
    }
    
    if (!empty($_REQUEST["modCom"])) {
        $modCom = $_REQUEST["modCom"];
    }
    
    if (!empty($_REQUEST["IP_local"])) {
        $localIP = $_REQUEST["IP_local"];
    }
    
    if (!empty($_REQUEST["IP_ext"])) {
        $extIP = $_REQUEST["IP_ext"];
    }
    
    if (!empty($_REQUEST["payloadSize"])) {
        $payloadSize = $_REQUEST["payloadSize"];
    }
    
    if (!empty($_REQUEST["agent"])) {
        $agent = $_REQUEST["agent"];
    }
    
    if (!empty($_REQUEST["motivation"])) {
        $motivation = $_REQUEST["motivation"];
    }
    
    if (!empty($_REQUEST["lang"])) {
        $lang = $_REQUEST["lang"];
    }
    
    if (!empty($_REQUEST["lat"])) {
        $lat = $_REQUEST["lat"];
    }
    
    if (!empty($_REQUEST["lon"])) {
        $lon = $_REQUEST["lon"];
    }
    
    if (!empty($_REQUEST["serviceUri"])) {
        $serviceUri = $_REQUEST["serviceUri"];
    }
    
    if (!empty($_REQUEST["message"])) {
        $message = $_REQUEST["message"];
    }
    
    if (!empty($_REQUEST["counts"])) {
        $counts = $_REQUEST["counts"];
    }
    
    if (!empty($_REQUEST["appName"])) {
        $appName = $_REQUEST["appName"];
    }
    
    if ((($lat == null) || ($lat == 0) || empty($lat) || !is_numeric($lat)) && (($lon == null) || ($lon == 0) || empty($lon)) || !is_numeric($lon)){
        if (($serviceUri != null) && !empty($serviceUri)) {
          if (strcasecmp($serviceUri, "undefined") != 0) {  
            //   $sparql_query = "SELECT ?lat ?lon WHERE { ".$serviceUri." <http://www.w3.org/2003/01/geo/wgs84_pos#lat> ?lat. ".$serviceUri." <http://www.w3.org/2003/01/geo/wgs84_pos#long> ?lon.}";
            $sparql_query = "SELECT ?lat ?lon WHERE { ".$serviceUri." <http://www.w3.org/2003/01/geo/wgs84_pos%23lat> ?lat. ".$serviceUri." <http://www.w3.org/2003/01/geo/wgs84_pos%23long> ?lon.}";
         //   $queryText = urlencode("http://192.168.0.206:8890/sparql?default-graph-uri=&query=".$sparql_query);
         //   $queryText = "http://192.168.0.206:8890/sparql?default-graph-uri=&query=".$sparql_query;

            // query example:
            // http://192.168.0.206:8890/sparql?default-graph-uri=&query=SELECT ?lat ?lon WHERE { <http://www.disit.org/km4city/resource/6ceacbc32f1e6824ecb71017a668c2a3> <http://www.w3.org/2003/01/geo/wgs84_pos#lat> ?lat. <http://www.disit.org/km4city/resource/6ceacbc32f1e6824ecb71017a668c2a3> <http://www.w3.org/2003/01/geo/wgs84_pos#long> ?lon.

           // $query_results = file_get_contents($queryText);
            $api_header = 'http://servicemap.disit.org/WebAppGrafo/api/v1/?realtime=false&serviceUri=';
            $api_request = $api_header.$serviceUri;
         //   $query_results = file_get_contents($api_request);

            $curl = curl_init();
            curl_setopt_array($curl, array(
                CURLOPT_RETURNTRANSFER => 1,
                CURLOPT_URL => $api_request,
            ));

            $result = curl_exec($curl);
            curl_close($curl);
            $res = json_decode($result, true);
        //    var_dump($res);
            $lon = $res['Service']['features'][0]['geometry']['coordinates'][0];
            $lat = $res['Service']['features'][0]['geometry']['coordinates'][1];
            if ((($lat == null) || ($lat == 0) || empty($lat) || !is_numeric($lat)) && (($lon == null) || ($lon == 0) || empty($lon)) || !is_numeric($lon)){
                $lon = 11.253586;
                $lat = 43.798712;
            }
            if (($lat == 45.76866) && ($lon == 8.990328)){
        	$lon = 11.253586;
        	$lat = 43.798712;
    	    }
          } else {
            $lon = 11.253586;
            $lat = 43.798712;
          }  
        } else {
            $lon = 11.253586;
            $lat = 43.798712;
        }
    
    }
  //  $accessDate = date("Y/m/d H:i:s");
    
 //   $remoteIp = $_SERVER['REMOTE_ADDR'];
 //   $userAgent = $_SERVER['HTTP_USER_AGENT'];
    
    // CHECK SE E' UN SERVIZIO ESTERNO  ANCORA PROVVISORIO CONTROLLARE !!! PER PRENDERE I TOP 10 "INTERNAL" ed "EXTERNAL" e poi fare query su SOLR groupando per ip_local o ip_ext ???
    if ($localIP != null && $extIP != null) {
        if (strcasecmp($extIP, 'undefined') != 0 && strcasecmp($extIP, 'null') != 0 && !empty($extIP)) {
            if (strcasecmp($modCom, 'TX') == 0) {
                if((strpos($localIP, '192.168.') !== false || strpos($localIP, 'disit.org') !== false || strpos($localIP, '172') !== false) || stripos($localIP, 'km4city') !== false || stripos($localIP, 'servicemap') !== false || stripos($localIP, 'rstudio.snap4city.org') !== false) {
                    if (((strpos($extIP, '192.168.') !== false || strpos($extIP, 'disit.org')) !== false || strpos($extIP, '172') !== false) || stripos($extIP, 'km4city') !== false || stripos($extIP, 'servicemap') !== false || stripos($extIP, 'rstudio.snap4city.org') !== false) {
                        $serviceScope = "INTERNAL";
                    } else {
                        $serviceScope = "EXTERNAL";
                    }
                } else {
                    $serviceScope = "EXTERNAL";
                }
            } else if (strcasecmp($modCom, 'RX') == 0) {
                if(strpos($localIP, '192.168.') !== false || strpos($localIP, 'disit.org') !== false || strpos($localIP, '172') !== false || stripos($localIP, 'km4city') !== false || stripos($localIP, 'servicemap') !== false || stripos($localIP, 'rstudio.snap4city.org') !== false) {
                    if(strpos($extIP, '192.168.') !== false || strpos($extIP, 'disit.org') !== false || strpos($extIP, '172') !== false || stripos($extIP, 'km4city') !== false || stripos($extIP, 'servicemap') !== false || stripos($extIP, 'rstudio.snap4city.org') !== false) {
                        $serviceScope = "INTERNAL";
                    } else {
                        $serviceScope = "EXTERNAL";
                    }
                } else {
                    $serviceScope = "EXTERNAL";
                }
            }
        } else {
            $serviceScope = "INTERNAL";
        }
    }
    
    if ($counts != null) {
        if (is_numeric($counts+1)) {
         //   if (!is_long($counts+1)) {
                $counts = $counts + 1 - 1;
         //   } else {
        //        $counts = 0;
        //    }
        } else {
            $counts = 0;
        }
    } else {
        $counts = 0;
    }
    
    if ($counts != null) {
        if (is_numeric($counts+1)) {
         //   if (!is_long($counts+1)) {
                $counts = $counts + 1 - 1;
         //   } else {
        //        $counts = 0;
        //    }
        } else {
            $counts = 0;
        }
    } else {
        $counts = 0;
    }
    
    $logString = "PID = ".$pidLocal.", PID-EXT = ".$pidExt.", MOD-COM = ".$modCom.", IP-LOCAL = ".$localIP.", IP-EXT = ". $extIP.", PAYLOAD = ".$payloadSize.
            ", AGENT = ".$agent.", MOTIVATION = ".$motivation.", LANG = ".$lang.", LAT = ".$lat.", LON = ".$lon.", SERVICE-URI = ".$serviceUri.", MESSAGE = ".$message.
            ", DATE-TIME = ".$dateTime.", DATE-TIME-1SEC = ".$dateTime_1sec.", DATE-TIME-10SEC = ".$dateTime_10sec.
            ", DATE-TIME-1MIN = ".$dateTime_1min.", DATE-TIME-10MIN = ".$dateTime_10min.
            ", DATE-TIME-1H = ".$dateTime_1h.", DATE-TIME-12H = ".$dateTime_12h.
 /* NEW ? */", DATE-TIME-1D = ".$dateTime_1d.", LONG-LAT-FOR-MAP = ".$lat.",".$lon.", SERVICE-SCOPE = ".$serviceScope.", COUNTS = ".$counts.", APP-NAME = ".$appName.";";
 /* OLD-OK!*/ // ", DATE-TIME-1D = ".$dateTime_1d.", LONG-LAT-FOR-MAP = ".$lat.",".$lon.", SERVICE-SCOPE = ".$serviceScope.", COUNTS = ".$counts.";";
    
         //   ", DATE-TIME-1D = ".$dateTime_1d.", LONG-LAT-FOR-MAP = ".$lon.",".$lat.", SERVICE-SCOPE = ".$serviceScope.", COUNTS = ".$counts.", APP-NAME = ".$appName.";"; // reversed "," GEOLOCATION
        //   ", DATE-TIME-1D = ".$dateTime_1d.", LONG-LAT-FOR-MAP = ".$lon." ".$lat.", SERVICE-SCOPE = ".$serviceScope.", COUNTS = ".$counts.", APP-NAME = ".$appName.";";  // reversed " " GEOLOCATION
    
    openlog("RsyslogAPI", 0, LOG_LOCAL0);
  
    syslog(LOG_NOTICE, $logString);
    
    closelog();
    //exec("logger($concStringToLogger)", $output);
    
    //echo $output;
    //echo $concStringToLogger;
    echo $logString;    
    
 /*  
    $solr_collection_name = "syslog";
    $host = "localhost";
    $solr_commit = "http://".$host.":8983/solr/".$solr_collection_name."/update?commit=true";
    $curl2 = curl_init();
    curl_setopt_array($curl2, array(
            CURLOPT_RETURNTRANSFER => 1,
            CURLOPT_URL => $api_request,
     )); 
    $result2 = curl_exec($curl2);
    curl_close($curl2); 
  */
    
 //   $int_flag = 1;
    
}

?>

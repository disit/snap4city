<?php
/* Linked Open Graph
   Copyright (C) 2017 DISIT Lab http://www.disit.org - University of Florence

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

//if($_GET['response_w'] == "sparql"){ //Sends the file for the relation menu.
//	require_once("database-connection.php");
//	echo JSON_ENCODE($response_w);
//}


if($_GET['relations'] == "sparql"){ //Sends the file for the relation menu.
	require_once("config.php");
	echo JSON_ENCODE($relations);
}
if($_GET['configuration']!=''){
	/* Sends the icons of the buttons and the informations for multiple endpoints.*/
	require_once("config.php");
	$conf_to_pass=$configuration;//Gets the button icons.
	$conf_to_pass["active_multiple_endpoints"]=$active_multiple_endpoints;
	$conf_to_pass["multiple_endpoint_priority"]=$multiple_endpoint_priority;
	$conf_to_pass["multiple_endpoints"]=$multiple_endpoints;
	$conf_to_pass["default_image"]=$node_default_image;
	
	echo JSON_ENCODE($conf_to_pass);
}
if($_GET['uri'] != ''){
	require_once("config.php");
	require_once( "includes/mysparql_function.php" ); 
	$sparql_url=$_GET['sparql'];
	if($OD_config=='only'){//Makes only the OD search.
		if(strpos($_GET['uri'],'http://')===false)return;//Checks if is a LD and not a urn
		$result=get_OD($_GET['uri']);
		$resulttmp['relations']=$result; //Returns the EP of the search.
		$resulttmp['EP']=$sparql_url; //Returns the EP of the search.
		$result=$resulttmp;
		echo JSON_ENCODE($result);
		return;
	}
	$result=get_sparql("<".$_GET['uri'] .">", $sparql_url);
	if (!$result && $OD_config=='noresult'){
		if(strpos($_GET['uri'],'http://')!==false)$result=get_first_OD($_GET['uri']);//Checks if is a LD and not a urn
	}
	if($OD_config=="mixed"){
		if(!$result){
			if(strpos($_GET['uri'],'http://')!==false){
				$result_f=get_first_OD($_GET['uri']);
				if($result_f->relations)$result=$result_f;//Changes the result only in the case that LD return somenthing.
			}
			$result['EP']=$sparql_url; //Returns the EP of the search.
		}
		else{
			if(strpos($_GET['uri'],'http://')!==false){
				$result2=get_OD($_GET['uri']);//TODO set get_first_OD if is totaly empty.
				//Merges the 2 results. Merges only the relations.
				$result=result_merge($result,$result2);
			}
			$resulttmp['relations']=$result; //Returns the EP of the search.
			$resulttmp['EP']=$sparql_url; //Returns the EP of the search.
			$result=$resulttmp;
		}	
	}
	echo JSON_ENCODE($result);
}  
elseif($_GET['first_uri'] != '') {
	require_once("config.php");
	require_once( "includes/mysparql_function.php" );
	// print "first uri request";return;
	$sparql_url=$_GET['sparql'];
	$warning=false;
	if($OD_config=='only' || $sparql_url=='LD'){//Makes only the OD search.
		// if(strpos($_GET['first_uri'],'http://')!==false){ 
			$result=get_first_OD($_GET['first_uri']);
			echo JSON_ENCODE($result);
		// }
		return;
	}
	$result=first_sparql( $_GET['first_uri'] , $sparql_url, $warning);
	// var_dump($warning);print "</br></br>"; 
	if($warning=="partial_results"){$result->warning="A Partial result is shown. Were found too many results.";}//For the case of a forced number of results.
	if (count($result->relations)==0 && $OD_config=='noresult'){
		if(strpos($_GET['first_uri'],'http://')!==false)$result=get_first_OD($_GET['first_uri']);
	}
	if($OD_config=="mixed"){
		if(!$result){
			if(strpos($_GET['first_uri'],'http://')!==false){
				$result_f=get_first_OD($_GET['first_uri']);
				if($result_f.relations)$result=$result_f;//Changes the result only in the case that LD return somenthing.
			}
		}
		else{
			if(strpos($_GET['first_uri'],'http://')!==false){
				$result2=get_OD($_GET['first_uri']);
				//Merges the 2 results. Merges only the relations.
				// var_dump($result2->relations);print"</br>";
				$result->relations=result_merge($result->relations,$result2->relations);
			}
		}
	}
	echo JSON_ENCODE($result);
}
elseif($_GET['LD'] != '') {
	require_once("config.php");
	require_once( "includes/mysparql_function.php" );
	$result=get_first_OD($_GET['LD']);
	echo JSON_ENCODE($result);
}
elseif($_GET['literals']){
	require_once("config.php");
	require_once( "includes/mysparql_function.php" ); 
	$sparql_url=$_GET['sparql'];
	if($OD_config=='only'){//Makes only the OD search.
		$result=get_OD_literals($_GET['literals']);
		return;
	}
	$result=get_literals( "<".$_GET['literals'] .">", $sparql_url );
	if($OD_config=='mixed'){
		if($result[0]['value']=='')$result=get_OD_literals($_GET['literals']);
		else{
			$result2=get_OD_literals($_GET['literals']);
			if($result[2]['value']=='')$result=literals_merge($result,$result2);
		}
	}
	elseif($result[0]['value']=='' && $OD_config=='noresult'){
		$result=get_OD_literals($_GET['literals']);
	}
	echo JSON_ENCODE($result);
}
elseif($_GET['more']!=''){
	require_once( "includes/mysparql_function.php" ); 
	$result=get_more($_GET['more'],$_GET['function'] ,$_GET['from'],$_GET['sparql'],$_GET['isInverse']);
	echo JSON_ENCODE($result);
}
elseif($_GET['suggestion']!=''){
	require_once( "includes/mysparql_function.php" ); 
	$result=get_suggestion($_GET['suggestion'],$_GET['text'],$_GET['filter_type'],$_GET['search_InClass']);
	echo JSON_ENCODE($result);
}
elseif($_GET['query_search']!=''){//Makes the query for each sparql endpoint in config and return the list of endpoint and the number of result.
	require_once( "includes/mysparql_function.php" );
	foreach ($_POST['select_list'] as $selectedOption) {
		echo $selectedOption . "\n";
	}
	$result = query_count($_GET['query_search'], $_GET['sparql'], $_GET['sparql_list']);
	echo JSON_ENCODE($result);
}
elseif($_GET['query_view']!=''){//Shows the result of a specific query.
	require_once( "includes/mysparql_function.php" );
	$result=query_columns_view($_GET['query_view'],$_GET['sparql'],$_GET['inbound'],$_GET['offset'],$_GET['limit']);
	echo JSON_ENCODE($result);
}
elseif($_GET['more_view']!=''){//Shows the result of a specific query.
	require_once("config.php");
	require_once( "includes/mysparql_function.php" );
	if($_GET['type']!='more_LD')$result=more_columns_view($_GET['more_view'],$_GET['function'],$_GET['inbound'],$_GET['sparql'],$_GET['offset'],$search_more_result_number,$_GET['key']);
	else $result=LD_more_columns_view($_GET['more_view'],$_GET['function'],$_GET['inbound'],$_GET['sparql'],$_GET['offset'],$search_more_result_number);
	echo JSON_ENCODE($result);
}
elseif($_GET['get_classes']!=''){//Gets the classes for a EndPoint. First checks in the cache, if is not present makes a query.
	require_once("config.php");
	require_once( "includes/mysparql_function.php" );
	//Searches in cache if are present the classes for the sparql.
	$username = $db_username;//Access to the database.
	$password = $db_psw;
	$hostname = $db_host; 
	$schema=$db_schema;
	// connects to the database		
	$output="";
	$dbhandle = mysqli_connect($hostname, $username, $password,$schema) 
	or die("Unable to connect to MySQL");
	$query="SELECT * FROM class_cache WHERE sparql_endpoint='".$_GET['get_classes'] ."' ";
	$result=mysqli_query($dbhandle,$query);
	$r=mysqli_fetch_array($result);
	if($r)//Checks if it's a read code.
	{
		//Checks if the endpoint work.
		if($r['classes']!=''){
			if((time()-$r['timestamp'])<$time_in_cache){//Checks if is present in cache and if it is not out of date.
				echo $r['classes'];return;
			}
			$output=search_classes($_GET['get_classes']);//Makes a query for retrieve the classes on a OpenData.				
			if($output && $output!=""){//Adds the result on the database.
				$json_conf=JSON_ENCODE($output);
				$query="UPDATE class_cache SET classes='".$json_conf."', timestamp=".time() ." WHERE sparql_endpoint= '".$_GET['get_classes'] ."'";
				if (!mysqli_query($dbhandle,$query))
				{
					die('Error: ' . mysqli_error($dbhandle));
				}
				mysqli_close($dbhandle);
			}
		}
	}
	else{//No results in cache founds.
		$output=search_classes($_GET['get_classes']);//Makes a query for retrieve the classes on a OpenData.			
		if($output && $output!=""){//Adds the result on the database.
			$json_conf=JSON_ENCODE($output);
			// var_dump($json_conf);print"</br></br>";return;
			// $f=fopen("classes.log",'at');//Query log.
			// fwrite($f,$json_conf. "    \n \n \n");
			// fclose($f);
			$query="INSERT INTO class_cache (sparql_endpoint,classes, timestamp) VALUES ('".$_GET['get_classes'] ."','".$json_conf."',".time() .")";
			if (!mysqli_query($dbhandle,$query))
			{
				die('Error: ' . mysqli_error($dbhandle));
			}
			mysqli_close($dbhandle);
		}
	}
	echo JSON_ENCODE($output);
}
elseif($_GET['retrieve_configuration']!='') {//retrieve the configuration specified.
	require_once("config.php");
	require_once( "includes/mysparql_function.php" ); 

	//Access to the database.
	$username = $db_username;
	$password = $db_psw;
	$hostname = $db_host; 
	$schema=$db_schema;
	// connects to the database		
	$dbhandle = mysqli_connect($hostname, $username, $password,$schema) 
	or die("Unable to connect to MySQL");
	$parent_id=null;
	if($_POST['parent_id']!=null)$parent_id=$_POST['parent_id'];
	$conf_id="".$email .time();
	$conf_id=md5($conf_id);
	$query="SELECT * FROM graph WHERE readwrite_id='".$_GET['retrieve_configuration'] ."' ";
	$result=mysqli_query($dbhandle,$query);
	$r=mysqli_fetch_array($result);
	if($r)//Check if it's a read code.
	{
		$title=$r['title'];
		$desc=$r['description'];
		if($title==null)$title='"LOG graph"';
		else{ //Prevents the occurence of string without "" 
			if(!(substr($title,0,1)=='"')||!(substr($title,-1,1)=='"')) $title=json_encode($title);
		}
		if($desc==null)$desc='"No description provided"';
		else{ //Prevents the occurence of string without ""
			if(!(substr($desc,0,1)=='"')||!(substr($desc,-1,1)=='"')) $desc=json_encode($desc);
		}
		//TODO Check if the endpoint work.
		//$endpoint=$r['endpoint'];			
		//pass the configuration
		$output=substr($r['config'],1);
		// if($r['status']!="")$output='{"op":"write","status":'.$r['status'] .','.$output;//Specify to js that this link is for read.
		// else $output='{"op":"write",'.$output;//Case for previous saves without status. Specify to js that this link is for read.
		if($r['status']!="")$output='{"op":"write","status":'.$r['status'] .',"title":'.$title .',"desc":'.$desc .','.$output;//Specify to js that this link is for read.
		else $output='{"op":"write","title":'.$title .',"desc":'.$desc .','.$output;//Case for previous saves without status. Specify to js that this link is for read.
		echo($output);						
	}
	else{//Check if the code is for write/read.
		$query="SELECT * FROM graph WHERE id='".$_GET['retrieve_configuration'] ."' ";
		$result=mysqli_query($dbhandle,$query);
		$r=mysqli_fetch_array($result);
		if($r){
			$title=$r['title'];
			$desc=$r['description'];
			if($title==null)$title='"LOG graph"';
			else{ //Prevents the occurence of string without ""
				if(!(substr($title,0,1)=='"')||!(substr($title,-1,1)=='"')) $title=json_encode($title);
			}
			if($desc==null)$desc='"No description provided"';
			else{ //Prevents the occurence of string without ""
				if(!(substr($desc,0,1)=='"')||!(substr($desc,-1,1)=='"')) $desc=json_encode($desc);
			}
			//TODO Check if the endpoint work.
			//$endpoint=$r['endpoint'];				
			//pass the configuration				
			$output=substr($r['config'],1);
			if($r['status']!="")$output='{"op":"read","status":'.$r['status'] .',"title":'.$title .',"desc":'.$desc .','.$output;//Specify to js that this link is for read.
			else $output='{"op":"read","title":'.$title .',"desc":'.$desc .','.$output;//Case for previous saves without status. Specify to js that this link is for read.
			echo($output);				
		}
		else echo JSON_ENCODE($error);
		// else die('Error: ' . mysqli_error($dbhandle));
	}
	mysqli_close($dbhandle);
}
elseif($_GET['more_LD']!=''){//Gets more result for a specified OD.
	require_once( "includes/mysparql_function.php" ); 
	$result=get_more_LD($_GET['more_LD'],$_GET['function'] ,$_GET['from']);
	echo JSON_ENCODE($result);
}
elseif($_GET['graphite']!=''){
	echo JSON_ENCODE(get_graphite($_GET['sparql'],$_GET['uri']));
}

function result_merge($result1,$result2){
	if(!$result1)return $result2;
	if(!$result2)return $result1;
	foreach($result2 as $r2){//Searches if a relation is present.
		// var_dump($r2->uri);print"</br>";
		if(!$result1[$r2->uri]){//Adds the relation.
			$result1[$r2->uri]=$r2;
			// print "Si:   ".$r2->uri ."</br>";
		}
	}
	return $result1;
}
function literals_merge($result1,$result2){
	$result=$result1;
	foreach($result2 as $r2){//Searches if a relation is present.
		// var_dump($r2->uri);print"</br>";
		$present=false;
		foreach($result1 as $r1){
			if($r1['name']==$r2['name']){$present=true;break;}
		}
		if(!present){//Adds the relation.
			$result[]=$r2;
			// print "Si:   ".$r2->uri ."</br>";
		}
	}
	return $result;
}
?>
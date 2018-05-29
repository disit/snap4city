<?php

/* Snap4city Drupal GDPR module
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

/* ********** 

SwissKnife Functions


***********/






function retrieve_user_name_by_mail($email){


    db_set_active('default');


    $query_user=db_select('users','u');
    $query_user->condition('u.mail',$email,'=');

    $query_user->fields('u', array('name',));
    

    $result = $query_user->execute();


    $rows=array();
    while ($row = $result->fetchAssoc()) {
                $rows[]=
                    //array(
                    $row['name'];

                    //);


    }

    
    return $rows;

      
    
}



/*Function to export csv */
function export_csv($variables) {

    db_set_active('default');

   
    drupal_add_http_header('Content-Type', 'text/csv; utf-8');
    drupal_add_http_header('Content-Disposition', 'attachment; filename=export.csv');

    $output = '';
    $keys = array();

    for ($i = 0; $i < count($variables); $i++) {
        for ($j=0;$j < 6;$j++){
            if($j==5)
                $output.=$variables[$i][$j]['data'];
            else
                $output.=$variables[$i][$j]['data'].",";
        }
        $output.="\n";
    }
    


    echo $output;
    exit;
}


/*Retrieve User name by email */
function retrieve_user_username_by_mail($email){


    //echo $email;
    
    db_set_active('default');
    $query_user=db_select('users','u');
    $query_user->condition('u.mail',$email,'=');

    $query_user->fields('u', array('name',));
    

    $result = $query_user->execute();


    $rows=array();
    while ($row = $result->fetchAssoc()) {
                $rows[]=
                    //array(
                    $row['name'];

                    //);


    }

    
    return $rows;

   
    
    
}

/* Update delegation table by username

OLD Way : insert into delegation ('',username_delegator, username_delegated, motivation, variable_name)

New Way: insert into delegation ('',username_delegator, username_delegated, motivation, variable_name, elementi_id(app_id), element_type)
*/
function update_delegation_table($username,$app_id,$motivation,$variable_name){


    global $user;
    db_set_active('profiledb');


   //insert into delegation values ('',user->name, username, motivation,variaible_name)
    
    db_insert('delegation')
        ->fields(array(
            'username_delegator' => $user->name,
            'username_delegated' => $username,
            'motivation' => $motivation,
            'variable_name' => $variable_name,
            'element_id'=> $app_id,
            'element_type' => 'AppID'
            
        ))
        ->execute();


    db_set_active('default');
    
}

function update_myannotation_delegation_table($username,$variable_name){


    global $user;
    



   //insert into delegation values ('',user->name, username, motivation,variaible_name)
  
  

   db_set_active('profiledb');
   db_insert('delegation')
        ->fields(array(
            'username_delegator' => $user->name,
            'username_delegated' => $username,
            'motivation' => 'Annotation',
            'variable_name' => $variable_name,
            'element_id'=> NULL,
            'element_type' => NULL
            
        ))
        ->execute();


    db_set_active('default');
    
}


/* Return table themed of name, mail about actual user */





function retrieve_mypersonaldata_app_list()
{

    global $user;


    $output = '';

    $table_rows = array();


    $header_row = array(

        array('data' => 'app_id', 'field' => 'da.app_id'),
        array('data' => 'app_name', 'field' => 'da.app_name'),
        
    );
    
    //select app_id, app_name from data where username = drupal user and delete_time = NULL
    
    db_set_active('profiledb');

    $query = db_select('data', 'da');
    $query->distinct();
    $query->condition('da.username',$user->name,'='); 
    $query->isNull('da.delete_time');
    $query->fields('da', array('app_id','app_name'));

//    $query = $query
    //      ->extend('TableSort')
    //  ->orderByHeader($header_row);
    $result = $query->execute();
    
     
     

    foreach($result as $stat_line) {

        $field_1='APP:             <a href=appdetails?='.$stat_line->app_id.'>'.$stat_line->app_name.'</a>';
        $field_2='('.$stat_line->app_id.')';
        $table_rows[] = array(
           
            array('data' => $field_1),
            array('data' => $field_2),

           
             
        );

    }

    //restore drupaldb flow
    db_set_active('default');
    
    //return a themed schedule table
    return theme_table(
        array(
            'header' => array(array('data'=>'')),
            'rows' => $table_rows,
            'attributes' => array('class' => 'table_dfs', 'width' => '100%'),
            'caption' => '',
            'colgroups' => array(),
            'sticky' => FALSE,
            'empty' => 'No App data is available',
        ));  
     
     
     
     
     
    
}


?>
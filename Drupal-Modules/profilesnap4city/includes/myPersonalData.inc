<?php

/* Snap4City profilesnap4city for drupal 7.
   Copyright (C) 2015 DISIT Lab http://www.disit.org - University of Florence

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
   
/* Used by Userinfo*/
function retrieve_drupaldata()
{

    global $user;

    $output = '';
    $table_rows = array();



    $header_row = array(
        array('data' => 'name', 'field' => 'u.name'),
        array('data' => 'mail', 'field' => 'u.mail'),


    );
    

   

    $query_user=db_select('users','u');
    $query_user->condition('u.uid',$user->uid,'=');

    $query_user->fields('u', array('mail','name',));
    $query_user = $query_user
        ->extend('TableSort')
        ->orderByHeader($header_row);

    $result = $query_user->execute();



    foreach($result as $stat_line) {

        $table_rows[] = array(
            
            array('data' => $stat_line->name),
            array('data' => $stat_line->mail),
             
             
        );

    }


    return theme_table( array('header' => $header_row,
    'rows' => $table_rows,
    'attributes' => array('class' => 'table_dfs', 'width' => '100%'),
    'caption' => '',
    'colgroups' => array(),
    'sticky' => FALSE,
    'empty' => 'No data is available',
    ));  
     
    
    
    
     
    
}


/*

  List of all owned app 
  Return a Rendered table
*/


function retrieve_mypersonaldata_app_list_datatable()
{

    global $user;


    $output = '';

    $table_rows = array();

   
    // Table settings.
    $attributes['datatable_options'] = array(
        'bFilter'   => TRUE,
        'bInfo'     => TRUE,
    );



    // Define table columns
    $header = array(
        array(
            'data' => t('IOT Application name'),
            'datatable_options' => array(
                'bSortable' => TRUE,
                'bSearchable' => TRUE,
            ),
        ),

    );
    // Table data.
   



    
    //select app_id, app_name from data where username = drupal user and delete_time = NULL
    
    db_set_active('profiledb');

    $query = db_select('data', 'da');
    $query->distinct();
    $query->condition('da.username',$user->name,'='); 
    $query->isNull('da.delete_time');
    $query->isNotNull('da.app_id');
    $query->isNotNull('da.app_name');
    $query->fields('da', array('app_id','app_name'));
    $result = $query->execute();
    
     
     

    foreach($result as $stat_line) {

        $field_1='<a href=appdetails?='.$stat_line->app_id.'>'.$stat_line->app_name.'</a>';
        $field_2='('.$stat_line->app_id.')';
        $table_rows[] = array(
           
            array('data' => $field_1),
            //    array('data' => $field_2),

           
             
        );

    }

    //restore drupaldb flow
    db_set_active('default');
     

    
    // Or, render using a theme function.
    $variables = array(
        'attributes' => $attributes,
        'header' => $header,
        'rows' => $table_rows,
    );

    return theme_datatable($variables);
    
     
     
    
}


function retrieve_mypersonaldata_variable_name($app_id)
{

    global $user;


    $rows=array();
    
    db_set_active('profiledb');
    $query = db_select('data', 'da');

    $query->condition('da.username',$user->name,'=');
    $query->distinct();
  
    $query->isNull('da.delete_time');
    $query->condition('da.app_id',$app_id,'=');
    $query->fields('da', array('variable_name'));

    $result = $query->execute();
    


    $i=0;
    
    while ($row = $result->fetchAssoc()) {
        $rows[$i]=
                    
            $row['variable_name'];
                    
        $i++;
                    


    }

    db_set_active('default');
    return $rows;
  
      
    
}
function retrieve_mypersonaldata_motivation($app_id)
{

    global $user;



    $rows=array();
    
    db_set_active('profiledb');

    $query = db_select('data', 'da');

    $query->condition('da.username',$user->name,'=');
    $query->distinct();
 
    $query->isNull('da.delete_time');
    $query->condition('da.app_id',$app_id,'=');
   

    $query->fields('da', array('motivation'));

    $result = $query->execute();
    

    $i=0;
    while ($row = $result->fetchAssoc()) {
        $rows[$i]=
            $row['motivation'];
        $i++;
    }

    db_set_active('default');
    return $rows;
    
    
}


function retrieve_mypersonaldata_app($app_id)
{

    global $user;




    $rows=array();
    
    db_set_active('profiledb');

    $query = db_select('data', 'da');

    $query->condition('da.username',$user->name,'=');
    $query->distinct();
 
    $query->isNull('da.delete_time');
    
    $query->condition('da.app_id',$app_id,'=');
    

    $query->fields('da', array('app_name','app_id'));

    $result = $query->execute();
    

    
    while ($row = $result->fetchAssoc()) {
        $rows[]=
            array(
                $row['app_name'],
                    
                    

            );


    }

    db_set_active('default');
    return $rows;
    
}


function retrieve_mypersonaldata_datatables($app_id)
{

    global $user;


    $output = '';

    $table_rows = array();

   

// Table settings.
    $attributes['datatable_options'] = array(
        'bFilter'   => TRUE,
        'bInfo'     => TRUE,
    );


    // Define table columns
    $header = array(
        array(
            'data' => 'Date',
            'datatable_options' => array(
                'bSortable' => TRUE,
                'bSearchable' => TRUE,
            ),
        ),
        array(
            'data' => 'IOT Application name',
            'datatable_options' => array(
                'bSortable' => TRUE,
                'bSearchable' => TRUE,
            ),
        ),
        array(
            'data' => 'Variable name',
            'datatable_options' => array(
                'bSortable' => TRUE,
                'bSearchable' => TRUE,
            ),
        ),
        array(
            'data' => 'Value',
            'datatable_options' => array(
                'bSortable' => TRUE,
                'bSearchable' => TRUE,
            ),
        ),
        array(
            'data' => 'Unit',
            'datatable_options' => array(
                'bSortable' => TRUE,
                'bSearchable' => TRUE,
            ),
        ),
        array(
            'data' => 'motivation',
            'datatable_options' => array(
                'bSortable' => TRUE,
                'bSearchable' => TRUE,
            ),
        ),
    );

    
    
    db_set_active('profiledb');
    $query = db_select('data', 'da');
    $query->condition('da.username',$user->name,'=');
    $query->condition('da.app_id',$app_id,'=');
    $query->isNull('da.delete_time');
   
    $query->fields('da', array('data_time','app_name','variable_name', 'variable_value','variable_unit', 'motivation' ));

    $result = $query->execute();
    
  
     

    foreach($result as $stat_line) {

        $table_rows[] = array(
            array('data' => $stat_line->data_time),
            array('data' => $stat_line->app_name),
            array('data' => $stat_line->variable_name),
            array('data' => $stat_line->variable_value),
            array('data' => $stat_line->variable_unit),
            array('data' => $stat_line->motivation),

             
             
        );

    }


    $variables = array(
        'attributes' => $attributes,
        'header' => $header,
        'rows' => $table_rows,
    );



    
    db_set_active('default');
    return theme_datatable($variables);
    
}


function retrieve_mypersonaldata_delegate_myapp($app_id)
{

    global $user;


    $output = '';

    $table_rows = array();


    $header_row = array(
       
        array('data' => 'User delegated', 'field' => 'de.username_delegated'),
        array('data' => 'Variable name', 'field' => 'de.variable_name'),
        array('data' => 'Delegation motivation', 'field' => 'de.motivation'),
        array('data' => 'id', 'field' => 'de.id'),

    );
    

    //select username_delegated, variable_name, motivation,id from delegation where username_delegator = user and element_id = app_id
   
    db_set_active('profiledb');
    $query = db_select('delegation', 'de');
    $query->condition('de.username_delegator',$user->name,'='); 
    $query->condition('de.element_id',$app_id,'=');
    $query->isNull('de.delete_time');
    $query->isNotNull('de.username_delegated');
    
    $query->fields('de', array('username_delegated','variable_name','motivation','id'));
    $query = $query
        ->extend('TableSort')
        ->orderByHeader($header_row);
    $result = $query->execute();
    

    
    db_set_active('default');

    
    /*
      GET the email per userid minimizing query 

      
    */


    foreach($result as $stat_line) {

        // $delegated[]= $stat_line->username_delegated;


        if (strcmp($stat_line->username_delegated,'ANONYMOUS')==0){
            $stat_line->username_delegated='PUBLIC/ANONYMOUS';
        }
       
       
        $table_rows[] = array(
            array('data' => $stat_line->username_delegated),
            array('data' => $stat_line->variable_name),
            array('data' => $stat_line->motivation),
            array('data' => $stat_line->id),
                 
        );
     
    }


     
    return $table_rows; 
     
     
    
}








/*For Group  */

function retrieve_mypersonaldata_group_delegate_myapp($app_id)
{

    global $user;


    $output = '';

    $table_rows = array();


    $header_row = array(
       
        array('data' => 'Group delegated', 'field' => 'de.groupname_delegated'),
        array('data' => 'Variable name', 'field' => 'de.variable_name'),
        array('data' => 'Delegation motivation', 'field' => 'de.motivation'),
        array('data' => 'id', 'field' => 'de.id'),

    );
    

    //select group_delegated, variable_name, motivation,id from delegation where username_delegator = user and element_id = app_id
   
    db_set_active('profiledb');
    $query = db_select('delegation', 'de');
    $query->condition('de.username_delegator',$user->name,'='); 
    $query->condition('de.element_id',$app_id,'=');
    $query->isNull('de.username_delegated');
    $query->isNull('de.delete_time');
    $query->fields('de', array('groupname_delegated','variable_name','motivation','id'));
    $query = $query
        ->extend('TableSort')
        ->orderByHeader($header_row);
    $result = $query->execute();
    
   
    db_set_active('default');


    foreach($result as $stat_line) {

        $dn_part=ldap_explode_dn($stat_line->groupname_delegated,0);

        if (strpos($stat_line->groupname_delegated, 'cn') !== false) 
        
        {
        
        $cn_part_explode=explode('=',$dn_part[0]);
        $cn=$cn_part_explode[1];
        $ou_part_explode=explode('=',$dn_part[1]);
        $ou=$ou_part_explode[1];

            }
        else{
            $cn='';
            $ou_part_explode=explode('=',$dn_part[0]);
            $ou=$ou_part_explode[1];
        }
        
        
       


       
        $table_rows[] = array(

            
            
            
            array('data' => $cn),
            array('data' => $ou),
            array('data' => $stat_line->variable_name),
            array('data' => $stat_line->motivation),
            array('data' => $stat_line->id),
                 
        );
     
    }

 


     
    return $table_rows; 
     
     
    
}




function profilesnap4city_delegation_group_add_more($form, &$form_state) {


    $url=$_SERVER['REQUEST_URI'];
    $app_id = substr($url, strrpos($url, '=') + 1);



    if (strcmp($app_id,'drupal/system/ajax')!=0){

        $_SESSION['current_app_id']=$app_id;
    }
    
    



    /*
      Read data from Delegation Table
    */

    $delegation_rows=retrieve_mypersonaldata_group_delegate_myapp($app_id);


    /*

      Build container

    */


    $form['some_text'] = array(
        '#markup' => '<h2>Actual delegation for Group</h2>'
    );


    if(count($delegation_rows)>0){

  
       
        $form['field_container'] = [
            '#type' => 'container',
            '#weight' => 80,
            '#tree' => TRUE,
            '#prefix' => '<div id="js-ajax-elements-wrapper-my-group">',
            '#suffix' => '</div>',
        ];



    
    
        $form_state['field_deltas'] = isset($form_state['field_deltas']) ? $form_state['field_deltas'] : range(0, count($delegation_rows)-1);

        $field_count = $form_state['field_deltas'];


        foreach ($field_count as $delta) {

        

            $form['field_container'][$delta] = [
                '#type' => 'container',
                '#attributes' => [
                    'class' => ['container-inline'],
                ],
                '#tree' => TRUE,
            ];

            $form['field_container'][$delta]['field1'] = [
            
                '#type' => 'textfield',
                '#value'=>$delegation_rows[$delta][0]['data'],
                
                '#attributes' => array('readonly' => 'readonly'),

                '#title' => t('Group Name' ),
                '#size' => 25,
            ];

            $form['field_container'][$delta]['field2'] = [
            
                '#type' => 'textfield',
                '#value'=>$delegation_rows[$delta][1]['data'],
                //'#disabled'=>'TRUE',
                '#attributes' => array('readonly' => 'readonly'),

                '#title' => t('OU' ),
                '#size' => 25,
            ];
            
            $form['field_container'][$delta]['field3'] = [
            
                '#type' => 'textfield',
                '#value'=>$delegation_rows[$delta][2]['data'],
                //'#disabled'=>'TRUE',
                '#attributes' => array('readonly' => 'readonly'),
                '#title' => t('Variable name' ),
                '#size' => 25,
            ];

        
   
            $form['field_container'][$delta]['field4'] = [
            
                '#type' => 'textfield',
                '#value'=>$delegation_rows[$delta][3]['data'],
                //'#disabled'=>'TRUE',
                '#attributes' => array('readonly' => 'readonly'),
                '#title' => t('Motivation' ),
                '#size' => 25,
            ];

        
            $form['field_container'][$delta]['field5'] = [
            
                '#type' => 'hidden',
                '#value'=>$delegation_rows[$delta][4]['data'],
                //'#disabled'=>'TRUE',
            
            ];
        
       
    
        
 
            $form['field_container'][$delta]['remove_name'] = [
                '#type' => 'submit',
                '#value' => t('Remove'),
                '#submit' => ['profilesnap4city_delegation_group_add_more_remove'],
                
                '#ajax' => [
                
                    'callback' => 'profilesnap4city_delegation_group_add_more_remove_callback',
                    'wrapper' => 'js-ajax-elements-wrapper-my-group',
                    'method' => 'replace',
                    'effect' => 'fade',
                    
                ],
                '#weight' => -50,
                '#attributes' => [
                    'class' => ['button-small'],
                ],
                '#name' => 'remove_name_' . $delta,
            ];
            



        }

    }


    
    return $form;
}





/*Remove 1*/
function profilesnap4city_delegation_group_add_more_remove($form, &$form_state) {


    
    $delta_remove = $form_state['triggering_element']['#parents'][1];

    
    $k = array_search($delta_remove, $form_state['field_deltas']);


    
    
    $_SESSION['Remove_Share_id']=$form_state['values']['field_container'][$k]['field5'];

   
    unset($form_state['field_deltas'][$k]);

       
    $form_state['rebuild'] = TRUE;
    drupal_get_messages();


    
}

/* Remove 2*/
function profilesnap4city_delegation_group_add_more_remove_callback($form, &$form_state) {

    drupal_set_message('Delegation removed');

    $id= $_SESSION['Remove_Share_id'];

    $app_id= $_SESSION['current_app_id'];
    


    /* openlog("MODULE", 0, LOG_LOCAL0); */
    /* syslog(LOG_WARNING, 'In callback:'.$app_id); */

    
    db_set_active('profiledb');

    $date = date('Y-m-d H:i:s');

    $newdate = strtotime('+1 month' , strtotime ($date));

    $newdate = date ( 'Y-m-d H:i:s' , $newdate );



    db_update('delegation')
        ->fields(array(
            'delete_time' => $date,

        ))
        ->condition('id', $id)
        ->execute();

    
    db_set_active('default');


    //return $form;



    ctools_include('ajax');
    $commands[] = ctools_ajax_command_redirect('appdetails', 0,array('query'=>array('appid'=>$app_id)));
    return array('#type' => 'ajax', '#commands' => $commands);
    

}



function retrieve_mypersonaldata_group_delegate()
{

    global $user;



    $output = '';

    $table_rows = array();



    //select username_delegated, variabile_name, motivation, id where username_delegator = $user->name

    $header_row = array(
       
        array('data' => 'Group delegated', 'field' => 'de.groupname_delegated'),
        array('data' => 'variable_name', 'field' => 'de.variable_name'),
        array('data' => 'Delegation motivation', 'field' => 'de.motivation'),
        array('data' => 'id', 'field' => 'de.id'),

    );
    
    
   
    db_set_active('profiledb');
    $query = db_select('delegation', 'de');
    $query->condition('de.username_delegator',$user->name,'='); 
  
    $query->fields('de', array('groupname_delegated','variable_name','motivation','id'));
    $query = $query
        ->extend('TableSort')
        ->orderByHeader($header_row);
    $result = $query->execute();
    

    
    db_set_active('default');

    /*
      GET the email per userid minimizing query 
    */


    

    $delegated=array();
     

    foreach($result as $stat_line) {

        $delegated[]= $stat_line->groupname_delegated;

        $table_rows[] = array(
            array('data' => $stat_line->groupname_delegated),
            array('data' => $stat_line->variable_name),
            array('data' => $stat_line->motivation),
            array('data' => $stat_line->id),
                 
        );
     
    }



    return $table_rows; 
     
     
    
}



/*End For Group  */




/*Main Function for trating delegation */

function profilesnap4city_delegation_add_more($form, &$form_state) {


    $url=$_SERVER['REQUEST_URI'];
    $app_id = substr($url, strrpos($url, '=') + 1);



    if (strcmp($app_id,'drupal/system/ajax')!=0){

        $_SESSION['current_app_id']=$app_id;
    }
    
    



    /*
      Read data from Delegation Table
    */

    $delegation_rows=retrieve_mypersonaldata_delegate_myapp($app_id);


    /*

      Build container

    */


    $form['some_text'] = array(
        '#markup' => '<h2>Actual delegation</h2>'
    );


    //$form['cv']='TEST';

    if(count($delegation_rows)>0){

  
       
        $form['field_container'] = [
            '#type' => 'container',
            '#weight' => 80,
            '#tree' => TRUE,
            // Set up the wrapper so that AJAX will be able to replace the fieldset.
            '#prefix' => '<div id="js-ajax-elements-wrapper-my">',
            '#suffix' => '</div>',
        ];



    
    
        $form_state['field_deltas'] = isset($form_state['field_deltas']) ? $form_state['field_deltas'] : range(0, count($delegation_rows)-1);

        $field_count = $form_state['field_deltas'];


        foreach ($field_count as $delta) {

        

            $form['field_container'][$delta] = [
                '#type' => 'container',
                '#attributes' => [
                    'class' => ['container-inline'],
                ],
                '#tree' => TRUE,
            ];

            $form['field_container'][$delta]['field1'] = [
            
                '#type' => 'textfield',
                '#value'=>$delegation_rows[$delta][0]['data'],
                //'#disabled'=>'TRUE',
                '#attributes' => array('readonly' => 'readonly'),

                
                '#title' => t('User' ),
                '#size' => 25,
            ];

            $form['field_container'][$delta]['field2'] = [
            
                '#type' => 'textfield',
                '#value'=>$delegation_rows[$delta][1]['data'],
                //'#disabled'=>'TRUE',
                '#attributes' => array('readonly' => 'readonly'),
                '#title' => t('Variable name' ),
                '#size' => 25,
            ];

        
   
            $form['field_container'][$delta]['field3'] = [
            
                '#type' => 'textfield',
                '#value'=>$delegation_rows[$delta][2]['data'],
                //'#disabled'=>'TRUE',
                '#attributes' => array('readonly' => 'readonly'),
                '#title' => t('Motivation' ),
                '#size' => 25,
            ];

        
            $form['field_container'][$delta]['field4'] = [
            
                '#type' => 'hidden',
                '#value'=>$delegation_rows[$delta][3]['data'],
                //'#disabled'=>'TRUE',
            
            ];
        
       
    
        
 
            $form['field_container'][$delta]['remove_name'] = [
                '#type' => 'submit',
                '#value' => t('Remove'),
                '#submit' => ['profilesnap4city_delegation_add_more_remove'],
                // See the examples in ajax_example.module for more details on the
                // properties of #ajax.
                '#ajax' => [
                
                    'callback' => 'profilesnap4city_delegation_add_more_remove_callback',
                    'wrapper' => 'js-ajax-elements-wrapper-my',
                    'method' => 'replace',
                    'effect' => 'fade',
                    
                ],
                '#weight' => -50,
                '#attributes' => [
                    'class' => ['button-small'],
                ],
                '#name' => 'remove_name_' . $delta,
            ];
            



        }

    }

    openlog("MODULE", 0, LOG_LOCAL0);
    syslog(LOG_WARNING, 'In source:'.$_SESSION['current_app_id']);

    
    return $form;
}





/*Remove 1*/
function profilesnap4city_delegation_add_more_remove($form, &$form_state) {


    
    $delta_remove = $form_state['triggering_element']['#parents'][1];

    
    $k = array_search($delta_remove, $form_state['field_deltas']);

    
    $_SESSION['Remove_Share_id']=$form_state['values']['field_container'][$k]['field4'];
    
   
    unset($form_state['field_deltas'][$k]);

   
    
    $form_state['rebuild'] = TRUE;


    // $app_url= $_SESSION['app_url'];
    

    // $form_state['redirect'] = 'http://192.168.0.47'.$app_url;


    drupal_get_messages();


    
}

/* Remove 2*/
function profilesnap4city_delegation_add_more_remove_callback($form, &$form_state) {

    drupal_set_message('Delegation removed');

    $id= $_SESSION['Remove_Share_id'];

    $app_id= $_SESSION['current_app_id'];
    


    /* openlog("MODULE", 0, LOG_LOCAL0); */
    /* syslog(LOG_WARNING, 'In callback:'.$app_id); */

    
    db_set_active('profiledb');

    $date = date('Y-m-d H:i:s');

    $newdate = strtotime('+1 month' , strtotime ($date));

    $newdate = date ( 'Y-m-d H:i:s' , $newdate );



    db_update('delegation')
        ->fields(array(
            'delete_time' => $date,

        ))
        ->condition('id', $id)
        ->execute();

    
    db_set_active('default');


    //return $form;



    ctools_include('ajax');
    $commands[] = ctools_ajax_command_redirect('appdetails', 0,array('query'=>array('appid'=>$app_id)));
    return array('#type' => 'ajax', '#commands' => $commands);
    

}



function retrieve_mypersonaldata_delegate()
{

    global $user;



    $output = '';

    $table_rows = array();



    //select username_delegated, variabile_name, motivation, id where username_delegator = $user->name

    $header_row = array(
       
        array('data' => 'User delegated', 'field' => 'de.username_delegated'),
        array('data' => 'variable_name', 'field' => 'de.variable_name'),
        array('data' => 'Delegation motivation', 'field' => 'de.motivation'),
        array('data' => 'id', 'field' => 'de.id'),

    );
    
    
   
    db_set_active('profiledb');
    $query = db_select('delegation', 'de');
    $query->condition('de.username_delegator',$user->name,'='); 
  
    $query->fields('de', array('username_delegated','variable_name','motivation','id'));
    $query = $query
        ->extend('TableSort')
        ->orderByHeader($header_row);
    $result = $query->execute();
    

    
    db_set_active('default');

    /*
      GET the email per userid minimizing query 
    */


    

    $delegated=array();
     

    foreach($result as $stat_line) {

        $delegated[]= $stat_line->username_delegated;

        $table_rows[] = array(
            array('data' => $stat_line->username_delegated),
            array('data' => $stat_line->variable_name),
            array('data' => $stat_line->motivation),
            array('data' => $stat_line->id),
                 
        );
     
    }



    if(count($delegated)>0){
    
          
        $query_user_mail=db_select('users','u');
        $query_user_mail->condition('u.name',$delegated,'in');

        $query_user_mail->fields('u', array('name','mail'));
        $result_mail = $query_user_mail->execute();
 

        foreach($result_mail as $stat_line) {

            // $table_email[] =array('uid'=>$stat_line->uid,'mail'=> $stat_line->mail);
            $table_email[$stat_line->name]= $stat_line->mail;
        }



        foreach($table_rows as $key => $table_row){
            $table_rows[$key][0]['data']=$table_email[$table_rows[$key][0]['data']];
        }
          
    }

     
    return $table_rows; 
     
     
    
}








function profilesnap4city_add_delegation_form($form, &$form_state) {




    $app_id=$_SESSION['current_app_id'];
    
    $my_variable_name=retrieve_mypersonaldata_variable_name($app_id);
    $my_motivation=retrieve_mypersonaldata_motivation($app_id);
    $my_app=retrieve_mypersonaldata_app($app_id);


    $form['some_text'] = array(
        '#markup' => '<h2>New delegation</h2>'
    );


    // App checkboxes field container.
    $form['app'] = array(
        '#type' => 'container',

        //'#tree' => TRUE,
    
    
    );

     
    $form['app']['motivation'] = array(
        '#type' => 'select',
        '#title'=> t('Motivation'),
        
        '#options' => $my_motivation,
        

    );
    $form['app']['variable_name'] = array(
        '#type' => 'select',
        '#title'=> t('Variable name'),
       
        '#options' => $my_variable_name,
        
    );
    


    $form['checkuser'] = array(
        '#title' => 'Make Public',
        '#type' => 'checkbox',
        '#options' => drupal_map_assoc(array(t('Yes'), t('No'))),
        '#ajax' => array(
            'callback' => 'user_callback_form',
            'wrapper' => 'user-check',
            'method' => 'replace',
            'effect' => 'fade',

        ),

        '#validated' => True,
        
        '#states' => array(
            "visible" => array(
                "input[name='checkgroup']" => array("checked" => FALSE)),
        ),
    );
    



    $form['app']['mytextcontainer'] = array(
        '#type' => 'container',
        '#states' => array(
            "visible" => array(
                "input[name='checkuser']" => array("checked" => FALSE)),
        ),
    );

    $form['app']['mytextcontainer']['user'] = array(
        '#type' => 'textfield',
        '#title' => t('User Email'), 
        '#weight' => 2,
        '#required' => (isset($form_state["values"]["user"]) ? ($form_state["values"]["user"] == true ? TRUE : FALSE) : TRUE),
        '#prefix' => '<div id="user-check">',
        '#suffix' => '</div>',

        '#states' => array(
            "visible" => array(
                "input[name='checkgroup']" => array("checked" => FALSE)),
        ),


        
    );



    $form['checkgroup'] = array(
        '#title' => 'Delegate Group',
        '#type' => 'checkbox',
        '#options' => drupal_map_assoc(array(t('Yes'), t('No'))),
        '#ajax' => array(
            'callback' => 'group_callback_form',
            'wrapper' => 'group-check',
            'method' => 'replace',
            'effect' => 'fade',

        ),

        '#validated' => True,

               
        '#states' => array(
            "visible" => array(
                "input[name='checkuser']" => array("checked" => FALSE)),
        ),

       
    );


    $form['app']['groupcontainer'] = array(
        '#type' => 'container',
        '#states' => array(
            "visible" => array(
                "input[name='checkgroup']" => array("checked" => TRUE)),
        ),
    );

    
    $form['app']['groupcontainer']['group'] = array(
        '#type' => 'textfield',
        '#title' => t('Group Name'), 
        '#weight' => 2,
        '#required' => (isset($form_state["values"]["group"]) ? ($form_state["values"]["group"] == true  ? TRUE : FALSE) : TRUE),
        '#prefix' => '<div id="group-check">',
        '#suffix' => '</div>',
    );
     
    $form['app']['groupcontainer']['ou'] = array(
        '#type' => 'textfield',
        '#title' => t('OU Name'), 
        '#weight' => 2,
        '#required' => (isset($form_state["values"]["ou"]) ? ($form_state["values"]["ou"] == true  ? TRUE : FALSE) : TRUE),
        '#prefix' => '<div id="ou-check">',
        '#suffix' => '</div>',
    );
     
    



 


    //$form['app']['user']['#attributes']['placeholder'] = t('Anonymous');

    $form['submit_button'] = array(
        '#type' => 'submit',
        '#value' => t('Add delegation'),
    );


    return $form;
}



function group_callback_form($form, &$form_state) {
    return $form['app']['groupcontainer']['group'];
}



//Callback function public/Private

function user_callback_form($form, &$form_state) {
    return $form['app']['mytextcontainer']['user'];
}


function profilesnap4city_add_delegation_form_submit($form, &$form_state) {



    global $base_url;
    
    $app_id=$_SESSION['current_app_id'];

    
    $user=$form_state['values']['user'];

    $group=$form_state['values']['group'];

    $ou=$form_state['values']['ou'];


    

    //FIXED to be gathered by user info on ldap
    //$DN="dc=foo, dc=example, dc=org";

    $DN=simple_ldap_user_variable_get('simple_ldap_user_basedn');



    //Group Delegation
    if ((strcmp($user,'')==0) )
        {


          
            //check OU
            $check=retrieve_group_name($ou);

            if (count($check) > 0)
                {
                    $motivation= $form['app']['motivation']['#options'][$form_state['values']['motivation']];
                    $variable_name= $form['app']['variable_name']['#options'][$form_state['values']['variable_name']];

                    if (strcmp($group,'')==0)

                {

        
                    update_delegation_table_group('ou='.$ou.','.$DN,$app_id, $motivation,$variable_name);
                }

            else{
                $check=retrieve_group_name($group);

                if (count($check) > 0)
                    {
                        /* openlog("MODULE", 0, LOG_LOCAL0); */
                        /* syslog(LOG_WARNING, 'Found'); */
                             
                        update_delegation_table_group('cn='.$group.',ou='.$ou.','.$DN,$app_id, $motivation,$variable_name);
                    }


            }
                    
                }
            
            

            
            /* else */
            /*                             { */
            /*                  openlog("MODULE", 0, LOG_LOCAL0); */
            /*                  syslog(LOG_WARNING, 'Not Found'); */
            /*             } */
            $form_state['redirect'] = $base_url.'/appdetails?='.$app_id;

            
        }
    
    else {
    

        $check=retrieve_user_name_by_mail($user);

          

    

    
        //If ok Procedee
        if (count(retrieve_user_name_by_mail($user)) == 1){

            $motivation= $form['app']['motivation']['#options'][$form_state['values']['motivation']];
            $variable_name= $form['app']['variable_name']['#options'][$form_state['values']['variable_name']];
        
            if (strcmp($user,'')==0){
                $user='ANONYMOUS';
            }
    
            else{
                $user=retrieve_user_name_by_mail($user);
            }

            update_delegation_table($user,$app_id,$motivation,$variable_name);
            drupal_set_message('Delegation Added');    
  
        }
        else{
            drupal_set_message('Sorry user not registered');
        }

        //Trick to return to view rendered
        $form_state['redirect'] = $base_url.'/appdetails?='.$app_id;

    }
}


?>
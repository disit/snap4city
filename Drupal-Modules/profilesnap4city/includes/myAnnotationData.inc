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
   
/*Block Form: List of annotation owned by the user*/

function retrieve_myannotationdata_variable_name()
{

    global $user;


    $rows=array();
    
    db_set_active('profiledb');
    $query = db_select('data', 'da');

    $query->condition('da.username',$user->name,'=');
    $query->distinct();
  
    $query->isNull('da.delete_time');
    $query->isNull('da.app_id');

    
    $query->fields('da', array('variable_name'));

    $result = $query->execute();
    


    $i=0;
    
    while ($row = $result->fetchAssoc()) {
                $rows[$i]=
                    //array(
                    $row['variable_name'];
                        //;

                $i++;
                    //);


    }

       db_set_active('default');
    return $rows;
  
      
    
}



/*Backedn function for the form*/

function retrieve_myannotationdata_datatables()
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
        'data' => 'Service/GraphID',
        'datatable_options' => array(
            'bSortable' => TRUE,
            'bSearchable' => TRUE,
        ),
    ),
    array(
        'data' => 'Text',
        'datatable_options' => array(
            'bSortable' => TRUE,
            'bSearchable' => TRUE,
        ),
    ),
);

    
    
    db_set_active('profiledb');
    $query = db_select('data', 'da');
    $query->condition('da.username',$user->name,'=');
    
    $query->isNull('da.delete_time');
    $query->isNull('da.app_id');
   
    $query->fields('da', array('data_time','variable_name', 'variable_value' ));

    $result = $query->execute();
    
  
     //http://www.disit.org/km4city/resource/CarParkCareggi#b

    foreach($result as $stat_line) {

        $table_rows[] = array(
            array('data' => $stat_line->data_time),
         
            array('data' => '<a href=https://www.km4city.org/webapp-new/?serviceUri='.$stat_line->variable_name.'>'.$stat_line->variable_name.'</a>'),
            array('data' => $stat_line->variable_value),
         

             
             
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

/*Backned function for delegated annotation*/


function retrieve_myannotationdata_delegate()
{

    global $user;


    $output = '';

    $table_rows = array();


    $header_row = array(
       
        array('data' => 'User delegated', 'field' => 'de.username_delegated'),
        array('data' => 'Service/GraphID', 'field' => 'de.variable_name'),
       
        array('data' => 'id', 'field' => 'de.id'),

    );
    

    //select username_delegated, variable_name, motivation,id from delegation where username_delegator = user and element_id = NULL
   
    db_set_active('profiledb');
    $query = db_select('delegation', 'de');
    $query->condition('de.username_delegator',$user->name,'=');
    $query->isNull('de.delete_time');
    $query->isNull('de.groupname_delegated');
    $query->isNull('de.element_id');
    $query->isNull('de.element_type');

    
    $query->fields('de', array('username_delegated','variable_name','id'));
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

        $table_rows[] = array(
            array('data' => $stat_line->username_delegated),
            array('data' => $stat_line->variable_name),
            array('data' => $stat_line->id),
                 
        );
     
    }


     
    return $table_rows; 
     
     
    
}


/**/
function retrieve_myannotationdata_group_delegate()
{

    global $user;


    $output = '';

    $table_rows = array();


    $header_row = array(
       
        array('data' => 'Group delegated', 'field' => 'de.groupname_delegated'),
        array('data' => 'Service/GraphID', 'field' => 'de.variable_name'),
               array('data' => 'id', 'field' => 'de.id'),

    );
    

    //select groupname_delegated, variable_name, id from delegation where username_delegator = user and element_id = NULL
   
    db_set_active('profiledb');
    $query = db_select('delegation', 'de');
    $query->condition('de.username_delegator',$user->name,'=');
    $query->isNotNull('de.groupname_delegated');
    $query->isNull('de.delete_time');
    $query->isNull('de.element_id');
    $query->isNull('de.element_type');

    
    $query->fields('de', array('groupname_delegated','variable_name','id'));
    $query = $query
        ->extend('TableSort')
        ->orderByHeader($header_row);
    $result = $query->execute();
    

    
    db_set_active('default');


    if (count($result)>0){
    

   foreach($result as $stat_line) {


         /* openlog("MODULE", 0, LOG_LOCAL0); */
        /* syslog(LOG_WARNING, 'Found '.$stat_line->element_id); */
                             

        
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
            array('data' => $stat_line->id),
      
       

                 
        );
     
    }

    }
     
    return $table_rows; 
     
     
    
}


/**/



/*Main Function for treating delegation */

function profilesnap4city_myannotation_delegation_add_more($form, &$form_state) {





    /*
      Read data from Delegation Table
    */

    $delegation_rows=retrieve_myannotationdata_delegate();


    /*

      Build container

    */


     $form['some_text'] = array(
          '#markup' => '<h2>Actual delegation</h2>'
    );



    if(count($delegation_rows)>0){

  
       
        $form['field_container'] = [
            '#type' => 'container',
            '#weight' => 80,
            '#tree' => TRUE,
            // Set up the wrapper so that AJAX will be able to replace the fieldset.
            '#prefix' => '<div id="js-ajax-elements-wrapper">',
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
                '#title' => t('Service/GraphID' ),
                '#size' => 70,
            ];

        
   
        
            $form['field_container'][$delta]['field3'] = [
            
                '#type' => 'hidden',
                '#value'=>$delegation_rows[$delta][2]['data'],
                //'#disabled'=>'TRUE',
            
            ];
        
       
    
        
 
            $form['field_container'][$delta]['remove_name'] = [
                '#type' => 'submit',
                '#value' => t('Remove'),
                '#submit' => ['profilesnap4city_myannotation_delegation_add_more_remove'],
                // See the examples in ajax_example.module for more details on the
                // properties of #ajax.
                '#ajax' => [
                
                     'callback' => 'profilesnap4city_myannotation_delegation_add_more_remove_callback',
                    'wrapper' => 'js-ajax-elements-wrapper',
                    
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
function profilesnap4city_myannotation_delegation_add_more_remove($form, &$form_state) {

    $delta_remove = $form_state['triggering_element']['#parents'][1];

    $k = array_search($delta_remove, $form_state['field_deltas']);

    $_SESSION['Remove_Share_id']=$form_state['values']['field_container'][$k]['field3'];
    
  
    unset($form_state['field_deltas'][$k]);

    
    $form_state['rebuild'] = TRUE;

    drupal_get_messages();

    
}

/* Remove 2*/
function profilesnap4city_myannotation_delegation_add_more_remove_callback($form, &$form_state) {

    drupal_set_message('Delegation removed');

    $id= $_SESSION['Remove_Share_id'];
    
    db_set_active('profiledb');


    $num_deleted = db_delete('delegation')
        ->condition('id', $id)
        ->execute();
    
    db_set_active('default');


    
    return $form['field_container'];
}



/**/



/*Main Function for treating delegation */

function profilesnap4city_myannotation_delegation_group_list($form, &$form_state) {





    /*
      Read data from Delegation Table
    */
//QUA
    $delegation_rows=retrieve_myannotationdata_group_delegate();


    /*

      Build container

    */


     $form['some_text'] = array(
         '#markup' => '<h2>Actual group delegation</h2>'
    );



    if(count($delegation_rows)>0){

  
       
        $form['field_container'] = [
            '#type' => 'container',
            '#weight' => 80,
            '#tree' => TRUE,
            // Set up the wrapper so that AJAX will be able to replace the fieldset.
            '#prefix' => '<div id="js-ajax-elements-wrapper">',
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
                '#size' => 20,
            ];

	    $form['field_container'][$delta]['field2'] = [
            
                '#type' => 'textfield',
                '#value'=>$delegation_rows[$delta][1]['data'],
                '#attributes' => array('readonly' => 'readonly'),
                '#title' => t('OU' ),
                '#size' => 20,
            ];
	    

            $form['field_container'][$delta]['field3'] = [
            
                '#type' => 'textfield',
                '#value'=>$delegation_rows[$delta][2]['data'],
                '#attributes' => array('readonly' => 'readonly'),
                '#title' => t('Service/GraphID' ),
                '#size' => 70,
            ];

        
   
        
            $form['field_container'][$delta]['field4'] = [
            
                '#type' => 'hidden',
                '#value'=>$delegation_rows[$delta][3]['data'],
                //'#disabled'=>'TRUE',
            
            ];
        
       
    
        
 
            $form['field_container'][$delta]['remove_name'] = [
                '#type' => 'submit',
                '#value' => t('Remove'),
                '#submit' => ['profilesnap4city_myannotation_delegation_group_list_remove'],
                // See the examples in ajax_example.module for more details on the
                // properties of #ajax.
                '#ajax' => [
                
                     'callback' => 'profilesnap4city_myannotation_delegation_group_list_remove_callback',
                    'wrapper' => 'js-ajax-elements-wrapper',
                    
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
function profilesnap4city_myannotation_delegation_group_list_remove($form, &$form_state) {

    $delta_remove = $form_state['triggering_element']['#parents'][1];

    $k = array_search($delta_remove, $form_state['field_deltas']);

    $_SESSION['Remove_Share_id']=$form_state['values']['field_container'][$k]['field4'];
    
  
    unset($form_state['field_deltas'][$k]);

    
    $form_state['rebuild'] = TRUE;

    drupal_get_messages();

    
}

/* Remove 2*/
function profilesnap4city_myannotation_delegation_group_list_remove_callback($form, &$form_state) {

    drupal_set_message('Delegation removed');

    $id= $_SESSION['Remove_Share_id'];
    
    db_set_active('profiledb');


    $num_deleted = db_delete('delegation')
        ->condition('id', $id)
        ->execute();
    
    db_set_active('default');


    
    return $form['field_container'];
}




/**/




function profilesnap4city_myannotation_add_delegation_form($form, &$form_state) {


    
    
    $my_variable_name=retrieve_myannotationdata_variable_name();
    
    

    $form['some_text'] = array(
          '#markup' => '<h2>New delegation</h2>'
    );


    // App checkboxes field container.
    $form['app'] = array(
        '#type' => 'container',

        //'#tree' => TRUE,
    
    
    );


 
    $form['app']['variable_name'] = array(
        '#type' => 'select',
        '#title'=> t('Service/GraphID'),
       
        '#options' => $my_variable_name,
        
    );



        $form['checkuser'] = array(
        '#title' => 'Make Public',
        '#type' => 'checkbox',
        '#options' => drupal_map_assoc(array(t('Yes'), t('No'))),
	        '#ajax' => array(
				          'callback' => 'myannotation_user_callback_form',
				           'wrapper' => 'user-check',
				           'method' => 'replace',
				              'effect' => 'fade',

            ),

        '#validated' => True,
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
            'callback' => 'myannotation_group_callback_form',
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

//Callback function public/Private

function myannotation_user_callback_form($form, &$form_state) {
    return $form['app']['mytextcontainer']['user'];
    }

function myannotation_group_callback_form($form, &$form_state) {
    return $form['app']['groupcontainer']['group'];
    }





function profilesnap4city_myannotation_add_delegation_form_submit($form, &$form_state) {



    global $base_url;
    
  
    
    $useremail=$form_state['values']['user'];


    $variable_name= $form['app']['variable_name']['#options'][$form_state['values']['variable_name']];
    $group=$form_state['values']['group'];

    $ou=$form_state['values']['ou'];


    

    //FIXED to be gathered by user info on ldap
//    $DN="dc=foo, dc=example, dc=org";

$DN=simple_ldap_user_variable_get('simple_ldap_user_basedn');



//Group Delegation
    if ((strcmp($useremail,'')==0) )
        {


                        


          
            //check OU
            $check=retrieve_group_name($ou);

            if (count($check) > 0)
                {
                    
                    

                    if (strcmp($group,'')==0)

                {



                    update_myiotdata_annotation_group_delegation_table('ou='.$ou.','.$DN,$variable_name);
                }

            else{
                $check=retrieve_group_name($group);

                if (count($check) > 0)
                    {
                        /* openlog("MODULE", 0, LOG_LOCAL0); */
                        /* syslog(LOG_WARNING, 'Found'); */

                             
                        update_myannotation_group_delegation_table('cn='.$group.',ou='.$ou.','.$DN,$variable_name);
                    }


            }
                    
                }
            

            
        }
    

    

    else{

    
    //If ok Procedee
     if (strcmp($useremail,'ANONYMOUS')==0){

        
        
        update_myannotation_delegation_table($useremail,$variable_name);
        drupal_set_message('Delegation Added');    
    }

    
     else if (count(retrieve_user_username_by_mail($useremail)) == 1)  {

        
        
        $username=retrieve_user_username_by_mail($useremail);
        update_myannotation_delegation_table($username,$variable_name);
        drupal_set_message('Delegation Added');    
  
    }


    else{
        drupal_set_message('Sorry user not registered');
    }
    }

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

function update_myannotation_group_delegation_table($groupname,$variable_name){


    global $user;
    




  
  

   db_set_active('profiledb');
   db_insert('delegation')
        ->fields(array(
            'username_delegator' => $user->name,
            'groupname_delegated' => $groupname,
            'motivation' => 'Annotation',
            'variable_name' => $variable_name,
            'element_id'=> NULL,
            'element_type' => NULL
            
        ))
        ->execute();


    db_set_active('default');
    
}




?>
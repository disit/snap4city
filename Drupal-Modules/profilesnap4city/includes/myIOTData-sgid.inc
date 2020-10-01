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
   
/*********************
My IOT Service URI Data Function
**********************/

function myiotdata_sgid_delegation_form($form, &$form_state) {




    /*
      Read data from Delegation Table
    */

    $delegation_rows=retrieve_myiotdata_sgid_delegate();


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
                '#title' => t('Service Uri' ),
                '#size' => 70,
            ];

        
           
            $form['field_container'][$delta]['field3'] = [
            
                '#type' => 'hidden',
                '#value'=>$delegation_rows[$delta][2]['data'],
                //'#disabled'=>'TRUE',
            
            ];
        


          http://www.disit.org/km4city/resource/CKAN/Disit_Unifi_Firenze_telecamere_datagate_telecamere
            

            $form['field_container'][$delta]['onservicemap'] =array(
                '#markup' =>'<a href=https://www.km4city.org/webapp-new/?geoJsonUri=http://www.disit.org/ServiceMap/api/v1/?selection=44.050872;10.731545;43.271961;12.004284%26maxResults=0%26lang=it%26format=json%26graphUri='.$delegation_rows[$delta][1]['data'].'> SEE on Map</a>',
            );
            
        
 
            $form['field_container'][$delta]['remove_name'] = [
                '#type' => 'submit',
                '#value' => t('Remove'),
                '#submit' => ['myiotdata_sgid_delegation_list_remove'],
                // See the examples in ajax_example.module for more details on the
                // properties of #ajax.
                '#ajax' => [
                
                     'callback' => 'myiotdata_sgid_delegation_list_remove_callback',
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



function myiotdata_sgid_delegation_list_remove($form, &$form_state) {

    $delta_remove = $form_state['triggering_element']['#parents'][1];

    $k = array_search($delta_remove, $form_state['field_deltas']);

    $_SESSION['Remove_Share_id']=$form_state['values']['field_container'][$k]['field3'];
    
  
    unset($form_state['field_deltas'][$k]);

    
    $form_state['rebuild'] = TRUE;

    drupal_get_messages();


    
}

function myiotdata_sgid_delegation_list_remove_callback($form, &$form_state) {

    drupal_set_message('Delegation removed');

    $id= $_SESSION['Remove_Share_id'];
    
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



    // $num_deleted = db_delete('delegation')
    //     ->condition('id', $id)
    //     ->execute();
    
    db_set_active('default');


    
    return $form['field_container'];
}




function myiotdata_sgid_delegation_add_form($form, &$form_state) {



  

   $my_element_id=retrieve_myiotdata_sgid_element_id();


    $form['some_text'] = array(
          '#markup' => '<h2>New delegation</h2>'
    );


    // App checkboxes field container.
    $form['app'] = array(
        '#type' => 'container',

        //'#tree' => TRUE,
    
    
    );

     
    $form['app']['element_id'] = array(
        '#type' => 'select',
        '#title'=> t('Service URI'),
        
        '#options' => $my_element_id,
        

    );
    



    $form['checkuser'] = array(
        '#title' => 'Make Public',
        '#type' => 'checkbox',
        '#options' => drupal_map_assoc(array(t('Yes'), t('No'))),
	        '#ajax' => array(
				          'callback' => 'myiotdata_sgid_user_callback_form',
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
     
    
    




   $form['submit_button'] = array(
        '#type' => 'submit',
        '#value' => t('Add delegation'),
    );


    return $form;
}





//Callback function public/Private

function myiotdata_sgid_user_callback_form($form, &$form_state) {
    return $form['app']['mytextcontainer']['user'];
    }


function myiotdata_sgid_delegation_add_form_submit($form, &$form_state) {



    global $base_url;
    


    
    $user=$form_state['values']['user'];

    
    //$check=retrieve_user_name_by_mail($user);

    
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
                    $element_id= $form['app']['element_id']['#options'][$form_state['values']['element_id']];
                    

                    if (strcmp($group,'')==0)

                {

        
                    update_myiotdata_su_group_delegation_table('ou='.$ou.','.$DN,$element_id);
                }

            else{
                $check=retrieve_group_name($group);

                if (count($check) > 0)
                    {
                        /* openlog("MODULE", 0, LOG_LOCAL0); */
                        /* syslog(LOG_WARNING, 'Found'); */
                             
                        update_myiotdata_su_group_delegation_table('cn='.$group.',ou='.$ou.','.$DN,$element_id);
                    }


            }
                    
                }
            

            
        }
    

    

    else{
    //If ok Procedee
    if (count(retrieve_user_name_by_mail($user)) == 1){

        
        $element_id= $form['app']['element_id']['#options'][$form_state['values']['element_id']];
        if (strcmp($user,'')==0){
            $user='ANONYMOUS';
        }
        else{
            $user=retrieve_user_name_by_mail($user);
        }

        
        update_myiotdata_sgid_delegation_table($user,$element_id);
        
        drupal_set_message('Delegation Added');    
  
    }
    else{
        drupal_set_message('Sorry user not registered');
    }

}


}

/*Utils*/

function retrieve_myiotdata_sgid_delegate()
{

    global $user;



    $output = '';

    $table_rows = array();



    $header_row = array(
       
        array('data' => 'User delegated', 'field' => 'de.username_delegated'),
        array('data' => 'Service URI', 'field' => 'de.element_id'),
        array('data' => 'id', 'field' => 'de.id'),

    );
    
    
   
    db_set_active('profiledb');
    $query = db_select('delegation', 'de');
    $query->condition('de.username_delegator',$user->name,'=');
    $query->condition('de.element_type','ServiceGraphID','=');
    $query->isNull('de.delete_time');
      $query->fields('de', array('username_delegated','element_id','id'));
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

       // $delegated[]= $stat_line->username_delegated;


       if (strcmp($stat_line->username_delegated,'ANONYMOUS')==0){
           $stat_line->username_delegated='PUBLIC/ANONYMOUS';
       }
       
       
        $table_rows[] = array(
            array('data' => $stat_line->username_delegated),
            array('data' => $stat_line->element_id),
                 array('data' => $stat_line->id),
                 
        );
     
    }


     
    return $table_rows; 
     
     
    
}


function retrieve_myiotdata_sgid_element_id()
{

    global $user;



    $rows=array();
    
    db_set_active('profiledb');

    $query = db_select('ownership', 'de');

    $query->condition('de.username',$user->name,'=');
    
    $query->condition('de.elementType','ServiceGraphID','=');
        $query->distinct();
 

    

    

    $query->fields('de', array('elementId'));

    $result = $query->execute();
    


    $i=0;
    while ($row = $result->fetchAssoc()) {
                $rows[$i]=
                    // array(
                    $row['elementId'];
                $i++;
                    
                    //,

                    //);


    }

       db_set_active('default');
    return $rows;
    
    
}

function update_myiotdata_sgid_delegation_table($username,$element_id){


    global $user;
    db_set_active('profiledb');


   //insert into delegation values ('',user->name, username, motivation,variaible_name)
    
    db_insert('delegation')
        ->fields(array(
            'username_delegator' => $user->name,
            'username_delegated' => $username,
         
           
            'element_id'=> $element_id,
            'element_type' => 'ServiceGraphID'
            
        ))
        ->execute();


    db_set_active('default');
    
}



//Here

/* QUI Liste actual delegation For Group  */

function myiotdata_sgid_group_delegation_form($form, &$form_state) {




    /*
      Read data from Delegation Table
    */

    $delegation_rows=retrieve_myiotdata_sgid_group_delegate();


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
                //'#disabled'=>'TRUE',
                '#attributes' => array('readonly' => 'readonly'),
                '#title' => t('Group name' ),
                '#size' => 20,
            ];

            $form['field_container'][$delta]['field2'] = [
            
                '#type' => 'textfield',
                '#value'=>$delegation_rows[$delta][1]['data'],
                //'#disabled'=>'TRUE',
                '#attributes' => array('readonly' => 'readonly'),
                '#title' => t('OU'),
                '#size' => 20,
            ];


           $form['field_container'][$delta]['field3'] = [
            
                '#type' => 'textfield',
                '#value'=>$delegation_rows[$delta][2]['data'],
                //'#disabled'=>'TRUE',
                '#attributes' => array('readonly' => 'readonly'),
                '#title' => t('Service Uri: '),
                '#size' => 70,
            ];

            


            $form['field_container'][$delta]['onservicemap'] =array(
                '#markup' =>'<a href=https://www.km4city.org/webapp-new/?serviceUri='.$delegation_rows[$delta][2]['data'].'> Show on Map</a>',
            );


        
           
            $form['field_container'][$delta]['field4'] = [
            
                '#type' => 'hidden',
                '#value'=>$delegation_rows[$delta][3]['data'],
                //'#disabled'=>'TRUE',
            
            ];
        
       
    
        
 
            $form['field_container'][$delta]['remove_name'] = [
                '#type' => 'submit',
                '#value' => t('Remove'),
                '#submit' => ['myiotdata_sgid_group_delegation_list_remove'],
                // See the examples in ajax_example.module for more details on the
                // properties of #ajax.
                '#ajax' => [
                
                    'callback' => 'myiotdata_sgid_group_delegation_list_remove_callback',
                    'wrapper' => 'js-ajax-group-elements-wrapper',
                    
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



function myiotdata_sgid_group_delegation_list_remove($form, &$form_state) {

    $delta_remove = $form_state['triggering_element']['#parents'][1];

    $k = array_search($delta_remove, $form_state['field_deltas']);

    $_SESSION['Remove_Share_id']=$form_state['values']['field_container'][$k]['field4'];
    
  
    unset($form_state['field_deltas'][$k]);

    
    $form_state['rebuild'] = TRUE;

    drupal_get_messages();


    
}

function myiotdata_sgid_group_delegation_list_remove_callback($form, &$form_state) {

    drupal_set_message('Delegation removed');

    $id= $_SESSION['Remove_Share_id'];
    
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


    
    return $form['field_container'];

}



function update_myiotdata_sgid_group_delegation_table($groupname,$element_id){


    global $user;
    db_set_active('profiledb');


   //insert into delegation values ('',user->name, username, motivation,variaible_name)
    
    db_insert('delegation')
        ->fields(array(
            'username_delegator' => $user->name,
            'groupname_delegated' => $groupname,
         
           
            'element_id'=> $element_id,
            'element_type' => 'ServiceURI'
            
        ))
        ->execute();


    db_set_active('default');
    
}



/*For Group  */

function retrieve_myiotdata_sgid_group_delegate()
{

    global $user;


    $output = '';

    $table_rows = array();



        $header_row = array(
       

        array('data' => 'Group delegated', 'field' => 'de.groupname_delegated'),
        array('data' => 'Service URI', 'field' => 'de.element_id'),
        array('data' => 'id', 'field' => 'de.id'),

    );
    

        /*select groupname_delegated, element_id, id  from delegation where username_delegator = and element_type='ServiceUri' and delete_time = NULL*/
   
    db_set_active('profiledb');
    $query = db_select('delegation', 'de');
    $query->condition('de.username_delegator',$user->name,'=');
    $query->condition('de.element_type','ServiceURI','=');
    $query->isNull('de.username_delegated');
    $query->isNull('de.delete_time');
    $query->fields('de', array('groupname_delegated','element_id','id'));
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
            array('data' => $stat_line->element_id),
            array('data' => $stat_line->id),
                 
        );
     
    }

    }


     
    return $table_rows; 
     
     
    
}



//END Here




?>
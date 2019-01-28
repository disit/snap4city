<?php
/*********************


My IOT Data Function


**********************/


function myiotdata_delegation_add_more($form, &$form_state) {


    $url=$_SERVER['REQUEST_URI'];

    $app_id = substr($url, strrpos($url, '=') + 1);

    $_SESSION['current_app_id']=$app_id;




    /*
      Read data from Delegation Table
    */

    $delegation_rows=retrieve_myiotdata_delegate_myapp($app_id);


    /*

      Build container

    */


     $form['some_text'] = array(
          '#markup' => '<h2>Actual shares</h2>'
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
                '#title' => t('User Email' ),
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


function myiotdata_add_delegation_form($form, &$form_state) {




    $app_id=$_SESSION['current_app_id'];
    
    $my_variable_name=retrieve_myiotdata_variable_name($app_id);
    $my_motivation=retrieve_myiotdata_motivation($app_id);
    $my_app=retrieve_myiotdata_app($app_id);


    $form['some_text'] = array(
          '#markup' => '<h2>New Share</h2>'
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
    
    $form['app']['user'] = array(
        '#type' => 'textfield',
        '#title'=> t('User Email'),
        '#attributes' => array(
            'placeholder' => t('Anonymous'),
            'title' => t("If leaved, anynone can access this share"),
        ),
        '#default_value' => t('ANONYMOUS'),

    );



   $form['submit_button'] = array(
        '#type' => 'submit',
        '#value' => t('Add share'),
    );


    return $form;
}




function myiotdata_add_delegation_form_submit($form, &$form_state) {



    global $base_url;
    
    $app_id=$_SESSION['current_app_id'];
    
    $user=$form_state['values']['user'];
    
    $check=retrieve_user_id_by_mail($user);

    
    //print_r($form_state['values']['motivation']);

    
    //If ok Procedee
    if (count(retrieve_user_id_by_mail($user)) == 1  || strcmp($user,'Anonymous')==0){

        $motivation= $form['app']['motivation']['#options'][$form_state['values']['motivation']];
        $variable_name= $form['app']['variable_name']['#options'][$form_state['values']['variable_name']];
        update_delegation_table($user,$app_id,$motivation,$variable_name);
        drupal_set_message('Share Added');    
  
    }
    else{
        drupal_set_message('Sorry user not registered');
    }

    //Trick to return to view rendered
    $form_state['redirect'] = $base_url.'/appdetails?='.$app_id;
}


/*For Group  */

function retrieve_myiotdata_group_delegate_myapp($app_id)
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

    $delegation_rows=retrieve_myiotdata_group_delegate_myapp($app_id);


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



function retrieve_myiotdata_group_delegate()
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



?>
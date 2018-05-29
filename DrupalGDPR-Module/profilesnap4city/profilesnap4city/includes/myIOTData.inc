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

    $delegation_rows=retrieve_mypersonaldata_delegate_myapp($app_id);


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
            

            /* $form['field_container'][$delta]['add_name'] = [ */
            /*     '#type' => 'submit', */
            /*     '#value' => t('+'), */
            /*     '#submit' => ['profilesnap4city_delegation_add_more_add_one'], */
            /*     // See the examples in ajax_example.module for more details on the */
            /*     // properties of #ajax. */
            /*     '#ajax' => [ */
            /*         'callback' => 'profilesnap4city_delegation_add_more_one_callback', */
            /*         'wrapper' => 'js-ajax-elements-wrapper', */
            /*     ], */
            /*     '#weight' => -50, */
            /*     '#attributes' => [ */
            /*         'class' => ['button-small'], */
            /*     ], */
            /*     '#name' => 'add_name_' . $delta, */
            /* ]; */
            


        }

    }

    return $form;
}


function myiotdata_add_delegation_form($form, &$form_state) {




    $app_id=$_SESSION['current_app_id'];
    
    $my_variable_name=retrieve_mypersonaldata_variable_name($app_id);
    $my_motivation=retrieve_mypersonaldata_motivation($app_id);
    $my_app=retrieve_mypersonaldata_app($app_id);


    $form['some_text'] = array(
          '#markup' => '<h2>New Share</h2>'
    );


    // App checkboxes field container.
    $form['app'] = array(
        '#type' => 'container',

        //'#tree' => TRUE,
    
    
    );


     /* $form['app']['myapp'] = array( */
    /*     '#type' => 'textfield', */
    /*     '#title'=> t('My app'), */
    
    
    /*     '#options' => $my_app, */
        

    /* ); */
     
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

    //$form['app']['user']['#attributes']['placeholder'] = t('Anonymous');

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

?>
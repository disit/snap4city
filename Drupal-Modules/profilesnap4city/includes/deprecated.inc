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

function retrieve_mypersonaldata($app_id)
{

    global $user;


    $output = '';

    $table_rows = array();


    $header_row = array(
        array('data' => 'data_time'),
        array('data' => 'app_name'),
        array('data' => 'variable_name'),
        array('data' => 'variable_value'),
        array('data' => 'variable_unit'),
        array('data' => 'motivation'),

    );
    

    
    
    db_set_active('profiledb');
    $query = db_select('data', 'da');

    $query->condition('da.username',$user->name,'=');
    $query->condition('da.app_id',$app_id,'=');
    $query->isNull('da.delete_time');
    //$query->condition('da_delete_time','','=');
    
    $query->fields('da', array('data_time','app_name','variable_name', 'variable_value','variable_unit', 'motivation' ));

    //$query = $query
    //    ->extend('TableSort')
    //    ->orderByHeader($header_row);
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

    db_set_active('default');
    //return a themed schedule table
    return theme_table( array('header' => $header_row,
    'rows' => $table_rows,
    'attributes' => array('class' => 'table_dfs', 'width' => '100%'),
    'caption' => '',
    'colgroups' => array(),
    'sticky' => FALSE,
    'empty' => 'No data is available',
    ));  
     
     
     
     
     
    
}









/**
 * A form with a dropdown whose options are dependent on a
 * choice made in a previous dropdown.
 *
 * On changing the first dropdown, the options in the second
 * are updated.
 */
function app_dependent_dropdown($form, &$form_state) {
    // get the list of manufacturers to populate the manuacturer dropdown
    //$options_first = _automobile_get_manufacturer_dropdown_options();
    $options_first = retrieve_mypersonaldata_app();


    if(count($options_first)>0){

    // if we have a value for the app  dropdown from
    // $form_state['values'] we use this both as the default value for
    // the first dropdown and also as a parameter to pass to the
    // function that retrieves the options for the second dropdown.
    $selected = isset($form_state['values']['app_dropdown']) ?  $form_state['values']['app_dropdown'] : key($options_first);


        $form['some_text'] = array(
          '#markup' => '<h2>New Share</h2>'
    );



    $form['app_dropdown'] = array(
        '#type' => 'select',
        '#title' => 'App',
        '#options' => $options_first,
        '#default_value' => $selected,
        // bind an ajax callback to the change event (which is the default for the
        // select form type) of the manufacturer dropdown. It will replace the
        // model dropdown when rebuilt
        '#ajax' => array(
            'callback' => 'app_dependent_dropdown_callback',
            'wrapper' => 'dropdown_variable_replace',
        ),
    );

    
    $form['variable_name_dropdown'] = array(
        '#type' => 'select',
        '#title' => 'Variable Name',
        // The entire enclosing div created here gets replaced when variable_dropdown
        // is changed.
        '#prefix' => '<div id="dropdown_variable_replace">',
        '#suffix' => '</div>',
        // when the form is rebuilt during ajax processing, the $selected variable
        // will now have the new value and so the models will change
        '#options' => _app_get_variable_dropdown_options($options_first[$selected]),
        '#default_value' => isset($form_state['values']['variable_name_dropdown']) ?   $form_state['values']['variable_name_dropdown'] : '',
    );


    $form['motivation_dropdown'] = array(
        '#type' => 'select',
        '#title' => 'Motivation',
        // The entire enclosing div created here gets replaced when manufacturer_dropdown
        // is changed.
        '#prefix' => '<div id="dropdown_motivation_replace">',
        '#suffix' => '</div>',
        // when the form is rebuilt during ajax processing, the $selected variable
        // will now have the new value and so the models will change
        '#options' => _app_get_motivation_dropdown_options($options_first[$selected]),
        '#default_value' => isset($form_state['values']['motivation_dropdown']) ?

        $form_state['values']['motivation_dropdown'] : '',
    );


        $form['app']['user'] = array(
        '#type' => 'textfield',
        '#title'=> t('User Email'),
        
        
    );



    $form['submit'] = array(
        '#type' => 'submit',
        '#value' => t('Submit'),
    );
}    
    return $form;
}
/**
 * Selects just the model dropdown to be returned for re-rendering
 *
 * The version here has been re-loaded with a different set of options and
 * is sent back to the page to be updated.
 *
 * @return renderable array (the second dropdown)
 */
function app_dependent_dropdown_callback($form, $form_state) {

    
    return $form['variable_name_dropdown'];
}
/**
 * Helper function to populate the manufacturer dropdown. This would normally be
 * pulling data from the database.
 *
 * @return array of options
 */
/* function _app_get_app_dropdown_options() { */
/*     // drupal_map_assoc() just makes an array('Strings' => 'Strings'...). */
/*     return drupal_map_assoc(array(t('Honda'), t('Toyota'), t('Ford'), t('Volkswagen'))); */
/* } */



/**
 * Helper function to populate the variable dropdown. This would normally be
 * pulling data from the database.
 *
 * @param key. This will determine which set of options is returned.
 *
 * @return array of options
 */
function _app_get_variable_dropdown_options($key ) {

    global $user;


    //print_r($key);
    //$key='6ff3a0ea0a5d92f345fa13c95d0b35ff77204413b9c98e3a71b1d269a26af11e';

    $rows=array();
    
    db_set_active('profiledb');
    $query = db_select('data', 'da');

    $query->condition('da.username',$user->name,'=');

    $query->condition('da.app_name',$key[0],'=');
    
    $query->distinct();
  
    $query->isNull('da.delete_time');
    //$query->condition('da_delete_time','','=');
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



/**
 * Helper function to populate the model dropdown. This would normally be
 * pulling data from the database.
 *
 * @param key. This will determine which set of options is returned.
 *
 * @return array of options
 */
function _app_get_motivation_dropdown_options($key) {


    global $user;



    //print_r($key);
    //$key='6ff3a0ea0a5d92f345fa13c95d0b35ff77204413b9c98e3a71b1d269a26af11e';

    $rows=array();
    
    db_set_active('profiledb');
    $query = db_select('data', 'da');

    $query->condition('da.username',$user->name,'=');

    $query->condition('da.app_name',$key[0],'=');
    
    $query->distinct();
  
    $query->isNull('da.delete_time');
    //$query->condition('da_delete_time','','=');
    $query->fields('da', array('motivation'));

    $result = $query->execute();
    
    $i=0;
    
    while ($row = $result->fetchAssoc()) {
                $rows[$i]=
                    //array(
                    $row['motivation'];
                        //;

                $i++;
                    //);


    }

       db_set_active('default');
    return $rows;

    
}


function app_dependent_dropdown_submit($form, &$form_state) {


    $user=$form_state['values']['user'];
    $check=retrieve_user_id_by_mail($user);


    
    //If ok Procedee
    if (count(retrieve_user_id_by_mail($user)) == 1){

        $motivation= $form['motivation_dropdown']['#options'][$form_state['values']['motivation_dropdown']];
        $variable_name= $form['variable_name_dropdown']['#options'][$form_state['values']['variable_name_dropdown']];
        update_delegation_table($check[0],$motivation,$variable_name);
        drupal_set_message('Share Added');
  
    }
    else{
        drupal_set_message('Sorry user not registered');
    }
}
/* Not used Ajax Add */
function profilesnap4city_delegation_add_more_add_one($form, &$form_state) {
    $form_state['field_deltas'][] = count($form_state['field_deltas']) > 0 ? max($form_state['field_deltas']) + 1 : 0;

    $form_state['rebuild'] = TRUE;
    drupal_get_messages();
}

function profilesnap4city_delegation_add_more_add_one_callback($form, $form_state) {
    drupal_set_message('Share added');
    return $form['field_container'];
}
?>
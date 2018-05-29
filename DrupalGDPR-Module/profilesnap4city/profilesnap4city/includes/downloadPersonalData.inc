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

function download_mypersonaldata_form($form, &$form_state) {

    $url=$_SERVER['REQUEST_URI'];

    $app_id = substr($url, strrpos($url, '=') + 1);

    $_SESSION['current_app_id']=$app_id;

    
    $form['download'] = array(
        '#type' => 'hidden',
        '#value' => $app_id,
    );

   

    $form['submit_button'] = array(
        '#type' => 'submit',
	
	//'#attributes' => array('class' => array('mybtn', 'mybtn-2')),
        '#value' => t('Download this data'),
    );

    return $form;
}



function download_mypersonaldata_form_submit($form, &$form_state) {
  
    

    global $user;

    $app_id=$_SESSION['current_app_id'];

    
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

    

    $query->fields('da', array('data_time','app_name','variable_name', 'variable_value','variable_unit', 'motivation' ));

    /* $query = $query */
    /*     ->extend('TableSort') */
    /*     ->orderByHeader($header_row); */
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


    export_csv($table_rows);
    db_set_active('default');    
    

    

    
}


?>
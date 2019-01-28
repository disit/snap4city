<?php

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
        '#value' => t('Download my data'),
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
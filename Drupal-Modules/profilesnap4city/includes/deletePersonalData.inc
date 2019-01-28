<?php

function delete_mypersonaldata_form($form, &$form_state) {

    $url=$_SERVER['REQUEST_URI'];

    $app_id = substr($url, strrpos($url, '=') + 1);

    $_SESSION['current_app_id']=$app_id;


    $form['forgetme'] = array(
        '#type' => 'hidden',
        '#value' => $app_id,
    );

    

    $form['submit_button'] = array(
        '#type' => 'submit',
        '#value' => t('Remove this data!'),
    );

    return $form;
}


function delete_mypersonaldata_form_submit($form, &$form_state) {



     $app_id =   $_SESSION['current_app_id'];

     $date = date('Y-m-d H:i:s');

     $newdate = strtotime('+1 month' , strtotime ($date));

     $newdate = date ( 'Y-m-d H:i:s' , $newdate );

        db_set_active('profiledb');

        db_update('data')
	        ->fields(array(
		            'delete_time' => $date,

        ))
	        ->condition('app_id', $app_id, '=')
		        ->execute();

        db_set_active('default');


}


?>
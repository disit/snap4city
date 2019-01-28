<?php

function delete_all_mypersonaldata_form($form, &$form_state) {



    $form['submit_button'] = array(
        '#type' => 'submit',
        '#value' => t('Forget me all!'),
    );


    return $form;
}


function delete_all_mypersonaldata_form_submit($form, &$form_state) {

    global $user;

    db_set_active('profiledb');



	

    drupal_set_message('You were forgeted');
    db_update('data')
        ->fields(array(
            'delete_time' => date('Y-m-d H:i:s'),

        ))
        ->condition('username', $user->name, '=')

        ->execute();

    db_update('delegation')
        ->fields(array(
            'delete_time' => date('Y-m-d H:i:s'),

        ))
        ->condition('username_delegator', $user->name,'=')
        ->execute();


			


    db_update('ownership')
        ->fields(array(
            'deleted' => date('Y-m-d H:i:s'),

        ))
        ->condition('username', $user->name, '=')
        ->execute();




        db_update('activity')
        ->fields(array(
            'delete_time' => date('Y-m-d H:i:s'),

        ))
        ->condition('username', $user->name, '=')
        ->execute();


    




	$url = 'https://main.snap4city.org/api/deleteUserDashboards.php';

	$headers = array('Content-Type' => 'application/x-www-form-urlencoded');

    // 	$data = array (
    // '	key' => 'A160FDE523826CD438260CABFCD43892DCA75'),
    //     'id' => $node->nid,
    // 	 'action' => $action
    // 	   );


    //$response = drupal_http_request($url, $headers, 'POST', http_build_query($data, '', '&'));


    $response = drupal_http_request($url, $headers, 'GET');





    
    db_set_active('default');


}


?>
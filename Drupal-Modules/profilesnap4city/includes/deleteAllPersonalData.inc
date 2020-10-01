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
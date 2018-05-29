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
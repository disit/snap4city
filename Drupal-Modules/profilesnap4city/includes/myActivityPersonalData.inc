<?php



/*

  List of all owned app 
  Return a Rendered table
*/





function retrieve_myactivityPersonaldata_app_list_datatable()
{

    global $user;


    $output = '';

    $table_rows = array();

   
    // Table settings.
    $attributes['datatable_options'] = array(
        'bFilter'   => TRUE,
        'bInfo'     => TRUE,
    );



    // Define table columns



//If user is rootAdmin 21 on prod
	if (user_has_role(11))
        {


            $header = array(
                array(
                    'data' => t('Time'),
        	    
		     
                    'datatable_options' => array(
                        'bSortable' => TRUE,
                        'bSearchable' => TRUE,
                    ),
                ),


                array(
	
                    'data' => t('Application name'),
	    
                    'datatable_options' => array(
                        'bSortable' => TRUE,
                        'bSearchable' => TRUE,
                    ),
                ),

                array(
	
                    'data' => t('Source'),
	    
                    'datatable_options' => array(
                        'bSortable' => TRUE,
                        'bSearchable' => TRUE,
                    ),
                ),
	
                array(
	
                    'data' => t('Variable'),
	    
                    'datatable_options' => array(
                        'bSortable' => TRUE,
                        'bSearchable' => TRUE,
                    ),
                ),
                array(
	
                    'data' => t('Motivation'),
	    
                    'datatable_options' => array(
                        'bSortable' => TRUE,
                        'bSearchable' => TRUE,
                    ),
                ),

                array(
	
                    'data' => t('Access type'),
	    
                    'datatable_options' => array(
                        'bSortable' => TRUE,
                        'bSearchable' => TRUE,
                    ),
                ),
	
                array(
	
                    'data' => t('Domain'),
	    
                    'datatable_options' => array(
                        'bSortable' => TRUE,
                        'bSearchable' => TRUE,
                    ),
                ),

                array(
	
                    'data' => t('Username'),
	    
                    'datatable_options' => array(
                        'bSortable' => TRUE,
                        'bSearchable' => TRUE,
                    ),
                ),


            );




        }
    else
        {



            $header = array(
                array(
                    'data' => t('Time'),
        
	     'width' =>'200px',
                    'datatable_options' => array(
                        'bSortable' => TRUE,
                        'bSearchable' => TRUE,
                    ),
                ),







                array(
	
                    'data' => t('Application name'),
	    
                    'datatable_options' => array(
                        'bSortable' => TRUE,
                        'bSearchable' => TRUE,
                    ),
                ),

                array(
	
                    'data' => t('Source'),
	    
                    'datatable_options' => array(
                        'bSortable' => TRUE,
                        'bSearchable' => TRUE,
                    ),
                ),
	
                array(
	
                    'data' => t('Variable'),
	    
                    'datatable_options' => array(
                        'bSortable' => TRUE,
                        'bSearchable' => TRUE,
                    ),
                ),
                array(
	
                    'data' => t('Motivation'),
	    
                    'datatable_options' => array(
                        'bSortable' => TRUE,
                        'bSearchable' => TRUE,
                    ),
                ),

                array(
	
                    'data' => t('Access type'),
	    
                    'datatable_options' => array(
                        'bSortable' => TRUE,
                        'bSearchable' => TRUE,
                    ),
                ),
	
                array(
	
                    'data' => t('Domain'),
	    
                    'datatable_options' => array(
                        'bSortable' => TRUE,
                        'bSearchable' => TRUE,
                    ),
                ),
	
            );
            // Table data.
        }   



    
    //select activity.time as ,activity.app_id, data.app_name from activity, data  where data.app_id = activity.app_id
    
    db_set_active('profiledb');

    $query = db_select('activity', 'ac');

  
    $query->distinct();


    if (user_has_role(11))
        {

            //$query->condition('ac.delegated_username',$user->name,'='); 
            $query->fields('ac', array('time','source_request','variable_name','motivation','access_type','domain','delegated_app_name','username'))

	     ->orderBy('time', 'DESC')//ORDER BY created
	          ->range(0,5000);//LIMIT to 2 records

            
            $result = $query->execute();
    
          
            foreach($result as $stat_line) {
    
                $field_1=$stat_line->time;
                $field_2=$stat_line->delegated_app_name;
                $field_3=$stat_line->source_request;
                $field_4=$stat_line->variable_name;
                $field_5=$stat_line->motivation;
                $field_6=$stat_line->access_type;
                $field_7=$stat_line->domain;
                $field_8=$stat_line->username;
        
                $table_rows[] = array(
           
                    array('data' => $field_1),
                    array('data' => $field_2),
                    array('data' => $field_3),
                    array('data' => $field_4),
                    array('data' => $field_5),
                    array('data' => $field_6),
                    array('data' => $field_7),
                    array('data' => $field_8),
   
           
             
                );

            }
        }
    else
        {


            $query->condition('ac.username',$user->name,'=');
            $query->isNull('ac.delete_time');
            $query->isNull('ac.delegated_username');
            
            $query->fields('ac', array('time','source_request','variable_name','motivation','access_type','domain','delegated_app_name'));


            
            $result = $query->execute();
    
          
            foreach($result as $stat_line) {
    
                $field_1=$stat_line->time;
                $field_2=$stat_line->delegated_app_name;
                $field_3=$stat_line->source_request;
                $field_4=$stat_line->variable_name;
                $field_5=$stat_line->motivation;
                $field_6=$stat_line->access_type;
                $field_7=$stat_line->domain;
        
                $table_rows[] = array(
           
                    array('data' => $field_1),
                    array('data' => $field_2),
                    array('data' => $field_3),
                    array('data' => $field_4),
                    array('data' => $field_5),
                    array('data' => $field_6),
                    array('data' => $field_7),
   
           
             
                );

            }
    
    

        }

    //restore drupaldb flow
    db_set_active('default');
     

    
    // Or, render using a theme function.
    $variables = array(
        'attributes' => $attributes,
        'header' => $header,
        'rows' => $table_rows,
    );

    return theme_datatable($variables);
    
     
     
    
}


function retrieve_activityOnMyPersonaldata_app_list_datatable()
{

    global $user;


    $output = '';

    $table_rows = array();

   
    // Table settings.
    $attributes['datatable_options'] = array(
        'bFilter'   => TRUE,
        'bInfo'     => TRUE,
    );



    // Define table columns
    $header = array(
        array(
            'data' => t('Time'),
        
	    
            'datatable_options' => array(
                'bSortable' => TRUE,
                'bSearchable' => TRUE,
            ),
        ),

        array(
	
            'data' => t('Application name'),
	    
            'datatable_options' => array(
                'bSortable' => TRUE,
                'bSearchable' => TRUE,
            ),
        ),

        array(
	
            'data' => t('Source'),
	    
            'datatable_options' => array(
                'bSortable' => TRUE,
                'bSearchable' => TRUE,
            ),
        ),
	
        array(
	
            'data' => t('Variable'),
	    
            'datatable_options' => array(
                'bSortable' => TRUE,
                'bSearchable' => TRUE,
            ),
        ),
        array(
	
            'data' => t('Motivation'),
	    
            'datatable_options' => array(
                'bSortable' => TRUE,
                'bSearchable' => TRUE,
            ),
        ),

        array(
	
            'data' => t('Access type'),
	    
            'datatable_options' => array(
                'bSortable' => TRUE,
                'bSearchable' => TRUE,
            ),
        ),
	
        array(
	
            'data' => t('Domain'),
	    
            'datatable_options' => array(
                'bSortable' => TRUE,
                'bSearchable' => TRUE,
            ),
        ),

        array(
	
            'data' => t('Who'),
	    
            'datatable_options' => array(
                'bSortable' => TRUE,
                'bSearchable' => TRUE,
            ),
        ),
    );
    // Table data.
   



    
    //select activity.time as ,activity.app_id, data.app_name from activity, data  where data.app_id = activity.app_id
    
    db_set_active('profiledb');

    $query = db_select('activity', 'ac');

    $query->join('data', 'da', ' da.app_id= ac.delegated_app_id');
    $query->distinct();

    $query->condition('ac.username',$user->name,'='); 
    $query->isNotNull('ac.delegated_username');

    $query->fields('ac', array('time','source_request','variable_name','motivation','access_type','domain','delegated_username'));

    $query->isNull('ac.delete_time');

    $query->fields('da', array('app_name'));
    
    $result = $query->execute();
    
          
    foreach($result as $stat_line) {
    
        $field_1=$stat_line->time;
        $field_2=$stat_line->app_name;
        $field_3=$stat_line->source_request;
        $field_4=$stat_line->variable_name;
        $field_5=$stat_line->motivation;
        $field_6=$stat_line->access_type;
        $field_7=$stat_line->domain;
        $field_8=$stat_line->delegated_username;
        
        
        $table_rows[] = array(
           
            array('data' => $field_1),
            array('data' => $field_2),
            array('data' => $field_3),
            array('data' => $field_4),
            array('data' => $field_5),
            array('data' => $field_6),
            array('data' => $field_7),
            array('data' => $field_8),
   
           
             
        );

    }

    //restore drupaldb flow
    db_set_active('default');
     

    
    // Or, render using a theme function.
    $variables = array(
        'attributes' => $attributes,
        'header' => $header,
        'rows' => $table_rows,
    );

    return theme_datatable($variables);
    
     
     
    
}


?>
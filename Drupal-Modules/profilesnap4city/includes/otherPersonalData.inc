<?php



/*

List of all owned app 
Return a Rendered table
 */

//TODO
function retrieve_otherpersonaldata_app_list_datatable()
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
            'data' => t('Application name'),
            'datatable_options' => array(
                'bSortable' => TRUE,
                'bSearchable' => TRUE,
            ),
        ),

    );
    // Table data.
   



    
    //select app_id, app_name from data where username = drupal user and delete_time = NULL
    
    db_set_active('profiledb');

    $query = db_select('data', 'da');
    $query->distinct();
    $query->condition('da.username',$user->name,'='); 
    $query->isNull('da.delete_time');
    $query->isNotNull('da.app_id');
    $query->isNotNull('da.app_name');
    $query->fields('da', array('app_id','app_name'));
    $result = $query->execute();
    
     
     

    foreach($result as $stat_line) {

        $field_1='<a href=appdetails?='.$stat_line->app_id.'>'.$stat_line->app_name.'</a>';
        $field_2='('.$stat_line->app_id.')';
        $table_rows[] = array(
           
            array('data' => $field_1),
        //    array('data' => $field_2),

           
             
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


//TODO
function retrieve_otherpersonaldata_datatables($app_id)
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
        'data' => 'Date',
        'datatable_options' => array(
            'bSortable' => TRUE,
            'bSearchable' => TRUE,
        ),
    ),
    array(
        'data' => 'Application name',
        'datatable_options' => array(
            'bSortable' => TRUE,
            'bSearchable' => TRUE,
        ),
    ),
    array(
        'data' => 'Variable name',
        'datatable_options' => array(
            'bSortable' => TRUE,
            'bSearchable' => TRUE,
        ),
    ),
    array(
        'data' => 'Value',
        'datatable_options' => array(
            'bSortable' => TRUE,
            'bSearchable' => TRUE,
        ),
    ),
    array(
        'data' => 'Unit',
        'datatable_options' => array(
            'bSortable' => TRUE,
            'bSearchable' => TRUE,
        ),
    ),
    array(
        'data' => 'motivation',
        'datatable_options' => array(
            'bSortable' => TRUE,
            'bSearchable' => TRUE,
        ),
    ),
);

    
    
    db_set_active('profiledb');
    $query = db_select('data', 'da');
    $query->condition('da.username',$user->name,'=');
    $query->condition('da.app_id',$app_id,'=');
    $query->isNull('da.delete_time');
   
    $query->fields('da', array('data_time','app_name','variable_name', 'variable_value','variable_unit', 'motivation' ));

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


$variables = array(
    'attributes' => $attributes,
    'header' => $header,
    'rows' => $table_rows,
);



    
    db_set_active('default');
    return theme_datatable($variables);
    
}

?>
<?php
/*
Snap4city -- list.php --
   Copyright (C) 2020 DISIT Lab http://www.disit.org - University of Florence
   This program is free software: you can redistribute it and/or modify
   it under the terms of the GNU Affero General Public License as
   published by the Free Software Foundation, either version 3 of the
   License, or (at your option) any later version.
   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU Affero General Public License for more details.
   You should have received a copy of the GNU Affero General Public License
   along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
include_once("connection.php");
if ($_REQUEST["db"] != "iot" || ($_REQUEST["table"] != "data" && $_REQUEST["table"] != "links" && $_REQUEST["table"] != "rules")) {
    echo 'Error';
    exit();
}
$query = "SELECT `COLUMN_NAME` FROM `INFORMATION_SCHEMA`.`COLUMNS` WHERE `TABLE_SCHEMA` = '" . mysqli_real_escape_string($connection, $_REQUEST['db']) . "' AND `TABLE_NAME` = '" . mysqli_real_escape_string($connection, $_REQUEST['table']) . "'";
$result = mysqli_query($connection, $query);
$fields = array();
$th = '';
$tt = '';
while ($row = $result->fetch_row()) {
    /* if ($row[0] != "id" && $row[0] != "username" && $row[0] != "iot_apps" && $row[0] != "devices_public" && $row[0] != "devices_private" && $row[0] != "dashboards_public" && $row[0] != "dashboards_private" && $row[0] != "date") {
      $tt .= '<th><a href="graph-motivations.php?field=' . $row[0] . '&days=' . $_REQUEST["days"] . '">' . str_replace("_", " ", $row[0]) . '</a></th>';
      } else {
      $tt .= '<th>' . str_replace("_", " ", $row[0]) . '</th>';
      } */
    if (strpos($row[0], "iot") !== false) {
     if($row[0] == "iot_db_storage_tx" || $row[0] == "iot_db_storage_rx" || $row[0] == "iot_filesystem_storage_tx" || $row[0] == "iot_filesystem_storage_rx" ||
        $row[0] == "iot_db_request_tx" || $row[0] == "iot_db_request_rx" || $row[0] == "iot_ascapi_tx" || $row[0] == "iot_ascapi_rx" || $row[0] == "iot_disces_tx" ||
        $row[0] == "iot_disces_rx" || $row[0] == "iot_dashboard_tx" || $row[0] == "iot_dashboard_rx" | $row[0] == "iot_datagate_tx" || $row[0] == "iot_datagate_rx" ||
        $row[0] == "iot_external_service_tx" || $row[0] == "iot_external_service_rx" || $row[0] == "iot_iot_service_tx" || $row[0] == "iot_iot_service_rx" ||
        $row[0] == "iot_mapping_tx" || $row[0] == "iot_mapping_rx" || $row[0] == "iot_microserviceusercreated_tx" || $row[0] == "iot_microserviceusercreated_rx" ||
        $row[0] == "iot_mydata_tx" || $row[0] == "iot_mydata_rx" || $row[0] == "iot_notificator_tx" || $row[0] == "iot_notificator_rx" || $row[0] == "iot_rstatistics_tx" ||
        $row[0] == "iot_rstatistics_rx" || $row[0] == "iot_sigfox_tx" || $row[0] == "iot_sigfox_rx" || $row[0] == "iot_undefined_tx" || $row[0] == "iot_undefined_rx" ||
        $row[0] == "iot_tx" || $row[0] == "iot_rx" || $row[0] == "iot_reads" || $row[0] == "iot_writes") {
        $th .= '<th data-column-id="' . $row[0] . '" data-type="numeric" data-visible="false" data-formatter="iot">' . str_replace("_", " ", $row[0]) . '</th>';
     } else {
        $th .= '<th data-column-id="' . $row[0] . '" data-type="numeric" data-formatter="iot">' . str_replace("_", " ", $row[0]) . '</th>';
       }
    } else {
        if($row[0] == "etl_writes") {
         $th .= '<th data-column-id="' . $row[0] . '" data-type="numeric" data-visible="false" data-formatter="' . $row[0] . '">' . str_replace("_", " ", $row[0]) . '</th>';
        } else {
           $th .= '<th data-column-id="' . $row[0] . '" data-type="numeric" data-formatter="' . $row[0] . '">' . str_replace("_", " ", $row[0]) . '</th>';
        }
    }
    $fields[] = $row[0];

    // if the table to be rendered is iot.data then include user's role and level fields after the username field (role and level are fields of the iot.roles_levels table)
    if ($_REQUEST['db'] == "iot" && $_REQUEST['table'] == "data" && $row[0] == "username") {
        $fields[] = "role";
        $th .= '<th data-column-id="role" data-type="numeric" data-formatter="role">role</th>';
        $fields[] = "level";
        $th .= '<th data-column-id="level" data-type="numeric" data-formatter="level">level</th>';
    }
}
?>
<html>
    <head>
        <title><?php
            if (isset($_REQUEST["title"])) {
                echo $_REQUEST["title"];
            } else {
                echo "IoT Metrics";
            }
            ?>
        </title>
        <link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.6/css/bootstrap.min.css" />
        <link rel = "stylesheet" href = "https://cdnjs.cloudflare.com/ajax/libs/jquery-bootgrid/1.3.1/jquery.bootgrid.css" />
        <!--<link rel = "stylesheet" type = "text/css" href = "css/typography.css" /> -->
        <script src = "https://ajax.googleapis.com/ajax/libs/jquery/2.2.0/jquery.min.js"></script> 
        <script src="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.6/js/bootstrap.min.js"></script>
        <script src="https://cdnjs.cloudflare.com/ajax/libs/jquery-bootgrid/1.3.1/jquery.bootgrid.js"></script>  
        <style>
            body {
                margin:0;
                padding:0;
                background-color:#f1f1f1;
            }
            .box {
                width:1270px;
                padding:20px;
                background-color:#fff;
                border:1px solid #ccc;
                border-radius:5px;
                margin-top:25px;
            }
            a {
                color:inherit;
                text-decoration: none;
            }
        </style>
        <style>
            .cgb-header-name {
                width: 50%;
            }
        </style>
    </head>
    <body>
        <div class="container box">
            <h1 align="center"><?php
                if (isset($_REQUEST["title"])) {
                    echo $_REQUEST["title"];
                } else {
                    echo "IoT Metrics";
                }
                ?>
            </h1>
            <br />
            <div align="right">
                <!--<button type="button" id="add_button" data-toggle="modal" data-target="#productModal" class="btn btn-info btn-lg">Add</button>-->
            </div>
            <div class="table-responsive">
                <table data-header-css-class="cbg-header-name" class="table table-bordered table-striped">
                    <thead>
                        <?php /* echo $tt; */ ?>
                    </thead>
                </table>
                <table id="product_data" data-header-css-class="cbg-header-name" class="table table-bordered table-striped">
                    <thead>
                        <tr>
                            <?php echo $th; ?>
                            <!--<th data-column-id="actions" data-width="30%" data-formatter="actions" data-sortable="false">Actions</th>-->
                        </tr>
                    </thead>
                </table>
            </div>
    </body>
</html>
<script type="text/javascript" language="javascript">
    $(document).ready(function () {
        $('#add_button').click(function () {
            $('#form')[0].reset();
            $('.modal-title').text("Add Row");
            $('#action').val("Add");
            $('#operation').val("Add");
        });

        function LinkFormatter(value, row, index) {
            return "<a href='" + row.url + "'>" + value + "</a>";
        }

        var table = $('#product_data').bootgrid({
            ajax: true,
            rowSelect: true,
            select: true,
            post: function () {
                return {
                    id: "b0df282a-0d67-40e5-8558-c9e93b7befed"
                };
            },
            url: "fetch.php?db=<?php echo $_REQUEST["db"]; ?>&table=<?php echo $_REQUEST["table"]; ?>&fields=<?php echo urlencode(json_encode($fields)); ?>&days=<?php echo $_REQUEST["days"]; ?>",
            formatters: {
                "actions": function (column, row) {
                    //return "<button type='button' class='btn btn-warning btn-xs update' data-row-id='" + row.id + "'>Edit</button>" +
                    //"&nbsp; <button type='button' class='btn btn-danger btn-xs delete' data-row-id='" + row.id + "'>Delete</button>";
                    return "<a href=\"graph.php?db=<?php echo $_REQUEST["db"]; ?>&table=<?php echo $_REQUEST["table"]; ?>&username=" + row.username + "\">Graph</a>";
                },
                "username": function (column, row) {
                    return "<a href=\"graph-user.php?db=<?php echo $_REQUEST["db"]; ?>&table=<?php echo $_REQUEST["table"]; ?>&username=" + row.username + "&role=" + row.role + "&level=" + row.level + "\">" + row.username + "</a>";
                },
                "iot": function (column, row) {
                    //console.log(row)
                    return "<a href=\"graph-motivations.php?field=" + column.id + "&days=<?php echo $_REQUEST["days"]; ?>\">" + row[column.id] + "</a>";
                },
                "action": function (column, row) {
                    return "<button type='button' class='btn btn-warning btn-xs update' data-row-id='" + row.id + "'>Edit</button>" +
                            "&nbsp; <button type='button' class='btn btn-danger btn-xs delete' data-row-id='" + row.id + "'>Delete</button>";
                }
            }
        });

        $(document).on('submit', '#form', function (event) {
            event.preventDefault();
            var dbName = "<?php echo $_REQUEST["db"]; ?>";
            var tableName = "<?php echo $_REQUEST["table"]; ?>";
            var fields = "<?php echo urlencode(json_encode($fields)); ?>";
<?php
foreach ($fields as $field) {
    echo "var " . $field . "=" . "$('#" . $field . "').val();";
}
?>
            var form_data = $(this).serialize();
            $.ajax({
                url: "insert.php",
                method: "POST",
                data: form_data,
                success: function (data) {
                    $('#form')[0].reset();
                    $('#productModal').modal('hide');
                    $('#product_data').bootgrid('reload');
                }
            });
        });

        $(document).on("loaded.rs.jquery.bootgrid", function () {
            table.find(".update").on("click", function (event) {
                var id = $(this).data("row-id");
                $.ajax({
                    url: "fetchSingle.php",
                    method: "POST",
                    data: {id: id, db: '<?php echo $_REQUEST["db"]; ?>', table: '<?php echo $_REQUEST["table"]; ?>', fields: '<?php echo urlencode(json_encode($fields)); ?>'},
                    dataType: "json",
                    success: function (data) {
                        $('#productModal').modal('show');
<?php
foreach ($fields as $field) {
    echo "$('#" . $field . "').val(data." . $field . ");";
}
?>
                        $('#id').val(id);
                        $('.modal-title').text("Edit");
                        $('#action').val("Edit");
                        $('#operation').val("Edit");
                    }
                });
            });
        });

        $(document).on("loaded.rs.jquery.bootgrid", function () {
            table.find(".delete").on("click", function (event) {
                if (confirm("Are you sure you want to delete this?")) {
                    var id = $(this).data("row-id");
                    $.ajax({
                        url: "delete.php",
                        method: "POST",
                        data: {id: id, db: "<?php echo $_REQUEST["db"]; ?>", table: "<?php echo $_REQUEST["table"]; ?>"},
                        success: function (data) {
                            alert(data);
                            $('#product_data').bootgrid('reload');
                        }
                    })
                } else {
                    return false;
                }
            });
        });
    });
</script>
<div id="productModal" class="modal fade">
    <div class="modal-dialog">
        <form method="post" id="form">
            <div class="modal-content">
                <div class="modal-header">
                    <button type="button" class="close" data-dismiss="modal">&times;</button>
                    <h4 class="modal-title">Add</h4>
                </div>
                <div class="modal-body">
                    <?php
                    if ($_REQUEST["table"] == "triggers") {
                        echo '<label>Select Type</label>';
                        echo '<select name="type" id="type" class="form-control">';
                        echo '<option value="Node-RED">Node-RED</option>';
                        echo '<option value="ETL">ETL</option>';
                        echo '</select>';
                        echo '<br />';
                    }
                    ?>
                    <?php
                    foreach ($fields as $field) {
                        if ($field != 'id') {
                            echo '<label>' . ucfirst($field) . '</label>';
                            echo '<input type="text" name="' . $field . '" id="' . $field . '" class="form-control"/>';
                            echo '<br />';
                        }
                    }
                    ?>
                </div>
                <div class="modal-footer">
                    <input type="hidden" name="id" id="id" />
                    <input type="hidden" name="operation" id="operation" />
                    <input type="hidden" name="dbName" id="dbName" value="<?php echo $_REQUEST["db"]; ?>"/>
                    <input type="hidden" name="tableName" id="tableName" value="<?php echo $_REQUEST["table"]; ?>"/>
                    <input type="hidden" name="fieldNames" id="fieldNames" value="<?php echo urlencode(json_encode($fields)); ?>"/>
                    <input type="submit" name="action" id="action" class="btn btn-success" value="Add" />
                </div>
            </div>
        </form>
    </div>
</div>

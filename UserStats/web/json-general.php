<?php
/*
Snap4city -- json-general.php --
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
include("connection.php");

// motivation for each user in the last x days
if (isset($_REQUEST["field"]) && isset($_REQUEST["username"]) && $_REQUEST["username"] == "all" && isset($_REQUEST["days"])) {
    $query = "SELECT username, UNIX_TIMESTAMP(date) * 1000 AS timestamp, IF(SUM(" . mysqli_real_escape_string($connection, $_REQUEST["field"]) . ") IS NOT NULL, SUM(" . mysqli_real_escape_string($connection, $_REQUEST["field"]) . "), 0) AS " . mysqli_real_escape_string($connection, $_REQUEST["field"]) . " FROM " . mysqli_real_escape_string($connection, $_REQUEST["db"]) . "." . mysqli_real_escape_string($connection, $_REQUEST["table"]) . " WHERE date >= CURDATE() - INTERVAL " . mysqli_real_escape_string($connection, $_REQUEST["days"]) . " DAY GROUP BY username";

    $result = mysqli_query($connection, $query);

    while ($row = mysqli_fetch_assoc($result)) {
        if ($row[$_REQUEST["field"]] != null && $row[$_REQUEST["field"]] >= 0) {
            $data[] = array("name" => $row["username"], "y" => doubleval($row[$_REQUEST["field"]]), "drilldown" => $row["username"]);
        }
    }
}
// all motivations for a user in the last x days
else if (isset($_REQUEST["db"]) && isset($_REQUEST["table"]) && isset($_REQUEST["field"]) && $_REQUEST["field"] == "all" && isset($_REQUEST["days"])) {
    // get the users' list
    $usernames = array();

    $query = "SELECT DISTINCT(username) FROM " . mysqli_real_escape_string($connection, $_REQUEST["db"]) . "." . mysqli_real_escape_string($connection, $_REQUEST["table"]) . " WHERE date >= CURDATE() - INTERVAL " . mysqli_real_escape_string($connection, $_REQUEST["days"]) . " DAY";

    $result = mysqli_query($connection, $query);

    while ($row = mysqli_fetch_assoc($result)) {
        $usernames[] = $row["username"];
    }

    foreach ($usernames as $username) {
        $query = "SELECT " .
                "IF(SUM(iot_db_storage_tx) IS NOT NULL, SUM(iot_db_storage_tx), 0) AS iot_db_storage_tx, " .
                "IF(SUM(iot_db_storage_rx) IS NOT NULL, SUM(iot_db_storage_rx), 0) AS iot_db_storage_rx, " .
                "IF(SUM(iot_filesystem_storage_tx) IS NOT NULL, SUM(iot_filesystem_storage_tx), 0) AS iot_filesystem_storage_tx, " .
                "IF(SUM(iot_filesystem_storage_rx) IS NOT NULL, SUM(iot_filesystem_storage_rx), 0) AS iot_filesystem_storage_rx, " .
                "IF(SUM(iot_db_request_tx) IS NOT NULL, SUM(iot_db_request_tx), 0) AS iot_db_request_tx, " .
                "IF(SUM(iot_db_request_rx) IS NOT NULL, SUM(iot_db_request_rx), 0) AS iot_db_request_rx, " .
                "IF(SUM(iot_ascapi_tx) IS NOT NULL, SUM(iot_ascapi_tx), 0) AS iot_ascapi_tx, " .
                "IF(SUM(iot_ascapi_rx) IS NOT NULL, SUM(iot_ascapi_rx), 0) AS iot_ascapi_rx, " .
                "IF(SUM(iot_disces_tx) IS NOT NULL, SUM(iot_disces_tx), 0) AS iot_disces_tx, " .
                "IF(SUM(iot_disces_rx) IS NOT NULL, SUM(iot_disces_rx), 0) AS iot_disces_rx, " .
                "IF(SUM(iot_dashboard_tx) IS NOT NULL, SUM(iot_dashboard_tx), 0) AS iot_dashboard_tx, " .
                "IF(SUM(iot_dashboard_rx) IS NOT NULL, SUM(iot_dashboard_rx), 0) AS iot_dashboard_rx, " .
                "IF(SUM(iot_datagate_tx) IS NOT NULL, SUM(iot_datagate_tx), 0) AS iot_datagate_tx, " .
                "IF(SUM(iot_datagate_rx) IS NOT NULL, SUM(iot_datagate_rx), 0) AS iot_datagate_rx, " .
                "IF(SUM(iot_external_service_tx) IS NOT NULL, SUM(iot_external_service_tx), 0) AS iot_external_service_tx, " .
                "IF(SUM(iot_external_service_rx) IS NOT NULL, SUM(iot_external_service_rx), 0) AS iot_external_service_rx, " .
                "IF(SUM(iot_iot_service_tx) IS NOT NULL, SUM(iot_iot_service_tx), 0) AS iot_iot_service_tx, " .
                "IF(SUM(iot_iot_service_rx) IS NOT NULL, SUM(iot_iot_service_rx), 0) AS iot_iot_service_rx, " .
                "IF(SUM(iot_mapping_tx) IS NOT NULL, SUM(iot_mapping_tx), 0) AS iot_mapping_tx, " .
                "IF(SUM(iot_mapping_rx) IS NOT NULL, SUM(iot_mapping_rx), 0) AS iot_mapping_rx, " .
                "IF(SUM(iot_microserviceusercreated_tx) IS NOT NULL, SUM(iot_microserviceusercreated_tx), 0) AS iot_microserviceusercreated_tx, " .
                "IF(SUM(iot_microserviceusercreated_rx) IS NOT NULL, SUM(iot_microserviceusercreated_rx), 0) AS iot_microserviceusercreated_rx, " .
                "IF(SUM(iot_mydata_tx) IS NOT NULL, SUM(iot_mydata_tx), 0) AS iot_mydata_tx, " .
                "IF(SUM(iot_mydata_rx) IS NOT NULL, SUM(iot_mydata_rx), 0) AS iot_mydata_rx, " .
                "IF(SUM(iot_notificator_tx) IS NOT NULL, SUM(iot_notificator_tx), 0) AS iot_notificator_tx, " .
                "IF(SUM(iot_notificator_rx) IS NOT NULL, SUM(iot_notificator_rx), 0) AS iot_notificator_rx, " .
                "IF(SUM(iot_rstatistics_tx) IS NOT NULL, SUM(iot_rstatistics_tx), 0) AS iot_rstatistics_tx, " .
                "IF(SUM(iot_rstatistics_rx) IS NOT NULL, SUM(iot_rstatistics_rx), 0) AS iot_rstatistics_rx, " .
                "IF(SUM(iot_sigfox_tx) IS NOT NULL, SUM(iot_sigfox_tx), 0) AS iot_sigfox_tx, " .
                "IF(SUM(iot_sigfox_rx) IS NOT NULL, SUM(iot_sigfox_rx), 0) AS iot_sigfox_rx, " .
                "IF(SUM(iot_undefined_tx) IS NOT NULL, SUM(iot_undefined_tx), 0) AS iot_undefined_tx, " .
                "IF(SUM(iot_undefined_rx) IS NOT NULL, SUM(iot_undefined_rx), 0) AS iot_undefined_rx    " .
                "FROM " . mysqli_real_escape_string($connection, $_REQUEST["db"]) . "." . mysqli_real_escape_string($connection, $_REQUEST["table"]) . " WHERE username = '" . mysqli_real_escape_string($connection, $username) . "' AND date >= CURDATE() - INTERVAL " . mysqli_real_escape_string($connection, $_REQUEST["days"]) . " DAY";

        $result = mysqli_query($connection, $query);

        $motivations = array();

        while ($row = mysqli_fetch_assoc($result)) {
            $motivations[] = array("iot_db_storage_tx", doubleval($row["iot_db_storage_tx"]));
            $motivations[] = array("iot_db_storage_rx", doubleval($row["iot_db_storage_rx"]));
            $motivations[] = array("iot_filesystem_storage_tx", doubleval($row["iot_filesystem_storage_tx"]));
            $motivations[] = array("iot_filesystem_storage_rx", doubleval($row["iot_filesystem_storage_rx"]));
            $motivations[] = array("iot_db_request_tx", doubleval($row["iot_db_request_tx"]));
            $motivations[] = array("iot_db_request_rx", doubleval($row["iot_db_request_rx"]));
            $motivations[] = array("iot_ascapi_tx", doubleval($row["iot_ascapi_tx"]));
            $motivations[] = array("iot_ascapi_rx", doubleval($row["iot_ascapi_rx"]));
            $motivations[] = array("iot_disces_tx", doubleval($row["iot_disces_tx"]));
            $motivations[] = array("iot_disces_rx", doubleval($row["iot_disces_rx"]));
            $motivations[] = array("iot_dashboard_tx", doubleval($row["iot_dashboard_tx"]));
            $motivations[] = array("iot_dashboard_rx", doubleval($row["iot_dashboard_rx"]));
            $motivations[] = array("iot_datagate_tx", doubleval($row["iot_datagate_tx"]));
            $motivations[] = array("iot_datagate_rx", doubleval($row["iot_datagate_rx"]));
            $motivations[] = array("iot_external_service_tx", doubleval($row["iot_external_service_tx"]));
            $motivations[] = array("iot_external_service_rx", doubleval($row["iot_external_service_rx"]));
            $motivations[] = array("iot_iot_service_tx", doubleval($row["iot_iot_service_tx"]));
            $motivations[] = array("iot_iot_service_rx", doubleval($row["iot_iot_service_rx"]));
            $motivations[] = array("iot_mapping_tx", doubleval($row["iot_mapping_tx"]));
            $motivations[] = array("iot_mapping_rx", doubleval($row["iot_mapping_rx"]));
            $motivations[] = array("iot_microserviceusercreated_tx", doubleval($row["iot_microserviceusercreated_tx"]));
            $motivations[] = array("iot_microserviceusercreated_rx", doubleval($row["iot_microserviceusercreated_rx"]));
            $motivations[] = array("iot_mydata_tx", doubleval($row["iot_mydata_tx"]));
            $motivations[] = array("iot_mydata_rx", doubleval($row["iot_mydata_rx"]));
            $motivations[] = array("iot_notificator_tx", doubleval($row["iot_notificator_tx"]));
            $motivations[] = array("iot_notificator_rx", doubleval($row["iot_notificator_rx"]));
            $motivations[] = array("iot_rstatistics_tx", doubleval($row["iot_rstatistics_tx"]));
            $motivations[] = array("iot_rstatistics_rx", doubleval($row["iot_rstatistics_rx"]));
            $motivations[] = array("iot_sigfox_tx", doubleval($row["iot_sigfox_tx"]));
            $motivations[] = array("iot_sigfox_rx", doubleval($row["iot_sigfox_rx"]));
            $motivations[] = array("iot_undefined_tx", doubleval($row["iot_undefined_tx"]));
            $motivations[] = array("iot_undefined_rx", doubleval($row["iot_undefined_rx"]));
        }
        $data[] = array("name" => $username, "id" => $username, "data" => $motivations);
    }
}
// kB (tx, rx) per user (Area Manager) per day (2 users)
else if (isset($_REQUEST["field"]) && isset($_REQUEST["role"]) && $_REQUEST["role"] == "AreaManager") {
    $query = "SELECT CEIL(SUM(" . mysqli_real_escape_string($connection, $_REQUEST["field"]) . ")/2) AS " . mysqli_real_escape_string($connection, $_REQUEST["field"]) . ", " .
        "UNIX_TIMESTAMP(date) * 1000 AS timestamp " .
        "FROM " . mysqli_real_escape_string($connection, $_REQUEST["db"]) . "." . mysqli_real_escape_string($connection, $_REQUEST["table"]) . " " .
        "WHERE (username = 'disit' OR username = 'snap4city') GROUP BY date(date)";

    $result = mysqli_query($connection, $query);

    while ($row = mysqli_fetch_assoc($result)) {
        $data[] = array(intval($row["timestamp"]), doubleval($row[$_REQUEST["field"]]));
    }
}
// kB (tx, rx) per user (Manager) per day (6 users)
else if (isset($_REQUEST["field"]) && isset($_REQUEST["role"]) && $_REQUEST["role"] == "Manager") {
    $query = "SELECT CEIL(SUM(" . mysqli_real_escape_string($connection, $_REQUEST["field"]) . ")/6) AS " . mysqli_real_escape_string($connection, $_REQUEST["field"]) . ", " .
        "UNIX_TIMESTAMP(date) * 1000 AS timestamp " .
        "FROM " . mysqli_real_escape_string($connection, $_REQUEST["db"]) . "." . mysqli_real_escape_string($connection, $_REQUEST["table"]) . " " .
        "WHERE (username = 'adifino' OR username = 'badii' OR username = 'PaoloNesi'" .
        "OR username = 'nicola.mitolo' OR username = 'pb1' OR username = 'pb3') GROUP BY date(date)";

    $result = mysqli_query($connection, $query);

    while ($row = mysqli_fetch_assoc($result)) {
        $data[] = array(intval($row["timestamp"]), doubleval($row[$_REQUEST["field"]]));
    }
}
// kB (tx, rx) (Area Manager) sum (2 users)
else if (isset($_REQUEST["role"]) && $_REQUEST["role"] == "AreaManager" && isset($_REQUEST["time"]) && $_REQUEST["time"] == "all") {
    $query = "SELECT SUM(iot_tx)/(2*(datediff(date(max(date)), date(min(date)))-1)) AS iot_tx, 
        SUM(iot_rx)/(2*(datediff(date(max(date)), date(min(date)))-1)) AS iot_rx
        FROM iot.data 
        WHERE (username = 'disit' OR username = 'snap4city') AND date < date(NOW())";

    $result = mysqli_query($connection, $query);

    while ($row = mysqli_fetch_assoc($result)) {
        $data[] = array(intval($row["iot_tx"]), intval($row["iot_rx"]));
    }
}
// kB (tx, rx) (Manager) sum (6 users)
else if (isset($_REQUEST["role"]) && $_REQUEST["role"] == "Manager" && isset($_REQUEST["time"]) && $_REQUEST["time"] == "all") {
    $query = "SELECT SUM(iot_tx)/(6*(datediff(date(max(date)), date(min(date)))-1)) AS iot_tx, " .
        "SUM(iot_rx)/(6*(datediff(date(max(date)), date(min(date)))-1)) AS iot_rx " .
        "FROM iot.data " .
        "WHERE (username = 'adifino' OR username = 'badii' OR username = 'PaoloNesi'" .
        "OR username = 'nicola.mitolo' OR username = 'pb1' OR username = 'pb3') AND date < date(NOW())";

    $result = mysqli_query($connection, $query);

    while ($row = mysqli_fetch_assoc($result)) {
        $data[] = array(intval($row["iot_tx"]), intval($row["iot_rx"]));
    }
}
// IoT reads (Area Manager) sum (2 users)
else if (isset($_REQUEST["field"]) && $_REQUEST["field"] == 'iot_reads' && isset($_REQUEST["role"]) && $_REQUEST["role"] == "AreaManager" && isset($_REQUEST["time"]) && $_REQUEST["time"] == "all") {
    $query = "SELECT SUM(iot_reads)/2 AS iot_reads, " .
        "UNIX_TIMESTAMP(date) * 1000 AS timestamp, " .
        "FROM iot.data " . 
        "WHERE (username = 'disit' OR username = 'snap4city') GROUP BY date(date)";

    $result = mysqli_query($connection, $query);

    while ($row = mysqli_fetch_assoc($result)) {
        $data[] = array(intval($row["timestamp"]), intval($row['iot_reads']));
    }
}
// IoT reads (Manager) sum (6 users)
else if (isset($_REQUEST["field"]) && $_REQUEST["field"] == 'iot_reads' && isset($_REQUEST["role"]) && $_REQUEST["role"] == "Manager") {
    $query = "SELECT SUM(iot_reads)/6 AS iot_reads, " .
        "UNIX_TIMESTAMP(date) * 1000 AS timestamp, " .
        "FROM iot.data " .
        "WHERE (username = 'adifino' OR username = 'badii' OR username = 'PaoloNesi'" .
        "OR username = 'nicola.mitolo' OR username = 'pb1' OR username = 'pb3') AND date < date(NOW()) GROUP BY date(date)";

    $result = mysqli_query($connection, $query);

    while ($row = mysqli_fetch_assoc($result)) {
        $data[] = array(intval($row["timestamp"]), intval($row['iot_reads']));
    }
}
// IoT writes (Area Manager) sum (2 users)
else if (isset($_REQUEST["field"]) && $_REQUEST["field"] == 'iot_writes' && isset($_REQUEST["role"]) && $_REQUEST["role"] == "AreaManager") {
    $query = "SELECT SUM(iot_writes)/2 AS iot_writes, " .
        "UNIX_TIMESTAMP(date) * 1000 AS timestamp, " .
        "FROM iot.data " .
        "WHERE (username = 'disit' OR username = 'snap4city') AND date < date(NOW()) GROUP BY date(date)";

    $result = mysqli_query($connection, $query);

    while ($row = mysqli_fetch_assoc($result)) {
        $data[] = array(intval($row["timestamp"]), intval($row['iot_writes']));
    }
}
// IoT writes (Manager) sum (6 users)
else if (isset($_REQUEST["field"]) && $_REQUEST["field"] == 'iot_writes' && isset($_REQUEST["role"]) && $_REQUEST["role"] == "Manager" && isset($_REQUEST["time"]) && $_REQUEST["time"] == "all") {
    $query = "SELECT SUM(iot_writes)/6 AS iot_writes, " .
        "UNIX_TIMESTAMP(date) * 1000 AS timestamp, " .
        "FROM iot.data " .
        "WHERE (username = 'adifino' OR username = 'badii' OR username = 'PaoloNesi'" .
        "OR username = 'nicola.mitolo' OR username = 'pb1' OR username = 'pb3') AND date < date(NOW()) GROUP BY date(date)";

    $result = mysqli_query($connection, $query);

    while ($row = mysqli_fetch_assoc($result)) {
        $data[] = array(intval($row["timestamp"]), intval($row['iot_writes']));
    }
}
// ETL writes ('disit' user) sum
else if (isset($_REQUEST["field"]) && $_REQUEST["field"] == 'etl_writes') {
    $query = "SELECT SUM(etl_writes) AS etl_writes, " .
        "UNIX_TIMESTAMP(date) * 1000 AS timestamp " .
        "FROM iot.data " .
        "WHERE (username = 'disit') AND date < date(NOW()) GROUP BY date(date)";

    $result = mysqli_query($connection, $query);

    while ($row = mysqli_fetch_assoc($result)) {
        $data[] = array(intval($row["timestamp"]), intval($row['etl_writes']));
    }
}
// Dashboards minutes (total per day)
else if (isset($_REQUEST["field"]) && $_REQUEST["field"] == 'dashboards_minutes') {
    $query = "SELECT SUM(dashboards_minutes) AS minutes, UNIX_TIMESTAMP(date) * 1000 AS timestamp FROM " . mysqli_real_escape_string($connection, $_REQUEST["db"]) . "." . mysqli_real_escape_string($connection, $_REQUEST["table"]) . " GROUP by date(date) having minutes > 0 ORDER BY date";
    $result = mysqli_query($connection, $query);

    while ($row = mysqli_fetch_assoc($result)) {
        $data[] = array(intval($row["timestamp"]), intval($row['minutes']));
    }
} else {
    $query = "SELECT UNIX_TIMESTAMP(date) * 1000 AS timestamp, IF(SUM(" . mysqli_real_escape_string($connection, $_REQUEST["field"]) . ") IS NOT NULL, SUM(" . mysqli_real_escape_string($connection, $_REQUEST["field"]) . "), 0) AS " . mysqli_real_escape_string($connection, $_REQUEST["field"]) . " FROM " . mysqli_real_escape_string($connection, $_REQUEST["db"]) . "." . mysqli_real_escape_string($connection, $_REQUEST["table"]) . " GROUP BY date";

    $result = mysqli_query($connection, $query);

    while ($row = mysqli_fetch_assoc($result)) {
        if ($row[$_REQUEST["field"]] != null && $row[$_REQUEST["field"]] > 0) {
            $data[] = array(intval($row["timestamp"]), doubleval($row[$_REQUEST["field"]]));
        }
    }
}
mysqli_close($connection);

echo json_encode($data);
?>

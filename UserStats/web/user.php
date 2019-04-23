<?php
// http://jsfiddle.net/RR4hw/7/ 
// https://jsfiddle.net/8jpmkcr5/2791/ 
// http://jsfiddle.net/cjp7y/2262/
// https://stackoverflow.com/questions/20964218/how-to-restrict-bootstrap-date-picker-from-future-date
// http://jsfiddle.net/tovic/ve8mU/light/
// http://markdotto.com/playground/3d-text/

header('Access-Control-Allow-Origin: *');
require 'sso/autoload.php';

use Jumbojett\OpenIDConnectClient;

function getToken() {
    $oidc = new OpenIDConnectClient(
            'https://www.snap4city.org', 'php-userstats', 'b941378b-4088-46e2-9e59-abaf8614ddf4'
    );

    $oidc->setVerifyHost(false);
    $oidc->setVerifyPeer(false);

    $oidc->providerConfigParam(array('authorization_endpoint' => 'https://www.snap4city.org/auth/realms/master/protocol/openid-connect/auth'));
    $oidc->providerConfigParam(array('token_endpoint' => 'https://www.snap4city.org/auth/realms/master/protocol/openid-connect/token'));
    $oidc->providerConfigParam(array('userinfo_endpoint' => 'https://www.snap4city.org/auth/realms/master/protocol/openid-connect/userinfo'));
    $oidc->providerConfigParam(array('jwks_uri' => 'https://www.snap4city.org/auth/realms/master/protocol/openid-connect/certs'));
    $oidc->providerConfigParam(array('issuer' => 'https://www.snap4city.org/auth/realms/master'));
    $oidc->providerConfigParam(array('end_session_endpoint' => 'https://www.snap4city.org/auth/realms/master/protocol/openid-connect/logout'));

    if (!isset($_SESSION['login'])) {
        $oidc->addScope(array('openid', 'username', 'profile'));
        $oidc->setRedirectURL($sso_login_redirect);
        $oidc->authenticate();
        $name = $oidc->requestUserInfo('username');
        $_SESSION['login'] = $name;
        $_SESSION['refreshToken'] = $oidc->getRefreshToken();
        $_SESSION['accessToken'] = $oidc->getAccessToken();
        //echo $_SESSION['login'] . "<br/>";
        return $_SESSION['accessToken'];
    } else {
        //echo $_SESSION['login'] . "<br/>";
        $tkn = $oidc->refreshToken($_SESSION['refreshToken']);
        // echo $tkn->access_token;
        $_SESSION['accessToken'] = $tkn->access_token;
        $_SESSION['refreshToken'] = $tkn->refresh_token;
        return $_SESSION['accessToken'];
    }
}

function getLimits($username) {
    $accessToken = getToken();
    if ($username != null) {
        $json = file_get_contents("http://192.168.0.10/ownership-api/v1/limits/?accessToken=" . $accessToken . "&username=" . $username);
    } else {
        $json = file_get_contents("http://192.168.0.10/ownership-api/v1/limits/?accessToken=" . $accessToken);
    }
    $json = json_decode($json, true);
    return $json["limits"];
}

session_start();

$limits = getLimits($_SESSION["username"]);

// check the permission
if (!isset($_SESSION["role"])) {
    header("location: ssoLogin.php");
}

include("connection.php");

$query = "SELECT role, level FROM iot.roles_levels WHERE username = '" . $_SESSION["username"] . "'";

$result = mysqli_query($connection, $query);

$role = null;

$level = null;

$users = array();

while ($row = mysqli_fetch_assoc($result)) {
    $role = $row["role"];
    $level = $row["level"];
    $_SESSION["level"] = $level;
}

if ($role == "RootAdmin") {
    $query = "SELECT username, role, level FROM iot.roles_levels ORDER BY username";
    $result = mysqli_query($connection, $query);
    while ($row = mysqli_fetch_assoc($result)) {
        $users[] = $row["username"];
    }
}

if (count($users) == 0) {
    $users[] = $_SESSION["username"];
    $connection->close();
}
?>

<html lang="en">
    <head>
        <!-- imports for datepickers -->
        <script type="text/javascript" src="javascript/jquery-2.2.4.min.js"></script>
        <script type="text/javascript" src="javascript/bootstrap.min.js"></script>
        <link rel="stylesheet" type="text/css" href="css/bootstrap.min.css"/>
        <link rel="stylesheet" type="text/css" href="css/datepicker.min.css"/>
        <link rel="stylesheet" type="text/css" href="css/user.css"/>
        <script type = "text/javascript" src = "javascript/bootstrap-datepicker.js" ></script>   
        <style>
            .data1{margin-left:30px; margin-top:10px;}
            .date1{margin-left:30px; margin-top:10px;}
        </style>
        <script>
            function clearFields() {
             $("#dashboards").text("0 (0/0)");
             $("#dashboardsAccesses").text("0");
             $("#dashboardsMinutes").text("0");
             $("#IoTDevices").text("0 (0/0)");
             $("#IoTTxRx").text("0/0");
             $("#IoTApplications").text("0");
             $("#ETL").text("0");
             $("#RTxRx").text("0/0")
            }

            function setData(username, data, obj) {
                clearFields()
                if (typeof obj["username"] != "undefined") {
                    dashboards_public = obj["dashboards_public"] == null ? "0" : obj["dashboards_public"]
                    dashboards_public = parseInt(dashboards_public) > 0 ? dashboards_public : "0"
                    dashboards_private = obj["dashboards_private"] == null ? "0" : obj["dashboards_private"]
                    dashboards_private = parseInt(dashboards_private) > 0 ? dashboards_private : "0"
                    dashboards = parseInt(dashboards_public) + parseInt(dashboards_private)
                    $("#dashboards").text(dashboards + " (" + dashboards_public + "/" + dashboards_private + ")");
                    dashboardsAccesses = obj["dashboards_accesses"] == null ? "0" : obj["dashboards_accesses"]
                    $("#dashboardsAccesses").text(obj["dashboards_accesses"]);
                    dashboardsMinutes = obj["dashboards_minutes"] == null ? "0" : obj["dashboards_minutes"]
                    $("#dashboardsMinutes").text(obj["dashboards_minutes"]);
                    devices_public = obj["devices_public"] == null ? "0" : obj["devices_public"]
                    devices_private = obj["devices_private"] == null ? "0" : obj["devices_private"]
                    devices = parseInt(devices_public) + parseInt(devices_private)
                    $("#IoTDevices").text(devices + " (" + devices_public + "/" + devices_private + ")");
                    tx = obj["iot_tx"] == null ? "0" : Math.round(parseFloat(obj["iot_tx"]) * 100) / 100
                    rx = obj["iot_rx"] == null ? "0" : Math.round(parseFloat(obj["iot_rx"]) * 100) / 100
                    $("#IoTTxRx").text(tx + "/" + rx);
                    iot_apps = obj["iot_apps"] == null ? "0" : obj["iot_apps"]
                    $("#IoTApplications").text(iot_apps);
                    //$("#AMMA").text("0");
                    //$("#DevDash").text("0");
                    //$("#ResDash").text("0");
                    //$("#IoTBlocks").text("0");
                    //$("#MicroService").text("0");
                    //$("#IoTApp").text("0");


                    r_statistics_tx = obj["r_statistics_tx"] == null ? "0" : obj["r_statistics_tx"]
                    r_statistics_rx = obj["r_statistics_rx"] == null ? "0" : obj["r_statistics_rx"]
                    $("#RTxRx").text(r_statistics_tx + "/" + r_statistics_rx)

                    etl_writes = obj["etl_writes"] == null ? "0" : obj["etl_writes"]
                    $("#ETL").text(etl_writes)

                    role = obj["role"] == null ? "-" : obj["role"]
                    $("#Role").text(role)

                    level = obj["level"] == null ? "-" : obj["level"]
                    $("#Level").text(level)

                    for (var key in obj["resources"]) {
                        if (obj["resources"].hasOwnProperty(key)) {
                            $("#" + key).text(obj["resources"][key]);
                        }
                    }
                    $("span#details").html("<button class=\"large button\"><a href=\"graph-user.php?db=iot&table=data&username=" + obj["username"] + "&role=" + obj["role"] + "&level=" + obj["level"] + "\">details</a></button>")
                } else {
                    $("#dashboards").text("0 (0/0)");
                    $("#dashboardsAccesses").text("0");
                    $("#dashboardsMinutes").text("0");
                    $("#IoTDevices").text("0 (0/0)");
                    $("#IoTTxRx").text("0/0");
                    $("#IoTApplications").text("0");
                    $("#RTxRx").text("0/0");
                    $("#ETL").text("0");
                    //$("#AMMA").text("0");
                    //$("#DevDash").text("0");
                    //$("#ResDash").text("0");
                    //$("#IoTBlocks").text("0");
                    //$("#MicroService").text("0");
                    //$("#IoTApp").text("0");
                }
            }
            $(document).ready(function () {
                $(function () {
                    // localized formats of moment.js:
                    // L for date only
                    // LT for time only
                    // L LT for date and time
                    $('#datepickerday').datepicker({
                        //format: '<?php /* echo $_REQUEST["time"] == "month" ? "yyyy-mm" : "yyyy-mm-dd"; */ ?>',
                        format: 'yyyy-mm-dd',
<?php /* echo $_REQUEST["time"] == "month" ? "startView: 'months',\nminViewMode: 'months',\n" : "endDate: '-1d',"; */ ?>
                        endDate: '-1d',
                        autoclose: true
                    }).on("changeDate", function (e) {
                        $.post("getUserData.php", {username: $("#userselect").val(), date: $(this).val()})
                                .done(function (data) {
                                    var obj = $.parseJSON(data);
                                    setData($("#userselect").val(), data, obj)
                                }).fail(function () {
                            $("#dashboards").text("-/-");
                            $("#dashboardsAccesses").text("-");
                            $("#dashboardsMinutes").text("-");
                            $("#IoTDevices").text("-/-");
                            $("#IoTTxRx").text("-/-");
                            $("#IoTApplications").text("-");
                            $("#RTxRx").text("-/-");
                            $("#ETL").text("-");
                        });
                    });
                    var d = new Date();
                    d.setDate(d.getDate() - 1);
                    $("#datepickerday").datepicker('setDate', d);
                    $("#datepickerday").datepicker('update');
                    $("#datepickerday").css('display', 'inline-block');
                    $('#datepickermonth').datepicker({
                        //format: '<?php /* echo $_REQUEST["time"] == "month" ? "yyyy-mm" : "yyyy-mm-dd"; */ ?>',
                        format: 'yyyy-mm',
<?php /* echo $_REQUEST["time"] == "month" ? "startView: 'months',\nminViewMode: 'months',\n" : "endDate: '-1d',"; */ ?>
                        startView: 'months', minViewMode: 'months',
                        autoclose: true
                    }).on("changeDate", function (e) {
                        $.post("getUserData.php", {username: $("#userselect").val(), date: $(this).val()})
                                .done(function (data) {
                                    var obj = $.parseJSON(data);
                                    setData($("#userselect").val(), data, obj)
                                }).fail(function () {
                            $("#dashboards").text("-/-");
                            $("#dashboardsAccesses").text("-");
                            $("#dashboardsMinutes").text("-");
                            $("#IoTDevices").text("-/-");
                            $("#IoTTxRx").text("-/-");
                            $("#IoTApplications").text("-");
                            $("#RTxRx").text("-/-");
                            $("#ETL").text("-");
                        });
                    });
                    var d = new Date();
                    d.setDate(d.getDate() - 1);
                    $("#datepickermonth").datepicker('setDate', d);
                    $("#datepickermonth").datepicker('update');
                    $("#datepickermonth").css('display', 'none');
                    updateData('<?php echo $_SESSION["username"]; ?>')
                });
            });
            function updateData(username) {
                if (!$("#datepickerday").prop('disabled') || !$("#datepickermonth").prop('disabled')) {
                    date = $("#datepickerday").css("display") == "inline-block" ? $("#datepickerday").val() : $("#datepickermonth").val()
                } else {
                    date = $("#timeselect").val()
                }

                $.post("getUserData.php", {username: username, date: date})
                        .done(function (data) {
                            var obj = $.parseJSON(data);
                            setData(username, data, obj);
                        }).fail(function () {
                    $("#dashboards").text("-/-");
                    $("#dashboardsAccesses").text("-");
                    $("#dashboardsMinutes").text("-");
                    $("#IoTDevices").text("-/-");
                    $("#IoTTxRx").text("-/-");
                    $("#IoTApplications").text("-");
                    $("#RTxRx").text("-/-");
                    $("#ETL").text("-");
                });
                $.post("getLimits.php", {username: username})
                        .done(function (data) {
                            var obj = $.parseJSON(data);
                            for (var o in obj) {
                                a = obj[o]
                                elementType = a["elementType"]
                                limit = a["limit"]
                                $("#" + elementType).text(limit)
                            }
                        }).fail(function () {
                    for (var o in obj) {
                        a = obj[o]
                        elementType = a["elementType"]
                        limit = a["limit"]
                        $("#" + elementType).text(limit)
                    }
                });
            }

            function updateDatePicker(type) {
                if (type == "Daily View") {
                    $("#datepickerday").css('display', 'inline-block');
                    $("#datepickermonth").css('display', 'none');
                    $("#datepickerday").prop('disabled', false);
                } else if (type == "Monthly View") {
                    $("#datepickerday").css('display', 'none');
                    $("#datepickermonth").css('display', 'inline-block');
                    $("#datepickermonth").prop('disabled', false);
                } else if (type == "Last 7 days") {
                    //$("#datepickerday").css('display', 'inline-block');
                    //$("#datepickermonth").css('display', 'none');
                    $("#datepickerday").prop('disabled', true);
                    $("#datepickermonth").prop('disabled', true);
                } else if (type == "Last 30 days") {
                    //$("#datepickerday").css('display', 'inline-block');
                    //$("#datepickermonth").css('display', 'none');
                    $("#datepickerday").prop('disabled', true);
                    $("#datepickermonth").prop('disabled', true);
                }
                updateData($("#userselect").val())
            }
        </script>
    </head>
    <body>
        <p> 
            <b>Username</b>
            <select id="userselect" onchange="updateData($(this).val())">
                <?php
                foreach ($users as $user) {
                    $selected = $user == $_SESSION["username"] ? "selected=\"selected\"" : "";
                    echo "<option " . $selected . " value=\"" . $user . "\">" . $user . "</option>";
                }
                ?>
            </select>&nbsp;
            <!--<b>Username:</b> <?php echo $_SESSION["username"]; ?>&nbsp;-->
            <b>Role:</b> <span id="Role"><?php echo $role; ?></span>&nbsp;
            <b>Level:</b> <span id="Level"><?php echo $level; ?></span>
            <input type="text" id="datepickerday" placeholder="YYYY-MM-DD" class="date-input">
            <input type="text" id="datepickermonth" placeholder="YYYY-MM" class="date-input">
            <span id="details"><button class="large button"><a href="graph-user.php?db=iot&table=data&username=<?php echo $_SESSION["username"]; ?>&role=<?php echo $role; ?>&level=<?php echo $level; ?>">details</a></button></span>
            <!--<button class="large button"><a href="user.php?time=<?php /* echo $_REQUEST["time"] == "day" ? "month" : "day"; ?>"><?php echo $_REQUEST["time"] == "day" ? "month" : "dai"; */ ?>ly view</a></button>-->
            <select id="timeselect" onchange="updateDatePicker($(this).val())">
                <option selected="selected">Daily View</option>
                <option>Monthly View</option>
                <option>Last 7 days</option>
                <option>Last 30 days</option>
            </select>
        </p>
        <p>
            <b>User's Limits: </b>
            <?php
            $keys = array("DashboardID" => "Dashboards", "AppID" => "IoT Apps", "IOTID" => "IoT Devices");
            $i = 0;
            foreach ($limits as $limit) {
                echo ($i != 0 ? ", " : "" ) . $keys[$limit["elementType"]] . ": " . "<span id=\"" . $limit["elementType"] . "\">" . $limit["limit"] . "</span>";
                $i++;
            }
            ?>
        </p>
        <div style="display: grid; grid-template-columns: auto auto auto; padding: 10px; margin: 10px;">
            <pre># Dashboards (public/private)</b><h1><span id="dashboards"></span></h1></pre>
            <pre># Dashboards Accesses</b><h1><span id="dashboardsAccesses"></span></h1></pre>
            <pre># Dashboards Minutes</b><h1><span id="dashboardsMinutes"></span></h1></pre>
            <pre># IoT devices (public/private)</b><h1><span id="IoTDevices"></span></h1></pre>
            <pre>IoT kB (Tx/Rx)</b><h1><span id="IoTTxRx"></span></h1></pre>
            <pre># IoT Applications</b><h1><span id="IoTApplications"></span></h1></pre>
            <pre>ETL kB</b><h1><span id="ETL"></span></h1></pre>
            <!--<pre># AMMA</b><h1><span id="AMMA"></span></h1></pre>-->
            <!--<pre># DevDash</b><h1><span id="DevDash"></span></h1></pre>-->
            <!--<pre># ResDash</b><h1><span id="ResDash"></span></h1></pre>-->
            <!--<pre># IoTBlocks</b><h1><span id="IoTBlocks"></span></h1></pre>-->
            <!--<pre># Microservice</b><h1><span id="MicroService"></span></h1></pre>-->
            <!--<pre># IoTApp</b><h1><span id="IoTApp"></span></h1></pre>-->
            <pre>R kB (Tx/Rx)</b><h1><span id="RTxRx"></span></h1></pre>
        </div>
    </body>
</html>

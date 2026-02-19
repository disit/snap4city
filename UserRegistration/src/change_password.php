<?php
/* eShare in a Snap.
  Copyright (C) 2024 SNAP4 S.R.L. https://www.snap4.eu

  All right reserved */
require_once './vendor/phpmailer/phpmailer/src/PHPMailer.php';
require_once './vendor/phpmailer/phpmailer/src/SMTP.php';
require_once './vendor/phpmailer/phpmailer/src/Exception.php';
error_reporting(E_ERROR | E_PARSE);
function verySimpleDecoding($string) {
    return base64_decode($string);
}


if(isset($_GET['token'])) {
    $servername = getenv("DB_HOST") ?? "db_host";
    $username = getenv("DB_USER") ?? "db_user";
    $password = getenv("DB_PASSWORD") ?? "db_password";
    $dbname = getenv("DB_NAME") ?? "db_name";

    try{
        $conn = new PDO("mysql:host=$servername;dbname=$dbname", $username, $password);
        $conn->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
        $stmt = $conn->prepare(
            "SELECT mail, password_reset.token, user, timeofreset 
            FROM password_reset join users on password_reset.user=users.userid 
            where (password_reset.token=:v2)
            union all
            SELECT mail, password_reset.token, user, timeofreset 
            FROM password_reset join operator_users on password_reset.user=operator_users.userid 
            where (password_reset.token=:v2)  
            order by timeofreset desc limit 1;"
        );
        $stmt->bindParam(":v2", $_GET['token']);
        $stmt->execute();
        $row = $stmt->fetch(PDO::FETCH_ASSOC);
        if ($row) {
            $now = new DateTime();
            $target = new DateTime($row["timeofreset"]);
            $diffInSeconds = $now->getTimestamp() - $target->getTimestamp(); 
            $totalMinutes = floor($diffInSeconds / 60);
            $maxTime = (int)getenv("RECOVER_LINK_DURATION_IN_MIN") ?: 15;
            if ($totalMinutes <= $maxTime) {
                $things_to_pass = [$row["token"],$row["user"]];
                outputHTML($errors=null, $code = 200, $params=$things_to_pass);
            }
            else {
                $stmt = $conn->prepare("DELETE FROM `user_login`.`password_reset` WHERE (password_reset.user=:v2)");
                $stmt->bindParam(':v2', $userid);
                $stmt->execute();
                $rowCount = $stmt->rowCount();
                if ($rowCount > 0) {
                    outputHTML($errors="Link on mail is expired, please start the procedure again", $error=400);
                } else {
                    error_log("Error trying to remove an user ($userid): user not exists");
                    outputHTML($errors="Something went wrong. Please contact support if the problem persist", $error=500);
                }
            }
        } else {
            outputHTML($errors="User does not exist",$code=409);
        }
    }catch(PDOException $e){
        error_log("Database error: ". $e->getMessage());
        outputHTML($errors="Something went wrong. Please contact support if the problem persist", $code=500);
    }
}

function outputHTML($errors=null, $code=500, $params=null) {
    http_response_code($code);
    //read mail config file
    $templatesFolder = getenv('TEMPLATES_FOLDER') ?: "";
    $config = [
        "error_template" => $templatesFolder . "/error_template.html",
        "form_template" =>  $templatesFolder . "/recover_password_form_template.html",
    ];
    if ($errors) {
        try{
            $html = get_template($config["error_template"]);
        }catch(Exception $ex){
            $html = errorHTML();
        }
        $html = str_replace("[[Error text]]", $errors, $html);
        echo $html;
        
    }
    elseif ($params) {
        try{
            $html = get_template($config["form_template"]);
        }catch(Exception $ex){
            $html = formHTML();
        }
        $html=str_replace("[[token]]", $params[0], $html);
        $html=str_replace("[[user]]", $params[1], $html);
        echo $html;
    }
    exit();
}

function errorHTML() {
    return <<<STRING
    <!DOCTYPE html>
    <html lang="en">
    <head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Error!</title>
    <!-- Bootstrap CSS -->
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet" integrity="sha384-QWTKZyjpPEjISv5WaRU9OFeRpok6YctnYmDr5pNlyT2bRjXh0JMhjY6hW+ALEwIH" crossorigin="anonymous">
    </head>
    <body>
    <div class="container mt-5" style="text-align: center;">
    <div class="row">
    <div class="col-md-12">
    <div class="error-container">
    <h1 class="error-heading text-danger" style="font-size: 40px;">An error occurred!</h1>
    <p class="error-message" style="font-size: 24px;">[[Error text]]</p>
    </div>
    </div>
    </div>
    </div>
    </body>
    </html>
    STRING;
}

function formHTML() {
    return <<<STRING
    <!DOCTYPE html>
    <html lang="en">
    <head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Change your password</title>
    <!-- Include Bootstrap CSS -->
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet" integrity="sha384-QWTKZyjpPEjISv5WaRU9OFeRpok6YctnYmDr5pNlyT2bRjXh0JMhjY6hW+ALEwIH" crossorigin="anonymous">
    <script>
        function checkForm(event) {
            var pattern = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])[A-Za-z\d@$!%*?&]{12,}$/;
            var string = document.getElementById("password").value;
            var match = document.getElementById("confirm_password").value;
            if(string == ''){
                alert("Password is empty.");
                event.preventDefault(); // Prevent form submission
            }
            if(match == ''){
                alert("Confirm password is empty.");
                event.preventDefault(); // Prevent form submission
            }
            if (!pattern.test(string)) {
                alert("Password wasn't strong enough.");
                event.preventDefault(); // Prevent form submission
            }
            if (string !== match) {
                alert("Password confirmation failed. Please ensure both passwords are identical");
                event.preventDefault(); // Prevent form submission
                return;
            }
        }
    </script>
    </head>
    <body>
    <div class="container">
    <h2>Reset your password</h2>
    <form action="edit_password_field.php" method="POST" onsubmit="checkForm(event)">
    <div class="form-group">
    <label for="password">Password:</label>
    <input type="password" class="form-control" id="password" name="password" required>
    <small id="passwordHelp" class="form-text text-muted">Your password must be at least 12 characters long and include at least one uppercase letter, one lowercase letter, one number, and one special character @$!%*?&</small>
    </div>
    <div class="form-group">
    <label for="confirm_password">Confirm Password:</label>
    <input type="password" class="form-control" id="confirm_password" name="confirm_password" required>
    </div>
    <input type="hidden" id="token" name="token" required value="[[token]]">
    <input type="hidden" id="user" name="user" required value="[[user]]">
    <button type="submit" class="btn btn-primary  mt-3">Change password</button>
    </form>
    </div>
    </body>
    </html>
    STRING;
}

function get_template($templateFilename) {
    $htmlBody = file_get_contents($templateFilename);
    if ($htmlBody === false) {
        throw new Exception('Could not read HTML file.');
    }
    return $htmlBody;
}
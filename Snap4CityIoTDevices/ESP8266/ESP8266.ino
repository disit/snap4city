/* IOTDEVICE-ESP8266.
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

#include <ESP8266HTTPClient.h>
#include <WiFiClientSecure.h>
#include <ESP8266WebServer.h>
#include <ESP8266mDNS.h>
#include <ESP8266WiFi.h>
#include <EEPROM.h>

#define ADD_NEW_CONNECTION 2
#define NO_CONNECTIONS 255
#define STANDARD 1
#define RED 82
#define GREEN 71
#define BLUE 66
#define WHITE 87
#define MINSIGNAL -240

// Needed to move heavy strings from heap to program memory
#define PROGMEM   ICACHE_RODATA_ATTR
#define ICACHE_RODATA_ATTR  __attribute__((section(".irom.text")))

// Max EEPROM size is 4096. Due to low memory constraints it's set to 2900
#define EEPROMSIZE 2900

// Flag used in sendMessage() to check the first connection and do the malloc of certificate and key.
bool firstConnection = true;

// State of the device
int state = NO_CONNECTIONS;

// Length of certificate and key. Global visibility needed.
int certificateLength;
int privateKeyLength;

// Certificate and key
unsigned char* certBuffer = 0;
unsigned char* keyBuffer = 0;

// Sensor data
float temp = 0;
float hum = 0;

const int maxConnections = 5;

// Arrays where SSIDs and PSWs are stored
String SSIDs[maxConnections];
String PSWs[maxConnections];

bool debugMode = true;

// Device info
String defaultDeviceTypeName = "Snap4CityArduino-cSEbD";
String deviceSWversion = "0.02";
String deviceFingerprint = "cSEbD-Dj3Ln-Asjdek-RSVnw-aGDSb";
String defaultType = "Ambiental";
const char *ssidAP = "cSEbD-Settings-Snap4CityArduino";
const char *passwordAP = "RSVnw-aGDSb";


// EEPROM pointers
int eepromStatusSavePosition = 0;
int brokerURISavePosition = 1;
int brokerPortSavePosition = 50;
int idSavePosition = 60;
int k1SavePosition = 110;
int k2SavePosition = 160;
int SHAfingerprintSavePosition = 210;
int deviceTypeSavePosition = 260;
int certificateSavePosition = 320;
int privateKeySavePosition = 1670;
int numberOfConnectionSavePosition = 2370;
int certificateLengthSavePosition = 2870;
int keyLengthSavePosition = 2871;

// Web server on port 80
ESP8266WebServer server(80);

void setup() {
  // Start EEPROM
  EEPROM.begin(EEPROMSIZE);
  // Start Serial
  Serial.begin(115200);
  randomSeed(analogRead(1));
  // Read device status  
  state = EEPROM.read(eepromStatusSavePosition);
  delay(1000);
  // Start AP with the following SSID and psw
  WiFi.softAP(ssidAP, passwordAP);
  delay(1000);
  
  if(state != NO_CONNECTIONS){
    WiFi.mode(WIFI_STA);
    // Tell Arduino that device is already configured
    Serial.println("C");
    if(EEPROM.read(numberOfConnectionSavePosition) == 0){
      state = NO_CONNECTIONS;
      EEPROM.write(eepromStatusSavePosition, NO_CONNECTIONS);
      EEPROM.commit();
    }
    else {
      state = ADD_NEW_CONNECTION;
      EEPROM.write(eepromStatusSavePosition, ADD_NEW_CONNECTION);
      EEPROM.commit();
    }
  } else {
    startBlink(WHITE);
  }

  // Associate different webserver's pages to hadlers
  server.on("/", handleRoot);
  server.on("/save", handleSave);
  server.on("/add", handleSaveNewConnection);
  server.on("/guide", handleGuide);
  server.on("/delete", handleDelete);
  server.on("/reset", handleReset);
  server.onNotFound(handleNotFound);
  server.begin();
}


void loop() {
  if(state == NO_CONNECTIONS || state == ADD_NEW_CONNECTION){
    server.handleClient();
    delay(100);
  }

  // Listen to serial for incoming data or commands
  if (Serial.available() > 0) {
    String incomingData = Serial.readStringUntil('\n');
    if(incomingData.length() > 4){
      temp = incomingData.substring(0, incomingData.indexOf(";")).toFloat();
      hum = incomingData.substring(incomingData.indexOf(";")+1, incomingData.indexOf("/n")).toFloat();
      sendMessage();
    } else {
      if(incomingData[0] == 'S')
        sendMessage();
      if(incomingData[0] == 'L')
        handleRoot();
    } 
  }  
}

void handleRoot() {
  startBlink(WHITE);
  WiFi.mode(WIFI_AP_STA);
  if(state == NO_CONNECTIONS){
    handleAddFirstConnection();
  } else {
    handleAddNewConnection();
  }
}

void handleAddFirstConnection(){
  byte macBytes[6];
    WiFi.macAddress(macBytes);
    String stringMAC =  "";
  
    // Translates MAC address in string
    macBytes[0] = macBytes[0] + 2;
    for (int i = 5; i >= 0; i--) {
      if (i != 5) {
        stringMAC = String(macBytes[i], HEX) + ":" + stringMAC;
      } else {
        stringMAC = String(macBytes[i], HEX);
      }
    }
    stringMAC.toUpperCase();
  
    // Find how many wifi are available
    int numberOfNetworks = WiFi.scanNetworks();
  
    // Configuration page html
    String content = "<html lang='en'><body><form action='/save' method='POST'><h1><strong>You are connected to <br><span style=\"color: #ff0000; \">" + defaultDeviceTypeName + "</span></strong></h1>"\
                     "<strong>MAC:  " + stringMAC + "</strong><br><strong>Device Fingerprint:  " + deviceFingerprint + "</strong><br><strong>SW version:  " + deviceSWversion + "</strong><br><hr>"\
                     "WiFi connections detected nearby. <br>Select the one you want to connect to<br>(or write its SSID below)<br><br><select id='list'><option id='null'>Show WiFi detected</option>";
    for(int i=0; i<numberOfNetworks; i++){
      content += "<option id='" + WiFi.SSID(i) + "'>" +  WiFi.SSID(i) +"</li> Signal: " + getSignal(WiFi.RSSI(i)) + "</span></option>";
    }
    
    const static char HTMLContent[] PROGMEM = R"=====(</select><br><br>WiFi-SSID:<input type='text' name='SSID' id='SSID' placeholder='ssid'><br><br>WiFi-PSW :<input type='password' name='PSW' placeholder='password'><br><hr>Device Type:<input type='text' name='DEVICETYPE' placeholder='leave empty for default'><br><br>IOT Device ID:<input type='text' name='ID' placeholder='id'><br><hr>Service Broker URI:<input type='text' name='BROKERURI' placeholder='broker URI'><br><br>Broker URI Port: <input type='text' name='BROKERPORT' placeholder='broker port'><br><br>SHA thumbprint:<input type='text' name='THUMBPRINT' placeholder='needed only for https'><hr>Select security level: <input id='k1k2' type='radio' name='security' value='k1k2'> K1, K2<input id='certkey' type='radio' name='security' value='mutual'>Certificate & Key <br><br><div id='unsecure'>K1:<input type='text' name='K1' placeholder='k1'><br><br>K2:<input type='text' name='K2' placeholder='k2'><br></div><div id='secure'>Certificate:<input type='file' id='UPLOADCERT'/><input type='text' name='CERTIFICATE' style='width:0px;height:0px;border:none;'><br><br>Private Key:<input type='file' id='UPLOADKEY'/><input type='text' name='PRIVATEKEY' style='width:0px;height:0px;border:none;'></div><br><hr><br><input type='submit' name='SAVE' value='SAVE'></form><br><a href='/guide'>Guide</a><script>document.getElementById('secure').hidden = true;document.getElementById('unsecure').hidden = true;document.getElementById('k1k2').addEventListener('click', function(){document.getElementById('secure').hidden = true;document.getElementById('unsecure').hidden = false;});document.getElementById('certkey').addEventListener('click', function(){document.getElementById('unsecure').hidden = true;document.getElementById('secure').hidden = false;});document.getElementById('list').addEventListener('change', function(){var w = list.options[list.options["selectedIndex"]].id;if(w != 'null')document.getElementsByName('SSID')[0].value = w;});var c = document.getElementsByName('CERTIFICATE')[0]; var k = document.getElementsByName('PRIVATEKEY')[0];c.value = 0;k.value = 0;var inputCertificate = document.getElementById('UPLOADCERT');var inputPrivateKey = document.getElementById('UPLOADKEY');inputCertificate.addEventListener('change', function(e){var certReader =new FileReader();certReader.onload = function(){var cert = certReader.result;cert = cert.substring(cert.indexOf('-----'));if(cert.length > 1800 || !cert.includes("-----BEGIN CERTIFICATE")){alert("CERTIFICATE NOT VALID");c.value = 1;return;}cert = cert.replace('-----BEGIN CERTIFICATE-----', '');cert = cert.replace('-----END CERTIFICATE-----', '');cert = cert.replace(/(\r\n\t|\n|\r\t)/gm,"");c.value = cert;};certReader.readAsText(inputCertificate.files[0]);}, false);inputPrivateKey.addEventListener('change',function(e){var keyReader = new FileReader();keyReader.onload = function(){var key = keyReader.result;if(key.length > 930 || !key.includes("-----BEGIN PRIVATE KEY")){alert("KEY NOT VALID");k.value = 1;return;}key = key.replace('-----BEGIN PRIVATE KEY-----', '');key = key.replace('-----END PRIVATE KEY-----', '');key = key.replace(/(\r\n\t|\n|\r\t)/gm,"");k.value = key;};keyReader.readAsText(inputPrivateKey.files[0]);}, false); </script>)=====";

    server.setContentLength(content.length() + sizeof(HTMLContent));
    server.send(200, "text/html", content);
    server.sendContent(HTMLContent);
}

String getSignal(int rssi){
  if(rssi >= -50)
    return "Excellent";
  else if(rssi < -50 && rssi >= -60)
    return "Good";
  else if(rssi < -60 && rssi >= -70)
    return "Fair";
  else return "Poor";
}

void handleSave() {
  if (server.hasArg("SSID") && server.hasArg("PSW")) {

    if (server.arg("SSID") == "" ||  server.arg("PSW") == "" ||  server.arg("BROKERURI") == "" ||  server.arg("ID") == "" 
        || server.arg("SSID").length() < 2 || server.arg("SSID").length() > 40
        || server.arg("PSW").length() < 2 || server.arg("PSW").length() > 40
        || server.arg("ID").length() < 2 || server.arg("ID").length() > 40
        || server.arg("BROKERURI").length() < 2 || server.arg("BROKERURI").length() > 190
        || server.arg("BROKERPORT").length() < 1 || server.arg("BROKERPORT").length() > 5
        || (server.arg("K1").length() < 2 && server.arg("CERTIFICATE") == "0")|| server.arg("K1").length() > 40
        || (server.arg("K2").length() < 2 && server.arg("CERTIFICATE") == "0") || server.arg("K2").length() > 40
        || server.arg("CERTIFICATE") == "1" || server.arg("PRIVATEKEY") == "1" || (server.arg("CERTIFICATE") == "0" && server.arg("PRIVATEKEY") != "0")
        || (server.arg("CERTIFICATE") != "0" && server.arg("PRIVATEKEY") == "0")
        
       ) {

      server.send(200, "text/html", "<h1>Invalid data!</h1><br><a href='/'>Back</a>");
      delay(300);
      return;
    } else {

      String bucket = "";

      bucket = server.arg("SSID");
      char ssidChars[bucket.length() + 1];
      (bucket).toCharArray(ssidChars, bucket.length() + 1);
      for (int i = 0; i < bucket.length(); i++) {
        EEPROM.write(numberOfConnectionSavePosition + 1 + i, ssidChars[i]);
        yield();
      }

      bucket = server.arg("PSW");
      char pswChars[bucket.length() + 1];
      (bucket).toCharArray(pswChars, bucket.length() + 1);
      for (int i = 0; i < bucket.length(); i++) {
        EEPROM.write(numberOfConnectionSavePosition + 50 + i, pswChars[i]);
        yield();
      }

      bucket = server.arg("ID");
      char idChars[bucket.length() + 1];
      (bucket).toCharArray(idChars, bucket.length() + 1);
      for (int i = 0; i < bucket.length(); i++) {
        EEPROM.write(idSavePosition + i, idChars[i]);
        yield();
      }

      bucket = server.arg("BROKERURI");
      if (bucket.endsWith("/")) {
        bucket = bucket.substring(0, bucket.length()-1);
      }
      char brokerURIChars[bucket.length() + 1];
      (bucket).toCharArray(brokerURIChars, bucket.length() + 1);
      for (int i = 0; i < bucket.length(); i++) {
        EEPROM.write(brokerURISavePosition + i, brokerURIChars[i]);
        yield();
      }

      bucket = server.arg("BROKERPORT");
      char portChars[bucket.length() + 1];
      (bucket).toCharArray(portChars, bucket.length() + 1);
      for (int i = 0; i < bucket.length(); i++) {
        EEPROM.write(brokerPortSavePosition + i, portChars[i]);
        yield();
      }

      bucket = server.arg("K1");
      char k1Chars[bucket.length() + 1];
      (bucket).toCharArray(k1Chars, bucket.length() + 1);
      for (int i = 0; i < bucket.length(); i++) {
        EEPROM.write(k1SavePosition + i, k1Chars[i]);
      }

      bucket = server.arg("K2");
      char k2Chars[bucket.length() + 1];
      (bucket).toCharArray(k2Chars, bucket.length() + 1);
      for (int i = 0; i < bucket.length(); i++) {
        EEPROM.write(k2SavePosition + i, k2Chars[i]);
      }

      bucket = server.arg("THUMBPRINT");
      bucket.replace(":", "");
      bucket.replace(" ", "");
      char SHA1fingeprintChars[bucket.length() + 1];
      (bucket).toCharArray(SHA1fingeprintChars, bucket.length() + 1);
      for (int i = 0; i < bucket.length(); i++) {
        EEPROM.write(SHAfingerprintSavePosition + i, SHA1fingeprintChars[i]);
      }

      bucket = server.arg("DEVICETYPE");
      if (bucket.length() < 2) {
        bucket = defaultType;
      }
      char deviceTypeChars[bucket.length() + 1];
      (bucket).toCharArray(deviceTypeChars, bucket.length() + 1);
      for (int i = 0; i < bucket.length(); i++) {
        EEPROM.write(deviceTypeSavePosition + i, deviceTypeChars[i]);
      }

      // Decode base64 and save to EEPROM
      bucket = server.arg("CERTIFICATE");
      int i = 6;
      uint8_t c;
      char a, b;
      int j = 0;
      int p = 0;

      while (j < bucket.length() && isbase64(bucket[j])) {
        b = decode(bucket[j]);
        if ((i += 2) == 8) {
          i -= 8;
        } else {
          c = (a << i) | (b >> (6-i));
        }
      a = b;
      if(j%4!=0){
        EEPROM.write(certificateSavePosition + p, int(c));
        p++;
      }
      j++;
      yield();
    }


      bucket = server.arg("PRIVATEKEY");
      i = 6;
      j = 0;
      p = 0;

      while (j < bucket.length() && isbase64(bucket[j])) {
        b = decode(bucket[j]);
        if ((i += 2) == 8) {
          i -= 8;
        } else {
          c = (a << i) | (b >> (6-i));
        }
      a = b;
      if(j%4!=0 && j>34){
        EEPROM.write(privateKeySavePosition + p, int(c));
        p++;
      }
      j++;
      yield();
    }

     
      EEPROM.write(eepromStatusSavePosition, 1);
      EEPROM.write(numberOfConnectionSavePosition, 1);
      EEPROM.commit();

      if (debugMode) {
        getSavedSSIDs();
        String savedSSID = "\"" + SSIDs[0] + "\"";
        getSavedPSWs();
        String savedPSW = "\"" + PSWs[0] + "\"";
        String savedDeviceTypeName = "\"" + getSavedDeviceType() + "\"";
        String savedid = "\"" + getSavedID() + "\"";
        String savedbrokerURI = "\"" + getSavedBrokerURI() + "\"";
        String savedk1 = "\"" + getSavedK1() + "\"";
        String savedk2 = "\"" + getSavedK2() + "\"";
        String savedSHAthumbprint = "\"" + getSavedSHAthumbprint() + "\"";
        server.send(200, "text/html", "<h1>WiFi credentials saved!</h1><br>saved SSID:" + savedSSID + "<br>saved PASSWORD:" + savedPSW + "<br>saved ID:" + savedid + "<br>saved BrokerURI:" + savedbrokerURI + "<br>saved SHA fingerprint:" + savedSHAthumbprint + "<br>saved savedDeviceType:" + savedDeviceTypeName + "<br><h2>Device ready for normal use, the device will turn off by itself</h2>");
      } else {
        server.send(200, "text/html", "<h1>WiFi credentials saved!</h1><br><h2>Device ready for normal use, the device will turn off by itself</h2>");
      }


      delay(200);
      
      endBlink(WHITE);

      connectToWiFi();

      state = ADD_NEW_CONNECTION;
      EEPROM.write(eepromStatusSavePosition, ADD_NEW_CONNECTION);
      EEPROM.commit();

      return;
    }
  }
}

void handleReset(){
  for (int i = 0; i < EEPROMSIZE; i++) {
    EEPROM.write(i, 255);
  }
  EEPROM.write(numberOfConnectionSavePosition, 0);
  EEPROM.commit();
  state = NO_CONNECTIONS;
  EEPROM.write(eepromStatusSavePosition, NO_CONNECTIONS);
  EEPROM.commit();
  String content = "<html><body>Reset Completed</body></html>";
  server.send(200, "text/html", content);
  WiFi.disconnect();
  free(certBuffer);
  free(keyBuffer);
  firstConnection = true;
  endBlink(WHITE);
}

void handleGuide() {
  String content = "<html><body>This is a guide page<br><a href='/'>Back</a></body></html>";
  server.send(200, "text/html", content);
}

void handleNotFound() {
  String message = "File Not Found\n\n";
  message += "URI: ";
  message += server.uri();
  message += "\nMethod: ";
  message += (server.method() == HTTP_GET) ? "GET" : "POST";
  message += "\nArguments: ";
  message += server.args();
  message += "\n";
  for (uint8_t i = 0; i < server.args(); i++) {
    message += " " + server.argName(i) + ": " + server.arg(i) + "\n";
  }
  server.send(404, "text/plain", message);
}

void handleAddNewConnection(){
  int connections = EEPROM.read(numberOfConnectionSavePosition);
  byte macBytes[6];
  WiFi.macAddress(macBytes);
  String stringMAC =  "";

  // Traduce il MAC Address in stringa
  macBytes[0] = macBytes[0] + 2;
  for (int i = 5; i >= 0; i--) {
    if (i != 5) {
      stringMAC = String(macBytes[i], HEX) + ":" + stringMAC;
    } else {
      stringMAC = String(macBytes[i], HEX);
    }
  }
  stringMAC.toUpperCase();
  
  int numberOfNetworks = WiFi.scanNetworks();
  int savedConnections = EEPROM.read(numberOfConnectionSavePosition);
  
  String content = "<html><body><h1><strong>You are connected to <br> <span style=\"color: #ff0000; \">" + defaultDeviceTypeName + "</span></strong></h1>";
  content += "<strong>MAC:  " + stringMAC + "</strong><br>";
  content += "<strong>Device Fingerprint:  " + deviceFingerprint + "</strong><br>";
  content += "<strong>SW version:  " + deviceSWversion + "</strong><br>";
  content += "<strong>Broker URI:  " + getSavedBrokerURI() + "</strong><br>";
  content += "<strong>ID:  " + getSavedID() + "</strong><br>";
  content += "<strong>Device type:  " + getSavedDeviceType() + "</strong><br>";

  content += "Saved connections: <br><form action='/delete' method='POST'><ul id='savedNetworks' style='list-style: none;'>";
  for(int i=0; i<savedConnections; i++){
    getSavedSSIDs();
    int index = i + 1;
    if(i==0){ // Solo il primo è checked
      content += "<li><input id='" + String(i) + "' type='radio' class='radio' name='networks' checked>" + String(SSIDs[i]) + "<span style='margin-right:10px;'>[" + String(index) + "/" + String(maxConnections) + "]</span></li>";
    }else {
      content += "<li><input id='" + String(i) + "' type='radio' class='radio' name='networks'>" + String(SSIDs[i]) + "<span style='margin-right:10px;'>[" + String(index) + "/" + String(maxConnections) + "]</span></li>";
    }
  }
  content += "</ul><input id='INDEX' name='INDEX' type='text' style='width:0; height:0; border:none;'>";
  content += "<input type='submit' name='DELETEWIFI' value='Delete'></form>";
  

  if(connections != maxConnections){
      content += "WiFi connections detected nearby.<br> Select the one you want to connect to<br>(or write its SSID below)<br><select id='list'><option id='null'>Show WiFi detected</option>";
      for(int i=0; i<numberOfNetworks; i++){
        content += "<option id='" + WiFi.SSID(i) +"'>" +  WiFi.SSID(i) +" Signal: " + getSignal(WiFi.RSSI(i)) + "</option>";
      }
      content += "</select>";

      content += "<form action='/add' method='POST'>";
      content += "<br><br>WiFi-SSID:<input type='text' name='SSID' id='SSID' placeholder='ssid'><br><br>";
      content += "WiFi-PSW :<input type='password' name='PSW' placeholder='password'><br><br>";
      content += "<input type='submit' name='SAVE' value='SAVE'></form>";
  } else {
    content += "Reached max number of saved connections. Please delete a saved connection to add a new wifi setting <br><br>";
  }


  content += "<br><br><button id='resetButton'>Reset</button><br><br>";



  content += "<a href='/guide'>Guide</a><script>if(screen.width<800){for(var i=0; i<document.getElementsByClassName('radio').length; i++){var r = document.getElementsByClassName('radio')[i]; r.style.width='30px'; r.style.height='30px';}};document.getElementById('INDEX').value=0;document.getElementById('list').addEventListener('change', function(){var w = list.options[list.options['selectedIndex']].id;if(w != 'null')document.getElementsByName('SSID')[0].value = w;});";
  content += "for(i=0; i<" + String(savedConnections) + "; i++){document.getElementById(i.toString()).addEventListener('click', function(){document.getElementById('INDEX').value = this.id})}document.getElementById('resetButton').addEventListener('click', function(){var check = confirm('Are you sure to reset?');if(check)location.href = 'http://192.168.4.1/reset';});";
  content += "</script></body></html>";
  server.send(200, "text/html", content);
}

void handleSaveNewConnection(){
  if (server.hasArg("SSID") && server.hasArg("PSW")) {

    if (server.arg("SSID") == "" ||  server.arg("PSW") == "" || server.arg("SSID").length() < 2 || server.arg("SSID").length() > 40 || server.arg("PSW").length() < 2 || server.arg("PSW").length() > 40) {

      server.send(200, "text/html", "<h1>Invalid data!</h1><br><a href='/'>Back</a>");


      delay(100);

      delay(100);

      delay(100);


      return;
    } else {

      String bucket = "";
      int connections = EEPROM.read(numberOfConnectionSavePosition);
      int offset;
      if(connections == 0)
        offset = 1;
      else offset = connections*100;
      bucket = server.arg("SSID");
      char ssidChars[bucket.length() + 1];
      (bucket).toCharArray(ssidChars, bucket.length() + 1);
      for (int i = 0; i < bucket.length(); i++) {
        EEPROM.write(numberOfConnectionSavePosition + offset + i, ssidChars[i]);
      }

      bucket = server.arg("PSW");
      char pswChars[bucket.length() + 1];
      (bucket).toCharArray(pswChars, bucket.length() + 1);
      for (int i = 0; i < bucket.length(); i++) {
        if(connections == 0)
          offset = 0;
        EEPROM.write(numberOfConnectionSavePosition + offset + 50 + i, pswChars[i]);
      }

      EEPROM.write(eepromStatusSavePosition, 1);
      EEPROM.write(numberOfConnectionSavePosition, connections+1);
      EEPROM.commit();

      if (debugMode) {
        getSavedSSIDs();
        String savedSSID = "\"" + SSIDs[connections] + "\"";
        getSavedPSWs();
        String savedPSW = "\"" + PSWs[connections] + "\"";
        server.send(200, "text/html", "<h1>WiFi credentials saved!</h1><br>saved SSID:" + savedSSID + "<br>saved PASSWORD:" + savedPSW + "<br><h2>Device ready for normal use, the device will turn off by itself</h2>");
      } else {
        server.send(200, "text/html", "<h1>WiFi credentials saved!</h1><br><h2>Device ready for normal use, the device will turn off by itself</h2>");
      }

      delay(100);

      endBlink(WHITE);

      connectToWiFi();

      return;
    }
  }
}

void handleDelete(){
  int index = server.arg("INDEX").toInt();
  String deletedName = SSIDs[index];
  int connections = EEPROM.read(numberOfConnectionSavePosition);

  // Ho solo una connessione salvata, semplicemente la cancello
  if(connections == 1){
    for(int i=1; i<49; i++){
      EEPROM.write(numberOfConnectionSavePosition + i, 255);
    }
    for(int i=50; i<99; i++){
      EEPROM.write(numberOfConnectionSavePosition + i, 255);

    state = NO_CONNECTIONS;
    EEPROM.write(eepromStatusSavePosition, NO_CONNECTIONS);
    EEPROM.commit();
    }
  } else if (connections != 1 && connections != index + 1) { // Se ho più di una connessione cancello quella all'index e la sostituisco con l'ultima
    int offset;
    if(index == 0)
      offset=1;
    else offset = 100*index;
    for(int i=0; i<49; i++){
      EEPROM.write(numberOfConnectionSavePosition + offset + i, EEPROM.read(numberOfConnectionSavePosition + 100*(connections-1) + i));
      EEPROM.write(numberOfConnectionSavePosition + 100*(connections-1) + i, 255);
    }
    for(int i=50; i<99; i++){
      if(index==0)
        offset=0;
      EEPROM.write(numberOfConnectionSavePosition + offset + i, EEPROM.read(numberOfConnectionSavePosition + 100*(connections-1) + i));
    }
  } else { // Se devo cancellare l'ultima connessione semplicemente la cancello
    int offset = 100*(connections-1);
    for(int i=0; i<49; i++){
      EEPROM.write(numberOfConnectionSavePosition + offset + i, 255);
      EEPROM.write(numberOfConnectionSavePosition + offset + 50 + i, 255);
    }
  }

  EEPROM.write(numberOfConnectionSavePosition, connections-1);
  EEPROM.commit();
  String content = "<html><body>" + deletedName +" deleted!</body></html>";
  server.send(200, "text/html", content);
  delay(500);
  endBlink(WHITE);
  state = ADD_NEW_CONNECTION;
  EEPROM.write(eepromStatusSavePosition, ADD_NEW_CONNECTION);
  EEPROM.commit();
  connectToWiFi();
}


void getSavedSSIDs(){
  int connections = EEPROM.read(numberOfConnectionSavePosition);
  int offset;
  for(int i=0; i<connections; i++){
    if(i == 0){
      offset = 1;
    }else{
      offset = i*100;
    }
    String savedSSID = "";
    int j = 0;
    do {
      savedSSID = savedSSID + char(EEPROM.read(numberOfConnectionSavePosition + offset + j));
      j++;
    } while (EEPROM.read(numberOfConnectionSavePosition + offset + j) != 255);
  SSIDs[i] = savedSSID;
  }
}

void getSavedPSWs(){
  int connections = EEPROM.read(numberOfConnectionSavePosition);
  int offset;
  for(int i=0; i<connections; i++){
    if(i == 0){
      offset = 50;
    }else{
      offset = i*100 + 50;
    }
    String savedPSW = "";
    int j = 0;
    do {
      savedPSW = savedPSW + char(EEPROM.read(numberOfConnectionSavePosition + offset + j));
      j++;
    } while (EEPROM.read(numberOfConnectionSavePosition + offset + j) != 255);
  PSWs[i] = savedPSW;
  }
}


String getSavedID() {
  String savedid = "";
  int i = 0;
  do {
    savedid = savedid + char(EEPROM.read(idSavePosition + i));
    i++;
  } while (EEPROM.read(idSavePosition + i) != 255);
  return savedid;
}

String getSavedBrokerURI() {
  String savedbrokerURI = "";
  int i = 0;
  do {
    savedbrokerURI = savedbrokerURI + char(EEPROM.read(brokerURISavePosition + i));
    i++;
  } while (EEPROM.read(brokerURISavePosition + i) != 255);
  return savedbrokerURI;
}

String getSavedPort(){
  String savedPort = "";
  int i = 0;
  do {
    savedPort = savedPort + char(EEPROM.read(brokerPortSavePosition + i));
    i++;
  } while (EEPROM.read(brokerPortSavePosition + i) != 255);
  return savedPort;
}

String getSavedK1() {
  String savedk1 = "";
  int i = 0;
  do {
    savedk1 = savedk1 + char(EEPROM.read(k1SavePosition + i));
    i++;
  } while (EEPROM.read(k1SavePosition + i) != 255);
  return savedk1;
}

String getSavedK2() {
  String savedk2 = "";
  int i = 0;
  do {
    savedk2 = savedk2 + char(EEPROM.read(k2SavePosition + i));
    i++;
  } while (EEPROM.read(k2SavePosition + i) != 255);
  return savedk2;
}

String getSavedSHAthumbprint() {
  String savedSHAfingerprint = "";
  int i = 0;
  do {
    savedSHAfingerprint = savedSHAfingerprint + char(EEPROM.read(SHAfingerprintSavePosition + i));
    i++;
  } while (EEPROM.read(SHAfingerprintSavePosition + i) != 255);
  if (savedSHAfingerprint.length() < 5) {
    savedSHAfingerprint = "";
  }
  return savedSHAfingerprint;
}

String getSavedDeviceType() {
  String savedDeviceType = "";
  int i = 0;
  do {
    savedDeviceType = savedDeviceType + char(EEPROM.read(deviceTypeSavePosition + i));
    i++;
  } while (EEPROM.read(deviceTypeSavePosition + i) != 255);
  return savedDeviceType;
}

int getCertificateLength(){
  int l = 1279;
  do{
    l++;
  } while(EEPROM.read(certificateSavePosition+l) != 255);
  return l;
}

int getKeyLength(){
  int l = 607;
  do{
    l++;
  } while(EEPROM.read(privateKeySavePosition+l) != 255);
  return l;
}

int connectToWiFi(){

  if(state == NO_CONNECTIONS){
    Serial.println("C");
  }
  
  // Disconnect from previous wifi. 
  WiFi.disconnect();

  // Variables
  int RSSIs[maxConnections] = {MINSIGNAL, MINSIGNAL, MINSIGNAL, MINSIGNAL, MINSIGNAL};
  int availableConnections = 0;
  bool connectionEstablished = false;
  int savedConnections = EEPROM.read(numberOfConnectionSavePosition);
  int scannedConnections = WiFi.scanNetworks();

  // Update of saved SSIDs and PSWs
  getSavedSSIDs();
  getSavedPSWs();

  // No available connections 
  if(scannedConnections==0){
    startBlink(RED);
    delay(1000);
    endBlink(RED);
    return false;
  }
  
  // Find RSSIs
  for(int i=0; i<savedConnections; i++){
    for(int j=0; j<scannedConnections; j++){
      if(SSIDs[i] == WiFi.SSID(j)){
        RSSIs[i] = WiFi.RSSI(j);
        availableConnections++;
      }
    }
  }

  while(!connectionEstablished && availableConnections){
    int index = nextConnection(RSSIs, maxConnections);
    char ssid[SSIDs[index].length()+1];
    SSIDs[index].toCharArray(ssid, SSIDs[index].length()+1);
    char psw[PSWs[index].length()+1];
    PSWs[index].toCharArray(psw, PSWs[index].length()+1);
    connectionEstablished = connect(ssid, psw);
    availableConnections--;
  }

  if(connectionEstablished){
    startBlink(GREEN);
    delay(500);
    endBlink(GREEN);
    return true;
  }
  else{
    startBlink(RED);
    delay(1000);
    endBlink(RED);
    return false;
  } 
}

bool connect(char* ssid, char* psw){
  int attemptNumber = 0;
  WiFi.begin(ssid, psw);
  while(WiFi.status() != WL_CONNECTED && attemptNumber < 50){
    startBlink(BLUE);
    startBlink(GREEN);
    delay(200);
    endBlink(BLUE);
    endBlink(GREEN);
    attemptNumber++;
  }

  if (WiFi.status() == WL_CONNECTED) {
    return true;
  } else {
    delay(1000);
    return false;
  }
}

int nextConnection(int myRSSIs[], int size){  
  int max = -200;
  int index;
  for(int k=0; k<size; k++){
    if(myRSSIs[k] > max){
      max = myRSSIs[k];
      index = k;
    }
  }
  myRSSIs[index] = -240;
  return index;
}

void sendMessage(){

  // Close Access Point mode
  WiFi.mode(WIFI_STA);

  // Check if wifi is connected
  if(WiFi.status() != WL_CONNECTED){
    startBlink(RED);
    delay(1000);
    endBlink(RED);
    delay(1000);
    connectToWiFi();
    return;
  }

  startBlink(BLUE);

  bool secure = false;
  
  if(EEPROM.read(certificateSavePosition) != 255)
    secure = true;

  if(firstConnection && secure){
    certificateLength = getCertificateLength();
    privateKeyLength =  getKeyLength();
      
    certBuffer = (unsigned char*) malloc(certificateLength * sizeof(unsigned char));
    keyBuffer = (unsigned char*) malloc(privateKeyLength * sizeof(unsigned char));

    for(int i=0; i<certificateLength; i++){
     certBuffer[i] = EEPROM.read(certificateSavePosition+i);
     yield();
    }
    
    for(int i=0; i<privateKeyLength; i++){
      keyBuffer[i] = EEPROM.read(privateKeySavePosition+i);
      yield();
    }
    
    firstConnection = false;
  }
    

  WiFiClientSecure wifiSecure;

  if(secure){
    wifiSecure.setCertificate(certBuffer, certificateLength);
    wifiSecure.setPrivateKey(keyBuffer, privateKeyLength);
  }
  
  String host = getSavedBrokerURI().substring(8);
  char hostChars[host.length() + 1];
  (host).toCharArray(hostChars, host.length() + 1);

  if (!wifiSecure.connect(hostChars, 8080)) {
    endBlink(BLUE);
    startBlink(RED);
    delay(1000);
    endBlink(RED);
    return;
  }

  String thumbprint = getSavedSHAthumbprint();
  char thumbprintChars[thumbprint.length() + 1];
  (thumbprint).toCharArray(thumbprintChars, thumbprint.length() + 1);

  if (!wifiSecure.verify(thumbprintChars, hostChars)) {
    endBlink(BLUE);
    startBlink(RED);
    delay(500);
    endBlink(RED);
    return;
  }
  
  // Make a HTTPS request:
  String data = buildMessage();
  if(secure)
    wifiSecure.println(buildRequestSecure());
  else wifiSecure.println(buildRequestUnsecure());
  String selectedHost = "Host: " + host;
  wifiSecure.println(selectedHost);
  wifiSecure.println("Connection: close");
  wifiSecure.println("Content-Type: application/json;");
  wifiSecure.print("Content-Length: ");
  wifiSecure.println(data.length());
  wifiSecure.println();
  wifiSecure.println(data);
  delay(10);

  while (wifiSecure.connected()) {
  String line = wifiSecure.readStringUntil('\n');
  if (line == "\r") {
    break;
    }
  }

  int responseLength = 0;
  
  while (wifiSecure.available()) {
    char c = wifiSecure.read();
    responseLength++;
  }

  if(responseLength>200){
    endBlink(BLUE);
    startBlink(GREEN);
    delay(500);
    endBlink(GREEN);
  }
  else {
    endBlink(BLUE);
    startBlink(RED);
    delay(500);
    endBlink(RED);
  }

  wifiSecure.stop();
}

void checkEEPROM(){
  for(int i=0; i<EEPROMSIZE; i++){
    Serial.print("P: ");
    Serial.print(i);
    Serial.print(", X: ");
    Serial.println(EEPROM.read(i));
    yield();
  }
}

void startBlink(int code){
  switch(code){
    case RED:
      Serial.println(char(RED));
      break;
    case GREEN:
      Serial.println(char(GREEN));
      break;
    case BLUE:
      Serial.println(char(BLUE));
      break;
    case WHITE:
      Serial.println(char(WHITE));
      break; 
  }
}

void endBlink(int code){
  switch(code){
    case RED:
      Serial.println(char(RED+32));
      break;
    case GREEN:
      Serial.println(char(GREEN+32));
      break;
    case BLUE:
      Serial.println(char(BLUE+32));
      break;
    case WHITE:
      Serial.println(char(WHITE+32));
      break; 
  }
}

int decode(char c) {
  switch (c) {
    case 'A'...'Z':
      return c - 'A';

    case 'a'...'z':
      return c - 'a' + 26;

    case '0'...'9':
      return c - '0' + 52;

    case '+':
      return 62;

    case '/':
      return 63;
  }
}

bool isbase64(char c) {
  return (isalnum(c) || c == '+' || c == '/');
}

String buildRequestSecure(){
  String req = "POST ";
  req += getSavedBrokerURI();
  req += ":";
  req += getSavedPort();
  req += "/v1/updateContext?elementid=";
  req += getSavedID();
  req += " HTTP/1.1";
  return req;
}

String buildRequestUnsecure(){
  String req = "POST ";
  req += getSavedBrokerURI();
  req += ":";
  req += getSavedPort();
  req += "/v1/updateContext?elementid=";
  req += getSavedID();
  req += "&k1=";
  req += getSavedK1();
  req += "&k2=";
  req += getSavedK2();
  req += " HTTP/1.1";
  return req;
}

String buildMessage(){
  String msg = "{\"contextElements\": [{\"type\": \"";
  msg += getSavedDeviceType();
  msg += "\",\"isPattern\": \"false\",\"id\": \"";
  msg += getSavedID();
  msg += "\",\"attributes\": [{\"name\": \"temperature\",\"type\": \"float\",\"value\": \"";
  msg += temp;
  msg += "\"},{\"name\": \"humidity\",\"type\": \"float\",\"value\": \"";
  msg += hum;
  msg += "\"}]}],\"updateAction\": \"APPEND\"}";
  return msg;
}

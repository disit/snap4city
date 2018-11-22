/* IOTDEVICE-ESP32.
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

#include <NeoPixelAnimator.h>
#include <NeoPixelBrightnessBus.h>
#include <NeoPixelBus.h>

// <--------------------- Include --------------------->
#include <WiFiClientSecure.h>
#include <esp_deep_sleep.h>
#include <NeoPixelBus.h>
#include <WebServer.h>
#include <EEPROM.h>
#include <WiFi.h>

// <--------------------- Define --------------------->
#define BUTTON_PIN_BITMASK 0x300000000
#define BUTTON_1_PIN GPIO_NUM_32
#define BUTTON_2_PIN GPIO_NUM_33
#define colorSaturation 20
// Non volatile memory size. Max value: 4096.
#define EEPROMSIZE 3900
// WiFi minsignal
#define MINSIGNAL -240

// <------------------ Button states ------------------>
#define SHORTPRESS 1
#define LONGPRESS 3
#define DOUBLEPRESS 2
#define UNPRESSED 0

// <------------------- Loop states ------------------->
#define RESETTED 0
#define CONFIGAP 1
#define MODIFYSETTINGS 16
#define HANDLECLIENT 2
#define DEEPSLEEP 3
#define READBUTTONPRESS 4
#define LITTLE_SLEEP 13
#define CONFIRM 14
#define CHECK_SLEEP_TYPE 15

// <--------------- Variables that should be keepen in memory even if deepsleep occurs --------------->
RTC_DATA_ATTR int state = DEEPSLEEP;
RTC_DATA_ATTR int bootCount = 0;
// Used to check which confirm should be sent (R stands for right -> square, L stands for left -> round)
RTC_DATA_ATTR bool confSendR = false;
RTC_DATA_ATTR bool confSendL = false;

int explicitReset = 0;

const uint16_t PixelCount = 4; // This example assumes 4 pixels, making it smaller will cause a failure
const uint8_t PixelPin = 16;  // Make sure to set this to the correct pin, ignored for Esp8266

NeoPixelBus<NeoGrbFeature, Neo800KbpsMethod> strip(PixelCount, PixelPin);

// <---------------------- Led colors ---------------------->
RgbColor red(colorSaturation, 0, 0);
RgbColor green(0, colorSaturation, 0);
RgbColor blue(0, 0, colorSaturation);
RgbColor white(10);
RgbColor purple(8, 4, 22);
RgbColor fuxia(16, 0, 16);
RgbColor yellow(14, 14, 3);
RgbColor orange(23, 4, 4);
RgbColor aqua(3, 14, 14);
RgbColor black(0);

// <------------------------- Device properties ------------------------->
String defaultDeviceTypeName = "Snap4AllButtonV1-8e5f6";
String deviceSWversion = "0.2";
String deviceFingerprint = "8e5f6-94abc-6749a-rb52c-16a10";
const char *ssidAP = "8e5f6-Settings-Snap4AllButton";
const char *passwordAP = "rb52c-16a10";

// <---------------------- EEPROM pointers ----------------------->
int explicitResetSavePosition = 0;
int brokerURISavePosition = 1;
int brokerPortSavePosition = 50;
int idSavePosition = 60;
int k1SavePosition = 110;
int k2SavePosition = 160;
int SHAfingerprintSavePosition = 210;
int deviceTypeSavePosition = 260;
int certificateSavePosition = 320;
int privateKeySavePosition = 2220;
int numberOfConnectionSavePosition = 3180;

const int maxConnections = 5;

// Arrays where the WiFi settings are saved
String SSIDs[maxConnections];
String PSWs[maxConnections];

// Certificate and key. A malloc will be done later.
char* certBuffer = 0;
char* keyBuffer = 0;

bool debugMode = true;

// <---------------------- AP timers ----------------------->
const long timeout = 240000;
const long impulseInterval = 1000;
unsigned long currentImpulseTime = 0;
unsigned long previousImpulseTime = 0;
bool endImpulse = false;

// <------------------- Button detection timers -------------------->
unsigned long previousMillis = 0;
unsigned long currentMillis = 0;
const long interval = 150;

// <-------------------- Button detection variables --------------------->
int rightState = UNPRESSED;
int leftState = UNPRESSED;
int valuesRight[6] = {0,0,0,0,0,0};
int valuesLeft[6] = {0,0,0,0,0,0};
int samples = 0;
int beforeRight = 0;
int nextRight = 0;
bool downRight = false;
int beforeLeft = 0;
int nextLeft = 0;
bool downLeft = false;

bool connectedToWiFi = false;

// Webserver started on port 80 
WebServer server(80);

void setup(){
  // Start leds
  strip.Begin();
  strip.Show();

  Serial.begin(115200);

  // Start EEPROM memory
  EEPROM.begin(EEPROMSIZE);

  // Check last saved status
  explicitReset = EEPROM.read(explicitResetSavePosition);
  
  pinMode(17, OUTPUT);
  digitalWrite(17, HIGH);
}

void loop(){
  checkState();
}

void handleRoot(){
  // If explicitReset is 255 or 0 a reset has occured (factory reset or user reset)
  if(explicitReset == 255 || explicitReset == 0)
    handleAddFirstConnection();
  else handleAddNewConnection();
}

void handleAddFirstConnection(){
    
    // Set WiFi transmission power to max to perform good wifi scan
    WiFi.setTxPower(WIFI_POWER_19_5dBm);
  
    // Find how many wifi are available
    int numberOfNetworks = WiFi.scanNetworks();

    // Set WiFi transmission power to min to save energy
    WiFi.setTxPower(WIFI_POWER_5dBm);
  
    // HTML of configuration page
    String content = "<html lang='en'><body><form action='/save' method='POST'><h1><strong>You are connected to <br><span style=\"color: #ff0000; \">" + defaultDeviceTypeName + "</span></strong></h1>"\
                     "<strong>MAC:  " + WiFi.macAddress() + "</strong><br><strong>Device Fingerprint:  " + deviceFingerprint + "</strong><br><strong>SW version:  " + deviceSWversion + "</strong><br><hr>"\
                     "WiFi connections detected nearby. <br>Select the one you want to connect to<br>(or write its SSID below)<br><br><select id='list'><option id='null'>Show WiFi detected</option>";
    for(int i=0; i<numberOfNetworks; i++){
      content += "<option id='" + WiFi.SSID(i) + "'>" +  WiFi.SSID(i) +"</li> Signal: " + getSignal(WiFi.RSSI(i)) + "</span></option>";
    }
    
    content += "</select><br><br>WiFi-SSID:<input type='text' name='SSID' id='SSID' placeholder='ssid'><br>WiFi-PSW :<input type='password' name='PSW' placeholder='password'><br><hr>Device Type:<input type='text' name='DEVICETYPE' placeholder='leave empty for default'><br><br>IOT Device ID:<input type='text' name='ID' placeholder='id'><br><hr>Service Broker URI:<input type='text' name='BROKERURI' placeholder='broker URI'><br><br>Broker URI Port: <input type='text' name='BROKERPORT' placeholder='broker port'><br><br>SHA thumbprint:<input type='text' name='THUMBPRINT' placeholder='needed only for https'><hr>Select security level: <input id='k1k2' type='radio' name='security' value='k1k2'> K1, K2 <input id='certkey' type='radio' name='security' value='mutual'>Certificate & Key <br><br><div id='unsecure'>K1: <input type='text' name='K1' placeholder='k1'><br><br>K2:<input type='text' name='K2' placeholder='k2'><br></div><div id='secure'>Certificate:<input type='file' id='UPLOADCERT'/><textarea rows='10' cols='2000' name='CERTIFICATE' style='width:0px;height:0px;border:none;'></textarea><br><br>Private Key:<input type='file' id='UPLOADKEY'/><textarea rows='10' cols='2000' name='PRIVATEKEY' style='width:0px;height:0px;border:none;'></textarea></div><br><hr><br><input type='submit' name='SAVE' value='SAVE'></form><button onclick=\"location.href='http://192.168.4.1/cancel';\" >Cancel</button><br><br><a href='/guide'>Guide</a><script>document.getElementById('secure').hidden = true;document.getElementById('unsecure').hidden = true;document.getElementById('k1k2').addEventListener('click', function(){document.getElementById('secure').hidden = true;document.getElementById('unsecure').hidden = false;});document.getElementById('certkey').addEventListener('click', function(){document.getElementById('unsecure').hidden = true;document.getElementById('secure').hidden = false;});document.getElementById('list').addEventListener('change', function(){var w = list.options[list.options['selectedIndex']].id;if(w != 'null')document.getElementsByName('SSID')[0].value = w;});var c = document.getElementsByName('CERTIFICATE')[0]; var k = document.getElementsByName('PRIVATEKEY')[0];c.value = 0;k.value = 0;var inputCertificate = document.getElementById('UPLOADCERT');var inputPrivateKey = document.getElementById('UPLOADKEY');inputCertificate.addEventListener('change', function(e){var certReader =new FileReader();certReader.onload = function(){var cert = certReader.result;cert = cert.substring(cert.indexOf('-----'));if(cert.length > 1900 || !cert.includes('-----BEGIN CERTIFICATE')){alert('CERTIFICATE NOT VALID');c.value = 1;return;}c.value = cert;};certReader.readAsText(inputCertificate.files[0]);}, false);inputPrivateKey.addEventListener('change',function(e){var keyReader = new FileReader();keyReader.onload = function(){var key = keyReader.result;if(key.length > 930 || !key.includes('-----BEGIN PRIVATE KEY')){alert('KEY NOT VALID');k.value = 1;return;}k.value = key;};keyReader.readAsText(inputPrivateKey.files[0]);}, false); </script>";

    server.send(200, "text/html", content);
}

// Converts decibel values to strings
String getSignal(int rssi){
  if(rssi >= -50)
    return "Excellent";
  else if(rssi < -50 && rssi >= -60)
    return "Good";
  else if(rssi < -60 && rssi >= -70)
    return "Fair";
  else return "Poor";
}

// To write the EEPROM: EEPROM.write(position, value). To save changes: EEPROM.commit()
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
        bucket = defaultDeviceTypeName.substring(0, 16);
      }
      char deviceTypeChars[bucket.length() + 1];
      (bucket).toCharArray(deviceTypeChars, bucket.length() + 1);
      for (int i = 0; i < bucket.length(); i++) {
        EEPROM.write(deviceTypeSavePosition + i, deviceTypeChars[i]);
      }

      bucket = server.arg("CERTIFICATE");
      for(int i=0; i<bucket.length(); i++){
        EEPROM.write(certificateSavePosition + i, bucket[i]);
        yield();
      }

      bucket = server.arg("PRIVATEKEY");
      for(int i=0; i<bucket.length(); i++){
        EEPROM.write(privateKeySavePosition + i, bucket[i]);
        yield();
      }
      
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
        server.send(200, "text/html", "<h1>WiFi credentials saved!</h1><br>saved SSID:" + savedSSID + "<br>saved PASSWORD:" + savedPSW + "<br>saved ID:" + savedid + "<br>saved BrokerURI:" + savedbrokerURI + "<br>saved SHA fingerprint:" + savedSHAthumbprint + "<br>saved DeviceType:" + savedDeviceTypeName + "<br><h2>Device ready for normal use, the device will turn off by itself</h2>");
      } else {
        server.send(200, "text/html", "<h1>WiFi credentials saved!</h1><br><h2>Device ready for normal use, the device will turn off by itself</h2>");
      }
      EEPROM.write(explicitResetSavePosition, 1);
      EEPROM.commit();
      delay(1000);
      state = DEEPSLEEP;
      return;
    }
  }
}

void handleAddNewConnection(){

  // Find the number of saved networks  
  int connections = EEPROM.read(numberOfConnectionSavePosition);
  
  // Set WiFi transmission power to max to perform good wifi scan
  WiFi.setTxPower(WIFI_POWER_19_5dBm);

  // Find how many wifi are available
  int numberOfNetworks = WiFi.scanNetworks();

  // Set WiFi transmission power to min to save energy
  WiFi.setTxPower(WIFI_POWER_5dBm);

  // HTML of configuration page
  String content = "<html><body><h1><strong>You are connected to <br><span style=\"color: #ff0000; \">" + defaultDeviceTypeName + "</span></strong></h1>";
  content += "<strong>MAC:  " + WiFi.macAddress() + "</strong><br>";
  content += "<strong>Device Fingerprint:  " + deviceFingerprint + "</strong><br>";
  content += "<strong>SW version:  " + deviceSWversion + "</strong><br>";
  content += "<strong>Broker URI:  " + getSavedBrokerURI() + "</strong><br>";
  content += "<strong>ID:  " + getSavedID() + "</strong><br>";
  content += "<strong>Device type:  " + getSavedDeviceType() + "</strong><br>";

  if(connections > 0){
    content += "Saved connections: <br><form action='/delete' method='POST'><ul id='savedNetworks' style='list-style: none;'>";
    for(int i=0; i<connections; i++){
      getSavedSSIDs();
      int index = i + 1;
      if(i==0){ // Only the first is checked
        content += "<li><input id='" + String(i) + "' type='radio' class='radio' name='networks' checked>" + String(SSIDs[i]) + "<span style='margin-right:10px;'>[" + String(index) + "/" + String(maxConnections) + "]</span></li>";
      }else {
        content += "<li><input id='" + String(i) + "' type='radio' class='radio' name='networks'>" + String(SSIDs[i]) + "<span style='margin-right:10px;'>[" + String(index) + "/" + String(maxConnections) + "]</span></li>";
      }
    }
    content += "</ul><input id='INDEX' name='INDEX' type='text' style='width:0; height:0; border:none;'>";
    content += "<input type='submit' name='DELETEWIFI' value='Delete'></form>";
  } else {
    content += "<br><br>No connection saved. Please add a WiFi setting in the form below<br><br>";
  }

  if(connections != maxConnections){
      content += "WiFi connections detected nearby.<br> Select the one you want to connect to<br>(or write its SSID below)<br><select id='list'><option id='null'>Show WiFi detected</option>";
      for(int i=0; i<numberOfNetworks; i++){
        content += "<option id='" + WiFi.SSID(i) +"'>" +  WiFi.SSID(i) +" Signal: " + getSignal(WiFi.RSSI(i)) + "</option>";
      }
      content += "</select>";

      content += "<form action='/add' method='POST'>";
      content += "<br><br>WiFi-SSID:<input type='text' name='SSID' id='SSID' placeholder='ssid'><br><br>";
      content += "WiFi-PSW :<input type='password' name='PSW' placeholder='password'><br><br>";
      content += "<input type='submit' name='SAVE' value='SAVE'></form><br><button onclick=\"location.href='http://192.168.4.1/cancel';\" >Cancel</button><br>";
  } else {
    content += "Reached max number of saved connections. Please delete a saved connection to add a new wifi setting <br><br>";
  }
  
  content += "<br><br><button id='resetButton'>Reset</button><br><br>";  
    
  content += "<a href='/guide'>Guide</a><script>if(screen.width<800 && document.getElementsByClassName('radio').length > 0){for(var i=0; i<document.getElementsByClassName('radio').length; i++){var r = document.getElementsByClassName('radio')[i]; r.style.width='30px'; r.style.height='30px';}};if(document.getElementById('INDEX')!=null){document.getElementById('INDEX').value=0;}document.getElementById('list').addEventListener('change', function(){var w = list.options[list.options['selectedIndex']].id;if(w != 'null')document.getElementsByName('SSID')[0].value = w;});";
  content += "for(i=0; i<" + String(connections) + "; i++){document.getElementById(i.toString()).addEventListener('click', function(){document.getElementById('INDEX').value = this.id})}document.getElementById('resetButton').addEventListener('click', function(){var check = confirm('Are you sure to reset?');if(check)location.href = 'http://192.168.4.1/reset';});";
  content += "</script></body></html>";
  server.send(200, "text/html", content);
}

// To write the EEPROM: EEPROM.write(position, value). To save changes: EEPROM.commit()
void handleSaveNewConnection(){
  if (server.hasArg("SSID") && server.hasArg("PSW")) {

    if (server.arg("SSID") == "" ||  server.arg("PSW") == "" || server.arg("SSID").length() < 2 || server.arg("SSID").length() > 40 || server.arg("PSW").length() < 2 || server.arg("PSW").length() > 40) {

      server.send(200, "text/html", "<h1>Invalid data!</h1><br><a href='/'>Back</a>");
      delay(300);
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

      EEPROM.write(explicitResetSavePosition, 1);
      EEPROM.commit();

      delay(1000);

      state = DEEPSLEEP;
      return;
    }
  }
}

void handleCancel(){
  String content = "<html><body>Access Point Shut Down</body></html>";
  server.send(200, "text/html", content);
  delay(1000);
  state = DEEPSLEEP;
}

void handleDelete(){
  int index = server.arg("INDEX").toInt();
  String deletedName = SSIDs[index];
  int connections = EEPROM.read(numberOfConnectionSavePosition);

  // If there is only 1 saved connection, delete it
  if(connections == 1){
    for(int i=1; i<49; i++){
      EEPROM.write(numberOfConnectionSavePosition + i, 255);
    }
    for(int i=50; i<99; i++){
      EEPROM.write(numberOfConnectionSavePosition + i, 255);
    }
  } else if (connections != 1 && connections != index + 1) { // If there is more than 1 connection, delete it according to the index and replace it with the last one
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
      EEPROM.write(numberOfConnectionSavePosition + 100*(connections-1) + i, 255);
    }
  } else { // If the selected network is the last one, just delete
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
  EEPROM.write(explicitResetSavePosition, 1);
  EEPROM.commit();
  delay(1000);
  state = DEEPSLEEP;
}

// Reset means writing 255 in all cells of EEPROM
void handleReset(){
  for (int i = 0; i < EEPROMSIZE; i++) {
    EEPROM.write(i, 255);
  }
  EEPROM.write(numberOfConnectionSavePosition, 0);
  EEPROM.write(explicitResetSavePosition, 0);
  EEPROM.commit();
  state = DEEPSLEEP;
  String content = "<html><body>Reset Completed</body></html>";
  server.send(200, "text/html", content);
  delay(1000);
}

void handleGuide() {
  String content = "<html><body>For information visit <i>https://www.snap4city.org/drupal/node/297</i><br><a href='/'>Back</a></body></html>";
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

// <----------------------------------- Read data from EEPROM ------------------------------------>

void getSavedSSIDs(){
  int connections = EEPROM.read(numberOfConnectionSavePosition);
  Serial.println(connections);
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
  Serial.println(savedSSID);
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
  Serial.println(savedPSW);
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

String getSavedCertificate(){
  String savedCertificate = "";
  int i = 0;
  do{
    int nextElement = EEPROM.read(certificateSavePosition + i);
    if(nextElement != 13)
      savedCertificate = savedCertificate + char(nextElement);
    i++;
  } while(EEPROM.read(certificateSavePosition + i) != 255);
  return savedCertificate;
}

String getSavedPrivateKey(){
  String savedPrivateKey = "";
  int i = 0;
  do{
    int nextElement = EEPROM.read(privateKeySavePosition + i);
    if(nextElement != 13)
      savedPrivateKey = savedPrivateKey + char(nextElement);
    i++;
  } while(EEPROM.read(privateKeySavePosition + i) != 255);
  return savedPrivateKey;
}

// <------------------------------ WiFi connection ------------------------------->

int connectToWiFi(RgbColor color){
  strip.SetPixelColor(0, color);
  strip.Show();
  
  WiFi.disconnect();

  int RSSIs[maxConnections] = {MINSIGNAL, MINSIGNAL, MINSIGNAL, MINSIGNAL, MINSIGNAL};
  int availableConnections = 0;
  bool connectionEstablished = false;
  int savedConnections = EEPROM.read(numberOfConnectionSavePosition);
  int scannedConnections = WiFi.scanNetworks();

  getSavedSSIDs();
  getSavedPSWs();
  
  if((scannedConnections==0 || savedConnections == 0) && color != black){
    strip.SetPixelColor(0, red);
    strip.Show();
    delay(1000);
    strip.SetPixelColor(0, black);
    strip.Show();
    return false;
  }

  // Find the RSSI signal for all SSIDs
  for(int i=0; i<savedConnections; i++){
    for(int j=0; j<scannedConnections; j++){
      if(SSIDs[i] == WiFi.SSID(j)){
        RSSIs[i] = WiFi.RSSI(j);
        availableConnections++;
      }
    }
  }

  // Retry connection until is not established and there are more networks to try  
  while(!connectionEstablished && availableConnections){
    int index = nextConnection(RSSIs, maxConnections);
    char ssid[SSIDs[index].length()+1];
    SSIDs[index].toCharArray(ssid, SSIDs[index].length()+1);
    char psw[PSWs[index].length()+1];
    PSWs[index].toCharArray(psw, PSWs[index].length()+1);
    connectionEstablished = connect(ssid, psw, color);
    availableConnections--;
  }

  if(connectionEstablished && color != black){
    strip.SetPixelColor(0, green);
    strip.Show();
    delay(1000);
    strip.SetPixelColor(0, black);
    strip.Show();
    return true;
  }
  else if (color != black){
    strip.SetPixelColor(0, red);
    strip.Show();
    delay(1000);
    strip.SetPixelColor(0, black);
    strip.Show();
    return false;
  } 
}

bool connect(char* ssid, char* psw, RgbColor color){
  int attemptNumber = 0;
  WiFi.begin(ssid, psw);
  while(WiFi.status() != WL_CONNECTED && attemptNumber < 50){
    strip.SetPixelColor(0, color);
    strip.Show();
    delay(325);
    strip.SetPixelColor(0, black);
    strip.Show();
    attemptNumber++;
  }

  if (WiFi.status() == WL_CONNECTED)
      return true;
  else return false;
}

// Find which connection has the best signal
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

// <------------------------------ Send Message ------------------------------->
void sendMessage(int code, String buttonType, RgbColor color){

  // Be sure to be in Station mode  
  WiFi.mode(WIFI_MODE_STA);

  // If already connected no need to reconnect again. Useful for double confirms.  
  if(!connectedToWiFi){
    connectToWiFi(color);
    connectedToWiFi = true;
  }
  
  strip.SetPixelColor(0, color);
  strip.Show();
  delay(1000);

  WiFiClientSecure wifiSecure;
  
  // Connect timeout
  wifiSecure.setTimeout(1000);

  // Check is K1,K2 or Cert&Key
  bool secure = false;

  // 48 is the ASCII for 0, which will be saved in certificateSavePosition if certificate is not uploaded
  if(EEPROM.read(certificateSavePosition) != 48)
    secure = true;

  if(secure){

    String certString = getSavedCertificate();
    String keyString = getSavedPrivateKey();
    
    certBuffer = (char*) malloc(certString.length() * sizeof(char));
    keyBuffer = (char*) malloc(keyString.length() * sizeof(char));

    for(int i=0; i<certString.length(); i++){
      certBuffer[i] = certString[i];
      yield();
    }

    for(int i=0; i<keyString.length(); i++){
      keyBuffer[i] = keyString[i];
      yield();
    }
    wifiSecure.setCertificate(certBuffer);
    wifiSecure.setPrivateKey(keyBuffer);
  }

      
  String host = getSavedBrokerURI().substring(8);
  char hostChars[host.length() + 1];
  (host).toCharArray(hostChars, host.length() + 1);

  Serial.print("POWER: ");
  Serial.println(WiFi.getTxPower());

  int attempts = 0;
  bool correctSend = false;

  // Three attempts to send data
  do {
    if(wifiSecure.connect(hostChars, 8080)){
      // Make a HTTP request:
      String data = buildMessage(code, buttonType);
      if(secure){
        wifiSecure.println(buildRequestSecure());
        strip.Show();
      } else{
        wifiSecure.println(buildRequestUnsecure());
      }
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
    
      if(responseLength>350 && color != black){
        strip.SetPixelColor(0, green);
        strip.Show();
        delay(1000);
        strip.SetPixelColor(0, black);
        strip.Show();
        if(buttonType == "roundbutton")
          confSendL = true;
        if(buttonType == "squarebutton")
          confSendR = true;
        correctSend = true;
      }
    }
    attempts++;
    Serial.println(attempts);
    delay(500);
  } while(attempts < 3 && (!correctSend) && color!=black);
  wifiSecure.stop();
  if (!correctSend && color != black){
    strip.SetPixelColor(0, red);
    strip.Show();
    delay(1000);
    strip.SetPixelColor(0, black);
    strip.Show();
  }
  state = LITTLE_SLEEP;
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

String buildMessage(int code, String buttonType){
  String msg = "{\"contextElements\":[{\"type\": \"";
  msg += getSavedDeviceType();
  msg += "\",\"isPattern\":false,\"id\": \"";
  msg += getSavedID();
  msg += "\", \"attributes\":[{\"name\":\"";
  msg += buttonType;
  msg += "\",\"type\":\"integer\",\"value\":";
  msg += code;
  msg += "}]}],\"updateAction\":\"APPEND\"}";
  return msg;
}

void readButtonPress(){
  while(samples < 6){
    if(samples == 0)
      currentMillis = 150;
    else currentMillis = millis();
    if(currentMillis - previousMillis >= interval){
        
      // Read right button
      if(digitalRead(BUTTON_1_PIN) == 1){ // Right button pressed
        valuesRight[samples] = 1;
        }  else { // Right button not pressed
        valuesRight[samples] = 0;
      }

      // Read left button
      if(digitalRead(BUTTON_2_PIN) == 1){ // Left button pressed
        valuesLeft[samples] = 1;
        }  else { // Left button not pressed
        valuesLeft[samples] = 0;
      }
      
      // Update timers and number of samples
      previousMillis = currentMillis;
      samples++;
      if(samples == 6)
        break;
    }
  }

  for(int i=0; i<6; i++){
    int current = valuesRight[i];
    Serial.print(current);
    if(current == 0)
      downRight = true;
    else{
      if(downRight)
        nextRight++;
      else beforeRight++;
    }
  }

  Serial.println();

  if(downRight && nextRight>0){
    rightState = DOUBLEPRESS;
  } else if (beforeRight > 4){
    rightState = LONGPRESS;
  } else if(beforeRight > 0 && beforeRight <= 4){
    rightState = SHORTPRESS;
  }

  Serial.println(rightState);

    for(int i=0; i<6; i++){
    int current = valuesLeft[i];
    Serial.print(current);
    if(current == 0)
      downLeft = true;
    else{
      if(downLeft)
        nextLeft++;
      else beforeLeft++;
    }
  }

  Serial.println();

  if(downLeft && nextLeft>0){
    leftState = DOUBLEPRESS;
  } else if (beforeLeft > 4){
    leftState = LONGPRESS;
  } else if(beforeLeft > 0 && beforeLeft <= 4){
    leftState = SHORTPRESS;
  }

  Serial.println(leftState);
}

void reactToPress(){
    // Associate actions to events
  if(rightState == LONGPRESS && leftState == LONGPRESS){
      state =  MODIFYSETTINGS;
  } else if(rightState == SHORTPRESS && leftState == SHORTPRESS){
    sendMessage(SHORTPRESS, "squarebutton", yellow);
    sendMessage(SHORTPRESS, "roundbutton", orange);
  }
  if(rightState == SHORTPRESS && leftState == UNPRESSED){
    sendMessage(SHORTPRESS, "squarebutton", yellow);
  } else if (rightState == DOUBLEPRESS){
    sendMessage(DOUBLEPRESS, "squarebutton", fuxia);
  } else if (rightState == LONGPRESS && leftState == UNPRESSED){
    sendMessage(LONGPRESS, "squarebutton", blue);
  } else if(leftState == SHORTPRESS && rightState == UNPRESSED){
    sendMessage(SHORTPRESS, "roundbutton", orange);
  } else if(leftState == DOUBLEPRESS){
    sendMessage(DOUBLEPRESS, "roundbutton", purple);
  } else if(leftState == LONGPRESS && rightState == UNPRESSED){
    sendMessage(LONGPRESS, "roundbutton", aqua);
  }
}

// Send null confirm
void sendNullMessage(){
  if(confSendR == true)
    sendMessage(0, "squarebutton", black);
  if(confSendL == true)
    sendMessage(0, "roundbutton", black);
  state = DEEPSLEEP;
  confSendR = false;
  confSendL = false;
}

void checkState(){
    if(state == RESETTED){
      EEPROM.write(explicitResetSavePosition, 0);
      EEPROM.commit();
      state = CONFIGAP;
    }
        
    if(state == CONFIGAP){
      createFirstAP();
    }
        
    if(state == HANDLECLIENT){
      APhandleClient();
    }
        
    if(state == DEEPSLEEP){
      sleepUntilHigh();
    }
        
    if(state == READBUTTONPRESS){
        readButtonPress();
        reactToPress();

        if(state!=MODIFYSETTINGS)
          state = LITTLE_SLEEP;
    }
    if(state == MODIFYSETTINGS){
      createFirstAP();
      APhandleClient();
    }
        
    if(state == LITTLE_SLEEP){
      sleepUntilThreeOrHigh();
    }
        
    if(state == CHECK_SLEEP_TYPE)
        checkSleepType();
    
    if(state == CONFIRM){
      sendNullMessage();
    }
}

// Create webserver associating pages to handlers
void createFirstAP(){
    WiFi.mode(WIFI_MODE_AP);
    WiFi.softAP(ssidAP, passwordAP);
    server.on("/", handleRoot);
    server.on("/save", handleSave);
    server.on("/add", handleSaveNewConnection);
    server.on("/guide", handleGuide);
    server.on("/delete", handleDelete);
    server.on("/cancel", handleCancel);
    server.on("/reset", handleReset);
    server.onNotFound(handleNotFound);
    server.begin();
    state = HANDLECLIENT;
    WiFi.setTxPower(WIFI_POWER_5dBm);
}

void APhandleClient(){
    currentImpulseTime = millis();
    // After 4 minutes auto shut down
    if(currentImpulseTime > timeout){
      state = DEEPSLEEP;
      return;
    }
    // Blink each second
    if(currentImpulseTime - previousImpulseTime >= impulseInterval){
      endImpulse = !endImpulse;
      previousImpulseTime = currentImpulseTime;
    } else {
      if(endImpulse){
        strip.SetPixelColor(0, black);
        strip.Show();
      } else {
        strip.SetPixelColor(0, white);
        strip.Show();
      }
    }
    Serial.println(WiFi.getTxPower());
    server.handleClient();
}

void sleepUntilHigh(){
    explicitReset = EEPROM.read(explicitResetSavePosition);
    if(explicitReset != 0 && explicitReset != 255)
      state = READBUTTONPRESS;
    else state = CONFIGAP;
    strip.SetPixelColor(0, black);
    strip.Show();
    digitalWrite(17, LOW);

    esp_sleep_enable_ext1_wakeup(BUTTON_PIN_BITMASK, ESP_EXT1_WAKEUP_ANY_HIGH);
    esp_deep_sleep_start();  
}

void sleepUntilThreeOrHigh(){
    // Deep sleep 3s after message sending. If a pressure occurs a new message will be sent, otherwise the confirm will be sent
    state = CHECK_SLEEP_TYPE;
    esp_sleep_enable_timer_wakeup(3000000);
    esp_sleep_enable_ext1_wakeup(BUTTON_PIN_BITMASK, ESP_EXT1_WAKEUP_ANY_HIGH);
    esp_deep_sleep_start(); 
}

void checkSleepType(){
    if(esp_sleep_get_wakeup_cause() == ESP_DEEP_SLEEP_WAKEUP_TIMER){
      Serial.println("WAKEUP: CONFIRM");
      state = CONFIRM;
    } else {
      Serial.println("WAKEUP: NEWBUTTONPRESS");
      state = READBUTTONPRESS;
    }
}

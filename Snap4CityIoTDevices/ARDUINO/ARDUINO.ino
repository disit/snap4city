/* IOTDEVICE-ARDUINO.
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

#include <Adafruit_Sensor.h>

#include <DHT.h>
#include <DHT_U.h>

#include <OneButton.h>

#define REDPIN 13
#define GREENPIN 12
#define BLUEPIN 11
#define DHTPIN 7
#define DHTTYPE DHT22

// Button connected to pin4
OneButton button(4, true);

// Temperature&Humidity sensor setup
DHT dht(DHTPIN, DHTTYPE);

// Contains the command from the ESP8266
int incomingByte;

// Prevents automatic message sending during client operations
bool busy = false;

// Flag that becomes true when the 'C' command is sent by the ESP8266
bool configured = false;

// Variables used to measure time and send data after a selected interval
unsigned long previousMillis = 0;
const long interval = 20000;  

void setup() {
  // Starts the Serial
  Serial.begin(115200);
  // Starts the sensor
  dht.begin();
  // Associates the led's pins to output
  pinMode(REDPIN, OUTPUT);
  pinMode(GREENPIN, OUTPUT);
  pinMode(BLUEPIN, OUTPUT);
  // Associates button's events to actions
  button.attachClick(sendData);
  button.attachLongPressStop(sendLongPress);
}

void loop() {
  // Check if the button is pressed
  button.tick();
  delay(10);

  unsigned long currentMillis = millis();

  // Measure time and send data after each interval. If the device is busy or not configured no data will be sent.
  if (currentMillis - previousMillis >= interval) {
    previousMillis = currentMillis;
    if(!busy && configured){
      sendData();
    }
  }

  // Listen to serial if there is a command from the ESP8266.
  // C: Configured, R: start blink Red, r: end blink red ecc, G: start blink Green, g: end blink green ecc.
  if (Serial.available() > 0) {
      incomingByte = Serial.read();
      if (incomingByte == 'R' || incomingByte == 'G' || incomingByte == 'B' || incomingByte == 'W'){
        busy = true;
        startBlink(incomingByte);
      }
      if (incomingByte == 'r' || incomingByte == 'g' || incomingByte == 'b' || incomingByte == 'w'){
        endBlink(incomingByte);
        busy = false;
        previousMillis = currentMillis;
      }
      if (incomingByte == 'C'){
        configured = true;
    }
  }
}

// Notify the ESP8266 that a long press appened
void sendLongPress(){
  if(configured){
    Serial.println("L");
  }
}

void sendData(){
  if(configured){
   float temperature;
   float humidity;
   // Read sensors
   temperature = dht.readTemperature();
   humidity = dht.readHumidity();

   // Data will have the format "temperature;humidity". Example: 23.1;45.4
   String data = String(temperature) + ";" + String(humidity);
 
   Serial.println(data); 
  }
}

// Start blink of the selected color
void startBlink(char code){
  switch(code){
    case ('R'):
      digitalWrite(REDPIN, HIGH);
      break;
    case ('G'):
      digitalWrite(GREENPIN, HIGH);
      break;
    case ('B'):
      digitalWrite(BLUEPIN, HIGH);
      break;
    case ('W'):
      digitalWrite(REDPIN, HIGH);
      digitalWrite(GREENPIN, HIGH);
      digitalWrite(BLUEPIN, HIGH);
      break;
  }
}

// End blink of the selected color
void endBlink(char code){
  switch(code){
    case ('r'):
      digitalWrite(REDPIN, LOW);
      break;
    case ('g'):
      digitalWrite(GREENPIN, LOW);
      break;
    case ('b'):
      digitalWrite(BLUEPIN, LOW);
      break;
    case ('w'):
      digitalWrite(REDPIN, LOW);
      digitalWrite(GREENPIN, LOW);
      digitalWrite(BLUEPIN, LOW);
      break;
  }
}

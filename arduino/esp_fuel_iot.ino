//#include <Wire.h>
//#include <LiquidCrystal_I2C.h>
#include <ESP8266WiFi.h>
#include <WiFiClient.h>
#include <ESP8266WebServer.h>

#ifndef APSSID
#define APSSID "FuelIOT"
#define APPSK "admin123"
#endif

const char *ssid = APSSID;
const char *password = APPSK;

ESP8266WebServer server(80);
//---------Alternative way to send data to app------------
//WiFiServer server(80);

//void sendData(char *data) {
//  server.send(200, "text/html", "<h1>Fuel tansfer</h1>"+*data);
//}
//void handleRoot() {
//  Serial.print("Started");
//}
//LiquidCrystal_I2C lcd(0x27, 20, 4); // set the LCD address to 0x27 for a 16x2 display
//----------------------------------------------------------
const int inputPin = D5; // D5 on NodeMCU board
const int outputPin = D6; // D6 on NodeMCU board
volatile int inputFlowCount = 0;
volatile int outputFlowCount = 0;
float inputFlowRate = 0.0;
float outputFlowRate = 0.0;
float inputFlowLitresPerMinute = 0.0;
float outputFlowLitresPerMinute = 0.0;
float inputTotalLitres = 0.0;
float outputTotalLitres = 0.0;
unsigned long oldTime = 0;
const int tankCapacity = 1000; // tank capacity in liters

void ICACHE_RAM_ATTR inputFlowInterrupt()
{
    inputFlowCount++;
}

void ICACHE_RAM_ATTR outputFlowInterrupt()
{
    outputFlowCount++;
}

void setup()
{
    //Setup serial input
    Serial.begin(115200);

    WiFi.softAP(ssid, password);

    IPAddress myIP = WiFi.softAPIP();
    Serial.print("AP IP address: ");
    Serial.println(myIP);
    server.on("/", handleRoot);
    server.begin();



    //
    pinMode(inputPin, INPUT_PULLUP);
    pinMode(outputPin, INPUT_PULLUP);
    //wire attachment function
    attachInterrupt(digitalPinToInterrupt(inputPin), inputFlowInterrupt, RISING);
    attachInterrupt(digitalPinToInterrupt(outputPin), outputFlowInterrupt, RISING);
    //Wire.begin();

    //initialize
    Serial.print("Water Monitor ");


    /*lcd.init();
      lcd.backlight();
      lcd.setCursor(0, 0);
      lcd.print("Water Monitor");
      delay(1000);
      lcd.clear();*/
    oldTime = millis();
}

void loop()
{
    //server.handleClient();
    if (millis() - oldTime > 1000) // update every second
    {
        detachInterrupt(digitalPinToInterrupt(inputPin));
        detachInterrupt(digitalPinToInterrupt(outputPin));
        inputFlowLitresPerMinute = ((1000.0 / (millis() - oldTime)) * inputFlowCount) / 6.5; // 6.5 is the pulse frequency
        outputFlowLitresPerMinute = ((1000.0 / (millis() - oldTime)) * outputFlowCount) / 6.5; // 6.5 is the pulse frequency
        oldTime = millis();
        inputTotalLitres += inputFlowLitresPerMinute / 60.0; // calculate flow in liters/minute
        outputTotalLitres += outputFlowLitresPerMinute / 60.0; // calculate flow in liters/minute
        inputFlowCount = 0;
        outputFlowCount = 0;
        attachInterrupt(digitalPinToInterrupt(inputPin), inputFlowInterrupt, RISING);
        attachInterrupt(digitalPinToInterrupt(outputPin), outputFlowInterrupt, RISING);
    }

    server.handleClient();


    // ------------ Alternative way to send data to app-------------
    //char result[10];
    //dtostrf(inputTotalLitres , 6, 2, result);
    //char message[20] = "The value is:";
    //strcat(message, result)

    //WiFiClient client = server.available();

    //if (client) {
    //  client.print(strcat(message, result));
    //  send a variable data to the server
    //  String variable_data = "Hello World";
    //  client.println(variable_data);

    // read the server response and print it to the console
    /*while (client.connected()) {
      if (client.available()) {
        client.print(variable_data);
      }
    }*/




    Serial.printf("Input Total litres %f", inputTotalLitres);

    //sendData(strcat(message, result));
    //sendData("hell");


    /*lcd.setCursor(0, 0);
      lcd.print("Input: ");
      lcd.print(inputTotalLitres, 1);
      lcd.print(" L    ");*/


    Serial.printf("Output Total litres %f", outputTotalLitres);
    /*lcd.setCursor(0, 1);
      lcd.print("Output: ");
      lcd.print(outputTotalLitres, 1);
      lcd.print(" L    ");*/
    delay(1000);
    //}
}
void handleRoot() {
    char result[10];
    dtostrf(inputTotalLitres , 6, 2, result);
    char output[10];
    dtostrf(outputTotalLitres , 6, 2, output);
    String data = strcat(strcat(result, "/"), output );
    server.send(200, "text/plain", data);
}

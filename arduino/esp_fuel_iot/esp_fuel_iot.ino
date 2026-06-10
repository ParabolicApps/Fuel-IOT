#include <WiFi.h>
#include <WebServer.h>
#include <Wire.h>
#include <LiquidCrystal_I2C.h>

#ifndef APSSID
#define APSSID "FuelIOT"
#define APPSK "admin123"
#endif

// ---------------- Test Mode (Station) ----------------
#define TEST_MODE true  // Set to true to connect to router, false for AP mode
#define ROUTER_SSID "Loading."
#define ROUTER_PASS "pakhi72544"

const char *ssid = APSSID;
const char *password = APPSK;

WebServer server(80);

// ---------------- LCD ----------------
LiquidCrystal_I2C lcd(0x27, 20, 4);

// ---------------- GPIO ----------------
const int inputPin = 18;
const int outputPin = 19;

// ---------------- Flow counters ----------------
volatile int inputFlowCount = 0;
volatile int outputFlowCount = 0;

// ---------------- Flow values ----------------
float inputFlowLPM = 0.0;
float outputFlowLPM = 0.0;

float inputTotalLitres = 0.0;
float outputTotalLitres = 0.0;

unsigned long oldTime = 0;

// ---------------- Device Info ----------------
String deviceIP;
String deviceName = "Fuel Guard Node 01";
String deviceStatus = "0"; // 0 = Active, 1 = Disabled/Locked

// ---------------- Interrupts ----------------
void IRAM_ATTR inputFlowInterrupt() {
  inputFlowCount++;
}

void IRAM_ATTR outputFlowInterrupt() {
  outputFlowCount++;
}

// =====================================================
// METHOD 1: SIMPLE FORMAT (/)
// =====================================================
void handleRoot() {
  String data =
    String(inputTotalLitres, 2) + "/" +
    String(outputTotalLitres, 2);

  server.send(200, "text/plain", data);
}

// =====================================================
// METHOD 2: TELEMETRY API (/data)
// =====================================================
void handleData() {
  // App expects format: "inputData/outputData"
  String data =
    String(inputTotalLitres, 2) + "/" +
    String(outputTotalLitres, 2);

  server.send(200, "text/plain", data);
}

// =====================================================
// METHOD 3: DEBUG FORMAT (/data2)
// =====================================================
void handleData2() {
  String data =
    "IP=" + deviceIP + "\n" +
    "IN_TOTAL=" + String(inputTotalLitres, 2) + "\n" +
    "OUT_TOTAL=" + String(outputTotalLitres, 2) + "\n" +
    "IN_LPM=" + String(inputFlowLPM, 2) + "\n" +
    "OUT_LPM=" + String(outputFlowLPM, 2);

  server.send(200, "text/plain", data);
}

// =====================================================
// METHOD 4: DISCOVERY API (/getDevice)
// =====================================================
void handleGetDevice() {
  // The Android app expects: Name + \0 + Status + \n
  // PostTask reads one line with readLine()

  String response = deviceName;
  response += (char)0; // Add null delimiter
  response += deviceStatus;
  response += '\n';

  server.send(200, "text/plain", response);
}

// =====================================================
// METHOD 5: CONFIGURATION API (/configure)
// =====================================================
void handleConfigure() {
  if (server.hasArg("title")) {
    deviceName = server.arg("title");
  }

  String response = deviceName; // App expects the new title back

  if (server.hasArg("ssid") && server.hasArg("psk")) {
    String newSSID = server.arg("ssid");
    String newPSK = server.arg("psk");
    Serial.printf("New Config: SSID=%s, PSK=%s, Title=%s\n", newSSID.c_str(), newPSK.c_str(), deviceName.c_str());

    // In a real scenario, you'd save to EEPROM/LittleFS and reboot to station mode
    lcd.clear();
    lcd.print("Config Updated");
    lcd.setCursor(0, 1);
    lcd.print(newSSID);
  }

  server.send(200, "text/plain", response);
}

// =====================================================
// SETUP
// =====================================================
void setup() {
  Serial.begin(115200);

  // LCD init
  Wire.begin(21, 22);
  lcd.init();
  lcd.backlight();

  if (TEST_MODE) {
    Serial.println("\nConnecting to WiFi...");
    lcd.setCursor(0, 0);
    lcd.print("WiFi Connecting");
    lcd.setCursor(0, 1);
    lcd.print(ROUTER_SSID);

    WiFi.begin(ROUTER_SSID, ROUTER_PASS);
    int retry = 0;
    while (WiFi.status() != WL_CONNECTED && retry < 20) {
      delay(500);
      Serial.print(".");
      retry++;
    }

    if (WiFi.status() == WL_CONNECTED) {
      deviceIP = WiFi.localIP().toString();
      Serial.println("\nConnected!");
    } else {
      Serial.println("\nFailed! Falling back to AP");
      WiFi.softAP(ssid, password);
      deviceIP = WiFi.softAPIP().toString();
    }
  } else {
    WiFi.softAP(ssid, password);
    IPAddress ip = WiFi.softAPIP();
    deviceIP = ip.toString();
  }

  Serial.println("\n========================");
  Serial.println("Fuel IoT System Started");
  Serial.printf("Mode: %s\n", TEST_MODE ? "Station (Router)" : "AP (Direct)");
  Serial.printf("IP Address: %s\n", deviceIP.c_str());
  Serial.println("========================");

  lcd.clear();
  lcd.setCursor(0, 0);
  lcd.print("Fuel Monitor");
  lcd.setCursor(0, 1);
  lcd.print(TEST_MODE ? "STA: " : "AP: ");
  lcd.print(deviceIP);
  delay(2000);
  lcd.clear();

  // Web routes
  server.on("/", handleRoot);
  server.on("/data", handleData);
  server.on("/data2", handleData2);
  server.on("/getDevice", HTTP_POST, handleGetDevice);
  server.on("/configure", HTTP_POST, handleConfigure);
  server.begin();

  // Flow sensors
  pinMode(inputPin, INPUT_PULLUP);
  pinMode(outputPin, INPUT_PULLUP);

  attachInterrupt(digitalPinToInterrupt(inputPin), inputFlowInterrupt, RISING);
  attachInterrupt(digitalPinToInterrupt(outputPin), outputFlowInterrupt, RISING);

  oldTime = millis();
}

// =====================================================
// LOOP
// =====================================================
void loop() {

  if (millis() - oldTime > 1000) {

    detachInterrupt(digitalPinToInterrupt(inputPin));
    detachInterrupt(digitalPinToInterrupt(outputPin));

    unsigned long dt = millis() - oldTime;

    inputFlowLPM =
      ((1000.0 / dt) * inputFlowCount) / 6.5;

    outputFlowLPM =
      ((1000.0 / dt) * outputFlowCount) / 6.5;

    oldTime = millis();

    inputTotalLitres += inputFlowLPM / 60.0;
    outputTotalLitres += outputFlowLPM / 60.0;

    inputFlowCount = 0;
    outputFlowCount = 0;

    attachInterrupt(digitalPinToInterrupt(inputPin), inputFlowInterrupt, RISING);
    attachInterrupt(digitalPinToInterrupt(outputPin), outputFlowInterrupt, RISING);

    // ================= SERIAL MONITOR =================
    Serial.printf(
      "\n[Fuel IoT]\n"
      "IP: %s\n"
      "IN Total  : %.2f L\n"
      "OUT Total : %.2f L\n"
      "IN Flow   : %.2f L/min\n"
      "OUT Flow  : %.2f L/min\n"
      "------------------------\n",
      deviceIP.c_str(),
      inputTotalLitres,
      outputTotalLitres,
      inputFlowLPM,
      outputFlowLPM
    );
  }

  server.handleClient();

  // ================= LCD DISPLAY =================
  lcd.setCursor(0, 0);
  lcd.print("IN : ");
  lcd.print(inputTotalLitres, 2);
  lcd.print(" L     ");

  lcd.setCursor(0, 1);
  lcd.print("OUT: ");
  lcd.print(outputTotalLitres, 2);
  lcd.print(" L     ");

  lcd.setCursor(0, 2);
  lcd.print("IN LPM : ");
  lcd.print(inputFlowLPM, 2);
  lcd.print("     ");

  lcd.setCursor(0, 3);
  lcd.print("OUT LPM: ");
  lcd.print(outputFlowLPM, 2);
  lcd.print("     ");

  delay(200);
}
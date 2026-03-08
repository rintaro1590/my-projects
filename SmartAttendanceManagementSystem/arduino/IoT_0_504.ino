#include "RTC.h"
#include <WiFiS3.h>
#include <NTPClient.h>
#include <WiFiUdp.h>
#include <ArduinoJson.h>
#include <Wire.h>
#include <DHT.h>
#include <SPI.h>
#include <Arduino.h>
#include <U8g2lib.h>

// --- OLED設定 ---
// I2C接続のSSD1306 128x64 OLEDディスプレイの設定
U8G2_SSD1306_128X64_NONAME_F_HW_I2C u8g2(U8G2_R0, /* reset=*/ U8X8_PIN_NONE);

// --- ピン定義 ---
#define VIBRATION_PIN 3       // 振動センサの入力ピン
#define BUZZER_PIN 8          // ブザーの出力ピン
#define DHTPIN 2              // DHT11温湿度センサのピン
#define DHTTYPE DHT11         // センサの種類をDHT11に指定
#define LIGHT_SENSOR_PIN A0   // 照度センサのアナログ入力ピン

DHT dht(DHTPIN, DHTTYPE);

// --- SPIセンサ（SCP1000等）設定 ---
const int chipSelectPin = 10; // SPIのチップセレクトピン
// 通信設定：1MHz, 最上位ビットから送信, モード3（以前のデバッグ結果に基づきMODE3に設定）
SPISettings scp1000Settings(1000000, MSBFIRST, SPI_MODE3);

// --- ネットワーク・サーバー設定 ---
const char *ssid = "oyama-android";
const char *pass = "oyama.android";
const char server_ip[] = "10.100.56.161";   // データを送信するサーバーIP
IPAddress local_IP(10, 100, 56, 171);       // Arduino自身の固定IP
IPAddress gateway(10, 100, 56, 254);
IPAddress subnet(255, 255, 255, 0);

WiFiUDP ntpUDP;
// NTPサーバーから時刻を取得するための設定（オフセット32400秒＝日本時間+9時間）
NTPClient timeClient(ntpUDP, "10.100.56.161", 32400);
WiFiClient client;

// --- 動作設定 ---
const int light_threshold = 0;          // 照度の判定しきい値
const char *room = "0-504";             // 部屋番号
const int send_time = 3600;             // 定期送信の間隔（秒）
const int vibration_threshold = 0;      // 振動カウントのしきい値
const unsigned long VIBRATION_TIMEOUT = 10000; // 振動検知終了までの待機時間（10秒）
const int vibrationsensor_lowtimeout = 10000;  // センサ異常（LOW張り付き）判定時間

// --- グローバル変数 ---
unsigned long lastUpdate = 0;           // 前回のgetData実行時刻
volatile bool alarm_fired = false;      // RTCタイマー割り込みフラグ
volatile bool vibration_detected = false; // 振動検知フラグ
volatile int vibration_count = 0;       // 振動回数カウント
unsigned long last_vibration_time = 0;
bool is_vibrating = false;              // 振動処理中かどうかの状態
unsigned long low_start_time = 0;       // 振動センサがLOWになり始めた時刻
char buf[64];                           // 日時文字列用バッファ
float t = 0, h = 0, p = 0;              // 温度, 湿度, 気圧の保持用
bool l = false;                         // 明るさ判定（true=明るい）

// --- プロトタイプ宣言 ---
void connectWiFi();
void syncTimeNTP();
void getData();
void wakeUpVibration();
void sendData(bool type);
void printCurrentTime();
void setNextIntervalAlarm(int intervalSeconds);
void alarm_callback();
void handleVibrationEvent();
void handleTimerEvent();
void writeRegister(byte registerAddress, byte value);
long readRegister(byte registerAddress, int numBytes);
void dispOLED_env();
void dispOLED_vib();
void dispOLED_vibsensor_warn();

void setup() {
  Serial.begin(115200);

  pinMode(VIBRATION_PIN, INPUT_PULLUP); // 内部プルアップ有効
  pinMode(BUZZER_PIN, OUTPUT);
  pinMode(chipSelectPin, OUTPUT);
  digitalWrite(chipSelectPin, HIGH);    // CSは通常HIGH（非選択状態）にする

  dht.begin();
  RTC.begin();
  SPI.begin();
  u8g2.begin();
  u8g2.setFont(u8g2_font_ncenB08_tr);

  connectWiFi();
  syncTimeNTP();
  setNextIntervalAlarm(send_time);      // 最初のタイマー送信を設定

  // 振動センサのピンがHIGHからLOWに変わった時に割り込み発生
  attachInterrupt(digitalPinToInterrupt(VIBRATION_PIN), wakeUpVibration, FALLING);

  // 気圧センサ(SCP1000)を測定モードに設定
  writeRegister(0x03, 0x0A);
  delay(100);
}

void loop() {
  // WiFi接続が切れていたら再接続
  if (WiFi.status() != WL_CONNECTED) {
    connectWiFi();
    syncTimeNTP();
  }

  // 1秒ごとにセンサ値の更新と時刻の同期
  if (millis() - lastUpdate > 1000) {
    getData();
    syncTimeNTP();
    lastUpdate = millis();
  }

  // 振動センサの異常監視（長時間LOWのままなら警告表示）
  int pinStatus = digitalRead(VIBRATION_PIN);
  if (pinStatus == LOW) {
    if (low_start_time == 0) low_start_time = millis();
    if (millis() - low_start_time > vibrationsensor_lowtimeout) {
      Serial.println("vibrationsensor_low");
      dispOLED_vibsensor_warn();
    }
  } else {
    low_start_time = 0;
    if (!is_vibrating) dispOLED_env(); // 通常時は環境データをOLEDに表示
  }

  // 割り込みで振動を検知した場合の処理
  if (vibration_detected) {
    handleVibrationEvent();
    vibration_detected = false;
  }

  // RTCアラーム（定時送信）が発生した場合の処理
  if (alarm_fired) {
    handleTimerEvent();
    alarm_fired = false;
    setNextIntervalAlarm(send_time);
  }
  
  delay(100);
}

// --- OLED表示関連 ---
void dispOLED_env() {
  RTCTime now;
  RTC.getTime(now);
  u8g2.clearBuffer();          
  u8g2.setFont(u8g2_font_6x10_tr); 
  // 現在時刻、温度、湿度、気圧、照度を表示
  char timestr[20];
  sprintf(timestr, "%04d/%02d/%02d %02d:%02d",
          now.getYear(), (int)now.getMonth() + 1, now.getDayOfMonth(),
          now.getHour(), now.getMinutes());
  u8g2.setCursor(0, 9); u8g2.print(timestr);
  u8g2.setCursor(0, 15); u8g2.print("---------------------");
  u8g2.setCursor(0, 23); u8g2.print("Room : "); u8g2.print(room);
  u8g2.setCursor(0, 32); u8g2.print("Temp : "); u8g2.print(t, 1); u8g2.print(" C");
  u8g2.setCursor(0, 41); u8g2.print("Humid: "); u8g2.print(h, 1); u8g2.print(" %");
  u8g2.setCursor(0, 50); u8g2.print("Pres : "); u8g2.print(p, 1); u8g2.print(" hPa");
  u8g2.setCursor(0, 59); u8g2.print("Light: "); u8g2.print(l ? "Bright":"Dark");
  u8g2.sendBuffer(); 
}

void dispOLED_vib() {
  u8g2.clearBuffer(); 
  u8g2.setCursor(0, 9);
  u8g2.print("Vibration Detected!!!");
  u8g2.sendBuffer();
}

void dispOLED_vibsensor_warn() {
  u8g2.clearBuffer(); 
  u8g2.setCursor(0, 9);
  u8g2.print("Warning : Vib LOW!");
  u8g2.sendBuffer();
}

// --- データ取得 ---
void getData() {
  h = dht.readHumidity();
  t = dht.readTemperature();

  // 気圧データ読み取り
  unsigned long msb = readRegister(0x1F, 1); // 0x1F: DATARD8 (最上位)
  unsigned long lsb = readRegister(0x20, 2); // 0x20: DATARD16 (下位)

  // デバッグ用：MSB/LSBが共に0の場合、通信経路（はんだ付けやCSピン）に問題あり
  Serial.print("MSB: "); Serial.print(msb, HEX);
  Serial.print(" LSB: "); Serial.println(lsb, HEX);

  // SCP1000の計算式: 20bitデータを4で割ってPaにし、さらに100で割ってhPaにする
  unsigned long pressureRaw = ((msb & 0x07) << 16) | lsb;
  p = (pressureRaw / 4.0) / 100.0;

  // 照度センサの判定
  l = (analogRead(LIGHT_SENSOR_PIN) > light_threshold) ? true : false;
}

// --- ネットワーク関連 ---
void connectWiFi() {
  if (WiFi.status() == WL_CONNECTED) return;
  WiFi.config(local_IP, gateway, subnet);
  WiFi.begin(ssid, pass);
  int timeout = 0;
  while (WiFi.status() != WL_CONNECTED && timeout < 20) {
    delay(1000);
    timeout++;
  }
}

void syncTimeNTP() {
  if (WiFi.status() != WL_CONNECTED) return;
  timeClient.begin();
  if (timeClient.update()) {
    RTCTime currentTime(timeClient.getEpochTime());
    RTC.setTime(currentTime);
  }
  Serial.println("NTP Time Synchronized");
}

// JSONデータをHTTP POSTで送信
void sendData(bool type) {
  if (WiFi.status() != WL_CONNECTED) return;
  
  if (client.connect(server_ip, 80)) {
    StaticJsonDocument<200> doc;
    doc["type"] = "Arduino";
    doc["datetime"] = buf;
    doc["room"] = room;
    if (type) { // 定時環境データ
      doc["temp"] = t;
      doc["humid"] = h; 
      doc["press"] = p; 
      doc["light"] = l;
    } else {    // 振動検知データ
      doc["quake"] = (int)vibration_count;
    }

    String jsonString;
    serializeJson(doc, jsonString);

    client.println("POST /api/api_main.php HTTP/1.1");
    client.print("Host: "); client.println(server_ip);
    client.println("Content-Type: application/json");
    client.print("Content-Length: "); client.println(jsonString.length());
    client.println("Connection: close");
    client.println(); 
    client.print(jsonString);
    
    delay(100);
    client.stop();
    Serial.println(jsonString);
  }
}

// --- RTCアラーム設定 ---
void setNextIntervalAlarm(int intervalSeconds) {
  RTCTime now;
  RTC.getTime(now);
  // 次の送信タイミング（例：1時間ごと）を計算
  unsigned long nextEpoch = ((now.getUnixTime() / intervalSeconds) + 1) * intervalSeconds;
  RTCTime alarmTime(nextEpoch);
  AlarmMatch match;
  match.addMatchDay(); match.addMatchHour(); match.addMatchMinute(); match.addMatchSecond();
  RTC.setAlarmCallback(alarm_callback, alarmTime, match);
}

void alarm_callback() { alarm_fired = true; }

// --- SPIレジスタ操作 ---
void writeRegister(byte registerAddress, byte value) {
  // アドレスの2ビット左シフトと、書き込み指示ビット(0x02)をセット
  byte address = (registerAddress << 2) | 0x02;
  SPI.beginTransaction(scp1000Settings);
  digitalWrite(chipSelectPin, LOW);
  SPI.transfer(address);
  SPI.transfer(value);
  digitalWrite(chipSelectPin, HIGH);
  SPI.endTransaction();
}

long readRegister(byte registerAddress, int numBytes) {
  // 読み出し時は最下位2ビットを0にする
  byte address = registerAddress << 2;
  long result = 0;
  SPI.beginTransaction(scp1000Settings);
  digitalWrite(chipSelectPin, LOW);
  SPI.transfer(address);
  // 指定されたバイト数分データを読み出す
  for (int i = 0; i < numBytes; i++) {
    result = (result << 8) | SPI.transfer(0x00);
  }
  digitalWrite(chipSelectPin, HIGH);
  SPI.endTransaction();
  return result;
}

// --- イベントハンドラ ---
void wakeUpVibration() { vibration_detected = true; }

void handleVibrationEvent() {
  is_vibrating = true;
  vibration_count = 0;
  RTCTime startTime;
  RTC.getTime(startTime);
  last_vibration_time = millis();
  bool last_pin_state = HIGH;

  // 10秒間振動をカウントするループ
  while (millis() - last_vibration_time <= VIBRATION_TIMEOUT) {
    bool current_pin_state = digitalRead(VIBRATION_PIN);
    if (current_pin_state == LOW && last_pin_state == HIGH) {
      vibration_count++;
      last_vibration_time = millis();
      digitalWrite(BUZZER_PIN, HIGH);
      delay(50);
      digitalWrite(BUZZER_PIN, LOW);
    }
    last_pin_state = current_pin_state;
    // ループ中に定時タイマーが来ても対応できるようにする
    if (alarm_fired) {
      handleTimerEvent();
      alarm_fired = false;
      setNextIntervalAlarm(send_time);
    }
    dispOLED_vib();
    delay(1);
  }
  
  if (vibration_count > vibration_threshold) {
    sprintf(buf, "%04d-%02d-%02d %02d:%02d:%02d",
            startTime.getYear(), (int)startTime.getMonth() + 1, startTime.getDayOfMonth(),
            startTime.getHour(), startTime.getMinutes(), startTime.getSeconds());
    sendData(false);
  }
  is_vibrating = false;
}

void handleTimerEvent() {
  printCurrentTime();
  sendData(true);
}

void printCurrentTime() {
  RTCTime now;
  RTC.getTime(now);
  sprintf(buf, "%04d-%02d-%02d %02d:%02d:%02d",
          now.getYear(), (int)now.getMonth() + 1, now.getDayOfMonth(),
          now.getHour(), now.getMinutes(), now.getSeconds());
}
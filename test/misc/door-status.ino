// This #include statement was automatically added by the Particle IDE.
#include <HttpClient.h>

HttpClient http;

// Headers currently need to be set at init, useful for API keys etc.
http_header_t headers[] = {
     { "Content-Type", "application/json" },
    //  { "Accept" , "application/json" },
    // { "Accept" , "*/*"},
    { NULL, NULL } // NOTE: Always terminate headers will NULL
};


http_request_t request;
http_response_t response;

/*
    VALUES
    3000 to 4096 LOCKED & CLOSED
    2900 to 3000 CLOSED
         to 2900 OPEN
*/
int LOCKED_TRESHOLD = 3200;
int OPEN_TRESHOLD = 3000;


enum DOOR_STATUS {
    OPEN, UNLOCKED, LOCKED, UNKNOWN
};

int halleffectsensor = A0;
int halleffectvalue;

DOOR_STATUS status = UNKNOWN;

const int numReadings = 10;

int readings[numReadings];      // the readings from the analog input
int readIndex = 0;              // the index of the current reading
int total = 0;                  // the running total
int average = 0;                // the average



void setup() {
    pinMode(halleffectsensor, INPUT);
    Particle.variable("halleffect", &halleffectvalue, INT);
    
    emitEvent("start", "none");
    
}

void loop() {
  total = total - readings[readIndex];
  // read from the sensor:
  readings[readIndex] = analogRead(halleffectsensor);
  // add the reading to the total:
  total = total + readings[readIndex];
  // advance to the next position in the array:
  readIndex = readIndex + 1;

  // if we're at the end of the array...
  if (readIndex >= numReadings) {
    // ...wrap around to the beginning:
    readIndex = 0;
  }

  // calculate the average:
  halleffectvalue = total / numReadings;
  
  if (halleffectvalue < OPEN_TRESHOLD && status != OPEN) {
      status = OPEN;
      emitEvent("open", String(halleffectvalue));
  } 
  
  if (halleffectvalue > LOCKED_TRESHOLD && status != LOCKED) {
      status = LOCKED;
      emitEvent("locked", String(halleffectvalue));
  }
  
  if (halleffectvalue > OPEN_TRESHOLD && halleffectvalue < LOCKED_TRESHOLD && status != UNLOCKED) {
      status = UNLOCKED;
      emitEvent("unlocked", String(halleffectvalue));
  }
}

void emitEvent(String event, String data) {
    postData(event, data);
    Particle.publish(event, data);
}

void postData(String title, String data) {
    request.hostname = "10.1.10.26";
    request.port = 9000;
    request.path = "/events";

    // The library also supports sending a body with your request:
    //request.body = "{\"key\":\"value\"}";
    request.body = "{\"title\":\"" + title + "\", \"data\":\"" + data + "\"}";

    // Get request
    http.post(request, response, headers);
}
/* 
*  analogRead for TeensyDAQ
*  Reads 5 analog values every 125us (8000Hz).
*
*/
#include <IntervalTimer.h>

volatile int readPins[10];
const int numberOfPins = 5;
int ledpin = 13;
long timeIndex = 0;

IntervalTimer timer; // timer
IntervalTimer blinkTimer; // timer
int startTimerValue = 0;
String txtMsg = "";  
char s;

//http://meettechniek.info/embedded/arduino-analog.html
//http://dorkbotpdx.org/blog/paul/control_voltage_cv_to_analog_input_pin
//1.2V with analogReference(INTERNAL)

void setup() {  
    for(int i = 0 ; i < 10 ; i ++){ 
      pinMode(14+i, INPUT);
    }
    pinMode(ledpin, OUTPUT);
    
    //set analog read resolution
    analogReadResolution(13);
    
    //Initialize serial connection
    Serial.begin(115200);

    //Initialize the blinker interrupt
    blinkTimer.begin(blink_callback,1000000);
    
    //Initialize the reader interrupt
    setSampleRate(8000);
}

//elapsedMicros time;

void loop() {
      
  while (Serial.available() > 0) {
        s=(char)Serial.read();
        if (s == '\n') {
            int samplerate = txtMsg.substring(7, 11).toInt();
            setSampleRate(samplerate);
            txtMsg = ""; 
        } else {  
            txtMsg +=s; 
        }
    }
}

void setSampleRate(float sampleRate){
  timer.end();  
  for(int i = 0 ; i < 8 ; i++){
    digitalWrite(13, !digitalRead(13));
    delay(80);
  }
  int microsecondsDelay = 1/sampleRate * 1000000;
  timer.begin(timer_callback,microsecondsDelay);  
}

// This function will be called with the desired frequency
// start the measurement
// in my low-res oscilloscope this function seems to take 1.5-2 us.
void timer_callback(void) {
    //long elapsedus = time;
   
    int values[numberOfPins];
    for(int i = 0 ; i < numberOfPins ; i ++){
      values[i] = analogRead(14+i);
    }
    
    Serial.print("T");
    Serial.print(timeIndex,HEX);
    
    for(int i = 0 ; i < numberOfPins ; i ++){
      Serial.print(" ");
      Serial.print(values[i],HEX);     
    }
    
    Serial.println();
    
    timeIndex++;
}

void blink_callback(void) {
    digitalWrite(13, !digitalRead(13));
}


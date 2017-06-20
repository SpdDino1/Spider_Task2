package com.example.vikramkumaresan.proximitysensor;

import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

//Manifest Modified to get permission for prox_sensor

public class MainActivity extends AppCompatActivity {

    int run_check;
    Thread lag;
    Handler updater;
    Runnable time_lag;
    TextView timer;
    MediaPlayer mp;
    RelativeLayout back;
    boolean getout;     //To prevent the handler from refreshing the thread (After object is far from sensor)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        back = (RelativeLayout)findViewById(R.id.back);
        timer=(TextView)findViewById(R.id.timer);

        SensorManager manage = (SensorManager) getSystemService(SENSOR_SERVICE);    //There are many types of services. Tells that you want to manage Sensors of 'Sensor Service"
                                                                                    //....redundant
        final Sensor prox_sensor = manage.getDefaultSensor(Sensor.TYPE_PROXIMITY);  //What sensor are you targeting?

        if(prox_sensor==null){  //If no proximity sensor, getDefaultSensor() return null
            Toast.makeText(this,"No Proximity Sensor on Device",Toast.LENGTH_LONG).show();
            finish();
        }

        mp = MediaPlayer.create(this,R.raw.sound);    //Reference to the sound file. Should be placed in res/raw/

        SensorEventListener listener = new SensorEventListener() {  //For sensors, you have to set event listeners like this.
            @Override
            /* All sensors return a 'event[float]' array when their sensor values have changes. This is caught by the method below. The array is of size 3, but the proximity
            sensor only uses index 0. The other 2 indices are garbaged. Other sensors (Like gyroscope) may use all 3.
            So we only care of event.values[0]. This value gives us the distance of an object wrt the sensor. If it is less than th getMaxRange(), it means that the object is
            close to the phone (Like the ear while talking)
            Otherwise, there is nothing in front of the phone.
            */
            public void onSensorChanged(final SensorEvent event) {
                if(event.values[0]<prox_sensor.getMaximumRange()){
                    back.setBackgroundColor(Color.YELLOW);
                    getout=false;
                    run_check=10;   //To keep track of time (For printing)

                    updater = new Handler(    //Updates the text view
                            new Handler.Callback() {
                                @Override
                                public boolean handleMessage(Message msg) {
                                    if(!getout){
                                        if(run_check==0){   //Time to play (10s up)
                                            back.setBackgroundColor(Color.RED);
                                            mp.start();
                                            timer.setText(""+run_check);
                                        }
                                        else {  //Reset the thread (10s not up yet)
                                            timer.setText(""+run_check);
                                            lag.start();
                                        }
                                    }
                                    else {
                                        return_to_normal();
                                    }
                                    return true;
                                }
                            }
                    );

                    time_lag = new Runnable() {
                        @Override
                        public void run() {
                            SystemClock.sleep(1000);    //Sleep for 1s
                            run_check--;
                            updater.sendEmptyMessage(0);
                        }
                    };
                    lag = new Thread(time_lag);
                    lag.start();

                }
                else {
                    getout=true;
                    if (mp.isPlaying()){
                        return_to_normal();
                    }
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {

            }
        };

        manage.registerListener(listener,prox_sensor,SensorManager.SENSOR_DELAY_NORMAL);    //You have to register the sensor's listener, the listener, and the frequency of updation.
        //You can give manual updation values too (IN MICROSECONDS, so multiply with 10^6 to get seconds)
    }

    void return_to_normal(){
        back.setBackgroundColor(Color.GREEN);
        timer.setText(""+10);   //Reset text view
        if(mp.isPlaying()){
            mp.pause(); //To pause
            mp.seekTo(0);   //To offset it (manually) to the start of the track
        }
    };

}
/* The MediaPlayer can be pause() and start()           but not stop() and start()
   It has to be stop() and reset()  */
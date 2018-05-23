package com.project.safereturn;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Vibrator;
import android.support.v7.app.AppCompatActivity;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class DetectActivity extends AppCompatActivity implements SensorEventListener{

    private SensorManager sensorManager;
    private Sensor sensor;

    private long shakeTime;

    private MediaRecorder recorder;
    private Runnable runnable = new Runnable(){

        public void run(){
            Log.e("df", String.valueOf(getDecibel()));
        }
    };
    private Timer timerDecibel;
    private TimerTask timerTaskDecibel;
    private TextView textViewDecibel;

    private double latitudeA = MainActivity.latitude;
    private double longitudeA = MainActivity.longitude;
    private Timer timerLocation;
    private TimerTask timerTaskLocation;
    private int loopCount = -1;

    private SmsManager smsManager;

    private boolean isSendSMS = false;

    private ImageView imageViewCurLocation;

    private int time = 0;
    private Timer timer;
    private TimerTask timerTask;
    private TextView textViewTimer;

    private Vibrator vibrator;

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch(getResultCode()){
                case Activity.RESULT_OK: //전송 성공
                    Log.e(DetectActivity.class.getSimpleName(), "RESULT_OK");
                    break;
                case SmsManager.RESULT_ERROR_GENERIC_FAILURE: //전송실패
                    Log.e(DetectActivity.class.getSimpleName(), "RESULT_ERROR_GENERIC_FAILURE");
                    break;
                case SmsManager.RESULT_ERROR_NO_SERVICE: //서비스 지역 아님
                    Log.e(DetectActivity.class.getSimpleName(), "RESULT_ERROR_NO_SERVICE");
                    break;
                case SmsManager.RESULT_ERROR_RADIO_OFF: //무선 신호 꺼져있음
                    Log.e(DetectActivity.class.getSimpleName(), "RESULT_ERROR_RADIO_OFF");
                    break;
                case SmsManager.RESULT_ERROR_NULL_PDU:
                    Log.e(DetectActivity.class.getSimpleName(), "RESULT_ERROR_NULL_PDU");
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detect);

        registerReceiver(receiver, new IntentFilter("SMS_SENT_ACTION"));

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);

        sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);

        final double rangeDistance = Math.sqrt(0.001794 * 0.001794 + 0.002549 * 0.002549);


        textViewDecibel = (TextView) findViewById(
                R.id.textView_decibel);
        timerDecibel = new Timer();
        timerTaskDecibel = new TimerTask() {
            @Override
            public void run() {
                //handler.post(runnable);

                final int decibel = getDecibel();

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        textViewDecibel.setText("데시벨 : " + String.valueOf(decibel));
                    }
                });


                if(87 <  decibel) {
                    if(!isSendSMS && MyApplication.preferences.getBoolean("shouting", false)) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(getApplicationContext(), "데시벨 기능 ("+String.valueOf(decibel)+")이 감지되었습니다.", Toast.LENGTH_SHORT).show();
                            }
                        });
                        sendSMS();
                        isSendSMS = true;
                    }
                }
            }
        };

        timerTaskLocation = new TimerTask() {
            @Override
            public void run() {

                double latitudeB = MainActivity.latitude;
                double longitudeB = MainActivity.longitude;

                double dx = latitudeA - latitudeB;
                double dy = longitudeA - longitudeB;
                double AtoBDistance = Math.sqrt(dx * dx + dy * dy);

                if(AtoBDistance < rangeDistance) { //Yes
                    loopCount++;
                }
                else { //No
                    latitudeA = latitudeB;
                    longitudeA = longitudeB;

                    loopCount = 0; //과정 초기화
                }

                if(loopCount == 6) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(), "위치 고정이 감지되었습니다.", Toast.LENGTH_SHORT).show();
                        }
                    });
                    sendSMS();
                    isSendSMS = true;
                    timerLocation.cancel();
                    timerLocation = null;
                }

                Log.e("loopCount", String.valueOf(loopCount));
            }
        };
        timerLocation = new Timer();

        if(!isSendSMS && MyApplication.preferences.getBoolean("lock", false)) {
            timerLocation.schedule(timerTaskLocation, 60000, 60000);
        }
        recorder = new MediaRecorder();
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        recorder.setOutputFile(Environment.getExternalStorageDirectory() + "/Download/sample.mp4");

        try {
            recorder.prepare();
            recorder.start();
        } catch (IOException e) {
            e.printStackTrace();
        }

        timerDecibel.schedule(timerTaskDecibel, 0, 500);

        smsManager = SmsManager.getDefault();

        imageViewCurLocation = (ImageView) findViewById(R.id.imageView_cur_location);
        imageViewCurLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getApplicationContext(), MapActivity.class));
            }
        });

        findViewById(R.id.imageVIew_finish).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(DetectActivity.this);
                builder.setTitle("감지 종료");
                builder.setMessage("종료하시겠습니까?")
                        .setCancelable(true)
                        .setNegativeButton("취소", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.dismiss();
                            }
                        })
                        .setPositiveButton("확인", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.dismiss();
                                DetectActivity.this.finish();
                            }
                        });

                AlertDialog alertDialog = builder.create();
                alertDialog.show();
            }
        });


        textViewTimer = (TextView) findViewById(R.id.textView_timer);
        timer = new Timer();
        timerTask = new TimerTask() {
            @Override
            public void run() {
                time++;

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        String second = String.valueOf(time % 60);

                        if(time % 60 < 10) {
                            second = "0" + String.valueOf(time % 60);
                        }

                        textViewTimer.setText(String.valueOf(time / 60) + ":" + second);
                    }
                });
            }
        };

        timer.schedule(timerTask, 1000, 1000);
    }

    private int getAmplitude() {
        return  recorder.getMaxAmplitude();
    }

    private int getDecibel() {
        int amplitude = getAmplitude();

        if(!Double.isInfinite((20 * Math.log10(amplitude)))) {
            return (int)(20 * Math.log10(amplitude));
        }
        else {
            return -1;
        }
    }

    private void sendSMS() {
        String message = "위험한 상황에 처했습니다 도와주세요\nhttps://www.google.co.kr/maps/@"+String.valueOf(MainActivity.latitude)+","+String.valueOf(MainActivity.longitude);
        //String message = "https://maps.google.com/?q="+String.valueOf(MainActivity.latitude)+","+String.valueOf(MainActivity.longitude)";
        final String phone01 = MyApplication.preferences.getString("phone01", null);
        final String phone02 = MyApplication.preferences.getString("phone02", null);
        final String phone03 = MyApplication.preferences.getString("phone03", null);

        Log.e(phone01, "Df");

        final ArrayList<String> messages = smsManager.divideMessage(message);

        final ArrayList<PendingIntent> pendingIntentArrayList = new ArrayList<>();
        for(int i = 0; i < messages.size(); i++) {
            pendingIntentArrayList.add(PendingIntent.getBroadcast(this, 0, new Intent("SMS_SENT_ACTION"), 0));
        }

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    if(phone01 != null)
                        smsManager.sendMultipartTextMessage(phone01, null, messages, pendingIntentArrayList, null);
                }catch (Exception e) {
                    e.printStackTrace();
                }

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        try{
                            if(phone02 != null)
                                smsManager.sendMultipartTextMessage(phone02, null, messages, pendingIntentArrayList, null);
                        } catch(Exception e) {
                            e.printStackTrace();
                        }
                    }

                }, 500);

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        try{
                            if(phone03 != null)
                                smsManager.sendMultipartTextMessage(phone03, null, messages, pendingIntentArrayList, null);
                        } catch(Exception e) {
                            e.printStackTrace();
                        }
                    }
                }, 1000);
            }
        });
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if(sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float axisX = sensorEvent.values[0];
            float axisY = sensorEvent.values[1];
            float axisZ = sensorEvent.values[2];

            float gravityX = axisX / SensorManager.GRAVITY_EARTH;
            float gravityY = axisY / SensorManager.GRAVITY_EARTH;
            float gravityZ = axisZ / SensorManager.GRAVITY_EARTH;

            float f= gravityX * gravityX + gravityY * gravityY + gravityZ * gravityZ;
            double squaredD = Math.sqrt(f);
            float gForce = (float) squaredD;
            if(gForce > 2.7f) {
                long currentTime = System.currentTimeMillis();
                if(shakeTime + 500 > currentTime) {
                    return;
                }

                shakeTime = currentTime;

                Log.e("Asdf", String.valueOf(gForce));

                if(!isSendSMS && MyApplication.preferences.getBoolean("shake", false)) {
                    sendSMS();
                    Toast.makeText(getApplicationContext(), "흔들기 기능이 감지되었습니다.", Toast.LENGTH_SHORT).show();
                    vibrator.vibrate(500);
                    isSendSMS = true;
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    @Override
    protected void onDestroy() {
        sensorManager.unregisterListener(this);

        if(timerDecibel != null)
            timerDecibel.cancel();

        if(timerLocation != null)
            timerLocation.cancel();

        recorder.stop();
        recorder.release();
        recorder = null;

        unregisterReceiver(receiver);
        super.onDestroy();
    }
}

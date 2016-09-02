package com.trekken;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.RotateAnimation;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

public class FallDetection extends Activity {

    private SharedPreferences defaultPref;
    private ProgressBar pb;
    private Handler pbHandler;
    private Runnable r;
    private Ringtone alarm;
    private Vibrator v;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.popup_timer);

        /** LayoutInflater inflater = getLayoutInflater();
         View popupLayout = inflater.inflate(R.layout.popup_timer, (ViewGroup) MainActivity.this.findViewById(R.id.popup_layout));
         helpBuilder.setView(popupLayout);
         final AlertDialog helpDialog = helpBuilder.create();*/

        pb = (ProgressBar) this.findViewById(R.id.progressBarTimer);
        AnimationSet anSet = new AnimationSet(true);

        //Rotate 90 degrees
        Animation anRotate = new RotateAnimation(0.0f, 90.0f, 250f, 273f);
        anSet.addAnimation(anRotate);

        //Move back to the right
        Animation anTranslate = new TranslateAnimation(0f, 263f, 0f, 0f);
        anSet.addAnimation(anTranslate);

        anSet.setInterpolator(new DecelerateInterpolator());
        anSet.setFillAfter(true);

        //Animate the Progress Bar
        anSet.start();
        pb.startAnimation(anSet);

        //Setting up the alarm sound
        Uri uriAlarm;
        defaultPref = PreferenceManager.getDefaultSharedPreferences(this);
        final String alarmRingtone = defaultPref.getString("notifications_alarm_ringtone", "notFound");
        if (!alarmRingtone.equals("notFound")) {
            uriAlarm = Uri.parse(alarmRingtone);
        } else {
            uriAlarm = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        }
        alarm = RingtoneManager.getRingtone(getApplicationContext(), uriAlarm);
        alarm.play();
        v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        long[] pattern = {0, 100, 1000};
        if (defaultPref.getBoolean("notifications_new_message_vibrate", false))
            v.vibrate(pattern, 0);

        // Handler functioning as a Timer with tick = 1s
        // Decrease the value of the ProgressBar until stopped by Button or it will send an Emergency Sms
        pbHandler = new Handler();
        r = new Runnable() {
            public void run() {
                if (pb.getProgress() > 1) {
                    pb.setProgress(pb.getProgress() - 1);
                    if (!alarm.isPlaying())
                        alarm.play();
                    pbHandler.postDelayed(this, 500);
                } else {
                    pbHandler.removeCallbacks(this);
                    //pb.setProgress(45);
                    sendEmergencySMS();
                    alarm.stop();
                    v.cancel();
                    // startSensors();
                    // helpDialog.dismiss();

                    Intent returnIntent = new Intent();
                    setResult(2, returnIntent);
                    finish();
                }
            }
        };
        pbHandler.removeCallbacks(r);
        pbHandler.postDelayed(r, 500);

        //User stops the Alarm
        Button btnDismiss = (Button) this.findViewById(R.id.btnDismissPopup);
        btnDismiss.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Reset the AlertDialog and its elements
                pbHandler.removeCallbacks(r);
                alarm.stop();
                v.cancel();

                Intent returnIntent = new Intent();
                setResult(2, returnIntent);
                finish();
            }
        });
    }

    //Called in case user or Android cancel the AlertDialog
    @Override
    public void onStop() {
        super.onStop();
        //Reset the AlertDialog and its elements
        pbHandler.removeCallbacks(r);
        alarm.stop();
        v.cancel();

        Intent returnIntent = new Intent();
        setResult(2, returnIntent);
        finish();
    }

    private void sendEmergencySMS() {
        //Log.i("Send SMS", "");
        Intent mainIntent = getIntent();
        double lat = mainIntent.getDoubleExtra("latitude", 0.0);
        double lon = mainIntent.getDoubleExtra("longitude", 0.0);

        String phoneNo = defaultPref.getString("emergency_number", "banana");
        String user = defaultPref.getString("display_name", "banana");

        String message = "Trekken user " + user + " might be in danger while hiking and has requested your aid!";
        if (lat != 0.0)
            message += "http://www.google.com/maps/place/" + Double.toString(lat) + "," + Double.toString(lon);

        try {
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(phoneNo, null, message, null, null);
            Toast.makeText(this, "SMS sent.", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "SMS failed!", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }
}

package com.trekken;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.popup_timer);

        //TODO farlo funzionare anche a cell bloccato e replace helpdialog con finish
        //Toast.makeText(this, "Fall detected", Toast.LENGTH_SHORT).show();

        //AlertDialog setup, inflating his view and finding ProgressBar and Button
        //AlertDialog.Builder helpBuilder = new AlertDialog.Builder(this);
        //helpBuilder.setTitle("Fall Detected !");

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
        String alarmRingtone = defaultPref.getString("notifications_alarm_ringtone", "notFound");
        if (!alarmRingtone.equals("notFound")) {
            uriAlarm = Uri.parse(alarmRingtone);
        } else {
            uriAlarm = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        }
        alarm = RingtoneManager.getRingtone(getApplicationContext(), uriAlarm);

        // Launching the Popup Dialog
        // helpDialog.setCancelable(false);
        // helpDialog.show();

        alarm.play();

        // Handler functioning as a Timer with tick = 1s
        // Decrease the value of the ProgressBar until stopped by Button or it will send an Emergency Sms
        pbHandler = new Handler();
        r = new Runnable() {
            public void run() {
                if (pb.getProgress() > 1) {
                    pb.setProgress(pb.getProgress() - 1);
                    pbHandler.postDelayed(this, 500);
                } else {
                    pbHandler.removeCallbacks(this);
                    //pb.setProgress(45);
                    sendEmergencySMS();
                    alarm.stop();
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
                // startSensors();
                // helpDialog.dismiss();

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
        // startSensors();

        Intent returnIntent = new Intent();
        setResult(2, returnIntent);
        finish();
    }

    private void sendEmergencySMS() {
        //Log.i("Send SMS", "");
        String phoneNo = defaultPref.getString("emergency_number", "banana");
        String user = defaultPref.getString("display_name", "banana");

        String message = "Trekken user " + user + " might be in danger while hiking and has requested your aid!";

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

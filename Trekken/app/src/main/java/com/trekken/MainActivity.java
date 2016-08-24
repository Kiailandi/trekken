package com.trekken;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.SwitchCompat;
import android.telephony.SmsManager;
import android.util.Log;
import android.util.StringBuilderPrinter;
import android.view.LayoutInflater;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.RotateAnimation;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private NavigationView navigationView;
    private View headerLayout;
    private SharedPreferences defaultPref;
    private SharedPreferences sharedPref;
    private SensorManager snrManager;
    private Sensor snrAccelerometer;
    private SensorEventListener listenerAccelerometer;
    private double rootSquare;
    static final double threshold = 3.0; //TODO rimettere 1.5 a fine test
    Handler pbHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        snrManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        snrAccelerometer = snrManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        listenerAccelerometer = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent sensorEvent) {
                snrAccelerometer = sensorEvent.sensor;
                //Log.i("Accelerometer1", (Float.toString(sensorEvent.values[0])));
                //Log.i("Accelerometer2", (Float.toString(sensorEvent.values[1])));
                //Log.i("Accelerometer3", (Float.toString(sensorEvent.values[2])));
                if (snrAccelerometer.getType() == Sensor.TYPE_ACCELEROMETER) {
                    //Vectorial sum of x,y,z axis
                    rootSquare = Math.sqrt(Math.pow(sensorEvent.values[0], 2) + Math.pow(sensorEvent.values[1], 2) + Math.pow(sensorEvent.values[2], 2));
                    if (rootSquare < threshold) // threshold detecting free fall of the phone, lower is more precise
                    {
                        stopSensors();
                        fallDetected();
                    }
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int i) {

            }
        };

        //Caricamento email utente
        sharedPref = this.getSharedPreferences("login_preferences", Context.MODE_PRIVATE);
        final String emailPreferences = sharedPref.getString("email", "rospo");

        //Caricamento Dysplay Name
        defaultPref = PreferenceManager.getDefaultSharedPreferences(this);
        String dysplayName = defaultPref.getString("display_name", "banana");

        //Caricamento immagine utente
        Resources res = getResources();
        Bitmap src = BitmapFactory.decodeResource(res, R.drawable.rospo);
        final RoundedBitmapDrawable dr = RoundedBitmapDrawableFactory.create(res, src);
        dr.setCornerRadius(Math.max(src.getWidth(), src.getHeight()) / 2.0f);

        //Trovo la view della Nav Bar per cambiare gli elementi all interno (senza inflate)
        navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        headerLayout = navigationView.getHeaderView(0);

        //Metto email utente da SharedPreferences
        TextView txtEmail = (TextView) headerLayout.findViewById(R.id.textView);
        txtEmail.setText(emailPreferences);

        //Metto Dysplay Name utente da DefaultSharedPreferences
        TextView txtName = (TextView) headerLayout.findViewById(R.id.textViewName);
        txtName.setText(dysplayName);

        //Metto immagine utente
        ImageView imgProfilo = (ImageView) headerLayout.findViewById(R.id.imageProfile);
        imgProfilo.setImageDrawable(dr);

        // Material Design
        final DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close) {

            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                //getActionBar().setTitle(mTitle);
            }

            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                //getActionBar().setTitle(mDrawerTitle);

                //Testing stuff
                final SwitchCompat swCompat = (SwitchCompat) findViewById(R.id.switchForActionBar);
                if (!swCompat.isChecked()) {
                    ImageView img = (ImageView) findViewById(R.id.imageView2);
                    //img.setBackgroundColor(Color.BLUE);
                    img.setImageResource(R.drawable.rospo);
                }

                swCompat.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (swCompat.isChecked())
                            startSensors();
                        else
                            stopSensors();

                    }
                });
            }
        };

        drawer.addDrawerListener(toggle);
        toggle.syncState();

        startSensors();
    }

    @Override
    protected void onStart() {
        super.onStart();

        //Caricamento Dysplay Name
        defaultPref = PreferenceManager.getDefaultSharedPreferences(this);
        String dysplayName = defaultPref.getString("display_name", "banana");

        //Metto Dysplay Name utente da DefaultSharedPreferences
        TextView txtName = (TextView) headerLayout.findViewById(R.id.textViewName);
        txtName.setText(dysplayName);
    }

    private void stopSensors() {
        snrManager.unregisterListener(listenerAccelerometer);
    }

    private void startSensors() {
        snrManager.registerListener(listenerAccelerometer, snrAccelerometer, SensorManager.SENSOR_DELAY_UI);
    }

    private void fallDetected() {
        //TODO farlo funzionare anche a cell bloccato
        //Toast.makeText(this, "Fall detected", Toast.LENGTH_SHORT).show();

        //AlertDialog setup, inflating his view and finding ProgressBar and Button
        AlertDialog.Builder helpBuilder = new AlertDialog.Builder(this);
        helpBuilder.setTitle("Fall Detected !");

        LayoutInflater inflater = getLayoutInflater();
        View popupLayout = inflater.inflate(R.layout.popup_timer, (ViewGroup) MainActivity.this.findViewById(R.id.popup_layout));
        helpBuilder.setView(popupLayout);
        final AlertDialog helpDialog = helpBuilder.create();

        final ProgressBar pb = (ProgressBar) popupLayout.findViewById(R.id.progressBarTimer);
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
        Uri uriAlarm = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        final Ringtone alarm = RingtoneManager.getRingtone(getApplicationContext(), uriAlarm);

        Button btnDismiss = (Button) popupLayout.findViewById(R.id.btnDismissPopup);
        btnDismiss.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                alarm.stop();
                startSensors();
                helpDialog.dismiss();
            }
        });

        helpDialog.show();
        alarm.play();

        // Handler functioning as a Timer with tick = 1s
        // Decrease the value of the ProgressBar until stopped by Button or it will send an Emergency Sms
        pbHandler = new Handler();
        final Runnable r = new Runnable() {
            public void run() {
                if (pb.getProgress() > 1)
                    pb.setProgress(pb.getProgress() - 1);
                else {
                    sendEmergencySMS();
                    alarm.stop();
                    startSensors();
                    helpDialog.dismiss();
                }
                pbHandler.postDelayed(this, 1000);
            }
        };
        pbHandler.postDelayed(r, 1000);
    }

    private void sendEmergencySMS() {
        //Log.i("Send SMS", "");
        String phoneNo = defaultPref.getString("emergency_number", "banana");
        String user = defaultPref.getString("display_name", "banana");

        String message = "Trekken user " + user + " might be in danger while hiking and has request your aid!";

        try {
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(phoneNo, null, message, null, null);
            Toast.makeText(this, "SMS sent.", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "SMS failed!", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }


    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_camera) {
            // Handle the camera action
        } else if (id == R.id.nav_gallery) {
            Toast.makeText(this, "gallery pressed", Toast.LENGTH_SHORT).show();

        } else if (id == R.id.nav_slideshow) {

        } else if (id == R.id.nav_manage) {
            Intent intent = new Intent(this, SettingsActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            //finish();
            startActivity(intent);

        } else if (id == R.id.nav_signout) {
            //Delete stored informations about user's login
            SharedPreferences sharedPref = this.getSharedPreferences("login_preferences", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.remove("logged");
            editor.remove("email");
            editor.apply();

            //Going back to Login Activity
            Intent intent = new Intent(this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            finish(); //calls onDestroy()
            startActivity(intent);

        } else if (id == R.id.nav_close) {
            finish(); //calls onDestroy(), free some resources
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        //When an item in the Drawer gets pressed, close the Drawer
        return true;
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }



}

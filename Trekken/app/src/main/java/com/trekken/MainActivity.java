package com.trekken;

import android.content.Context;
import android.content.DialogInterface;
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
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.IntentSender;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.widget.EditText;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, DialogInterface.OnCancelListener, OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    private NavigationView navigationView;
    private View headerLayout;
    private SharedPreferences defaultPref;
    private SharedPreferences sharedPref;
    private SensorManager snrManager;
    private Sensor snrAccelerometer;
    private SensorEventListener listenerAccelerometer;
    private double rootSquare;
    static final double threshold = 3.0; //TODO rimettere 1.5 a fine test
    private ProgressBar pb;
    private Handler pbHandler;
    private Runnable r;
    private Ringtone alarm;

    EditText txtLog;
    Button btnLog;
    Button btnStart;
    Button btnStop;
    Button btnLoad;

    ArrayList<LatLng> pathPoints;
    Polyline line;
    ArrayList<Float> pathPointsAccuracy;

    GoogleApiClient googleApiClient;
    LocationRequest mLocationRequest;
    Location mCurrentLocation;
    LocationSettingsRequest.Builder builder;

    //Check invertire
    static final long time_interval = 1000 * 5;      //Millisecondi
    static final long fastest_time_interval = 1000 * 3;    //Millisecondi

    String mLastUpdateTime;
    GoogleMap gMap = null;
    boolean afterOnConnected = false;

    static final int lineWidth = 6;

    File filepath;
    FileWriter writer;

    int waitToStart = 0;

    String _pathLoad;

    private ArrayList<LatLng> readFromFile() {
        ArrayList<LatLng> ret = null;
        BufferedReader inputStream = null;

        try {
            ret = new ArrayList<>();
            inputStream = new BufferedReader(new FileReader(_pathLoad));

            String receiveString;

            while ((receiveString = inputStream.readLine()) != null) {

                Log.d("Carica: ", receiveString);

                if (receiveString.contains(";")) {
                    String[] split = receiveString.split(";");
                    ret.add(new LatLng(Double.valueOf(split[1]), Double.valueOf(split[2])));
                }
            }

            inputStream.close();
        } catch (FileNotFoundException e) {
            Log.e("File", "File not found: " + e.toString());
        } catch (IOException e) {
            Log.e("File", "Can not read file: " + e.toString());
        }

        return ret;
    }

    protected void writeLogsAndroid(String msg) {
        txtLog.append(msg);
        Log.d("MapActivity", msg);
    }

    protected boolean checkGpsEnabled() {

        String msg = "Per una maggiore precisione ti consigliamo di attivare il gps in modalità precisione elevata, vuoi abilitarlo?";
        boolean gpsEnabled = ((LocationManager) this.getSystemService(LOCATION_SERVICE)).isProviderEnabled(LocationManager.GPS_PROVIDER);

        if (!gpsEnabled) {
            AlertDialog ad = new AlertDialog.Builder(MainActivity.this)
                    .setTitle("Attiva GPS")
                    .setMessage(msg)
                    .setPositiveButton("Vai a impostazioni", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {

                            Intent gpsOptionsIntent = new Intent(
                                    android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                            startActivity(gpsOptionsIntent);
                        }
                    })
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
        }

        return ((LocationManager) this.getSystemService(LOCATION_SERVICE)).isProviderEnabled(LocationManager.GPS_PROVIDER) || ((LocationManager) this.getSystemService(LOCATION_SERVICE)).isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    protected boolean writeLogs() {
        File root = new File(Environment.getExternalStorageDirectory(), "Notes");

        if (!root.exists()) {
            root.mkdirs();
        }

        filepath = new File(root, "Maps_Gps_LatLon" + ".txt");  // file path to save

        try {
            writer = new FileWriter(filepath);

            if (pathPoints.size() > 0 && pathPoints != null) {
                for (int i = 0; i < pathPoints.size(); i++)
                    writer.append((i + 1) + ";" + pathPoints.get(i).latitude + ";" + pathPoints.get(i).longitude + ";" + pathPointsAccuracy.get(i) + "\r\n");

                writer.flush();
                Toast.makeText(getApplicationContext(), filepath.getName() + " creato! " + pathPoints.size() + " records", Toast.LENGTH_LONG).show();
            }

        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    protected void createLocationRequest() {
        mLocationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(time_interval)
                .setFastestInterval(fastest_time_interval);
    }

    private boolean isGooglePlayServicesAvailable() {
        GoogleApiAvailability googleAPI = GoogleApiAvailability.getInstance();
        int result = googleAPI.isGooglePlayServicesAvailable(this);
        if (result != ConnectionResult.SUCCESS) {
            if (googleAPI.isUserResolvableError(result)) {
                googleAPI.getErrorDialog(this, result, 9000).show();
            }
            return false;
        }
        return true;
    }

    protected void handleNewLocation(Location location) {
        if (waitToStart == 0) {
            txtLog.append(" \nhandleNewLocation .........");
            mCurrentLocation = location;

            if (mCurrentLocation != null) {
                double lat = mCurrentLocation.getLatitude();
                double lng = mCurrentLocation.getLongitude();

                txtLog.append(" \n\nAt Time: " + mLastUpdateTime + "\n" +
                        "Latitude: " + lat + "\n" +
                        "Longitude: " + lng + "\n" +
                        "Accuracy: " + mCurrentLocation.getAccuracy() + "\n" +
                        "Provider: " + mCurrentLocation.getProvider() + "\n");

                final LatLng currentPosition = new LatLng(lat, lng);

                if (!afterOnConnected)
                    gMap.animateCamera(CameraUpdateFactory.newLatLng(currentPosition));
                else {
                    gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentPosition, 14));
                    afterOnConnected = false;
                }

                pathPoints.add(currentPosition);
                pathPointsAccuracy.add(mCurrentLocation.getAccuracy());

                PolylineOptions options = new PolylineOptions().width(lineWidth).color(Color.RED).geodesic(true).addAll(pathPoints);
                line = gMap.addPolyline(options);
            } else {
                txtLog.append(" \nLocation is null .........");

                final LatLng besenello = new LatLng(45.940966, 11.1091463);
                gMap.addMarker(new MarkerOptions().position(besenello).title("Marker a Besenello"));
                gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(besenello, 14));
            }

            txtLog.setSelection(txtLog.getText().length());
        } else
            waitToStart--;
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == -1) {
            _pathLoad = data.getStringExtra("GetPath") + "/" + data.getStringExtra("GetFileName");
//            Toast.makeText(this, data.getStringExtra("GetPath") + "/" + data.getStringExtra("GetFileName"),
//                    Toast.LENGTH_LONG).show();

            ArrayList<LatLng> loadedPathPoint = readFromFile();
            PolylineOptions options = new PolylineOptions().width(lineWidth).color(Color.RED).geodesic(true).addAll(loadedPathPoint);
            line = gMap.addPolyline(options);
        } else {
            Toast.makeText(this, "Nessun file selezionato",
                    Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onMapReady(GoogleMap map) {
        txtLog.append(" \nonMapReady initiated .........");
        gMap = map;
        gMap.setMyLocationEnabled(true);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Checking the presence of googlePlayServices
        if (!isGooglePlayServicesAvailable()) {
            finish(); // drastic!
        }

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, SignInActivity.class);
                startActivity(intent);
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        //region Button Listeners Setup
        final SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        txtLog = (EditText) findViewById(R.id.txtLog);
        btnStart = (Button) findViewById(R.id.btnStart);

        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                writeLogsAndroid("btnStart .........");
                googleApiClient.connect();
            }
        });

        btnStop = (Button) findViewById(R.id.btnStop);

        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                writeLogsAndroid("btnStop .........");
                googleApiClient.disconnect();
            }
        });

        btnLog = (Button) findViewById(R.id.btnLog);

        btnLog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                writeLogs();
            }
        });

        btnLoad = (Button) findViewById(R.id.btnLoad);

        btnLoad.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent myIntent = new Intent(MainActivity.this, FileBrowserActivity.class);
                myIntent.putExtra("key", 15); //Optional parameters
                //MainActivity.this.startActivity(myIntent);
                startActivityForResult(myIntent, 100);
            }
        });

        txtLog.setTextIsSelectable(true);
        txtLog.setText("Map_v2 onCreate " + DateFormat.getTimeInstance().format(new Date()) + " .........");
        //endregion

        //region Accelerometer
        snrManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        snrAccelerometer = snrManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
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
        //endregion

        //region Getting Data
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
        //endregion

        //region Material Design
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
        //endregion

        pathPoints = new ArrayList<>();
        pathPointsAccuracy = new ArrayList<>();

        googleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(AppIndex.API).build();

        //Starting GPS and Accelerometer Services
        createLocationRequest();
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

    @Override
    protected void onPause() {
        super.onPause();
        if (googleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, this);
            googleApiClient.disconnect();
            writeLogsAndroid(" \nonPause Location update stopped .........");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        writeLogsAndroid(" \nonResume .........");

        googleApiClient.connect();
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

        pb = (ProgressBar) popupLayout.findViewById(R.id.progressBarTimer);
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
        alarm = RingtoneManager.getRingtone(getApplicationContext(), uriAlarm);

        helpBuilder.setOnCancelListener(this);
        helpDialog.show();
        alarm.play();

        // Handler functioning as a Timer with tick = 1s
        // Decrease the value of the ProgressBar until stopped by Button or it will send an Emergency Sms
        pbHandler = new Handler();
        r = new Runnable() {
            public void run() {
                if (pb.getProgress() > 1) {
                    pb.setProgress(pb.getProgress() - 1);
                    pbHandler.postDelayed(this, 500);
                }
                else {
                    pb.setProgress(45);
                    sendEmergencySMS();
                    alarm.stop();
                    startSensors();
                    helpDialog.dismiss();
                    pbHandler.removeCallbacks(this);
                }
            }
        };
        pbHandler.removeCallbacks(r);
        pbHandler.postDelayed(r, 500);

        //User stops the Alarm
        Button btnDismiss = (Button) popupLayout.findViewById(R.id.btnDismissPopup);
        btnDismiss.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Reset the AlertDialog and its elements
                pbHandler.removeCallbacks(r);
                alarm.stop();
                startSensors();
                helpDialog.dismiss();
            }
        });
    }

    //Called in case user or Android cancel the AlertDialog
    @Override
    public void onCancel(DialogInterface dialog) {
        //Reset the AlertDialog and its elements
        pbHandler.removeCallbacks(r);
        pb.setProgress(45);
        alarm.stop();
        startSensors();
        dialog.dismiss();
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

    //GoogleApiClient.ConnectionCallbacks provides call back for GoogleApiClient onConnected.
    @Override
    public void onConnected(@Nullable Bundle bundle) {
        writeLogsAndroid(" \nonConnected - isConnected : " + googleApiClient.isConnected());

        if (googleApiClient.isConnected()) {
            if (checkGpsEnabled()) {
                LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, mLocationRequest, this);  //return this PendingResult<Status> pendingResult;
                afterOnConnected = true;
            }
        }
    }

    //LocationListener provides call back for location change through onLocationChanged.
    @Override
    public void onLocationChanged(Location location) {
        writeLogsAndroid(" \nFiring onLocationChanged .........");
        mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());
        handleNewLocation(location);
    }

    //GoogleApiClient.OnConnectionFailedListener provides call back for GoogleApiClient onConnectionFailed.
    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        if (connectionResult.hasResolution()) {
            try {
                // Start an Activity that tries to resolve the error
                connectionResult.startResolutionForResult(this, 9000);
            } catch (IntentSender.SendIntentException e) {
                e.printStackTrace();
            }
        } else {
            writeLogsAndroid(" \nConnection failed: " + connectionResult.toString());
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        writeLogsAndroid(" \nonConnectionSuspended .........");
    }
}


package com.trekken;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.math.RoundingMode;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {
//      Ex CaricaLog -> Chiama il FileBrowser
//    Intent myIntent = new Intent(MainActivity.this, FileBrowserActivity.class);
//    myIntent.putExtra("key", 15); //Optional parameters
//    //MainActivity.this.startActivity(myIntent);
//    startActivityForResult(myIntent, 100);

    private NavigationView navigationView;
    private View headerLayout;
    private SharedPreferences defaultPref;
    private SharedPreferences sharedPref;
    private SensorManager snrManager;
    private Sensor snrAccelerometer;
    private SensorEventListener listenerAccelerometer;
    private double rootSquare;
    static final double threshold = 1.0; //TODO rimettere 1.5 a fine test
    private DataSnapshot paths;

    Button btnLog;
    Button btnStart;
    Button btnStop;
    Button btnLoad;
    Button btnNear;

    ArrayList<LatLng> pathPoints, pointsFromDb;
    ArrayList<String> nearpaths;
    Polyline line;
    ArrayList<Float> pathPointsAccuracy;

    GoogleApiClient googleApiClient;
    LocationRequest mLocationRequest;
    Location mCurrentLocation;
    //LocationSettingsRequest.Builder builder;

    //Check invertire
    static final long time_interval = 1000 * 5;      //Millisecondi
    static final long fastest_time_interval = 1000 * 3;    //Millisecondi

    //private FirebaseAuth mAuth;
    private DatabaseReference mRef;

    String mLastUpdateTime;
    GoogleMap gMap = null;
    boolean afterOnConnected = false;

    static final int lineWidth = 6;

    File filepath;
    FileWriter writer;

    int waitToStart = 1, trackColor, trackColorNear;
    String _pathLoad;

    final double radius = 1.24274;

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

    protected void lookForNearPaths(){
        nearpaths = new ArrayList<>();
        pointsFromDb = new ArrayList<>();

        DataSnapshot tmp;

        for (DataSnapshot path : paths.getChildren()) {
            tmp = path.child("points").child("0");
            if(isInRadius(new LatLng(Double.parseDouble(tmp.child("latitude").getValue().toString()), Double.parseDouble(tmp.child("longitude").getValue().toString())))){
                nearpaths.add(path.getKey().toString());
            }
        }

        gMap.clear();

        int i = 0;

        for(String key : nearpaths) {
            for(DataSnapshot point : paths.child(key).child("points").getChildren()){
                LatLng tmpPoint = new LatLng(Double.parseDouble(point.child("latitude").getValue().toString()), Double.parseDouble(point.child("longitude").getValue().toString()));
                pointsFromDb.add(tmpPoint);
                //disegna percorso qui
                PolylineOptions options = new PolylineOptions().width(lineWidth).color(trackColorNear).geodesic(true).addAll(pointsFromDb);
                line = gMap.addPolyline(options);

                if(i == 0) {
                    gMap.addMarker(new MarkerOptions().position(tmpPoint).title("Start point"));
                    i++;
                }
            }

            pointsFromDb.clear();
            i = 0;
        }

        nearpaths.clear();
    }

    protected boolean isInRadius(LatLng pos){
//        a = Math.Acos(Math.Sin(LatFromDb * 0.0175) * Math.Sin(LatCurrentPos * 0.0175)
//                + Math.Cos(LatFromDb * 0.0175) * Math.Cos(LatCurrentPos * 0.0175)
//                * Math.Cos((LonCurrentPos * 0.0175) - (LonFromDb * 0.0175))) * 3959;
//
//        a <= 1.24274 (2Km)

        double latFromDb = pos.latitude;
        double lonFromDb = pos.longitude;
        double latCurrentPos = mCurrentLocation.getLatitude();
        double lonCurrentPos = mCurrentLocation.getLongitude();

        double tmp = Math.acos(Math.sin(latFromDb * 0.0175) * Math.sin(latCurrentPos * 0.0175)
                        + Math.cos(latFromDb * 0.0175) * Math.cos(latCurrentPos * 0.0175)
                        * Math.cos((lonCurrentPos * 0.0175) - (lonFromDb * 0.0175))) * 3959;

        return tmp <= radius;
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
        mRef = FirebaseDatabase.getInstance().getReference();
        try {
            writer = new FileWriter(filepath);
            String key = mRef.child("paths").push().getKey();
            mRef.child("paths/" + key + "/creator").setValue(FirebaseAuth.getInstance().getCurrentUser().getUid());
            if (pathPoints.size() > 0 && pathPoints != null) {
                for (int i = 0; i < pathPoints.size(); i++) {
                    mRef.child("paths/" + key + "/points/" + i + "/latitude").setValue(pathPoints.get(i).latitude);
                    mRef.child("paths/" + key + "/points/" + i + "/longitude").setValue(pathPoints.get(i).longitude);
                    mRef.child("paths/" + key + "/points/" + i + "/precision").setValue(pathPointsAccuracy.get(i));
                    writer.append((i + 1) + ";" + pathPoints.get(i).latitude + ";" + pathPoints.get(i).longitude + ";" + pathPointsAccuracy.get(i) + "\r\n");
                }
                writer.flush();
                //TODO resettare qui polyline e array
                //pathPoints.clear();
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
            Log.e("LogsFunctions", " \nhandleNewLocation .........");
            mCurrentLocation = location;

            if (mCurrentLocation != null) {
                double lat = mCurrentLocation.getLatitude();
                double lng = mCurrentLocation.getLongitude();

                Log.d("LogsFunctions", " \n\nAt Time: " + mLastUpdateTime + "\n" +
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

                DecimalFormat df = new DecimalFormat("#.#####");
                df.setRoundingMode(RoundingMode.CEILING);

                if(pathPoints.size() >= 1 && !(df.format(pathPoints.get(pathPoints.size() - 1).latitude).equals(df.format(mCurrentLocation.getLatitude())) && df.format(pathPoints.get(pathPoints.size() - 1).longitude).equals(df.format(mCurrentLocation.getLongitude())))) {
                    pathPoints.add(currentPosition);
                    pathPointsAccuracy.add(mCurrentLocation.getAccuracy());
                }
                else if(pathPoints.size() < 1){
                    pathPoints.add(currentPosition);
                    pathPointsAccuracy.add(mCurrentLocation.getAccuracy());
                }

                Log.d("Coordinate", "Size: " + pathPoints.size());
                for(int i = 0; i < pathPoints.size(); i++){
                    Log.d("Coordinate", pathPoints.get(i).latitude + " " + pathPoints.get(i).longitude);
                }

                PolylineOptions options = new PolylineOptions().width(lineWidth).color(trackColor).geodesic(true).addAll(pathPoints);
                line = gMap.addPolyline(options);
            } else {
                Log.e("LogsFunctions", " \nLocation is null .........");

//                final LatLng besenello = new LatLng(45.940966, 11.1091463);
//                gMap.addMarker(new MarkerOptions().position(besenello).title("Marker a Besenello"));
//                gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(besenello, 14));
            }

        } else
            waitToStart--;
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        //File Browser
        if (resultCode == -1) {
            _pathLoad = data.getStringExtra("GetPath") + "/" + data.getStringExtra("GetFileName");
//            Toast.makeText(this, data.getStringExtra("GetPath") + "/" + data.getStringExtra("GetFileName"),
//                    Toast.LENGTH_LONG).show();

            ArrayList<LatLng> loadedPathPoint = readFromFile();
            PolylineOptions options = new PolylineOptions().width(lineWidth).color(trackColor).geodesic(true).addAll(loadedPathPoint);
            line = gMap.addPolyline(options);
        }
        // Fall Detection
        else if (requestCode == 2) {
            startSensors();
        } else
            Toast.makeText(this, "Nessun file selezionato", Toast.LENGTH_LONG).show();

    }

    @Override
    public void onMapReady(GoogleMap map) {
        Log.e("LogsFunctions", " \nonMapReady initiated .........");
        gMap = map;
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
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

        //Setup design elements
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
        btnStart = (Button) findViewById(R.id.btnStart);

        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.e("LogsFunctions", "btnStart .........");
                googleApiClient.connect();
            }
        });

        btnStop = (Button) findViewById(R.id.btnStop);

        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.e("LogsFunctions", "btnStop .........");
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

            }
        });

        btnNear = (Button)findViewById(R.id.btnNear);

        btnNear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                lookForNearPaths();
            }
        });

        Log.e("LogsFunctions", "Map_v2 onCreate " + DateFormat.getTimeInstance().format(new Date()) + " .........");
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
                        // fallDetected();
                        Intent i = new Intent(MainActivity.this, FallDetection.class);
                        if (mCurrentLocation != null) {
                            i.putExtra("latitude", mCurrentLocation.getLatitude());
                            i.putExtra("longitude", mCurrentLocation.getLongitude());
                        }
                        startActivityForResult(i, 2);
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

        //Caricamento Display Name
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

        //Metto Display Name utente da DefaultSharedPreferences
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
                        if (swCompat.isChecked()) {
                            if (defaultPref.getBoolean("fall_detection", true))
                                startSensors();
                            Log.e("LogsFunctions", " \nonResume .........");
                            googleApiClient.connect();
                        } else {
                            stopSensors();
                            if (googleApiClient.isConnected()) {
                                LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, MainActivity.this);
                                googleApiClient.disconnect();
                                Log.e("LogsFunctions", " \nonPause Location update stopped .........");
                            }
                        }

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
        if (defaultPref.getBoolean("fall_detection", true))
            startSensors();

        mRef = FirebaseDatabase.getInstance().getReference(); //TODO inizializzarlo all inizio

        mRef.child("paths/").addValueEventListener(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        paths = dataSnapshot;
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                    }
                });
    }

    @Override
    protected void onStart() {
        super.onStart();

        //Caricamento Display Name
        defaultPref = PreferenceManager.getDefaultSharedPreferences(this);
        String dysplayName = defaultPref.getString("display_name", "banana");

        //Metto Display Name utente da DefaultSharedPreferences
        TextView txtName = (TextView) headerLayout.findViewById(R.id.textViewName);
        txtName.setText(dysplayName);

        String color = defaultPref.getString("color_list", "-1");
        switch (color) {
            case "1":
                trackColor = ContextCompat.getColor(this, R.color.colorPrimary);
                break; //Green
            case "0":
                trackColor = ContextCompat.getColor(this, R.color.colorPrimary3);
                break; //Blue
            default:
                trackColor = ContextCompat.getColor(this, R.color.colorPrimary2);
                break; //Red
        }

        trackColorNear = ContextCompat.getColor(this, R.color.yellow);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (googleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, this);
            googleApiClient.disconnect();
            Log.e("LogsFunctions", " \nonPause Location update stopped .........");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.e("LogsFunctions", " \nonResume .........");
        googleApiClient.connect();
    }

    private void stopSensors() {
        snrManager.unregisterListener(listenerAccelerometer);
    }

    private void startSensors() {
        snrManager.registerListener(listenerAccelerometer, snrAccelerometer, SensorManager.SENSOR_DELAY_UI);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        DataSnapshot tmp;
        if (id == R.id.nav_my_paths) {
            mRef = FirebaseDatabase.getInstance().getReference();
            pointsFromDb = new ArrayList<>();
            mRef.child("paths/").addValueEventListener(
                    new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            Iterator<DataSnapshot> dataIterator = dataSnapshot.getChildren().iterator();
                            DataSnapshot pointsIterator;
                            LatLng tmpPoint;
                            boolean first = true;

                            gMap.clear();
                            //Check for every path if it has been created by the current user
                            do {
                                DataSnapshot tmp = dataIterator.next(); //Current path
                                if (tmp.child("creator").getValue().toString().equals(FirebaseAuth.getInstance().getCurrentUser().getUid())) {
                                    Log.i("banana", "creatore riconosciuto");
                                    pointsIterator = tmp.child("points");
                                    for (DataSnapshot item : pointsIterator.getChildren()) {
                                        Log.i("banana", item.toString());
                                        tmpPoint = new LatLng(Double.parseDouble(item.child("latitude").getValue().toString()), Double.parseDouble(item.child("longitude").getValue().toString()));
                                        pointsFromDb.add(tmpPoint);
                                        if (first) {
                                            gMap.addMarker(new MarkerOptions().position(tmpPoint).title("Start point"));
                                            first = false;
                                        }
                                    }

                                    first = true;
                                    PolylineOptions options2 = new PolylineOptions().width(lineWidth).color(trackColor).geodesic(true).addAll(pointsFromDb);
                                    line = gMap.addPolyline(options2);
                                    pointsFromDb.clear();
                                }
                            } while (dataIterator.hasNext());
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }
                    });

        } else if (id == R.id.nav_near_paths) {
            lookForNearPaths();
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
        Log.e("LogsFunctions", " \nonConnected - isConnected : " + googleApiClient.isConnected());

        if (googleApiClient.isConnected()) {
            if (checkGpsEnabled()) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, mLocationRequest, this);  //return this PendingResult<Status> pendingResult;
                    afterOnConnected = true;
                }
            }
        }
    }

    //LocationListener provides call back for location change through onLocationChanged.
    @Override
    public void onLocationChanged(Location location) {
        Log.e("LogsFunctions", " \nFiring onLocationChanged .........");
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
            Log.e("LogsFunctions", " \nConnection failed: " + connectionResult.toString());
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.e("LogsFunctions", " \nonConnectionSuspended .........");
    }
}


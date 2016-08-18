package com.trekken;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;

import java.util.List;

public class HiddenMain extends Activity {

    SensorManager sMng;
    List<Sensor> sensorList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hidden_main);

        //Manda via Log tutti i sensori presenti sul dispositivo
        sMng = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sensorList = sMng.getSensorList(Sensor.TYPE_ALL);
        for (Sensor s: sensorList) {
            Log.i(ACCESSIBILITY_SERVICE, s.getName());
        }

        SharedPreferences sharedPref = this.getSharedPreferences("login_preferences", Context.MODE_PRIVATE);
        String textData = sharedPref.getString("logged", "no");

        // Se non ce salvato nessuno nelle SharedPreferences lancia Login
        if(textData.equals("no")) {
            Intent intent = new Intent(this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            finish();
            startActivity(intent);
        }

        else {
            Intent intent = new Intent(this,MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            finish();
            startActivity(intent);
        }
    }
}

package com.trekken;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.annotation.MainThread;
import android.util.Log;
import android.view.View;

import com.firebase.ui.auth.AuthUI;
import com.google.firebase.auth.FirebaseAuth;

import java.util.List;

import butterknife.BindView;

public class HiddenMain extends Activity {

    SensorManager sMng;
    List<Sensor> sensorList;

    private static final int RC_SIGN_IN = 100;

    @BindView(android.R.id.content)
    View mRootView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hidden_main);

        //Send Log messages for every Sensor present
        sMng = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sensorList = sMng.getSensorList(Sensor.TYPE_ALL);
        for (Sensor s: sensorList) {
            Log.i(ACCESSIBILITY_SERVICE, s.getName());
        }

        SharedPreferences sharedPref = this.getSharedPreferences("login_preferences", Context.MODE_PRIVATE);
        String textData = sharedPref.getString("logged", "no");

        // If there is no user saved in Shared Preferences, start Login
        if(textData.equals("no") && FirebaseAuth.getInstance().getCurrentUser() == null) {
            startActivityForResult(
                    AuthUI.getInstance().createSignInIntentBuilder()
                            .setLogo(R.drawable.tekken_logo)
                            .setIsSmartLockEnabled(false)
                            .setTheme(R.style.AppTheme)
                            .setProviders(new String[]{AuthUI.EMAIL_PROVIDER, AuthUI.GOOGLE_PROVIDER})
                            .build(),
                    RC_SIGN_IN);
        }

        //else go directly to Main Activity
        else {
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            finish();
            startActivity(intent);
        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            handleSignInResponse(resultCode, data);
            return;
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @MainThread
    private void handleSignInResponse(int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            SharedPreferences sharedPref = this.getSharedPreferences("login_preferences", Context.MODE_PRIVATE);

            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString("logged", "yes");
            editor.putString("email", FirebaseAuth.getInstance().getCurrentUser().getEmail());
            editor.apply();

            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            finish();
            startActivity(intent);

            return;
        }

        if (resultCode == RESULT_CANCELED) {
            finish();
            return;
        }

    }
}

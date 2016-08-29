package com.trekken;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.annotation.MainThread;
import android.support.annotation.StringRes;
import android.support.design.widget.Snackbar;
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
            startActivityForResult(
                    AuthUI.getInstance().createSignInIntentBuilder()
                        /*DEBUG*/
                            .setIsSmartLockEnabled(false)
                            .setTheme(AuthUI.getDefaultTheme())
                            .setProviders(AuthUI.GOOGLE_PROVIDER)
                            .build(),
                    RC_SIGN_IN);
        }

        else {
            Intent intent = new Intent(this,MainActivity.class);
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

        showSnackbar(R.string.unknown_response);
    }

    @MainThread
    private void handleSignInResponse(int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            SharedPreferences sharedPref = this.getSharedPreferences("login_preferences", Context.MODE_PRIVATE);

            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString("logged", "yes");
            editor.putString("email", FirebaseAuth.getInstance().getCurrentUser().getEmail());
            //editor.putString("email", mPassword); Questo non dovrebbe essere necessario e crea problemi inutili
            editor.apply();

            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            finish();
            startActivity(intent);

            return;
        }

        if (resultCode == RESULT_CANCELED) {
            showSnackbar(R.string.sign_in_cancelled);
            return;
        }

        showSnackbar(R.string.unknown_sign_in_response);
    }

    @MainThread
    private void showSnackbar(@StringRes int errorMessageRes) {
        Snackbar.make(mRootView, errorMessageRes, Snackbar.LENGTH_LONG).show();
    }

}

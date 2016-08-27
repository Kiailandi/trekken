package com.trekken;

import android.content.Context;
import android.content.Intent;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.annotation.MainThread;

import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.RadioButton;
import android.support.design.widget.Snackbar;

import android.support.annotation.StringRes;

import com.firebase.ui.auth.AuthUI;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;


public class SignInActivity extends AppCompatActivity {

    private static final int RC_SIGN_IN = 100;

    @BindView(R.id.email_provider)
    CheckBox mEmailProvider;

    @BindView(R.id.google_provider)
    CheckBox mGoogleProvider;

    @BindView(R.id.sign_in)
    Button mSignIn;

    @BindView(android.R.id.content)
    View mRootView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            startActivity(WelcomeActivity.createIntent(this));
            finish();
        }

        setContentView(R.layout.signed_in_layout);
        ButterKnife.bind(this);
    }

    @OnClick(R.id.sign_in)
    public void signIn(View view) {
        startActivityForResult(
                AuthUI.getInstance().createSignInIntentBuilder()
                        /*DEBUG*/
                        .setLogo(AuthUI.NO_LOGO)
                        .setIsSmartLockEnabled(false)
                        .setTheme(AuthUI.getDefaultTheme())
                        .setProviders(getSelectedProviders())
                        .build(),
                RC_SIGN_IN);
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
            startActivity(WelcomeActivity.createIntent(this));
            finish();
            return;
        }

        if (resultCode == RESULT_CANCELED) {
            showSnackbar(R.string.sign_in_cancelled);
            return;
        }

        showSnackbar(R.string.unknown_sign_in_response);
    }

    @MainThread
    private String[] getSelectedProviders() {
        ArrayList<String> selectedProviders = new ArrayList<>();

        if (mEmailProvider.isChecked()) {
            selectedProviders.add(AuthUI.EMAIL_PROVIDER);
        }

        if (mGoogleProvider.isChecked()) {
            selectedProviders.add(AuthUI.GOOGLE_PROVIDER);
        }

        return selectedProviders.toArray(new String[selectedProviders.size()]);
    }

    @MainThread
    private void showSnackbar(@StringRes int errorMessageRes) {
        Snackbar.make(mRootView, errorMessageRes, Snackbar.LENGTH_LONG).show();
    }

    public static Intent createIntent(Context context) {
        Intent in = new Intent();
        in.setClass(context, SignInActivity.class);
        return in;
    }
}
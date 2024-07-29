package com.example.pm1personas;

import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;

import android.content.SharedPreferences;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.auth.api.identity.BeginSignInRequest;
import com.google.android.gms.auth.api.identity.Identity;
import com.google.android.gms.auth.api.identity.SignInCredential;
import com.google.android.gms.auth.api.identity.SignInClient;
import com.google.android.gms.common.api.ApiException;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.annotations.Nullable;

public class LoginActivity extends AppCompatActivity {

    private static final int REQ_ONE_TAP = 2;
    private static final String TAG = "LoginActivity";

    private SignInClient oneTapClient;
    private BeginSignInRequest signInRequest;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_activity);

        mAuth = FirebaseAuth.getInstance();

        Button signInButton = findViewById(R.id.sign_in_button);
        signInButton.setOnClickListener(v -> signIn());

        oneTapClient = Identity.getSignInClient(this);

        String webClientId = getString(R.string.cliente_web_google_auth);

        signInRequest = BeginSignInRequest.builder()
                .setGoogleIdTokenRequestOptions(
                        BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                                .setSupported(true)
                                .setServerClientId(webClientId)
                                .setFilterByAuthorizedAccounts(false)
                                .build())
                .build();
    }

    private void signIn() {
        oneTapClient.beginSignIn(signInRequest)
                .addOnSuccessListener(response -> {
                    try {
                        IntentSender signInIntentSender = response.getPendingIntent().getIntentSender();
                        startIntentSenderForResult(signInIntentSender, REQ_ONE_TAP, null, 0, 0, 0);
                    } catch (IntentSender.SendIntentException e) {
                        Log.e(TAG, "Error al iniciar proceso de ingreso", e);
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Fallo al ingresar", e));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQ_ONE_TAP) {
            try {
                SignInCredential credential = oneTapClient.getSignInCredentialFromIntent(data);
                String idToken = credential.getGoogleIdToken();
                if (idToken != null) {
                    AuthCredential firebaseCredential = GoogleAuthProvider.getCredential(idToken, null);
                    mAuth.signInWithCredential(firebaseCredential)
                            .addOnCompleteListener(this, task -> {
                                if (task.isSuccessful()) {
                                    FirebaseUser user = mAuth.getCurrentUser();
                                    updateUI(user);
                                } else {
                                    Log.w(TAG, "signInWithCredential:failure", task.getException());
                                    updateUI(null);
                                }
                            });
                } else {
                    Log.d(TAG, "No ID token!");
                }
            } catch (ApiException e) {
                Log.e(TAG, "Error  al obtener credenciales", e);
            }
        }
    }
    /*@Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQ_ONE_TAP) {
            try {
                SignInCredential credential = oneTapClient.getSignInCredentialFromIntent(data);
                String idToken = credential.getGoogleIdToken();
                if (idToken != null) {
                    AuthCredential firebaseCredential = GoogleAuthProvider.getCredential(idToken, null);
                    mAuth.signInWithCredential(firebaseCredential)
                            .addOnCompleteListener(this, task -> {
                                if (task.isSuccessful()) {
                                    FirebaseUser user = mAuth.getCurrentUser();

                                    MantenerUsuarioSalvado(user);
                                    updateUI(user);
                                } else {
                                    Log.w(TAG, "signInWithCredential:failure", task.getException());
                                    updateUI(null);
                                }
                            });
                } else {
                    Log.d(TAG, "No ID token!");
                }
            } catch (ApiException e) {
                Log.e(TAG, "Error getting credential from intent", e);
            }
        }
    }*/

    private void MantenerUsuarioSalvado(FirebaseUser user) {
        SharedPreferences sharedPref = getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString("user_id", user.getUid());
        editor.putString("user_email", user.getEmail());
        editor.putString("user_display_name", user.getDisplayName());
        editor.apply();
    }


    private void updateUI(FirebaseUser user) {
        if (user != null) {
            Intent intent = new Intent(LoginActivity.this, CreatePersonaActivity.class);
            startActivity(intent);
            finish();
        }
    }

    /*@Override
    public void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            updateUI(currentUser);
        } else {
            SharedPreferences sharedPref = getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
            String userId = sharedPref.getString("user_id", null);
            if (userId != null) {
                Intent intent = new Intent(LoginActivity.this, CreatePersonaActivity.class);
                startActivity(intent);
                finish();
            }
        }
    }*/


    @Override
    public void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        updateUI(currentUser);
    }
}

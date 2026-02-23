package com.example.chathurang;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.airbnb.lottie.LottieAnimationView;
import com.airbnb.lottie.LottieDrawable;
import com.airbnb.lottie.RenderMode;
import com.google.android.gms.auth.api.signin.*;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.*;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;

import java.util.HashMap;
import java.util.Map;

public class AuthActivity extends AppCompatActivity {

    private static final int RC_SIGN_IN = 100;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseAnalytics analytics;
    private GoogleSignInClient mGoogleSignInClient;

    private Button btnGoogleLogin, btnSaveUsername;
    private EditText etUsername;
    private TextView tvUsernameError;
    LottieAnimationView authBg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);
        RotationLocker.lockPortrait(this);

        View authCard = findViewById(R.id.authCard);
        TextView title = findViewById(R.id.titleText);

        authBg = findViewById(R.id.authBackground);
        authBg.addLottieOnCompositionLoadedListener(composition -> {
            int totalFrames = (int) composition.getEndFrame();
        });

        authBg.addLottieOnCompositionLoadedListener(composition -> {

            float startFrame = composition.getStartFrame();
            float endFrame = composition.getEndFrame();

            // Start at 30%
            float customStart = startFrame + (endFrame - startFrame) * 0f;

            authBg.setMinAndMaxFrame((int) customStart, (int) endFrame);
            authBg.setRepeatCount(LottieDrawable.INFINITE);
            authBg.setRepeatMode(LottieDrawable.RESTART);
            authBg.playAnimation();
        });

        // 🔥 Slow down animation
        authBg.setSpeed(0.5f);

        // Optional: Start from beginning every time
        authBg.setProgress(0f);
        authBg.setRenderMode(RenderMode.HARDWARE);
        authBg.enableMergePathsForKitKatAndAbove(true);

// Card Entrance Animation
        authCard.animate()
                .scaleX(1f)
                .scaleY(1f)
                .alpha(1f)
                .setDuration(1000)
                .setStartDelay(400)
                .start();

// Title glow animation
        ObjectAnimator titleAnim = ObjectAnimator.ofFloat(title, "alpha", 1f, 1f);
        titleAnim.setDuration(2000);
        titleAnim.setRepeatMode(ValueAnimator.REVERSE);
        titleAnim.setRepeatCount(ValueAnimator.INFINITE);
        titleAnim.start();

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        analytics = FirebaseAnalytics.getInstance(this);

        btnGoogleLogin = findViewById(R.id.btnGoogleLogin);
        btnSaveUsername = findViewById(R.id.btnSaveUsername);
        etUsername = findViewById(R.id.etUsername);
        tvUsernameError = findViewById(R.id.tvUsernameError);

        // Auto-login if already signed in
        if (mAuth.getCurrentUser() != null) {
            checkIfUsernameExists();
        }

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(
                GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        btnGoogleLogin.setOnClickListener(v -> signIn());
        btnSaveUsername.setOnClickListener(v -> checkAndSaveUsername());
    }

    private void signIn() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task =
                    GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account =
                        task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                Log.w("AUTH", "Google sign in failed", e);
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential =
                GoogleAuthProvider.getCredential(idToken, null);

        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {

                        analytics.logEvent("user_logged_in", null);

                        checkIfUsernameExists();
                    } else {
                        Toast.makeText(this,
                                "Authentication Failed",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void checkIfUsernameExists() {

        String uid = mAuth.getCurrentUser().getUid();

        db.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(doc -> {

                    if (doc.exists()) {
                        goToMain();
                    } else {
                        showUsernameUI();
                    }
                });
    }

    private void showUsernameUI() {

        btnGoogleLogin.animate().alpha(0f).setDuration(400)
                .withEndAction(() -> btnGoogleLogin.setVisibility(View.GONE));

        etUsername.setAlpha(0f);
        btnSaveUsername.setAlpha(0f);

        etUsername.setVisibility(View.VISIBLE);
        btnSaveUsername.setVisibility(View.VISIBLE);

        etUsername.animate().alpha(1f).setDuration(600);
        btnSaveUsername.animate().alpha(1f).setDuration(600);
    }

    private boolean isValidUsername(String username) {

        return username.matches("^[a-z0-9_]{4,15}$");
    }

    private void checkAndSaveUsername() {

        clearUsernameError();

        String username = etUsername.getText().toString()
                .trim()
                .toLowerCase();

        if (!isValidUsername(username)) {
            showUsernameError(
                    "4-15 chars | lowercase | numbers | underscore only");
            return;
        }

        String uid = mAuth.getCurrentUser().getUid();

        DocumentReference usernameRef =
                db.collection("usernames").document(username);

        usernameRef.get().addOnSuccessListener(snapshot -> {

            if (snapshot.exists()) {
                showUsernameError("Name already chosen");
                shakeView(etUsername);
                return;
            }

            // Username is free → Save both documents
            saveUsername(username, uid);

        }).addOnFailureListener(e -> {

            showUsernameError("Something went wrong");
        });
    }

    private void saveUsername(String username, String uid) {

        Map<String, Object> usernameMap = new HashMap<>();
        usernameMap.put("uid", uid);

        Map<String, Object> userMap = new HashMap<>();
        userMap.put("username", username);
        userMap.put("email", mAuth.getCurrentUser().getEmail());
        userMap.put("createdAt", System.currentTimeMillis());

        db.collection("usernames").document(username)
                .set(usernameMap)
                .addOnSuccessListener(unused -> {

                    db.collection("users").document(uid)
                            .set(userMap)
                            .addOnSuccessListener(unused1 -> goToMain())
                            .addOnFailureListener(e ->
                                    showUsernameError("Failed to save user"));

                })
                .addOnFailureListener(e ->
                        showUsernameError("Failed to save username"));
    }

    private void goToMain() {
        Intent intent = new Intent(this, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void showUsernameError(String message) {
        tvUsernameError.setText(message);
        tvUsernameError.setVisibility(View.VISIBLE);
        etUsername.setBackgroundTintList(
                ColorStateList.valueOf(Color.RED));
    }

    private void clearUsernameError() {
        tvUsernameError.setVisibility(View.GONE);
        etUsername.setBackgroundTintList(
                ColorStateList.valueOf(Color.WHITE));
    }

    private void shakeView(View view) {
        ObjectAnimator shake =
                ObjectAnimator.ofFloat(view,
                        "translationX",
                        0, 25, -25, 20, -20, 10, -10, 0);
        shake.setDuration(400);
        shake.start();
    }
}
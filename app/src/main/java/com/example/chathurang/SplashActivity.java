// SplashActivity.java

package com.example.chathurang;

import android.animation.Animator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;
import com.airbnb.lottie.LottieAnimationView;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        RotationLocker.lockPortrait(this);
        setContentView(R.layout.activity_splash);

        LottieAnimationView la = findViewById(R.id.splashLottie);

        la.addAnimatorListener(new Animator.AnimatorListener() {
            @Override public void onAnimationStart(Animator animation) {}

            @Override
            public void onAnimationEnd(Animator animation) {
                Intent intent = new Intent(SplashActivity.this, AuthActivity.class);
                startActivity(intent);

                // Apply smooth fade transition
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);

                finish();
            }

            @Override public void onAnimationCancel(Animator animation) {}
            @Override public void onAnimationRepeat(Animator animation) {}
        });
    }
}


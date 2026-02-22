//HomeActivity.java
package com.example.chathurang;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.view.ViewGroup;
import android.graphics.Color;
import androidx.appcompat.app.AppCompatActivity;


public class HomeActivity extends AppCompatActivity {


    ImageView dice;
    private boolean demoLaunched = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        RotationLocker.lockPortrait(this);
        setContentView(R.layout.activity_home);

        if(!AppPrefs.isDemoDone(this) && !demoLaunched){
            demoLaunched = true;
            startActivity(new Intent(this, MainActivity.class));
            return;
        }

        dice = findViewById(R.id.ivDiceHero);

        startFloatingAnimation();

        findViewById(R.id.btnPlay).setOnClickListener(v ->
                showModeDialog()
        );

        findViewById(R.id.card).setOnClickListener(v ->{
            ThemeManager.toggleTheme();
            applyThemeToHome();
        });

    }

    private void startFloatingAnimation() {
        dice.animate()
                .translationYBy(-12f)
                .setDuration(2200)
                .setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator())
                .withEndAction(() -> {
                    dice.animate()
                            .translationYBy(12f)
                            .setDuration(2200)
                            .start();
                })
                .start();
    }

    private void showModeDialog() {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_game_setup, null);
        builder.setView(view);

        AlertDialog dialog = builder.create();
        dialog.show();

        final boolean[] vsAI = {true};
        final int[] minutes = {-1};

        // MODE
        View cardTwo = view.findViewById(R.id.card_two_player);
        View cardAI = view.findViewById(R.id.card_vs_ai);

        cardAI.setOnClickListener(v -> {
            selectMode(cardAI, cardTwo);
            vsAI[0] = true;
        });

        cardTwo.setOnClickListener(v -> {
            selectMode(cardTwo, cardAI);
            vsAI[0] = false;
        });

        selectMode(cardAI, cardTwo); // default

        // TIME
        setupTimeCard(view, R.id.card_1min, "1 min", 1, minutes);
        setupTimeCard(view, R.id.card_3min, "3 min", 3, minutes);
        setupTimeCard(view, R.id.card_5min, "5 min", 5, minutes);
        setupTimeCard(view, R.id.card_10min, "10 min", 10, minutes);
        setupTimeCard(view, R.id.card_no_timer, "No Timer", -1, minutes);

        view.findViewById(R.id.btn_play_game).setOnClickListener(v -> {
            Intent i = new Intent(this, MainActivity.class);
            i.putExtra("mode_single_player", vsAI[0]);
            i.putExtra("mode_two_player", !vsAI[0]);
            i.putExtra("time_minutes", minutes[0]);
            startActivity(i);
            dialog.dismiss();
        });

        view.findViewById(R.id.btn_cancel_mode)
                .setOnClickListener(v -> dialog.dismiss());
    }

    private void applyThemeToHome() {
        GameTheme theme = ThemeManager.getTheme();
        View root = findViewById(R.id.root);

        if (theme == GameTheme.DARK_ELEGANT) {
            root.setBackgroundResource(R.drawable.bg_wood_dark);
        } else {
            root.setBackgroundResource(R.drawable.home_bg_gradient);
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        RotationLocker.unlockRotation(this); // if your helper exists — restore system rotation
        finish();
    }

    private void selectMode(View selected, View other) {
        selected.setBackgroundResource(R.drawable.card_selected_bg);
        other.setBackgroundResource(R.drawable.bg_glass_card);
    }

    private void setupTimeCard(View root, int id, String text, int value, int[] minutes) {

        View card = root.findViewById(id);
        TextView tv = card.findViewById(R.id.tv_time_text);
        tv.setText(text);

        card.setOnClickListener(v -> {

            // 🔥 Instead of casting parent, reset manually all time cards

            int[] allIds = {
                    R.id.card_1min,
                    R.id.card_3min,
                    R.id.card_5min,
                    R.id.card_10min,
                    R.id.card_no_timer
            };

            for (int timeId : allIds) {
                View c = root.findViewById(timeId);
                if (c != null) {
                    c.setBackgroundResource(R.drawable.bg_glass_card);
                }
            }

            // Highlight selected
            card.setBackgroundResource(R.drawable.card_selected_bg);

            minutes[0] = value;
        });
    }

}

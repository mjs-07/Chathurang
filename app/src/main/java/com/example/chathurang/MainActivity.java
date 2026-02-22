//MainActivity.java
package com.example.chathurang;

import android.animation.ValueAnimator;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.airbnb.lottie.LottieAnimationView;
import com.getkeepsafe.taptargetview.TapTarget;
import com.getkeepsafe.taptargetview.TapTargetSequence;

import androidx.appcompat.app.AppCompatActivity;
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat;
import android.content.SharedPreferences;



public class MainActivity extends AppCompatActivity {

    ChessBoardView chessBoard;
    TextView tvTurn, tvDice;
    Button btnRoll, btnResign;
    ImageView ivDice, center;
    ClockManager clockManager;
    private ValueAnimator whiteFlashAnimator;
    private ValueAnimator blackFlashAnimator;
    TextView tvWhiteTimer, tvBlackTimer;
    FrameLayout root;
    // keep track of last shown dice face so we never replace it by accident
    private int lastDiceFace = -1;

    Handler handler = new Handler();

    public boolean demoMode = false;
    public DemoController demo;

    boolean singlePlayer, twoPlayer;
    private boolean timerEnabled = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        RotationLocker.lockPortrait(this);
        SoundManager.init(this);
        setContentView(R.layout.activity_main);

        chessBoard = findViewById(R.id.chessBoard);
        tvWhiteTimer = findViewById(R.id.tvWhiteTimer);
        tvBlackTimer = findViewById(R.id.tvBlackTimer);
        View blackPanel = findViewById(R.id.includeBlackTimer);
        View whitePanel = findViewById(R.id.includeWhiteTimer);

        /*if (demoMode) {
            demo.start();
        }*/

        root = findViewById(R.id.root);

        chessBoard = findViewById(R.id.chessBoard);
        tvTurn = findViewById(R.id.tvTurn);
        tvDice = findViewById(R.id.tvDice);
        btnRoll = findViewById(R.id.btnRoll);
        btnResign = findViewById(R.id.btnResign);
        ivDice = findViewById(R.id.ivDice);
        center = findViewById(R.id.center);

        tvWhiteTimer = findViewById(R.id.tvWhiteTimer);
        tvWhiteTimer.setTextColor(Color.BLACK);
        tvBlackTimer = findViewById(R.id.tvBlackTimer);

        // Trigger initial UI state properly

        int minutes = getIntent().getIntExtra("time_minutes", -1);
        tvBlackTimer.setText(minutes+":00");

        if (minutes == -1) {
            timerEnabled = false;
            // NO TIMER MODE
            clockManager = null;

            if (blackPanel != null) blackPanel.setVisibility(View.GONE);
            if (whitePanel != null) whitePanel.setVisibility(View.GONE);

        } else {
            timerEnabled = true;
            long initialMillis = minutes * 60L * 1000L;

            clockManager = new ClockManager(initialMillis, new ClockManager.ClockListener() {

                @Override
                public void onTick(boolean whiteTurn, long millisLeft) {

                    String formatted = formatTime(millisLeft);

                    if (whiteTurn && tvWhiteTimer != null) {
                        tvWhiteTimer.setText(formatted);
                    } else if (tvBlackTimer != null) {
                        tvBlackTimer.setText(formatted);
                    }

                    handleLowTimeFlash(whiteTurn, millisLeft);
                }

                @Override
                public void onLowTimeTick(boolean white, long millisLeft) {

                    if((singlePlayer && chessBoard.engine.whiteTurn) || !singlePlayer){
                        // 🔥 Vibrate every second under 10
                        if (millisLeft <= 5) {
                            HapticHelper.vibrate(MainActivity.this, 80);
                        } else {
                            HapticHelper.vibrate(MainActivity.this, 40);
                        }
                    }

                    // Optional visual flash
                    handleLowTimeFlash(white, millisLeft);
                }

                @Override
                public void onTimeUp(boolean whiteLost) {

                    chessBoard.engine.gameOver = true;

                    if (clockManager != null) clockManager.stop();

                    String winner = whiteLost ? "Black" : "White";
                    showGameOverDialog(winner + " wins (Time)");
                }
            });

            // SAFELY initialize UI state AFTER everything exists
            chessBoard.post(() -> {

                boolean initialTurn = chessBoard.engine.whiteTurn;

                positionTimers(initialTurn);
                updateActiveTimerGlow(initialTurn);

                if (clockManager != null && !chessBoard.engine.gameOver&& timerEnabled) {
                    clockManager.startTurn(initialTurn);
                }
            });
        }

        center.setVisibility(View.INVISIBLE);
        // after setContentView(...) and after chessBoard initialization...
        singlePlayer = getIntent().getBooleanExtra("mode_single_player", true);
        twoPlayer = getIntent().getBooleanExtra("mode_two_player", false);

        chessBoard.engine.blackIsAI = singlePlayer;

        // apply to chessBoard
        chessBoard.setSinglePlayer(singlePlayer);
        chessBoard.setTwoPlayerMode(twoPlayer);

        applyThemeToMain();


        // Wire UI callbacks
        chessBoard.setUiCallbacks(new ChessBoardView.UiCallbacks() {
            @Override
            public void onTurnChanged(boolean whiteTurn) {
                tvTurn.setText("Turn: " + (whiteTurn ? "White" : "Black"));
                android.util.Log.d("UI-CB", "onTurnChanged: whiteTurn=" + whiteTurn);
                rotateBoardForTurn();
                updateRollButtonState();
                positionTimers(whiteTurn);
                updateActiveTimerGlow(whiteTurn);

                if (clockManager != null && !chessBoard.engine.gameOver && timerEnabled) {
                    clockManager.startTurn(whiteTurn);
                }
            }

            @Override
            public void onDiceRollStart() {
                if (timerEnabled && clockManager != null) {
                    clockManager.stop();
                }
                android.util.Log.d("UI-CB", "onDiceRollStart()");
                // Disable roll immediately (one-roll-per-turn guarantee)
                btnRoll.setEnabled(false);
                btnRoll.setAlpha(0.6f);


                // Start dice animation if available, otherwise perform rotation animation
                try {
                    SoundManager.playDiceRoll();
                    ivDice.setImageResource(R.drawable.dice_frame_anim);
                    Drawable d = ivDice.getDrawable();
                    if (d instanceof AnimationDrawable) {
                        AnimationDrawable ad = (AnimationDrawable) d;
                        ad.stop();
                        ad.start();
                        return;
                    }
                    if (d instanceof AnimatedVectorDrawable) {
                        ((AnimatedVectorDrawable) d).start();
                        return;
                    }
                    if (d instanceof AnimatedVectorDrawableCompat) {
                        ((AnimatedVectorDrawableCompat) d).start();
                        return;
                    }
                } catch (Exception ex) {
                    android.util.Log.w("UI-CB", "dice_anim start failed: " + ex.getMessage());
                }
                ivDice.setRotation(0f);
                ivDice.animate().rotationBy(1080f).setDuration(520).start();
            }

            @Override
            public void onDiceChanged(int value, Piece.Type allowedType) {
                android.util.Log.d("UI-CB", "onDiceChanged: value=" + value + " allowed=" + allowedType);

                // Update lastDiceFace only when we get a real positive value.
                if (value > 0) lastDiceFace = value;

                // Show the final dice image if possible. Do not replace a good image with neutral.
                if (value > 0) {
                    int res = getResources().getIdentifier("dice_face_" + value, "drawable", getPackageName());
                    if (res != 0) {
                        ivDice.setImageResource(res);
                    } else {
                        android.util.Log.e("UI-CB", "Missing dice face resource for value=" + value);
                        if (lastDiceFace <= 0) {
                            int neutral = getResources().getIdentifier("dice_red", "drawable", getPackageName());
                            if (neutral != 0) ivDice.setImageResource(neutral);
                        }
                    }

                    String nice = (allowedType == null) ? "-" :
                            allowedType.name().substring(0,1).toUpperCase() + allowedType.name().substring(1).toLowerCase();
                    tvDice.setText("Dice: " + value + " (" + nice + ")");
                    if (clockManager != null && !chessBoard.engine.gameOver && timerEnabled) {
                        clockManager.startTurn(chessBoard.engine.whiteTurn);
                    }

                } else {
                    // non-positive (abnormal) -> keep last visible face if exists; otherwise neutral
                    android.util.Log.w("UI-CB", "onDiceChanged called with non-positive value=" + value);
                    if (lastDiceFace > 0) {
                        int res = getResources().getIdentifier("dice_face_" + lastDiceFace, "drawable", getPackageName());
                        if (res != 0) ivDice.setImageResource(res);
                    } else {
                        int neutral = getResources().getIdentifier("dice_red", "drawable", getPackageName());
                        if (neutral != 0) ivDice.setImageResource(neutral);
                    }
                    tvDice.setText("Dice : -");
                }

                // Keep roll button disabled while there is an active allowedType (a roll is pending).
                updateRollButtonState();

            }

            @Override
            public void onPromotionRequired(Piece pawn, int toR, int toC) {

                if (pawn == null) return;

                // TWO PLAYER MODE → always show dialog
                if (twoPlayer) {
                    showPromotionDialog(pawn);
                    return;
                }

                // SINGLE PLAYER MODE
                if (singlePlayer) {

                    if (pawn.color == Piece.Color.WHITE) {
                        // Human player
                        showPromotionDialog(pawn);
                    } else {
                        // AI pawn → auto promote
                        chessBoard.engine.completePromotion(Piece.Type.QUEEN);
                        chessBoard.loadPieceBitmaps();
                        chessBoard.invalidate();
                    }
                }
            }


            @Override
            public void onGameOver(String reason) {
                HapticHelper.vibrate(getBaseContext(), 120);
                android.util.Log.d("UI-CB", "onGameOver: " + reason);
                showGameOverDialog(reason);
                if (clockManager != null) clockManager.stop();
            }

            @Override
            public void onNoValidMoves(boolean wasHumanTurn) {
                // Show clear message for short time; keep dice face image unchanged
                tvDice.setText("NO VALID MOVES");

                // Ensure dice image remains a valid face (do not replace with neutral)
                if (lastDiceFace > 0) {
                    int res = getResources().getIdentifier("dice_face_" + lastDiceFace, "drawable", getPackageName());
                    if (res != 0) ivDice.setImageResource(res);
                }

                // After short delay, restore default text and update roll button state
                tvDice.postDelayed(() -> {
                    tvDice.setText("Dice: -");
                    updateRollButtonState();
                }, 1200);
            }

        });

        // initial roll-button state (no roll yet)
        updateRollButtonState();

        // Hook roll button to animation + roll (single handler for human)
        btnRoll.setOnClickListener(v -> {
            HapticHelper.vibrate(this, 30);
            chessBoard.playerRollDice();
        });

        btnResign.setOnClickListener(v -> {
            showResignConfirmDialog();
        });

        if (!AppPrefs.isDemoDone(this)) {
            new DemoController(
                    this
            ).start();
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        applyThemeToMain();
    }



    // helper to enable/disable roll button based on current game state
    private void updateRollButtonState() {
        if (chessBoard == null || chessBoard.engine == null) {
            btnRoll.setEnabled(false);
            btnRoll.setAlpha(0.6f);
            return;
        }

        // If there is an active roll already (allowedTypeThisTurn != null) -> disable roll
        if (chessBoard.engine.allowedTypeThisTurn != null) {
            btnRoll.setEnabled(false);
            btnRoll.setAlpha(0.6f);
            return;
        }

        // No active roll => button enabled only when:
        // singlePlayer: only White may roll
        // two-player: either may roll
        if (singlePlayer) {
            boolean enable = chessBoard.engine.whiteTurn && !chessBoard.engine.gameOver;
            btnRoll.setEnabled(enable);
            btnRoll.setAlpha(enable ? 1f : 0.6f);
        } else {
            // two-player mode: allow roll when game not over
            boolean enable = !chessBoard.engine.gameOver;
            btnRoll.setEnabled(enable);
            btnRoll.setAlpha(enable ? 1f : 0.6f);
        }
    }

    // Dice animation: cycles images quickly then performs the actual roll via chessBoard.playerRollDice()
    // This keeps the same visible dice face logic: after roll complete, onDiceChanged will set the actual face.
    private void animateDiceRollAndApply() {
        final int frames = 12; // total frames shown
        final long frameDuration = 40; // ms (slightly faster)
        handler.post(new Runnable() {
            int count = 0;
            @Override
            public void run() {
                count++;
                int face = (count % 6) + 1;
                int resId = getResources().getIdentifier("dice_face_" + face, "drawable", getPackageName());
                if (resId != 0) ivDice.setImageResource(resId);
                if (count < frames) {
                    handler.postDelayed(this, frameDuration);
                } else {
                    // After animation frames, invoke the board's roll logic which sets dice and handles skipping/AI
                    if (chessBoard != null && chessBoard.engine != null && !chessBoard.engine.gameOver) {
                        // Use ChessBoardView API to roll; it will set engine dice and manage AI flow
                        chessBoard.playerRollDice();
                        if (timerEnabled && clockManager != null) {
                            clockManager.startTurn(chessBoard.engine.whiteTurn);
                        }
                        // Update lastDiceFace from engine (if immediate). But playerRollDice triggers onDiceChanged so UI will update afterwards.
                        int actual = chessBoard.engine.diceValue;
                        if (actual > 0) {
                            lastDiceFace = actual;
                            int res = getResources().getIdentifier("dice_face_" + actual, "drawable", getPackageName());
                            if (res != 0) ivDice.setImageResource(res);
                        }
                        // We already disabled the roll button in onDiceRollStart(); keep it disabled until move or turn change.
                    }
                }
            }
        });
    }


    private void showGameOverDialog(String reason) {
        boolean isVictory = reason.toLowerCase().contains("white");

        AlertDialog.Builder b = new AlertDialog.Builder(this, R.style.Theme_Chathurang_Dialog);
        View v = getLayoutInflater().inflate(R.layout.dialog_match_over, null);
        b.setView(v);
        AlertDialog d = b.create();
        d.show();

        TextView tvResult = v.findViewById(R.id.tvResult);
        TextView tvQuote = v.findViewById(R.id.tvQuote);
        TextView tvTurn = v.findViewById(R.id.tvTurn);
        LinearLayout tvback = v.findViewById(R.id.tvback);
        ImageView trophy = v.findViewById(R.id.imgTrophy);
        LottieAnimationView confetti = v.findViewById(R.id.confetti);

        if (isVictory) {
            tvResult.setText("VICTORY");
            tvQuote.setText(getRandomWinQuote());
            tvTurn.setText(reason);
            tvback.setBackgroundColor(getResources().getColor(R.color.win));
            confetti.playAnimation();
            SoundManager.playVictorySound();
        } else {
            tvResult.setText("DEFEAT");
            tvTurn.setText(reason);
            tvback.setBackgroundColor(getResources().getColor(R.color.defeat));
            trophy.setVisibility(View.GONE);
            tvQuote.setText(getRandomLossQuote());
            SoundManager.playDefeatSound();
        }

        v.findViewById(R.id.btnNewMatch).setOnClickListener(btn -> {
            d.dismiss();
            showModeSelectionDialog();
        });

        v.findViewById(R.id.btnHome).setOnClickListener(btn -> {
            startActivity(new Intent(this, HomeActivity.class));
            finish();
        });
    }


    // Promotion dialog: simple list choice; uses engine.completePromotion(...)
    private void showPromotionDialog(Piece pawn) {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_pawn_promotion, null);
        builder.setView(view);

        AlertDialog dialog = builder.create();
        dialog.setCancelable(false);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        dialog.show();

        LinearLayout root = view.findViewById(R.id.pawnPromotionRoot);
        if(pawn.color == Piece.Color.BLACK){
            root.setRotation(180f);
        }

        ImageButton btnQueen  = view.findViewById(R.id.btnQueen);
        ImageButton btnRook   = view.findViewById(R.id.btnRook);
        ImageButton btnBishop = view.findViewById(R.id.btnBishop);
        ImageButton btnKnight = view.findViewById(R.id.btnKnight);

        // Determine color prefix
        String prefix = pawn.color == Piece.Color.WHITE ? "w" : "b";

        btnQueen.setImageResource(getRes(prefix + "queen"));
        btnRook.setImageResource(getRes(prefix + "rook"));
        btnBishop.setImageResource(getRes(prefix + "bishop"));
        btnKnight.setImageResource(getRes(prefix + "knight"));

        btnQueen.setOnClickListener(v -> promote(Piece.Type.QUEEN, dialog));
        btnRook.setOnClickListener(v -> promote(Piece.Type.ROOK, dialog));
        btnBishop.setOnClickListener(v -> promote(Piece.Type.BISHOP, dialog));
        btnKnight.setOnClickListener(v -> promote(Piece.Type.KNIGHT, dialog));
    }


    private void promote(Piece.Type type, AlertDialog dialog) {

        chessBoard.engine.completePromotion(type);
        chessBoard.loadPieceBitmaps();
        chessBoard.invalidate();
        dialog.dismiss();

        // 🔥 FIX: If single player & now black turn → trigger AI
        if (singlePlayer &&
                !chessBoard.engine.whiteTurn &&
                !chessBoard.engine.gameOver) {

            chessBoard.postDelayed(() -> {
                chessBoard.aiTakeTurn();
            }, 400);
        }
    }

    private int getRes(String name) {
        return getResources().getIdentifier(
                name,
                "drawable",
                getPackageName()
        );
    }



    private void rotateBoardForTurn() {
        if (!singlePlayer) { // Two player
            if (chessBoard.engine.whiteTurn) {
                root.setRotation(0f);
                chessBoard.setRotation(0f);
            } else {
                root.setRotation(180f);
                chessBoard.setRotation(180f);
            }
        } else {
            root.setRotation(0f); // default for single player
            chessBoard.setRotation(0f);
        }
    }

    private void showModeSelectionDialog() {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this); // or default
        View dialogView = getLayoutInflater().inflate(R.layout.activity_mode_selection, null);
        builder.setView(dialogView);
        final androidx.appcompat.app.AlertDialog dlg = builder.create();
        dlg.setCancelable(true);

        // Find tappable containers (the FrameLayouts)
        View boxTwo = dialogView.findViewById(R.id.card_two_player);
        View boxAI  = dialogView.findViewById(R.id.card_vs_ai);
        Button btnCancel = dialogView.findViewById(R.id.btn_cancel_mode);

        // When user taps Two Player
        boxTwo.setOnClickListener(v -> {
            Intent i = new Intent(MainActivity.this, MainActivity.class);
            i.putExtra("mode_two_player", true);
            i.putExtra("mode_single_player", false);
            startActivity(i);
            dlg.dismiss();
            finish(); // optional, close HomeActivity so back exits app
        });

        // When user taps Play vs AI
        boxAI.setOnClickListener(v -> {
            Intent i = new Intent(MainActivity.this, MainActivity.class);
            i.putExtra("mode_single_player", true);
            i.putExtra("mode_two_player", false);
            startActivity(i);
            dlg.dismiss();
            finish();
        });

        btnCancel.setOnClickListener(v -> {
            Intent i = new Intent(MainActivity.this, HomeActivity.class);
            startActivity(i);
            dlg.dismiss();
            finish();
        });

        // Accessibility: focus for keyboard/tts
        boxTwo.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
        boxAI.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);

        dlg.show();

        // Optional: enlarge dialog to ~width of screen minus margins
        int width = (int) (getResources().getDisplayMetrics().widthPixels * 0.92f);
        dlg.getWindow().setLayout(width, WindowManager.LayoutParams.WRAP_CONTENT);
    }

    private void showResignConfirmDialog() {
        AlertDialog.Builder b = new AlertDialog.Builder(this, R.style.Theme_Chathurang_Dialog);
        View v = getLayoutInflater().inflate(R.layout.dialog_resign, null);
        b.setView(v);
        AlertDialog dialog = b.create();
        dialog.show();

        FrameLayout root = v.findViewById(R.id.root);
        if(!singlePlayer && !chessBoard.engine.whiteTurn){
            root.setRotation(180f);
        }

        v.findViewById(R.id.btnYes).setOnClickListener(btn -> {
            dialog.dismiss();

            if (chessBoard != null && chessBoard.engine != null) {
                boolean isSinglePlayer = singlePlayer;

                String winner;

                if (isSinglePlayer) {
                    // User always resigns → AI wins
                    winner = "Black";
                } else {
                    // Two player → opponent of current turn wins
                    winner = chessBoard.engine.whiteTurn ? "Black" : "White";
                }
                chessBoard.engine.gameOver = true;
                showGameOverDialog(winner + " wins (resignation)");
            }
        });

        v.findViewById(R.id.btnNo).setOnClickListener(btn -> dialog.dismiss());


    }


    private String getRandomWinQuote() {
        String[] quotes = {
                "Victory favors the bold. And today, you rolled destiny your way.",
                "Every roll mattered. Every move counted. Well played!",
                "You didn’t just win — you outplayed fate.",
                "Luck opened the door. Skill sealed the victory.",
                "The board bowed! The dice obeyed!",
                "Strategy met chance — and you mastered both.",
                "Champions adapt. Legends conquer. Today, you did both."
        };
        return quotes[new java.util.Random().nextInt(quotes.length)];
    }

    private String getRandomLossQuote() {
        String[] quotes = {
                "Fate turned its back — but the next roll may tell a different story.",
                "Not every battle is won. Every lesson is earned.",
                "The dice were cruel, but your journey isn’t over.",
                "Defeat today. Experience forever.",
                "Even kings fall — only to rise stronger.",
                "Chance won this time. Strategy will win the next.",
                "Every loss sharpens the mind for the next victory."
        };
        return quotes[new java.util.Random().nextInt(quotes.length)];
    }

    public void disableAllButtons() {
        btnRoll.setEnabled(false);
        btnResign.setEnabled(false);
    }

    public void enableRollOnly() {
        btnRoll.setEnabled(true);
        btnResign.setEnabled(false);
    }/*

    public void endDemoAndGoHome() {
        AppPrefs.markDemoDone(this);
        startActivity(new Intent(this, HomeActivity.class));
        finish();
    }*/

    private void applyThemeToMain() {
        GameTheme theme = ThemeManager.getTheme();

        View root = findViewById(R.id.root);
        if (theme == GameTheme.DARK_ELEGANT) {
            root.setBackgroundResource(R.drawable.bg_wood_dark);
        } else {
            root.setBackgroundResource(R.drawable.home_bg_gradient);
        }

        chessBoard.applyTheme(theme);
    }




    @Override
    protected void onDestroy() {
        RotationLocker.unlockRotation(this);
        super.onDestroy();
        //SoundManager.release();
        finish();
    }

    private String formatTime(long millis) {

        long totalSeconds = millis / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;

        return String.format("%02d:%02d", minutes, seconds);
    }

    private void positionTimers(boolean whiteTurn) {

        View blackPanel = findViewById(R.id.includeBlackTimer);
        View whitePanel = findViewById(R.id.includeWhiteTimer);

        if (blackPanel == null || whitePanel == null) return;

        FrameLayout.LayoutParams blackParams =
                (FrameLayout.LayoutParams) blackPanel.getLayoutParams();

        FrameLayout.LayoutParams whiteParams =
                (FrameLayout.LayoutParams) whitePanel.getLayoutParams();

        // Always align to right side
        int rightMargin = 30;
        int topMargin = 500;
        int bottomMargin = 500;

        blackParams.setMargins(0, topMargin, rightMargin, 500);
        whiteParams.setMargins(0, 500, rightMargin, bottomMargin);

        if (!singlePlayer) {

            // TWO PLAYER MODE
            if (whiteTurn) {
                // White at bottom, Black at top
                blackParams.gravity = Gravity.TOP | Gravity.END;
                whiteParams.gravity = Gravity.BOTTOM | Gravity.END;
            } else {
                // Flip positions
                blackParams.gravity = Gravity.BOTTOM | Gravity.END;
                whiteParams.gravity = Gravity.TOP | Gravity.END;
            }

        } else {
            // SINGLE PLAYER
            blackParams.gravity = Gravity.TOP | Gravity.END;
            whiteParams.gravity = Gravity.BOTTOM | Gravity.END;
        }
        blackPanel.animate().alpha(0f).setDuration(100)
                .withEndAction(() -> {
                    blackPanel.setLayoutParams(blackParams);
                    blackPanel.animate().alpha(1f).setDuration(100).start();
                }).start();

        whitePanel.animate().alpha(0f).setDuration(100)
                .withEndAction(() -> {
                    whitePanel.setLayoutParams(whiteParams);
                    whitePanel.animate().alpha(1f).setDuration(100).start();
                }).start();
    }

    private void updateActiveTimerGlow(boolean whiteTurn) {

        View whitePanel = findViewById(R.id.includeWhiteTimer);
        View blackPanel = findViewById(R.id.includeBlackTimer);

        if (whitePanel == null || blackPanel == null) return;

        if (whiteTurn) {
            whitePanel.setBackgroundResource(R.drawable.white_timer_active_bg);
            blackPanel.setBackgroundResource(R.drawable.black_timer_panel_bg);
        } else {
            blackPanel.setBackgroundResource(R.drawable.black_timer_active_bg);
            whitePanel.setBackgroundResource(R.drawable.white_timer_panel_bg);
        }
    }

    private void handleLowTimeFlash(boolean whiteTurn, long millisLeft) {

        TextView tvWhite = findViewById(R.id.tvWhiteTimer);
        TextView tvBlack = findViewById(R.id.tvBlackTimer);

        if (millisLeft > 10000) {
            stopFlash();
            return;
        }

        TextView target = whiteTurn ? tvWhite : tvBlack;

        if (target == null) return;

        ValueAnimator animator = ValueAnimator.ofFloat(1f, 0f);
        animator.setDuration(500);
        animator.setRepeatMode(ValueAnimator.REVERSE);
        animator.setRepeatCount(ValueAnimator.INFINITE);

        animator.addUpdateListener(a -> {
            float value = (float) a.getAnimatedValue();
            target.setTextColor(
                    Color.argb(255,
                            255,
                            (int)(value * 255),
                            (int)(value * 255))
            );
        });

        animator.start();

        if (whiteTurn) {
            if (whiteFlashAnimator != null) whiteFlashAnimator.cancel();
            whiteFlashAnimator = animator;
        } else {
            if (blackFlashAnimator != null) blackFlashAnimator.cancel();
            blackFlashAnimator = animator;
        }
    }

    private void stopFlash() {

        if (whiteFlashAnimator != null) {
            whiteFlashAnimator.cancel();
            whiteFlashAnimator = null;
        }

        if (blackFlashAnimator != null) {
            blackFlashAnimator.cancel();
            blackFlashAnimator = null;
        }

        TextView tvWhite = findViewById(R.id.tvWhiteTimer);
        TextView tvBlack = findViewById(R.id.tvBlackTimer);

        if (tvWhite != null) tvWhite.setTextColor(Color.BLACK);
        if (tvBlack != null) tvBlack.setTextColor(Color.WHITE);
    }
}

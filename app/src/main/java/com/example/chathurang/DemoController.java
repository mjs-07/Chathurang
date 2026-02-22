package com.example.chathurang;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.AnimationUtils;

import com.getkeepsafe.taptargetview.TapTarget;
import com.getkeepsafe.taptargetview.TapTargetSequence;
import com.getkeepsafe.taptargetview.TapTargetView;

public class DemoController {

    private final MainActivity a;
    private final ChessBoardView board;
    private int step = 0;

    public DemoController(MainActivity a) {
        this.a = a;
        this.board = a.chessBoard;
        board.enableDemoMode();
    }

    public void start() {
        step0_hook();
    }

    private void step0_hook() {
        TapTargetView.showFor(a,
                TapTarget.forView(
                                a.center,
                                "Welcome to Chathurang",
                                "Chess. With Uncertainty.")
                        .transparentTarget(true)
                        .cancelable(false)
                        .targetCircleColor(R.color.teal_700)
                        .descriptionTextColor(R.color.white)
                        .titleTextColor(R.color.black)
                        .outerCircleColor(R.color.teal_500)
                        .drawShadow(true),
                new TapTargetView.Listener() {
                    @Override public void onTargetClick(TapTargetView view) {
                        view.dismiss(true);
                        step1_roll();
                    }
                });
    }

    private void step1_roll() {
        a.enableRollOnly();

        TapTargetView.showFor(a,
                TapTarget.forView(a.btnRoll,
                                "Roll the Dice",
                                "The dice decides which piece you can move")
                        .transparentTarget(true)
                        .cancelable(false)
                        .targetCircleColor(R.color.purple_700)
                        .descriptionTextColor(R.color.white)
                        .titleTextColor(R.color.black)
                        .outerCircleColor(R.color.purple_500)
                        .drawShadow(true),
                new TapTargetView.Listener() {
                    @Override public void onTargetClick(TapTargetView view) {
                        a.btnRoll.performClick();
                        view.dismiss(true);
                        step2_dice();
                    }
                });
    }

    private void step2_dice() {
        board.forceDice(2); // Knight

        TapTargetView.showFor(a,
                TapTarget.forView(a.ivDice,
                                "Dice maps the piece type",
                                "1 -> Pawn"
                                        + "\n" + "2 -> Knight"
                                        + "\n" + "3 -> Bishop"
                                        + "\n" + "4 -> Rook"
                                        + "\n" + "5 -> Queen"
                                        + "\n" + "6 -> King")
                        .transparentTarget(true)
                        .cancelable(false)
                        .targetCircleColor(R.color.gold_soft)
                        .descriptionTextColor(R.color.white)
                        .titleTextColor(R.color.black)
                        .outerCircleColor(R.color.gold)
                        .drawShadow(true),
                new TapTargetView.Listener() {
                    @Override public void onTargetClick(TapTargetView view) {
                        view.dismiss(true);
                        step3_turn();
                    }
                });
    }

    private void step3_turn() {
        TapTargetView.showFor(a,
                TapTarget.forView(a.tvTurn,
                                "Turn Indicator",
                                "This shows whose turn it is and what piece to move")
                        .transparentTarget(true)
                        .cancelable(false)
                        .targetCircleColor(R.color.orange_dark)
                        .descriptionTextColor(R.color.white)
                        .titleTextColor(R.color.black)
                        .outerCircleColor(R.color.orange)
                        .drawShadow(true),
                new TapTargetView.Listener() {
                    @Override public void onTargetClick(TapTargetView view) {
                        view.dismiss(true);
                        step5_knightMove();
                    }
                });
    }

    private void step5_knightMove() {

        // Arm engine
        board.engine.allowedTypeThisTurn = Piece.Type.KNIGHT;
        board.engine.diceValue = 2;

        board.setDemoMode(true);

        // Unlock interaction
        board.setInputBlocked(false);
        board.setDemoMoveAllowed(true);
        board.startKnightPulse();
        // Demo behavior
        board.enableDemoSelectionOnly(true);
        //board.highlightOnlyType(Piece.Type.KNIGHT);

        // Listen for real move
        board.setDemoMoveListener(() -> {
            board.setDemoMoveAllowed(true);
            board.setDemoMode(true);
            waitForMoveThenStep6();
        });

        Rect boardRect = new Rect();
        a.chessBoard.getGlobalVisibleRect(boardRect);

        TapTargetView.showFor(a,
                TapTarget.forBounds(boardRect,
                                "You still choose the move",
                                "Tap a knight to see its options")
                        .transparentTarget(true)
                        .cancelable(false)
                        .targetCircleColor(R.color.dark_wooden)
                        .descriptionTextColor(R.color.white)
                        .titleTextColor(R.color.black)
                        .outerCircleColor(R.color.light_wooden)
                        .drawShadow(true),
                new TapTargetView.Listener() {
                    @Override public void onTargetClick(TapTargetView view) {
                        board.stopKnightPulse();
                        view.dismiss(true);
                        // DO NOTHING ELSE
                        // Tutorial now waits for actual move
                    }
                });
    }




    private void waitForMoveThenStep6() {
        board.forceDice(6); // King
        a.tvDice.setText("NO VALID MOVES");
        TapTargetView.showFor(a,
                TapTarget.forView(a.tvTurn,
                                "One roll. One move.",
                                "")
                        .transparentTarget(true)
                        .cancelable(false)
                        .targetCircleColor(R.color.black)
                        .titleTextColor(R.color.white)
                        .outerCircleColor(R.color.bg_dark)
                        .drawShadow(true),
                new TapTargetView.Listener() {
                    @Override public void onTargetClick(TapTargetView view) {
                        view.dismiss(true);
                        step7_noMove();
                    }
        });
    }

    private void step7_noMove() {
        Rect boardRect = new Rect();
        a.chessBoard.getGlobalVisibleRect(boardRect);
        a.tvTurn.setText("Turn: White");
        a.tvDice.setText("Dice : -");

        TapTargetView.showFor(a,
                        TapTarget.forBounds(
                                boardRect,
                        "No move possible — turn skipped",
                        "")
                .transparentTarget(true)
                .cancelable(false)
                                .targetCircleColor(R.color.dark_classic)
                                .descriptionTextColor(R.color.white)
                                .titleTextColor(R.color.black)
                                .outerCircleColor(R.color.win)
                                .drawShadow(true),
                new TapTargetView.Listener() {
                    @Override public void onTargetClick(TapTargetView view) {
                        view.dismiss(true);
                        step8_finish();
                    }
                });
    }

    private void step8_finish() {

        // Create a small invisible rect at center of screen
        Rect centerRect = new Rect(
                a.getWindow().getDecorView().getWidth() / 2 - 10,
                a.getWindow().getDecorView().getHeight() / 2 - 10,
                a.getWindow().getDecorView().getWidth() / 2 + 10,
                a.getWindow().getDecorView().getHeight() / 2 + 10
        );

        TapTargetView.showFor(a,
                TapTarget.forBounds(
                                centerRect,
                                "Luck limits. Skill wins.",
                                "Tap to begin your journey"
                        )
                        .transparentTarget(true)
                        .cancelable(false)
                        .targetCircleColor(R.color.defeat_dark)
                        .descriptionTextColor(R.color.white)
                        .titleTextColor(R.color.black)
                        .outerCircleColor(R.color.defeat)
                        .drawShadow(true),
                new TapTargetView.Listener() {
                    @Override
                    public void onTargetClick(TapTargetView view) {
                    // Mark demo complete
                        AppPrefs.markDemoDone(a);

                        view.dismiss(true);
                        board.setDemoMode(false);
                        board.setDemoMoveAllowed(false);

                        //Fade out MainActivity
                        a.getWindow().getDecorView().animate()
                                .alpha(0f)
                                .setDuration(400)
                                .withEndAction(() -> {
                                    a.startActivity(
                                            new Intent(a, HomeActivity.class)
                                    );
                                    a.finish();
                                })
                                .start();
                        //finishDemo();
                    }
                }
        );
    }

    private void finishDemo() {
        // 1️⃣ Mark demo completed
        AppPrefs.markDemoDone(a);

        // 2️⃣ Go to Home
        Intent i = new Intent(a, HomeActivity.class);
        a.startActivity(i);

        // 3️⃣ Close MainActivity
        a.finish();
    }



}
/*package com.example.chathurang;

import android.app.Activity;
import android.graphics.Rect;
import android.os.Handler;
import android.view.View;

import com.getkeepsafe.taptargetview.TapTarget;
import com.getkeepsafe.taptargetview.TapTargetSequence;
import com.getkeepsafe.taptargetview.TapTargetView;

public class DemoController {

    private static final long PAUSE = 1200;

    private final Activity activity;
    private final ChessBoardView board;
    private final View diceView;
    private final View playButton;

    private final Handler h = new Handler();

    public DemoController(Activity a, ChessBoardView b, View dice, View play) {
        activity = a;
        board = b;
        diceView = dice;
        playButton = play;
    }

    public void start() {
        board.engine.isDemoMode = true;
        board.resetGame();

        step0_hook();
    }

    // 0–3s — Emotion
    private void step0_hook() {
        *//*TapTargetViewOnce(
                null,
                "Chess. With Uncertainty.",
                null,
                () -> h.postDelayed(this::step1_diceRule, PAUSE)
        );*//*
        TapTargetView.showFor(activity,
                TapTarget.forBounds(
                                new Rect(
                                        board.getWidth()/2 - 50,
                                        board.getHeight()/2 - 10,
                                        board.getWidth()/2 + 50,
                                        board.getHeight()/2 + 50
                                ),
                                "Chess. With Uncertainty.",
                                "")
                        .transparentTarget(true)
                        .cancelable(false),
                new TapTargetView.Listener() {
                    @Override public void onTargetClick(TapTargetView view) {
                        step1_diceRule();
                    }
                });
    }

    // 3–6s — Dice chooses
    private void step1_diceRule() {
        board.engine.forceDice(Piece.Type.KNIGHT);
        board.invalidate();

        TapTargetViewOnce(
                diceView,
                "Dice chooses the piece type",
                null,
                () -> h.postDelayed(this::step2_playerAgency, PAUSE)
        );
    }

    // 6–12s — Player agency
    private void step2_playerAgency() {
        // hard-coded safe knight move
        int fr = 7, fc = 1; // white knight
        int tr = 5, tc = 2;

        TapTargetViewOnce(
                board,
                "You still choose the move",
                null,
                () -> {
                    board.engine.movePiece(fr, fc, tr, tc);
                    board.invalidate();
                    h.postDelayed(this::step3_rhythm, PAUSE);
                }
        );
    }

    // 12–15s — Rhythm
    private void step3_rhythm() {
        TapTargetViewOnce(
                null,
                "One roll. One move.",
                null,
                () -> h.postDelayed(this::step4_noMove, PAUSE)
        );
    }

    // 15–20s — No valid move
    private void step4_noMove() {
        board.engine.forceDice(Piece.Type.KING);

        TapTargetViewOnce(
                null,
                "No move possible — turn skipped",
                null,
                () -> h.postDelayed(this::step5_strategy, PAUSE)
        );
    }

    // 20–25s — Strategy framing
    private void step5_strategy() {
        board.engine.forceDice(Piece.Type.QUEEN);

        TapTargetViewOnce(
                diceView,
                "Plan around uncertainty",
                null,
                () -> h.postDelayed(this::step6_cta, PAUSE)
        );
    }

    // 25–30s — CTA
    private void step6_cta() {
        TapTargetViewOnce(
                playButton,
                "Luck limits. Skill wins.",
                null,
                this::finish
        );
    }

    private void finish() {
        board.engine.isDemoMode = false;
        AppPrefs.markDemoDone(activity);
        activity.finish();
        activity.startActivity(
                new android.content.Intent(activity, HomeActivity.class)
        );
    }

    // ---------- Helper ----------
    private void TapTargetViewOnce(
            View target,
            String title,
            String desc,
            Runnable onDone
    ) {
        Rect centerRect = new Rect(
                activity.getWindow().getDecorView().getWidth() / 2 - 10,
                activity.getWindow().getDecorView().getHeight() / 2 - 10,
                activity.getWindow().getDecorView().getWidth() / 2 + 10,
                activity.getWindow().getDecorView().getHeight() / 2 + 10
        );
        board.getGlobalVisibleRect(centerRect);
        TapTarget t = (target == null)
                ? TapTarget.forBounds(centerRect, title, desc)
                : TapTarget.forView(target, title, desc);

        TapTargetSequence seq = new TapTargetSequence(activity)
                .targets(t)
                .listener(new TapTargetSequence.Listener() {
                    @Override public void onSequenceFinish() { onDone.run(); }
                    @Override public void onSequenceStep(TapTarget lastTarget, boolean targetClicked) {}
                    @Override public void onSequenceCanceled(TapTarget lastTarget) {}
                });
        seq.start();
    }
}*/

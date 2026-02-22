package com.example.chathurang;

import android.os.CountDownTimer;

public class ClockManager {

    public interface ClockListener {
        void onTick(boolean whiteTurn, long millisLeft);
        void onTimeUp(boolean whiteLost);
        void onLowTimeTick(boolean whiteTurn, long millisLeft);
    }

    private CountDownTimer timer;
    private long whiteTime;
    private long blackTime;
    private boolean whiteTurn;
    private ClockListener listener;
    private long lastVibratedSecond = -1;
    public ClockManager(long initialMillis, ClockListener listener) {
        this.whiteTime = initialMillis;
        this.blackTime = initialMillis;
        this.listener = listener;
    }

    public void startTurn(boolean white) {
        if (whiteTime <= 0 && blackTime <= 0) return;
        stop(); // stop previous timer
        lastVibratedSecond = -1;

        whiteTurn = white;
        long time = white ? whiteTime : blackTime;

        timer = new CountDownTimer(time, 1000) {

            @Override
            public void onTick(long millisUntilFinished) {

                if (whiteTurn) {
                    whiteTime = millisUntilFinished;
                } else {
                    blackTime = millisUntilFinished;
                }

                long secondsLeft = millisUntilFinished / 1000;

                // 🔥 HAPTIC EVERY SECOND UNDER 10
                if (secondsLeft <= 10 && secondsLeft > 0) {

                    if (secondsLeft != lastVibratedSecond) {

                        lastVibratedSecond = secondsLeft;

                        if (listener != null) {
                            listener.onLowTimeTick(whiteTurn, secondsLeft);
                        }
                    }
                }

                if (listener != null) {
                    listener.onTick(whiteTurn, millisUntilFinished);
                }
            }

            @Override
            public void onFinish() {
                if (listener != null) {
                    listener.onTimeUp(whiteTurn);
                }
            }
        }.start();
    }

    public void stop() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    public void reset(long millis) {
        stop();
        whiteTime = millis;
        blackTime = millis;
    }
}
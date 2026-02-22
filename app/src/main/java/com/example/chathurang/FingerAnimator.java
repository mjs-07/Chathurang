package com.example.chathurang;

import android.widget.ImageView;

public class FingerAnimator {

    ImageView finger;

    public FingerAnimator(ImageView finger) {
        this.finger = finger;
    }

    public void tap(float x, float y) {
        finger.setX(x);
        finger.setY(y);
        finger.setScaleX(1f);
        finger.setScaleY(1f);
        finger.setAlpha(0f);

        finger.animate()
                .alpha(1f)
                .scaleX(0.8f)
                .scaleY(0.8f)
                .setDuration(300)
                .withEndAction(() ->
                        finger.animate()
                                .alpha(0f)
                                .setDuration(300)
                                .start()
                ).start();
    }

    public void swipe(float startX, float startY, float endX, float endY) {
        finger.setX(startX);
        finger.setY(startY);
        finger.setAlpha(1f);

        finger.animate()
                .x(endX)
                .y(endY)
                .setDuration(900)
                .withEndAction(() ->
                        finger.animate().alpha(0f).setDuration(200).start()
                ).start();
    }
}


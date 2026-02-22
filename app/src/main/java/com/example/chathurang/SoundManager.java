package com.example.chathurang;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.SoundPool;

public class SoundManager {

    private static SoundPool soundPool;
    private static int diceRollSound;
    private static int pieceMoveSound;
    private static int victorySound;
    private static int defeatSound;

    public static void init(Context context) {
        if (soundPool != null) return;

        AudioAttributes attrs = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();

        soundPool = new SoundPool.Builder()
                .setMaxStreams(4)
                .setAudioAttributes(attrs)
                .build();

        diceRollSound = soundPool.load(context, R.raw.dice_roll, 1);
        pieceMoveSound = soundPool.load(context, R.raw.piece_move, 1);
        victorySound = soundPool.load(context, R.raw.victory_fanfare, 1);
        defeatSound = soundPool.load(context, R.raw.defeat_low_brass, 1);
    }

    public static void playDiceRoll() {
        if (soundPool != null)
            soundPool.play(diceRollSound, 1f, 1f, 1, 0, 1f);
    }

    public static void playPieceMove() {
        if (soundPool != null)
            soundPool.play(pieceMoveSound, 1f, 1f, 1, 0, 1f);
    }

    public static void playVictorySound() {
        if (soundPool != null)
            soundPool.play(victorySound, 1f, 1f, 1, 0, 1f);
    }

    public static void playDefeatSound() {
        if (soundPool != null)
            soundPool.play(defeatSound, 1f, 1f, 1, 0, 1f);
    }

    public static void release() {
        if (soundPool != null) {
            soundPool.release();
            soundPool = null;
        }
    }
}

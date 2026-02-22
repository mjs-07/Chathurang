//ChessBoardView.java
package com.example.chathurang;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ChessBoardView extends View {

    public GameEngine engine;
    private static final long TURN_PAUSE_MS = 800; // UX pause between turns

    // --- UX Timing Constants ---
    private static final long AI_THINK_DELAY = 700;        // before dice roll
    private static final long AI_AFTER_ROLL_DELAY = 600;   // after dice result
    private static final long NO_MOVE_PAUSE = 1000;        // breathing space

    // Last move highlight
    private int lastFromR = -1, lastFromC = -1;
    private int lastToR = -1, lastToC = -1;

    private Paint lastFromPaint = new Paint();
    private Paint lastToPaint = new Paint();


    private Paint lightPaint = new Paint();
    private Paint darkPaint = new Paint();
    private Paint highlightPaint = new Paint();
    private Paint dotPaint = new Paint();

    private int viewWidth = 0, viewHeight = 0;
    private float cell = 0f;
    private float offsetX = 0f, offsetY = 0f;

    // selection + legal moves
    private int selectedR = -1, selectedC = -1;
    private List<int[]> highlighted = new ArrayList<>();

    // animation state
    private boolean animating = false;
    private float animX = 0f, animY = 0f;              // top-left of animated bitmap
    private Bitmap animBmp = null;
    private Piece animPieceRef = null;
    private int animFromR = -1, animFromC = -1;
    private int animToR = -1, animToC = -1;

    private boolean inputBlocked = false;

    public boolean singlePlayer = true;
    public boolean twoPlayerMode = false;

    private UiCallbacks ui;
    public boolean demoMode = false;
    private boolean demoMoveAllowed = false;

    private boolean demoSelectionOnly = false;

    private Random rnd = new Random();
    private boolean pulseKnights = false;
    private float pulseScale = 1f;
    private ValueAnimator pulseAnimator;


    public interface UiCallbacks {
        void onTurnChanged(boolean whiteTurn);
        void onDiceChanged(int value, Piece.Type allowedType);
        void onPromotionRequired(Piece pawn, int toR, int toC);
        void onGameOver(String reason);

        // called when a roll produced no legal moves for the current player
        void onNoValidMoves(boolean wasHumanTurn); // true if it was the human's (white) turn
        void onDiceRollStart();
    }

    public interface DemoMoveListener {
        void onDemoMoveCompleted();
    }

    private DemoMoveListener demoMoveListener;

    public void setDemoMoveListener(DemoMoveListener l) {
        demoMoveListener = l;
    }


    public void setUiCallbacks(UiCallbacks cb) { this.ui = cb; }

    public ChessBoardView(Context c) { this(c, null); }
    public ChessBoardView(Context c, AttributeSet a) {
        super(c, a);
        init();
    }

    private void init() {
        //lightPaint.setColor(Color.parseColor("#EEEED2"));
        //darkPaint.setColor(Color.parseColor("#769656"));
        applyTheme(ThemeManager.getTheme());
        highlightPaint.setColor(Color.argb(140, 255, 220, 40));
        highlightPaint.setStyle(Paint.Style.FILL);
        dotPaint.setColor(Color.parseColor("#333333"));
        dotPaint.setAlpha(200);



        lastFromPaint.setColor(Color.argb(140, 70, 130, 255)); // soft blue
        lastFromPaint.setStyle(Paint.Style.FILL);

        lastToPaint.setColor(Color.argb(160, 255, 200, 70)); // warm gold
        lastToPaint.setStyle(Paint.Style.FILL);


        engine = new GameEngine(new GameEngine.GameListener() {
            @Override public void onBoardChanged() {
                // bitmaps will be (re)loaded when size known (onSizeChanged calls load)
                postInvalidate();
                if (ui != null) ui.onTurnChanged(engine.whiteTurn);
                if (ui != null) ui.onDiceChanged(engine.diceValue, engine.allowedTypeThisTurn);
            }

            @Override public void onGameOver(String reason) {
                if (ui != null) ui.onGameOver(reason);
            }

            @Override
            public void onPromotionRequired(Piece pawn, int toR, int toC) {
                if (ui != null) {
                    ui.onPromotionRequired(pawn, toR, toC);
                }
            }


        });

        // initialize engine board (bitmap load waits for onSizeChanged)
        engine.resetBoard();
    }

    // -------------------------
    // Size & bitmap handling
    // -------------------------
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        viewWidth = w;
        viewHeight = h;
        computeSizing();
        loadPieceBitmaps();
        invalidate();
    }

    private void computeSizing() {
        float min = Math.min(viewWidth, viewHeight);
        cell = min / 8f;
        offsetX = (viewWidth - 8f * cell) / 2f;
        offsetY = (viewHeight - 8f * cell) / 2f;
    }

    public void loadPieceBitmaps() {
        if (viewWidth <= 0 || viewHeight <= 0 || cell <= 0f) return;

        int size = Math.max(1, Math.round(cell));

        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece p = engine.board[r][c];
                if (p != null) {
                    int res = getResources().getIdentifier(p.getResName(), "drawable", getContext().getPackageName());
                    if (res != 0) {
                        Bitmap decoded = BitmapFactory.decodeResource(getResources(), res);
                        if (decoded != null) {
                            Bitmap scaled = Bitmap.createScaledBitmap(decoded, size, size, true);
                            p.bitmap = scaled;
                            // cache rotated bitmap for black pieces as well as white (to simplify drawing)
                            Matrix m = new Matrix();
                            // rotate in two-player mode if you want pieces upside-down for the player at top.
                            if (!singlePlayer) {
                                m.postRotate(180f);
                            }
                            p.bitmapRotated = Bitmap.createBitmap(scaled, 0, 0, scaled.getWidth(), scaled.getHeight(), m, true);
                        } else {
                            p.bitmap = null;
                            p.bitmapRotated = null;
                        }
                    } else {
                        p.bitmap = null;
                        p.bitmapRotated = null;
                    }
                }
            }
        }
    }

    // -------------------------
    // Drawing
    // -------------------------
    @Override
    protected void onDraw(android.graphics.Canvas canvas) {
        super.onDraw(canvas);

        float drawCell = cell;
        float ox = offsetX;
        float oy = offsetY;

        // Draw squares (light/dark pattern)
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                float left = ox + c * drawCell;
                float top = oy + r * drawCell;
                canvas.drawRect(left, top, left + drawCell, top + drawCell, ((r + c) % 2 == 0) ? lightPaint : darkPaint);
            }
        }

        // selection highlight
        if (selectedR >= 0 && selectedC >= 0) {
            float left = ox + selectedC * drawCell;
            float top  = oy + selectedR * drawCell;
            canvas.drawRect(left, top, left + drawCell, top + drawCell, highlightPaint);
        }

        // grey dots for legal moves
        for (int[] m : highlighted) {
            float cx = ox + m[1] * drawCell + drawCell / 2f;
            float cy = oy + m[0] * drawCell + drawCell / 2f;
            canvas.drawCircle(cx, cy, drawCell * 0.15f, dotPaint);
        }

        // 🔹 Last move highlight (FROM)
        if (lastFromR >= 0 && lastFromC >= 0) {
            float left = offsetX + lastFromC * cell;
            float top = offsetY + lastFromR * cell;
            canvas.drawRect(left, top, left + cell, top + cell, lastFromPaint);
        }

// 🔸 Last move highlight (TO)
        if (lastToR >= 0 && lastToC >= 0) {
            float left = offsetX + lastToC * cell;
            float top = offsetY + lastToR * cell;
            canvas.drawRect(left, top, left + cell, top + cell, lastToPaint);
        }


        // draw pieces (skip animating source)
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                if (animating && r == animFromR && c == animFromC) continue;
                Piece p = engine.board[r][c];
                if (p == null) continue;

                Bitmap toDraw;
                if (!singlePlayer) {
                    // two-player: optionally rotate bitmaps so player on top faces camera
                    toDraw = (!engine.whiteTurn) ? p.bitmapRotated : p.bitmap;
                } else {
                    // single-player: white upright, black rotated so black faces away
                    toDraw = (p.color == Piece.Color.BLACK) ? p.bitmapRotated : p.bitmap;
                }
                if (toDraw == null) continue;

                float left = ox + c * drawCell;
                float top = oy + r * drawCell;
                if (pulseKnights && p.type == Piece.Type.KNIGHT && p.color == Piece.Color.WHITE) {

                    canvas.save();

                    float cx = left + cell / 2f;
                    float cy = top + cell / 2f;

                    canvas.scale(pulseScale, pulseScale, cx, cy);
                    canvas.drawBitmap(toDraw, left, top, null);

                    canvas.restore();

                } else {
                    canvas.drawBitmap(toDraw, left, top, null);
                }

            }
        }

        // draw animating piece on top (safe)
        if (animating && animPieceRef != null) {
            Bitmap bmpToUse = (animPieceRef.color == Piece.Color.BLACK) ? animPieceRef.bitmapRotated : animPieceRef.bitmap;
            if (bmpToUse != null) {
                canvas.drawBitmap(bmpToUse, animX, animY, null);
            } else if (animBmp != null) {
                canvas.drawBitmap(animBmp, animX, animY, null);
            }
        }
    }

    // -------------------------
    // Touch handling (click-to-move)
    // -------------------------
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (engine.isDemoMode) return true;

        if ((inputBlocked && !demoMoveAllowed) || engine.gameOver) return true;
        if (event.getAction() != MotionEvent.ACTION_DOWN) return true;

        float x = event.getX(), y = event.getY();
        if (x < offsetX || y < offsetY || x > offsetX + 8 * cell || y > offsetY + 8 * cell) {
            clearSelection();
            return true;
        }
        int c = (int) ((x - offsetX) / cell);
        int r = (int) ((y - offsetY) / cell);

        // if tapped a highlighted move -> perform it
        for (int[] m : highlighted) {
            if (m[0] == r && m[1] == c) {
                int fromR = selectedR, fromC = selectedC;
                clearSelectionImmediate(); // clear selection now (we saved from coords)
                performMoveAnimated(fromR, fromC, r, c);
                return true;
            }
        }

        // otherwise, select piece if it's the current player's piece
        Piece p = engine.board[r][c];

        // Disallow selection if game over or input blocked
        if (engine.gameOver || inputBlocked) {
            clearSelection();
            return true;
        }

        // Selection: ensure the tapped piece belongs to the current player
        if (p != null && engine.isCurrentPlayerPiece(p)) {
            // Enforce dice: must roll first (allowedTypeThisTurn must be set) except if we are allowing selection for review
            if (engine.allowedTypeThisTurn == null) {
                clearSelection();
                return true;
            }

            // ensure piece matches dice type
            if (engine.allowedTypeThisTurn != null && p.type != engine.allowedTypeThisTurn) {
                clearSelection();
                return true;
            }

            // Selecting a piece
            selectedR = r;
            selectedC = c;

            /*// DEMO STEP: first tap → select only
            if (demoSelectionOnly) {
                highlighted.clear();
                invalidate();
                demoSelectionOnly = false; // unlock move dots on NEXT tap
                return true;
            }*/

            // Normal flow: show legal moves
            highlighted = engine.getLegalMoves(r, c);
            invalidate();


        } else {
            clearSelection();
        }

        return true;
    }

    private void clearSelectionImmediate() {
        selectedR = selectedC = -1;
        highlighted.clear();
        invalidate();
    }
    private void clearSelection() { clearSelectionImmediate(); }

    // -------------------------
    // Animation & move execution
    // -------------------------
    private void performMoveAnimated(int fr, int fc, int tr, int tc) {

        if (!in(fr, fc) || !in(tr, tc)) return;

        Piece p = engine.board[fr][fc];
        if (p == null) return;

        inputBlocked = true;
        animFromR = fr; animFromC = fc;
        animToR = tr; animToC = tc;
        animPieceRef = p;

        // choose animation bitmap (use rotated for black)
        animBmp = (p.color == Piece.Color.BLACK && p.bitmapRotated != null) ? p.bitmapRotated : p.bitmap;
        if (animBmp == null) {
            engine.movePiece(fr, fc, tr, tc);
            inputBlocked = false;
            postInvalidate();
            return;
        }

        float startX = offsetX + fc * cell;
        float startY = offsetY + fr * cell;
        float endX = offsetX + tc * cell;
        float endY = offsetY + tr * cell;

        animX = startX; animY = startY;
        animating = true;

        ValueAnimator va = ValueAnimator.ofFloat(0f, 1f);
        va.setDuration(280);
        va.setInterpolator(new DecelerateInterpolator());
        va.addUpdateListener(animation -> {
            float t = (float) animation.getAnimatedValue();
            animX = startX + (endX - startX) * t;
            animY = startY + (endY - startY) * t;
            invalidate();
        });

        HapticHelper.vibrate(this.getContext(), 20);

        va.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(android.animation.Animator animation) {
                animating = false;

                SoundManager.playPieceMove();

                boolean isCapture = engine.board[animToR][animToC] != null;

                if (isCapture) {
                    HapticHelper.vibrate(getContext(), 60);
                } else {
                    HapticHelper.vibrate(getContext(), 20);
                }

                boolean moved = engine.movePiece(fr, fc, tr, tc);

                // ✅ Store last move for highlight
                lastFromR = fr;
                lastFromC = fc;
                lastToR = tr;
                lastToC = tc;

                // If promotion is pending, STOP everything here
                if (engine.pendingPromotion != null) {
                    loadPieceBitmaps();
                    invalidate();
                    inputBlocked = false;
                    return; // 🔥 THIS WAS MISSING
                }

                // Normal flow
                loadPieceBitmaps();
                invalidate();
                postDelayed(() -> {
                    inputBlocked = false;
                    // schedule AI if needed (AI is Black in singlePlayer)
                    if (!demoMode && singlePlayer && !engine.whiteTurn && !engine.gameOver) {
                        postDelayed(() -> aiTakeTurn(), TURN_PAUSE_MS);
                    }
                }, TURN_PAUSE_MS);

                if (demoMoveAllowed && demoMoveListener != null) {
                    demoMoveAllowed = false;
                    demoMoveListener.onDemoMoveCompleted();
                }
                inputBlocked = false;
            }
        });

        va.start();
    }
    public void setInputBlocked(boolean blocked) {
        this.inputBlocked = blocked;
    }

    // ---------------------------------------------------------
    // PUBLIC: Reset game (called from MainActivity)
    // ---------------------------------------------------------
    public void resetGame() {
        lastFromR = lastFromC = -1;
        lastToR = lastToC = -1;

        if (engine != null) {
            engine.resetBoard();
        }
        clearSelectionImmediate();
        loadPieceBitmaps();
        invalidate();
    }

    // PUBLIC API: player presses Roll button
    public void playerRollDice() {
        lastFromR = lastFromC = -1;
        lastToR = lastToC = -1;
        invalidate();

        if (engine == null) return;
        if (engine.gameOver || inputBlocked) return;

        // Enforce only one roll per turn:
        if (engine.allowedTypeThisTurn != null) {
            // roll already used this turn
            return;
        }

        // HUMAN ROLL LOGIC:
        // Allowed when:
        //  1) Two-player mode  → the player whose turn it is may roll
        //  2) Single-player mode → only White (the human) may roll
        if (singlePlayer && !engine.whiteTurn) return;

        // Notify UI to start dice animation and let UI (MainActivity) disable the Roll button immediately.
        if (ui != null) ui.onDiceRollStart();

        HapticHelper.vibrate(getContext(), 40);

        // After animation delay, perform actual roll and notify UI
        postDelayed(() -> {
            if (engine == null) return;

            int v = engine.rollDice(); // sets diceValue and allowedTypeThisTurn
            if (ui != null) ui.onDiceChanged(v, engine.allowedTypeThisTurn);

            boolean any = hasAnyLegalMoveForDice();
            if (!any) {

                // 🔔 Notify UI first (message / pause)
                if (ui != null) ui.onNoValidMoves(engine.whiteTurn);

                // ⏸ Pause BEFORE turn flips
                postDelayed(() -> {
                    engine.endTurn();
                    inputBlocked = false;
                    if (ui != null) ui.onTurnChanged(engine.whiteTurn);

                    // If AI's turn next, schedule AI AFTER pause
                    if (!demoMode && singlePlayer && !engine.whiteTurn && !engine.gameOver) {
                        postDelayed(this::aiTakeTurn, TURN_PAUSE_MS);
                    }
                }, TURN_PAUSE_MS);

                return;
            }

        }, 520); // animation duration (adjust to match your dice animation)
    }

    // AI turn: roll dice + move (Black)
    public void aiTakeTurn() {
        if (!singlePlayer) return;
        if (engine == null || engine.gameOver || inputBlocked) return;
        if (engine.whiteTurn) return; // not AI's turn

        inputBlocked = true;

        // 1️⃣ Small pause before AI does anything (feels human)
        postDelayed(() -> {

            // 2️⃣ Dice roll animation
            if (ui != null) ui.onDiceRollStart();

            postDelayed(() -> {

                int v = engine.rollDice();
                if (ui != null) ui.onDiceChanged(v, engine.allowedTypeThisTurn);

                // 3️⃣ Pause after dice roll (thinking time)
                postDelayed(() -> {

                    boolean anyMove = hasAnyLegalMoveForDice();

                    // 🚫 NO VALID MOVES
                    if (!anyMove) {
                        if (ui != null) ui.onNoValidMoves(false);

                        postDelayed(() -> {
                            engine.endTurn();
                            inputBlocked = false;
                            if (ui != null) ui.onTurnChanged(engine.whiteTurn);
                        }, NO_MOVE_PAUSE);

                        return;
                    }

                    // ✅ VALID MOVE EXISTS
                    int[] move = engine.chooseAIMove();
                    if (move != null) {
                        performMoveAnimated(move[0], move[1], move[2], move[3]);
                    } else {
                        // Safety fallback
                        postDelayed(() -> {
                            engine.endTurn();
                            inputBlocked = false;
                            if (ui != null) ui.onTurnChanged(engine.whiteTurn);
                        }, NO_MOVE_PAUSE);
                    }

                }, AI_AFTER_ROLL_DELAY);

            }, 520); // dice animation duration

        }, AI_THINK_DELAY);
    }


    private boolean hasAnyLegalMoveForDice() {
        Piece.Type need = engine.allowedTypeThisTurn;
        if (need == null) return false;
        boolean white = engine.whiteTurn;
        for (int r=0;r<8;r++) for (int c=0;c<8;c++) {
            Piece p = engine.board[r][c];
            if (p != null && p.color == (white ? Piece.Color.WHITE : Piece.Color.BLACK) && p.type == need) {
                List<int[]> moves = engine.getLegalMoves(r,c);
                if (moves != null && !moves.isEmpty()) return true;
            }
        }
        return false;
    }

    // -------------------------
    // Utility & helpers
    // -------------------------
    private boolean in(int r,int c) { return r>=0 && r<8 && c>=0 && c<8; }

    public void setSinglePlayer(boolean v) {
        this.singlePlayer = v;
        this.twoPlayerMode = !v;
    }

    public void setTwoPlayerMode(boolean v) {
        this.twoPlayerMode = v;
        this.singlePlayer = !v;
    }

    public void enableDemoMode() {
        demoMode = true;
        inputBlocked = true;
    }

    public void disableDemoMode() {
        demoMode = false;
        inputBlocked = false;
    }

    public void forceDice(int value) {
        engine.diceValue = value;
        engine.allowedTypeThisTurn = GameEngine.diceToType(value);
        if (ui != null) ui.onDiceChanged(value, engine.allowedTypeThisTurn);
    }

    public void forceMove(int fr, int fc, int tr, int tc) {
        performMoveAnimated(fr, fc, tr, tc);
    }

    /*public void highlightOnlyType(Piece.Type t) {
        highlighted.clear();
        for (int r = 0; r < 8; r++)
            for (int c = 0; c < 8; c++) {
                Piece p = engine.board[r][c];
                if (p != null && p.type == t && p.color == Piece.Color.WHITE) {
                    highlighted.addAll(engine.getLegalMoves(r, c));
                }
            }
        invalidate();
    }*/

    public void highlightOnlyType(Piece.Type type) {
        highlighted.clear();
        selectedR = selectedC = -1;

        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece p = engine.board[r][c];
                if (p != null && p.type == type && engine.isCurrentPlayerPiece(p)) {
                    // draw glow only, no moves
                }
            }
        }
        invalidate();
    }

    public void setDemoMoveAllowed(boolean allowed) {
        demoMoveAllowed = allowed;
    }

    public void enableDemoSelectionOnly(boolean enable) {
        demoSelectionOnly = enable;
    }

    public void setDemoMode(boolean demo){
        demoMode = demo;
    }

    public void startKnightPulse() {
        pulseKnights = true;

        if (pulseAnimator != null) pulseAnimator.cancel();

        pulseAnimator = ValueAnimator.ofFloat(1f, 1.12f);
        pulseAnimator.setDuration(500);
        pulseAnimator.setRepeatMode(ValueAnimator.REVERSE);
        pulseAnimator.setRepeatCount(ValueAnimator.INFINITE);

        pulseAnimator.addUpdateListener(a -> {
            pulseScale = (float) a.getAnimatedValue();
            invalidate();
        });

        pulseAnimator.start();
    }
    public void stopKnightPulse() {
        pulseKnights = false;
        pulseScale = 1f;
        if (pulseAnimator != null) pulseAnimator.cancel();
        invalidate();
    }
    public void applyTheme(GameTheme theme) {
        if (theme == GameTheme.DARK_ELEGANT) {
            lightPaint.setColor(Color.parseColor("#F0D9B5"));
            darkPaint.setColor(Color.parseColor("#B58863"));
        } else {
            lightPaint.setColor(Color.parseColor("#EEEED2"));
            darkPaint.setColor(Color.parseColor("#769656"));
        }
        invalidate();
    }


}

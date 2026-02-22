//GameEngine.java
package com.example.chathurang;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GameEngine {

    public Piece[][] board = new Piece[8][8];
    public boolean whiteTurn = true;
    public int diceValue = -1;
    public Piece.Type allowedTypeThisTurn = null;
    public boolean gameOver = false;

    public int[] enPassantTarget = null; // (row,col)
    public PendingPromotion pendingPromotion = null;
    public boolean blackIsAI = true;
    public boolean isDemoMode = false;



    public static class PendingPromotion {
        public final Piece pawn;
        public final int toR, toC;
        public PendingPromotion(Piece p, int r, int c) {
            pawn = p; toR = r; toC = c;
        }
    }

    public interface GameListener {
        void onBoardChanged();
        void onGameOver(String reason);
        void onPromotionRequired(Piece pawn, int toR, int toC);
    }

    public GameListener listener;
    private Random rnd = new Random();

    public GameEngine(GameListener l) { listener = l; }

    // ---------------------------------------------------
    // RESET BOARD
    // ---------------------------------------------------
    // White on bottom rows 6-7, Black on top rows 0-1
    public void resetBoard() {
        board = new Piece[8][8];
        whiteTurn = true;
        diceValue = -1;
        allowedTypeThisTurn = null;
        enPassantTarget = null;
        pendingPromotion = null;
        gameOver = false;

        // Black pawns on row 1, black back rank on row 0 (top)
        for (int c = 0; c < 8; c++) {
            board[1][c] = new Piece(Piece.Type.PAWN, Piece.Color.BLACK, 1, c, null);
        }
        board[0][0] = new Piece(Piece.Type.ROOK, Piece.Color.BLACK, 0,0,null);
        board[0][1] = new Piece(Piece.Type.KNIGHT, Piece.Color.BLACK, 0,1,null);
        board[0][2] = new Piece(Piece.Type.BISHOP, Piece.Color.BLACK, 0,2,null);
        board[0][3] = new Piece(Piece.Type.QUEEN, Piece.Color.BLACK, 0,3,null);
        board[0][4] = new Piece(Piece.Type.KING, Piece.Color.BLACK, 0,4,null);
        board[0][5] = new Piece(Piece.Type.BISHOP, Piece.Color.BLACK, 0,5,null);
        board[0][6] = new Piece(Piece.Type.KNIGHT, Piece.Color.BLACK, 0,6,null);
        board[0][7] = new Piece(Piece.Type.ROOK, Piece.Color.BLACK, 0,7,null);

        // White pawns on row 6, white back rank on row 7 (bottom)
        for (int c = 0; c < 8; c++) {
            board[6][c] = new Piece(Piece.Type.PAWN, Piece.Color.WHITE, 6, c, null);
        }
        board[7][0] = new Piece(Piece.Type.ROOK, Piece.Color.WHITE, 7,0,null);
        board[7][1] = new Piece(Piece.Type.KNIGHT, Piece.Color.WHITE, 7,1,null);
        board[7][2] = new Piece(Piece.Type.BISHOP, Piece.Color.WHITE, 7,2,null);
        board[7][3] = new Piece(Piece.Type.QUEEN, Piece.Color.WHITE, 7,3,null);
        board[7][4] = new Piece(Piece.Type.KING, Piece.Color.WHITE, 7,4,null);
        board[7][5] = new Piece(Piece.Type.BISHOP, Piece.Color.WHITE, 7,5,null);
        board[7][6] = new Piece(Piece.Type.KNIGHT, Piece.Color.WHITE, 7,6,null);
        board[7][7] = new Piece(Piece.Type.ROOK, Piece.Color.WHITE, 7,7,null);

        if (listener != null) listener.onBoardChanged();
    }

    // ---------------------------------------------------
    // DICE LOGIC
    // ---------------------------------------------------
    public int rollDice() {
        int v = rnd.nextInt(6) + 1;
        diceValue = v;
        allowedTypeThisTurn = diceToType(v);
        return v;
    }

    public static Piece.Type diceToType(int v) {
        switch (v) {
            case 1: return Piece.Type.PAWN;
            case 2: return Piece.Type.KNIGHT;
            case 3: return Piece.Type.BISHOP;
            case 4: return Piece.Type.ROOK;
            case 5: return Piece.Type.QUEEN;
            case 6: return Piece.Type.KING;
        }
        return null;
    }

    // ---------------------------------------------------
    // MOVE GENERATION
    // ---------------------------------------------------
    public List<int[]> getLegalMoves(int r, int c) {
        List<int[]> out = new ArrayList<>();
        Piece p = board[r][c];
        if (p == null) return out;

        // dice restriction
        if (allowedTypeThisTurn != null && p.type != allowedTypeThisTurn)
            return out;

        switch (p.type) {
            case PAWN: pawnMoves(p, out); break;
            case KNIGHT: knightMoves(p, out); break;
            case BISHOP: slideMoves(p, out, dirsDiag); break;
            case ROOK:   slideMoves(p, out, dirsStraight); break;
            case QUEEN:  slideMoves(p, out, dirsQueen); break;
            case KING:   kingMoves(p, out); castlingMoves(p, out); break;
        }
        return out;
    }

    private static final int[][] dirsStraight = {{1,0},{-1,0},{0,1},{0,-1}};
    private static final int[][] dirsDiag     = {{1,1},{1,-1},{-1,1},{-1,-1}};
    private static final int[][] dirsQueen    = {
            {1,0},{-1,0},{0,1},{0,-1},
            {1,1},{1,-1},{-1,1},{-1,-1}
    };

    private void knightMoves(Piece p, List<int[]> out) {
        int[][] k = {{2,1},{2,-1},{-2,1},{-2,-1},{1,2},{1,-2},{-1,2},{-1,-2}};
        for (int[] d : k) {
            int r = p.row + d[0], c = p.col + d[1];
            if (in(r,c) && (board[r][c] == null || board[r][c].color != p.color))
                out.add(new int[]{r,c});
        }
    }

    private void slideMoves(Piece p, List<int[]> out, int[][] dirs) {
        for (int[] d : dirs) {
            for (int s = 1; s < 8; s++) {
                int r = p.row + d[0]*s, c = p.col + d[1]*s;
                if (!in(r,c)) break;
                if (board[r][c] == null) out.add(new int[]{r,c});
                else {
                    if (board[r][c].color != p.color)
                        out.add(new int[]{r,c});
                    break;
                }
            }
        }
    }

    // Pawn logic: white moves up (row decreases), black moves down (row increases)
    private void pawnMoves(Piece p, List<int[]> out) {
        int dir = (p.color == Piece.Color.WHITE) ? -1 : 1;
        int oneR = p.row + dir;

        // Forward 1 if empty
        if (in(oneR, p.col) && board[oneR][p.col] == null) {
            out.add(new int[]{oneR, p.col});
            // Forward 2 from initial rank (only if forward 1 also empty)
            int twoR = p.row + 2 * dir;
            if (!p.hasMoved && in(twoR, p.col) && board[twoR][p.col] == null && board[oneR][p.col] == null) {
                out.add(new int[]{twoR, p.col});
            }
        }

        // Diagonal captures and en-passant
        for (int dc : new int[]{-1, 1}) {
            int cr = p.row + dir;
            int cc = p.col + dc;
            if (!in(cr, cc)) continue;

            // Normal capture
            if (board[cr][cc] != null && board[cr][cc].color != p.color) {
                out.add(new int[]{cr, cc});
            } else {
                // EN PASSANT: landing square equals enPassantTarget
                if (enPassantTarget != null &&
                        enPassantTarget[0] == cr &&
                        enPassantTarget[1] == cc) {

                    int pawnRow = p.row;
                    int pawnCol = cc;

                    if (in(pawnRow, pawnCol)) {
                        Piece adjacentPawn = board[pawnRow][pawnCol];

                        if (adjacentPawn != null &&
                                adjacentPawn.type == Piece.Type.PAWN &&
                                adjacentPawn.color != p.color) {

                            out.add(new int[]{cr, cc});
                        }
                    }
                }
            }
        }
    }

    private void kingMoves(Piece p, List<int[]> out) {
        for (int dr=-1; dr<=1; dr++)
            for (int dc=-1; dc<=1; dc++) {
                if (dr==0 && dc==0) continue;
                int r = p.row + dr, c = p.col + dc;
                if (in(r,c) && (board[r][c] == null || board[r][c].color != p.color))
                    out.add(new int[]{r,c});
            }
    }

    private void castlingMoves(Piece k, List<int[]> out) {
        if (k.hasMoved) return;
        int r = k.row;

        // king-side
        if (castleClear(r,4,7))
            out.add(new int[]{r,6});

        // queen-side
        if (castleClear(r,4,0))
            out.add(new int[]{r,2});
    }

    private boolean castleClear(int r, int kingCol, int rookCol) {
        // Ensure there is a king at kingCol and it's the correct piece
        Piece king = board[r][kingCol];
        if (king == null || king.type != Piece.Type.KING) return false;

        Piece rook = board[r][rookCol];
        if (rook == null || rook.type != Piece.Type.ROOK) return false;

        // Both must be same color
        if (rook.color != king.color) return false;

        // Neither must have moved
        if (king.hasMoved || rook.hasMoved) return false;

        // Squares between king and rook must be empty
        int start = Math.min(kingCol, rookCol) + 1;
        int end = Math.max(kingCol, rookCol) - 1;
        for (int c = start; c <= end; c++) {
            if (board[r][c] != null) return false;
        }

        return true;
    }

    // ---------------------------------------------------
    // MOVE EXECUTION
    // ---------------------------------------------------
    public boolean movePiece(int fr, int fc, int tr, int tc) {
        if (!in(fr,fc) || !in(tr,tc)) return false;

        Piece p = board[fr][fc];
        if (p == null) return false;

        // legal move?
        boolean isLegal = false;
        for (int[] m : getLegalMoves(fr,fc))
            if (m[0]==tr && m[1]==tc)
                isLegal = true;

        if (!isLegal) return false;

        // EN PASSANT capture removal
        if (p.type == Piece.Type.PAWN &&
                enPassantTarget != null &&
                enPassantTarget[0] == tr &&
                enPassantTarget[1] == tc &&
                board[tr][tc] == null) {

            int capR = (p.color == Piece.Color.WHITE) ? tr + 1 : tr - 1;
            if (in(capR, tc)) board[capR][tc] = null;
        }

        // CASTLING
        if (p.type == Piece.Type.KING && Math.abs(tc-fc)==2) {
            if (tc==6) { // king-side
                Piece rook = board[fr][7];
                board[fr][5] = rook;
                board[fr][7] = null;
                rook.col = 5;
                rook.hasMoved = true;
            } else {     // queen-side
                Piece rook = board[fr][0];
                board[fr][3] = rook;
                board[fr][0] = null;
                rook.col = 3;
                rook.hasMoved = true;
            }
        }

        // PAWN DOUBLE-STEP (set en-passant target)
        enPassantTarget = null;
        if (p.type == Piece.Type.PAWN && Math.abs(tr-fr)==2)
            enPassantTarget = new int[]{ (fr+tr)/2 , fc };

        // KING CAPTURE ends game immediately
        if (board[tr][tc]!=null && board[tr][tc].type==Piece.Type.KING) {
            board[tr][tc] = p;
            board[fr][fc] = null;
            p.row = tr; p.col = tc;
            gameOver = true;
            if (listener!=null)
                listener.onGameOver((p.color== Piece.Color.WHITE?"White":"Black")+" captured the king!");
            return true;
        }

        // MOVE PIECE
        board[tr][tc] = p;
        board[fr][fc] = null;
        p.row = tr; p.col = tc;
        p.hasMoved = true;

        // PROMOTION?
        if (p.type == Piece.Type.PAWN && (tr == 7 || tr == 0)) {

            boolean isHumanPromotion =
                    (p.color == Piece.Color.WHITE) ||
                            (p.color == Piece.Color.BLACK && !blackIsAI);

            if (isHumanPromotion) {
                pendingPromotion = new PendingPromotion(p, tr, tc);
                if (listener != null) listener.onPromotionRequired(p, tr, tc);
            } else {
                // AI promotion only
                p.type = Piece.Type.QUEEN;
                endTurn();
            }
            return true;
        }



        endTurn();
        return true;
    }

    public void endTurn() {
        diceValue = -1;
        allowedTypeThisTurn = null;
        whiteTurn = !whiteTurn;
        if (listener != null) listener.onBoardChanged();
    }

    public boolean completePromotion(Piece.Type t) {
        if (pendingPromotion == null) return false;

        pendingPromotion.pawn.type = t;
        pendingPromotion.pawn.hasMoved = true;

        pendingPromotion = null;

        // FULL CLEAN RESET
        diceValue = -1;
        allowedTypeThisTurn = null;
        enPassantTarget = null;

        whiteTurn = !whiteTurn;

        if (listener != null) listener.onBoardChanged();
        return true;
    }

    private boolean in(int r,int c){ return r>=0&&r<8&&c>=0&&c<8; }

    // -------------------------
    // Helper for view/AI
    // -------------------------
    /**
     * Returns true if the piece belongs to the current player to move.
     */
    public boolean isCurrentPlayerPiece(Piece p) {
        if (p == null) return false;
        return (whiteTurn && p.color == Piece.Color.WHITE) || (!whiteTurn && p.color == Piece.Color.BLACK);
    }

    /**
     * Choose a move for AI (simple heuristic).
     * Returns int[]{fromR, fromC, toR, toC} or null if no move.
     */
    public int[] chooseAIMove() {
        // collect movable pieces for current side
        List<int[]> candidates = new ArrayList<>();
        for (int r=0;r<8;r++) for (int c=0;c<8;c++) {
            Piece p = board[r][c];
            if (p==null) continue;
            if (!isCurrentPlayerPiece(p)) continue;
            if (allowedTypeThisTurn != null && p.type != allowedTypeThisTurn) continue;
            List<int[]> moves = getLegalMoves(r,c);
            for (int[] m : moves) {
                candidates.add(new int[]{r,c,m[0],m[1]});
            }
        }
        if (candidates.isEmpty()) return null;

        // score each candidate and pick best
        int bestIdx = -1;
        int bestScore = Integer.MIN_VALUE;
        for (int i=0;i<candidates.size();i++) {
            int[] mv = candidates.get(i);
            int fr = mv[0], fc = mv[1], tr = mv[2], tc = mv[3];
            int score = evaluateMoveForAI(fr,fc,tr,tc);
            if (score > bestScore || (score==bestScore && rnd.nextBoolean())) {
                bestScore = score; bestIdx = i;
            }
        }
        return candidates.get(bestIdx);
    }

    private int pieceValue(Piece p) {
        if (p==null) return 0;
        switch (p.type) {
            case PAWN: return 100;
            case KNIGHT: return 320;
            case BISHOP: return 330;
            case ROOK: return 500;
            case QUEEN: return 900;
            case KING: return 10000;
        }
        return 0;
    }

    private int evaluateMoveForAI(int fr, int fc, int tr, int tc) {
        int score = 0;
        Piece target = board[tr][tc];
        if (target != null && target.color != (whiteTurn ? Piece.Color.WHITE : Piece.Color.BLACK)) {
            score += pieceValue(target) * 10; // prioritize captures
        }

        // mobility after the move (simulate)
        Piece moving = board[fr][fc];
        Piece backupFrom = moving;
        Piece backupTo = board[tr][tc];
        board[tr][tc] = moving;
        board[fr][fc] = null;
        int mobility = getLegalMoves(tr,tc).size();
        // restore
        board[fr][fc] = backupFrom;
        board[tr][tc] = backupTo;

        score += mobility * 5;
        score += rnd.nextInt(10); // small randomness to vary moves
        return score;
    }
    public void forceDice(Piece.Type type) {
        allowedTypeThisTurn = type;
        diceValue = mapTypeToDice(type);
    }

    private int mapTypeToDice(Piece.Type t) {
        switch (t) {
            case PAWN: return 1;
            case KNIGHT: return 2;
            case BISHOP: return 3;
            case ROOK: return 4;
            case QUEEN: return 5;
            case KING: return 6;
        }
        return -1;
    }

    public void notifyTurnStart() {
        if (listener != null) {
            listener.onBoardChanged();
        }
    }

}

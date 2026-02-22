package com.example.chathurang;

import android.graphics.Bitmap;

public class Piece {
    public enum Type { PAWN, KNIGHT, BISHOP, ROOK, QUEEN, KING }
    public enum Color { WHITE, BLACK }

    public Type type;
    public Color color;
    public int row;
    public int col;
    public Bitmap bitmap;         // scaled bitmap for drawing (white as-is)
    public Bitmap bitmapRotated;  // for black pieces (rotated) - cached
    public boolean hasMoved;

    public Piece(Type type, Color color, int row, int col, Bitmap bmp) {
        this.type = type;
        this.color = color;
        this.row = row;
        this.col = col;
        this.bitmap = bmp;
        this.bitmapRotated = null;
        this.hasMoved = false;
    }

    public String getResName() {
        String prefix = (this.color == Color.WHITE) ? "w" : "b";
        String body;
        switch (this.type) {
            case PAWN:   body = "pawn"; break;
            case KNIGHT: body = "knight"; break;
            case BISHOP: body = "bishop"; break;
            case ROOK:   body = "rook"; break;
            case QUEEN:  body = "queen"; break;
            case KING:   body = "king"; break;
            default:     body = "pawn"; break;
        }
        return prefix + body;
    }
}

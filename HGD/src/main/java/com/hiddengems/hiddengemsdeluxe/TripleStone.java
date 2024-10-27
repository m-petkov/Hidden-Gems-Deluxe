package com.hiddengems.hiddengemsdeluxe;

import javafx.scene.canvas.GraphicsContext;

class TripleStone {
    private final HiddenGemsApplication hiddenGemsApplication;
    int row;
    int col;
    char[] colors;

    TripleStone(HiddenGemsApplication hiddenGemsApplication, int row, int col, char[] colors) {
        this.hiddenGemsApplication = hiddenGemsApplication;
        this.row = row;
        this.col = col;
        this.colors = colors;
    }

    void draw(GraphicsContext gc, double offsetX, double offsetY, double size) {
        for (int i = 0; i < colors.length; i++) {
            double x = offsetX + col * size;
            double y = offsetY + (row + i) * size;
            hiddenGemsApplication.drawCell(gc, colors[i], x, y, size);
        }
    }

    void drawPreview(GraphicsContext gc, double offsetX, double offsetY, double size) {
        for (int i = 0; i < colors.length; i++) {
            double x = offsetX;
            double y = offsetY + i * size;
            hiddenGemsApplication.drawCell(gc, colors[i], x, y, size);
        }
    }

    boolean moveDown(char[][] board) {
        if (canMoveDown(board)) {
            row++;
            return true;
        }
        placeOnBoard(board);
        return false;
    }

    boolean canMoveDown(char[][] board) {
        for (int i = 0; i < colors.length; i++) {
            if (row + i + 1 >= HiddenGemsApplication.NUM_ROWS || board[row + i + 1][col] != ' ') {
                return false;
            }
        }
        return true;
    }

    void moveLeft(char[][] board) {
        if (col > 0 && canMoveLeft(board)) {
            col--;
        }
    }

    void moveRight(char[][] board) {
        if (col < HiddenGemsApplication.NUM_COLS - 1 && canMoveRight(board)) {
            col++;
        }
    }

    boolean canMoveLeft(char[][] board) {
        for (int i = 0; i < colors.length; i++) {
            if (col - 1 < 0 || board[row + i][col - 1] != ' ') {
                return false;
            }
        }
        return true;
    }

    boolean canMoveRight(char[][] board) {
        for (int i = 0; i < colors.length; i++) {
            if (col + 1 >= HiddenGemsApplication.NUM_COLS || board[row + i][col + 1] != ' ') {
                return false;
            }
        }
        return true;
    }

    void shiftUp() {
        if (colors.length > 1) {
            char temp = colors[0];
            for (int i = 0; i < colors.length - 1; i++) {
                colors[i] = colors[i + 1];
            }
            colors[colors.length - 1] = temp;
        }
    }

    void placeOnBoard(char[][] board) {
        for (int i = 0; i < colors.length; i++) {
            board[row + i][col] = colors[i];
        }
    }
}

package com.minesweeper.model;

/**
 * An immutable (row, column) coordinate on the board.
 *
 * @param row zero-based row index
 * @param col zero-based column index
 */
public record Position(int row, int col) {
}

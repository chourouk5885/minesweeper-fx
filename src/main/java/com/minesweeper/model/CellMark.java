package com.minesweeper.model;

/**
 * Marker a player can place on a hidden cell with a right-click.
 */
public enum CellMark {
    /** No marker. */
    NONE,
    /** The player believes this cell is a mine. Flagged cells cannot be revealed. */
    FLAG,
    /** The player is unsure. Question-marked cells can still be revealed. */
    QUESTION
}

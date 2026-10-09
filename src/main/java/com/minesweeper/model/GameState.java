package com.minesweeper.model;

/**
 * Lifecycle of a single game.
 */
public enum GameState {
    /** Board created, no cell revealed yet. Mines are not placed until the first reveal. */
    READY,
    /** At least one cell revealed and the game is still running. */
    PLAYING,
    /** Every safe cell has been revealed. */
    WON,
    /** A mine was revealed. */
    LOST;

    /**
     * @return {@code true} when the game has finished and the board should ignore input
     */
    public boolean isOver() {
        return this == WON || this == LOST;
    }
}

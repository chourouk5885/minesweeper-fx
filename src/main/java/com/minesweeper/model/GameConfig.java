package com.minesweeper.model;

import java.util.Objects;

/**
 * The size and mine count of a board, together with the difficulty it belongs to.
 *
 * @param difficulty preset this configuration came from, or {@link Difficulty#CUSTOM}
 * @param rows       board height
 * @param cols       board width
 * @param mines      number of mines
 */
public record GameConfig(Difficulty difficulty, int rows, int cols, int mines) {

    /**
     * Validates the configuration.
     *
     * @throws IllegalArgumentException if any dimension or the mine count is out of range
     */
    public GameConfig {
        Objects.requireNonNull(difficulty, "difficulty");
        validate(rows, cols, mines);
    }

    /**
     * Builds the configuration for a preset difficulty.
     *
     * @param difficulty a preset (not {@link Difficulty#CUSTOM})
     * @return the matching configuration
     */
    public static GameConfig of(Difficulty difficulty) {
        if (!difficulty.isPreset()) {
            throw new IllegalArgumentException("Use GameConfig.custom(...) for custom boards");
        }
        return new GameConfig(difficulty, difficulty.getRows(), difficulty.getCols(), difficulty.getMines());
    }

    /**
     * Builds a validated custom configuration.
     *
     * @param rows  board height
     * @param cols  board width
     * @param mines number of mines
     * @return the configuration
     */
    public static GameConfig custom(int rows, int cols, int mines) {
        return new GameConfig(Difficulty.CUSTOM, rows, cols, mines);
    }

    /**
     * Checks the limits documented on {@link Difficulty}.
     *
     * @param rows  board height
     * @param cols  board width
     * @param mines number of mines
     * @throws IllegalArgumentException if anything is out of range
     */
    public static void validate(int rows, int cols, int mines) {
        if (rows < Difficulty.MIN_ROWS || rows > Difficulty.MAX_ROWS) {
            throw new IllegalArgumentException("Rows must be between " + Difficulty.MIN_ROWS
                    + " and " + Difficulty.MAX_ROWS + ": " + rows);
        }
        if (cols < Difficulty.MIN_COLS || cols > Difficulty.MAX_COLS) {
            throw new IllegalArgumentException("Columns must be between " + Difficulty.MIN_COLS
                    + " and " + Difficulty.MAX_COLS + ": " + cols);
        }
        int maxMines = Difficulty.maxMines(rows, cols);
        if (mines < Difficulty.MIN_MINES || mines > maxMines) {
            throw new IllegalArgumentException("Mines must be between " + Difficulty.MIN_MINES
                    + " and " + maxMines + ": " + mines);
        }
    }
}

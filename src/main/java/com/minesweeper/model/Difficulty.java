package com.minesweeper.model;

/**
 * Difficulty presets plus the limits that apply to custom boards.
 */
public enum Difficulty {
    BEGINNER("Beginner", 9, 9, 10),
    INTERMEDIATE("Intermediate", 16, 16, 40),
    EXPERT("Expert", 16, 30, 99),
    /** Player-defined size; the numbers here are the defaults offered in the custom dialog. */
    CUSTOM("Custom", 12, 20, 40);

    /** Smallest allowed number of rows on a custom board. */
    public static final int MIN_ROWS = 5;
    /** Largest allowed number of rows on a custom board. */
    public static final int MAX_ROWS = 30;
    /** Smallest allowed number of columns on a custom board. */
    public static final int MIN_COLS = 5;
    /** Largest allowed number of columns on a custom board. */
    public static final int MAX_COLS = 50;
    /** Every board needs at least this many mines. */
    public static final int MIN_MINES = 1;
    /** Size of the guaranteed mine-free block around the first click (3 x 3). */
    public static final int SAFE_ZONE_CELLS = 9;

    private final String displayName;
    private final int rows;
    private final int cols;
    private final int mines;

    Difficulty(String displayName, int rows, int cols, int mines) {
        this.displayName = displayName;
        this.rows = rows;
        this.cols = cols;
        this.mines = mines;
    }

    /** @return human readable name, e.g. "Intermediate" */
    public String getDisplayName() {
        return displayName;
    }

    /** @return board height for this preset */
    public int getRows() {
        return rows;
    }

    /** @return board width for this preset */
    public int getCols() {
        return cols;
    }

    /** @return mine count for this preset */
    public int getMines() {
        return mines;
    }

    /** @return {@code true} for the three fixed presets (best times are only kept for these) */
    public boolean isPreset() {
        return this != CUSTOM;
    }

    /**
     * Highest mine count allowed on a board of the given size, leaving room for the safe opening.
     *
     * @param rows board height
     * @param cols board width
     * @return the maximum number of mines
     */
    public static int maxMines(int rows, int cols) {
        return rows * cols - SAFE_ZONE_CELLS;
    }
}

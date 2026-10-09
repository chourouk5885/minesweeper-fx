package com.minesweeper.util;

import com.minesweeper.model.Difficulty;

/**
 * User preferences that survive between launches.
 */
public final class Settings {

    /** Default cell size in pixels. */
    public static final int DEFAULT_CELL_SIZE = 32;
    /** Default theme name (matches {@code Theme.LIGHT}). */
    public static final String DEFAULT_THEME = "LIGHT";

    private Difficulty difficulty = Difficulty.BEGINNER;
    private int customRows = Difficulty.CUSTOM.getRows();
    private int customCols = Difficulty.CUSTOM.getCols();
    private int customMines = Difficulty.CUSTOM.getMines();
    private String themeName = DEFAULT_THEME;
    private boolean questionMarksEnabled = true;
    private boolean soundEnabled = false;
    private int cellSize = DEFAULT_CELL_SIZE;
    private String lastPlayerName = "";

    public Difficulty getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(Difficulty difficulty) {
        this.difficulty = difficulty;
    }

    public int getCustomRows() {
        return customRows;
    }

    public void setCustomRows(int customRows) {
        this.customRows = customRows;
    }

    public int getCustomCols() {
        return customCols;
    }

    public void setCustomCols(int customCols) {
        this.customCols = customCols;
    }

    public int getCustomMines() {
        return customMines;
    }

    public void setCustomMines(int customMines) {
        this.customMines = customMines;
    }

    public String getThemeName() {
        return themeName;
    }

    public void setThemeName(String themeName) {
        this.themeName = themeName;
    }

    public boolean isQuestionMarksEnabled() {
        return questionMarksEnabled;
    }

    public void setQuestionMarksEnabled(boolean questionMarksEnabled) {
        this.questionMarksEnabled = questionMarksEnabled;
    }

    public boolean isSoundEnabled() {
        return soundEnabled;
    }

    public void setSoundEnabled(boolean soundEnabled) {
        this.soundEnabled = soundEnabled;
    }

    public int getCellSize() {
        return cellSize;
    }

    public void setCellSize(int cellSize) {
        this.cellSize = cellSize;
    }

    public String getLastPlayerName() {
        return lastPlayerName;
    }

    public void setLastPlayerName(String lastPlayerName) {
        this.lastPlayerName = lastPlayerName;
    }
}

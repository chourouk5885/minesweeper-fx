package com.minesweeper.util;

import com.minesweeper.model.Difficulty;
import com.minesweeper.model.GameStats;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Loads and saves {@link Settings} and {@link GameStats} as {@code .properties} files in
 * {@code ~/.minesweeper-fx}. Failures are logged and never crash the game.
 */
public final class SettingsManager {

    private static final String APP_DIRECTORY = ".minesweeper-fx";
    private static final String SETTINGS_FILE = "settings.properties";
    private static final String STATS_FILE = "stats.properties";
    private static final String FIELD_SEPARATOR = "|";
    private static final String FIELD_SEPARATOR_REGEX = "\\|";
    private static final int BEST_TIME_FIELDS = 3;
    /** Smallest and largest cell size accepted from the settings file. */
    private static final int MIN_CELL_SIZE = 16;
    private static final int MAX_CELL_SIZE = 64;

    private static final String KEY_DIFFICULTY = "difficulty";
    private static final String KEY_CUSTOM_ROWS = "custom.rows";
    private static final String KEY_CUSTOM_COLS = "custom.cols";
    private static final String KEY_CUSTOM_MINES = "custom.mines";
    private static final String KEY_THEME = "theme";
    private static final String KEY_QUESTION_MARKS = "questionMarks";
    private static final String KEY_SOUND = "sound";
    private static final String KEY_CELL_SIZE = "cellSize";
    private static final String KEY_PLAYER = "lastPlayerName";

    private final Path directory;

    /** Uses {@code ~/.minesweeper-fx}. */
    public SettingsManager() {
        this(Path.of(System.getProperty("user.home"), APP_DIRECTORY));
    }

    /**
     * @param directory folder that holds the settings and statistics files
     */
    public SettingsManager(Path directory) {
        this.directory = directory;
    }

    /** @return saved settings, or defaults for anything missing or unreadable */
    public Settings loadSettings() {
        Properties p = read(directory.resolve(SETTINGS_FILE));
        Settings s = new Settings();
        s.setDifficulty(parseDifficulty(p.getProperty(KEY_DIFFICULTY), s.getDifficulty()));
        s.setCustomRows(parseInt(p.getProperty(KEY_CUSTOM_ROWS), s.getCustomRows()));
        s.setCustomCols(parseInt(p.getProperty(KEY_CUSTOM_COLS), s.getCustomCols()));
        s.setCustomMines(parseInt(p.getProperty(KEY_CUSTOM_MINES), s.getCustomMines()));
        s.setThemeName(p.getProperty(KEY_THEME, s.getThemeName()));
        s.setQuestionMarksEnabled(parseBoolean(p.getProperty(KEY_QUESTION_MARKS), s.isQuestionMarksEnabled()));
        s.setSoundEnabled(parseBoolean(p.getProperty(KEY_SOUND), s.isSoundEnabled()));
        int cellSize = parseInt(p.getProperty(KEY_CELL_SIZE), s.getCellSize());
        s.setCellSize(Math.max(MIN_CELL_SIZE, Math.min(MAX_CELL_SIZE, cellSize)));
        s.setLastPlayerName(p.getProperty(KEY_PLAYER, s.getLastPlayerName()));
        return s;
    }

    /**
     * @param s settings to write
     */
    public void saveSettings(Settings s) {
        Properties p = new Properties();
        p.setProperty(KEY_DIFFICULTY, s.getDifficulty().name());
        p.setProperty(KEY_CUSTOM_ROWS, Integer.toString(s.getCustomRows()));
        p.setProperty(KEY_CUSTOM_COLS, Integer.toString(s.getCustomCols()));
        p.setProperty(KEY_CUSTOM_MINES, Integer.toString(s.getCustomMines()));
        p.setProperty(KEY_THEME, s.getThemeName());
        p.setProperty(KEY_QUESTION_MARKS, Boolean.toString(s.isQuestionMarksEnabled()));
        p.setProperty(KEY_SOUND, Boolean.toString(s.isSoundEnabled()));
        p.setProperty(KEY_CELL_SIZE, Integer.toString(s.getCellSize()));
        p.setProperty(KEY_PLAYER, s.getLastPlayerName());
        write(directory.resolve(SETTINGS_FILE), p, "Minesweeper settings");
    }

    /** @return saved statistics, or empty statistics if none exist */
    public GameStats loadStats() {
        Properties p = read(directory.resolve(STATS_FILE));
        GameStats stats = new GameStats();
        for (Difficulty d : Difficulty.values()) {
            String prefix = d.name() + ".";
            List<GameStats.BestTime> times = new ArrayList<>();
            for (int i = 0; i < GameStats.MAX_BEST_TIMES; i++) {
                GameStats.BestTime t = parseBestTime(p.getProperty(prefix + "best." + i));
                if (t != null) {
                    times.add(t);
                }
            }
            stats.restore(d,
                    parseInt(p.getProperty(prefix + "played"), 0),
                    parseInt(p.getProperty(prefix + "won"), 0),
                    parseInt(p.getProperty(prefix + "currentStreak"), 0),
                    parseInt(p.getProperty(prefix + "bestStreak"), 0),
                    times);
        }
        return stats;
    }

    /**
     * @param stats statistics to write
     */
    public void saveStats(GameStats stats) {
        Properties p = new Properties();
        for (Difficulty d : Difficulty.values()) {
            String prefix = d.name() + ".";
            GameStats.DifficultyStats s = stats.get(d);
            p.setProperty(prefix + "played", Integer.toString(s.getPlayed()));
            p.setProperty(prefix + "won", Integer.toString(s.getWon()));
            p.setProperty(prefix + "currentStreak", Integer.toString(s.getCurrentStreak()));
            p.setProperty(prefix + "bestStreak", Integer.toString(s.getBestStreak()));
            List<GameStats.BestTime> times = s.getBestTimes();
            for (int i = 0; i < times.size(); i++) {
                GameStats.BestTime t = times.get(i);
                // Name goes last so a '|' inside it cannot break parsing.
                p.setProperty(prefix + "best." + i,
                        t.millis() + FIELD_SEPARATOR + t.date() + FIELD_SEPARATOR + t.name());
            }
        }
        write(directory.resolve(STATS_FILE), p, "Minesweeper statistics");
    }

    // ------------------------------------------------------------------ helpers

    private Properties read(Path file) {
        Properties p = new Properties();
        if (!Files.isRegularFile(file)) {
            return p;
        }
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            p.load(reader);
        } catch (IOException | IllegalArgumentException e) {
            System.err.println("Could not read " + file + ": " + e.getMessage());
        }
        return p;
    }

    private void write(Path file, Properties p, String comment) {
        try {
            Files.createDirectories(directory);
            try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                p.store(writer, comment);
            }
        } catch (IOException e) {
            System.err.println("Could not save " + file + ": " + e.getMessage());
        }
    }

    private static GameStats.BestTime parseBestTime(String value) {
        if (value == null) {
            return null;
        }
        String[] parts = value.split(FIELD_SEPARATOR_REGEX, BEST_TIME_FIELDS);
        if (parts.length != BEST_TIME_FIELDS) {
            return null;
        }
        try {
            return new GameStats.BestTime(parts[2], Long.parseLong(parts[0]), LocalDate.parse(parts[1]));
        } catch (NumberFormatException | DateTimeParseException e) {
            return null;
        }
    }

    private static Difficulty parseDifficulty(String value, Difficulty fallback) {
        if (value == null) {
            return fallback;
        }
        try {
            return Difficulty.valueOf(value);
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }

    private static int parseInt(String value, int fallback) {
        if (value == null) {
            return fallback;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static boolean parseBoolean(String value, boolean fallback) {
        return value == null ? fallback : Boolean.parseBoolean(value.trim());
    }
}

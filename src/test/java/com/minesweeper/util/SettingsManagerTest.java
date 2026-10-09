package com.minesweeper.util;

import com.minesweeper.model.Difficulty;
import com.minesweeper.model.GameStats;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SettingsManagerTest {

    @Test
    @DisplayName("Settings round-trip through disk")
    void settingsRoundTrip() throws IOException {
        Path dir = Files.createTempDirectory("ms-settings");
        SettingsManager manager = new SettingsManager(dir);
        Settings s = new Settings();
        s.setDifficulty(Difficulty.EXPERT);
        s.setThemeName("DARK");
        s.setSoundEnabled(true);
        s.setQuestionMarksEnabled(false);
        s.setCellSize(40);
        s.setLastPlayerName("Ada");
        manager.saveSettings(s);

        Settings loaded = manager.loadSettings();
        assertEquals(Difficulty.EXPERT, loaded.getDifficulty());
        assertEquals("DARK", loaded.getThemeName());
        assertTrue(loaded.isSoundEnabled());
        assertFalse(loaded.isQuestionMarksEnabled());
        assertEquals(40, loaded.getCellSize());
        assertEquals("Ada", loaded.getLastPlayerName());
    }

    @Test
    @DisplayName("Missing files give defaults")
    void defaultsWhenMissing() throws IOException {
        SettingsManager manager = new SettingsManager(Files.createTempDirectory("ms-empty"));
        assertEquals(Difficulty.BEGINNER, manager.loadSettings().getDifficulty());
        assertEquals(0, manager.loadStats().get(Difficulty.BEGINNER).getPlayed());
    }

    @Test
    @DisplayName("Statistics and best times (including names with '|') round-trip")
    void statsRoundTrip() throws IOException {
        SettingsManager manager = new SettingsManager(Files.createTempDirectory("ms-stats"));
        GameStats stats = new GameStats();
        stats.recordWin(Difficulty.BEGINNER);
        stats.recordLoss(Difficulty.BEGINNER);
        stats.addBestTime(Difficulty.BEGINNER,
                new GameStats.BestTime("A|B", 12_345, LocalDate.of(2026, 1, 2)));
        manager.saveStats(stats);

        GameStats loaded = manager.loadStats();
        GameStats.DifficultyStats b = loaded.get(Difficulty.BEGINNER);
        assertEquals(2, b.getPlayed());
        assertEquals(1, b.getWon());
        assertEquals(1, b.getBestStreak());
        assertEquals("A|B", b.getBestTimes().get(0).name());
        assertEquals(12_345, b.getBestTimes().get(0).millis());
    }
}

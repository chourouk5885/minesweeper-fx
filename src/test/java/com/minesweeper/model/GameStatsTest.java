package com.minesweeper.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GameStatsTest {

    private static final LocalDate DAY = LocalDate.of(2026, 10, 9);

    @Test
    @DisplayName("Wins and losses update played, won and streaks")
    void streaks() {
        GameStats stats = new GameStats();
        stats.recordWin(Difficulty.BEGINNER);
        stats.recordWin(Difficulty.BEGINNER);
        stats.recordLoss(Difficulty.BEGINNER);
        stats.recordWin(Difficulty.BEGINNER);

        GameStats.DifficultyStats s = stats.get(Difficulty.BEGINNER);
        assertEquals(4, s.getPlayed());
        assertEquals(3, s.getWon());
        assertEquals(1, s.getCurrentStreak());
        assertEquals(2, s.getBestStreak());
        assertEquals(0.75, s.getWinRate(), 1e-9);
        assertEquals(0, stats.get(Difficulty.EXPERT).getPlayed());
    }

    @Test
    @DisplayName("Best times stay sorted and are capped at five")
    void bestTimesSortedAndCapped() {
        GameStats stats = new GameStats();
        long[] times = {9000, 5000, 7000, 3000, 8000, 6000};
        for (long t : times) {
            stats.addBestTime(Difficulty.EXPERT, new GameStats.BestTime("p" + t, t, DAY));
        }
        List<GameStats.BestTime> table = stats.get(Difficulty.EXPERT).getBestTimes();
        assertEquals(GameStats.MAX_BEST_TIMES, table.size());
        assertEquals(3000, table.get(0).millis());
        assertEquals(8000, table.get(4).millis());
        assertEquals(GameStats.NOT_RANKED, stats.bestTimeRank(Difficulty.EXPERT, 8500));
        assertEquals(1, stats.bestTimeRank(Difficulty.EXPERT, 1000));
    }

    @Test
    @DisplayName("Custom games never enter the best-time table")
    void customHasNoBestTimes() {
        GameStats stats = new GameStats();
        assertEquals(GameStats.NOT_RANKED,
                stats.addBestTime(Difficulty.CUSTOM, new GameStats.BestTime("me", 1000, DAY)));
    }

    @Test
    @DisplayName("Reset clears everything")
    void reset() {
        GameStats stats = new GameStats();
        stats.recordWin(Difficulty.INTERMEDIATE);
        stats.addBestTime(Difficulty.INTERMEDIATE, new GameStats.BestTime("me", 1000, DAY));
        stats.reset();
        assertEquals(0, stats.get(Difficulty.INTERMEDIATE).getPlayed());
        assertEquals(0, stats.get(Difficulty.INTERMEDIATE).getBestTimes().size());
    }
}

package com.minesweeper.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Win/loss statistics and best-time tables, kept per difficulty.
 * Best times are only recorded for the three presets.
 */
public final class GameStats {

    /** Number of entries kept in each best-time table. */
    public static final int MAX_BEST_TIMES = 5;
    /** Returned by {@link #bestTimeRank} when a time does not make the table. */
    public static final int NOT_RANKED = -1;

    /**
     * One entry in a best-time table.
     *
     * @param name   player name
     * @param millis completion time in milliseconds
     * @param date   date the record was set
     */
    public record BestTime(String name, long millis, LocalDate date) {
        /** Validates the entry. */
        public BestTime {
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(date, "date");
            if (millis < 0) {
                throw new IllegalArgumentException("millis must not be negative");
            }
        }
    }

    /** Counters for one difficulty. */
    public static final class DifficultyStats {
        private int played;
        private int won;
        private int currentStreak;
        private int bestStreak;
        private final List<BestTime> bestTimes = new ArrayList<>();

        /** @return games finished (won or lost) */
        public int getPlayed() {
            return played;
        }

        /** @return games won */
        public int getWon() {
            return won;
        }

        /** @return consecutive wins up to now */
        public int getCurrentStreak() {
            return currentStreak;
        }

        /** @return longest run of consecutive wins */
        public int getBestStreak() {
            return bestStreak;
        }

        /** @return win ratio between 0 and 1 (0 when nothing was played) */
        public double getWinRate() {
            return played == 0 ? 0.0 : (double) won / played;
        }

        /** @return the best-time table, fastest first */
        public List<BestTime> getBestTimes() {
            return Collections.unmodifiableList(bestTimes);
        }

        private void clear() {
            played = 0;
            won = 0;
            currentStreak = 0;
            bestStreak = 0;
            bestTimes.clear();
        }
    }

    private final Map<Difficulty, DifficultyStats> byDifficulty = new EnumMap<>(Difficulty.class);

    /** Creates empty statistics. */
    public GameStats() {
        for (Difficulty d : Difficulty.values()) {
            byDifficulty.put(d, new DifficultyStats());
        }
    }

    /**
     * @param difficulty a difficulty
     * @return its counters
     */
    public DifficultyStats get(Difficulty difficulty) {
        return byDifficulty.get(difficulty);
    }

    /**
     * Records a won game.
     *
     * @param difficulty difficulty of the game
     */
    public void recordWin(Difficulty difficulty) {
        DifficultyStats s = get(difficulty);
        s.played++;
        s.won++;
        s.currentStreak++;
        s.bestStreak = Math.max(s.bestStreak, s.currentStreak);
    }

    /**
     * Records a lost (or abandoned) game.
     *
     * @param difficulty difficulty of the game
     */
    public void recordLoss(Difficulty difficulty) {
        DifficultyStats s = get(difficulty);
        s.played++;
        s.currentStreak = 0;
    }

    /**
     * Where a time would land in the best-time table.
     *
     * @param difficulty difficulty of the game
     * @param millis     completion time
     * @return 1-based rank, or {@link #NOT_RANKED} if it would not make the table
     */
    public int bestTimeRank(Difficulty difficulty, long millis) {
        if (!difficulty.isPreset()) {
            return NOT_RANKED;
        }
        List<BestTime> times = get(difficulty).bestTimes;
        int rank = 1;
        for (BestTime t : times) {
            if (t.millis() <= millis) {
                rank++;
            }
        }
        return rank <= MAX_BEST_TIMES ? rank : NOT_RANKED;
    }

    /**
     * Inserts a time into the table if it qualifies.
     *
     * @param difficulty difficulty of the game
     * @param time       the entry to add
     * @return the 1-based rank it was stored at, or {@link #NOT_RANKED}
     */
    public int addBestTime(Difficulty difficulty, BestTime time) {
        int rank = bestTimeRank(difficulty, time.millis());
        if (rank == NOT_RANKED) {
            return NOT_RANKED;
        }
        List<BestTime> times = get(difficulty).bestTimes;
        times.add(rank - 1, time);
        while (times.size() > MAX_BEST_TIMES) {
            times.remove(times.size() - 1);
        }
        return rank;
    }

    /**
     * Restores saved counters (used when loading from disk). Best times are re-sorted and trimmed.
     *
     * @param difficulty    difficulty to restore
     * @param played        games played
     * @param won           games won
     * @param currentStreak current streak
     * @param bestStreak    best streak
     * @param bestTimes     saved best times in any order
     */
    public void restore(Difficulty difficulty, int played, int won, int currentStreak, int bestStreak,
                        List<BestTime> bestTimes) {
        DifficultyStats s = get(difficulty);
        s.clear();
        s.played = Math.max(0, played);
        s.won = Math.max(0, Math.min(won, s.played));
        s.currentStreak = Math.max(0, currentStreak);
        s.bestStreak = Math.max(s.currentStreak, bestStreak);
        if (difficulty.isPreset()) {
            List<BestTime> sorted = new ArrayList<>(bestTimes);
            sorted.sort((a, b) -> Long.compare(a.millis(), b.millis()));
            s.bestTimes.addAll(sorted.subList(0, Math.min(MAX_BEST_TIMES, sorted.size())));
        }
    }

    /** Clears every counter and best time. */
    public void reset() {
        for (DifficultyStats s : byDifficulty.values()) {
            s.clear();
        }
    }
}

package com.minesweeper.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BoardTest {

    private static final int SEEDS_TO_TRY = 300;

    // ------------------------------------------------------------ mine placement

    @Test
    @DisplayName("First click is never a mine and its 3x3 block is always mine-free")
    void firstClickIsAlwaysSafe() {
        for (int seed = 0; seed < SEEDS_TO_TRY; seed++) {
            Random random = new Random(seed);
            Board board = new Board(16, 30, 99, random);
            int row = random.nextInt(16);
            int col = random.nextInt(30);

            RevealResult result = board.reveal(row, col);

            assertFalse(result.hitMine(), "seed " + seed);
            for (int r = row - 1; r <= row + 1; r++) {
                for (int c = col - 1; c <= col + 1; c++) {
                    if (board.isInBounds(r, c)) {
                        assertFalse(board.getCell(r, c).isMine(), "mine next to first click, seed " + seed);
                    }
                }
            }
            assertEquals(0, board.getCell(row, col).getAdjacentMines(), "first click must open an area");
        }
    }

    @Test
    @DisplayName("Mines are only placed after the first click, and exactly the requested number")
    void placesExactMineCountLazily() {
        Board board = new Board(16, 16, 40, new Random(42));
        assertFalse(board.areMinesPlaced());
        assertEquals(GameState.READY, board.getState());

        board.reveal(8, 8);

        assertTrue(board.areMinesPlaced());
        int mines = 0;
        for (int r = 0; r < board.getRows(); r++) {
            for (int c = 0; c < board.getCols(); c++) {
                if (board.getCell(r, c).isMine()) {
                    mines++;
                }
            }
        }
        assertEquals(40, mines);
    }

    @Test
    @DisplayName("Maximum density custom board still leaves the first click safe")
    void maximumDensityStillSafe() {
        int rows = 5;
        int cols = 5;
        Board board = new Board(rows, cols, Difficulty.maxMines(rows, cols), new Random(7));
        RevealResult result = board.reveal(2, 2);
        assertFalse(result.hitMine());
        assertEquals(GameState.WON, board.getState(), "only the 3x3 block is safe, so it is won at once");
    }

    // ------------------------------------------------------------ adjacency

    @Test
    @DisplayName("Adjacent mine counts match the layout")
    void adjacentCounts() {
        Board board = Board.fromLayout(
                "*..",
                ".*.",
                "...");
        assertEquals(2, board.getCell(0, 1).getAdjacentMines());
        assertEquals(1, board.getCell(0, 2).getAdjacentMines());
        assertEquals(2, board.getCell(1, 0).getAdjacentMines());
        assertEquals(1, board.getCell(2, 2).getAdjacentMines());
        assertEquals(1, board.getCell(1, 1).getAdjacentMines(), "mines count their mine neighbours too");
    }

    @Test
    @DisplayName("A cell surrounded by eight mines reports 8")
    void surroundedCellReportsEight() {
        Board board = Board.fromLayout(
                "***",
                "*.*",
                "***");
        assertEquals(8, board.getCell(1, 1).getAdjacentMines());
    }

    // ------------------------------------------------------------ flood fill

    @Test
    @DisplayName("Revealing a zero cell flood-fills only its connected region and its border")
    void floodFillOpensConnectedRegion() {
        Board board = Board.fromLayout(
                "....*....",
                "....*....",
                "....*....",
                "....*....");
        RevealResult result = board.reveal(0, 0);

        assertFalse(result.hitMine());
        // Left side: columns 0..3 all revealed (column 3 is the numbered border).
        for (int r = 0; r < 4; r++) {
            for (int c = 0; c <= 3; c++) {
                assertTrue(board.getCell(r, c).isRevealed(), "(" + r + "," + c + ") should be open");
            }
            for (int c = 5; c < 9; c++) {
                assertFalse(board.getCell(r, c).isRevealed(), "(" + r + "," + c + ") is behind the wall");
            }
        }
        assertEquals(16, result.revealed().size());
        assertEquals(result.revealed().get(0), new Position(0, 0), "BFS order starts at the click");
    }

    @Test
    @DisplayName("Flood fill does not uncover flagged cells")
    void floodFillStopsAtFlags() {
        Board board = Board.fromLayout(
                ".....",
                ".....",
                "....*");
        board.toggleMark(0, 4, true);
        board.reveal(0, 0);
        assertFalse(board.getCell(0, 4).isRevealed());
        assertTrue(board.getCell(0, 4).isFlagged());
    }

    @Test
    @DisplayName("Flood fill on a huge empty board does not overflow the stack")
    void floodFillLargeBoard() {
        Board board = new Board(Difficulty.MAX_ROWS, Difficulty.MAX_COLS, 1, new Random(3));
        RevealResult result = board.reveal(15, 25);
        assertFalse(result.hitMine());
        assertTrue(result.revealed().size() > 1000);
    }

    // ------------------------------------------------------------ chording

    @Test
    @DisplayName("Chording a number with matching flags reveals the other neighbours")
    void chordRevealsNeighbours() {
        Board board = Board.fromLayout(
                "*..",
                "...",
                "...",
                "..*");
        board.reveal(1, 1);
        assertEquals(1, board.getCell(1, 1).getAdjacentMines());
        board.toggleMark(0, 0, true);

        RevealResult result = board.chord(1, 1);

        assertFalse(result.hitMine());
        assertTrue(board.getCell(0, 1).isRevealed());
        assertTrue(board.getCell(2, 2).isRevealed());
        assertTrue(board.getCell(1, 0).isRevealed());
        assertFalse(board.getCell(0, 0).isRevealed(), "flagged mine stays covered");
    }

    @Test
    @DisplayName("Chording does nothing when the flag count does not match")
    void chordRequiresMatchingFlags() {
        Board board = Board.fromLayout(
                "*..",
                "...",
                "..*");
        board.reveal(1, 1);
        RevealResult result = board.chord(1, 1);
        assertFalse(result.changed());
        assertFalse(board.getCell(0, 1).isRevealed());
    }

    @Test
    @DisplayName("Chording with a wrongly placed flag loses the game")
    void chordWithWrongFlagLoses() {
        Board board = Board.fromLayout(
                "*..",
                "...",
                "...");
        board.reveal(1, 1);
        board.toggleMark(2, 2, true); // wrong flag

        RevealResult result = board.chord(1, 1);

        assertTrue(result.hitMine());
        assertEquals(GameState.LOST, board.getState());
        assertTrue(board.getCell(0, 0).isExploded());
        assertTrue(board.isWrongFlag(2, 2));
    }

    @Test
    @DisplayName("Chording an unrevealed or zero cell is ignored")
    void chordIgnoresInvalidTargets() {
        Board board = Board.fromLayout(
                "*....",
                ".....",
                ".....");
        assertFalse(board.chord(2, 4).changed(), "hidden cell");
        board.reveal(2, 4);
        assertFalse(board.chord(2, 4).changed(), "zero cell");
    }

    @Test
    @DisplayName("chordTargets lists the hidden, unflagged cells of the 3x3 block")
    void chordTargets() {
        Board board = Board.fromLayout(
                "*..",
                "...",
                "..*");
        board.reveal(1, 1);
        board.toggleMark(0, 0, true);
        List<Position> targets = board.chordTargets(1, 1);
        assertEquals(7, targets.size());
        assertFalse(targets.contains(new Position(0, 0)));
        assertFalse(targets.contains(new Position(1, 1)));
    }

    // ------------------------------------------------------------ win / loss

    @Test
    @DisplayName("Revealing every safe cell wins and auto-flags the mines")
    void winDetection() {
        Board board = Board.fromLayout(
                "*..",
                "...",
                "...");
        board.reveal(2, 2);
        assertEquals(GameState.WON, board.getState());
        assertTrue(board.getCell(0, 0).isFlagged());
        assertEquals(0, board.getRemainingMineCounter());
    }

    @Test
    @DisplayName("Win is not declared while a safe cell is still hidden")
    void noEarlyWin() {
        Board board = Board.fromLayout(
                "*.*",
                "...",
                "...");
        board.reveal(2, 2);
        assertEquals(GameState.PLAYING, board.getState(), "(0,1) is still covered");
        board.reveal(0, 1);
        assertEquals(GameState.WON, board.getState());
    }

    @Test
    @DisplayName("Revealing a mine loses, reveals every unflagged mine and records the exploded one")
    void lossDetection() {
        Board board = Board.fromLayout(
                "*..",
                "...",
                "..*");
        board.toggleMark(2, 2, true);
        board.toggleMark(1, 2, true); // wrong flag
        RevealResult result = board.reveal(0, 0);

        assertTrue(result.hitMine());
        assertEquals(GameState.LOST, board.getState());
        assertEquals(new Position(0, 0), board.getExplodedAt().orElseThrow());
        assertTrue(board.getCell(0, 0).isRevealed());
        assertTrue(board.getCell(2, 2).isFlagged(), "correct flag stays a flag");
        assertFalse(board.isWrongFlag(2, 2));
        assertTrue(board.isWrongFlag(1, 2));
    }

    @Test
    @DisplayName("All input is ignored once the game is over")
    void inputIgnoredAfterGameOver() {
        Board board = Board.fromLayout(
                "*..",
                "...",
                "...");
        board.reveal(0, 0);
        assertEquals(GameState.LOST, board.getState());
        assertFalse(board.reveal(2, 2).changed());
        assertFalse(board.toggleMark(2, 2, true));
        assertFalse(board.getCell(2, 2).isRevealed());
    }

    // ------------------------------------------------------------ marks

    @Test
    @DisplayName("Right-click cycles flag, question mark, none (question marks optional)")
    void markCycle() {
        Board board = Board.fromLayout(
                "*..",
                "...",
                "...");
        board.toggleMark(0, 0, true);
        assertEquals(CellMark.FLAG, board.getCell(0, 0).getMark());
        board.toggleMark(0, 0, true);
        assertEquals(CellMark.QUESTION, board.getCell(0, 0).getMark());
        board.toggleMark(0, 0, true);
        assertEquals(CellMark.NONE, board.getCell(0, 0).getMark());

        board.toggleMark(0, 0, false);
        board.toggleMark(0, 0, false);
        assertEquals(CellMark.NONE, board.getCell(0, 0).getMark(), "flag goes straight back to none");
    }

    @Test
    @DisplayName("Mine counter is mines minus flags and can go negative")
    void mineCounterCanGoNegative() {
        Board board = Board.fromLayout(
                "*..",
                "...",
                "...");
        board.toggleMark(0, 1, true);
        board.toggleMark(0, 2, true);
        assertEquals(-1, board.getRemainingMineCounter());
        board.toggleMark(0, 2, true); // flag -> question
        assertEquals(0, board.getRemainingMineCounter());
    }

    @Test
    @DisplayName("Flagged cells cannot be revealed; question-marked cells can")
    void flaggedCellsAreProtected() {
        Board board = Board.fromLayout(
                "*....",
                ".....",
                ".....");
        board.toggleMark(0, 0, true);
        assertFalse(board.reveal(0, 0).changed());
        board.toggleMark(2, 4, true);
        board.toggleMark(2, 4, true); // question mark
        assertTrue(board.reveal(2, 4).changed());
        assertEquals(CellMark.NONE, board.getCell(2, 4).getMark());
    }

    // ------------------------------------------------------------ configuration

    @Test
    @DisplayName("Invalid custom configurations are rejected")
    void rejectsInvalidConfigs() {
        assertThrows(IllegalArgumentException.class, () -> GameConfig.custom(4, 10, 5));
        assertThrows(IllegalArgumentException.class, () -> GameConfig.custom(10, 51, 5));
        assertThrows(IllegalArgumentException.class, () -> GameConfig.custom(10, 10, 0));
        assertThrows(IllegalArgumentException.class, () -> GameConfig.custom(10, 10, 92));
        assertEquals(91, GameConfig.custom(10, 10, 91).mines());
    }

    @Test
    @DisplayName("Presets have the classic dimensions")
    void presets() {
        assertEquals(new GameConfig(Difficulty.EXPERT, 16, 30, 99), GameConfig.of(Difficulty.EXPERT));
        assertThrows(IllegalArgumentException.class, () -> GameConfig.of(Difficulty.CUSTOM));
        assertNotEquals(GameConfig.of(Difficulty.BEGINNER), GameConfig.of(Difficulty.INTERMEDIATE));
    }
}

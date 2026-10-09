package com.minesweeper.model;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;

/**
 * The Minesweeper rules engine. Completely independent of any UI toolkit.
 *
 * <p>Mines are placed lazily on the first reveal so the first click is always safe and the
 * 3 x 3 block around it never contains a mine, which guarantees an opening.
 */
public final class Board {

    private static final int[][] NEIGHBOUR_OFFSETS = {
            {-1, -1}, {-1, 0}, {-1, 1},
            {0, -1}, {0, 1},
            {1, -1}, {1, 0}, {1, 1}
    };
    /** Radius of the mine-free zone around the first click (1 = a 3 x 3 block). */
    private static final int SAFE_RADIUS = 1;
    private static final char LAYOUT_MINE = '*';

    private final int rows;
    private final int cols;
    private final int mineCount;
    private final Cell[][] cells;
    private final Random random;

    private GameState state = GameState.READY;
    private boolean minesPlaced;
    private int revealedSafeCells;
    private int flagCount;
    private Position explodedAt;

    /**
     * Creates a board for the given configuration with a non-deterministic mine layout.
     *
     * @param config board size and mine count
     */
    public Board(GameConfig config) {
        this(config.rows(), config.cols(), config.mines(), new Random());
    }

    /**
     * Creates a board with a caller-supplied random source (useful for reproducible tests).
     *
     * @param rows      board height
     * @param cols      board width
     * @param mineCount number of mines
     * @param random    source of randomness for mine placement
     */
    public Board(int rows, int cols, int mineCount, Random random) {
        this(rows, cols, mineCount, random, true);
    }

    private Board(int rows, int cols, int mineCount, Random random, boolean validate) {
        if (validate) {
            GameConfig.validate(rows, cols, mineCount);
        }
        this.rows = rows;
        this.cols = cols;
        this.mineCount = mineCount;
        this.random = Objects.requireNonNull(random, "random");
        this.cells = new Cell[rows][cols];
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                cells[r][c] = new Cell();
            }
        }
    }

    /**
     * Builds a board with a fixed mine layout, e.g. {@code fromLayout("*..", "...", "..*")}.
     * The first-click safety rule does not apply because the mines are already placed.
     * Intended for tests, puzzles and debugging.
     *
     * @param layout one string per row; {@code '*'} is a mine, any other character is safe
     * @return a board in the {@link GameState#READY} state
     */
    public static Board fromLayout(String... layout) {
        if (layout.length == 0 || layout[0].isEmpty()) {
            throw new IllegalArgumentException("Layout must not be empty");
        }
        int width = layout[0].length();
        int mines = 0;
        for (String row : layout) {
            if (row.length() != width) {
                throw new IllegalArgumentException("All layout rows must have the same length");
            }
            for (char ch : row.toCharArray()) {
                if (ch == LAYOUT_MINE) {
                    mines++;
                }
            }
        }
        Board board = new Board(layout.length, width, mines, new Random(0), false);
        for (int r = 0; r < layout.length; r++) {
            for (int c = 0; c < width; c++) {
                board.cells[r][c].setMine(layout[r].charAt(c) == LAYOUT_MINE);
            }
        }
        board.minesPlaced = true;
        board.computeAdjacency();
        return board;
    }

    // ------------------------------------------------------------------ actions

    /**
     * Reveals a cell. Revealing a zero cell flood-fills its neighbourhood (iterative BFS).
     * Flagged and already-revealed cells are ignored, as is any input after the game ends.
     *
     * @param row cell row
     * @param col cell column
     * @return the cells uncovered, in BFS order, and whether a mine was hit
     */
    public RevealResult reveal(int row, int col) {
        if (state.isOver() || !isInBounds(row, col)) {
            return RevealResult.NONE;
        }
        Cell cell = cells[row][col];
        if (cell.isRevealed() || cell.isFlagged()) {
            return RevealResult.NONE;
        }
        startIfNeeded(row, col);
        if (cell.isMine()) {
            explode(row, col);
            return new RevealResult(List.of(new Position(row, col)), true);
        }
        List<Position> opened = floodReveal(row, col);
        checkForWin();
        return new RevealResult(opened, false);
    }

    /**
     * Chords a revealed number: if the number of adjacent flags equals its value, every other
     * hidden neighbour is revealed. A wrongly placed flag therefore loses the game.
     *
     * @param row cell row
     * @param col cell column
     * @return the cells uncovered and whether a mine was hit; {@link RevealResult#NONE} if the
     *         chord was not possible
     */
    public RevealResult chord(int row, int col) {
        if (state.isOver() || !isInBounds(row, col)) {
            return RevealResult.NONE;
        }
        Cell cell = cells[row][col];
        if (!cell.isRevealed() || cell.isMine() || cell.getAdjacentMines() == 0) {
            return RevealResult.NONE;
        }
        if (countAdjacentFlags(row, col) != cell.getAdjacentMines()) {
            return RevealResult.NONE;
        }
        List<Position> targets = new ArrayList<>();
        for (Position n : neighbours(row, col)) {
            Cell neighbour = cell(n);
            if (!neighbour.isRevealed() && !neighbour.isFlagged()) {
                targets.add(n);
            }
        }
        for (Position target : targets) {
            if (cell(target).isMine()) {
                explode(target.row(), target.col());
                return new RevealResult(List.of(target), true);
            }
        }
        List<Position> opened = new ArrayList<>();
        for (Position target : targets) {
            opened.addAll(floodReveal(target.row(), target.col()));
        }
        checkForWin();
        return new RevealResult(opened, false);
    }

    /**
     * Cycles the marker on a hidden cell: none, flag, question mark (if enabled), none.
     *
     * @param row                  cell row
     * @param col                  cell column
     * @param questionMarksEnabled whether the question-mark step is part of the cycle
     * @return {@code true} if the marker changed
     */
    public boolean toggleMark(int row, int col, boolean questionMarksEnabled) {
        if (state.isOver() || !isInBounds(row, col)) {
            return false;
        }
        Cell cell = cells[row][col];
        if (cell.isRevealed()) {
            return false;
        }
        CellMark next = switch (cell.getMark()) {
            case NONE -> CellMark.FLAG;
            case FLAG -> questionMarksEnabled ? CellMark.QUESTION : CellMark.NONE;
            case QUESTION -> CellMark.NONE;
        };
        if (cell.isFlagged()) {
            flagCount--;
        }
        if (next == CellMark.FLAG) {
            flagCount++;
        }
        cell.setMark(next);
        return true;
    }

    // ------------------------------------------------------------------ queries

    /** @return current game state */
    public GameState getState() {
        return state;
    }

    /** @return board height */
    public int getRows() {
        return rows;
    }

    /** @return board width */
    public int getCols() {
        return cols;
    }

    /** @return total number of mines */
    public int getMineCount() {
        return mineCount;
    }

    /** @return number of flags currently placed */
    public int getFlagCount() {
        return flagCount;
    }

    /** @return the value for the mine counter display: mines minus flags (may be negative) */
    public int getRemainingMineCounter() {
        return mineCount - flagCount;
    }

    /** @return number of safe cells revealed so far */
    public int getRevealedSafeCells() {
        return revealedSafeCells;
    }

    /** @return number of cells that do not contain a mine */
    public int getSafeCellCount() {
        return rows * cols - mineCount;
    }

    /** @return {@code true} once the mine layout exists (after the first reveal) */
    public boolean areMinesPlaced() {
        return minesPlaced;
    }

    /** @return the mine that ended the game, if the game was lost */
    public Optional<Position> getExplodedAt() {
        return Optional.ofNullable(explodedAt);
    }

    /**
     * @param row cell row
     * @param col cell column
     * @return the cell at that position
     */
    public Cell getCell(int row, int col) {
        return cells[row][col];
    }

    /**
     * @param position cell position
     * @return the cell at that position
     */
    public Cell getCell(Position position) {
        return cell(position);
    }

    /**
     * @param row cell row
     * @param col cell column
     * @return {@code true} if the position lies on the board
     */
    public boolean isInBounds(int row, int col) {
        return row >= 0 && row < rows && col >= 0 && col < cols;
    }

    /**
     * @param row cell row
     * @param col cell column
     * @return {@code true} if the game is lost and this cell carries a flag but no mine
     */
    public boolean isWrongFlag(int row, int col) {
        Cell cell = cells[row][col];
        return state == GameState.LOST && cell.isFlagged() && !cell.isMine();
    }

    /**
     * @param row cell row
     * @param col cell column
     * @return the in-bounds neighbours of the position (up to eight)
     */
    public List<Position> neighbours(int row, int col) {
        List<Position> result = new ArrayList<>(NEIGHBOUR_OFFSETS.length);
        for (int[] offset : NEIGHBOUR_OFFSETS) {
            int r = row + offset[0];
            int c = col + offset[1];
            if (isInBounds(r, c)) {
                result.add(new Position(r, c));
            }
        }
        return result;
    }

    /**
     * The cells a chord at this position would act on: hidden, unflagged cells in the 3 x 3
     * block centred here. Used by the view to show them pressed while the mouse is held.
     *
     * @param row cell row
     * @param col cell column
     * @return the affected positions (possibly empty)
     */
    public List<Position> chordTargets(int row, int col) {
        List<Position> result = new ArrayList<>();
        if (!isInBounds(row, col)) {
            return result;
        }
        List<Position> block = neighbours(row, col);
        block.add(new Position(row, col));
        for (Position p : block) {
            Cell c = cell(p);
            if (!c.isRevealed() && !c.isFlagged()) {
                result.add(p);
            }
        }
        return result;
    }

    /**
     * @param row cell row
     * @param col cell column
     * @return number of flags around the position
     */
    public int countAdjacentFlags(int row, int col) {
        int count = 0;
        for (Position n : neighbours(row, col)) {
            if (cell(n).isFlagged()) {
                count++;
            }
        }
        return count;
    }

    // ------------------------------------------------------------------ internals

    private Cell cell(Position p) {
        return cells[p.row()][p.col()];
    }

    private void startIfNeeded(int row, int col) {
        if (state != GameState.READY) {
            return;
        }
        if (!minesPlaced) {
            placeMines(row, col);
        }
        state = GameState.PLAYING;
    }

    private void placeMines(int safeRow, int safeCol) {
        List<Position> candidates = new ArrayList<>(rows * cols);
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                boolean inSafeZone = Math.abs(r - safeRow) <= SAFE_RADIUS
                        && Math.abs(c - safeCol) <= SAFE_RADIUS;
                if (!inSafeZone) {
                    candidates.add(new Position(r, c));
                }
            }
        }
        Collections.shuffle(candidates, random);
        for (int i = 0; i < mineCount; i++) {
            cell(candidates.get(i)).setMine(true);
        }
        minesPlaced = true;
        computeAdjacency();
    }

    private void computeAdjacency() {
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                int count = 0;
                for (Position n : neighbours(r, c)) {
                    if (cell(n).isMine()) {
                        count++;
                    }
                }
                cells[r][c].setAdjacentMines(count);
            }
        }
    }

    /** Iterative breadth-first reveal; never recurses, so huge empty areas are safe. */
    private List<Position> floodReveal(int row, int col) {
        List<Position> opened = new ArrayList<>();
        Deque<Position> queue = new ArrayDeque<>();
        queue.add(new Position(row, col));
        while (!queue.isEmpty()) {
            Position p = queue.poll();
            Cell c = cell(p);
            if (c.isRevealed() || c.isMine() || c.isFlagged()) {
                continue;
            }
            c.setRevealed(true);
            c.setMark(CellMark.NONE);
            revealedSafeCells++;
            opened.add(p);
            if (c.getAdjacentMines() == 0) {
                for (Position n : neighbours(p.row(), p.col())) {
                    Cell neighbour = cell(n);
                    if (!neighbour.isRevealed() && !neighbour.isFlagged()) {
                        queue.add(n);
                    }
                }
            }
        }
        return opened;
    }

    private void explode(int row, int col) {
        state = GameState.LOST;
        explodedAt = new Position(row, col);
        cells[row][col].setExploded(true);
        for (Cell[] rowCells : cells) {
            for (Cell c : rowCells) {
                if (c.isMine() && !c.isFlagged()) {
                    c.setMark(CellMark.NONE);
                    c.setRevealed(true);
                }
            }
        }
    }

    private void checkForWin() {
        if (revealedSafeCells != getSafeCellCount()) {
            return;
        }
        state = GameState.WON;
        for (Cell[] rowCells : cells) {
            for (Cell c : rowCells) {
                if (c.isMine()) {
                    c.setMark(CellMark.FLAG);
                }
            }
        }
        flagCount = mineCount;
    }
}

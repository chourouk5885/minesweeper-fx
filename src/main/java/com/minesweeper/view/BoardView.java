package com.minesweeper.view;

import com.minesweeper.model.Board;
import com.minesweeper.model.Cell;
import com.minesweeper.model.GameState;
import com.minesweeper.model.Position;
import javafx.animation.AnimationTimer;
import javafx.animation.TranslateTransition;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * The minefield. Draws the whole board on a single canvas and owns all board animations:
 * the reveal fade/scale, the flood-fill ripple, the loss shake and the mine cascade.
 *
 * <p>This class only renders; input is handled by the controller, which uses
 * {@link #positionAt(double, double)} to translate mouse coordinates.
 */
public final class BoardView extends Canvas {

    private static final long NANOS_PER_MILLI = 1_000_000L;
    private static final long REVEAL_DURATION_NS = 100 * NANOS_PER_MILLI;
    /** Total time budget for the ripple start offsets; with the reveal itself this stays under ~300 ms. */
    private static final long RIPPLE_BUDGET_NS = 200 * NANOS_PER_MILLI;
    private static final long MAX_STAGGER_NS = 12 * NANOS_PER_MILLI;
    private static final long CASCADE_DELAY_NS = 260 * NANOS_PER_MILLI;
    private static final long CASCADE_SPAN_NS = 520 * NANOS_PER_MILLI;
    private static final Duration SHAKE_STEP = Duration.millis(40);
    private static final double SHAKE_DISTANCE = 6;
    private static final int SHAKE_CYCLES = 6;
    private static final long NOT_ANIMATED = 0L;

    private Board board;
    private Theme theme;
    private double cellSize;
    private long[][] revealStart;

    private Position hover;
    private Position focus = new Position(0, 0);
    private boolean focusVisible;
    private Set<Position> pressed = Set.of();

    private long animationEnd;
    private boolean animating;
    private final AnimationTimer timer = new AnimationTimer() {
        @Override
        public void handle(long now) {
            draw(now);
            if (now > animationEnd) {
                stop();
                animating = false;
            }
        }
    };

    /**
     * @param board    the board to render
     * @param theme    initial theme
     * @param cellSize initial cell size in pixels
     */
    public BoardView(Board board, Theme theme, double cellSize) {
        this.theme = Objects.requireNonNull(theme);
        this.cellSize = cellSize;
        setFocusTraversable(true);
        setBoard(board);
    }

    // ------------------------------------------------------------------ configuration

    /**
     * Shows a new board and clears all view state.
     *
     * @param board the new board
     */
    public void setBoard(Board board) {
        this.board = Objects.requireNonNull(board);
        this.revealStart = new long[board.getRows()][board.getCols()];
        this.hover = null;
        this.pressed = Set.of();
        this.focus = new Position(
                Math.min(focus.row(), board.getRows() - 1),
                Math.min(focus.col(), board.getCols() - 1));
        timer.stop();
        animating = false;
        setTranslateX(0);
        resizeToBoard();
        redraw();
    }

    /** @param theme the theme to draw with */
    public void setTheme(Theme theme) {
        this.theme = Objects.requireNonNull(theme);
        redraw();
    }

    /** @param cellSize new cell edge length in pixels */
    public void setCellSize(double cellSize) {
        this.cellSize = cellSize;
        resizeToBoard();
        redraw();
    }

    /** @return current cell edge length in pixels */
    public double getCellSize() {
        return cellSize;
    }

    // ------------------------------------------------------------------ interaction state

    /**
     * Converts canvas coordinates to a board position.
     *
     * @param x canvas x
     * @param y canvas y
     * @return the position, or {@code null} if outside the board
     */
    public Position positionAt(double x, double y) {
        if (x < 0 || y < 0) {
            return null;
        }
        int col = (int) (x / cellSize);
        int row = (int) (y / cellSize);
        return board.isInBounds(row, col) ? new Position(row, col) : null;
    }

    /** @param position cell under the mouse, or {@code null} */
    public void setHover(Position position) {
        if (!Objects.equals(hover, position)) {
            hover = position;
            redraw();
        }
    }

    /** @param positions cells to draw depressed */
    public void setPressed(Collection<Position> positions) {
        Set<Position> next = Set.copyOf(positions);
        if (!next.equals(pressed)) {
            pressed = next;
            redraw();
        }
    }

    /** @param position keyboard focus cell */
    public void setFocus(Position position) {
        if (!Objects.equals(focus, position)) {
            focus = position;
            redraw();
        }
    }

    /** @param visible whether to draw the keyboard focus ring (hidden while using the mouse) */
    public void setFocusVisible(boolean visible) {
        if (focusVisible != visible) {
            focusVisible = visible;
            redraw();
        }
    }

    // ------------------------------------------------------------------ animations

    /**
     * Fades and scales the given cells in, staggered in the order given (BFS order produces a
     * ripple spreading out from the click).
     *
     * @param cells cells that were just revealed
     */
    public void animateReveal(List<Position> cells) {
        if (cells.isEmpty()) {
            redraw();
            return;
        }
        long now = System.nanoTime();
        long stagger = Math.min(MAX_STAGGER_NS, RIPPLE_BUDGET_NS / cells.size());
        for (int i = 0; i < cells.size(); i++) {
            Position p = cells.get(i);
            revealStart[p.row()][p.col()] = now + i * stagger;
        }
        runAnimationUntil(now + (cells.size() - 1) * stagger + REVEAL_DURATION_NS);
    }

    /**
     * Shakes the board, then reveals the remaining mines in a cascade spreading from the one
     * that exploded.
     *
     * @param exploded the mine the player hit
     */
    public void animateLoss(Position exploded) {
        long now = System.nanoTime();
        revealStart[exploded.row()][exploded.col()] = now;

        List<Position> mines = new ArrayList<>();
        double maxDistance = 1;
        for (int r = 0; r < board.getRows(); r++) {
            for (int c = 0; c < board.getCols(); c++) {
                Cell cell = board.getCell(r, c);
                if (cell.isMine() && cell.isRevealed() && !cell.isExploded()) {
                    mines.add(new Position(r, c));
                    maxDistance = Math.max(maxDistance, distance(exploded, r, c));
                }
            }
        }
        for (Position p : mines) {
            double t = distance(exploded, p.row(), p.col()) / maxDistance;
            revealStart[p.row()][p.col()] = now + CASCADE_DELAY_NS + (long) (t * CASCADE_SPAN_NS);
        }
        runAnimationUntil(now + CASCADE_DELAY_NS + CASCADE_SPAN_NS + REVEAL_DURATION_NS);
        shake();
    }

    /** Redraws the board immediately. */
    public void redraw() {
        draw(System.nanoTime());
    }

    // ------------------------------------------------------------------ internals

    private void runAnimationUntil(long end) {
        animationEnd = Math.max(animationEnd, end);
        if (!animating) {
            animating = true;
            timer.start();
        }
        redraw();
    }

    private void shake() {
        TranslateTransition shake = new TranslateTransition(SHAKE_STEP, this);
        shake.setFromX(-SHAKE_DISTANCE);
        shake.setToX(SHAKE_DISTANCE);
        shake.setCycleCount(SHAKE_CYCLES);
        shake.setAutoReverse(true);
        shake.setOnFinished(e -> setTranslateX(0));
        shake.play();
    }

    private void resizeToBoard() {
        setWidth(board.getCols() * cellSize);
        setHeight(board.getRows() * cellSize);
    }

    private void draw(long now) {
        GraphicsContext gc = getGraphicsContext2D();
        gc.clearRect(0, 0, getWidth(), getHeight());
        GameState state = board.getState();
        boolean interactive = !state.isOver();
        for (int r = 0; r < board.getRows(); r++) {
            for (int c = 0; c < board.getCols(); c++) {
                Cell cell = board.getCell(r, c);
                boolean hovered = interactive && hover != null && hover.row() == r && hover.col() == c;
                boolean isPressed = interactive && !pressed.isEmpty() && pressed.contains(new Position(r, c));
                boolean focused = focusVisible && interactive && focus.row() == r && focus.col() == c;
                CellView.RenderState renderState = new CellView.RenderState(
                        hovered, isPressed, focused, progress(r, c, now), board.isWrongFlag(r, c));
                CellView.paint(gc, theme, cell, c * cellSize, r * cellSize, cellSize, renderState);
            }
        }
    }

    private double progress(int row, int col, long now) {
        long start = revealStart[row][col];
        if (start == NOT_ANIMATED) {
            return 1.0;
        }
        if (now < start) {
            return -1.0;
        }
        return Math.min(1.0, (now - start) / (double) REVEAL_DURATION_NS);
    }

    private static double distance(Position from, int row, int col) {
        return Math.hypot(from.row() - row, from.col() - col);
    }
}

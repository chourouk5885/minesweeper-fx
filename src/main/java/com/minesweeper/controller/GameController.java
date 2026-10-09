package com.minesweeper.controller;

import com.minesweeper.model.Board;
import com.minesweeper.model.Cell;
import com.minesweeper.model.Difficulty;
import com.minesweeper.model.GameConfig;
import com.minesweeper.model.GameState;
import com.minesweeper.model.GameStats;
import com.minesweeper.model.Position;
import com.minesweeper.model.RevealResult;
import com.minesweeper.util.Settings;
import com.minesweeper.util.SettingsManager;
import com.minesweeper.util.SoundPlayer;
import com.minesweeper.view.BoardView;
import com.minesweeper.view.Dialogs;
import com.minesweeper.view.FaceIcon;
import com.minesweeper.view.GameMenuBar;
import com.minesweeper.view.HeaderBar;
import com.minesweeper.view.MainWindow;
import com.minesweeper.view.Theme;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Rectangle2D;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.time.LocalDate;
import java.util.List;

/**
 * Connects the model to the view: translates mouse and keyboard input into board actions,
 * runs the timer, moves the game between states, records statistics and opens dialogs.
 */
public final class GameController implements GameMenuBar.Actions, GameMenuBar.State {

    private static final Duration TIMER_TICK = Duration.millis(200);
    private static final int MAX_DISPLAY_SECONDS = 999;
    private static final long MILLIS_PER_SECOND = 1000L;
    private static final long NANOS_PER_MILLI = 1_000_000L;
    private static final Duration WIN_DIALOG_DELAY = Duration.millis(450);
    private static final double MIN_CELL_SIZE = 18;
    /** Space taken by window decorations, menu, header and padding, used to fit big boards on screen. */
    private static final double WINDOW_CHROME_WIDTH = 90;
    private static final double WINDOW_CHROME_HEIGHT = 210;

    private final Stage stage;
    private final SettingsManager settingsManager;
    private final Settings settings;
    private final GameStats stats;
    private final SoundPlayer sound = new SoundPlayer();
    private final HeaderBar header = new HeaderBar();
    private final BoardView boardView;
    private final MainWindow window;
    private final Timeline ticker;

    private Theme theme;
    private GameConfig config;
    private Board board;
    private long startNanos;
    private long finalMillis;
    private boolean shutDown;

    // mouse state
    private boolean primaryDown;
    private boolean secondaryDown;
    private boolean middleDown;
    /** Set once a chord fires so releasing the other button of a left+right chord does nothing. */
    private boolean chordConsumed;

    private Position focus = new Position(0, 0);

    /**
     * Builds the window and starts the first game (not shown until {@link #show()}).
     *
     * @param stage           the primary stage
     * @param settingsManager persistence for settings and statistics
     */
    public GameController(Stage stage, SettingsManager settingsManager) {
        this.stage = stage;
        this.settingsManager = settingsManager;
        this.settings = settingsManager.loadSettings();
        this.stats = settingsManager.loadStats();
        this.theme = Theme.fromName(settings.getThemeName());
        this.config = initialConfig();
        this.board = new Board(config);
        sound.setEnabled(settings.isSoundEnabled());

        boardView = new BoardView(board, theme, settings.getCellSize());
        GameMenuBar menuBar = new GameMenuBar(this, this);
        window = new MainWindow(stage, menuBar, header, boardView);
        window.applyTheme(theme);

        ticker = new Timeline(new KeyFrame(TIMER_TICK, e -> updateTimerDisplay()));
        ticker.setCycleCount(Animation.INDEFINITE);

        header.setOnRestart(this::newGame);
        wireMouse();
        window.getScene().addEventFilter(KeyEvent.KEY_PRESSED, this::onKeyPressed);
        stage.setOnCloseRequest(e -> shutdown());

        newGame(config);
    }

    /** Shows the window. */
    public void show() {
        stage.show();
        boardView.requestFocus();
    }

    // ================================================================== game lifecycle

    @Override
    public void newGame() {
        newGame(config);
    }

    private void newGame(GameConfig newConfig) {
        recordAbandonedGame();
        ticker.stop();
        config = newConfig;
        board = new Board(config);
        finalMillis = 0;
        resetMouseState();

        header.setSeconds(0);
        header.setMineCount(board.getRemainingMineCounter());
        header.setMood(FaceIcon.Mood.HAPPY);
        boardView.setCellSize(effectiveCellSize());
        boardView.setBoard(board);
        focus = new Position(Math.min(focus.row(), board.getRows() - 1), Math.min(focus.col(), board.getCols() - 1));
        boardView.setFocus(focus);
        window.fitToContent();
    }

    private void startTimer() {
        startNanos = System.nanoTime();
        ticker.playFromStart();
        updateTimerDisplay();
    }

    private void stopTimer() {
        finalMillis = (System.nanoTime() - startNanos) / NANOS_PER_MILLI;
        ticker.stop();
        updateTimerDisplay();
    }

    private long elapsedMillis() {
        return board.getState() == GameState.PLAYING
                ? (System.nanoTime() - startNanos) / NANOS_PER_MILLI
                : finalMillis;
    }

    private void updateTimerDisplay() {
        header.setSeconds((int) Math.min(MAX_DISPLAY_SECONDS, elapsedMillis() / MILLIS_PER_SECOND));
    }

    private void onWin() {
        stopTimer();
        long millis = finalMillis;
        Difficulty difficulty = config.difficulty();
        stats.recordWin(difficulty);
        int rank = stats.bestTimeRank(difficulty, millis);
        saveStats();

        sound.play(SoundPlayer.Effect.WIN);
        header.setMood(FaceIcon.Mood.COOL);
        header.setMineCount(0);
        boardView.setPressed(List.of());

        Board wonBoard = board;
        PauseTransition pause = new PauseTransition(WIN_DIALOG_DELAY);
        // showAndWait is not allowed inside an animation callback, so defer to the next pulse.
        pause.setOnFinished(e -> Platform.runLater(() -> showWinDialog(wonBoard, difficulty, millis, rank)));
        pause.play();
    }

    private void showWinDialog(Board wonBoard, Difficulty difficulty, long millis, int rank) {
        if (wonBoard != board) {
            return; // the player already started another game
        }
        Dialogs.WinResult result = Dialogs.win(stage, theme, difficulty.getDisplayName(), millis, rank,
                settings.getLastPlayerName());
        result.playerName().ifPresent(name -> {
            stats.addBestTime(difficulty, new GameStats.BestTime(name, millis, LocalDate.now()));
            settings.setLastPlayerName(name);
            saveSettings();
            saveStats();
        });
        if (result.playAgain()) {
            newGame();
        }
    }

    private void onLoss() {
        stopTimer();
        stats.recordLoss(config.difficulty());
        saveStats();
        sound.play(SoundPlayer.Effect.EXPLOSION);
        header.setMood(FaceIcon.Mood.DEAD);
        // Schedule the cascade before any redraw, otherwise every mine would flash for one frame.
        board.getExplodedAt().ifPresent(boardView::animateLoss);
        boardView.setPressed(List.of());
    }

    /** Leaving a game in progress counts as a loss, as in the classic game. */
    private void recordAbandonedGame() {
        if (board != null && board.getState() == GameState.PLAYING) {
            stats.recordLoss(config.difficulty());
            saveStats();
        }
    }

    // ================================================================== board actions

    private void primaryAction(Position p) {
        if (board.getCell(p).isRevealed()) {
            chordAt(p);
        } else {
            revealAt(p);
        }
    }

    private void revealAt(Position p) {
        boolean firstMove = board.getState() == GameState.READY;
        RevealResult result = board.reveal(p.row(), p.col());
        if (!result.changed()) {
            return;
        }
        if (firstMove) {
            startTimer();
        }
        handleResult(result);
    }

    private void chordAt(Position p) {
        RevealResult result = board.chord(p.row(), p.col());
        if (result.changed()) {
            handleResult(result);
        }
    }

    private void toggleMark(Position p) {
        if (board.toggleMark(p.row(), p.col(), settings.isQuestionMarksEnabled())) {
            sound.play(SoundPlayer.Effect.FLAG);
            header.setMineCount(board.getRemainingMineCounter());
            boardView.redraw();
        }
    }

    private void handleResult(RevealResult result) {
        header.setMineCount(board.getRemainingMineCounter());
        switch (board.getState()) {
            case LOST -> onLoss();
            case WON -> {
                boardView.animateReveal(result.revealed());
                onWin();
            }
            default -> {
                sound.play(SoundPlayer.Effect.REVEAL);
                boardView.animateReveal(result.revealed());
            }
        }
    }

    // ================================================================== mouse

    private void wireMouse() {
        boardView.setOnMouseMoved(e -> boardView.setHover(positionAt(e)));
        boardView.setOnMouseExited(e -> {
            boardView.setHover(null);
            if (!anyButtonDown()) {
                boardView.setPressed(List.of());
            }
        });
        boardView.setOnMousePressed(this::onMousePressed);
        boardView.setOnMouseDragged(e -> {
            Position p = positionAt(e);
            boardView.setHover(p);
            updatePressedCells(p);
        });
        boardView.setOnMouseReleased(this::onMouseReleased);
    }

    private void onMousePressed(MouseEvent e) {
        boardView.requestFocus();
        boardView.setFocusVisible(false);
        if (board.getState().isOver()) {
            return;
        }
        Position p = positionAt(e);
        switch (e.getButton()) {
            case PRIMARY -> primaryDown = true;
            case MIDDLE -> middleDown = true;
            case SECONDARY -> {
                secondaryDown = true;
                if (!primaryDown && p != null) {
                    toggleMark(p);
                }
            }
            default -> {
                return;
            }
        }
        if (primaryDown || middleDown) {
            header.setMood(FaceIcon.Mood.SURPRISED);
        }
        updatePressedCells(p);
    }

    private void onMouseReleased(MouseEvent e) {
        Position p = positionAt(e);
        boolean chording = middleDown || (primaryDown && secondaryDown);
        boolean primaryReleased = e.getButton() == MouseButton.PRIMARY && primaryDown;

        primaryDown = e.isPrimaryButtonDown();
        secondaryDown = e.isSecondaryButtonDown();
        middleDown = e.isMiddleButtonDown();

        if (!board.getState().isOver() && !chordConsumed) {
            if (chording) {
                if (p != null) {
                    chordAt(p);
                }
                chordConsumed = true;
            } else if (primaryReleased && p != null) {
                primaryAction(p);
            }
        }
        if (!anyButtonDown()) {
            chordConsumed = false;
        }
        updatePressedCells(p);
        if (!board.getState().isOver()) {
            header.setMood(primaryDown || middleDown ? FaceIcon.Mood.SURPRISED : FaceIcon.Mood.HAPPY);
        }
    }

    /** Shows which cells the current press would affect (one cell, or a chord's 3x3 block). */
    private void updatePressedCells(Position p) {
        if (p == null || board.getState().isOver() || chordConsumed || !(primaryDown || middleDown)) {
            boardView.setPressed(List.of());
            return;
        }
        boolean chording = middleDown || secondaryDown;
        Cell cell = board.getCell(p);
        if (chording || cell.isRevealed()) {
            boardView.setPressed(board.chordTargets(p.row(), p.col()));
        } else if (!cell.isFlagged()) {
            boardView.setPressed(List.of(p));
        } else {
            boardView.setPressed(List.of());
        }
    }

    private Position positionAt(MouseEvent e) {
        return boardView.positionAt(e.getX(), e.getY());
    }

    private boolean anyButtonDown() {
        return primaryDown || secondaryDown || middleDown;
    }

    private void resetMouseState() {
        primaryDown = false;
        secondaryDown = false;
        middleDown = false;
        chordConsumed = false;
    }

    // ================================================================== keyboard

    private void onKeyPressed(KeyEvent e) {
        if (e.isControlDown() || e.isAltDown() || e.isMetaDown()) {
            return; // leave modified keys to the menus and the OS
        }
        KeyCode code = e.getCode();
        switch (code) {
            case F2, N -> newGame();
            case F1 -> showHowToPlay();
            case UP -> moveFocus(-1, 0);
            case DOWN -> moveFocus(1, 0);
            case LEFT -> moveFocus(0, -1);
            case RIGHT -> moveFocus(0, 1);
            case SPACE, ENTER -> keyboardAction(() -> primaryAction(focus));
            case F -> keyboardAction(() -> toggleMark(focus));
            case C -> keyboardAction(() -> chordAt(focus));
            default -> {
                return;
            }
        }
        e.consume();
    }

    private void moveFocus(int dRow, int dCol) {
        int row = Math.max(0, Math.min(board.getRows() - 1, focus.row() + dRow));
        int col = Math.max(0, Math.min(board.getCols() - 1, focus.col() + dCol));
        focus = new Position(row, col);
        boardView.setFocus(focus);
        boardView.setFocusVisible(true);
    }

    private void keyboardAction(Runnable action) {
        if (board.getState().isOver()) {
            return;
        }
        boardView.setFocusVisible(true);
        action.run();
    }

    // ================================================================== menu actions

    @Override
    public void selectDifficulty(Difficulty difficulty) {
        settings.setDifficulty(difficulty);
        saveSettings();
        newGame(GameConfig.of(difficulty));
    }

    @Override
    public void customDifficulty() {
        GameConfig current = config.difficulty() == Difficulty.CUSTOM ? config : savedCustomConfig();
        Dialogs.customGame(stage, theme, current).ifPresent(custom -> {
            settings.setDifficulty(Difficulty.CUSTOM);
            settings.setCustomRows(custom.rows());
            settings.setCustomCols(custom.cols());
            settings.setCustomMines(custom.mines());
            saveSettings();
            newGame(custom);
        });
    }

    @Override
    public void showStatistics() {
        Dialogs.statistics(stage, theme, stats, () -> {
            stats.reset();
            saveStats();
        });
    }

    @Override
    public void showBestTimes() {
        Dialogs.bestTimes(stage, theme, stats);
    }

    @Override
    public void exit() {
        shutdown();
        Platform.exit();
    }

    @Override
    public void selectTheme(Theme newTheme) {
        theme = newTheme;
        settings.setThemeName(newTheme.name());
        saveSettings();
        window.applyTheme(newTheme);
        boardView.setTheme(newTheme);
    }

    @Override
    public void toggleQuestionMarks() {
        settings.setQuestionMarksEnabled(!settings.isQuestionMarksEnabled());
        saveSettings();
    }

    @Override
    public void toggleSound() {
        boolean enabled = !settings.isSoundEnabled();
        settings.setSoundEnabled(enabled);
        sound.setEnabled(enabled);
        saveSettings();
        sound.play(SoundPlayer.Effect.FLAG); // audible confirmation when turning on
    }

    @Override
    public void selectCellSize(int pixels) {
        settings.setCellSize(pixels);
        saveSettings();
        boardView.setCellSize(effectiveCellSize());
        window.fitToContent();
    }

    @Override
    public void showHowToPlay() {
        Dialogs.howToPlay(stage, theme);
    }

    @Override
    public void showAbout() {
        Dialogs.about(stage, theme);
    }

    // ================================================================== menu state

    @Override
    public Difficulty currentDifficulty() {
        return config.difficulty();
    }

    @Override
    public Theme currentTheme() {
        return theme;
    }

    @Override
    public boolean questionMarksEnabled() {
        return settings.isQuestionMarksEnabled();
    }

    @Override
    public boolean soundEnabled() {
        return settings.isSoundEnabled();
    }

    @Override
    public int preferredCellSize() {
        return settings.getCellSize();
    }

    // ================================================================== helpers

    private GameConfig initialConfig() {
        Difficulty d = settings.getDifficulty();
        return d.isPreset() ? GameConfig.of(d) : savedCustomConfig();
    }

    private GameConfig savedCustomConfig() {
        try {
            return GameConfig.custom(settings.getCustomRows(), settings.getCustomCols(), settings.getCustomMines());
        } catch (IllegalArgumentException e) {
            Difficulty fallback = Difficulty.CUSTOM;
            return GameConfig.custom(fallback.getRows(), fallback.getCols(), fallback.getMines());
        }
    }

    /** The preferred cell size, shrunk if needed so very large boards still fit on screen. */
    private double effectiveCellSize() {
        Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
        double fitWidth = (bounds.getWidth() - WINDOW_CHROME_WIDTH) / config.cols();
        double fitHeight = (bounds.getHeight() - WINDOW_CHROME_HEIGHT) / config.rows();
        double size = Math.min(settings.getCellSize(), Math.min(fitWidth, fitHeight));
        return Math.max(MIN_CELL_SIZE, Math.floor(size));
    }

    private void saveSettings() {
        settingsManager.saveSettings(settings);
    }

    private void saveStats() {
        settingsManager.saveStats(stats);
    }

    private void shutdown() {
        if (shutDown) {
            return;
        }
        shutDown = true;
        recordAbandonedGame();
        ticker.stop();
        saveSettings();
        sound.shutdown();
    }
}

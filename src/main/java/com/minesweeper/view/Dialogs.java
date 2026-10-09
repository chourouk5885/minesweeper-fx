package com.minesweeper.view;

import com.minesweeper.model.Difficulty;
import com.minesweeper.model.GameConfig;
import com.minesweeper.model.GameStats;
import javafx.animation.AnimationTimer;
import javafx.event.ActionEvent;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Window;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Random;

/**
 * All dialogs used by the game. Every dialog is styled with the active theme.
 */
public final class Dialogs {

    /** Longest player name accepted for the best-time table. */
    public static final int MAX_NAME_LENGTH = 24;

    private static final double GRID_HGAP = 18;
    private static final double GRID_VGAP = 8;
    private static final double CONTENT_SPACING = 10;
    private static final double TEXT_WIDTH = 440;
    private static final double WIN_FACE_SIZE = 64;
    private static final double SPINNER_WIDTH = 110;
    private static final double MILLIS_PER_SECOND = 1000.0;
    private static final double PERCENT = 100.0;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH);

    /**
     * What the player chose in the win dialog.
     *
     * @param playerName name entered for a new best time (empty when the time did not rank)
     * @param playAgain  {@code true} if "Play Again" was pressed
     */
    public record WinResult(Optional<String> playerName, boolean playAgain) {
    }

    private Dialogs() {
    }

    // ------------------------------------------------------------------ custom game

    /**
     * Asks for a custom board size. The mine limit follows the chosen size live.
     *
     * @param owner   parent window
     * @param theme   active theme
     * @param current values to pre-fill
     * @return the chosen configuration, or empty if cancelled
     */
    public static Optional<GameConfig> customGame(Window owner, Theme theme, GameConfig current) {
        Dialog<GameConfig> dialog = create(owner, theme, "Custom Game", "Design your own minefield");

        Spinner<Integer> rows = spinner(Difficulty.MIN_ROWS, Difficulty.MAX_ROWS, current.rows());
        Spinner<Integer> cols = spinner(Difficulty.MIN_COLS, Difficulty.MAX_COLS, current.cols());
        Spinner<Integer> mines = spinner(Difficulty.MIN_MINES,
                Difficulty.maxMines(current.rows(), current.cols()), current.mines());
        Label summary = new Label();
        summary.getStyleClass().add("dialog-muted");

        Runnable update = () -> {
            int max = Difficulty.maxMines(rows.getValue(), cols.getValue());
            SpinnerValueFactory.IntegerSpinnerValueFactory factory =
                    (SpinnerValueFactory.IntegerSpinnerValueFactory) mines.getValueFactory();
            factory.setMax(max);
            if (factory.getValue() > max) {
                factory.setValue(max);
            }
            double density = PERCENT * mines.getValue() / (rows.getValue() * cols.getValue());
            summary.setText(String.format(Locale.ROOT, "%d × %d board · up to %d mines · %.0f%% density",
                    cols.getValue(), rows.getValue(), max, density));
        };
        rows.valueProperty().addListener((obs, oldValue, newValue) -> update.run());
        cols.valueProperty().addListener((obs, oldValue, newValue) -> update.run());
        mines.valueProperty().addListener((obs, oldValue, newValue) -> update.run());
        update.run();

        GridPane grid = formGrid();
        grid.addRow(0, new Label("Height (rows)"), rows);
        grid.addRow(1, new Label("Width (columns)"), cols);
        grid.addRow(2, new Label("Mines"), mines);
        grid.add(summary, 0, 3, 2, 1);
        dialog.getDialogPane().setContent(grid);

        ButtonType start = new ButtonType("Start Game", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(start, ButtonType.CANCEL);
        dialog.setResultConverter(button -> {
            if (button != start) {
                return null;
            }
            commit(rows);
            commit(cols);
            update.run();
            commit(mines);
            return GameConfig.custom(rows.getValue(), cols.getValue(), mines.getValue());
        });
        dialog.setOnShown(e -> rows.requestFocus());
        return dialog.showAndWait();
    }

    // ------------------------------------------------------------------ win

    /**
     * Celebrates a win, with confetti. If the time ranks, asks for a name.
     *
     * @param owner          parent window
     * @param theme          active theme
     * @param difficultyName difficulty label
     * @param millis         completion time
     * @param rank           1-based rank in the best-time table, or {@link GameStats#NOT_RANKED}
     * @param defaultName    pre-filled name
     * @return the player's choices
     */
    public static WinResult win(Window owner, Theme theme, String difficultyName, long millis, int rank,
                                String defaultName) {
        Dialog<Boolean> dialog = create(owner, theme, "You won!", null);
        boolean record = rank != GameStats.NOT_RANKED;

        Label title = new Label("Board cleared!");
        title.getStyleClass().add("dialog-title");
        Label time = new Label(String.format(Locale.ROOT, "%.1f seconds", millis / MILLIS_PER_SECOND));
        time.getStyleClass().add("dialog-big-number");
        Label detail = new Label(difficultyName);
        detail.getStyleClass().add("dialog-muted");

        VBox content = new VBox(CONTENT_SPACING, new FaceIcon(WIN_FACE_SIZE, FaceIcon.Mood.COOL), title, time, detail);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(8, 24, 8, 24));

        TextField nameField = new TextField(defaultName);
        if (record) {
            Label badge = new Label(rank == 1
                    ? "New record! Fastest " + difficultyName + " time ever."
                    : "New best time — #" + rank + " on the " + difficultyName + " table.");
            badge.getStyleClass().add("dialog-accent");
            nameField.setPromptText("Your name");
            nameField.setMaxWidth(220);
            nameField.textProperty().addListener((obs, oldValue, newValue) -> {
                if (newValue.length() > MAX_NAME_LENGTH) {
                    nameField.setText(oldValue);
                }
            });
            content.getChildren().addAll(badge, new Label("Enter your name for the leaderboard:"), nameField);
        }

        Confetti confetti = new Confetti();
        StackPane stack = new StackPane(confetti, content); // confetti falls behind the text
        confetti.setManaged(false);
        confetti.setMouseTransparent(true);
        dialog.getDialogPane().setContent(stack);

        ButtonType again = new ButtonType("Play Again", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(again, ButtonType.CLOSE);
        dialog.setResultConverter(button -> button == again);
        dialog.setOnShown(e -> {
            confetti.setWidth(stack.getWidth());
            confetti.setHeight(stack.getHeight());
            confetti.play();
            if (record) {
                nameField.requestFocus();
                nameField.selectAll();
            }
        });
        dialog.setOnHidden(e -> confetti.stop());

        boolean playAgain = dialog.showAndWait().orElse(false);
        Optional<String> name = Optional.empty();
        if (record) {
            String entered = nameField.getText().trim();
            name = Optional.of(entered.isEmpty() ? "Anonymous" : entered);
        }
        return new WinResult(name, playAgain);
    }

    // ------------------------------------------------------------------ statistics

    /**
     * Shows statistics for every difficulty, with a Reset button.
     *
     * @param owner   parent window
     * @param theme   active theme
     * @param stats   statistics to display
     * @param onReset called after the player confirms a reset
     */
    public static void statistics(Window owner, Theme theme, GameStats stats, Runnable onReset) {
        Dialog<ButtonType> dialog = create(owner, theme, "Statistics", "Your Minesweeper record");
        GridPane grid = formGrid();
        fillStatistics(grid, stats);
        dialog.getDialogPane().setContent(grid);

        ButtonType reset = new ButtonType("Reset…", ButtonBar.ButtonData.LEFT);
        dialog.getDialogPane().getButtonTypes().addAll(reset, ButtonType.CLOSE);
        dialog.setResultConverter(button -> button);
        Button resetButton = (Button) dialog.getDialogPane().lookupButton(reset);
        resetButton.addEventFilter(ActionEvent.ACTION, event -> {
            event.consume(); // keep the statistics dialog open
            Window self = dialog.getDialogPane().getScene().getWindow();
            if (confirm(self, theme, "Reset statistics?",
                    "This permanently clears games played, win rates, streaks and best times for every difficulty.")) {
                onReset.run();
                fillStatistics(grid, stats);
            }
        });
        dialog.showAndWait();
    }

    private static void fillStatistics(GridPane grid, GameStats stats) {
        grid.getChildren().clear();
        String[] rowNames = {"Games played", "Games won", "Win rate", "Current streak", "Best streak"};
        for (int i = 0; i < rowNames.length; i++) {
            grid.add(styled(new Label(rowNames[i]), "stat-name"), 0, i + 1);
        }
        int col = 1;
        for (Difficulty d : Difficulty.values()) {
            GameStats.DifficultyStats s = stats.get(d);
            Label header = styled(new Label(d.getDisplayName()), "stat-header");
            grid.add(header, col, 0);
            GridPane.setHalignment(header, HPos.RIGHT);
            String[] values = {
                    Integer.toString(s.getPlayed()),
                    Integer.toString(s.getWon()),
                    s.getPlayed() == 0 ? "—" : String.format(Locale.ROOT, "%.0f%%", s.getWinRate() * PERCENT),
                    Integer.toString(s.getCurrentStreak()),
                    Integer.toString(s.getBestStreak())};
            for (int i = 0; i < values.length; i++) {
                Label value = styled(new Label(values[i]), "stat-value");
                grid.add(value, col, i + 1);
                GridPane.setHalignment(value, HPos.RIGHT);
            }
            col++;
        }
    }

    // ------------------------------------------------------------------ best times

    /**
     * Shows the top five times for each preset difficulty.
     *
     * @param owner parent window
     * @param theme active theme
     * @param stats statistics holding the tables
     */
    public static void bestTimes(Window owner, Theme theme, GameStats stats) {
        Dialog<ButtonType> dialog = create(owner, theme, "Best Times", "Fastest clears");
        VBox sections = new VBox(16);
        for (Difficulty d : Difficulty.values()) {
            if (!d.isPreset()) {
                continue;
            }
            Label heading = styled(new Label(d.getDisplayName()), "section-title");
            GridPane table = new GridPane();
            table.setHgap(GRID_HGAP);
            table.setVgap(4);
            ColumnConstraints nameColumn = new ColumnConstraints();
            nameColumn.setMinWidth(160);
            table.getColumnConstraints().addAll(new ColumnConstraints(24), nameColumn,
                    new ColumnConstraints(70), new ColumnConstraints());
            List<GameStats.BestTime> times = stats.get(d).getBestTimes();
            if (times.isEmpty()) {
                table.add(styled(new Label("No times yet — go set one!"), "dialog-muted"), 0, 0, 4, 1);
            }
            for (int i = 0; i < times.size(); i++) {
                GameStats.BestTime t = times.get(i);
                Label seconds = new Label(String.format(Locale.ROOT, "%.1f s", t.millis() / MILLIS_PER_SECOND));
                table.addRow(i,
                        styled(new Label((i + 1) + "."), "dialog-muted"),
                        new Label(t.name()),
                        seconds,
                        styled(new Label(DATE_FORMAT.format(t.date())), "dialog-muted"));
                GridPane.setHalignment(seconds, HPos.RIGHT);
            }
            sections.getChildren().addAll(new VBox(6, heading, table));
        }
        sections.setPadding(new Insets(4, 8, 4, 8));
        dialog.getDialogPane().setContent(sections);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.setResultConverter(button -> button);
        dialog.showAndWait();
    }

    // ------------------------------------------------------------------ help / about

    /**
     * Rules and controls.
     *
     * @param owner parent window
     * @param theme active theme
     */
    public static void howToPlay(Window owner, Theme theme) {
        String text = """
                Goal
                Uncover every square that does not hide a mine. A number tells you how many \
                mines touch that square, including diagonals.

                Mouse
                • Left-click: reveal a square. Your first click is always safe and opens an area.
                • Right-click: cycle flag → question mark → blank (question marks can be turned off in Options).
                • Chord: when a number has exactly that many flags around it, left-click it, middle-click it, \
                or press left and right together to reveal all its other neighbours.
                • Click the face to start a new game.

                Keyboard
                • Arrow keys: move the highlighted square
                • Space or Enter: reveal  •  F: flag  •  C: chord
                • F2 or N: new game  •  F1: this help

                Tips
                • The counter on the left shows mines minus flags; it can go negative if you over-flag.
                • Chording with a wrong flag will set off a mine, so flag carefully.
                • Abandoning a game in progress counts as a loss in your statistics.""";
        info(owner, theme, "How to Play", "How to play Minesweeper", text);
    }

    /**
     * About box.
     *
     * @param owner parent window
     * @param theme active theme
     */
    public static void about(Window owner, Theme theme) {
        String text = """
                Minesweeper FX 1.0

                A modern take on the classic puzzle, written in Java with JavaFX.
                Features light and dark themes, chording, keyboard play, statistics and best times.

                Settings and statistics are stored in ~/.minesweeper-fx""";
        info(owner, theme, "About", "Minesweeper FX", text);
    }

    // ------------------------------------------------------------------ shared helpers

    /**
     * Simple yes/no confirmation.
     *
     * @param owner   parent window
     * @param theme   active theme
     * @param header  question
     * @param message explanation
     * @return {@code true} if confirmed
     */
    public static boolean confirm(Window owner, Theme theme, String header, String message) {
        Dialog<ButtonType> dialog = create(owner, theme, "Confirm", header);
        Label label = new Label(message);
        label.setWrapText(true);
        label.setMaxWidth(TEXT_WIDTH);
        dialog.getDialogPane().setContent(label);
        ButtonType yes = new ButtonType("Reset", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(yes, ButtonType.CANCEL);
        dialog.setResultConverter(button -> button);
        return dialog.showAndWait().filter(b -> b == yes).isPresent();
    }

    private static void info(Window owner, Theme theme, String title, String header, String text) {
        Dialog<ButtonType> dialog = create(owner, theme, title, header);
        Label label = new Label(text);
        label.setWrapText(true);
        label.setMaxWidth(TEXT_WIDTH);
        label.setMinHeight(Label.USE_PREF_SIZE);
        dialog.getDialogPane().setContent(label);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.OK);
        dialog.setResultConverter(button -> button);
        dialog.showAndWait();
    }

    private static <T> Dialog<T> create(Window owner, Theme theme, String title, String header) {
        Dialog<T> dialog = new Dialog<>();
        dialog.initOwner(owner);
        dialog.setTitle(title);
        dialog.setHeaderText(header);
        DialogPane pane = dialog.getDialogPane();
        pane.getStylesheets().add(MainWindow.stylesheet());
        pane.setStyle(theme.toCss());
        pane.getStyleClass().add("ms-dialog");
        return dialog;
    }

    private static GridPane formGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(GRID_HGAP);
        grid.setVgap(GRID_VGAP);
        grid.setPadding(new Insets(8, 12, 8, 12));
        return grid;
    }

    private static Spinner<Integer> spinner(int min, int max, int value) {
        Spinner<Integer> spinner = new Spinner<>(min, max, Math.max(min, Math.min(max, value)));
        spinner.setEditable(true);
        spinner.setPrefWidth(SPINNER_WIDTH);
        spinner.focusedProperty().addListener((obs, was, is) -> {
            if (!is) {
                commit(spinner);
            }
        });
        return spinner;
    }

    /** Applies typed text to a spinner's value, reverting anything that is not a valid number. */
    private static void commit(Spinner<Integer> spinner) {
        SpinnerValueFactory.IntegerSpinnerValueFactory factory =
                (SpinnerValueFactory.IntegerSpinnerValueFactory) spinner.getValueFactory();
        try {
            int typed = Integer.parseInt(spinner.getEditor().getText().trim());
            factory.setValue(Math.max(factory.getMin(), Math.min(factory.getMax(), typed)));
        } catch (NumberFormatException e) {
            // keep the previous value
        }
        spinner.getEditor().setText(Integer.toString(factory.getValue()));
    }

    private static <N extends Node> N styled(N node, String styleClass) {
        node.getStyleClass().add(styleClass);
        return node;
    }

    /** A short burst of falling confetti behind the win dialog's content. */
    private static final class Confetti extends Canvas {
        private static final int PIECES = 80;
        private static final long DURATION_NS = 2_600_000_000L;
        private static final long FADE_NS = 700_000_000L;
        private static final double GRAVITY = 260;
        private static final double NANOS_PER_SECOND = 1e9;
        private static final double PIECE_WIDTH = 6;
        private static final double PIECE_HEIGHT = 11;
        private static final Color[] COLORS = {
                Color.web("#ff5252"), Color.web("#ffd740"), Color.web("#69f0ae"),
                Color.web("#40c4ff"), Color.web("#e040fb"), Color.web("#ffab40")};

        private final double[] x = new double[PIECES];
        private final double[] y = new double[PIECES];
        private final double[] vx = new double[PIECES];
        private final double[] vy = new double[PIECES];
        private final double[] angle = new double[PIECES];
        private final double[] spin = new double[PIECES];
        private final Color[] color = new Color[PIECES];
        private final Random random = new Random();
        private long start;
        private long last;

        private final AnimationTimer timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (start == 0) {
                    start = now;
                    last = now;
                }
                step((now - last) / NANOS_PER_SECOND);
                last = now;
                long age = now - start;
                double alpha = age < DURATION_NS - FADE_NS ? 1.0
                        : Math.max(0, (DURATION_NS - age) / (double) FADE_NS);
                render(alpha);
                if (age > DURATION_NS) {
                    Confetti.this.stop();
                }
            }
        };

        void play() {
            double w = getWidth();
            for (int i = 0; i < PIECES; i++) {
                x[i] = random.nextDouble() * w;
                y[i] = -random.nextDouble() * getHeight() * 0.6;
                vx[i] = (random.nextDouble() - 0.5) * 80;
                vy[i] = random.nextDouble() * 60;
                angle[i] = random.nextDouble() * 360;
                spin[i] = (random.nextDouble() - 0.5) * 540;
                color[i] = COLORS[random.nextInt(COLORS.length)];
            }
            start = 0;
            timer.start();
        }

        void stop() {
            timer.stop();
            getGraphicsContext2D().clearRect(0, 0, getWidth(), getHeight());
        }

        private void step(double dt) {
            for (int i = 0; i < PIECES; i++) {
                vy[i] += GRAVITY * dt;
                x[i] += vx[i] * dt;
                y[i] += vy[i] * dt;
                angle[i] += spin[i] * dt;
            }
        }

        private void render(double alpha) {
            GraphicsContext gc = getGraphicsContext2D();
            gc.clearRect(0, 0, getWidth(), getHeight());
            gc.setGlobalAlpha(alpha);
            for (int i = 0; i < PIECES; i++) {
                gc.save();
                gc.translate(x[i], y[i]);
                gc.rotate(angle[i]);
                gc.setFill(color[i]);
                gc.fillRect(-PIECE_WIDTH / 2, -PIECE_HEIGHT / 2, PIECE_WIDTH, PIECE_HEIGHT);
                gc.restore();
            }
            gc.setGlobalAlpha(1.0);
        }
    }
}

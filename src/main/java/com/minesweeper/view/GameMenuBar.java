package com.minesweeper.view;

import com.minesweeper.model.Difficulty;
import javafx.geometry.Pos;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

/**
 * The application menu: Game, Options and Help.
 *
 * <p>Items are {@link CustomMenuItem}s so each one can carry a tooltip, a right-aligned hint
 * (shortcut or board size) and a check mark that is refreshed every time a menu opens.
 */
public final class GameMenuBar extends MenuBar {

    /** Small, medium and large cell sizes offered in Options. */
    public static final int CELL_SMALL = 24;
    public static final int CELL_MEDIUM = 32;
    public static final int CELL_LARGE = 40;

    private static final double ROW_MIN_WIDTH = 230;
    private static final double CHECK_COLUMN_WIDTH = 16;
    private static final double ROW_SPACING = 6;
    private static final Duration TOOLTIP_DELAY = Duration.millis(500);
    private static final String CHECK_MARK = "✓";

    /** Commands the menu can trigger. Implemented by the controller. */
    public interface Actions {
        void newGame();

        void selectDifficulty(Difficulty difficulty);

        void customDifficulty();

        void showStatistics();

        void showBestTimes();

        void exit();

        void selectTheme(Theme theme);

        void toggleQuestionMarks();

        void toggleSound();

        void selectCellSize(int pixels);

        void showHowToPlay();

        void showAbout();
    }

    /** Current values used to draw check marks. Implemented by the controller. */
    public interface State {
        Difficulty currentDifficulty();

        Theme currentTheme();

        boolean questionMarksEnabled();

        boolean soundEnabled();

        int preferredCellSize();
    }

    private final List<Runnable> refreshers = new ArrayList<>();

    /**
     * @param actions command handler
     * @param state   current settings for check marks
     */
    public GameMenuBar(Actions actions, State state) {
        getMenus().addAll(buildGameMenu(actions, state), buildOptionsMenu(actions, state), buildHelpMenu(actions));
    }

    private Menu buildGameMenu(Actions actions, State state) {
        Menu difficulty = new Menu("Difficulty");
        for (Difficulty d : Difficulty.values()) {
            if (!d.isPreset()) {
                continue;
            }
            String size = d.getCols() + "×" + d.getRows() + " · " + d.getMines();
            difficulty.getItems().add(item(d.getDisplayName(), size,
                    d.getDisplayName() + ": " + d.getCols() + " × " + d.getRows() + " board with "
                            + d.getMines() + " mines",
                    () -> state.currentDifficulty() == d, () -> actions.selectDifficulty(d)));
        }
        Menu game = new Menu("_Game");
        game.getItems().addAll(
                item("New Game", "F2", "Start a fresh board at the current difficulty", null, actions::newGame),
                new SeparatorMenuItem(),
                difficulty,
                item("Custom…", null, "Choose your own board size and mine count",
                        () -> state.currentDifficulty() == Difficulty.CUSTOM, actions::customDifficulty),
                new SeparatorMenuItem(),
                item("Statistics…", null, "Games played, win rate and streaks", null, actions::showStatistics),
                item("Best Times…", null, "Top five times for each difficulty", null, actions::showBestTimes),
                new SeparatorMenuItem(),
                item("Exit", null, "Save and close Minesweeper", null, actions::exit));
        watch(game, difficulty);
        return game;
    }

    private Menu buildOptionsMenu(Actions actions, State state) {
        Menu theme = new Menu("Theme");
        for (Theme t : Theme.values()) {
            theme.getItems().add(item(t.getDisplayName(), null, "Switch to the " + t.getDisplayName().toLowerCase()
                    + " colour theme", () -> state.currentTheme() == t, () -> actions.selectTheme(t)));
        }
        Menu cellSize = new Menu("Cell Size");
        int[] sizes = {CELL_SMALL, CELL_MEDIUM, CELL_LARGE};
        String[] names = {"Small", "Medium", "Large"};
        for (int i = 0; i < sizes.length; i++) {
            int px = sizes[i];
            cellSize.getItems().add(item(names[i], px + " px", "Draw each cell " + px + " pixels wide",
                    () -> state.preferredCellSize() == px, () -> actions.selectCellSize(px)));
        }
        Menu options = new Menu("_Options");
        options.getItems().addAll(
                theme,
                new SeparatorMenuItem(),
                item("Question Marks", null, "Include \"?\" in the right-click cycle (hidden → flag → ? → hidden)",
                        state::questionMarksEnabled, actions::toggleQuestionMarks),
                item("Sound Effects", null, "Play short sounds for reveals, flags, explosions and wins",
                        state::soundEnabled, actions::toggleSound),
                new SeparatorMenuItem(),
                cellSize);
        watch(options, theme, cellSize);
        return options;
    }

    private Menu buildHelpMenu(Actions actions) {
        Menu help = new Menu("_Help");
        help.getItems().addAll(
                item("How to Play", "F1", "Rules, mouse controls and keyboard shortcuts", null, actions::showHowToPlay),
                new SeparatorMenuItem(),
                item("About", null, "Version and credits", null, actions::showAbout));
        return help;
    }

    /**
     * Builds one menu row.
     *
     * @param text    label
     * @param hint    right-aligned hint (shortcut or detail), may be {@code null}
     * @param tooltip tooltip text
     * @param checked supplies the check-mark state, or {@code null} for a plain command
     * @param action  what the item does
     */
    private MenuItem item(String text, String hint, String tooltip, BooleanSupplier checked, Runnable action) {
        Label check = new Label();
        check.getStyleClass().add("menu-check");
        check.setMinWidth(CHECK_COLUMN_WIDTH);
        Label label = new Label(text);
        label.getStyleClass().add("menu-text");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label hintLabel = new Label(hint == null ? "" : hint);
        hintLabel.getStyleClass().add("menu-hint");

        HBox row = new HBox(ROW_SPACING, check, label, spacer, hintLabel);
        row.getStyleClass().add("menu-row");
        row.setAlignment(Pos.CENTER_LEFT);
        row.setMinWidth(ROW_MIN_WIDTH);
        Tooltip tip = new Tooltip(tooltip);
        tip.setShowDelay(TOOLTIP_DELAY);
        Tooltip.install(row, tip);

        CustomMenuItem item = new CustomMenuItem(row, true);
        item.setOnAction(e -> action.run());
        if (checked != null) {
            refreshers.add(() -> check.setText(checked.getAsBoolean() ? CHECK_MARK : ""));
        }
        return item;
    }

    private void watch(Menu... menus) {
        for (Menu m : menus) {
            m.setOnShowing(e -> refresh());
        }
    }

    /** Updates every check mark from the current state. */
    public void refresh() {
        refreshers.forEach(Runnable::run);
    }
}

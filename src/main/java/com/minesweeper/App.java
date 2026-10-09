package com.minesweeper;

import com.minesweeper.controller.GameController;
import com.minesweeper.util.SettingsManager;
import javafx.application.Application;
import javafx.stage.Stage;

/**
 * JavaFX application entry point.
 */
public final class App extends Application {

    @Override
    public void start(Stage stage) {
        new GameController(stage, new SettingsManager()).show();
    }

    /**
     * Launches the game.
     *
     * @param args command-line arguments (unused)
     */
    public static void main(String[] args) {
        launch(args);
    }
}

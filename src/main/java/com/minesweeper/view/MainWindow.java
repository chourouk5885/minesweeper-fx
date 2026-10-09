package com.minesweeper.view;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.net.URL;
import java.util.Objects;

/**
 * Assembles the window: menu bar on top, then the header bar and the framed board.
 * The window is sized to fit the board and is resized whenever the board changes.
 */
public final class MainWindow {

    /** Window title. */
    public static final String TITLE = "Minesweeper";

    private static final String STYLESHEET = "styles.css";
    private static final double CONTENT_PADDING = 14;
    private static final double CONTENT_SPACING = 12;
    private static final double ICON_SIZE = 64;
    private static final double ICON_CORNER = 16;

    private final Stage stage;
    private final BorderPane root;
    private final Scene scene;

    /**
     * @param stage     the primary stage
     * @param menuBar   application menu
     * @param header    mine counter / face / timer bar
     * @param boardView the minefield
     */
    public MainWindow(Stage stage, GameMenuBar menuBar, HeaderBar header, BoardView boardView) {
        this.stage = stage;

        StackPane boardFrame = new StackPane(boardView);
        boardFrame.getStyleClass().add("board-frame");

        VBox content = new VBox(CONTENT_SPACING, header, boardFrame);
        content.getStyleClass().add("content");
        content.setPadding(new Insets(CONTENT_PADDING));

        root = new BorderPane(content);
        root.setTop(menuBar);

        scene = new Scene(root);
        scene.getStylesheets().add(stylesheet());

        stage.setTitle(TITLE);
        stage.setScene(scene);
        stage.setResizable(false);
        stage.getIcons().add(createIcon());
    }

    /** @return URL of the application stylesheet, for scenes and dialogs */
    public static String stylesheet() {
        URL url = Objects.requireNonNull(MainWindow.class.getResource(STYLESHEET), "styles.css missing");
        return url.toExternalForm();
    }

    /** @param theme theme to apply to the whole window */
    public void applyTheme(Theme theme) {
        root.setStyle(theme.toCss());
    }

    /** Resizes the window to fit its content (after a board or cell-size change). */
    public void fitToContent() {
        stage.sizeToScene();
    }

    /** @return the window's scene */
    public Scene getScene() {
        return scene;
    }

    /** @return the primary stage */
    public Stage getStage() {
        return stage;
    }

    private static Image createIcon() {
        Canvas canvas = new Canvas(ICON_SIZE, ICON_SIZE);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setFill(Color.web("#2f6fed"));
        gc.fillRoundRect(0, 0, ICON_SIZE, ICON_SIZE, ICON_CORNER, ICON_CORNER);
        CellView.paintMine(gc, Color.WHITE, Color.web("#2f6fed"), ICON_SIZE / 2, ICON_SIZE / 2, ICON_SIZE * 1.1);
        SnapshotParameters params = new SnapshotParameters();
        params.setFill(Color.TRANSPARENT);
        return canvas.snapshot(params, null);
    }
}

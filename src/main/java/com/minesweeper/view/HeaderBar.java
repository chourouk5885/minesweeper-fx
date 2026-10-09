package com.minesweeper.view;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.util.Duration;

/**
 * The bar above the board: mine counter (left), status face / restart button (centre) and
 * timer (right), in the classic layout with a modern finish.
 */
public final class HeaderBar extends HBox {

    private static final double FACE_SIZE = 30;
    private static final int LED_MAX = 999;
    private static final int LED_MIN = -99;
    private static final Duration TOOLTIP_DELAY = Duration.millis(400);

    private final Label mineCounter = new Label();
    private final Label timer = new Label();
    private final FaceIcon face = new FaceIcon(FACE_SIZE);
    private final Button faceButton = new Button();

    /** Builds the header. */
    public HeaderBar() {
        getStyleClass().add("header-bar");
        setAlignment(Pos.CENTER);

        mineCounter.getStyleClass().add("led");
        timer.getStyleClass().add("led");
        mineCounter.setTooltip(tooltip("Mines left (total mines minus flags placed)"));
        timer.setTooltip(tooltip("Seconds elapsed. Starts on your first click."));

        faceButton.getStyleClass().add("face-button");
        faceButton.setGraphic(face);
        faceButton.setFocusTraversable(false);
        faceButton.setTooltip(tooltip("New game (F2)"));

        Region leftSpacer = new Region();
        Region rightSpacer = new Region();
        HBox.setHgrow(leftSpacer, Priority.ALWAYS);
        HBox.setHgrow(rightSpacer, Priority.ALWAYS);
        getChildren().addAll(mineCounter, leftSpacer, faceButton, rightSpacer, timer);

        setMineCount(0);
        setSeconds(0);
    }

    /** @param count value for the mine counter; may be negative */
    public void setMineCount(int count) {
        mineCounter.setText(formatLed(count));
    }

    /** @param seconds elapsed seconds (capped at 999 for display) */
    public void setSeconds(int seconds) {
        timer.setText(formatLed(seconds));
    }

    /** @param mood expression for the face */
    public void setMood(FaceIcon.Mood mood) {
        face.setMood(mood);
    }

    /** @param action called when the face is clicked */
    public void setOnRestart(Runnable action) {
        faceButton.setOnAction(e -> action.run());
    }

    private static String formatLed(int value) {
        int clamped = Math.max(LED_MIN, Math.min(LED_MAX, value));
        return clamped < 0 ? String.format("-%02d", -clamped) : String.format("%03d", clamped);
    }

    private static Tooltip tooltip(String text) {
        Tooltip t = new Tooltip(text);
        t.setShowDelay(TOOLTIP_DELAY);
        return t;
    }
}

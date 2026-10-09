package com.minesweeper.view;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.StrokeLineCap;

/**
 * The status face on the restart button, drawn as vector graphics so it looks the same on
 * every platform (emoji rendering in JavaFX varies a lot between operating systems).
 */
public final class FaceIcon extends Canvas {

    /** Expressions the face can show. */
    public enum Mood {
        /** Normal play. */
        HAPPY,
        /** Mouse button held on the board. */
        SURPRISED,
        /** Game won. */
        COOL,
        /** Game lost. */
        DEAD
    }

    private static final Color SKIN = Color.web("#ffd54f");
    private static final Color OUTLINE = Color.web("#c79100");
    private static final Color FEATURES = Color.web("#3e2723");
    private static final Color GLASSES = Color.web("#212121");
    private static final double OUTLINE_RATIO = 0.06;
    private static final double LINE_RATIO = 0.07;
    private static final double EYE_LEFT = 0.36;
    private static final double EYE_RIGHT = 0.64;
    private static final double EYE_Y = 0.4;
    private static final double EYE_RADIUS = 0.065;
    private static final double SMILE_START_ANGLE = 200;
    private static final double FROWN_START_ANGLE = 20;
    private static final double MOUTH_EXTENT = 140;

    private Mood mood = Mood.HAPPY;

    /**
     * @param size edge length in pixels
     */
    public FaceIcon(double size) {
        super(size, size);
        draw();
    }

    /**
     * @param size edge length in pixels
     * @param mood initial expression
     */
    public FaceIcon(double size, Mood mood) {
        super(size, size);
        this.mood = mood;
        draw();
    }

    /** @param mood the expression to show */
    public void setMood(Mood mood) {
        if (this.mood != mood) {
            this.mood = mood;
            draw();
        }
    }

    /** @return the expression currently shown */
    public Mood getMood() {
        return mood;
    }

    private void draw() {
        double s = getWidth();
        GraphicsContext gc = getGraphicsContext2D();
        gc.clearRect(0, 0, s, s);

        double outline = s * OUTLINE_RATIO;
        gc.setFill(SKIN);
        gc.fillOval(outline / 2, outline / 2, s - outline, s - outline);
        gc.setStroke(OUTLINE);
        gc.setLineWidth(outline);
        gc.strokeOval(outline / 2, outline / 2, s - outline, s - outline);

        gc.setStroke(FEATURES);
        gc.setFill(FEATURES);
        gc.setLineWidth(s * LINE_RATIO);
        gc.setLineCap(StrokeLineCap.ROUND);

        switch (mood) {
            case HAPPY -> {
                eyes(gc, s);
                gc.strokeArc(s * 0.3, s * 0.36, s * 0.4, s * 0.32, SMILE_START_ANGLE, MOUTH_EXTENT, ArcType.OPEN);
            }
            case SURPRISED -> {
                eyes(gc, s);
                gc.fillOval(s * 0.42, s * 0.58, s * 0.16, s * 0.2);
            }
            case COOL -> {
                gc.setFill(GLASSES);
                gc.fillRoundRect(s * 0.22, s * 0.32, s * 0.24, s * 0.15, s * 0.08, s * 0.08);
                gc.fillRoundRect(s * 0.54, s * 0.32, s * 0.24, s * 0.15, s * 0.08, s * 0.08);
                gc.setStroke(GLASSES);
                gc.strokeLine(s * 0.44, s * 0.36, s * 0.56, s * 0.36);
                gc.setStroke(FEATURES);
                gc.strokeArc(s * 0.3, s * 0.38, s * 0.4, s * 0.3, SMILE_START_ANGLE, MOUTH_EXTENT, ArcType.OPEN);
            }
            case DEAD -> {
                double d = s * 0.06;
                for (double ex : new double[]{EYE_LEFT, EYE_RIGHT}) {
                    double cx = s * ex;
                    double cy = s * EYE_Y;
                    gc.strokeLine(cx - d, cy - d, cx + d, cy + d);
                    gc.strokeLine(cx + d, cy - d, cx - d, cy + d);
                }
                gc.strokeArc(s * 0.32, s * 0.6, s * 0.36, s * 0.24, FROWN_START_ANGLE, MOUTH_EXTENT, ArcType.OPEN);
            }
            default -> throw new IllegalStateException("Unknown mood " + mood);
        }
    }

    private static void eyes(GraphicsContext gc, double s) {
        double r = s * EYE_RADIUS;
        gc.fillOval(s * EYE_LEFT - r, s * EYE_Y - r, r * 2, r * 2);
        gc.fillOval(s * EYE_RIGHT - r, s * EYE_Y - r, r * 2, r * 2);
    }
}

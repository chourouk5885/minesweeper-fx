package com.minesweeper.view;

import com.minesweeper.model.Cell;
import com.minesweeper.model.CellMark;
import javafx.geometry.VPos;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

/**
 * Paints a single cell onto the board canvas.
 *
 * <p>The board is drawn on one {@link javafx.scene.canvas.Canvas} rather than as hundreds of
 * scene-graph nodes; this class is the "view" of one cell within that canvas. Everything is
 * vector-drawn, so it stays crisp at any cell size.
 */
public final class CellView {

    /** Preferred font for numbers; JavaFX falls back to the system font if it is missing. */
    public static final String NUMBER_FONT_FAMILY = "Segoe UI";

    private static final double GAP = 1.0;
    private static final double CORNER_RATIO = 0.2;
    private static final double GLYPH_RATIO = 0.6;
    private static final double GLYPH_BASELINE_NUDGE = 0.03;
    private static final double MIN_REVEAL_SCALE = 0.7;
    private static final double FOCUS_STROKE = 2.0;
    private static final double FOCUS_INSET = 2.0;
    private static final double MINE_RADIUS_RATIO = 0.21;
    private static final double MINE_SPIKE_RATIO = 1.55;
    private static final double MINE_SPIKE_WIDTH_RATIO = 0.07;
    private static final double MINE_HIGHLIGHT_RATIO = 0.32;
    private static final int MINE_SPIKES = 8;
    private static final double FLAG_POLE_X = 0.58;
    private static final double FLAG_TOP = 0.2;
    private static final double FLAG_MID = 0.36;
    private static final double FLAG_BOTTOM = 0.52;
    private static final double FLAG_TIP_X = 0.24;
    private static final double POLE_BOTTOM = 0.74;
    private static final double BASE_LEFT = 0.34;
    private static final double BASE_RIGHT = 0.8;
    private static final double BASE_HEIGHT = 0.07;
    private static final double LINE_RATIO = 0.07;
    private static final double CROSS_INSET = 0.2;

    private static Font cachedFont;
    private static double cachedFontSize = -1;

    /**
     * Per-frame rendering flags for one cell.
     *
     * @param hovered        the mouse is over the cell and the game accepts input
     * @param pressed        draw the cell depressed (mouse held / chord preview)
     * @param focused        draw the keyboard focus ring
     * @param revealProgress reveal animation progress: below 0 = not started yet (draw hidden),
     *                       0..1 = animating, 1 = done
     * @param wrongFlag      the game is lost and this flag was wrong
     */
    public record RenderState(boolean hovered, boolean pressed, boolean focused,
                              double revealProgress, boolean wrongFlag) {
    }

    private CellView() {
    }

    /**
     * Paints one cell.
     *
     * @param gc    target graphics context
     * @param theme active theme
     * @param cell  the cell's model
     * @param x     left edge in canvas pixels
     * @param y     top edge in canvas pixels
     * @param size  cell edge length in pixels
     * @param state per-frame flags
     */
    public static void paint(GraphicsContext gc, Theme theme, Cell cell,
                             double x, double y, double size, RenderState state) {
        boolean showRevealed = cell.isRevealed() && state.revealProgress() >= 0;
        if (!showRevealed) {
            if (state.pressed()) {
                paintRecessed(gc, theme, x, y, size);
            } else {
                paintRaised(gc, theme, x, y, size, state.hovered());
            }
            paintMark(gc, theme, cell.getMark(), x, y, size, state.wrongFlag());
        } else {
            double p = Math.min(1.0, state.revealProgress());
            if (p < 1.0) {
                gc.save();
                gc.setGlobalAlpha(1.0 - p);
                paintRaised(gc, theme, x, y, size, false);
                gc.restore();
            }
            gc.save();
            gc.setGlobalAlpha(p);
            double scale = MIN_REVEAL_SCALE + (1.0 - MIN_REVEAL_SCALE) * p;
            double cx = x + size / 2;
            double cy = y + size / 2;
            gc.translate(cx, cy);
            gc.scale(scale, scale);
            gc.translate(-cx, -cy);
            paintRevealed(gc, theme, cell, x, y, size);
            gc.restore();
        }
        if (state.focused()) {
            gc.setStroke(theme.focusRing);
            gc.setLineWidth(FOCUS_STROKE);
            double corner = size * CORNER_RATIO;
            gc.strokeRoundRect(x + FOCUS_INSET, y + FOCUS_INSET,
                    size - 2 * FOCUS_INSET, size - 2 * FOCUS_INSET, corner, corner);
        }
    }

    /**
     * Draws a mine centred on a point. Also used for the window icon.
     *
     * @param gc   target graphics context
     * @param body mine colour
     * @param shine highlight colour
     * @param cx   centre x
     * @param cy   centre y
     * @param size reference size (cell edge length)
     */
    public static void paintMine(GraphicsContext gc, Color body, Color shine, double cx, double cy, double size) {
        double r = size * MINE_RADIUS_RATIO;
        double spike = r * MINE_SPIKE_RATIO;
        gc.setStroke(body);
        gc.setLineWidth(Math.max(1, size * MINE_SPIKE_WIDTH_RATIO));
        gc.setLineCap(StrokeLineCap.ROUND);
        for (int i = 0; i < MINE_SPIKES; i++) {
            double angle = Math.PI * 2 * i / MINE_SPIKES;
            gc.strokeLine(cx, cy, cx + Math.cos(angle) * spike, cy + Math.sin(angle) * spike);
        }
        gc.setFill(body);
        gc.fillOval(cx - r, cy - r, r * 2, r * 2);
        double h = r * MINE_HIGHLIGHT_RATIO;
        gc.setFill(shine);
        gc.fillOval(cx - r * 0.5 - h / 2, cy - r * 0.5 - h / 2, h, h);
    }

    // ------------------------------------------------------------------ pieces

    private static void paintRaised(GraphicsContext gc, Theme theme, double x, double y, double size,
                                    boolean hovered) {
        double s = size - 2 * GAP;
        double corner = size * CORNER_RATIO;
        gc.setFill(hovered ? theme.hoverFill : theme.hiddenFill);
        gc.fillRoundRect(x + GAP, y + GAP, s, s, corner, corner);
        gc.setLineWidth(1);
        gc.setStroke(theme.bevelDark);
        gc.strokeRoundRect(x + GAP + 0.5, y + GAP + 0.5, s - 1, s - 1, corner, corner);
        gc.setStroke(theme.bevelLight);
        gc.setLineCap(StrokeLineCap.ROUND);
        gc.strokeLine(x + GAP + corner / 2, y + GAP + 1.5, x + GAP + s - corner / 2, y + GAP + 1.5);
    }

    private static void paintRecessed(GraphicsContext gc, Theme theme, double x, double y, double size) {
        double s = size - 2 * GAP;
        double corner = size * CORNER_RATIO;
        gc.setFill(theme.pressedFill);
        gc.fillRoundRect(x + GAP, y + GAP, s, s, corner, corner);
        gc.setLineWidth(1);
        gc.setStroke(theme.revealedBorder);
        gc.strokeRoundRect(x + GAP + 0.5, y + GAP + 0.5, s - 1, s - 1, corner, corner);
    }

    private static void paintRevealed(GraphicsContext gc, Theme theme, Cell cell, double x, double y, double size) {
        gc.setFill(cell.isExploded() ? theme.explodedFill : theme.revealedFill);
        gc.fillRect(x, y, size, size);
        gc.setLineWidth(1);
        gc.setStroke(theme.revealedBorder);
        gc.strokeRect(x + 0.5, y + 0.5, size - 1, size - 1);
        if (cell.isMine()) {
            Color body = cell.isExploded() ? Color.web("#1a1a1a") : theme.mineBody;
            paintMine(gc, body, theme.mineHighlight, x + size / 2, y + size / 2, size);
        } else if (cell.getAdjacentMines() > 0) {
            paintGlyph(gc, Integer.toString(cell.getAdjacentMines()),
                    theme.numberColor(cell.getAdjacentMines()), x, y, size);
        }
    }

    private static void paintMark(GraphicsContext gc, Theme theme, CellMark mark, double x, double y,
                                  double size, boolean wrongFlag) {
        if (mark == CellMark.FLAG) {
            paintFlag(gc, theme, x, y, size);
            if (wrongFlag) {
                paintCross(gc, theme.wrongMark, x, y, size);
            }
        } else if (mark == CellMark.QUESTION) {
            paintGlyph(gc, "?", theme.questionMark, x, y, size);
        }
    }

    private static void paintFlag(GraphicsContext gc, Theme theme, double x, double y, double size) {
        double poleX = x + size * FLAG_POLE_X;
        gc.setStroke(theme.flagPole);
        gc.setLineWidth(Math.max(1, size * LINE_RATIO));
        gc.setLineCap(StrokeLineCap.ROUND);
        gc.strokeLine(poleX, y + size * FLAG_TOP, poleX, y + size * POLE_BOTTOM);
        gc.setFill(theme.flag);
        gc.fillPolygon(
                new double[]{poleX, poleX, x + size * FLAG_TIP_X},
                new double[]{y + size * FLAG_TOP, y + size * FLAG_BOTTOM, y + size * FLAG_MID},
                3);
        gc.setFill(theme.flagPole);
        double baseHeight = Math.max(2, size * BASE_HEIGHT);
        gc.fillRoundRect(x + size * BASE_LEFT, y + size * POLE_BOTTOM - baseHeight / 2,
                size * (BASE_RIGHT - BASE_LEFT), baseHeight, baseHeight, baseHeight);
    }

    private static void paintCross(GraphicsContext gc, Color color, double x, double y, double size) {
        double a = size * CROSS_INSET;
        double b = size * (1 - CROSS_INSET);
        gc.setStroke(color);
        gc.setLineWidth(Math.max(1.5, size * LINE_RATIO * 1.2));
        gc.setLineCap(StrokeLineCap.ROUND);
        gc.strokeLine(x + a, y + a, x + b, y + b);
        gc.strokeLine(x + b, y + a, x + a, y + b);
    }

    private static void paintGlyph(GraphicsContext gc, String text, Color color, double x, double y, double size) {
        gc.setFill(color);
        gc.setFont(fontFor(size));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(VPos.CENTER);
        gc.fillText(text, x + size / 2, y + size / 2 + size * GLYPH_BASELINE_NUDGE);
    }

    private static Font fontFor(double size) {
        if (size != cachedFontSize) {
            cachedFont = Font.font(NUMBER_FONT_FAMILY, FontWeight.BOLD, size * GLYPH_RATIO);
            cachedFontSize = size;
        }
        return cachedFont;
    }
}

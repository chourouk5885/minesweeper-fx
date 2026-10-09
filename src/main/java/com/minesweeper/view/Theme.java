package com.minesweeper.view;

import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Paint;
import javafx.scene.paint.Stop;

import java.util.Locale;

/**
 * Every colour used by the game, for both themes, lives here.
 *
 * <p>Board colours are used directly by the canvas renderer; window and dialog colours are
 * exported as CSS "looked-up colours" through {@link #toCss()} and referenced by
 * {@code styles.css}, so switching theme is a single {@code setStyle} call.
 */
public enum Theme {
    LIGHT("Light"),
    DARK("Dark");

    private static final int MAX_NUMBER = 8;
    private static final double CHANNEL_MAX = 255.0;

    private final String displayName;

    // ---- window chrome
    final Color windowBackground;
    final Color panelBackground;
    final Color divider;
    final Color text;
    final Color mutedText;
    final Color accent;
    final Color inputBackground;
    final Color controlBase;
    final Color ledBackground;
    final Color ledText;
    final Color faceBackground;
    final Color faceHover;

    // ---- board
    final Paint hiddenFill;
    final Paint hoverFill;
    final Color bevelLight;
    final Color bevelDark;
    final Color pressedFill;
    final Color revealedFill;
    final Color revealedBorder;
    final Color explodedFill;
    final Color mineBody;
    final Color mineHighlight;
    final Color flag;
    final Color flagPole;
    final Color wrongMark;
    final Color questionMark;
    final Color focusRing;
    private final Color[] numberColors;

    Theme(String displayName) {
        this.displayName = displayName;
        boolean dark = "Dark".equals(displayName);

        windowBackground = dark ? Color.web("#16191f") : Color.web("#eef1f5");
        panelBackground = dark ? Color.web("#22262e") : Color.web("#ffffff");
        divider = dark ? Color.web("#333944") : Color.web("#d7dce3");
        text = dark ? Color.web("#e7e9ec") : Color.web("#1f2933");
        mutedText = dark ? Color.web("#9aa3ae") : Color.web("#5f6b7a");
        accent = dark ? Color.web("#5b8cff") : Color.web("#2f6fed");
        inputBackground = dark ? Color.web("#1b1f26") : Color.web("#ffffff");
        controlBase = dark ? Color.web("#353b47") : Color.web("#e9edf2");
        ledBackground = dark ? Color.web("#0c0e12") : Color.web("#1b1b1f");
        ledText = dark ? Color.web("#ff5a4f") : Color.web("#ff3b30");
        faceBackground = dark ? Color.web("#2d323c") : Color.web("#f3f5f8");
        faceHover = dark ? Color.web("#3a404c") : Color.web("#e6eaf0");

        hiddenFill = verticalGradient(
                dark ? Color.web("#4b5262") : Color.web("#e3e8ef"),
                dark ? Color.web("#3d4351") : Color.web("#c9d1dc"));
        hoverFill = verticalGradient(
                dark ? Color.web("#5a6275") : Color.web("#f0f4f9"),
                dark ? Color.web("#4a5161") : Color.web("#d8dfe9"));
        bevelLight = dark ? Color.web("#ffffff", 0.14) : Color.web("#ffffff", 0.95);
        bevelDark = dark ? Color.web("#000000", 0.45) : Color.web("#8e99a8", 0.65);
        pressedFill = dark ? Color.web("#2b2f38") : Color.web("#e4e8ee");
        revealedFill = dark ? Color.web("#2a2e37") : Color.web("#f7f8fa");
        revealedBorder = dark ? Color.web("#353a45") : Color.web("#e0e4ea");
        explodedFill = dark ? Color.web("#c0392b") : Color.web("#ff5c5c");
        mineBody = dark ? Color.web("#dfe3e8") : Color.web("#20242b");
        mineHighlight = dark ? Color.web("#ffffff", 0.9) : Color.web("#ffffff", 0.7);
        flag = dark ? Color.web("#ff5252") : Color.web("#e53935");
        flagPole = dark ? Color.web("#cfd6de") : Color.web("#37474f");
        wrongMark = dark ? Color.web("#ffb74d") : Color.web("#d32f2f");
        questionMark = dark ? Color.web("#c9ced6") : Color.web("#455a64");
        focusRing = dark ? Color.web("#7aa2ff") : Color.web("#2f6fed");

        numberColors = dark
                ? new Color[]{
                        Color.web("#6ea8ff"), Color.web("#5fd17a"), Color.web("#ff6b6b"), Color.web("#9fa8ff"),
                        Color.web("#e58a8a"), Color.web("#4fd1c5"), Color.web("#f1f1f1"), Color.web("#a3a9b1")}
                : new Color[]{
                        Color.web("#1f5bd6"), Color.web("#2e8b37"), Color.web("#d63031"), Color.web("#1a237e"),
                        Color.web("#8b1a1a"), Color.web("#00838f"), Color.web("#212121"), Color.web("#757575")};
    }

    /** @return name shown in menus */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * @param n a number between 1 and 8
     * @return the colour used to draw that number
     */
    public Color numberColor(int n) {
        return numberColors[Math.max(1, Math.min(MAX_NUMBER, n)) - 1];
    }

    /**
     * @param name a saved theme name
     * @return the matching theme, or {@link #LIGHT} if unknown
     */
    public static Theme fromName(String name) {
        for (Theme t : values()) {
            if (t.name().equalsIgnoreCase(name)) {
                return t;
            }
        }
        return LIGHT;
    }

    /**
     * Inline CSS that defines this theme's looked-up colours and tunes Modena's base colours.
     *
     * @return a style string for {@code Node.setStyle}
     */
    public String toCss() {
        return "-fx-base: " + css(controlBase) + ";"
                + "-fx-background: " + css(windowBackground) + ";"
                + "-fx-control-inner-background: " + css(inputBackground) + ";"
                + "-fx-accent: " + css(accent) + ";"
                + "-fx-focus-color: " + css(accent) + ";"
                + "-fx-faint-focus-color: transparent;"
                + "-ms-window-bg: " + css(windowBackground) + ";"
                + "-ms-panel-bg: " + css(panelBackground) + ";"
                + "-ms-divider: " + css(divider) + ";"
                + "-ms-text: " + css(text) + ";"
                + "-ms-muted: " + css(mutedText) + ";"
                + "-ms-accent: " + css(accent) + ";"
                + "-ms-led-bg: " + css(ledBackground) + ";"
                + "-ms-led-text: " + css(ledText) + ";"
                + "-ms-face-bg: " + css(faceBackground) + ";"
                + "-ms-face-hover: " + css(faceHover) + ";";
    }

    private static Paint verticalGradient(Color top, Color bottom) {
        // Proportional gradient: coordinates are relative to each shape, so one Paint serves every cell.
        return new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, top), new Stop(1, bottom));
    }

    private static String css(Color c) {
        return String.format(Locale.ROOT, "rgba(%d,%d,%d,%.3f)",
                Math.round(c.getRed() * CHANNEL_MAX),
                Math.round(c.getGreen() * CHANNEL_MAX),
                Math.round(c.getBlue() * CHANNEL_MAX),
                c.getOpacity());
    }
}

package com.minesweeper;

import javafx.application.Application;

/**
 * Plain main class that does not extend {@link Application}.
 *
 * <p>Use this as the main class when running from an IDE without module-path setup or from a
 * single "fat" jar; launching an {@code Application} subclass directly from the classpath
 * fails with "JavaFX runtime components are missing".
 */
public final class Launcher {

    private Launcher() {
    }

    /**
     * @param args command-line arguments (unused)
     */
    public static void main(String[] args) {
        Application.launch(App.class, args);
    }
}

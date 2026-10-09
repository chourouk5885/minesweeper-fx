package com.minesweeper.model;

import java.util.List;

/**
 * Outcome of a reveal or chord action.
 *
 * @param revealed cells uncovered by the action, in flood-fill (BFS) order so the view can ripple them
 * @param hitMine  {@code true} if the action uncovered a mine and lost the game
 */
public record RevealResult(List<Position> revealed, boolean hitMine) {

    /** Result for an action that changed nothing. */
    public static final RevealResult NONE = new RevealResult(List.of(), false);

    /** Makes the list immutable. */
    public RevealResult {
        revealed = List.copyOf(revealed);
    }

    /** @return {@code true} if the action changed the board */
    public boolean changed() {
        return hitMine || !revealed.isEmpty();
    }
}

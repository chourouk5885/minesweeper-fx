package com.minesweeper.model;

/**
 * A single square on the board.
 *
 * <p>Cells are read-only outside the model package; only {@link Board} mutates them.
 */
public final class Cell {

    private boolean mine;
    private boolean revealed;
    private boolean exploded;
    private CellMark mark = CellMark.NONE;
    private int adjacentMines;

    /** @return {@code true} if this cell holds a mine */
    public boolean isMine() {
        return mine;
    }

    /** @return {@code true} if this cell has been uncovered */
    public boolean isRevealed() {
        return revealed;
    }

    /** @return {@code true} if this is the mine the player clicked to lose */
    public boolean isExploded() {
        return exploded;
    }

    /** @return the player's marker on this cell */
    public CellMark getMark() {
        return mark;
    }

    /** @return {@code true} if the player flagged this cell */
    public boolean isFlagged() {
        return mark == CellMark.FLAG;
    }

    /** @return number of mines in the eight surrounding cells */
    public int getAdjacentMines() {
        return adjacentMines;
    }

    void setMine(boolean mine) {
        this.mine = mine;
    }

    void setRevealed(boolean revealed) {
        this.revealed = revealed;
    }

    void setExploded(boolean exploded) {
        this.exploded = exploded;
    }

    void setMark(CellMark mark) {
        this.mark = mark;
    }

    void setAdjacentMines(int adjacentMines) {
        this.adjacentMines = adjacentMines;
    }
}

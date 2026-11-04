package Model;

/**
 * מיקום על הלוח: שורה ועמודה. אובייקט בלתי ניתן לשינוי.
 */
public final class Position {

    private final int row;
    private final int col;

    /**
     * @param row שורה, מ-0 (למעלה)
     * @param col עמודה, מ-0 (שמאל)
     */
    public Position(int row, int col) {
        this.row = row;
        this.col = col;
    }

    public int getRow() {
        return row;
    }

    public int getCol() {
        return col;
    }

    /** @return מיקום חדש שמוזז בהיסט הנתון */
    public Position plus(int dRow, int dCol) {
        return new Position(row + dRow, col + dCol);
    }

    /**
     * מסובב את המיקום ברבע סיבוב עם כיוון השעון בתוך לוח בגודל size.
     *
     * @param size גודל צד הלוח
     * @param quarterTurns מספר רבעי הסיבוב (0 עד 3)
     * @return המיקום המסובב
     */
    public Position rotate(int size, int quarterTurns) {
        int r = row;
        int c = col;
        for (int i = 0; i < quarterTurns; i++) {
            int newRow = c;
            int newCol = size - 1 - r;
            r = newRow;
            c = newCol;
        }
        return new Position(r, c);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Position)) {
            return false;
        }
        Position p = (Position) o;
        return row == p.row && col == p.col;
    }

    @Override
    public int hashCode() {
        return row * 31 + col;
    }

    @Override
    public String toString() {
        return "(" + row + "," + col + ")";
    }
}

package Model;

/**
 * מהלך חוקי אפשרי: איזה כלי זז, מאיפה ולאן במסלול שלו.
 */
public final class Move {

    private final Pawn pawn;
    private final int from;
    private final int to;
    private final boolean stoppedByBlock;

    /**
     * @param pawn            הכלי
     * @param from            המקום הנוכחי במסלול ({@link Pawn#IN_BASE} אם יוצא מהטירה)
     * @param to              המקום החדש במסלול
     * @param stoppedByBlock  true אם הכלי נעצר לפני Double Block ולא ניצל את כל הצעדים
     */
    public Move(Pawn pawn, int from, int to, boolean stoppedByBlock) {
        this.pawn = pawn;
        this.from = from;
        this.to = to;
        this.stoppedByBlock = stoppedByBlock;
    }

    public Pawn getPawn() {
        return pawn;
    }

    public int getFrom() {
        return from;
    }

    public int getTo() {
        return to;
    }

    public boolean isStoppedByBlock() {
        return stoppedByBlock;
    }

    @Override
    public String toString() {
        return pawn + ": " + from + "->" + to + (stoppedByBlock ? " (block)" : "");
    }
}

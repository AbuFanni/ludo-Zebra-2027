package Model;

/**
 * משבצת הגנה (כוכב). כלים שעומדים עליה לא נאכלים.
 * מבוסס על {@code Cells.StarCell} בפרויקט המקור; שם הכוכב הקפיץ את הכלי לכוכב הבא, וכאן הוא רק מגן.
 */
public class SafeSquare extends Square {

    public SafeSquare(Position position) {
        super(position, null);
    }

    @Override
    public SquareKind getKind() {
        return SquareKind.SAFE;
    }

    @Override
    public boolean isSafe() {
        return true;
    }
}

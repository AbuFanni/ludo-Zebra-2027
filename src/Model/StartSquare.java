package Model;

/**
 * משבצת ההתחלה (#) של שחקן: הכלי נכנס אליה מהטירה בתוצאה 6. המשבצת מוגנת.
 * מבוסס על {@code Cells.ColoredSimpleCell} בפרויקט המקור.
 */
public class StartSquare extends Square {

    public StartSquare(Position position, PlayerColor owner) {
        super(position, owner);
    }

    @Override
    public SquareKind getKind() {
        return SquareKind.START;
    }

    @Override
    public boolean isSafe() {
        return true;
    }
}

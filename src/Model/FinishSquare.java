package Model;

/**
 * בית הסיום של שחקן, במרכז הלוח. נכנסים אליו רק בתוצאה מדויקת.
 * מבוסס על {@code Cells.FinalCell} בפרויקט המקור.
 */
public class FinishSquare extends Square {

    public FinishSquare(Position position, PlayerColor owner) {
        super(position, owner);
    }

    @Override
    public SquareKind getKind() {
        return SquareKind.FINISH;
    }

    @Override
    public boolean isSafe() {
        return true;
    }

    @Override
    public boolean isPrivate() {
        return true;
    }
}

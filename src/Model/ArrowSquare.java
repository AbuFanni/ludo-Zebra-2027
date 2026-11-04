package Model;

/**
 * משבצת חץ בצבע שחקן. הכלים של השחקן הזה ממשיכים ממנה למסלול הפנימי הבא
 * (זה כבר חלק מהמסלול שלהם ב-{@link Board}). לשאר השחקנים היא משבצת רגילה לכל דבר.
 */
public class ArrowSquare extends Square {

    public ArrowSquare(Position position, PlayerColor owner) {
        super(position, owner);
    }

    @Override
    public SquareKind getKind() {
        return SquareKind.ARROW;
    }

    @Override
    public boolean isSafe() {
        return false;
    }
}

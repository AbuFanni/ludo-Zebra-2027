package Model;

/** משבצת רגילה במסלול. אפשר לאכול עליה כלי יחיד של יריב. */
public class RegularSquare extends Square {

    public RegularSquare(Position position) {
        super(position, null);
    }

    @Override
    public SquareKind getKind() {
        return SquareKind.REGULAR;
    }

    @Override
    public boolean isSafe() {
        return false;
    }
}

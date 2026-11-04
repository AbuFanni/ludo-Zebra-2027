package Model;

/** משבצת במסלול הבית של שחקן. רק הכלים שלו נכנסים אליה, ולכן אין עליה אכילה. */
public class HomePathSquare extends Square {

    public HomePathSquare(Position position, PlayerColor owner) {
        super(position, owner);
    }

    @Override
    public SquareKind getKind() {
        return SquareKind.HOME_PATH;
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

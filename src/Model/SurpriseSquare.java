package Model;

/**
 * משבצת הפתעה. היא גם משבצת הגנה.
 * באיטרציה 1 היא מוצגת ומגינה; הגרלת ההפתעה מתווספת באיטרציה 2.
 */
public class SurpriseSquare extends Square {

    public SurpriseSquare(Position position) {
        super(position, null);
    }

    @Override
    public SquareKind getKind() {
        return SquareKind.SURPRISE;
    }

    @Override
    public boolean isSafe() {
        return true;
    }
}

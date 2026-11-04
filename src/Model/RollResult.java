package Model;

/**
 * תוצאה של הטלת קובייה בתור.
 */
public final class RollResult {

    private final Player player;
    private final int value;
    private final boolean cancelledThirdSix;
    private final boolean hasMoves;
    private final boolean rollAgain;

    RollResult(Player player, int value, boolean cancelledThirdSix, boolean hasMoves, boolean rollAgain) {
        this.player = player;
        this.value = value;
        this.cancelledThirdSix = cancelledThirdSix;
        this.hasMoves = hasMoves;
        this.rollAgain = rollAgain;
    }

    /** @return השחקן שהטיל */
    public Player getPlayer() {
        return player;
    }

    public int getValue() {
        return value;
    }

    /** @return true אם זו הטלת 6 שלישית ברצף, שבוטלה והתור עבר (FR-19) */
    public boolean isCancelledThirdSix() {
        return cancelledThirdSix;
    }

    /** @return true אם יש לשחקן לפחות מהלך חוקי אחד */
    public boolean hasMoves() {
        return hasMoves;
    }

    /** @return true אם אין מהלך, אבל יצא 6 ולכן השחקן מטיל שוב */
    public boolean isRollAgain() {
        return rollAgain;
    }
}

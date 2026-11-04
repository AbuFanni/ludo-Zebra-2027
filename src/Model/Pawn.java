package Model;

/**
 * כלי משחק של שחקן.
 * <p>
 * מבוסס על {@code GameplayAndLogic.Pawn} בפרויקט Ludo-Griniaris-Game (MIT). שם הכלי החזיק מספר תא
 * ואייקון; כאן הוא מחזיק רק את המקום שלו במסלול של הצבע שלו, בלי תמונות (MVC).
 */
public class Pawn {

    /** הערך של {@link #getPathIndex()} כשהכלי בטירה. */
    public static final int IN_BASE = -1;

    private final Player owner;
    private final int number;
    private int pathIndex = IN_BASE;

    /**
     * @param owner  השחקן שהכלי שלו
     * @param number מספר הכלי אצל השחקן, 0 עד 3
     */
    Pawn(Player owner, int number) {
        this.owner = owner;
        this.number = number;
    }

    public Player getOwner() {
        return owner;
    }

    public PlayerColor getColor() {
        return owner.getColor();
    }

    /** @return מספר הכלי אצל השחקן, 0 עד 3 */
    public int getNumber() {
        return number;
    }

    /** @return המקום במסלול של הצבע, או {@link #IN_BASE} אם הכלי בטירה */
    public int getPathIndex() {
        return pathIndex;
    }

    public boolean isInBase() {
        return pathIndex == IN_BASE;
    }

    /** מזיז את הכלי. נקרא רק מ-{@link Game}. */
    void setPathIndex(int pathIndex) {
        this.pathIndex = pathIndex;
    }

    /** מחזיר את הכלי לטירה (אחרי שנאכל). */
    void sendToBase() {
        this.pathIndex = IN_BASE;
    }

    @Override
    public String toString() {
        return owner.getName() + "#" + (number + 1);
    }
}

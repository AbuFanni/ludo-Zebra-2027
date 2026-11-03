package Model;

/**
 * הצבעים שאפשר לבחור לשחקן (FR-05).
 * <p>
 * המודל לא משתמש במחלקות ממשק (NFR-06), ולכן אין כאן {@code java.awt.Color};
 * ההמרה לצבע מסך נעשית בחבילת View.
 */
public enum PlayerColor {
    RED("אדום"),
    GREEN("ירוק"),
    YELLOW("צהוב"),
    BLUE("כחול");

    private final String hebrewName;

    PlayerColor(String hebrewName) {
        this.hebrewName = hebrewName;
    }

    /** @return שם הצבע בעברית. */
    public String getHebrewName() {
        return hebrewName;
    }

    @Override
    public String toString() {
        return hebrewName;
    }
}

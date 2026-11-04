package Model;

/**
 * הצבעים של השחקנים (FR-05), לפי סדר הפינות עם כיוון השעון:
 * ירוק בפינה השמאלית העליונה, צהוב בימנית העליונה, כחול בימנית התחתונה ואדום בשמאלית התחתונה.
 * <p>
 * המודל לא משתמש במחלקות ממשק, ולכן אין כאן java.awt.Color. ההמרה לצבע מסך נעשית ב-View.
 */
public enum PlayerColor {
    GREEN("ירוק", 0),
    YELLOW("צהוב", 1),
    BLUE("כחול", 2),
    RED("אדום", 3);

    private final String hebrewName;
    private final int quarterTurns;

    PlayerColor(String hebrewName, int quarterTurns) {
        this.hebrewName = hebrewName;
        this.quarterTurns = quarterTurns;
    }

    /** @return שם הצבע בעברית */
    public String getHebrewName() {
        return hebrewName;
    }

    /**
     * @return כמה סיבובים של 90 מעלות עם כיוון השעון מעבירים את הצד של הירוק לצד של הצבע הזה.
     *         כך כל המסלולים נבנים מהמסלול של הירוק.
     */
    public int getQuarterTurns() {
        return quarterTurns;
    }

    @Override
    public String toString() {
        return hebrewName;
    }
}

package Model;

/**
 * ששת אירועי ההפתעה וההסתברות של כל אחד, לפי מסמך הפרויקט (FR-44).
 * סכום האחוזים הוא 100.
 *
 * @author Nour Eldin Khatib (Zebra) – Iteration 2, US-8 (FR-44 עד FR-52)
 */
public enum SurpriseEvent {

    /** הכלי עובר ישר לבית הסיום (FR-45). */
    JUMP_TO_FINISH(10, "קפיצה לבית הסיום",
            "הכלי עובר ישר לבית הסיום, בלי מספר מדויק ובלי לעצור ב-Double Block."),
    /** בוחרים יריב וכלי שלו, והוא זז אחורה (FR-46, FR-47). */
    MOVE_OPPONENT(10, "הזזת כלי יריב",
            "בחרו יריב וכלי שלו במסלול המשותף. הכלי זז אחורה חצי מתוצאת קוביית צעדים, מעוגל למעלה."),
    /** הכלי חוזר לטירה (FR-48). */
    BACK_TO_BASE(10, "חזרה לטירה",
            "הכלי חוזר לטירה."),
    /** הכלי חוזר 3 צעדים (FR-49). */
    BACK_THREE(30, "חזרה 3 צעדים",
            "הכלי חוזר 3 צעדים לאורך המסלול, ולא מעבר למשבצת ההתחלה."),
    /** השחקן מפסיד את התור הבא (FR-50). */
    LOSE_NEXT_TURN(30, "הפסד התור הבא",
            "התור הבא של השחקן מדולג, והטלה נוספת מ-6 מבוטלת."),
    /** הטלה מיוחדת של קוביית צעדים ו-UltraBoost (FR-51, FR-52). */
    ULTRA_BOOST(10, "UltraBoost",
            "הטלה מיוחדת של קוביית צעדים ו-UltraBoost. מספר הצעדים: התוצאה בקוביית הצעדים כפול התוצאה ב-UltraBoost פחות 1.");

    private final int percent;
    private final String hebrewName;
    private final String description;

    SurpriseEvent(int percent, String hebrewName, String description) {
        this.percent = percent;
        this.hebrewName = hebrewName;
        this.description = description;
    }

    /** @return ההסתברות באחוזים (FR-44) */
    public int getPercent() {
        return percent;
    }

    public String getHebrewName() {
        return hebrewName;
    }

    /** @return הסבר קצר למשתמש על מה שהאירוע עושה */
    public String getDescription() {
        return description;
    }

    /** @return true אם צריך בחירה של השחקן (יריב וכלי, או הטלה) לפני שהאירוע מתבצע */
    public boolean needsChoice() {
        return this == MOVE_OPPONENT || this == ULTRA_BOOST;
    }
}

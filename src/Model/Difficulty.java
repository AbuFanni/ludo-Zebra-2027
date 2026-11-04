package Model;

/**
 * רמות הקושי של המשחק ומה שכל רמה כוללת (FR-03, FR-09).
 * <p>
 * כל הפרמטרים של הרמה מוגדרים כאן במקום אחד, כך ששינוי ברמה לא מחייב שינוי בשאר הקוד.
 * <ul>
 *   <li>קל: לוח 15x15, מסלול של 52 משבצות.</li>
 *   <li>בינוני: מסלול חיצוני של 64 משבצות מסביב ללוח הרגיל (לוח 17x17).</li>
 *   <li>קשה: מסלול נוסף של 72 משבצות מסביב לבינוני (לוח 19x19).</li>
 * </ul>
 */
public enum Difficulty {
    EASY("קל", "easy", 0, 4, 0, 0),
    MEDIUM("בינוני", "medium", 1, 8, 2, 2),
    HARD("קשה", "hard", 2, 12, 3, 3);

    /** גודל הלוח הרגיל (לוח הקל), בלי מסלולים חיצוניים. */
    public static final int INNER_SIZE = 15;

    private final String hebrewName;
    private final String csvName;
    private final int outerRings;
    private final int safeSquares;
    private final int questionsPerLevel;
    private final int surpriseSquares;

    Difficulty(String hebrewName, String csvName, int outerRings, int safeSquares,
               int questionsPerLevel, int surpriseSquares) {
        this.hebrewName = hebrewName;
        this.csvName = csvName;
        this.outerRings = outerRings;
        this.safeSquares = safeSquares;
        this.questionsPerLevel = questionsPerLevel;
        this.surpriseSquares = surpriseSquares;
    }

    /** @return שם הרמה בעברית, לתצוגה */
    public String getHebrewName() {
        return hebrewName;
    }

    /** @return שם הרמה כפי שנשמר בקובץ ההיסטוריה (easy / medium / hard) */
    public String getCsvName() {
        return csvName;
    }

    /** @return מספר המסלולים החיצוניים מסביב ללוח הרגיל (0, 1 או 2) */
    public int getOuterRings() {
        return outerRings;
    }

    /** @return גודל הצד של הלוח במשבצות (15, 17 או 19) */
    public int getGridSize() {
        return INNER_SIZE + 2 * outerRings;
    }

    /** @return מספר משבצות ההגנה (כוכב) שמוגרלות על הלוח */
    public int getSafeSquares() {
        return safeSquares;
    }

    /** @return מספר משבצות השאלה מכל רמת שאלה (קלה, בינונית, קשה) */
    public int getQuestionsPerLevel() {
        return questionsPerLevel;
    }

    /** @return מספר משבצות ההפתעה */
    public int getSurpriseSquares() {
        return surpriseSquares;
    }

    /**
     * @param csvName שם הרמה מהקובץ
     * @return הרמה המתאימה, או null אם השם לא מוכר
     */
    public static Difficulty fromCsvName(String csvName) {
        for (Difficulty d : values()) {
            if (d.csvName.equalsIgnoreCase(csvName.trim())) {
                return d;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return hebrewName;
    }
}

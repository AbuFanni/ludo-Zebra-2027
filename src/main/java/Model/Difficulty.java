package Model;

/**
 * רמות הקושי של המשחק (FR-03).
 * <p>
 * הפרמטרים של כל רמה (גודל הלוח, משבצות וקוביות – טבלה 4 ב-SRS) יוגדרו במקום אחד
 * במודל הלוח (NFR-15). כאן רק הזיהוי והשם לתצוגה.
 */
public enum Difficulty {
    EASY("קל"),
    MEDIUM("בינוני"),
    HARD("קשה");

    private final String hebrewName;

    Difficulty(String hebrewName) {
        this.hebrewName = hebrewName;
    }

    /** @return שם הרמה בעברית, לתצוגה במסכים. */
    public String getHebrewName() {
        return hebrewName;
    }

    @Override
    public String toString() {
        return hebrewName;
    }
}

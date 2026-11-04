package Model;

/**
 * רמות הקושי של שאלה, וכמה צעדים זזים בתשובה נכונה או שגויה.
 * הצבע של כל רמה על הלוח: קלה ירוק, בינונית צהוב, קשה אדום.
 */
public enum QuestionLevel {
    EASY("קלה", 1),
    MEDIUM("בינונית", 2),
    HARD("קשה", 3);

    private final String hebrewName;
    private final int steps;

    QuestionLevel(String hebrewName, int steps) {
        this.hebrewName = hebrewName;
        this.steps = steps;
    }

    public String getHebrewName() {
        return hebrewName;
    }

    /** @return מספר הצעדים קדימה בתשובה נכונה (ואחורה בתשובה שגויה) */
    public int getSteps() {
        return steps;
    }
}

package Model;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * שאלה אמריקאית מהמאגר: רמה, טקסט, 4 תשובות ותשובה נכונה אחת (FR-38, FR-69).
 * <p>
 * אובייקט קבוע (immutable). נוצר רק דרך {@link QuestionFactory}, שבודק שהשאלה תקינה, כך שכל
 * {@code Question} במערכת היא שאלה שאפשר להציג.
 *
 * @author Mohamed Shini (Zebra) – Iteration 2, US-7 (FR-38–FR-43)
 */
public final class Question {

    /** מספר התשובות לכל שאלה. */
    public static final int ANSWERS = 4;

    private final int id;
    private final QuestionLevel level;
    private final String text;
    private final List<String> answers;
    private final int correctIndex;

    /** נקרא רק מ-{@link QuestionFactory}, אחרי הבדיקות. */
    Question(int id, QuestionLevel level, String text, String[] answers, int correctIndex) {
        this.id = id;
        this.level = level;
        this.text = text;
        this.answers = Collections.unmodifiableList(Arrays.asList(answers.clone()));
        this.correctIndex = correctIndex;
    }

    /** @return המזהה של השאלה במאגר */
    public int getId() {
        return id;
    }

    public QuestionLevel getLevel() {
        return level;
    }

    public String getText() {
        return text;
    }

    /** @return ארבע התשובות לפי הסדר */
    public List<String> getAnswers() {
        return answers;
    }

    /** @return האינדקס של התשובה הנכונה, 0 עד 3 */
    public int getCorrectIndex() {
        return correctIndex;
    }

    public String getCorrectAnswer() {
        return answers.get(correctIndex);
    }

    /**
     * בודק תשובה (FR-40, FR-41): האם היא נכונה, וכמה צעדים הכלי זז – קדימה בתשובה נכונה ואחורה
     * באותו מספר צעדים בתשובה שגויה (1 לשאלה קלה, 2 לבינונית, 3 לקשה).
     *
     * @param choice האינדקס של התשובה שנבחרה, 0 עד 3
     * @return תוצאת הבדיקה
     * @throws IllegalArgumentException אם האינדקס לא בין 0 ל-3
     */
    public AnswerCheck check(int choice) {
        if (choice < 0 || choice >= ANSWERS) {
            throw new IllegalArgumentException("Answer must be 0 to 3: " + choice);
        }
        return new AnswerCheck(this, choice, choice == correctIndex);
    }

    @Override
    public String toString() {
        return "Q" + id + "[" + level + "] " + text;
    }
}

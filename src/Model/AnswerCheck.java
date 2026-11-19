package Model;

/**
 * התוצאה של בדיקת תשובה לשאלה (FR-40, FR-41): האם התשובה נכונה, מה הייתה התשובה הנכונה, וכמה
 * צעדים הכלי זז לפי רמת השאלה.
 * <p>
 * רק בדיקה: היא לא מזיזה כלי. את התנועה עצמה עושה {@link Game#answer(int)}.
 *
 * @author Mohamed Shini (Zebra) – Iteration 2, US-7 (FR-40, FR-41)
 */
public final class AnswerCheck {

    private final Question question;
    private final int chosenIndex;
    private final boolean correct;

    AnswerCheck(Question question, int chosenIndex, boolean correct) {
        this.question = question;
        this.chosenIndex = chosenIndex;
        this.correct = correct;
    }

    public Question getQuestion() {
        return question;
    }

    /** @return האינדקס של התשובה שנבחרה, 0 עד 3 */
    public int getChosenIndex() {
        return chosenIndex;
    }

    public boolean isCorrect() {
        return correct;
    }

    /** @return התשובה הנכונה, להצגה אחרי טעות (FR-41) */
    public String getCorrectAnswer() {
        return question.getCorrectAnswer();
    }

    /** @return מספר הצעדים לפי הרמה: 1 לקלה, 2 לבינונית, 3 לקשה */
    public int getSteps() {
        return question.getLevel().getSteps();
    }

    /** @return הצעדים עם סימן: חיובי קדימה בתשובה נכונה, שלילי אחורה בתשובה שגויה (FR-40) */
    public int getSignedSteps() {
        return correct ? getSteps() : -getSteps();
    }
}

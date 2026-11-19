package Model;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Factory Method: יוצר שאלות לפי רמה (SDP 6.2).
 * <p>
 * לכל רמה יש יוצר משלה ({@link #forLevel(QuestionLevel)}), והמתודה {@link #create} בודקת את השאלה
 * באותו אופן לכל הרמות לפני שהיא נוצרת. מי שקורא שאלות (הקובץ, המאגר ההתחלתי, ובאיטרציה 3 אשף
 * המנהל) לא יוצר {@link Question} בעצמו, ולכן אי אפשר שתגיע למשחק שאלה פגומה (FR-43, FR-71).
 * הוספת רמה חדשה משנה רק את {@link QuestionLevel} ואת היוצר שלה.
 *
 * @author Mohamed Shini (Zebra) – Iteration 2, US-7 (FR-38, FR-43)
 */
public abstract class QuestionFactory {

    private static final QuestionFactory EASY = new EasyQuestionFactory();
    private static final QuestionFactory MEDIUM = new MediumQuestionFactory();
    private static final QuestionFactory HARD = new HardQuestionFactory();

    QuestionFactory() {
    }

    /** @return היוצר של השאלות ברמה */
    public static QuestionFactory forLevel(QuestionLevel level) {
        switch (level) {
            case EASY:
                return EASY;
            case MEDIUM:
                return MEDIUM;
            case HARD:
                return HARD;
            default:
                throw new IllegalArgumentException("Unknown level: " + level);
        }
    }

    /** Factory Method: הרמה של השאלות שהיוצר הזה יוצר. */
    protected abstract QuestionLevel level();

    /**
     * יוצר שאלה ברמה של היוצר, אחרי בדיקה שהיא תקינה.
     *
     * @param id           המזהה במאגר, מספר חיובי
     * @param text         טקסט השאלה
     * @param answers      בדיוק 4 תשובות שונות זו מזו
     * @param correctIndex האינדקס של התשובה הנכונה, 0 עד 3
     * @return שאלה חדשה
     * @throws IllegalArgumentException עם הסבר מה חסר או שגוי
     */
    public final Question create(int id, String text, List<String> answers, int correctIndex) {
        if (id <= 0) {
            throw new IllegalArgumentException("מזהה השאלה חייב להיות מספר חיובי");
        }
        String cleanText = clean(text, "טקסט השאלה");
        if (answers == null || answers.size() != Question.ANSWERS) {
            throw new IllegalArgumentException("לשאלה צריכות להיות בדיוק 4 תשובות");
        }
        String[] clean = new String[Question.ANSWERS];
        Set<String> seen = new HashSet<String>();
        for (int i = 0; i < Question.ANSWERS; i++) {
            clean[i] = clean(answers.get(i), "תשובה " + (i + 1));
            if (!seen.add(clean[i].toLowerCase())) {
                throw new IllegalArgumentException("תשובה " + (i + 1) + " זהה לתשובה אחרת");
            }
        }
        if (correctIndex < 0 || correctIndex >= Question.ANSWERS) {
            throw new IllegalArgumentException("התשובה הנכונה חייבת להיות 1 עד 4");
        }
        return new Question(id, level(), cleanText, clean, correctIndex);
    }

    /** מוריד רווחים מהקצוות ובודק שהשדה לא ריק ושאין בו טאב או ירידת שורה (שמפרידים בקובץ). */
    private static String clean(String value, String field) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(field + " ריק");
        }
        if (value.indexOf('\t') >= 0 || value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0) {
            throw new IllegalArgumentException(field + " מכיל טאב או ירידת שורה");
        }
        return value.trim();
    }

    /** יוצר שאלות קלות: תשובה נכונה מקדמת צעד אחד. */
    static final class EasyQuestionFactory extends QuestionFactory {
        @Override
        protected QuestionLevel level() {
            return QuestionLevel.EASY;
        }
    }

    /** יוצר שאלות בינוניות: 2 צעדים. */
    static final class MediumQuestionFactory extends QuestionFactory {
        @Override
        protected QuestionLevel level() {
            return QuestionLevel.MEDIUM;
        }
    }

    /** יוצר שאלות קשות: 3 צעדים. */
    static final class HardQuestionFactory extends QuestionFactory {
        @Override
        protected QuestionLevel level() {
            return QuestionLevel.HARD;
        }
    }
}

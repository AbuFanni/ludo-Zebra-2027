package Model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * השאלות של משחק אחד, חפיסה לכל רמה (FR-38, FR-42).
 * <p>
 * כל חפיסה מעורבבת באקראי. שאלה לא חוזרת לפני שהוצגו כל השאלות ברמה שלה; כשהחפיסה נגמרת היא
 * מעורבבת מחדש, והשאלה האחרונה שהוצגה לא תהיה הראשונה בסבב החדש (אם יש יותר משאלה אחת).
 * לכל משחק נוצרת חפיסה חדשה, ולכן שינויים במאגר חלים מהמשחק הבא (FR-73).
 *
 * @author Mohamed Shini (Zebra) – Iteration 2, US-7 (FR-42)
 */
public class QuestionDeck {

    private final Map<QuestionLevel, List<Question>> all = new EnumMap<QuestionLevel, List<Question>>(QuestionLevel.class);
    private final Map<QuestionLevel, List<Question>> remaining = new EnumMap<QuestionLevel, List<Question>>(QuestionLevel.class);
    private final Map<QuestionLevel, Question> last = new EnumMap<QuestionLevel, Question>(QuestionLevel.class);
    private final Random random;

    /**
     * @param questions כל השאלות במאגר, מכל הרמות
     * @param random    מקור האקראיות (קבוע בבדיקות)
     */
    public QuestionDeck(List<Question> questions, Random random) {
        this.random = random;
        for (QuestionLevel level : QuestionLevel.values()) {
            all.put(level, new ArrayList<Question>());
            remaining.put(level, new ArrayList<Question>());
        }
        for (Question q : questions) {
            all.get(q.getLevel()).add(q);
        }
    }

    /** @return כמה שאלות יש ברמה */
    public int size(QuestionLevel level) {
        return all.get(level).size();
    }

    /** @return כמה שאלות ברמה עוד לא הוצגו בסבב הנוכחי */
    public int remaining(QuestionLevel level) {
        return remaining.get(level).size();
    }

    /**
     * @return השאלה הבאה ברמה, אקראית, בלי חזרה לפני שכל השאלות ברמה הוצגו
     * @throws IllegalStateException אם אין שאלות ברמה
     */
    public Question next(QuestionLevel level) {
        List<Question> pile = remaining.get(level);
        if (pile.isEmpty()) {
            List<Question> source = all.get(level);
            if (source.isEmpty()) {
                throw new IllegalStateException("No questions at level " + level);
            }
            pile.addAll(source);
            Collections.shuffle(pile, random);
            // השאלה האחרונה מהסבב הקודם לא תופיע שוב מיד (היא בסוף הרשימה = נשלפת אחרונה)
            Question prev = last.get(level);
            if (prev != null && pile.size() > 1 && pile.get(pile.size() - 1) == prev) {
                Collections.swap(pile, 0, pile.size() - 1);
            }
        }
        Question q = pile.remove(pile.size() - 1);
        last.put(level, q);
        return q;
    }
}

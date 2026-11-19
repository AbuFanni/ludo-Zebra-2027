package Model;

import java.util.List;

/**
 * Observer: מי שרוצה לדעת מה קורה במשחק (בדרך כלל ה-Controller, שמעדכן את המסך).
 * המודל לא מכיר את המסך, הוא רק מודיע למאזינים.
 */
public interface GameListener {

    /** הוטלה קובייה. */
    default void onRolled(RollResult result) {
    }

    /** בוצע מהלך. */
    default void onMoved(MoveResult result) {
    }

    /** שחקן ענה על שאלה והכלי זז לפי התשובה (FR-40). */
    default void onAnswered(AnswerResult result) {
    }

    /** כלי נחת על משבצת הפתעה והוגרל אירוע (FR-44); האירוע מחכה לביצוע. */
    default void onSurprise(SurpriseEvent event, Pawn pawn) {
    }

    /** אירוע הפתעה בוצע (FR-45 עד FR-52). */
    default void onSurpriseResolved(SurpriseResult result) {
    }

    /** מדלגים על התור של שחקן שהפסיד את התור הבא באירוע הפתעה (FR-50). */
    default void onTurnSkipped(Player player) {
    }

    /** התור עבר לשחקן אחר. */
    default void onTurnChanged(Player current) {
    }

    /** המשחק נגמר. */
    default void onGameOver(List<Player> ranking) {
    }
}

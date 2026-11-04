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

    /** התור עבר לשחקן אחר. */
    default void onTurnChanged(Player current) {
    }

    /** המשחק נגמר. */
    default void onGameOver(List<Player> ranking) {
    }
}

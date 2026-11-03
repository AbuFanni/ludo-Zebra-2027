package Model;

import java.util.List;

/**
 * הגדרות משחק תקינות: הרמה ורשימת השחקנים לפי סדר התורות.
 * <p>
 * נוצר רק דרך {@link GameSetupValidator} אחרי שכל השדות עברו בדיקה, והרשימה
 * מועתקת לרשימה בלתי ניתנת לשינוי – כך ההגדרות נעולות אחרי תחילת המשחק (FR-08).
 *
 * @param difficulty רמת הקושי
 * @param players    השחקנים, לפי סדר ההזנה (FR-07)
 */
public record GameSettings(Difficulty difficulty, List<Player> players) {

    /** יוצר עותק בלתי ניתן לשינוי של רשימת השחקנים. */
    public GameSettings {
        players = List.copyOf(players);
    }
}

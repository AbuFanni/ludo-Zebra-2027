package Model;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * בודק את מסך הגדרת המשחק (FR-03 עד FR-07). הבדיקה כולה במודל, כך שאפשר לבדוק אותה בלי מסך;
 * ה-View רק מציג את השגיאות (FR-06).
 * <ul>
 *   <li>2 עד 4 שחקנים ורמת קושי.</li>
 *   <li>שם: 1 עד 15 תווים אחרי הסרת רווחים, בלי פסיק (כדי לא לשבור את קובץ ה-CSV), ושונה משמות
 *       שאר השחקנים בלי הבדל בין אותיות גדולות לקטנות.</li>
 *   <li>צבע לכל שחקן, בלי כפילות.</li>
 * </ul>
 * מבוסס על בדיקת הקלט ב-{@code SettingsPanel.wrongInput()/isUniqueUsersName()} בפרויקט המקור,
 * שהציגה חלון שגיאה אחד; כאן כל שגיאה נשמרת ליד השדה שלה.
 */
public final class GameSetupValidator {

    /** אורך שם מקסימלי. */
    public static final int MAX_NAME_LENGTH = 15;

    private GameSetupValidator() {
    }

    /**
     * @param playerCount מספר השחקנים שנבחר
     * @param difficulty  הרמה שנבחרה, או null
     * @param names       השמות לפי סדר השורות (לפחות playerCount)
     * @param colors      הצבעים לפי סדר השורות (null לצבע שלא נבחר)
     * @return השגיאות, או הגדרות תקינות
     */
    public static SetupValidationResult validate(int playerCount, Difficulty difficulty,
                                                 List<String> names, List<PlayerColor> colors) {
        SetupValidationResult result = new SetupValidationResult();
        if (playerCount < 2 || playerCount > 4) {
            result.setGeneralError("יש לבחור 2 עד 4 שחקנים");
            return result;
        }
        if (difficulty == null) {
            result.setGeneralError("יש לבחור רמת קושי");
        }

        List<String> clean = new ArrayList<String>();
        for (int i = 0; i < playerCount; i++) {
            String raw = i < names.size() ? names.get(i) : null;
            String name = raw == null ? "" : raw.trim();
            clean.add(name);
            if (name.isEmpty()) {
                result.addNameError(i, "יש להזין שם");
            } else if (nameLength(name) > MAX_NAME_LENGTH) {
                result.addNameError(i, "השם ארוך מדי – עד " + MAX_NAME_LENGTH + " תווים");
            } else if (name.contains(",")) {
                result.addNameError(i, "אסור להשתמש בפסיק בשם");
            } else {
                for (int j = 0; j < i; j++) {
                    if (clean.get(j).toLowerCase(Locale.ROOT).equals(name.toLowerCase(Locale.ROOT))) {
                        result.addNameError(i, "השם כבר נבחר ע\"י שחקן " + (j + 1));
                        break;
                    }
                }
            }

            PlayerColor color = i < colors.size() ? colors.get(i) : null;
            if (color == null) {
                result.addColorError(i, "יש לבחור צבע");
            } else {
                for (int j = 0; j < i; j++) {
                    if (j < colors.size() && colors.get(j) == color) {
                        result.addColorError(i, "הצבע כבר נבחר ע\"י שחקן " + (j + 1));
                        break;
                    }
                }
            }
        }

        if (result.getGeneralError() == null && result.getNameErrors().isEmpty()
                && result.getColorErrors().isEmpty()) {
            result.setSettings(new GameSettings(difficulty, clean, colors.subList(0, playerCount)));
        }
        return result;
    }

    /**
     * אורך השם בתווים כפי שהשחקן רואה אותם (FR-04). {@code String.length()} סופר תווים מסוימים
     * (למשל אימוג'י) כשניים, ולכן שם של 15 תווים כאלה היה נדחה בטעות.
     *
     * @param name השם אחרי הסרת רווחים
     * @return מספר התווים בשם
     */
    static int nameLength(String name) {
        return name.codePointCount(0, name.length());
    }
}

package Model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * בודק את הקלט של מסך הגדרת המשחק ויוצר {@link GameSettings} רק כשהכול תקין.
 * <ul>
 *   <li>FR-03 – 2 עד 4 שחקנים, ורמה אחת מתוך קל / בינוני / קשה.</li>
 *   <li>FR-04 – שם של 1 עד 15 תווים, בלי פסיק, ששונה משמות שאר השחקנים.</li>
 *   <li>FR-05 – צבע אחד מתוך ארבעה, בלי כפילות.</li>
 *   <li>FR-06 – לכל שדה לא תקין מוחזרת הודעה שמסבירה מה לתקן.</li>
 *   <li>FR-07 – סדר התורות לפי סדר ההזנה.</li>
 * </ul>
 * המחלקה לא תלויה בממשק (NFR-06), ולכן אפשר לבדוק אותה ב-JUnit בלי מסך.
 */
public final class GameSetupValidator {

    /** מספר השחקנים המינימלי. */
    public static final int MIN_PLAYERS = 2;
    /** מספר השחקנים המקסימלי. */
    public static final int MAX_PLAYERS = 4;
    /** אורך השם המקסימלי (FR-04). */
    public static final int MAX_NAME_LENGTH = 15;

    static final String ERR_COUNT = "יש לבחור 2, 3 או 4 שחקנים";
    static final String ERR_DIFFICULTY = "יש לבחור רמת קושי";
    static final String ERR_NAME_EMPTY = "יש להזין שם";
    static final String ERR_NAME_LONG = "השם ארוך מדי – עד 15 תווים";
    static final String ERR_NAME_COMMA = "אסור להשתמש בפסיק בשם";
    static final String ERR_COLOR_EMPTY = "יש לבחור צבע";

    /**
     * בודק את כל השדות.
     *
     * @param playerCount מספר השחקנים שנבחר
     * @param difficulty  הרמה שנבחרה (יכולה להיות {@code null})
     * @param names       השמות כפי שהוזנו, לפי הסדר; נבדקים רק {@code playerCount} הראשונים
     * @param colors      הצבעים שנבחרו, לפי הסדר; ערך {@code null} = לא נבחר
     * @return תוצאה עם שגיאה לכל שדה לא תקין, או הגדרות תקינות
     */
    public SetupValidationResult validate(int playerCount, Difficulty difficulty,
                                          List<String> names, List<PlayerColor> colors) {
        Map<String, String> errors = new LinkedHashMap<>();

        if (playerCount < MIN_PLAYERS || playerCount > MAX_PLAYERS) {
            errors.put(SetupValidationResult.PLAYER_COUNT, ERR_COUNT);
            return new SetupValidationResult(errors, null);
        }
        if (difficulty == null) {
            errors.put(SetupValidationResult.DIFFICULTY, ERR_DIFFICULTY);
        }

        Map<String, Integer> seenNames = new HashMap<>();
        Map<PlayerColor, Integer> seenColors = new HashMap<>();
        List<Player> players = new ArrayList<>();

        for (int i = 0; i < playerCount; i++) {
            String name = i < names.size() && names.get(i) != null ? names.get(i).trim() : "";
            PlayerColor color = i < colors.size() ? colors.get(i) : null;

            String nameError = checkName(name, seenNames);
            if (nameError != null) {
                errors.put(SetupValidationResult.nameField(i), nameError);
            } else {
                seenNames.put(normalize(name), i);
            }

            String colorError = checkColor(color, seenColors);
            if (colorError != null) {
                errors.put(SetupValidationResult.colorField(i), colorError);
            } else {
                seenColors.put(color, i);
            }

            players.add(new Player(name, color, i));
        }

        GameSettings settings = errors.isEmpty() ? new GameSettings(difficulty, players) : null;
        return new SetupValidationResult(errors, settings);
    }

    private String checkName(String name, Map<String, Integer> seenNames) {
        if (name.isEmpty()) {
            return ERR_NAME_EMPTY;
        }
        if (name.length() > MAX_NAME_LENGTH) {
            return ERR_NAME_LONG;
        }
        if (name.contains(",")) {
            return ERR_NAME_COMMA;
        }
        Integer owner = seenNames.get(normalize(name));
        if (owner != null) {
            return "השם כבר נבחר ע\"י שחקן " + (owner + 1);
        }
        return null;
    }

    private String checkColor(PlayerColor color, Map<PlayerColor, Integer> seenColors) {
        if (color == null) {
            return ERR_COLOR_EMPTY;
        }
        Integer owner = seenColors.get(color);
        if (owner != null) {
            return "הצבע כבר נבחר ע\"י שחקן " + (owner + 1);
        }
        return null;
    }

    /** שמות זהים בלי קשר לרווחים בקצוות ולאותיות גדולות/קטנות נחשבים כפולים. */
    private static String normalize(String name) {
        return name.trim().toLowerCase(Locale.ROOT);
    }
}

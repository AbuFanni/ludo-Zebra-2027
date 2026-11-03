package Model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * תוצאת בדיקת מסך ההגדרה: הודעת שגיאה לכל שדה לא תקין (FR-06), או הגדרות משחק תקינות.
 * <p>
 * מפתחות השדות: {@link #PLAYER_COUNT}, {@link #DIFFICULTY}, ו-{@link #nameField(int)} /
 * {@link #colorField(int)} לכל שחקן (אינדקס מ-0).
 */
public final class SetupValidationResult {

    /** מפתח השדה של מספר השחקנים. */
    public static final String PLAYER_COUNT = "playerCount";
    /** מפתח השדה של רמת הקושי. */
    public static final String DIFFICULTY = "difficulty";

    private final Map<String, String> errors;
    private final GameSettings settings;

    SetupValidationResult(Map<String, String> errors, GameSettings settings) {
        this.errors = Collections.unmodifiableMap(new LinkedHashMap<>(errors));
        this.settings = settings;
    }

    /** @return מפתח שדה השם של שחקן {@code index}. */
    public static String nameField(int index) {
        return "name." + index;
    }

    /** @return מפתח שדה הצבע של שחקן {@code index}. */
    public static String colorField(int index) {
        return "color." + index;
    }

    /** @return {@code true} כשכל השדות תקינים ואפשר להתחיל משחק. */
    public boolean isValid() {
        return errors.isEmpty();
    }

    /** @return הודעת השגיאה של השדה, אם יש. */
    public Optional<String> errorFor(String field) {
        return Optional.ofNullable(errors.get(field));
    }

    /** @return כל השגיאות, לפי סדר השדות במסך. */
    public Map<String, String> getErrors() {
        return errors;
    }

    /**
     * @return ההגדרות הנעולות של המשחק
     * @throws IllegalStateException אם יש שדה לא תקין (FR-06 – לא מתחילים משחק)
     */
    public GameSettings getSettings() {
        if (!isValid()) {
            throw new IllegalStateException("אי אפשר להתחיל משחק כשיש שדה לא תקין");
        }
        return settings;
    }
}

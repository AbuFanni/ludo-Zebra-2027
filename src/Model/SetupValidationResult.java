package Model;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

/**
 * תוצאת בדיקת מסך ההגדרה: הודעת שגיאה לכל שדה שגוי (FR-06), או הגדרות תקינות.
 */
public final class SetupValidationResult {

    private final Map<Integer, String> nameErrors = new TreeMap<Integer, String>();
    private final Map<Integer, String> colorErrors = new TreeMap<Integer, String>();
    private String generalError;
    private GameSettings settings;

    void addNameError(int row, String message) {
        if (!nameErrors.containsKey(row)) {
            nameErrors.put(row, message);
        }
    }

    void addColorError(int row, String message) {
        if (!colorErrors.containsKey(row)) {
            colorErrors.put(row, message);
        }
    }

    void setGeneralError(String message) {
        this.generalError = message;
    }

    void setSettings(GameSettings settings) {
        this.settings = settings;
    }

    public boolean isValid() {
        return settings != null;
    }

    /** @return ההגדרות התקינות, או null אם יש שגיאות */
    public GameSettings getSettings() {
        return settings;
    }

    /** @return שגיאה לפי מספר שורת שחקן (מ-0) */
    public Map<Integer, String> getNameErrors() {
        return Collections.unmodifiableMap(nameErrors);
    }

    public Map<Integer, String> getColorErrors() {
        return Collections.unmodifiableMap(colorErrors);
    }

    /** @return שגיאה כללית (מספר שחקנים או רמה), או null */
    public String getGeneralError() {
        return generalError;
    }
}

package Model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * שורה אחת בהיסטוריית המשחקים, לפי תבנית ה-CSV של הקורס:
 * gameID, difficulty, timestamp, שמות המקומות 1 עד 4, game_duration.
 */
public final class GameRecord {

    private final int gameId;
    private final String difficulty;
    private final String timestamp;
    private final List<String> places;
    private final String duration;

    /**
     * @param gameId     מספר המשחק
     * @param difficulty easy / medium / hard
     * @param timestamp  DD/MM/YYYY;HH:MM:SS
     * @param places     שמות השחקנים לפי סדר הסיום (2 עד 4)
     * @param duration   HH:MM:SS
     */
    public GameRecord(int gameId, String difficulty, String timestamp, List<String> places, String duration) {
        this.gameId = gameId;
        this.difficulty = difficulty;
        this.timestamp = timestamp;
        this.places = Collections.unmodifiableList(new ArrayList<String>(places));
        this.duration = duration;
    }

    public int getGameId() {
        return gameId;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public String getTimestamp() {
        return timestamp;
    }

    /** @return שמות המקומות לפי הסדר; מקום שלא היה בו שחקן הוא מחרוזת ריקה */
    public List<String> getPlaces() {
        return places;
    }

    /** @return שם השחקן במקום (1 עד 4), או מחרוזת ריקה */
    public String getPlace(int place) {
        return place - 1 < places.size() ? places.get(place - 1) : "";
    }

    public String getDuration() {
        return duration;
    }
}

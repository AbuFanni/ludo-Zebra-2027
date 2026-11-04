package Model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * לוח המשחק של רמה מסוימת (FR-09): כל המשבצות, והמסלול של כל צבע ממשבצת ההתחלה ועד בית הסיום.
 * <p>
 * הרעיון של מסלול לכל צבע כרשימת משבצות לקוח מ-{@code Gameplay.greenPath()/redPath()...} בפרויקט
 * Ludo-Griniaris-Game (MIT). שם כל מסלול נכתב ידנית; כאן המסלולים נבנים ב-{@link BoardBuilder}
 * מהמסלול של הירוק בסיבוב של 90 מעלות, לכל גודל לוח.
 * <p>
 * נוצר רק דרך {@link BoardBuilder}.
 */
public final class Board {

    /** גודל הטירה (אזור הבסיס) של כל שחקן, במשבצות. */
    public static final int BASE_SIZE = 6;

    private final Difficulty difficulty;
    private final int size;
    private final Map<Position, Square> squares;
    private final Map<PlayerColor, List<Square>> paths;
    private final Map<PlayerColor, Position> baseOrigins;
    private final List<Square> sharedTrack;

    Board(Difficulty difficulty, Map<Position, Square> squares, Map<PlayerColor, List<Square>> paths,
          Map<PlayerColor, Position> baseOrigins, List<Square> sharedTrack) {
        this.difficulty = difficulty;
        this.size = difficulty.getGridSize();
        this.squares = Collections.unmodifiableMap(new LinkedHashMap<Position, Square>(squares));
        EnumMap<PlayerColor, List<Square>> copy = new EnumMap<PlayerColor, List<Square>>(PlayerColor.class);
        for (Map.Entry<PlayerColor, List<Square>> e : paths.entrySet()) {
            copy.put(e.getKey(), Collections.unmodifiableList(new ArrayList<Square>(e.getValue())));
        }
        this.paths = Collections.unmodifiableMap(copy);
        this.baseOrigins = Collections.unmodifiableMap(new EnumMap<PlayerColor, Position>(baseOrigins));
        this.sharedTrack = Collections.unmodifiableList(new ArrayList<Square>(sharedTrack));
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }

    /** @return גודל צד הלוח במשבצות */
    public int getSize() {
        return size;
    }

    /** @return המשבצת במיקום, או null אם אין שם משבצת (טירה, רקע או מרכז) */
    public Square getSquare(Position p) {
        return squares.get(p);
    }

    /** @return כל המשבצות בלוח */
    public Collection<Square> getSquares() {
        return squares.values();
    }

    /** @return כל משבצות המסלולים המשותפים (בלי מסלולי הבית ובתי הסיום) */
    public List<Square> getSharedTrack() {
        return sharedTrack;
    }

    /**
     * @param color צבע השחקן
     * @return המסלול של הצבע: אינדקס 0 הוא משבצת ההתחלה, והאחרון הוא בית הסיום
     */
    public List<Square> getPath(PlayerColor color) {
        return paths.get(color);
    }

    /** @return האינדקס של בית הסיום במסלול (זהה לכל הצבעים) */
    public int getFinishIndex() {
        return paths.get(PlayerColor.GREEN).size() - 1;
    }

    /**
     * @param color צבע השחקן
     * @return הפינה השמאלית העליונה של הטירה של הצבע (ריבוע של {@link #BASE_SIZE} משבצות)
     */
    public Position getBaseOrigin(PlayerColor color) {
        return baseOrigins.get(color);
    }

    /**
     * @param ringIndex 0 למסלול החיצוני ביותר
     * @return מספר המשבצות במסלול, למשל 52 בלוח הקל, 64 ו-52 בבינוני, 72, 64 ו-52 בקשה
     */
    public int getRingLength(int ringIndex) {
        if (ringIndex >= difficulty.getOuterRings()) {
            return BoardBuilder.INNER_TRACK_LENGTH;
        }
        return 4 * (size - 1 - 2 * ringIndex);
    }

    /** @return מספר המשבצות מסוג מסוים בלוח */
    public int count(SquareKind kind) {
        int n = 0;
        for (Square s : squares.values()) {
            if (s.getKind() == kind) {
                n++;
            }
        }
        return n;
    }
}

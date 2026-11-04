package Model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Builder: בונה את הלוח של כל רמה (FR-09).
 * <p>
 * הגאומטריה מבוססת על {@code GameBoardData.GameBoardMap} בפרויקט Ludo-Griniaris-Game (MIT), שבנה לוח
 * צלב בגודל משתנה. כאן נוספו המסלולים החיצוניים של הרמות הבינונית והקשה לפי מסמך הפרויקט:
 * <ul>
 *   <li>קל: לוח 15x15. הכלי מתחיל ב-# ליד הטירה, מקיף את מסלול 52 המשבצות וממשיך במשבצת החץ
 *       למסלול הבית.</li>
 *   <li>בינוני וקשה: הכלי מתחיל ב-# בפינת המסלול החיצוני, מתקדם עם כיוון השעון עד משבצת החץ
 *       בצבע שלו, עובר בה למסלול הפנימי הבא, וכך עד לוח 52 המשבצות ומסלול הבית.</li>
 * </ul>
 * כל המסלולים נבנים מהמסלול של הירוק (צד שמאל) בסיבוב של 90 מעלות. מיקום משבצות ההגנה,
 * השאלה וההפתעה מוגרל בכל משחק, רק על המסלולים המשותפים, בלי שתי משבצות מיוחדות באותו מקום.
 */
public final class BoardBuilder {

    /** אורך המסלול של הלוח הרגיל. */
    public static final int INNER_TRACK_LENGTH = 52;

    /** מספר משבצות מסלול הבית לפני בית הסיום. */
    public static final int HOME_PATH_LENGTH = 5;

    private BoardBuilder() {
    }

    /**
     * @param difficulty הרמה
     * @param random     מקור אקראיות למיקום המשבצות המיוחדות (אפשר להעביר seed בבדיקות)
     * @return לוח חדש
     */
    public static Board build(Difficulty difficulty, Random random) {
        int n = difficulty.getGridSize();
        int rings = difficulty.getOuterRings();
        int o = rings; // היסט הלוח הפנימי בתוך הלוח הגדול

        // 1. המסלול והמשבצות המיוחדות של הירוק
        List<Position> greenPath = new ArrayList<Position>();
        Map<Position, SquareKind> greenRoles = new HashMap<Position, SquareKind>();

        for (int k = 0; k < rings; k++) {
            Position start = (k == 0) ? new Position(0, 0) : new Position(arrowRow(k - 1, rings), k);
            Position arrow = new Position(arrowRow(k, rings), k);
            List<Position> ring = ring(n, k);
            int from = ring.indexOf(start);
            for (int i = 0; ; i++) {
                Position p = ring.get((from + i) % ring.size());
                greenPath.add(p);
                if (p.equals(arrow)) {
                    break;
                }
            }
            greenRoles.put(arrow, SquareKind.ARROW);
        }
        if (rings > 0) {
            greenPath.add(new Position(o + 6, o)); // הכניסה ללוח הרגיל
        }
        for (Position p : innerGreenTrack()) {
            greenPath.add(p.plus(o, o));
        }
        greenRoles.put(new Position(o + 7, o), SquareKind.ARROW); // הכניסה למסלול הבית
        for (int c = 1; c <= HOME_PATH_LENGTH; c++) {
            Position p = new Position(o + 7, o + c);
            greenPath.add(p);
            greenRoles.put(p, SquareKind.HOME_PATH);
        }
        Position finish = new Position(o + 7, o + HOME_PATH_LENGTH + 1);
        greenPath.add(finish);
        greenRoles.put(finish, SquareKind.FINISH);
        greenRoles.put(greenPath.get(0), SquareKind.START);

        // 2. המסלול המשותף (כל המסלולים החיצוניים והלוח הרגיל)
        Set<Position> shared = new LinkedHashSet<Position>();
        for (int k = 0; k < rings; k++) {
            shared.addAll(ring(n, k));
        }
        for (int q = 0; q < 4; q++) {
            shared.add(new Position(6, 0).plus(o, o).rotate(n, q));
            for (Position p : innerGreenTrack()) {
                shared.add(p.plus(o, o).rotate(n, q));
            }
        }

        // 3. סיבוב התפקידים והמסלולים לכל צבע
        Map<Position, SquareKind> kinds = new HashMap<Position, SquareKind>();
        Map<Position, PlayerColor> owners = new HashMap<Position, PlayerColor>();
        Map<PlayerColor, List<Position>> colorPaths = new EnumMap<PlayerColor, List<Position>>(PlayerColor.class);
        Map<PlayerColor, Position> baseOrigins = new EnumMap<PlayerColor, Position>(PlayerColor.class);
        for (PlayerColor color : PlayerColor.values()) {
            int q = color.getQuarterTurns();
            List<Position> path = new ArrayList<Position>();
            for (Position p : greenPath) {
                path.add(p.rotate(n, q));
            }
            colorPaths.put(color, path);
            for (Map.Entry<Position, SquareKind> e : greenRoles.entrySet()) {
                Position p = e.getKey().rotate(n, q);
                kinds.put(p, e.getValue());
                owners.put(p, color);
            }
            baseOrigins.put(color, baseOrigin(n, o, q));
        }

        // 4. הגרלת משבצות הגנה, שאלה והפתעה על משבצות פנויות במסלול המשותף
        List<Position> free = new ArrayList<Position>();
        for (Position p : shared) {
            if (!kinds.containsKey(p)) {
                free.add(p);
            }
        }
        Collections.shuffle(free, random);
        Map<Position, QuestionLevel> levels = new HashMap<Position, QuestionLevel>();
        int next = 0;
        for (int i = 0; i < difficulty.getSafeSquares(); i++) {
            kinds.put(free.get(next++), SquareKind.SAFE);
        }
        for (QuestionLevel level : QuestionLevel.values()) {
            for (int i = 0; i < difficulty.getQuestionsPerLevel(); i++) {
                Position p = free.get(next++);
                kinds.put(p, SquareKind.QUESTION);
                levels.put(p, level);
            }
        }
        for (int i = 0; i < difficulty.getSurpriseSquares(); i++) {
            kinds.put(free.get(next++), SquareKind.SURPRISE);
        }

        // 5. יצירת אובייקטי המשבצות
        Map<Position, Square> squares = new HashMap<Position, Square>();
        Set<Position> all = new LinkedHashSet<Position>(shared);
        all.addAll(kinds.keySet());
        for (Position p : all) {
            SquareKind kind = kinds.containsKey(p) ? kinds.get(p) : SquareKind.REGULAR;
            squares.put(p, SquareFactory.create(kind, p, owners.get(p), levels.get(p)));
        }
        Map<PlayerColor, List<Square>> paths = new EnumMap<PlayerColor, List<Square>>(PlayerColor.class);
        for (Map.Entry<PlayerColor, List<Position>> e : colorPaths.entrySet()) {
            List<Square> path = new ArrayList<Square>();
            for (Position p : e.getValue()) {
                path.add(squares.get(p));
            }
            paths.put(e.getKey(), path);
        }
        List<Square> sharedSquares = new ArrayList<Square>();
        for (Position p : shared) {
            sharedSquares.add(squares.get(p));
        }
        return new Board(difficulty, squares, paths, baseOrigins, sharedSquares);
    }

    /**
     * השורה של משבצת החץ בעמודה השמאלית של מסלול חיצוני k. כל מסלול פנימי יותר מתחיל
     * משבצת אחת מתחת, כך שהחצים יורדים באלכסון לכיוון הלוח הרגיל, כמו בתמונות הלוחות.
     */
    private static int arrowRow(int k, int rings) {
        return rings + 6 - (rings - 1 - k);
    }

    /** משבצות המסלול החיצוני k, עם כיוון השעון, מהפינה השמאלית העליונה. */
    static List<Position> ring(int n, int k) {
        List<Position> ring = new ArrayList<Position>();
        int last = n - 1 - k;
        for (int c = k; c <= last; c++) {
            ring.add(new Position(k, c));
        }
        for (int r = k + 1; r <= last; r++) {
            ring.add(new Position(r, last));
        }
        for (int c = last - 1; c >= k; c--) {
            ring.add(new Position(last, c));
        }
        for (int r = last - 1; r >= k + 1; r--) {
            ring.add(new Position(r, k));
        }
        return ring;
    }

    /**
     * המסלול של הירוק בלוח הרגיל (קואורדינטות 15x15), מ-# ב-(6,1) עם כיוון השעון ועד החץ ב-(7,0):
     * 51 משבצות. יחד עם (6,0) הן 52 משבצות המסלול.
     */
    static List<Position> innerGreenTrack() {
        List<Position> t = new ArrayList<Position>();
        for (int c = 1; c <= 5; c++) {
            t.add(new Position(6, c));
        }
        for (int r = 5; r >= 0; r--) {
            t.add(new Position(r, 6));
        }
        t.add(new Position(0, 7));
        for (int r = 0; r <= 5; r++) {
            t.add(new Position(r, 8));
        }
        for (int c = 9; c <= 14; c++) {
            t.add(new Position(6, c));
        }
        t.add(new Position(7, 14));
        for (int c = 14; c >= 9; c--) {
            t.add(new Position(8, c));
        }
        for (int r = 9; r <= 14; r++) {
            t.add(new Position(r, 8));
        }
        t.add(new Position(14, 7));
        for (int r = 14; r >= 9; r--) {
            t.add(new Position(r, 6));
        }
        for (int c = 5; c >= 0; c--) {
            t.add(new Position(8, c));
        }
        t.add(new Position(7, 0));
        return t;
    }

    /** הפינה השמאלית העליונה של הטירה אחרי סיבוב הטירה של הירוק. */
    private static Position baseOrigin(int n, int o, int quarterTurns) {
        int s = Board.BASE_SIZE - 1;
        Position[] corners = {
                new Position(o, o), new Position(o, o + s), new Position(o + s, o), new Position(o + s, o + s)
        };
        int minR = Integer.MAX_VALUE;
        int minC = Integer.MAX_VALUE;
        for (Position c : corners) {
            Position r = c.rotate(n, quarterTurns);
            minR = Math.min(minR, r.getRow());
            minC = Math.min(minC, r.getCol());
        }
        return new Position(minR, minC);
    }
}

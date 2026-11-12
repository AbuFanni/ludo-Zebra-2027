package Model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.LongSupplier;

/**
 * מצב המשחק וחוקי התור: הטלת קובייה, מהלכים חוקיים, תנועה, אכילה, Double Block, סיום ודירוג
 * (FR-15, FR-19 עד FR-23).
 * <p>
 * החוקים מבוססים על {@code GameplayAndLogic.Gameplay} בפרויקט Ludo-Griniaris-Game (MIT):
 * {@code checkValidMoves()} (בדיקת חסימה משבצת אחר משבצת), {@code mouseClicked()} (תנועה, אכילה
 * והחזרה לטירה, תור נוסף ב-6) ו-{@code isGameFinished()}. שם הכול היה בתוך מאזין עכבר של Swing
 * עם חלונות הודעה; כאן החוקים הם מודל נקי שמחזיר תוצאות ומודיע למאזינים (Observer), והחוקים
 * עצמם הותאמו למסמך הפרויקט שלנו ולתשובות המתרגלים:
 * <ul>
 *   <li>יוצאים מהטירה רק ב-6 (במקור 5). כלי שנאכל חוזר לטירה.</li>
 *   <li>6 נותן הטלה נוספת, גם כשאין מהלך. 6 שלישי ברצף מבוטל והתור עובר.</li>
 *   <li>Double Block (שני כלים או יותר של אותו שחקן) חוסם מעבר ונחיתה של יריבים. אם אין אף כלי
 *       שיכול לזוז את כל הצעדים, כלי שנתקל בבלוק נעצר במשבצת שלפניו.</li>
 *   <li>אין אכילה על משבצת מוגנת (כוכב, #, הפתעה). נכנסים לבית רק בתוצאה מדויקת.</li>
 *   <li>חובה לזוז כשיש מהלך חוקי. כל הטלה מזיזה כלי אחד.</li>
 *   <li>המשחק נגמר כשכל השחקנים חוץ מאחד סיימו; האחרון מקבל את המקום האחרון.</li>
 * </ul>
 */
public class Game {

    private final Board board;
    private final List<Player> players;
    private final Die die;
    private final LongSupplier clock;
    private final List<GameListener> listeners = new CopyOnWriteArrayList<GameListener>();
    private final List<Player> ranking = new ArrayList<Player>();

    private int current;
    private GamePhase phase = GamePhase.ROLL;
    private int lastRoll;
    private int sixesInRow;
    private List<Move> legalMoves = Collections.emptyList();
    private final long startMillis;
    private long endMillis = -1;

    /**
     * @param board   הלוח
     * @param players השחקנים לפי סדר התורות (FR-07)
     * @param die     הקובייה
     * @param clock   מקור הזמן במילישניות (System::currentTimeMillis, או שעון קבוע בבדיקות)
     */
    public Game(Board board, List<Player> players, Die die, LongSupplier clock) {
        if (players.size() < 2 || players.size() > 4) {
            throw new IllegalArgumentException("Game needs 2 to 4 players");
        }
        this.board = board;
        this.players = Collections.unmodifiableList(new ArrayList<Player>(players));
        this.die = die;
        this.clock = clock;
        this.startMillis = clock.getAsLong();
    }

    // ---------------------------------------------------------------- Observer

    public void addListener(GameListener l) {
        listeners.add(l);
    }

    public void removeListener(GameListener l) {
        listeners.remove(l);
    }

    // ---------------------------------------------------------------- מצב

    public Board getBoard() {
        return board;
    }

    public List<Player> getPlayers() {
        return players;
    }

    public Player getCurrentPlayer() {
        return players.get(current);
    }

    public GamePhase getPhase() {
        return phase;
    }

    /** @return התוצאה האחרונה של הקובייה, או 0 לפני ההטלה הראשונה */
    public int getLastRoll() {
        return lastRoll;
    }

    /** @return המהלכים החוקיים של השחקן הנוכחי אחרי ההטלה (ריק אם צריך להטיל) */
    public List<Move> getLegalMoves() {
        return legalMoves;
    }

    /** @return השחקנים לפי סדר הסיום, כולל המקום האחרון בסוף המשחק */
    public List<Player> getRanking() {
        return Collections.unmodifiableList(ranking);
    }

    public boolean isOver() {
        return phase == GamePhase.OVER;
    }

    /** @return כמה זמן עבר מתחילת המשחק, עד הסיום אם הסתיים */
    public long getElapsedMillis() {
        long end = endMillis >= 0 ? endMillis : clock.getAsLong();
        return end - startMillis;
    }

    public long getStartMillis() {
        return startMillis;
    }

    /** @return המשבצת שעליה הכלי עומד, או null אם הוא בטירה */
    public Square squareOf(Pawn pawn) {
        if (pawn.isInBase()) {
            return null;
        }
        return board.getPath(pawn.getColor()).get(pawn.getPathIndex());
    }

    /** @return true אם הכלי הגיע לבית הסיום */
    public boolean isFinished(Pawn pawn) {
        return pawn.getPathIndex() == board.getFinishIndex();
    }

    /** @return כל הכלים שעומדים על המשבצת */
    public List<Pawn> pawnsAt(Square square) {
        List<Pawn> result = new ArrayList<Pawn>();
        for (Player p : players) {
            for (Pawn pawn : p.getPawns()) {
                if (squareOf(pawn) == square) {
                    result.add(pawn);
                }
            }
        }
        return result;
    }

    /**
     * @return true אם יש על המשבצת Double Block של יריב של mover: לפחות שני כלים של אותו יריב.
     *         הבלוק חוסם רק יריבים; השחקן עצמו עובר דרכו.
     */
    public boolean isBlockedFor(Square square, Player mover) {
        if (square.isPrivate()) {
            return false;
        }
        Map<Player, Integer> counts = new HashMap<Player, Integer>();
        for (Pawn pawn : pawnsAt(square)) {
            if (pawn.getOwner() != mover) {
                Integer c = counts.get(pawn.getOwner());
                int n = (c == null ? 0 : c) + 1;
                if (n >= 2) {
                    return true;
                }
                counts.put(pawn.getOwner(), n);
            }
        }
        return false;
    }

    // ---------------------------------------------------------------- תור

    /**
     * Rolls the step die for the current player (FR-15). If there is no legal move and no extra
     * roll, the turn passes (FR-19); a 6 with no legal move rolls again (FR-17). A third 6 in a row
     * is cancelled and the turn passes (FR-18).
     *
     * @return what was rolled and what happens now
     * @throws IllegalStateException if it is not the roll phase
     */
    public RollResult roll() {
        if (phase != GamePhase.ROLL) {
            throw new IllegalStateException("Not the roll phase: " + phase);
        }
        Player player = getCurrentPlayer();
        // FR-15: throw the step die once and keep the value for the screen
        int value = die.roll();
        lastRoll = value;
        // FR-18: count the 6s this player rolled in a row
        sixesInRow = (value == 6) ? sixesInRow + 1 : 0;

        RollResult result;
        // FR-18: a third 6 in a row is cancelled and the turn passes
        if (value == 6 && sixesInRow == 3) {
            legalMoves = Collections.emptyList();
            result = new RollResult(player, value, true, false, false);
            fireRolled(result);
            advanceTurn();
            return result;
        }

        legalMoves = computeLegalMoves(player, value);
        if (!legalMoves.isEmpty()) {
            // at least one legal move: wait for the player to pick a piece
            phase = GamePhase.MOVE;
            result = new RollResult(player, value, false, true, false);
            fireRolled(result);
        } else if (value == 6) {
            // FR-17: a 6 with no legal move, so the same player rolls again
            result = new RollResult(player, value, false, false, true);
            fireRolled(result);
        } else {
            // FR-19: no legal move and no extra roll, so the turn passes
            result = new RollResult(player, value, false, false, false);
            fireRolled(result);
            advanceTurn();
        }
        return result;
    }

    /**
     * @return המהלך החוקי של הכלי, או null אם הכלי לא יכול לזוז בתוצאה הנוכחית
     */
    public Move legalMoveFor(Pawn pawn) {
        for (Move m : legalMoves) {
            if (m.getPawn() == pawn) {
                return m;
            }
        }
        // כמה כלים בטירה: המהלך נשמר לכלי אחד מהם, וכל כלי בטירה מייצג אותו
        if (pawn.isInBase()) {
            for (Move m : legalMoves) {
                if (m.getPawn().isInBase() && m.getPawn().getOwner() == pawn.getOwner()) {
                    return m;
                }
            }
        }
        return null;
    }

    /**
     * מבצע מהלך חוקי (FR-20 עד FR-22): מזיז את הכלי, אוכל כלי יחיד של יריב במשבצת לא מוגנת,
     * מעדכן סיום ודירוג, ונותן הטלה נוספת אם יצא 6.
     *
     * @param move אחד מהמהלכים ב-{@link #getLegalMoves()}
     * @return מה קרה
     */
    public MoveResult move(Move move) {
        if (phase != GamePhase.MOVE) {
            throw new IllegalStateException("Not the move phase: " + phase);
        }
        if (!legalMoves.contains(move)) {
            throw new IllegalArgumentException("Illegal move: " + move);
        }
        Player player = getCurrentPlayer();
        Pawn pawn = move.getPawn();
        pawn.setPathIndex(move.getTo());
        legalMoves = Collections.emptyList();

        Square target = squareOf(pawn);
        List<Pawn> captured = new ArrayList<Pawn>();
        if (!target.isSafe()) {
            Map<Player, List<Pawn>> byOwner = new HashMap<Player, List<Pawn>>();
            for (Pawn other : pawnsAt(target)) {
                if (other.getOwner() != player) {
                    List<Pawn> list = byOwner.get(other.getOwner());
                    if (list == null) {
                        list = new ArrayList<Pawn>();
                        byOwner.put(other.getOwner(), list);
                    }
                    list.add(other);
                }
            }
            for (List<Pawn> list : byOwner.values()) {
                if (list.size() == 1) {
                    list.get(0).sendToBase();
                    captured.add(list.get(0));
                }
            }
        }

        boolean reachedFinish = isFinished(pawn);
        boolean playerFinished = false;
        if (reachedFinish && allFinished(player)) {
            playerFinished = true;
            ranking.add(player);
            player.setPlace(ranking.size());
        }

        boolean gameOver = checkGameOver();
        // FR-17: a 6 gives another roll, unless the game ended or this player has just finished.
        // Otherwise the turn passes (FR-19).
        boolean extraTurn = !gameOver && lastRoll == 6 && !player.isDone();
        MoveResult result = new MoveResult(move, captured, reachedFinish, playerFinished, extraTurn, gameOver);
        for (GameListener l : listeners) {
            l.onMoved(result);
        }

        if (gameOver) {
            phase = GamePhase.OVER;
            endMillis = clock.getAsLong();
            for (GameListener l : listeners) {
                l.onGameOver(getRanking());
            }
        } else if (extraTurn) {
            phase = GamePhase.ROLL;
        } else {
            advanceTurn();
        }
        return result;
    }

    // ---------------------------------------------------------------- עזר

    /**
     * המהלכים החוקיים לתוצאה. מהלכים מלאים קודמים; רק אם אין אף מהלך מלא, כלי שנתקל ב-Double Block
     * נעצר במשבצת שלפניו (תשובות המתרגלים על Double Block).
     */
    List<Move> computeLegalMoves(Player player, int value) {
        List<Square> path = board.getPath(player.getColor());
        int finish = board.getFinishIndex();
        List<Move> full = new ArrayList<Move>();
        List<Move> partial = new ArrayList<Move>();
        boolean baseMoveAdded = false;

        for (Pawn pawn : player.getPawns()) {
            if (pawn.isInBase()) {
                if (value == 6 && !baseMoveAdded && !isBlockedFor(path.get(0), player)) {
                    full.add(new Move(pawn, Pawn.IN_BASE, 0, false));
                    baseMoveAdded = true;
                }
                continue;
            }
            int from = pawn.getPathIndex();
            if (from == finish) {
                continue;
            }
            int to = from + value;
            if (to > finish) {
                continue; // נכנסים לבית רק בתוצאה מדויקת
            }
            int blockedAt = -1;
            for (int i = from + 1; i <= to; i++) {
                if (isBlockedFor(path.get(i), player)) {
                    blockedAt = i;
                    break;
                }
            }
            if (blockedAt < 0) {
                full.add(new Move(pawn, from, to, false));
            } else if (blockedAt - 1 > from) {
                partial.add(new Move(pawn, from, blockedAt - 1, true));
            }
        }
        return full.isEmpty() ? partial : full;
    }

    private boolean allFinished(Player player) {
        for (Pawn pawn : player.getPawns()) {
            if (!isFinished(pawn)) {
                return false;
            }
        }
        return true;
    }

    /** המשחק נגמר כשנשאר רק שחקן אחד שלא סיים; הוא מקבל את המקום האחרון. */
    private boolean checkGameOver() {
        List<Player> left = new ArrayList<Player>();
        for (Player p : players) {
            if (!p.isDone()) {
                left.add(p);
            }
        }
        if (left.size() <= 1) {
            for (Player p : left) {
                ranking.add(p);
                p.setPlace(ranking.size());
            }
            return true;
        }
        return false;
    }

    /**
     * Passes the turn to the next player in entry order (FR-07, FR-19). Players who finished are
     * skipped, and the new player's 6s are counted from zero (FR-18).
     */
    private void advanceTurn() {
        sixesInRow = 0;
        legalMoves = Collections.emptyList();
        do {
            current = (current + 1) % players.size();
        } while (players.get(current).isDone());
        phase = GamePhase.ROLL;
        for (GameListener l : listeners) {
            l.onTurnChanged(getCurrentPlayer());
        }
    }

    private void fireRolled(RollResult result) {
        for (GameListener l : listeners) {
            l.onRolled(result);
        }
    }
}

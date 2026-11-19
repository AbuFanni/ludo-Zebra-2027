package Model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * משבצות ההפתעה (US-8, FR-44 עד FR-52): הגרלת האירוע כשכלי נוחת על {@code !}, וביצוע ששת
 * האירועים.
 * <p>
 * לפי ההצעה בקבוצה, כל הלוגיקה של ההפתעות נמצאת כאן ולא ב-{@link Game}. ב-{@code Game} נשארו רק
 * נקודות חיבור קטנות: נחיתה ({@link #draw(Pawn)}), הטלה נוספת ({@link #extraRollDue(int)}), תחילת
 * הטלה ({@link #onRoll()}) ודילוג על תור ({@link #skipsTurn(Player)}). התנועה עצמה משתמשת בכלים
 * הקיימים של {@code Game} (אכילה, תנועה אחורה, שאלות, סיום), בלי לשנות אותם.
 *
 * @author Nour Eldin Khatib (Zebra) – Iteration 2, US-8 (FR-44 עד FR-52)
 */
public final class SurpriseHandler {

    private final Game game;
    private final SurpriseDeck deck;
    /** שחקנים שהפסידו את התור הבא שלהם (FR-50). */
    private final Set<Player> skipNextTurn = new HashSet<Player>();

    private SurpriseEvent pending;
    private Pawn pawn;
    /** יצא 6 בהטלת ה-UltraBoost, ולכן מגיעה הטלה נוספת (FR-52). */
    private boolean specialSix;
    /** מספר הצעדים בהטלת ה-UltraBoost בזמן שבוחרים כלי, אחרת -1. */
    private int specialSteps = -1;

    /**
     * @param game המשחק
     * @param deck מקור האירועים וקוביית ה-UltraBoost, או null בלי הפתעות
     */
    SurpriseHandler(Game game, SurpriseDeck deck) {
        this.game = game;
        this.deck = deck;
    }

    // ---------------------------------------------------------------- מצב

    /** @return האירוע שמחכה בשלב {@link GamePhase#SURPRISE}, אחרת null */
    public SurpriseEvent getPendingSurprise() {
        return pending;
    }

    /** @return הכלי שעל משבצת ההפתעה; האירוע חל על בעליו (FR-27), או null */
    public Pawn getSurprisePawn() {
        return pawn;
    }

    /** @return מספר הצעדים בהטלת UltraBoost בזמן שבוחרים כלי אחריה; אחרת -1 */
    public int getSpecialSteps() {
        return specialSteps;
    }

    /** @return true אם השחקן הפסיד את התור הבא שלו, ועוד לא דילגו עליו (FR-50) */
    public boolean losesNextTurn(Player player) {
        return skipNextTurn.contains(player);
    }

    // ---------------------------------------------------------------- נקודות החיבור ב-Game

    /**
     * נקרא מ-{@link Game} כשכלי מגיע למשבצת. אם זו משבצת הפתעה ויש הפתעות במשחק: מגריל אירוע
     * (FR-44) ועובר לשלב ההפתעה.
     *
     * @return true אם הוגרל אירוע
     */
    boolean draw(Pawn landed) {
        if (deck == null || !(game.squareOf(landed) instanceof SurpriseSquare)) {
            return false;
        }
        pending = deck.draw();
        pawn = landed;
        game.setPhase(GamePhase.SURPRISE);
        return true;
    }

    /** מודיע למאזינים שהוגרל אירוע. */
    void fireDrawn() {
        for (GameListener l : game.getListeners()) {
            l.onSurprise(pending, pawn);
        }
    }

    /** נקרא בתחילת כל הטלה רגילה: ההטלה המיוחדת כבר נוצלה. */
    void onRoll() {
        specialSix = false;
        specialSteps = -1;
    }

    /**
     * @param lastRoll ההטלה האחרונה של קוביית הצעדים
     * @return true אם לשחקן שתורו מגיעה הטלה נוספת: יצא 6 (FR-17) או 6 בהטלת UltraBoost (FR-52),
     *         והוא לא הפסיד את התור הבא (FR-50)
     */
    boolean extraRollDue(int lastRoll) {
        return (lastRoll == 6 || specialSix) && !skipNextTurn.contains(game.getCurrentPlayer());
    }

    /**
     * נקרא כשהתור עובר לשחקן: אם הוא הפסיד את התור הבא, מדלגים עליו פעם אחת ומודיעים (FR-50).
     *
     * @return true אם מדלגים עליו
     */
    boolean skipsTurn(Player next) {
        specialSix = false;
        specialSteps = -1;
        if (!skipNextTurn.remove(next)) {
            return false;
        }
        for (GameListener l : game.getListeners()) {
            l.onTurnSkipped(next);
        }
        return true;
    }

    // ---------------------------------------------------------------- האירועים

    /**
     * מבצע אירוע שלא דורש בחירה (FR-44): קפיצה לבית (FR-45), חזרה לטירה (FR-48), חזרה 3 צעדים
     * (FR-49) או הפסד התור הבא (FR-50). אי אפשר לוותר על האירוע. "הזזת כלי יריב" עוברת כאן רק
     * כשלאף יריב אין כלי להזיז, ואז היא מבוטלת (FR-47).
     *
     * @throws IllegalStateException אם אין הפתעה שמחכה, או שהאירוע דורש בחירה
     */
    public SurpriseResult resolveSurprise() {
        requireSurprise();
        SurpriseEvent event = pending;
        Pawn p = pawn;
        Player owner = p.getOwner();
        List<Pawn> none = Collections.emptyList();
        int from = p.getPathIndex();
        switch (event) {
            case JUMP_TO_FINISH: {
                // ישר לבית הסיום: בלי מספר מדויק ובלי עצירה ב-Double Block (FR-45)
                clear();
                p.setPathIndex(game.getBoard().getFinishIndex());
                boolean playerFinished = game.recordFinish(owner);
                return end(event, p, p, from, p.getPathIndex(), 0, 0, false,
                        AnswerResult.Limit.NONE, none, true, playerFinished);
            }
            case BACK_TO_BASE:
                clear();
                p.sendToBase();
                return end(event, p, p, from, Pawn.IN_BASE, 0, 0, false, AnswerResult.Limit.NONE, none, false, false);
            case BACK_THREE:
                return moveBack(event, p, p, 3, 0);
            case LOSE_NEXT_TURN:
                clear();
                skipNextTurn.add(owner);
                return end(event, p, null, from, from, 0, 0, false, AnswerResult.Limit.NONE, none, false, false);
            case MOVE_OPPONENT:
                if (!surpriseOpponents().isEmpty()) {
                    throw new IllegalStateException("Choose an opponent's pawn: moveOpponent(Pawn)");
                }
                clear();
                return end(event, p, null, from, from, 0, 0, true, AnswerResult.Limit.NONE, none, false, false);
            default:
                throw new IllegalStateException("Roll for UltraBoost: rollUltraBoost()");
        }
    }

    /**
     * היריבים שאפשר לבחור ב"הזזת כלי יריב": מי שיש לו כלי שאפשר להזיז אחורה במסלול המשותף
     * (FR-46, FR-47).
     */
    public List<Player> surpriseOpponents() {
        if (game.getPhase() != GamePhase.SURPRISE || pending != SurpriseEvent.MOVE_OPPONENT) {
            return Collections.emptyList();
        }
        List<Player> result = new ArrayList<Player>();
        for (Player p : game.getPlayers()) {
            if (p != pawn.getOwner() && !surpriseTargets(p).isEmpty()) {
                result.add(p);
            }
        }
        return result;
    }

    /**
     * הכלים של היריב שאפשר להזיז: על המסלול המשותף, לא בטירה, לא במסלול הבית ולא על משבצת
     * ההתחלה (משם אין לאן לחזור, FR-26).
     */
    public List<Pawn> surpriseTargets(Player opponent) {
        List<Pawn> result = new ArrayList<Pawn>();
        for (Pawn p : opponent.getPawns()) {
            if (!p.isInBase() && p.getPathIndex() > 0 && !game.squareOf(p).isPrivate()) {
                result.add(p);
            }
        }
        return result;
    }

    /**
     * "הזזת כלי יריב" (FR-46): הטלה חדשה של קוביית הצעדים, והכלי שנבחר זז אחורה בחצי מהתוצאה,
     * מעוגל למעלה (1-2 → 1, 3-4 → 2, 5-6 → 3), לפי FR-26 ו-FR-33, עם אכילה והפעלת המשבצת
     * שהגיע אליה לבעליו (FR-27, FR-29).
     */
    public SurpriseResult moveOpponent(Pawn target) {
        requireSurprise();
        if (pending != SurpriseEvent.MOVE_OPPONENT) {
            throw new IllegalStateException("Not a move-opponent surprise: " + pending);
        }
        if (target.getOwner() == pawn.getOwner() || !surpriseTargets(target.getOwner()).contains(target)) {
            throw new IllegalArgumentException("Not an opponent's pawn on the shared track: " + target);
        }
        int roll = game.getDie().roll();
        return moveBack(SurpriseEvent.MOVE_OPPONENT, pawn, target, (roll + 1) / 2, roll);
    }

    /**
     * אירוע "UltraBoost" (FR-51): הטלה של קוביית צעדים ו-UltraBoost, בכל רמה ובלי קשר ל-6 קודמים.
     * הצעדים: קוביית הצעדים כפול (UltraBoost פחות 1) (FR-16). אם יש מהלך, המשחק עובר לשלב
     * {@link GamePhase#MOVE} ובעל הכלי בוחר כלי ב-{@link Game#move(Move)}; ב-6 אפשר גם להוציא כלי
     * מהטירה. 6 בהטלה הזו נותן הטלה נוספת, בלי לגעת ברצף ה-6 (FR-52).
     */
    public SurpriseResult rollUltraBoost() {
        requireSurprise();
        if (pending != SurpriseEvent.ULTRA_BOOST) {
            throw new IllegalStateException("Not an UltraBoost surprise: " + pending);
        }
        Pawn p = pawn;
        Player owner = p.getOwner();
        clear();
        int step = game.getDie().roll();
        int ultra = deck.rollUltraBoost();
        int steps = step * (ultra - 1);
        if (step == 6 && owner == game.getCurrentPlayer()) {
            specialSix = true;
        }
        List<Move> moves = ultraBoostMoves(owner, step, steps);
        List<Pawn> none = Collections.emptyList();
        if (!moves.isEmpty()) {
            game.setLegalMoves(moves);
            game.setPhase(GamePhase.MOVE);
            specialSteps = steps;
            SurpriseResult result = new SurpriseResult(SurpriseEvent.ULTRA_BOOST, p, null, Pawn.IN_BASE,
                    Pawn.IN_BASE, step, ultra, false, AnswerResult.Limit.NONE, none, false, false, true,
                    false, false, true);
            fire(result);
            return result;
        }
        return end(SurpriseEvent.ULTRA_BOOST, p, null, Pawn.IN_BASE, Pawn.IN_BASE, step, ultra, false,
                AnswerResult.Limit.NONE, none, false, false);
    }

    // ---------------------------------------------------------------- עזר

    /**
     * המהלכים בהטלת UltraBoost, בעזרת {@code Game.computeLegalMoves} הקיימת ובלי לשנות אותה:
     * הזזת כלים על הלוח בכל הצעדים, והוצאה מהטירה רק כשבקוביית הצעדים יצא 6 (FR-20).
     */
    private List<Move> ultraBoostMoves(Player owner, int step, int steps) {
        List<Move> board = new ArrayList<Move>();
        if (steps > 0) {
            for (Move m : game.computeLegalMoves(owner, steps)) {
                if (m.getFrom() != Pawn.IN_BASE) {
                    board.add(m);
                }
            }
        }
        Move leave = null;
        if (step == 6) {
            for (Move m : game.computeLegalMoves(owner, 6)) {
                if (m.getFrom() == Pawn.IN_BASE) {
                    leave = m;
                }
            }
        }
        List<Move> result = new ArrayList<Move>();
        boolean anyFull = leave != null;
        for (Move m : board) {
            anyFull |= !m.isStoppedByBlock();
        }
        for (Move m : board) {
            // עצירה לפני Double Block רק אם אין אף מהלך מלא (FR-35)
            if (!anyFull || !m.isStoppedByBlock()) {
                result.add(m);
            }
        }
        if (leave != null) {
            result.add(leave);
        }
        return result;
    }

    /** מזיז כלי אחורה (FR-26, FR-29, FR-33), ומפעיל את המשבצת שהגיע אליה (FR-27). */
    private SurpriseResult moveBack(SurpriseEvent event, Pawn p, Pawn moved, int steps, int roll) {
        clear();
        int from = moved.getPathIndex();
        int[] target = new int[1];
        AnswerResult.Limit limit = game.questionTarget(moved, -steps, target);
        int to = target[0];
        moved.setPathIndex(to);
        List<Pawn> captured = Collections.emptyList();
        if (to != from) {
            captured = game.captureAt(moved.getOwner(), game.squareOf(moved));
            Question next = game.drawQuestion(moved);
            boolean surprise = next == null && draw(moved);
            if (next != null || surprise) {
                SurpriseResult result = new SurpriseResult(event, p, moved, from, to, roll, 0, false, limit,
                        captured, false, false, false, false, false, true);
                fire(result);
                if (surprise) {
                    fireDrawn();
                }
                return result;
            }
        }
        return end(event, p, moved, from, to, roll, 0, false, limit, captured, false, false);
    }

    /** סוף אירוע: סיום משחק, הטלה נוספת או מעבר תור. */
    private SurpriseResult end(SurpriseEvent event, Pawn p, Pawn moved, int from, int to, int stepRoll,
                               int ultraRoll, boolean cancelled, AnswerResult.Limit limit, List<Pawn> captured,
                               boolean reachedFinish, boolean playerFinished) {
        boolean gameOver = game.checkGameOver();
        boolean extraTurn = !gameOver && extraRollDue(game.getLastRoll()) && !game.getCurrentPlayer().isDone();
        SurpriseResult result = new SurpriseResult(event, p, moved, from, to, stepRoll, ultraRoll, cancelled,
                limit, captured, reachedFinish, playerFinished, false, extraTurn, gameOver, false);
        fire(result);
        game.endMove(gameOver, extraTurn);
        return result;
    }

    private void requireSurprise() {
        if (game.getPhase() != GamePhase.SURPRISE) {
            throw new IllegalStateException("No surprise waiting: " + game.getPhase());
        }
    }

    private void clear() {
        pending = null;
        pawn = null;
    }

    private void fire(SurpriseResult result) {
        for (GameListener l : game.getListeners()) {
            l.onSurpriseResolved(result);
        }
    }
}

package Model;

import java.util.Collections;
import java.util.List;

/**
 * מה קרה באירוע הפתעה: איזה כלי זז ולאן, מי נאכל, ומה קורה עכשיו (FR-44 עד FR-52).
 *
 * @author Nour Eldin Khatib (Zebra) – Iteration 2, US-8
 */
public final class SurpriseResult {

    private final SurpriseEvent event;
    private final Pawn pawn;
    private final Pawn movedPawn;
    private final int from;
    private final int to;
    private final int stepRoll;
    private final int ultraBoostRoll;
    private final boolean cancelled;
    private final AnswerResult.Limit limit;
    private final List<Pawn> captured;
    private final boolean reachedFinish;
    private final boolean playerFinished;
    private final boolean hasMoves;
    private final boolean extraTurn;
    private final boolean gameOver;
    private final boolean continues;

    SurpriseResult(SurpriseEvent event, Pawn pawn, Pawn movedPawn, int from, int to, int stepRoll,
                   int ultraBoostRoll, boolean cancelled, AnswerResult.Limit limit, List<Pawn> captured,
                   boolean reachedFinish, boolean playerFinished, boolean hasMoves, boolean extraTurn,
                   boolean gameOver, boolean continues) {
        this.event = event;
        this.pawn = pawn;
        this.movedPawn = movedPawn;
        this.from = from;
        this.to = to;
        this.stepRoll = stepRoll;
        this.ultraBoostRoll = ultraBoostRoll;
        this.cancelled = cancelled;
        this.limit = limit;
        this.captured = Collections.unmodifiableList(captured);
        this.reachedFinish = reachedFinish;
        this.playerFinished = playerFinished;
        this.hasMoves = hasMoves;
        this.extraTurn = extraTurn;
        this.gameOver = gameOver;
        this.continues = continues;
    }

    public SurpriseEvent getEvent() {
        return event;
    }

    /** @return הכלי שנחת על משבצת ההפתעה; האירוע חל על בעליו (FR-27) */
    public Pawn getPawn() {
        return pawn;
    }

    /** @return הכלי שזז באירוע (הכלי עצמו, או כלי היריב ב"הזזת כלי יריב"), או null אם אף כלי לא זז */
    public Pawn getMovedPawn() {
        return movedPawn;
    }

    /** @return המקום במסלול של הכלי שזז, לפני האירוע ({@link Pawn#IN_BASE} בטירה) */
    public int getFrom() {
        return from;
    }

    /** @return המקום במסלול של הכלי שזז, אחרי האירוע */
    public int getTo() {
        return to;
    }

    /** @return תוצאת קוביית הצעדים באירוע (הזזת יריב או UltraBoost), או 0 */
    public int getStepRoll() {
        return stepRoll;
    }

    /** @return תוצאת קוביית ה-UltraBoost, או 0 */
    public int getUltraBoostRoll() {
        return ultraBoostRoll;
    }

    /** @return מספר הצעדים בהטלת ה-UltraBoost: קוביית הצעדים כפול (UltraBoost פחות 1) (FR-16) */
    public int getUltraBoostSteps() {
        return stepRoll * Math.max(0, ultraBoostRoll - 1);
    }

    /** @return true אם האירוע בוטל כי לאף יריב אין כלי להזיז (FR-47) */
    public boolean isCancelled() {
        return cancelled;
    }

    /** @return למה הכלי זז פחות מהמתוכנן: משבצת ההתחלה או Double Block, או NONE */
    public AnswerResult.Limit getLimit() {
        return limit;
    }

    public List<Pawn> getCaptured() {
        return captured;
    }

    public boolean isReachedFinish() {
        return reachedFinish;
    }

    public boolean isPlayerFinished() {
        return playerFinished;
    }

    /** @return ב-UltraBoost: true אם יש מהלך חוקי, והשחקן צריך לבחור כלי */
    public boolean hasMoves() {
        return hasMoves;
    }

    /** @return true אם השחקן שתורו מטיל שוב */
    public boolean isExtraTurn() {
        return extraTurn;
    }

    public boolean isGameOver() {
        return gameOver;
    }

    /**
     * @return true אם התור עוד לא נגמר: השחקן בוחר כלי (UltraBoost), או שהכלי נחת על משבצת שאלה או
     *         הפתעה נוספת (FR-27)
     */
    public boolean isContinues() {
        return continues;
    }
}

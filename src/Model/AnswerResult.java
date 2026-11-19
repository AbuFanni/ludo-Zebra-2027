package Model;

import java.util.Collections;
import java.util.List;

/**
 * מה קרה אחרי תשובה לשאלה (FR-40): בדיקת התשובה, לאן הכלי זז, מי נאכל, ומה קורה עכשיו בתור.
 *
 * @author Mohamed Shini (Zebra) – Iteration 2, US-7 (FR-38–FR-43)
 */
public final class AnswerResult {

    /** למה הכלי זז פחות צעדים ממה שהתשובה נתנה. */
    public enum Limit {
        /** הכלי זז את כל הצעדים. */
        NONE,
        /** נכנסים לבית רק במספר מדויק, ולכן הכלי לא זז (FR-24). */
        FINISH_NOT_EXACT,
        /** הכלי נעצר לפני Double Block של יריב (FR-33). */
        DOUBLE_BLOCK,
        /** אחורה הכלי חוזר עד משבצת ההתחלה שלו ולא מעבר לה (FR-26). */
        START_SQUARE
    }

    private final AnswerCheck check;
    private final Pawn pawn;
    private final int from;
    private final int to;
    private final Limit limit;
    private final List<Pawn> captured;
    private final boolean reachedFinish;
    private final boolean playerFinished;
    private final boolean extraTurn;
    private final boolean gameOver;
    private final Question nextQuestion;

    AnswerResult(AnswerCheck check, Pawn pawn, int from, int to, Limit limit, List<Pawn> captured,
                 boolean reachedFinish, boolean playerFinished, boolean extraTurn, boolean gameOver,
                 Question nextQuestion) {
        this.check = check;
        this.pawn = pawn;
        this.from = from;
        this.to = to;
        this.limit = limit;
        this.captured = Collections.unmodifiableList(captured);
        this.reachedFinish = reachedFinish;
        this.playerFinished = playerFinished;
        this.extraTurn = extraTurn;
        this.gameOver = gameOver;
        this.nextQuestion = nextQuestion;
    }

    public AnswerCheck getCheck() {
        return check;
    }

    public Pawn getPawn() {
        return pawn;
    }

    /** @return המקום במסלול לפני התשובה */
    public int getFrom() {
        return from;
    }

    /** @return המקום במסלול אחרי התשובה */
    public int getTo() {
        return to;
    }

    /** @return כמה צעדים הכלי זז בפועל: חיובי קדימה, שלילי אחורה, 0 אם לא זז */
    public int getStepsMoved() {
        return to - from;
    }

    public Limit getLimit() {
        return limit;
    }

    /** @return כלי היריבים שנאכלו במשבצת שהכלי הגיע אליה (FR-29) */
    public List<Pawn> getCaptured() {
        return captured;
    }

    public boolean isReachedFinish() {
        return reachedFinish;
    }

    public boolean isPlayerFinished() {
        return playerFinished;
    }

    /** @return true אם השחקן מטיל שוב (ההטלה שהביאה אותו לשאלה הייתה 6) */
    public boolean isExtraTurn() {
        return extraTurn;
    }

    public boolean isGameOver() {
        return gameOver;
    }

    /** @return השאלה הבאה אם הכלי נחת על עוד משבצת שאלה (FR-27), אחרת null */
    public Question getNextQuestion() {
        return nextQuestion;
    }
}

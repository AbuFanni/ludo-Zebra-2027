package Model;

import java.util.Collections;
import java.util.List;

/**
 * מה קרה אחרי מהלך: מי נאכל, האם הכלי הגיע לבית, האם השחקן סיים ומי משחק עכשיו.
 */
public final class MoveResult {

    private final Move move;
    private final List<Pawn> captured;
    private final boolean reachedFinish;
    private final boolean playerFinished;
    private final boolean extraTurn;
    private final boolean gameOver;
    private final Question question;

    MoveResult(Move move, List<Pawn> captured, boolean reachedFinish, boolean playerFinished,
               boolean extraTurn, boolean gameOver) {
        this(move, captured, reachedFinish, playerFinished, extraTurn, gameOver, null);
    }

    MoveResult(Move move, List<Pawn> captured, boolean reachedFinish, boolean playerFinished,
               boolean extraTurn, boolean gameOver, Question question) {
        this.move = move;
        this.captured = Collections.unmodifiableList(captured);
        this.reachedFinish = reachedFinish;
        this.playerFinished = playerFinished;
        this.extraTurn = extraTurn;
        this.gameOver = gameOver;
        this.question = question;
    }

    public Move getMove() {
        return move;
    }

    /** @return כלי היריבים שנאכלו וחזרו לטירה */
    public List<Pawn> getCaptured() {
        return captured;
    }

    public boolean isReachedFinish() {
        return reachedFinish;
    }

    /** @return true אם כל ארבעת הכלים של השחקן הגיעו לבית */
    public boolean isPlayerFinished() {
        return playerFinished;
    }

    /** @return true אם השחקן מקבל הטלה נוספת (יצא 6) */
    public boolean isExtraTurn() {
        return extraTurn;
    }

    public boolean isGameOver() {
        return gameOver;
    }

    /**
     * @return השאלה שצריך לענות עליה אם הכלי נחת על משבצת שאלה (FR-38), אחרת null. כשיש שאלה, התור
     *         ממשיך רק אחרי {@link Game#answer(int)}.
     */
    public Question getQuestion() {
        return question;
    }
}

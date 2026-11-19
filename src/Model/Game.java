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
 *   <li>כלי שנוחת על משבצת שאלה עוצר את התור עד שבעליו עונה (שלב {@link GamePhase#QUESTION}),
 *       והתשובה מזיזה אותו קדימה או אחורה (FR-38 עד FR-43).</li>
 *   <li>כלי שנוחת על משבצת הפתעה מעביר את הטיפול ל-{@link SurpriseHandler} (FR-44 עד FR-52). כאן
 *       נשארו רק נקודות החיבור (hooks): הנחיתה, הטלה נוספת ודילוג על תור.</li>
 * </ul>
 */
public class Game {

    private final Board board;
    private final List<Player> players;
    private final Die die;
    private final LongSupplier clock;
    private final List<GameListener> listeners = new CopyOnWriteArrayList<GameListener>();
    private final List<Player> ranking = new ArrayList<Player>();
    private final QuestionDeck questions;
    /** משבצות ההפתעה; כל הלוגיקה שלהן במחלקה משלה (US-8). */
    private final SurpriseHandler surprises;

    private int current;
    private GamePhase phase = GamePhase.ROLL;
    private int lastRoll;
    private int sixesInRow;
    private List<Move> legalMoves = Collections.emptyList();
    private final long startMillis;
    private long endMillis = -1;
    private Question pendingQuestion;
    private Pawn questionPawn;

    /**
     * @param board   הלוח
     * @param players השחקנים לפי סדר התורות (FR-07)
     * @param die     הקובייה
     * @param clock   מקור הזמן במילישניות (System::currentTimeMillis, או שעון קבוע בבדיקות)
     */
    public Game(Board board, List<Player> players, Die die, LongSupplier clock) {
        this(board, players, die, clock, null);
    }

    /**
     * @param board     הלוח
     * @param players   השחקנים לפי סדר התורות (FR-07)
     * @param die       הקובייה
     * @param clock     מקור הזמן במילישניות
     * @param questions השאלות של המשחק, או null בלי שאלות (ואז משבצת שאלה מתנהגת כמשבצת רגילה)
     */
    public Game(Board board, List<Player> players, Die die, LongSupplier clock, QuestionDeck questions) {
        this(board, players, die, clock, questions, null);
    }

    /**
     * @param surprises אירועי ההפתעה, או null בלי הפתעות (ואז משבצת הפתעה היא רק משבצת מוגנת)
     */
    public Game(Board board, List<Player> players, Die die, LongSupplier clock, QuestionDeck questions,
                SurpriseDeck surprises) {
        if (players.size() < 2 || players.size() > 4) {
            throw new IllegalArgumentException("Game needs 2 to 4 players");
        }
        this.board = board;
        this.players = Collections.unmodifiableList(new ArrayList<Player>(players));
        this.die = die;
        this.clock = clock;
        this.questions = questions;
        this.surprises = new SurpriseHandler(this, surprises);
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

    /** @return השאלה שמחכה לתשובה בשלב {@link GamePhase#QUESTION}, אחרת null */
    public Question getPendingQuestion() {
        return pendingQuestion;
    }

    /** @return הכלי שעל משבצת השאלה; בעליו הוא מי שעונה (FR-27), או null */
    public Pawn getQuestionPawn() {
        return questionPawn;
    }

    /** @return הטיפול במשבצות ההפתעה (FR-44 עד FR-52) */
    public SurpriseHandler getSurprises() {
        return surprises;
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
     * מטיל את הקובייה של השחקן הנוכחי (FR-15). אם אין מהלך חוקי, התור עובר (או שהשחקן מטיל שוב
     * אם יצא 6). 6 שלישי ברצף מבוטל והתור עובר (FR-19).
     *
     * @return מה יצא ומה קורה עכשיו
     * @throws IllegalStateException אם עכשיו לא שלב ההטלה
     */
    public RollResult roll() {
        if (phase != GamePhase.ROLL) {
            throw new IllegalStateException("Not the roll phase: " + phase);
        }
        Player player = getCurrentPlayer();
        surprises.onRoll();
        int value = die.roll();
        lastRoll = value;
        sixesInRow = (value == 6) ? sixesInRow + 1 : 0;

        RollResult result;
        if (value == 6 && sixesInRow == 3) {
            legalMoves = Collections.emptyList();
            result = new RollResult(player, value, true, false, false);
            fireRolled(result);
            advanceTurn();
            return result;
        }

        legalMoves = computeLegalMoves(player, value);
        if (!legalMoves.isEmpty()) {
            phase = GamePhase.MOVE;
            result = new RollResult(player, value, false, true, false);
            fireRolled(result);
        } else if (value == 6) {
            result = new RollResult(player, value, false, false, true);
            fireRolled(result);
        } else {
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
     * <p>
     * אם הכלי נחת על משבצת שאלה, האכילה קורית קודם (FR-39), המשחק עובר לשלב
     * {@link GamePhase#QUESTION} והתור ממשיך רק אחרי {@link #answer(int)}.
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
        Pawn pawn = move.getPawn();
        // בדרך כלל השחקן שתורו; אחרי UltraBoost של הפתעה על כלי של יריב – בעל הכלי (FR-27)
        Player player = pawn.getOwner();
        pawn.setPathIndex(move.getTo());
        legalMoves = Collections.emptyList();

        List<Pawn> captured = captureAt(player, squareOf(pawn));
        Question question = drawQuestion(pawn);
        boolean surprise = question == null && surprises.draw(pawn);
        if (question != null || surprise) {
            MoveResult result = new MoveResult(move, captured, false, false, false, false, question);
            for (GameListener l : listeners) {
                l.onMoved(result);
            }
            if (surprise) {
                surprises.fireDrawn();
            }
            return result;
        }

        boolean reachedFinish = isFinished(pawn);
        boolean playerFinished = reachedFinish && recordFinish(player);
        boolean gameOver = checkGameOver();
        boolean extraTurn = !gameOver && surprises.extraRollDue(lastRoll) && !player.isDone();
        MoveResult result = new MoveResult(move, captured, reachedFinish, playerFinished, extraTurn, gameOver);
        for (GameListener l : listeners) {
            l.onMoved(result);
        }
        endMove(gameOver, extraTurn);
        return result;
    }

    /**
     * עונה על השאלה שמחכה (FR-38 עד FR-41) ומזיז את הכלי לפי התשובה: קדימה בתשובה נכונה ואחורה
     * באותו מספר צעדים בתשובה שגויה (1/2/3 לפי רמת השאלה, FR-40).
     * <ul>
     *   <li>קדימה נכנסים לבית רק במספר מדויק; מספר גדול מדי לא מזיז את הכלי (FR-24).</li>
     *   <li>אחורה הכלי חוזר לאורך המסלול שעבר, ולא מעבר למשבצת ההתחלה שלו (FR-26).</li>
     *   <li>הכלי לא נוחת על Double Block של יריב ולא עובר אותו, ונעצר לפניו (FR-33).</li>
     *   <li>במשבצת שהכלי הגיע אליה יש אכילה כרגיל, גם אחורה (FR-29).</li>
     *   <li>אם הכלי נחת על עוד משבצת שאלה, היא מופעלת מיד באותו תור (FR-27).</li>
     * </ul>
     *
     * @param choice האינדקס של התשובה שנבחרה, 0 עד 3
     * @return מה קרה
     * @throws IllegalStateException    אם אין שאלה שמחכה לתשובה
     * @throws IllegalArgumentException אם האינדקס לא בין 0 ל-3
     */
    public AnswerResult answer(int choice) {
        if (phase != GamePhase.QUESTION) {
            throw new IllegalStateException("No question to answer: " + phase);
        }
        AnswerCheck check = pendingQuestion.check(choice);
        Pawn pawn = questionPawn;
        Player player = pawn.getOwner();
        pendingQuestion = null;
        questionPawn = null;

        int from = pawn.getPathIndex();
        int[] target = new int[1];
        AnswerResult.Limit limit = questionTarget(pawn, check.getSignedSteps(), target);
        int to = target[0];
        pawn.setPathIndex(to);

        List<Pawn> captured = Collections.emptyList();
        Question next = null;
        if (to != from) {
            captured = captureAt(player, squareOf(pawn));
            next = drawQuestion(pawn);
        }
        boolean surprise = to != from && next == null && surprises.draw(pawn);
        if (next != null || surprise) {
            AnswerResult result = new AnswerResult(check, pawn, from, to, limit, captured,
                    false, false, false, false, next);
            fireAnswered(result);
            if (surprise) {
                surprises.fireDrawn();
            }
            return result;
        }

        boolean reachedFinish = to != from && isFinished(pawn);
        boolean playerFinished = reachedFinish && recordFinish(player);
        boolean gameOver = checkGameOver();
        boolean extraTurn = !gameOver && surprises.extraRollDue(lastRoll) && !player.isDone();
        AnswerResult result = new AnswerResult(check, pawn, from, to, limit, captured,
                reachedFinish, playerFinished, extraTurn, gameOver, null);
        fireAnswered(result);
        endMove(gameOver, extraTurn);
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

    /** אוכל כלי יחיד של כל יריב במשבצת, אם היא לא מוגנת. */
    List<Pawn> captureAt(Player player, Square target) {
        List<Pawn> captured = new ArrayList<Pawn>();
        if (target.isSafe()) {
            return captured;
        }
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
        return captured;
    }

    /**
     * אם הכלי על משבצת שאלה ויש שאלות במשחק: שולף שאלה ברמת המשבצת (FR-38) ועובר לשלב השאלה.
     *
     * @return השאלה, או null אם אין שאלה
     */
    Question drawQuestion(Pawn pawn) {
        Square square = squareOf(pawn);
        if (questions == null || !(square instanceof QuestionSquare)) {
            return null;
        }
        pendingQuestion = questions.next(((QuestionSquare) square).getLevel());
        questionPawn = pawn;
        phase = GamePhase.QUESTION;
        return pendingQuestion;
    }

    /**
     * לאן הכלי זז אחרי תשובה (FR-24, FR-26, FR-33).
     *
     * @param steps  חיובי קדימה, שלילי אחורה
     * @param target מקבל את המקום החדש במסלול
     * @return למה הכלי זז פחות מ-steps, או NONE
     */
    AnswerResult.Limit questionTarget(Pawn pawn, int steps, int[] target) {
        List<Square> path = board.getPath(pawn.getColor());
        Player player = pawn.getOwner();
        int from = pawn.getPathIndex();
        target[0] = from;
        if (steps > 0) {
            int to = from + steps;
            if (to > board.getFinishIndex()) {
                return AnswerResult.Limit.FINISH_NOT_EXACT;
            }
            for (int i = from + 1; i <= to; i++) {
                if (isBlockedFor(path.get(i), player)) {
                    target[0] = i - 1;
                    return AnswerResult.Limit.DOUBLE_BLOCK;
                }
            }
            target[0] = to;
            return AnswerResult.Limit.NONE;
        }
        int to = Math.max(0, from + steps);
        for (int i = from - 1; i >= to; i--) {
            if (isBlockedFor(path.get(i), player)) {
                target[0] = i + 1;
                return AnswerResult.Limit.DOUBLE_BLOCK;
            }
        }
        target[0] = to;
        return to == from + steps ? AnswerResult.Limit.NONE : AnswerResult.Limit.START_SQUARE;
    }

    /** אם כל הכלים של השחקן בבית: רושם את המקום שלו בדירוג. */
    boolean recordFinish(Player player) {
        if (!allFinished(player)) {
            return false;
        }
        ranking.add(player);
        player.setPlace(ranking.size());
        return true;
    }

    /** אחרי שהמהלך (והשאלות שבעקבותיו) הסתיים: סיום משחק, הטלה נוספת או מעבר תור. */
    void endMove(boolean gameOver, boolean extraTurn) {
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
    }

    private void fireAnswered(AnswerResult result) {
        for (GameListener l : listeners) {
            l.onAnswered(result);
        }
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
    boolean checkGameOver() {
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

    private void advanceTurn() {
        sixesInRow = 0;
        legalMoves = Collections.emptyList();
        do {
            current = (current + 1) % players.size();
        } while (players.get(current).isDone() || surprises.skipsTurn(players.get(current)));
        phase = GamePhase.ROLL;
        for (GameListener l : listeners) {
            l.onTurnChanged(getCurrentPlayer());
        }
    }

    // ---------------------------------------------------------------- נקודות חיבור ל-SurpriseHandler

    void setPhase(GamePhase phase) {
        this.phase = phase;
    }

    void setLegalMoves(List<Move> moves) {
        this.legalMoves = moves;
    }

    Die getDie() {
        return die;
    }

    List<GameListener> getListeners() {
        return listeners;
    }

    private void fireRolled(RollResult result) {
        for (GameListener l : listeners) {
            l.onRolled(result);
        }
    }
}

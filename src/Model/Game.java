package Model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
 *   <li>כלי שנוחת על משבצת הפתעה עוצר את התור עד שהאירוע שהוגרל מתבצע (שלב
 *       {@link GamePhase#SURPRISE}, FR-44 עד FR-52).</li>
 *   <li>שחקן שסיים מקבל מקום לפי סדר הסיום, ומדלגים עליו בתורות (FR-53).</li>
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
    private final SurpriseDeck surprises;
    /** שחקנים שהפסידו את התור הבא באירוע הפתעה (FR-50). */
    private final Set<Player> skipNextTurn = new HashSet<Player>();

    private int current;
    private GamePhase phase = GamePhase.ROLL;
    private int lastRoll;
    private int sixesInRow;
    private List<Move> legalMoves = Collections.emptyList();
    private final long startMillis;
    private long endMillis = -1;
    private Question pendingQuestion;
    private Pawn questionPawn;
    private SurpriseEvent pendingSurprise;
    private Pawn surprisePawn;
    /** יצא 6 בהטלת ה-UltraBoost של אירוע הפתעה, ולכן מגיעה הטלה נוספת (FR-52). */
    private boolean specialSix;
    /** הטלת ה-UltraBoost האחרונה, כדי להסביר למה כלי לא זז. */
    private int specialSteps = -1;

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
     * @param board     הלוח
     * @param players   השחקנים לפי סדר התורות (FR-07)
     * @param die       קוביית הצעדים
     * @param clock     מקור הזמן במילישניות
     * @param questions השאלות של המשחק, או null בלי שאלות
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
        this.surprises = surprises;
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

    /** @return אירוע ההפתעה שמחכה בשלב {@link GamePhase#SURPRISE}, אחרת null */
    public SurpriseEvent getPendingSurprise() {
        return pendingSurprise;
    }

    /** @return הכלי שעל משבצת ההפתעה; האירוע חל על בעליו (FR-27), או null */
    public Pawn getSurprisePawn() {
        return surprisePawn;
    }

    /**
     * @return מספר הצעדים בהטלת UltraBoost של הפתעה, כשהשחקן בוחר כלי אחריה; אחרת -1
     */
    public int getSpecialSteps() {
        return specialSteps;
    }

    /** @return true אם השחקן הפסיד את התור הבא שלו באירוע הפתעה, ועוד לא דילגו עליו (FR-50) */
    public boolean losesNextTurn(Player player) {
        return skipNextTurn.contains(player);
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
        specialSix = false;
        specialSteps = -1;
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
        // בדרך כלל זה השחקן שתורו; אחרי UltraBoost של הפתעה על כלי של יריב – בעל הכלי (FR-27)
        Player player = pawn.getOwner();
        pawn.setPathIndex(move.getTo());
        legalMoves = Collections.emptyList();
        specialSteps = -1;

        List<Pawn> captured = captureAt(player, squareOf(pawn));
        Question question = drawQuestion(pawn);
        boolean surprise = question == null && drawSurprise(pawn);
        if (question != null || surprise) {
            MoveResult result = new MoveResult(move, captured, false, false, false, false, question);
            for (GameListener l : listeners) {
                l.onMoved(result);
            }
            if (surprise) {
                fireSurprise();
            }
            return result;
        }

        boolean reachedFinish = isFinished(pawn);
        boolean playerFinished = reachedFinish && recordFinish(player);
        boolean gameOver = checkGameOver();
        boolean extraTurn = !gameOver && extraRollDue();
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
        boolean surprise = false;
        if (to != from) {
            captured = captureAt(player, squareOf(pawn));
            next = drawQuestion(pawn);
            surprise = next == null && drawSurprise(pawn);
        }
        if (next != null || surprise) {
            AnswerResult result = new AnswerResult(check, pawn, from, to, limit, captured,
                    false, false, false, false, next);
            fireAnswered(result);
            if (surprise) {
                fireSurprise();
            }
            return result;
        }

        boolean reachedFinish = to != from && isFinished(pawn);
        boolean playerFinished = reachedFinish && recordFinish(player);
        boolean gameOver = checkGameOver();
        boolean extraTurn = !gameOver && extraRollDue();
        AnswerResult result = new AnswerResult(check, pawn, from, to, limit, captured,
                reachedFinish, playerFinished, extraTurn, gameOver, null);
        fireAnswered(result);
        endMove(gameOver, extraTurn);
        return result;
    }

    // ---------------------------------------------------------------- הפתעות

    /**
     * מבצע את אירוע ההפתעה שמחכה, כשהוא לא דורש בחירה (FR-44): קפיצה לבית הסיום (FR-45), חזרה
     * לטירה (FR-48), חזרה 3 צעדים (FR-49) או הפסד התור הבא (FR-50). אי אפשר לוותר על האירוע.
     * <p>
     * גם "הזזת כלי יריב" עוברת כאן כשלאף יריב אין כלי במסלול המשותף: האירוע מבוטל (FR-47).
     *
     * @return מה קרה
     * @throws IllegalStateException אם אין הפתעה שמחכה, או שהאירוע דורש בחירה
     */
    public SurpriseResult resolveSurprise() {
        requireSurprise();
        SurpriseEvent event = pendingSurprise;
        Pawn pawn = surprisePawn;
        Player owner = pawn.getOwner();
        List<Pawn> none = Collections.emptyList();
        int from = pawn.getPathIndex();
        switch (event) {
            case JUMP_TO_FINISH: {
                // ישר לבית הסיום: בלי מספר מדויק ובלי עצירה ב-Double Block (FR-45)
                clearSurprise();
                pawn.setPathIndex(board.getFinishIndex());
                boolean playerFinished = recordFinish(owner);
                return endSurprise(event, pawn, pawn, from, pawn.getPathIndex(), 0, 0, false,
                        AnswerResult.Limit.NONE, none, true, playerFinished);
            }
            case BACK_TO_BASE:
                clearSurprise();
                pawn.sendToBase();
                return endSurprise(event, pawn, pawn, from, Pawn.IN_BASE, 0, 0, false,
                        AnswerResult.Limit.NONE, none, false, false);
            case BACK_THREE:
                return moveBack(event, pawn, pawn, 3, 0);
            case LOSE_NEXT_TURN:
                clearSurprise();
                skipNextTurn.add(owner);
                return endSurprise(event, pawn, null, from, from, 0, 0, false,
                        AnswerResult.Limit.NONE, none, false, false);
            case MOVE_OPPONENT:
                if (!surpriseOpponents().isEmpty()) {
                    throw new IllegalStateException("Choose an opponent's pawn: moveOpponent(Pawn)");
                }
                // לאף יריב אין כלי להזיז: האירוע מבוטל (FR-47)
                clearSurprise();
                return endSurprise(event, pawn, null, from, from, 0, 0, true,
                        AnswerResult.Limit.NONE, none, false, false);
            default:
                throw new IllegalStateException("Roll for UltraBoost: rollUltraBoost()");
        }
    }

    /**
     * היריבים שאפשר לבחור באירוע "הזזת כלי יריב": מי שיש לו לפחות כלי אחד שאפשר להזיז אחורה במסלול
     * המשותף (FR-46, FR-47). אם הרשימה ריקה, {@link #resolveSurprise()} מבטל את האירוע.
     */
    public List<Player> surpriseOpponents() {
        if (phase != GamePhase.SURPRISE || pendingSurprise != SurpriseEvent.MOVE_OPPONENT) {
            return Collections.emptyList();
        }
        List<Player> result = new ArrayList<Player>();
        for (Player p : players) {
            if (p != surprisePawn.getOwner() && !surpriseTargets(p).isEmpty()) {
                result.add(p);
            }
        }
        return result;
    }

    /**
     * הכלים של היריב שאפשר להזיז באירוע "הזזת כלי יריב": על המסלול המשותף, לא בטירה, לא במסלול
     * הבית ולא על משבצת ההתחלה (משם אין לאן לחזור, FR-26).
     */
    public List<Pawn> surpriseTargets(Player opponent) {
        List<Pawn> result = new ArrayList<Pawn>();
        for (Pawn pawn : opponent.getPawns()) {
            if (!pawn.isInBase() && pawn.getPathIndex() > 0 && !squareOf(pawn).isPrivate()) {
                result.add(pawn);
            }
        }
        return result;
    }

    /**
     * "הזזת כלי יריב" (FR-46): מטיל קוביית צעדים חדשה ומזיז את הכלי שנבחר אחורה בחצי מהתוצאה,
     * מעוגל למעלה (1-2 → 1, 3-4 → 2, 5-6 → 3). התנועה אחורה לפי FR-26 ו-FR-33, ובמשבצת שהכלי
     * הגיע אליה יש אכילה והפעלה של שאלה או הפתעה לבעליו (FR-27, FR-29).
     *
     * @param target כלי של יריב מתוך {@link #surpriseTargets(Player)}
     * @return מה קרה
     */
    public SurpriseResult moveOpponent(Pawn target) {
        requireSurprise();
        if (pendingSurprise != SurpriseEvent.MOVE_OPPONENT) {
            throw new IllegalStateException("Not a move-opponent surprise: " + pendingSurprise);
        }
        if (target.getOwner() == surprisePawn.getOwner() || !surpriseTargets(target.getOwner()).contains(target)) {
            throw new IllegalArgumentException("Not an opponent's pawn on the shared track: " + target);
        }
        int roll = die.roll();
        return moveBack(SurpriseEvent.MOVE_OPPONENT, surprisePawn, target, (roll + 1) / 2, roll);
    }

    /**
     * אירוע "UltraBoost" (FR-51): הטלה של קוביית צעדים ו-UltraBoost, בכל רמה ובלי קשר ל-6 קודמים.
     * הצעדים הם קוביית הצעדים כפול (UltraBoost פחות 1) (FR-16). אם יש מהלך חוקי, המשחק עובר לשלב
     * {@link GamePhase#MOVE} ובעל הכלי בוחר כלי ב-{@link #move(Move)}; ב-6 אפשר גם להוציא כלי מהטירה.
     * 6 בהטלה הזו נותן הטלה נוספת, בלי לאפס את רצף ה-6 ובלי לספור אותה בו (FR-52).
     *
     * @return התוצאות, והאם יש מהלך
     */
    public SurpriseResult rollUltraBoost() {
        requireSurprise();
        if (pendingSurprise != SurpriseEvent.ULTRA_BOOST) {
            throw new IllegalStateException("Not an UltraBoost surprise: " + pendingSurprise);
        }
        Pawn pawn = surprisePawn;
        Player owner = pawn.getOwner();
        clearSurprise();
        int step = die.roll();
        int ultra = surprises.rollUltraBoost();
        int steps = step * (ultra - 1);
        if (step == 6 && owner == getCurrentPlayer()) {
            specialSix = true; // sixesInRow לא משתנה (FR-52)
        }
        legalMoves = computeLegalMoves(owner, step == 6, steps);
        List<Pawn> none = Collections.emptyList();
        if (!legalMoves.isEmpty()) {
            phase = GamePhase.MOVE;
            specialSteps = steps;
            SurpriseResult result = new SurpriseResult(SurpriseEvent.ULTRA_BOOST, pawn, null, Pawn.IN_BASE,
                    Pawn.IN_BASE, step, ultra, false, AnswerResult.Limit.NONE, none, false, false, true,
                    false, false, true);
            fireSurpriseResolved(result);
            return result;
        }
        return endSurprise(SurpriseEvent.ULTRA_BOOST, pawn, null, Pawn.IN_BASE, Pawn.IN_BASE, step, ultra,
                false, AnswerResult.Limit.NONE, none, false, false);
    }

    /** מזיז כלי אחורה באירוע הפתעה (FR-26, FR-29, FR-33), ומפעיל את המשבצת שהגיע אליה (FR-27). */
    private SurpriseResult moveBack(SurpriseEvent event, Pawn pawn, Pawn moved, int steps, int roll) {
        clearSurprise();
        int from = moved.getPathIndex();
        int[] target = new int[1];
        AnswerResult.Limit limit = questionTarget(moved, -steps, target);
        int to = target[0];
        moved.setPathIndex(to);
        List<Pawn> captured = Collections.emptyList();
        if (to != from) {
            captured = captureAt(moved.getOwner(), squareOf(moved));
            Question next = drawQuestion(moved);
            boolean surprise = next == null && drawSurprise(moved);
            if (next != null || surprise) {
                SurpriseResult result = new SurpriseResult(event, pawn, moved, from, to, roll, 0, false, limit,
                        captured, false, false, false, false, false, true);
                fireSurpriseResolved(result);
                if (surprise) {
                    fireSurprise();
                }
                return result;
            }
        }
        return endSurprise(event, pawn, moved, from, to, roll, 0, false, limit, captured, false, false);
    }

    /** סוף אירוע הפתעה שלא ממשיך: סיום משחק, הטלה נוספת או מעבר תור. */
    private SurpriseResult endSurprise(SurpriseEvent event, Pawn pawn, Pawn moved, int from, int to, int stepRoll,
                                       int ultraRoll, boolean cancelled, AnswerResult.Limit limit,
                                       List<Pawn> captured, boolean reachedFinish, boolean playerFinished) {
        boolean gameOver = checkGameOver();
        boolean extraTurn = !gameOver && extraRollDue();
        SurpriseResult result = new SurpriseResult(event, pawn, moved, from, to, stepRoll, ultraRoll, cancelled,
                limit, captured, reachedFinish, playerFinished, false, extraTurn, gameOver, false);
        fireSurpriseResolved(result);
        endMove(gameOver, extraTurn);
        return result;
    }

    /**
     * אם הכלי על משבצת הפתעה ויש הפתעות במשחק: מגריל אירוע (FR-44) ועובר לשלב ההפתעה.
     *
     * @return true אם הוגרל אירוע
     */
    private boolean drawSurprise(Pawn pawn) {
        if (surprises == null || !(squareOf(pawn) instanceof SurpriseSquare)) {
            return false;
        }
        pendingSurprise = surprises.draw();
        surprisePawn = pawn;
        phase = GamePhase.SURPRISE;
        return true;
    }

    private void requireSurprise() {
        if (phase != GamePhase.SURPRISE) {
            throw new IllegalStateException("No surprise waiting: " + phase);
        }
    }

    private void clearSurprise() {
        pendingSurprise = null;
        surprisePawn = null;
    }

    /**
     * @return true אם השחקן שתורו מטיל שוב: יצא לו 6 (FR-17) או 6 בהטלת UltraBoost של הפתעה (FR-52),
     *         והוא לא סיים ולא הפסיד את התור הבא (FR-50)
     */
    private boolean extraRollDue() {
        Player p = getCurrentPlayer();
        return (lastRoll == 6 || specialSix) && !p.isDone() && !skipNextTurn.contains(p);
    }

    private void fireSurprise() {
        for (GameListener l : listeners) {
            l.onSurprise(pendingSurprise, surprisePawn);
        }
    }

    private void fireSurpriseResolved(SurpriseResult result) {
        for (GameListener l : listeners) {
            l.onSurpriseResolved(result);
        }
    }

    // ---------------------------------------------------------------- עזר

    /**
     * המהלכים החוקיים לתוצאה. מהלכים מלאים קודמים; רק אם אין אף מהלך מלא, כלי שנתקל ב-Double Block
     * נעצר במשבצת שלפניו (תשובות המתרגלים על Double Block).
     */
    List<Move> computeLegalMoves(Player player, int value) {
        return computeLegalMoves(player, value == 6, value);
    }

    /**
     * @param canLeaveBase true אם מותר להוציא כלי מהטירה (יצא 6 בקוביית הצעדים, FR-20)
     * @param steps        כמה צעדים זזים; 0 – אף כלי על הלוח לא זז (מקדם 0, FR-16)
     */
    List<Move> computeLegalMoves(Player player, boolean canLeaveBase, int steps) {
        int value = steps;
        List<Square> path = board.getPath(player.getColor());
        int finish = board.getFinishIndex();
        List<Move> full = new ArrayList<Move>();
        List<Move> partial = new ArrayList<Move>();
        boolean baseMoveAdded = false;

        for (Pawn pawn : player.getPawns()) {
            if (pawn.isInBase()) {
                if (canLeaveBase && !baseMoveAdded && !isBlockedFor(path.get(0), player)) {
                    full.add(new Move(pawn, Pawn.IN_BASE, 0, false));
                    baseMoveAdded = true;
                }
                continue;
            }
            int from = pawn.getPathIndex();
            if (from == finish || value <= 0) {
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
    private List<Pawn> captureAt(Player player, Square target) {
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
    private Question drawQuestion(Pawn pawn) {
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
    private AnswerResult.Limit questionTarget(Pawn pawn, int steps, int[] target) {
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
    private boolean recordFinish(Player player) {
        if (!allFinished(player)) {
            return false;
        }
        ranking.add(player);
        player.setPlace(ranking.size());
        return true;
    }

    /** אחרי שהמהלך (והשאלות שבעקבותיו) הסתיים: סיום משחק, הטלה נוספת או מעבר תור. */
    private void endMove(boolean gameOver, boolean extraTurn) {
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

    private void advanceTurn() {
        sixesInRow = 0;
        legalMoves = Collections.emptyList();
        specialSix = false;
        specialSteps = -1;
        while (true) {
            current = (current + 1) % players.size();
            Player next = players.get(current);
            if (next.isDone()) {
                continue; // שחקן שסיים לא משחק יותר (FR-53)
            }
            if (skipNextTurn.remove(next)) {
                for (GameListener l : listeners) {
                    l.onTurnSkipped(next); // הפסד התור הבא (FR-50)
                }
                continue;
            }
            break;
        }
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

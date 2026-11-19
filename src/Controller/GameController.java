package Controller;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import Model.AnswerCheck;
import Model.AnswerResult;

import Model.Board;
import Model.BoardBuilder;
import Model.Game;
import Model.GameListener;
import Model.GamePhase;
import Model.GameRecord;
import Model.GameSettings;
import Model.HistoryRepository;
import Model.Move;
import Model.MoveResult;
import Model.Pawn;
import Model.Player;
import Model.Question;
import Model.QuestionDeck;
import Model.QuestionFileException;
import Model.QuestionRepository;
import Model.RollResult;
import Model.StepDie;
import Model.SurpriseDeck;
import Model.SurpriseEvent;
import Model.SurpriseResult;
import View.Dialogs;
import View.GameScreenPanel;
import View.MainFrame;
import View.QuestionDialog;
import View.SurpriseDialog;

/**
 * מחבר בין מסך המשחק למודל: הטלת קובייה, בחירת כלי, השעון, יומן הפעולות ושמירת ההיסטוריה בסוף
 * (FR-12, FR-13, FR-15, FR-19 עד FR-23), שאלות על משבצת {@code ?} (FR-38 עד FR-43), הפתעות על
 * משבצת {@code !} (FR-44 עד FR-52) ומסך הסיום (FR-53, FR-54).
 * <p>
 * במקור ({@code GamePanel} ו-{@code Gameplay} בפרויקט Ludo-Griniaris-Game) המאזינים של הכפתורים
 * הכילו את החוקים עצמם. כאן ה-Controller רק מעביר את הפעולה למודל ומעדכן את המסך לפי
 * ההודעות שהמודל שולח (GameListener).
 */
public class GameController implements GameListener {

    private final MainFrame frame;
    private final GameScreenPanel view;
    private final HistoryRepository history;
    private final QuestionRepository questions;
    private final MainController main;
    private final Random random = new Random();
    private final Timer clock;
    private Game game;
    private boolean asking;
    /** הודעת סיום המשחק, אם המשחק נגמר בזמן שחלון שאלה פתוח; מוצגת אחרי שהוא נסגר. */
    private Runnable gameOverAfterQuestion;

    public GameController(MainFrame frame, GameScreenPanel view, HistoryRepository history,
                          QuestionRepository questions, MainController main) {
        this.frame = frame;
        this.view = view;
        this.history = history;
        this.questions = questions;
        this.main = main;
        this.clock = new Timer(1000, e -> updateClock());
    }

    void bind() {
        view.getRollButton().addActionListener(e -> onRoll());
        view.getBoardPanel().setPawnClickListener(this::onPawnClicked);
    }

    public boolean isRunning() {
        return game != null && !game.isOver();
    }

    /**
     * מתחיל משחק חדש עם לוח חדש לרמה (המשבצות המיוחדות מוגרלות מחדש). השאלות נקראות מהקובץ בכל
     * משחק, כך ששינויים במאגר חלים מהמשחק הבא (FR-73).
     *
     * @return false אם המשחק לא התחיל, כי קובץ השאלות חסר או פגום ברמה בינונית או קשה (FR-43)
     */
    public boolean start(GameSettings settings) {
        QuestionDeck deck = null;
        if (settings.getDifficulty().getQuestionsPerLevel() > 0) {
            try {
                deck = new QuestionDeck(questions.loadAll(), random);
            } catch (QuestionFileException e) {
                Dialogs.error(frame, "קובץ השאלות",
                        e.getMessage() + "\nאי אפשר להתחיל משחק ברמה " + settings.getDifficulty().getHebrewName()
                                + ". אפשר לשחק ברמה קלה, שאין בה שאלות.");
                return false;
            }
        }
        stop();
        Board board = BoardBuilder.build(settings.getDifficulty(), random);
        List<Player> players = settings.createPlayers();
        game = new Game(board, players, new StepDie(random), System::currentTimeMillis, deck,
                new SurpriseDeck(random));
        game.addListener(this);
        view.showGame(game);
        view.appendLog("המשחק התחיל ברמה " + settings.getDifficulty().getHebrewName()
                + ". יוצאים מהטירה ב-6.");
        view.setRollEnabled(true);
        view.setHint("לחצו על \"הטל קובייה\"");
        updateClock();
        clock.start();
        return true;
    }

    /** עוצר את המשחק הנוכחי (יציאה לתפריט). */
    public void stop() {
        clock.stop();
        if (game != null) {
            game.removeListener(this);
        }
        game = null;
    }

    private void onRoll() {
        if (game == null || game.getPhase() != GamePhase.ROLL) {
            return;
        }
        game.roll();
    }

    /** לחיצה על כלי: מזיז אותו אם המהלך חוקי, ואחרת מסביר למה לא (FR-23). */
    private void onPawnClicked(Pawn pawn) {
        if (game == null || game.getPhase() != GamePhase.MOVE) {
            return;
        }
        Move move = game.legalMoveFor(pawn);
        if (move == null) {
            view.setHint(reasonCannotMove(pawn));
            return;
        }
        game.move(move);
    }

    private String reasonCannotMove(Pawn pawn) {
        // אחרי הטלת UltraBoost של הפתעה זזים לפי הצעדים שלה (FR-51)
        int roll = game.getSpecialSteps() >= 0 ? game.getSpecialSteps() : game.getLastRoll();
        if (pawn.isInBase()) {
            return roll == 6 ? "משבצת ההתחלה חסומה ב-Double Block" : "יוצאים מהטירה רק ב-6";
        }
        if (game.isFinished(pawn)) {
            return "הכלי הזה כבר בבית";
        }
        int left = game.getBoard().getFinishIndex() - pawn.getPathIndex();
        if (roll > left) {
            return "כדי להיכנס לבית צריך בדיוק " + left;
        }
        return "Double Block חוסם את הדרך – הזיזו כלי אחר";
    }

    private void updateClock() {
        if (game != null) {
            view.setClock(HistoryRepository.formatDuration(game.getElapsedMillis()));
        }
    }

    // ------------------------------------------------------------ GameListener

    @Override
    public void onRolled(RollResult r) {
        view.showDice(r.getValue());
        String who = r.getPlayer().getName();
        if (r.isCancelledThirdSix()) {
            view.appendLog(who + " הטיל 6 בפעם השלישית ברצף – ההטלה בוטלה והתור עובר.");
        } else if (r.hasMoves()) {
            view.appendLog(who + " הטיל " + r.getValue() + ".");
            view.setRollEnabled(false);
            view.setHint("בחרו כלי מסומן בצהוב");
        } else if (r.isRollAgain()) {
            view.appendLog(who + " הטיל 6 ואין מהלך חוקי – מטילים שוב.");
            view.setRollEnabled(true);
            view.setHint("יצא 6: הטילו שוב");
        } else {
            view.appendLog(who + " הטיל " + r.getValue() + " ואין מהלך חוקי – התור עובר.");
        }
        view.getBoardPanel().repaint();
    }

    @Override
    public void onMoved(MoveResult r) {
        Move m = r.getMove();
        Player p = m.getPawn().getOwner();
        if (m.getFrom() == Pawn.IN_BASE) {
            view.appendLog(p.getName() + " הוציא כלי מהטירה.");
        } else if (m.isStoppedByBlock()) {
            view.appendLog(p.getName() + " זז " + (m.getTo() - m.getFrom()) + " צעדים ונעצר לפני Double Block.");
        } else {
            view.appendLog(p.getName() + " זז " + (m.getTo() - m.getFrom()) + " צעדים.");
        }
        for (Pawn eaten : r.getCaptured()) {
            view.appendLog(p.getName() + " אכל כלי של " + eaten.getOwner().getName() + ", והוא חזר לטירה.");
        }
        if (r.isReachedFinish()) {
            view.appendLog("כלי של " + p.getName() + " הגיע לבית.");
        }
        if (r.isPlayerFinished()) {
            view.appendLog(p.getName() + " סיים במקום " + p.getPlace() + "!");
        }
        if (r.isExtraTurn()) {
            view.appendLog("יצא 6: " + p.getName() + " מטיל שוב.");
            view.setRollEnabled(true);
            view.setHint("יצא 6: הטילו שוב");
        }
        if (r.getQuestion() != null) {
            announceQuestion(p, r.getQuestion());
        }
        view.updatePlayers(game);
        view.getBoardPanel().repaint();
    }

    // ------------------------------------------------------------ שאלות

    private void announceQuestion(Player p, Question q) {
        view.appendLog(p.getName() + " נחת על משבצת שאלה " + q.getLevel().getHebrewName() + ".");
        view.setRollEnabled(false);
        view.setHint("ענו על השאלה");
        if (!asking) {
            // אחרי שהמסך מתעדכן, כדי שרואים איפה הכלי נחת
            SwingUtilities.invokeLater(this::askPendingQuestions);
        }
    }

    // ------------------------------------------------------------ הפתעות

    @Override
    public void onSurprise(SurpriseEvent event, Pawn pawn) {
        view.appendLog(pawn.getOwner().getName() + " נחת על משבצת הפתעה: " + event.getHebrewName() + "!");
        view.setRollEnabled(false);
        view.setHint("הפתעה!");
        view.getBoardPanel().repaint();
        if (!asking) {
            SwingUtilities.invokeLater(this::askPendingQuestions);
        }
    }

    /** מציג את חלון ההפתעה (FR-44) ומעביר לו את הפעולות במשחק. */
    private void showSurprise() {
        final Game current = game;
        Map<Player, List<Pawn>> opponents = new LinkedHashMap<Player, List<Pawn>>();
        for (Player p : current.surpriseOpponents()) {
            opponents.put(p, current.surpriseTargets(p));
        }
        SurpriseDialog.show(frame, current.getPendingSurprise(), current.getSurprisePawn(), opponents,
                new SurpriseDialog.Actions() {
                    @Override
                    public SurpriseResult resolve() {
                        return current.resolveSurprise();
                    }

                    @Override
                    public SurpriseResult moveOpponent(Pawn target) {
                        return current.moveOpponent(target);
                    }

                    @Override
                    public SurpriseResult rollUltraBoost() {
                        return current.rollUltraBoost();
                    }
                });
    }

    @Override
    public void onSurpriseResolved(SurpriseResult r) {
        if (r.getStepRoll() > 0) {
            view.showDice(r.getStepRoll());
        }
        view.appendLog(SurpriseDialog.describe(r));
        Player owner = r.getPawn().getOwner();
        if (r.hasMoves()) {
            view.setRollEnabled(false);
            view.setHint(owner.getName() + ": בחרו כלי מסומן (" + r.getUltraBoostSteps() + " צעדים"
                    + (r.getStepRoll() == 6 ? ", או הוצאה מהטירה" : "") + ")");
        } else if (r.isContinues() && game != null && game.getPhase() == GamePhase.QUESTION) {
            announceQuestion(game.getQuestionPawn().getOwner(), game.getPendingQuestion());
        } else if (r.isExtraTurn()) {
            view.setRollEnabled(true);
            view.setHint("יצא 6: הטילו שוב");
        }
        view.updatePlayers(game);
        view.getBoardPanel().repaint();
    }

    @Override
    public void onTurnSkipped(Player player) {
        view.appendLog(player.getName() + " מפסיד את התור הזה (הפתעה).");
    }

    /**
     * מציג את השאלה שמחכה, ושולח את התשובה למודל. אם הכלי נחת על עוד משבצת שאלה, שואלים שוב באותו
     * תור (FR-27).
     */
    private void askPendingQuestions() {
        if (asking) {
            return;
        }
        asking = true;
        try {
            while (game != null && (game.getPhase() == GamePhase.QUESTION || game.getPhase() == GamePhase.SURPRISE)) {
                if (game.getPhase() == GamePhase.SURPRISE) {
                    showSurprise(); // FR-27: גם הפתעה אחרי שאלה, או שאלה אחרי הפתעה, באותו תור
                    continue;
                }
                Question q = game.getPendingQuestion();
                Player answerer = game.getQuestionPawn().getOwner();
                final Game current = game;
                QuestionDialog.ask(frame, q, answerer, current::answer);
            }
        } finally {
            asking = false;
        }
        if (gameOverAfterQuestion != null) {
            Runnable show = gameOverAfterQuestion;
            gameOverAfterQuestion = null;
            SwingUtilities.invokeLater(show);
        }
    }

    @Override
    public void onAnswered(AnswerResult r) {
        AnswerCheck check = r.getCheck();
        Player p = r.getPawn().getOwner();
        String level = check.getQuestion().getLevel().getHebrewName();
        if (check.isCorrect()) {
            view.appendLog(p.getName() + " ענה נכון על שאלה " + level + ".");
        } else {
            view.appendLog(p.getName() + " טעה בשאלה " + level + ". התשובה הנכונה: " + check.getCorrectAnswer() + ".");
        }
        view.appendLog(QuestionDialog.describeMove(r));
        for (Pawn eaten : r.getCaptured()) {
            view.appendLog(p.getName() + " אכל כלי של " + eaten.getOwner().getName() + ", והוא חזר לטירה.");
        }
        if (r.isReachedFinish()) {
            view.appendLog("כלי של " + p.getName() + " הגיע לבית.");
        }
        if (r.isPlayerFinished()) {
            view.appendLog(p.getName() + " סיים במקום " + p.getPlace() + "!");
        }
        if (r.getNextQuestion() != null) {
            announceQuestion(p, r.getNextQuestion());
        } else if (r.isExtraTurn()) {
            view.appendLog("יצא 6: " + p.getName() + " מטיל שוב.");
            view.setRollEnabled(true);
            view.setHint("יצא 6: הטילו שוב");
        }
        view.updatePlayers(game);
        view.getBoardPanel().repaint();
    }

    @Override
    public void onTurnChanged(Player current) {
        view.showTurn(current);
        view.setRollEnabled(true);
        view.setHint("לחצו על \"הטל קובייה\"");
        view.getBoardPanel().repaint();
    }

    @Override
    public void onGameOver(List<Player> ranking) {
        clock.stop();
        updateClock();
        view.setRollEnabled(false);
        view.updatePlayers(game);
        view.appendLog("המשחק נגמר: " + ranking.get(ranking.size() - 1).getName() + " נשאר אחרון.");
        final List<Player> places = ranking;
        final String duration = HistoryRepository.formatDuration(game.getElapsedMillis());
        final String difficulty = game.getBoard().getDifficulty().getHebrewName();
        String note;
        try {
            GameRecord rec = history.save(game);
            note = "נשמר בהיסטוריה כמשחק מס' " + rec.getGameId() + ".";
        } catch (IOException e) {
            note = "לא הצלחנו לשמור את המשחק בהיסטוריה: " + e.getMessage();
        }
        final String saveNote = note;
        // מסך הסיום (FR-54): המקומות, הזמן והרמה, עם "משחק חדש" ו"לתפריט"
        Runnable show = () -> {
            stop();
            main.showGameOver(places, duration, difficulty, saveNote);
        };
        if (asking) {
            // המשחק נגמר מתשובה לשאלה: ההודעה מחכה עד שחלון השאלה נסגר, ולא נפתחת מעליו
            gameOverAfterQuestion = show;
        } else {
            SwingUtilities.invokeLater(show);
        }
    }
}

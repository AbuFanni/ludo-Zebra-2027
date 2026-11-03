package Controller;

import Model.GameSettings;
import View.Dialogs;
import View.MainFrame;
import View.MainFrame.Screen;

/**
 * ה-Controller הראשי: ניווט בין המסכים (FR-01) ואישור יציאה (FR-02).
 * <p>
 * מחזיק את מצב "יש משחק פעיל", כי רק ממשחק פעיל היציאה מאבדת נתונים.
 */
public class MainController {

    static final String EXIT_GAME_TITLE = "יציאה מהמשחק";
    static final String EXIT_GAME_MESSAGE = "לצאת מהמשחק הנוכחי?\nהמשחק לא יישמר ולא יופיע בהיסטוריה.";
    static final String EXIT_APP_TITLE = "יציאה";
    static final String EXIT_APP_MESSAGE = "לסגור את Ludo Zebra?";

    private final MainFrame frame;
    private final ConfirmDialog confirm;
    private final Runnable shutdown;
    private GameSettings activeGame;

    /** פונקציית אישור – מוחלפת בבדיקות, כדי לבדוק את הזרימה בלי חלון אמיתי. */
    @FunctionalInterface
    public interface ConfirmDialog {
        boolean ask(String title, String message, String yes, String no);
    }

    /** בונה Controller עם דיאלוג אמיתי ויציאה מהתוכנה. */
    public MainController(MainFrame frame) {
        this(frame,
                (t, m, y, n) -> Dialogs.confirm(frame, t, m, y, n),
                () -> {
                    frame.dispose();
                    System.exit(0);
                });
    }

    /**
     * @param frame    החלון הראשי
     * @param confirm  איך לשאול את המשתמש
     * @param shutdown מה לעשות ביציאה מהתוכנה
     */
    public MainController(MainFrame frame, ConfirmDialog confirm, Runnable shutdown) {
        this.frame = frame;
        this.confirm = confirm;
        this.shutdown = shutdown;

        frame.getMainMenu().onNewGame(e -> showSetup());
        frame.getMainMenu().onHistory(e -> frame.show(Screen.HISTORY));
        frame.getMainMenu().onQuestions(e -> frame.show(Screen.QUESTIONS));
        frame.getMainMenu().onExit(e -> requestExitApp());
        frame.getHistory().onBack(e -> showMainMenu());
        frame.getQuestions().onBack(e -> showMainMenu());
        frame.getGame().onExitToMenu(e -> requestExitToMenu());
        frame.onCloseRequest(this::requestExitApp);

        new SetupController(frame.getSetup(), this);
    }

    /** מציג את מסך הפתיחה (FR-01). */
    public void showMainMenu() {
        frame.show(Screen.MAIN_MENU);
    }

    /** מציג מסך הגדרה נקי. */
    public void showSetup() {
        frame.getSetup().reset();
        frame.show(Screen.SETUP);
    }

    /**
     * מתחיל משחק עם ההגדרות הנעולות (FR-08) ועובר למסך המשחק.
     *
     * @param settings הגדרות תקינות מהמודל
     */
    public void startGame(GameSettings settings) {
        activeGame = settings;
        frame.getGame().showGame(settings);
        frame.show(Screen.GAME);
    }

    /** יציאה ממשחק פעיל לתפריט – רק אחרי אישור (FR-02). */
    public void requestExitToMenu() {
        if (activeGame == null
                || confirm.ask(EXIT_GAME_TITLE, EXIT_GAME_MESSAGE, "כן, לצאת", "המשך לשחק")) {
            activeGame = null;
            showMainMenu();
        }
    }

    /** סגירת התוכנה. באמצע משחק – עם אזהרה שהמשחק לא יישמר (FR-02). */
    public void requestExitApp() {
        boolean ok = activeGame != null
                ? confirm.ask(EXIT_GAME_TITLE, EXIT_GAME_MESSAGE, "כן, לצאת", "המשך לשחק")
                : confirm.ask(EXIT_APP_TITLE, EXIT_APP_MESSAGE, "סגור", "ביטול");
        if (ok) {
            shutdown.run();
        }
    }

    /** @return {@code true} כשיש משחק שהתחיל ועוד לא הסתיים */
    public boolean hasActiveGame() {
        return activeGame != null;
    }
}

package Controller;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import Model.GameSettings;
import Model.HistoryRepository;
import View.Dialogs;
import View.MainFrame;

/**
 * הניווט בין המסכים ואישור היציאה (FR-01, FR-02).
 * <p>
 * מחליף את המאזינים של {@code WindowInterface.WindowInterf} בפרויקט המקור (מעבר בין פאנלים
 * וסגירת החלון). כאן כל ההחלטות על מעברים נמצאות ב-Controller, והמסכים רק מציגים.
 *
 * @author Nour Eldin Khatib (Zebra) – Iteration 1, SDP table 2
 */
public class MainController {

    private final MainFrame frame;
    private final SetupController setupController;
    private final GameController gameController;
    private final HistoryController historyController;

    public MainController(MainFrame frame, HistoryRepository history) {
        this.frame = frame;
        this.setupController = new SetupController(frame.getSetupPanel(), this);
        this.gameController = new GameController(frame, frame.getGamePanel(), history, this);
        this.historyController = new HistoryController(frame.getHistoryPanel(), history);
    }

    /** מחבר את הכפתורים ומציג את מסך הפתיחה. */
    public void start() {
        frame.getMenuPanel().getNewGameButton().addActionListener(e -> showSetup());
        frame.getMenuPanel().getHistoryButton().addActionListener(e -> showHistory());
        frame.getMenuPanel().getQuestionsButton().addActionListener(e -> frame.showCard(MainFrame.QUESTIONS));
        frame.getMenuPanel().getExitButton().addActionListener(e -> requestQuit());
        frame.getHistoryPanel().getBackButton().addActionListener(e -> showMenu());
        frame.getQuestionsPanel().getBackButton().addActionListener(e -> showMenu());
        frame.getGamePanel().getExitButton().addActionListener(e -> requestLeaveGame());
        setupController.bind();
        gameController.bind();
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (MainFrame.GAME.equals(frame.getCurrentCard()) && gameController.isRunning()) {
                    if (confirmLeaveGame()) {
                        gameController.stop();
                        quit();
                    }
                } else {
                    requestQuit();
                }
            }
        });
        showMenu();
        frame.setVisible(true);
    }

    public void showMenu() {
        frame.showCard(MainFrame.MENU);
    }

    public void showSetup() {
        frame.getSetupPanel().reset();
        frame.showCard(MainFrame.SETUP);
    }

    public void showHistory() {
        historyController.refresh();
        frame.showCard(MainFrame.HISTORY);
    }

    /** מתחיל משחק בהגדרות התקינות ועובר למסך המשחק. */
    public void startGame(GameSettings settings) {
        gameController.start(settings);
        frame.showCard(MainFrame.GAME);
    }

    /** יציאה מהמשחק לתפריט, רק אחרי אישור (FR-02). */
    public void requestLeaveGame() {
        if (confirmLeaveGame()) {
            gameController.stop();
            showMenu();
        }
    }

    private boolean confirmLeaveGame() {
        return Dialogs.confirm(frame, "יציאה מהמשחק",
                "לצאת מהמשחק? המשחק לא יישמר בהיסטוריה.", "כן, לצאת", "המשך לשחק");
    }

    /** סגירת התוכנה, רק אחרי אישור (FR-02). */
    public void requestQuit() {
        if (Dialogs.confirm(frame, "יציאה", "לסגור את Ludo Zebra?", "סגור", "ביטול")) {
            quit();
        }
    }

    private void quit() {
        frame.dispose();
        System.exit(0);
    }
}

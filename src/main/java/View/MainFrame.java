package View;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.WindowConstants;
import java.awt.CardLayout;
import java.awt.ComponentOrientation;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * החלון הראשי. מחזיק את כל המסכים ב-{@link CardLayout} ומחליף ביניהם לפי {@link Screen}.
 * <p>
 * סגירה ב-X לא סוגרת ישר את החלון: היא מועברת ל-Controller, שמבקש אישור (FR-02).
 */
public class MainFrame extends JFrame {

    /** המסכים במערכת. מסכים חדשים (סיום, היסטוריה, אשף) מתווספים כאן. */
    public enum Screen { MAIN_MENU, SETUP, GAME, HISTORY, QUESTIONS }

    private final CardLayout cards = new CardLayout();
    private final JPanel root = new JPanel(cards);
    private Screen current;

    private final MainMenuPanel mainMenu = new MainMenuPanel();
    private final GameSetupPanel setup = new GameSetupPanel();
    private final GameScreenPanel game = new GameScreenPanel();
    private final PlaceholderPanel history =
            new PlaceholderPanel("היסטוריית משחקים", "המסך יתווסף באיטרציה 3.");
    private final PlaceholderPanel questions =
            new PlaceholderPanel("ניהול שאלות", "כניסת המנהל ואשף השאלות יתווספו באיטרציה 3.");

    /** בונה את החלון בגודל שמתאים למסך 1366×768 (NFR-17). */
    public MainFrame() {
        super("Ludo Zebra");
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        setMinimumSize(new Dimension(1024, 680));
        setSize(new Dimension(1200, 720));
        setLocationRelativeTo(null);

        root.add(mainMenu, Screen.MAIN_MENU.name());
        root.add(setup, Screen.SETUP.name());
        root.add(game, Screen.GAME.name());
        root.add(history, Screen.HISTORY.name());
        root.add(questions, Screen.QUESTIONS.name());
        setContentPane(root);
        applyComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        show(Screen.MAIN_MENU);
    }

    /** @param screen המסך שיוצג */
    public void show(Screen screen) {
        cards.show(root, screen.name());
        current = screen;
    }

    /** @return המסך שמוצג כרגע */
    public Screen getCurrentScreen() {
        return current;
    }

    /** @param action מה לעשות כשהמשתמש לוחץ על X */
    public void onCloseRequest(Runnable action) {
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                action.run();
            }
        });
    }

    public MainMenuPanel getMainMenu() {
        return mainMenu;
    }

    public GameSetupPanel getSetup() {
        return setup;
    }

    public GameScreenPanel getGame() {
        return game;
    }

    public PlaceholderPanel getHistory() {
        return history;
    }

    public PlaceholderPanel getQuestions() {
        return questions;
    }
}

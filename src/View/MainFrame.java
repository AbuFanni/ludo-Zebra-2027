package View;

import java.awt.CardLayout;
import java.awt.ComponentOrientation;
import java.awt.Dimension;
import java.awt.Toolkit;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

/**
 * החלון הראשי. מחזיק את כל המסכים ב-CardLayout ומחליף ביניהם לפי בקשת ה-Controller.
 * <p>
 * מחליף את {@code WindowInterface.WindowInterf} בפרויקט המקור, שבו החלון גם הכיל את הלוגיקה של
 * המעברים; כאן החלון רק מציג, וה-Controller מחליט לאן עוברים.
 *
 * @author Nour Eldin Khatib (Zebra) – Iteration 1, SDP table 2
 */
public class MainFrame extends JFrame {

    private static final long serialVersionUID = 1L;

    public static final String MENU = "menu";
    public static final String SETUP = "setup";
    public static final String GAME = "game";
    public static final String HISTORY = "history";
    public static final String QUESTIONS = "questions";

    private final CardLayout cards = new CardLayout();
    private final JPanel root = new JPanel(cards);
    private final MainMenuPanel menuPanel = new MainMenuPanel();
    private final GameSetupPanel setupPanel = new GameSetupPanel();
    private final GameScreenPanel gamePanel = new GameScreenPanel();
    private final HistoryPanel historyPanel = new HistoryPanel();
    private final PlaceholderPanel questionsPanel = new PlaceholderPanel(
            "ניהול שאלות", "אשף ניהול השאלות, עם הרשאת מנהל, יתווסף באיטרציה 3.");
    private String currentCard = MENU;

    public MainFrame() {
        super("Ludo Zebra");
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        root.add(menuPanel, MENU);
        root.add(setupPanel, SETUP);
        root.add(gamePanel, GAME);
        root.add(historyPanel, HISTORY);
        root.add(questionsPanel, QUESTIONS);
        setContentPane(root);
        applyComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        setIconImages(ZebraArt.windowIcons());
        root.setBackground(UiTheme.BACKGROUND);
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        int w = Math.min(1280, Math.max(900, screen.width - 80));
        int h = Math.min(860, Math.max(640, screen.height - 80));
        setSize(w, h);
        setMinimumSize(new Dimension(900, 640));
        setLocationRelativeTo(null);
    }

    /** עובר למסך. */
    public void showCard(String name) {
        currentCard = name;
        cards.show(root, name);
    }

    /** @return שם המסך שמוצג עכשיו */
    public String getCurrentCard() {
        return currentCard;
    }

    public MainMenuPanel getMenuPanel() {
        return menuPanel;
    }

    public GameSetupPanel getSetupPanel() {
        return setupPanel;
    }

    public GameScreenPanel getGamePanel() {
        return gamePanel;
    }

    public HistoryPanel getHistoryPanel() {
        return historyPanel;
    }

    public PlaceholderPanel getQuestionsPanel() {
        return questionsPanel;
    }
}

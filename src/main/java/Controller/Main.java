package Controller;

import View.MainFrame;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * נקודת הכניסה של Ludo Zebra. בונה את החלון ואת ה-Controller ומציג את מסך הפתיחה (FR-01).
 */
public final class Main {

    private Main() {
    }

    /**
     * @param args לא בשימוש
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            } catch (Exception ignored) {
                // נשארים עם ברירת המחדל של Swing
            }
            MainFrame frame = new MainFrame();
            new MainController(frame);
            frame.setVisible(true);
        });
    }
}

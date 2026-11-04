package Controller;

import java.io.File;
import java.net.URISyntaxException;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import Model.HistoryRepository;
import View.Dialogs;
import View.MainFrame;

/**
 * נקודת הכניסה של Ludo Zebra.
 * <p>
 * בסיס הקוד: Ludo-Griniaris-Game של Alexandros Plessias, רישיון MIT
 * (https://github.com/AlexandrosPlessias/Ludo-Griniaris-Game). הפרויקט פורק לחבילות Model, View
 * ו-Controller והותאם לדרישות של פרויקט הקורס. פירוט מה נלקח ומה שונה נמצא ב-NOTICE.md.
 *
 * @author Nour Eldin Khatib (Zebra) – Iteration 1, SDP table 2
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception ignored) {
            // נשארים עם ברירת המחדל
        }
        SwingUtilities.invokeLater(() -> {
            Dialogs.installHebrewDefaults();
            MainFrame frame = new MainFrame();
            HistoryRepository history = new HistoryRepository(new File(appDirectory(), HistoryRepository.DEFAULT_FILE));
            new MainController(frame, history).start();
        });
    }

    /** @return התיקייה של קובץ ה-JAR, כדי שקובץ ההיסטוריה יישמר לידו; אחרת תיקיית העבודה */
    static File appDirectory() {
        try {
            File location = new File(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            if (location.isFile()) {
                return location.getParentFile();
            }
        } catch (URISyntaxException | SecurityException | NullPointerException ignored) {
            // נופלים לתיקיית העבודה
        }
        return new File(System.getProperty("user.dir"));
    }
}

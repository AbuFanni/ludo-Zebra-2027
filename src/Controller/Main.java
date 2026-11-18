package Controller;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import Model.DefaultQuestions;
import Model.HistoryRepository;
import Model.QuestionRepository;
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
            QuestionRepository questions = new QuestionRepository(new File(appDirectory(), QuestionRepository.DEFAULT_FILE));
            createQuestionsFile(questions);
            new MainController(frame, history, questions).start();
        });
    }

    /**
     * בהפעלה הראשונה יוצר את קובץ השאלות עם המאגר ההתחלתי (FR-74). אם זה נכשל, המשחק עדיין עולה,
     * ורמה בינונית או קשה יציגו שגיאה כשמנסים להתחיל אותן (FR-43).
     */
    private static void createQuestionsFile(QuestionRepository questions) {
        try {
            questions.createIfMissing(DefaultQuestions.all());
        } catch (IOException e) {
            System.err.println("Cannot create " + questions.getFile() + ": " + e.getMessage());
        }
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

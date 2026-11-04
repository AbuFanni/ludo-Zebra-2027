package View;

import java.awt.Component;
import java.awt.ComponentOrientation;
import javax.swing.JOptionPane;
import javax.swing.UIManager;

/**
 * חלונות האישור וההודעה של המערכת, בעברית ומימין לשמאל (FR-02).
 *
 * @author Nour Eldin Khatib (Zebra) – Iteration 1, SDP table 2
 */
public final class Dialogs {

    private Dialogs() {
    }

    /** מגדיר את כפתורי חלונות ההודעה בעברית. נקרא פעם אחת בתחילת התוכנית. */
    public static void installHebrewDefaults() {
        UIManager.put("OptionPane.yesButtonText", "כן");
        UIManager.put("OptionPane.noButtonText", "לא");
        UIManager.put("OptionPane.okButtonText", "אישור");
        UIManager.put("OptionPane.cancelButtonText", "ביטול");
        UIManager.put("OptionPane.messageFont", UiTheme.BODY);
        UIManager.put("OptionPane.buttonFont", UiTheme.BODY);
    }

    /**
     * @return true אם המשתמש אישר
     */
    public static boolean confirm(Component parent, String title, String message, String yes, String no) {
        JOptionPane pane = new JOptionPane(message, JOptionPane.QUESTION_MESSAGE, JOptionPane.YES_NO_OPTION,
                null, new Object[]{yes, no}, no);
        pane.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        pane.createDialog(parent, title).setVisible(true);
        return yes.equals(pane.getValue());
    }

    /** מציג הודעה עם כפתור אישור. */
    public static void info(Component parent, String title, String message) {
        JOptionPane pane = new JOptionPane(message, JOptionPane.INFORMATION_MESSAGE);
        pane.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        pane.createDialog(parent, title).setVisible(true);
    }

    /** מציג הודעת שגיאה. */
    public static void error(Component parent, String title, String message) {
        JOptionPane pane = new JOptionPane(message, JOptionPane.ERROR_MESSAGE);
        pane.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        pane.createDialog(parent, title).setVisible(true);
    }
}

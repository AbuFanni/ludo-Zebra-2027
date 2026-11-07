package View;

import java.awt.Color;
import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Container;
import java.awt.Dimension;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.UIManager;

/**
 * חלונות האישור וההודעה של המערכת, בעברית ומימין לשמאל (FR-02), בעיצוב של Zebra:
 * רקע לבן ולוגו הקבוצה במקום האייקון הרגיל של Java.
 *
 * @author Nour Eldin Khatib (Zebra) – Iteration 1, SDP table 2
 */
public final class Dialogs {

    private static ImageIcon logoIcon;

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
        UIManager.put("OptionPane.messageForeground", UiTheme.TEXT);
    }

    private static ImageIcon logo() {
        if (logoIcon == null) {
            logoIcon = new ImageIcon(ZebraArt.logoImage(52));
        }
        return logoIcon;
    }

    /**
     * @return true אם המשתמש אישר
     */
    public static boolean confirm(Component parent, String title, String message, String yes, String no) {
        JOptionPane pane = new JOptionPane(message, JOptionPane.QUESTION_MESSAGE, JOptionPane.YES_NO_OPTION,
                logo());
        JButton yesButton = UiTheme.primaryButton(yes);
        JButton noButton = UiTheme.secondaryButton(no);
        options(pane, yesButton, noButton);
        pane.setInitialValue(noButton);
        show(pane, parent, title);
        return pane.getValue() == yesButton;
    }

    /** מציג הודעה עם כפתור אישור. */
    public static void info(Component parent, String title, String message) {
        JOptionPane pane = new JOptionPane(message, JOptionPane.INFORMATION_MESSAGE, JOptionPane.DEFAULT_OPTION,
                logo());
        options(pane, UiTheme.primaryButton("אישור"));
        show(pane, parent, title);
    }

    /** מציג הודעת שגיאה, עם אייקון השגיאה הרגיל כדי שיהיה ברור שמשהו לא תקין. */
    public static void error(Component parent, String title, String message) {
        JOptionPane pane = new JOptionPane(message, JOptionPane.ERROR_MESSAGE);
        options(pane, UiTheme.primaryButton("אישור"));
        show(pane, parent, title);
    }

    /**
     * שם בחלון כפתורים בעיצוב של המערכת. לחיצה על כפתור סוגרת את החלון ושומרת אותו כתשובה.
     */
    private static void options(JOptionPane pane, JButton... buttons) {
        for (JButton b : buttons) {
            b.setPreferredSize(new Dimension(Math.max(110, b.getPreferredSize().width / 2), 42));
            b.addActionListener(e -> pane.setValue(b));
        }
        pane.setOptions(buttons);
    }

    private static void show(JOptionPane pane, Component parent, String title) {
        pane.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        paintWhite(pane, UiTheme.SURFACE);
        JDialog dialog = pane.createDialog(parent, title);
        dialog.getContentPane().setBackground(UiTheme.SURFACE);
        dialog.setVisible(true);
        dialog.dispose();
    }

    /** צובע את הרקע של חלון ההודעה ושל הפאנלים שבתוכו. */
    private static void paintWhite(Container c, Color bg) {
        c.setBackground(bg);
        for (Component child : c.getComponents()) {
            if (child instanceof JPanel || child instanceof JOptionPane) {
                paintWhite((Container) child, bg);
            }
        }
    }
}

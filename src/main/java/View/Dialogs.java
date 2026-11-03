package View;

import javax.swing.JDialog;
import javax.swing.JOptionPane;
import java.awt.Component;
import java.awt.ComponentOrientation;

/**
 * חלונות אישור בעברית, מימין לשמאל.
 */
public final class Dialogs {

    private Dialogs() {
    }

    /**
     * מציג שאלת כן/לא.
     *
     * @param parent   החלון שמעליו מוצג הדיאלוג
     * @param title    כותרת החלון
     * @param message  השאלה
     * @param yesText  טקסט כפתור האישור
     * @param noText   טקסט כפתור הביטול
     * @return {@code true} אם המשתמש אישר
     */
    public static boolean confirm(Component parent, String title, String message,
                                  String yesText, String noText) {
        Object[] options = {yesText, noText};
        JOptionPane pane = new JOptionPane(message, JOptionPane.WARNING_MESSAGE,
                JOptionPane.YES_NO_OPTION, null, options, noText);
        pane.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        pane.setFont(UiTheme.BODY);
        JDialog dialog = pane.createDialog(parent, title);
        dialog.applyComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        dialog.setVisible(true);
        dialog.dispose();
        return yesText.equals(pane.getValue());
    }
}

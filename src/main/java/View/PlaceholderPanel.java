package View;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.GridBagLayout;
import java.awt.event.ActionListener;

/**
 * מסך זמני למסכים שימומשו באיטרציה 3 (היסטוריה, ניהול שאלות), עם "חזרה" לתפריט (FR-01).
 */
public class PlaceholderPanel extends JPanel {

    private final JButton backButton = UiTheme.secondaryButton("חזרה לתפריט");

    /**
     * @param title   שם המסך
     * @param message הסבר קצר
     */
    public PlaceholderPanel(String title, String message) {
        setLayout(new GridBagLayout());
        setBackground(UiTheme.BACKGROUND);

        JPanel column = new JPanel();
        column.setOpaque(false);
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));

        JLabel t = new JLabel(title, SwingConstants.CENTER);
        t.setFont(UiTheme.HEADING);
        t.setForeground(UiTheme.PRIMARY);
        JLabel m = new JLabel(message, SwingConstants.CENTER);
        m.setFont(UiTheme.BODY);
        m.setForeground(UiTheme.MUTED);

        for (Component c : new Component[]{t, m, backButton}) {
            ((javax.swing.JComponent) c).setAlignmentX(Component.CENTER_ALIGNMENT);
        }
        column.add(t);
        column.add(Box.createVerticalStrut(10));
        column.add(m);
        column.add(Box.createVerticalStrut(24));
        column.add(backButton);
        add(column);
        applyComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
    }

    /** @param l פעולה ל"חזרה לתפריט" */
    public void onBack(ActionListener l) {
        backButton.addActionListener(l);
    }
}

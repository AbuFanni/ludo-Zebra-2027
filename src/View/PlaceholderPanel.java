package View;

import java.awt.Component;
import java.awt.GridBagLayout;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * מסך זמני לחלק שיתווסף באיטרציה מאוחרת יותר, עם כפתור חזרה לתפריט.
 *
 * @author Nour Eldin Khatib (Zebra) – Iteration 1, SDP table 2
 */
public class PlaceholderPanel extends JPanel {

    private final JButton backButton = UiTheme.secondaryButton("חזרה לתפריט");

    public PlaceholderPanel(String title, String message) {
        setLayout(new GridBagLayout());
        setBackground(UiTheme.BACKGROUND);
        JPanel column = new JPanel();
        column.setOpaque(false);
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));
        JLabel t = UiTheme.label(title, UiTheme.HEADING, UiTheme.PRIMARY);
        JLabel m = UiTheme.label(message, UiTheme.SUBTITLE, UiTheme.MUTED);
        for (Component c : new Component[]{t, m, backButton}) {
            ((javax.swing.JComponent) c).setAlignmentX(Component.CENTER_ALIGNMENT);
        }
        column.add(t);
        column.add(Box.createVerticalStrut(10));
        column.add(m);
        column.add(Box.createVerticalStrut(24));
        backButton.setMaximumSize(backButton.getPreferredSize());
        column.add(backButton);
        add(column);
    }

    public JButton getBackButton() {
        return backButton;
    }
}

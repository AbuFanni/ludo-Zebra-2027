package View;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.event.ActionListener;

/**
 * מסך הפתיחה (FR-01): משחק חדש, היסטוריית משחקים, ניהול שאלות ויציאה.
 * <p>
 * המסך רק מציג; מה קורה בלחיצה קובע ה-Controller דרך {@code on...}.
 */
public class MainMenuPanel extends JPanel {

    private final JButton newGameButton = UiTheme.primaryButton("משחק חדש");
    private final JButton historyButton = UiTheme.primaryButton("היסטוריית משחקים");
    private final JButton questionsButton = UiTheme.primaryButton("ניהול שאלות");
    private final JButton exitButton = UiTheme.secondaryButton("יציאה");

    /** בונה את מסך הפתיחה. */
    public MainMenuPanel() {
        setLayout(new GridBagLayout());
        setBackground(UiTheme.BACKGROUND);

        JPanel column = new JPanel();
        column.setOpaque(false);
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Ludo Zebra", SwingConstants.CENTER);
        title.setFont(UiTheme.TITLE);
        title.setForeground(UiTheme.PRIMARY);

        JLabel subtitle = new JLabel("משחק לודו לימודי – הנדסת תוכנה ואבטחת איכות", SwingConstants.CENTER);
        subtitle.setFont(UiTheme.SUBTITLE);
        subtitle.setForeground(UiTheme.MUTED);

        column.add(center(title));
        column.add(Box.createVerticalStrut(8));
        column.add(center(subtitle));
        column.add(Box.createVerticalStrut(40));
        for (JButton b : new JButton[]{newGameButton, historyButton, questionsButton}) {
            b.setMaximumSize(new Dimension(320, 56));
            column.add(center(b));
            column.add(Box.createVerticalStrut(14));
        }
        exitButton.setMaximumSize(new Dimension(320, 48));
        exitButton.setPreferredSize(new Dimension(320, 48));
        column.add(Box.createVerticalStrut(10));
        column.add(center(exitButton));

        column.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        add(column);
        applyComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
    }

    private static Component center(javax.swing.JComponent c) {
        c.setAlignmentX(Component.CENTER_ALIGNMENT);
        return c;
    }

    /** @param l פעולה ל"משחק חדש" */
    public void onNewGame(ActionListener l) {
        newGameButton.addActionListener(l);
    }

    /** @param l פעולה ל"היסטוריית משחקים" */
    public void onHistory(ActionListener l) {
        historyButton.addActionListener(l);
    }

    /** @param l פעולה ל"ניהול שאלות" */
    public void onQuestions(ActionListener l) {
        questionsButton.addActionListener(l);
    }

    /** @param l פעולה ל"יציאה" */
    public void onExit(ActionListener l) {
        exitButton.addActionListener(l);
    }
}

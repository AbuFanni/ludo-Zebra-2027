package View;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * מסך הפתיחה: משחק חדש, היסטוריית משחקים, ניהול שאלות ויציאה (FR-01).
 * מחליף את {@code WindowInterface.WelcomePanel} בפרויקט המקור, עם אותו לוגו.
 *
 * @author Nour Eldin Khatib (Zebra) – Iteration 1, SDP table 2
 */
public class MainMenuPanel extends JPanel {

    private final JButton newGameButton = UiTheme.primaryButton("משחק חדש");
    private final JButton historyButton = UiTheme.secondaryButton("היסטוריית משחקים");
    private final JButton questionsButton = UiTheme.secondaryButton("ניהול שאלות");
    private final JButton exitButton = UiTheme.secondaryButton("יציאה");

    public MainMenuPanel() {
        setLayout(new GridBagLayout());
        setBackground(UiTheme.BACKGROUND);

        JPanel column = new JPanel();
        column.setOpaque(false);
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));

        Image logo = Images.logo();
        if (logo != null) {
            JLabel logoLabel = new JLabel(new ImageIcon(logo.getScaledInstance(260, -1, Image.SCALE_SMOOTH)));
            logoLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            column.add(logoLabel);
            column.add(Box.createVerticalStrut(8));
        }
        JLabel title = UiTheme.label("Ludo Zebra", UiTheme.TITLE, UiTheme.PRIMARY);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel subtitle = UiTheme.label("לודו לימודי בהנדסת תוכנה ואבטחת איכות", UiTheme.SUBTITLE, UiTheme.MUTED);
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        column.add(title);
        column.add(Box.createVerticalStrut(4));
        column.add(subtitle);
        column.add(Box.createVerticalStrut(28));

        for (JButton b : new JButton[]{newGameButton, historyButton, questionsButton, exitButton}) {
            b.setAlignmentX(Component.CENTER_ALIGNMENT);
            b.setMaximumSize(b.getPreferredSize());
            column.add(b);
            column.add(Box.createVerticalStrut(12));
        }

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(20, 20, 20, 20);
        add(column, c);
    }

    public JButton getNewGameButton() {
        return newGameButton;
    }

    public JButton getHistoryButton() {
        return historyButton;
    }

    public JButton getQuestionsButton() {
        return questionsButton;
    }

    public JButton getExitButton() {
        return exitButton;
    }
}

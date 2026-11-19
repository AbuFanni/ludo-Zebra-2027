package View;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import Model.Player;

/**
 * מסך הסיום (טבלה 2 ב-SRS, FR-53, FR-54): טבלת המקומות לפי סדר הסיום, זמן המשחק ורמת הקושי,
 * עם "משחק חדש" ו"לתפריט".
 * <p>
 * המסך רק מציג. ה-Controller, שמאזין למשחק (Observer, {@code GameListener.onGameOver}), ממלא אותו
 * כשהמשחק נגמר ומחליט לאן עוברים בלחיצה על הכפתורים.
 *
 * @author Nour Eldin Khatib (Zebra) – Iteration 2, US-9 (FR-53, FR-54)
 */
public class GameOverPanel extends JPanel {

    private static final long serialVersionUID = 1L;
    private static final Color[] MEDALS = {
        new Color(0xE3A417), new Color(0xA7A9AC), new Color(0xB87333), new Color(0xD9D9D2)
    };

    private final JPanel table = new JPanel();
    private final JLabel duration = UiTheme.label(" ", UiTheme.SUBTITLE, UiTheme.TEXT);
    private final JLabel difficulty = UiTheme.label(" ", UiTheme.SUBTITLE, UiTheme.TEXT);
    private final JLabel note = UiTheme.label(" ", UiTheme.SMALL, UiTheme.MUTED);
    private final JButton newGameButton = UiTheme.primaryButton("משחק חדש");
    private final JButton menuButton = UiTheme.secondaryButton("לתפריט");

    public GameOverPanel() {
        super(new BorderLayout());
        setBackground(UiTheme.BACKGROUND);
        add(new PlaceholderPanel.StripeBand(), BorderLayout.NORTH);

        JPanel card = new JPanel();
        card.setBackground(UiTheme.SURFACE);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xDCDCD5), 1),
                BorderFactory.createEmptyBorder(28, 40, 28, 40)));

        JLabel title = UiTheme.label("המשחק נגמר!", UiTheme.TITLE, UiTheme.INK);
        table.setOpaque(false);
        table.setLayout(new GridLayout(0, 1, 0, 8));

        JPanel facts = new JPanel(new GridLayout(1, 2, 24, 0));
        facts.setOpaque(false);
        facts.add(fact("זמן המשחק", duration));
        facts.add(fact("רמת קושי", difficulty));

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        buttons.setOpaque(false);
        newGameButton.setPreferredSize(new Dimension(200, 48));
        menuButton.setPreferredSize(new Dimension(200, 48));
        buttons.add(newGameButton);
        buttons.add(menuButton);

        for (JComponent c : new JComponent[]{title, table, facts, note, buttons}) {
            c.setAlignmentX(Component.CENTER_ALIGNMENT);
        }
        card.add(title);
        card.add(Box.createVerticalStrut(20));
        card.add(table);
        card.add(Box.createVerticalStrut(20));
        card.add(facts);
        card.add(Box.createVerticalStrut(10));
        card.add(note);
        card.add(Box.createVerticalStrut(22));
        card.add(buttons);

        JPanel center = new JPanel(new GridBagLayout());
        center.setOpaque(false);
        center.add(card);
        add(center, BorderLayout.CENTER);
        applyComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
    }

    /**
     * ממלא את המסך בתוצאות המשחק.
     *
     * @param ranking        השחקנים לפי המקומות; האחרון הוא מי שנשאר בסוף (FR-54)
     * @param durationText   זמן המשחק (HH:MM:SS)
     * @param difficultyName רמת הקושי בעברית
     * @param saveNote       הודעה על השמירה בהיסטוריה, או ריק
     */
    public void showResults(List<Player> ranking, String durationText, String difficultyName, String saveNote) {
        table.removeAll();
        for (Player p : ranking) {
            table.add(new PlaceRow(p));
        }
        duration.setText(durationText);
        difficulty.setText(difficultyName);
        note.setText(saveNote == null || saveNote.isEmpty() ? " " : saveNote);
        applyComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        revalidate();
        repaint();
    }

    public JButton getNewGameButton() {
        return newGameButton;
    }

    public JButton getMenuButton() {
        return menuButton;
    }

    private static JPanel fact(String name, JLabel value) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        JLabel n = UiTheme.label(name, UiTheme.SMALL, UiTheme.MUTED);
        value.setFont(UiTheme.HEADING);
        n.setAlignmentX(Component.CENTER_ALIGNMENT);
        value.setAlignmentX(Component.CENTER_ALIGNMENT);
        p.add(n);
        p.add(value);
        return p;
    }

    /** שורה בטבלת המקומות: מדליה עם מספר המקום, נקודת הצבע והשם. */
    private static final class PlaceRow extends JPanel {

        private static final long serialVersionUID = 1L;

        PlaceRow(Player p) {
            super(new BorderLayout(14, 0));
            setOpaque(false);
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(0xE6E6E0)),
                    BorderFactory.createEmptyBorder(6, 4, 8, 4)));
            int place = p.getPlace();
            Medal medal = new Medal(place, MEDALS[Math.max(0, Math.min(MEDALS.length, place) - 1)]);
            JLabel name = UiTheme.label(p.getName(), UiTheme.HEADING.deriveFont(Font.PLAIN, 20f), UiTheme.TEXT);
            name.setIcon(new SurpriseDialog.Dot(UiTheme.toAwt(p.getColor())));
            name.setIconTextGap(10);
            JLabel placeText = UiTheme.label("מקום " + place, UiTheme.BODY, UiTheme.MUTED);
            placeText.setHorizontalAlignment(SwingConstants.TRAILING);
            add(medal, BorderLayout.LINE_START);
            add(name, BorderLayout.CENTER);
            add(placeText, BorderLayout.LINE_END);
            setPreferredSize(new Dimension(380, 50));
        }
    }

    /** עיגול בצבע המדליה עם מספר המקום. */
    private static final class Medal extends JComponent {

        private static final long serialVersionUID = 1L;
        private final int place;
        private final Color color;

        Medal(int place, Color color) {
            this.place = place;
            this.color = color;
            setPreferredSize(new Dimension(38, 38));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            ZebraArt.smooth(g2);
            int d = Math.min(getWidth(), getHeight()) - 2;
            g2.setColor(color);
            g2.fillOval(1, 1, d, d);
            g2.setColor(UiTheme.INK);
            g2.drawOval(1, 1, d, d);
            g2.setFont(UiTheme.BUTTON);
            String s = String.valueOf(place);
            int w = g2.getFontMetrics().stringWidth(s);
            int h = g2.getFontMetrics().getAscent();
            g2.drawString(s, 1 + (d - w) / 2, 1 + (d + h) / 2 - 2);
            g2.dispose();
        }
    }
}

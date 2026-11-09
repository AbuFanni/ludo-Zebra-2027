package View;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.image.BufferedImage;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/**
 * מסך הפתיחה: משחק חדש, היסטוריית משחקים, ניהול שאלות ויציאה (FR-01).
 * <p>
 * מצד ימין התפריט, ומצד שמאל פאנל עם פסי זברה והלוגו של הקבוצה.
 * מחליף את {@code WindowInterface.WelcomePanel} בפרויקט המקור.
 *
 * @author Nour Eldin Khatib (Zebra) – Iteration 1, SDP table 2
 */
public class MainMenuPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private final JButton newGameButton = UiTheme.primaryButton("משחק חדש");
    private final JButton historyButton = UiTheme.secondaryButton("היסטוריית משחקים");
    private final JButton questionsButton = UiTheme.secondaryButton("ניהול שאלות");
    private final JButton exitButton = UiTheme.quietButton("יציאה");

    public MainMenuPanel() {
        super(new BorderLayout());
        setBackground(UiTheme.BACKGROUND);

        JPanel menu = new JPanel(new GridBagLayout());
        menu.setBackground(UiTheme.BACKGROUND);
        menu.setPreferredSize(new Dimension(440, 600));
        menu.setBorder(BorderFactory.createEmptyBorder(0, 48, 0, 48));

        JPanel column = new JPanel();
        column.setOpaque(false);
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));

        JLabel title = UiTheme.label("Ludo Zebra", UiTheme.DISPLAY, UiTheme.INK);
        JLabel subtitle = UiTheme.label("<html><div dir='rtl' style='width:300px;text-align:right'>לודו עם שאלות בהנדסת תוכנה. "
                + "2 עד 4 שחקנים על מחשב אחד.</div></html>", UiTheme.SUBTITLE, UiTheme.MUTED);
        subtitle.setHorizontalAlignment(SwingConstants.RIGHT);
        stack(column, title, 6);
        stack(column, subtitle, 36);

        for (JButton b : new JButton[]{newGameButton, historyButton, questionsButton}) {
            b.setMaximumSize(new Dimension(320, 52));
            b.setPreferredSize(new Dimension(320, 52));
            stack(column, b, 12);
        }
        exitButton.setMaximumSize(new Dimension(320, 44));
        exitButton.setPreferredSize(new Dimension(320, 44));
        stack(column, exitButton, 40);

        JLabel footer = UiTheme.label("קבוצת Zebra, אוניברסיטת חיפה", UiTheme.SMALL, UiTheme.MUTED);
        stack(column, footer, 0);

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(24, 0, 24, 0);
        menu.add(column, c);

        add(menu, BorderLayout.LINE_START);
        add(new StripeHero(), BorderLayout.CENTER);
    }

    private static void stack(JPanel column, Component c, int gapAfter) {
        ((javax.swing.JComponent) c).setAlignmentX(Component.RIGHT_ALIGNMENT);
        column.add(c);
        if (gapAfter > 0) {
            column.add(Box.createVerticalStrut(gapAfter));
        }
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

    /**
     * הפאנל של פסי הזברה עם הלוגו במרכז. הציור נשמר בתמונה ומצויר מחדש רק כשגודל החלון משתנה.
     */
    private static final class StripeHero extends JPanel {

        private static final long serialVersionUID = 1L;

        private transient BufferedImage cache;

        StripeHero() {
            setOpaque(true);
            setBackground(UiTheme.PAPER);
            setPreferredSize(new Dimension(520, 600));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            int w = getWidth();
            int h = getHeight();
            if (w <= 0 || h <= 0) {
                return;
            }
            if (cache == null || cache.getWidth() != w || cache.getHeight() != h) {
                cache = render(w, h);
            }
            g.drawImage(cache, 0, 0, null);
        }

        private static BufferedImage render(int w, int h) {
            BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = img.createGraphics();
            ZebraArt.smooth(g);
            g.setColor(UiTheme.PAPER);
            g.fillRect(0, 0, w, h);
            double spacing = Math.max(46, Math.min(w, h) / 9.0);
            ZebraArt.paintStripes(g, w, h, spacing, -32, 2027L, UiTheme.INK);

            double size = Math.min(w, h) * 0.46;
            double x = (w - size) / 2;
            double y = (h - size) / 2;
            // הילה לבנה סביב הלוגו כדי שיבלוט מעל הפסים
            g.setColor(UiTheme.PAPER);
            double halo = size * 0.045;
            g.fill(new java.awt.geom.Ellipse2D.Double(x - halo, y - halo, size + 2 * halo, size + 2 * halo));
            ZebraArt.paintLogo(g, x, y, size);
            g.dispose();
            return img;
        }
    }
}

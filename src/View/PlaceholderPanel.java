package View;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/**
 * מסך זמני לחלק שיתווסף באיטרציה מאוחרת יותר, עם כפתור חזרה לתפריט.
 * בראש המסך רצועת פסי זברה, ובמרכז כרטיס עם הלוגו וההסבר.
 *
 * @author Nour Eldin Khatib (Zebra) – Iteration 1, SDP table 2
 */
public class PlaceholderPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private final JButton backButton = UiTheme.secondaryButton("חזרה לתפריט");

    public PlaceholderPanel(String title, String message) {
        super(new BorderLayout());
        setBackground(UiTheme.BACKGROUND);
        add(new StripeBand(), BorderLayout.NORTH);

        JPanel card = new JPanel();
        card.setBackground(UiTheme.SURFACE);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new java.awt.Color(0xDCDCD5), 1),
                BorderFactory.createEmptyBorder(32, 40, 32, 40)));

        JComponent logo = new JComponent() {
            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(Graphics g) {
                ZebraArt.paintLogo((Graphics2D) g, 0, 0, Math.min(getWidth(), getHeight()));
            }
        };
        logo.setPreferredSize(new Dimension(84, 84));
        logo.setMaximumSize(new Dimension(84, 84));

        JLabel t = UiTheme.label(title, UiTheme.HEADING, UiTheme.INK);
        JLabel m = UiTheme.label("<html><div style='width:320px;text-align:center'>" + message + "</div></html>",
                UiTheme.BODY, UiTheme.MUTED);
        m.setHorizontalAlignment(SwingConstants.CENTER);
        backButton.setMaximumSize(new Dimension(240, 48));
        backButton.setPreferredSize(new Dimension(240, 48));

        for (JComponent c : new JComponent[]{logo, t, m, backButton}) {
            c.setAlignmentX(Component.CENTER_ALIGNMENT);
        }
        card.add(logo);
        card.add(Box.createVerticalStrut(18));
        card.add(t);
        card.add(Box.createVerticalStrut(8));
        card.add(m);
        card.add(Box.createVerticalStrut(26));
        card.add(backButton);

        JPanel center = new JPanel(new GridBagLayout());
        center.setOpaque(false);
        center.add(card);
        add(center, BorderLayout.CENTER);
    }

    public JButton getBackButton() {
        return backButton;
    }

    /** רצועה דקה של פסי זברה בראש המסך. */
    static final class StripeBand extends JComponent {

        private static final long serialVersionUID = 1L;

        StripeBand() {
            setPreferredSize(new Dimension(100, 44));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            ZebraArt.smooth(g2);
            g2.setColor(UiTheme.PAPER);
            g2.fillRect(0, 0, getWidth(), getHeight());
            ZebraArt.paintStripes(g2, getWidth(), getHeight(), 38, -18, 2027L, UiTheme.INK);
            g2.dispose();
        }
    }
}

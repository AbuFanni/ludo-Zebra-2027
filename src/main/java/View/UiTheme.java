package View;

import Model.Difficulty;
import Model.PlayerColor;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;

/**
 * צבעים, גופנים ורכיבים משותפים לכל המסכים, כדי שכל הצוות ישתמש באותו עיצוב.
 */
public final class UiTheme {

    public static final Color BACKGROUND = new Color(0xF5F1E8);
    public static final Color PRIMARY = new Color(0x1F3A5F);
    public static final Color PRIMARY_HOVER = new Color(0x2C5282);
    public static final Color TEXT = new Color(0x1A1A1A);
    public static final Color MUTED = new Color(0x5A5A5A);
    public static final Color ERROR = new Color(0xC0392B);

    public static final Font TITLE = new Font(Font.SANS_SERIF, Font.BOLD, 40);
    public static final Font SUBTITLE = new Font(Font.SANS_SERIF, Font.PLAIN, 18);
    public static final Font HEADING = new Font(Font.SANS_SERIF, Font.BOLD, 22);
    public static final Font BODY = new Font(Font.SANS_SERIF, Font.PLAIN, 16);
    public static final Font SMALL = new Font(Font.SANS_SERIF, Font.PLAIN, 13);

    private UiTheme() {
    }

    /** @return צבע המסך של צבע שחקן. ההמרה כאן ולא במודל (NFR-06). */
    public static Color toAwt(PlayerColor color) {
        return switch (color) {
            case RED -> new Color(0xD64541);
            case GREEN -> new Color(0x2E9E5B);
            case YELLOW -> new Color(0xE9B824);
            case BLUE -> new Color(0x2F6FD0);
        };
    }

    /** @return צבע הרמה לפי FR-12: ירוק לקל, צהוב לבינוני, אדום לקשה. */
    public static Color toAwt(Difficulty difficulty) {
        return switch (difficulty) {
            case EASY -> new Color(0x2E9E5B);
            case MEDIUM -> new Color(0xE9B824);
            case HARD -> new Color(0xD64541);
        };
    }

    /** @return כפתור ראשי בעיצוב האחיד. */
    public static JButton primaryButton(String text) {
        JButton b = new JButton(text);
        b.setFont(BODY.deriveFont(Font.BOLD, 18f));
        b.setForeground(Color.WHITE);
        b.setBackground(PRIMARY);
        b.setFocusPainted(false);
        b.setOpaque(true);
        b.setBorderPainted(false);
        b.setBorder(BorderFactory.createEmptyBorder(12, 24, 12, 24));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setPreferredSize(new Dimension(280, 52));
        b.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                b.setBackground(PRIMARY_HOVER);
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                b.setBackground(PRIMARY);
            }
        });
        return b;
    }

    /** @return כפתור משני (חזרה, ביטול). */
    public static JButton secondaryButton(String text) {
        JButton b = new JButton(text);
        b.setFont(BODY);
        b.setForeground(PRIMARY);
        b.setBackground(Color.WHITE);
        b.setFocusPainted(false);
        b.setOpaque(true);
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(PRIMARY, 1),
                BorderFactory.createEmptyBorder(10, 20, 10, 20)));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    /** @return תווית מיושרת לימין בגופן ובצבע הנתונים. */
    public static JLabel label(String text, Font font, Color color) {
        JLabel l = new JLabel(text, SwingConstants.RIGHT);
        l.setFont(font);
        l.setForeground(color);
        return l;
    }
}

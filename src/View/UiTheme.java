package View;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.util.Arrays;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;

import Model.PlayerColor;
import Model.QuestionLevel;

/**
 * הצבעים, הגופנים והכפתורים המשותפים לכל המסכים, כדי שכל המסכים ייראו אותו דבר.
 *
 * @author Nour Eldin Khatib (Zebra) – Iteration 1, SDP table 2
 */
public final class UiTheme {

    public static final Color BACKGROUND = new Color(0xF6F1E7);
    public static final Color SURFACE = Color.WHITE;
    public static final Color PRIMARY = new Color(0x1F3A5F);
    public static final Color ACCENT = new Color(0xE0A100);
    public static final Color TEXT = new Color(0x22262E);
    public static final Color MUTED = new Color(0x6B7280);
    public static final Color ERROR = new Color(0xC62828);
    public static final Color LINE = new Color(0x3A3A3A);
    public static final Color BOARD_BG = new Color(0xFFFDF8);
    public static final Color SURPRISE = new Color(0x7B3FA0);

    private static final String FONT_FAMILY = pickFont();

    public static final Font TITLE = new Font(FONT_FAMILY, Font.BOLD, 34);
    public static final Font HEADING = new Font(FONT_FAMILY, Font.BOLD, 22);
    public static final Font SUBTITLE = new Font(FONT_FAMILY, Font.PLAIN, 17);
    public static final Font BODY = new Font(FONT_FAMILY, Font.PLAIN, 15);
    public static final Font SMALL = new Font(FONT_FAMILY, Font.PLAIN, 13);
    public static final Font BUTTON = new Font(FONT_FAMILY, Font.BOLD, 16);

    private UiTheme() {
    }

    /** בוחר גופן שקיים במחשב ותומך בעברית. */
    private static String pickFont() {
        List<String> installed = Arrays.asList(
                GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames());
        for (String f : new String[]{"Segoe UI", "Arial", "Tahoma", "Rubik", "Heebo", "DejaVu Sans"}) {
            if (installed.contains(f)) {
                return f;
            }
        }
        return Font.SANS_SERIF;
    }

    /** @return צבע המסך של צבע השחקן */
    public static Color toAwt(PlayerColor c) {
        switch (c) {
            case GREEN:
                return new Color(0x1E9E4A);
            case YELLOW:
                return new Color(0xF2C200);
            case BLUE:
                return new Color(0x1E63D6);
            case RED:
            default:
                return new Color(0xE0322B);
        }
    }

    /** @return גוון בהיר של צבע השחקן, למסלול הבית */
    public static Color light(PlayerColor c) {
        Color base = toAwt(c);
        return mix(base, Color.WHITE, 0.35);
    }

    /** @return צבע משבצת שאלה לפי הרמה: ירוק, צהוב, אדום */
    public static Color question(QuestionLevel level) {
        switch (level) {
            case EASY:
                return new Color(0xBFE8C8);
            case MEDIUM:
                return new Color(0xFBE7A1);
            case HARD:
            default:
                return new Color(0xF6B7B2);
        }
    }

    /** @return ערבוב של שני צבעים; ratio הוא החלק של a */
    public static Color mix(Color a, Color b, double ratio) {
        double r = Math.max(0, Math.min(1, ratio));
        return new Color(
                (int) (a.getRed() * r + b.getRed() * (1 - r)),
                (int) (a.getGreen() * r + b.getGreen() * (1 - r)),
                (int) (a.getBlue() * r + b.getBlue() * (1 - r)));
    }

    public static JLabel label(String text, Font font, Color color) {
        JLabel l = new JLabel(text);
        l.setFont(font);
        l.setForeground(color);
        return l;
    }

    /** כפתור ראשי, בצבע הכחול של המשחק. */
    public static JButton primaryButton(String text) {
        return button(text, PRIMARY, Color.WHITE);
    }

    /** כפתור משני, לבן עם מסגרת. */
    public static JButton secondaryButton(String text) {
        JButton b = button(text, SURFACE, PRIMARY);
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(PRIMARY, 2),
                BorderFactory.createEmptyBorder(8, 18, 8, 18)));
        return b;
    }

    private static JButton button(String text, Color bg, Color fg) {
        JButton b = new JButton(text);
        b.setFont(BUTTON);
        b.setBackground(bg);
        b.setForeground(fg);
        b.setFocusPainted(false);
        b.setOpaque(true);
        b.setContentAreaFilled(true);
        b.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setPreferredSize(new Dimension(260, 48));
        return b;
    }
}

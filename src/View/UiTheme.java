package View;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.geom.RoundRectangle2D;
import java.util.Arrays;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.ButtonModel;
import javax.swing.JButton;
import javax.swing.JLabel;

import Model.PlayerColor;
import Model.QuestionLevel;

/**
 * העיצוב המשותף לכל המסכים: הצבעים, הגופנים והכפתורים של הזהות של קבוצת Zebra.
 * <p>
 * הזהות: שחור ולבן של פסי זברה, וצבע דגש אחד, זהב של שמש הסוואנה, לפוקוס ולמעבר עכבר.
 * כל המסכים משתמשים בקבועים ובכפתורים מכאן, כך ששינוי כאן מעדכן את כל המערכת.
 * צבעי השחקנים והלוח נשארים הצבעים הרגילים של לודו.
 *
 * @author Nour Eldin Khatib (Zebra) – Iteration 1, SDP table 2
 */
public final class UiTheme {

    /** השחור של פסי הזברה. */
    public static final Color INK = new Color(0x121212);
    /** הלבן של פסי הזברה. */
    public static final Color PAPER = Color.WHITE;

    public static final Color BACKGROUND = new Color(0xF1F1EC);
    public static final Color SURFACE = PAPER;
    public static final Color PRIMARY = INK;
    /** זהב של שמש הסוואנה: הלוגו, פוקוס ומעבר עכבר. */
    public static final Color ACCENT = new Color(0xE3A417);
    public static final Color TEXT = new Color(0x1C1C1C);
    public static final Color MUTED = new Color(0x66665F);
    public static final Color ERROR = new Color(0xC62828);
    public static final Color LINE = new Color(0x3A3A3A);
    public static final Color BOARD_BG = new Color(0xFFFDF8);
    public static final Color SURPRISE = new Color(0x7B3FA0);

    private static final List<String> INSTALLED = Arrays.asList(
            GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames());
    private static final String FONT_FAMILY = pickFont();

    /** הכותרת הגדולה של מסך הפתיחה. */
    public static final Font DISPLAY = displayFont(52);
    public static final Font TITLE = new Font(FONT_FAMILY, Font.BOLD, 34);
    public static final Font HEADING = new Font(FONT_FAMILY, Font.BOLD, 22);
    public static final Font SUBTITLE = new Font(FONT_FAMILY, Font.PLAIN, 17);
    public static final Font BODY = new Font(FONT_FAMILY, Font.PLAIN, 15);
    public static final Font SMALL = new Font(FONT_FAMILY, Font.PLAIN, 13);
    public static final Font BUTTON = semiboldFont(16);

    private UiTheme() {
    }

    /** בוחר גופן שקיים במחשב ותומך בעברית. */
    private static String pickFont() {
        for (String f : new String[]{"Segoe UI", "Arial", "Tahoma", "Rubik", "Heebo", "DejaVu Sans"}) {
            if (INSTALLED.contains(f)) {
                return f;
            }
        }
        return Font.SANS_SERIF;
    }

    /** גופן כבד לכותרת: Segoe UI Black ב-Windows, ובמחשבים אחרים הגופן הרגיל בעובי Bold. */
    private static Font displayFont(int size) {
        if (INSTALLED.contains("Segoe UI Black")) {
            return new Font("Segoe UI Black", Font.PLAIN, size);
        }
        return new Font(FONT_FAMILY, Font.BOLD, size);
    }

    private static Font semiboldFont(int size) {
        if (INSTALLED.contains("Segoe UI Semibold")) {
            return new Font("Segoe UI Semibold", Font.PLAIN, size);
        }
        return new Font(FONT_FAMILY, Font.BOLD, size);
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

    /** כפתור ראשי: שחור עם טקסט לבן. */
    public static JButton primaryButton(String text) {
        return new ZebraButton(text, ZebraButton.Style.PRIMARY);
    }

    /** כפתור משני: לבן עם מסגרת שחורה. */
    public static JButton secondaryButton(String text) {
        return new ZebraButton(text, ZebraButton.Style.SECONDARY);
    }

    /** כפתור שקט, בלי רקע, לפעולות כמו יציאה. */
    public static JButton quietButton(String text) {
        return new ZebraButton(text, ZebraButton.Style.QUIET);
    }

    /**
     * כפתור מעוגל שמצויר בקוד, עם מצב מעבר עכבר, לחיצה, מושבת ופוקוס מקלדת (טבעת זהב).
     */
    static final class ZebraButton extends JButton {

        private static final long serialVersionUID = 1L;
        private static final int ARC = 12;

        enum Style { PRIMARY, SECONDARY, QUIET }

        private final Style style;

        ZebraButton(String text, Style style) {
            super(text);
            this.style = style;
            setFont(BUTTON);
            setFocusPainted(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setOpaque(false);
            setRolloverEnabled(true);
            setBorder(BorderFactory.createEmptyBorder(10, 22, 10, 22));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setPreferredSize(new Dimension(280, 50));
            setForeground(style == Style.PRIMARY ? PAPER : INK);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            ZebraArt.smooth(g2);
            ButtonModel m = getModel();
            double w = getWidth();
            double h = getHeight();
            RoundRectangle2D.Double shape = new RoundRectangle2D.Double(2, 2, w - 4, h - 4, ARC, ARC);

            Color fill = null;
            Color outline = null;
            Color text = INK;
            switch (style) {
                case PRIMARY:
                    fill = !isEnabled() ? new Color(0xB9B9B3) : m.isPressed() ? Color.BLACK
                            : m.isRollover() ? new Color(0x2B2B2B) : INK;
                    text = PAPER;
                    break;
                case SECONDARY:
                    fill = m.isPressed() ? new Color(0xE4E4DE) : m.isRollover() ? new Color(0xF4F4EF) : PAPER;
                    outline = isEnabled() ? INK : new Color(0xB9B9B3);
                    text = isEnabled() ? INK : MUTED;
                    break;
                case QUIET:
                default:
                    fill = m.isRollover() || m.isPressed() ? new Color(0xE6E6E0) : null;
                    text = m.isRollover() ? INK : MUTED;
                    break;
            }
            if (fill != null) {
                g2.setColor(fill);
                g2.fill(shape);
            }
            if (outline != null) {
                g2.setColor(outline);
                g2.setStroke(new BasicStroke(1.6f));
                g2.draw(shape);
            }
            if (m.isRollover() && isEnabled() && style != Style.QUIET) {
                // פס זהב קצר בתחתית הכפתור במעבר עכבר
                g2.setColor(ACCENT);
                double bw = Math.min(56, w * 0.25);
                g2.fill(new RoundRectangle2D.Double((w - bw) / 2, h - 7, bw, 3, 3, 3));
            }
            if (isFocusOwner()) {
                g2.setColor(ACCENT);
                g2.setStroke(new BasicStroke(2.5f));
                g2.draw(new RoundRectangle2D.Double(1, 1, w - 2, h - 2, ARC + 2, ARC + 2));
            }
            g2.dispose();
            setForeground(text);
            super.paintComponent(g);
        }
    }
}

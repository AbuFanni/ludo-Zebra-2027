package View;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Timer;

import Model.Player;

/**
 * מסך הסיום (טבלה 2 ב-SRS, FR-53, FR-54) בשפה העיצובית של Zebra: אותה פריסה כמו מסך הפתיחה –
 * עמודת תוצאות, וצד של פסי זברה עם הכלי של המנצח בתוך עיגול השמש והכתר. בעמודה יש פודיום שעולה
 * מהמקום האחרון לראשון, זמן המשחק ורמת הקושי, ו"משחק חדש" / "לתפריט". כשהמסך מופיע יורד קונפטי
 * בצבעי השחקנים.
 * <p>
 * המסך רק מציג. ה-Controller, שמאזין למשחק (Observer, {@code GameListener.onGameOver}), ממלא אותו
 * כשהמשחק נגמר ומחליט לאן עוברים בלחיצה על הכפתורים.
 *
 * @author Nour Eldin Khatib (Zebra) – Iteration 2, US-9 (FR-53, FR-54)
 */
public class GameOverPanel extends JPanel {

    private static final long serialVersionUID = 1L;
    static final Color[] MEDALS = {
        new Color(0xE3A417), new Color(0xA7A9AC), new Color(0xB87333), new Color(0xD9D9D2)
    };

    private final JLabel winnerLine = UiTheme.label(" ", UiTheme.SUBTITLE, UiTheme.MUTED);
    private final Podium podium = new Podium();
    private final JLabel others = UiTheme.label(" ", UiTheme.BODY, UiTheme.MUTED);
    private final JLabel duration = UiTheme.label(" ", UiTheme.HEADING, UiTheme.TEXT);
    private final JLabel difficulty = UiTheme.label(" ", UiTheme.HEADING, UiTheme.TEXT);
    private final JLabel note = UiTheme.label(" ", UiTheme.SMALL, UiTheme.MUTED);
    private final JButton newGameButton = UiTheme.primaryButton("משחק חדש");
    private final JButton menuButton = UiTheme.secondaryButton("לתפריט");
    private final Hero hero = new Hero();

    public GameOverPanel() {
        super(new BorderLayout());
        setBackground(UiTheme.BACKGROUND);

        JPanel side = new JPanel(new GridBagLayout());
        side.setBackground(UiTheme.BACKGROUND);
        side.setPreferredSize(new Dimension(500, 600));
        side.setBorder(BorderFactory.createEmptyBorder(0, 40, 0, 40));

        JPanel column = new JPanel();
        column.setOpaque(false);
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));

        // גופן הכותרות הרגיל, כי גופן התצוגה (DISPLAY) לא תומך בעברית בכל מערכת
        JLabel title = UiTheme.label("המשחק נגמר!", UiTheme.TITLE.deriveFont(40f), UiTheme.INK);
        stack(column, title, 4);
        stack(column, winnerLine, 18);
        stack(column, podium, 6);
        stack(column, others, 18);

        JPanel facts = new JPanel(new GridLayout(1, 2, 16, 0));
        facts.setOpaque(false);
        facts.add(fact("זמן המשחק", duration));
        facts.add(fact("רמת קושי", difficulty));
        facts.setMaximumSize(new Dimension(400, 70));
        stack(column, facts, 8);
        stack(column, note, 22);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
        buttons.setOpaque(false);
        newGameButton.setPreferredSize(new Dimension(190, 50));
        menuButton.setPreferredSize(new Dimension(190, 50));
        buttons.add(newGameButton);
        buttons.add(Box.createHorizontalStrut(12));
        buttons.add(menuButton);
        buttons.setMaximumSize(new Dimension(400, 50));
        stack(column, buttons, 0);

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(24, 0, 24, 0);
        side.add(column, c);

        add(side, BorderLayout.LINE_START);
        add(hero, BorderLayout.CENTER);
        applyComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
    }

    /**
     * ממלא את המסך בתוצאות המשחק ומפעיל את האנימציות.
     *
     * @param ranking        השחקנים לפי המקומות; האחרון הוא מי שנשאר בסוף (FR-54)
     * @param durationText   זמן המשחק (HH:MM:SS)
     * @param difficultyName רמת הקושי בעברית
     * @param saveNote       הודעה על השמירה בהיסטוריה, או ריק
     */
    public void showResults(List<Player> ranking, String durationText, String difficultyName, String saveNote) {
        Player winner = ranking.isEmpty() ? null : ranking.get(0);
        // RLM בהתחלה: שם באנגלית לא יהפוך את כיוון השורה
        winnerLine.setText(winner == null ? " " : "\u200F" + winner.getName() + " ניצח!");
        podium.setRanking(ranking);
        if (ranking.size() > 3) {
            StringBuilder sb = new StringBuilder();
            for (int i = 3; i < ranking.size(); i++) {
                Player p = ranking.get(i);
                sb.append(sb.length() == 0 ? "" : ", ").append("מקום ").append(p.getPlace()).append(": ").append(p.getName());
            }
            others.setText(sb.toString());
        } else {
            others.setText(" ");
        }
        duration.setText(durationText);
        difficulty.setText(difficultyName);
        note.setText(saveNote == null || saveNote.isEmpty() ? " " : saveNote);
        hero.setWinner(winner, ranking);
        applyComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        revalidate();
        repaint();
        if (SurpriseDialog.animate) {
            podium.animate();
            hero.celebrate();
        } else {
            podium.finish();
        }
    }

    public JButton getNewGameButton() {
        return newGameButton;
    }

    public JButton getMenuButton() {
        return menuButton;
    }

    private static void stack(JPanel column, JComponent c, int gapAfter) {
        c.setAlignmentX(Component.RIGHT_ALIGNMENT);
        column.add(c);
        if (gapAfter > 0) {
            column.add(Box.createVerticalStrut(gapAfter));
        }
    }

    private static JPanel fact(String name, JLabel value) {
        JPanel p = new JPanel();
        p.setOpaque(true);
        p.setBackground(UiTheme.PAPER);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xDCDCD5), 1),
                BorderFactory.createEmptyBorder(8, 14, 8, 14)));
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        JLabel n = UiTheme.label(name, UiTheme.SMALL, UiTheme.MUTED);
        n.setAlignmentX(Component.RIGHT_ALIGNMENT);
        value.setAlignmentX(Component.RIGHT_ALIGNMENT);
        p.add(n);
        p.add(value);
        return p;
    }

    /** easeOutBack: עולה קצת מעבר ליעד וחוזר, כמו קפיץ. */
    private static double easeOutBack(double t) {
        double c1 = 1.70158;
        double c3 = c1 + 1;
        return 1 + c3 * Math.pow(t - 1, 3) + c1 * Math.pow(t - 1, 2);
    }

    // ------------------------------------------------------------------ פודיום

    /**
     * פודיום של שלושת המקומות הראשונים (2 – 1 – 3). הבמות עולות אחת אחרי השנייה, מהמקום האחרון
     * לראשון, ועל כל במה יש מדליה, נקודת צבע ושם.
     */
    private static final class Podium extends JComponent {

        private static final long serialVersionUID = 1L;
        private static final double[] HEIGHTS = {118, 86, 62};
        private static final long STAGGER = 380;
        private static final long RISE = 520;

        private List<Player> top = Collections.emptyList();
        private long startMillis = -1;
        private transient Timer timer;

        Podium() {
            setPreferredSize(new Dimension(400, 230));
            setMaximumSize(new Dimension(400, 230));
        }

        void setRanking(List<Player> ranking) {
            top = new ArrayList<Player>(ranking.subList(0, Math.min(3, ranking.size())));
        }

        void animate() {
            startMillis = System.currentTimeMillis();
            if (timer != null) {
                timer.stop();
            }
            timer = new Timer(16, e -> {
                repaint();
                if (System.currentTimeMillis() - startMillis > STAGGER * 3 + RISE + 100) {
                    ((Timer) e.getSource()).stop();
                }
            });
            timer.start();
        }

        void finish() {
            startMillis = -1;
            repaint();
        }

        /** כמה הבמה של המקום ה-place (0 = ראשון) כבר עלתה, 0 עד בערך 1. */
        private double progress(int place) {
            if (startMillis < 0) {
                return 1;
            }
            int order = top.size() - 1 - place; // האחרון עולה ראשון
            double t = (System.currentTimeMillis() - startMillis - order * STAGGER) / (double) RISE;
            t = Math.max(0, Math.min(1, t));
            return t == 0 ? 0 : easeOutBack(t);
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            ZebraArt.smooth(g);
            int w = getWidth();
            int h = getHeight();
            double base = h - 4;
            g.setColor(UiTheme.INK);
            g.fillRect(0, (int) base, w, 4);
            // סדר הבמות משמאל לימין: שני, ראשון, שלישי
            int[] visual = top.size() >= 3 ? new int[]{1, 0, 2} : top.size() == 2 ? new int[]{1, 0} : new int[]{0};
            double colW = w / 3.0;
            double startX = (w - colW * visual.length) / 2;
            for (int i = 0; i < visual.length; i++) {
                int place = visual[i];
                Player p = top.get(place);
                double prog = progress(place);
                double bh = HEIGHTS[place] * prog;
                double x = startX + i * colW + 8;
                double bw = colW - 16;
                double y = base - bh;
                // הבמה: לבנה עם פסי זברה עדינים ופס בצבע השחקן למעלה
                RoundRectangle2D block = new RoundRectangle2D.Double(x, y, bw, bh + 8, 10, 10);
                g.setColor(UiTheme.PAPER);
                g.fill(block);
                Graphics2D clip = (Graphics2D) g.create();
                clip.clip(block);
                clip.translate(x, y);
                ZebraArt.paintStripes(clip, bw, Math.max(1, bh), 22, -30, 7L + place,
                        UiTheme.mix(UiTheme.INK, UiTheme.PAPER, 0.09));
                clip.dispose();
                g.setColor(UiTheme.toAwt(p.getColor()));
                g.fill(new RoundRectangle2D.Double(x, y, bw, Math.min(10, bh), 10, 10));
                g.setColor(UiTheme.INK);
                g.setStroke(new BasicStroke(2f));
                g.draw(block);
                if (prog <= 0.05) {
                    continue;
                }
                // מספר המקום על הבמה
                g.setFont(UiTheme.DISPLAY.deriveFont(Math.max(12f, (float) Math.min(40, bh * 0.45))));
                FontMetrics fm = g.getFontMetrics();
                String num = String.valueOf(p.getPlace());
                g.setColor(UiTheme.INK);
                if (bh > 30) {
                    g.drawString(num, (float) (x + (bw - fm.stringWidth(num)) / 2), (float) (y + bh / 2 + fm.getAscent() / 3.0 + 6));
                }
                // מדליה ושם מעל הבמה
                double alpha = Math.min(1, prog);
                Graphics2D a = (Graphics2D) g.create();
                a.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, (float) alpha));
                double md = 30;
                double mx = x + (bw - md) / 2;
                double my = y - md - 30;
                a.setColor(MEDALS[Math.min(place, MEDALS.length - 1)]);
                a.fill(new Ellipse2D.Double(mx, my, md, md));
                a.setColor(UiTheme.INK);
                a.setStroke(new BasicStroke(2f));
                a.draw(new Ellipse2D.Double(mx, my, md, md));
                if (place == 0) {
                    paintCrown(a, mx + md * 0.15, my - md * 0.45, md * 0.7);
                }
                a.setFont(UiTheme.BUTTON);
                FontMetrics nf = a.getFontMetrics();
                String name = p.getName();
                double nameW = nf.stringWidth(name) + 18;
                double nx = x + (bw - nameW) / 2;
                a.setColor(UiTheme.toAwt(p.getColor()));
                a.fill(new Ellipse2D.Double(nx, y - 22, 12, 12));
                a.setColor(UiTheme.INK);
                a.drawString(name, (float) (nx + 18), (float) (y - 12));
                a.dispose();
            }
            g.dispose();
        }
    }

    /** כתר קטן בצבע הדגש עם מתאר שחור. */
    static void paintCrown(Graphics2D g, double x, double y, double w) {
        double h = w * 0.62;
        Path2D.Double crown = new Path2D.Double();
        crown.moveTo(x, y + h);
        crown.lineTo(x, y + h * 0.3);
        crown.lineTo(x + w * 0.27, y + h * 0.62);
        crown.lineTo(x + w * 0.5, y);
        crown.lineTo(x + w * 0.73, y + h * 0.62);
        crown.lineTo(x + w, y + h * 0.3);
        crown.lineTo(x + w, y + h);
        crown.closePath();
        g.setColor(UiTheme.ACCENT);
        g.fill(crown);
        g.setColor(UiTheme.INK);
        g.setStroke(new BasicStroke((float) Math.max(1.5, w * 0.06), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(crown);
    }

    // ------------------------------------------------------------------ צד הזברה

    /**
     * הצד של מסך הסיום: פסי זברה כמו במסך הפתיחה, ובמרכז עיגול השמש עם הכלי של המנצח בצבע שלו
     * וכתר. כשהמסך מופיע יורד קונפטי בצבעי השחקנים.
     */
    private static final class Hero extends JPanel {

        private static final long serialVersionUID = 1L;
        private static final int PIECES = 150;
        private static final long DURATION = 4200;

        private transient BufferedImage cache;
        private Color winnerColor = UiTheme.ACCENT;
        private final List<Color> palette = new ArrayList<Color>();
        private final double[][] confetti = new double[PIECES][7];
        private long confettiStart = -1;
        private transient Timer timer;

        Hero() {
            setOpaque(true);
            setBackground(UiTheme.PAPER);
            setPreferredSize(new Dimension(520, 600));
        }

        void setWinner(Player winner, List<Player> ranking) {
            winnerColor = winner == null ? UiTheme.PAPER : UiTheme.toAwt(winner.getColor());
            palette.clear();
            for (Player p : ranking) {
                palette.add(UiTheme.toAwt(p.getColor()));
            }
            palette.add(UiTheme.ACCENT);
            palette.add(winnerColor);
            palette.add(winnerColor);
            cache = null;
            repaint();
        }

        void celebrate() {
            Random r = new Random();
            for (double[] c : confetti) {
                c[0] = r.nextDouble();                 // x יחסי
                c[1] = -0.1 - r.nextDouble() * 0.9;    // y יחסי, מעל המסך
                c[2] = 0.18 + r.nextDouble() * 0.22;   // מהירות נפילה (גבהי מסך לשנייה)
                c[3] = r.nextDouble() * Math.PI * 2;   // זווית
                c[4] = (r.nextDouble() - 0.5) * 8;     // מהירות סיבוב
                c[5] = r.nextInt(Math.max(1, palette.size()));
                c[6] = r.nextDouble() * Math.PI * 2;   // שלב התנודה לצדדים
            }
            confettiStart = System.currentTimeMillis();
            if (timer != null) {
                timer.stop();
            }
            timer = new Timer(16, e -> {
                repaint();
                if (System.currentTimeMillis() - confettiStart > DURATION || !isShowing()) {
                    ((Timer) e.getSource()).stop();
                    confettiStart = -1;
                    repaint();
                }
            });
            timer.start();
        }

        @Override
        protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            int w = getWidth();
            int h = getHeight();
            if (w <= 0 || h <= 0) {
                return;
            }
            if (cache == null || cache.getWidth() != w || cache.getHeight() != h) {
                cache = render(w, h, winnerColor);
            }
            g0.drawImage(cache, 0, 0, null);
            if (confettiStart < 0 || palette.isEmpty()) {
                return;
            }
            Graphics2D g = (Graphics2D) g0.create();
            ZebraArt.smooth(g);
            double t = (System.currentTimeMillis() - confettiStart) / 1000.0;
            double fade = Math.max(0, Math.min(1, (DURATION / 1000.0 - t) / 0.8));
            g.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, (float) fade));
            for (double[] c : confetti) {
                double y = (c[1] + c[2] * t) * h;
                if (y < -20 || y > h + 20) {
                    continue;
                }
                double x = c[0] * w + Math.sin(c[6] + t * 3) * 18;
                Graphics2D p = (Graphics2D) g.create();
                p.translate(x, y);
                p.rotate(c[3] + c[4] * t);
                p.setColor(palette.get((int) c[5] % palette.size()));
                p.fillRect(-5, -3, 10, 6);
                p.dispose();
            }
            g.dispose();
        }

        private static BufferedImage render(int w, int h, Color winner) {
            BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = img.createGraphics();
            ZebraArt.smooth(g);
            g.setColor(UiTheme.PAPER);
            g.fillRect(0, 0, w, h);
            double spacing = Math.max(46, Math.min(w, h) / 9.0);
            ZebraArt.paintStripes(g, w, h, spacing, -32, 2027L, UiTheme.INK);

            double size = Math.min(w, h) * 0.46;
            double x = (w - size) / 2;
            double y = (h - size) / 2 + size * 0.06;
            double halo = size * 0.045;
            g.setColor(UiTheme.PAPER);
            g.fill(new Ellipse2D.Double(x - halo, y - halo, size + 2 * halo, size + 2 * halo));
            double ring = Math.max(1.5, size * 0.035);
            g.setColor(UiTheme.INK);
            g.fill(new Ellipse2D.Double(x, y, size, size));
            g.setColor(UiTheme.ACCENT);
            g.fill(new Ellipse2D.Double(x + ring, y + ring, size - 2 * ring, size - 2 * ring));
            // הכלי של המנצח בצבע שלו
            double ph = size * 0.58;
            double pw = 0.78 * ph;
            double px = x + (size - pw) / 2;
            double py = y + size * 0.22;
            java.awt.Shape pawn = ZebraArt.pawn(px, py, ph);
            g.setColor(winner);
            g.fill(pawn);
            g.setColor(UiTheme.INK);
            g.setStroke(new BasicStroke((float) Math.max(2, size * 0.022), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(pawn);
            // כתר מעל העיגול
            double cw = size * 0.34;
            double cx = x + (size - cw) / 2;
            double halo2 = cw * 0.12;
            Path2D.Double back = new Path2D.Double();
            back.append(new Ellipse2D.Double(cx - halo2, y - cw * 0.55 - halo2, cw + 2 * halo2, cw * 0.62 + 2 * halo2), false);
            g.setColor(UiTheme.PAPER);
            g.fill(back);
            paintCrown(g, cx, y - cw * 0.55, cw);
            g.dispose();
            return img;
        }
    }
}

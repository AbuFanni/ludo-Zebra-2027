package View;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import javax.swing.JComponent;
import javax.swing.Timer;

import Model.SurpriseEvent;

/**
 * הציורים של חלון ההפתעה בשפה העיצובית של Zebra: כותרת עם פסי זברה ו-{@code !} סגול, אייקון לכל
 * אירוע, ו"גלגל" שמראה את ההגרלה. האירוע עצמו מוגרל במודל; הגלגל רק מציג אותו בסוף.
 *
 * @author Nour Eldin Khatib (Zebra) – Iteration 2, US-8
 */
final class SurpriseArt {

    private SurpriseArt() {
    }

    /** כותרת החלון: פסי זברה, ובמרכז עיגול סגול עם {@code !} והילה לבנה. */
    static final class Header extends JComponent {

        private static final long serialVersionUID = 1L;
        private transient BufferedImage cache;

        Header() {
            setPreferredSize(new Dimension(520, 112));
        }

        @Override
        protected void paintComponent(Graphics g) {
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
            ZebraArt.paintStripes(g, w, h, 34, -28, 44L, UiTheme.INK);
            double size = h * 0.74;
            double x = (w - size) / 2;
            double y = (h - size) / 2;
            double halo = size * 0.1;
            g.setColor(UiTheme.PAPER);
            g.fill(new Ellipse2D.Double(x - halo, y - halo, size + 2 * halo, size + 2 * halo));
            g.setColor(UiTheme.INK);
            g.fill(new Ellipse2D.Double(x, y, size, size));
            double ring = size * 0.05;
            g.setColor(UiTheme.SURPRISE);
            g.fill(new Ellipse2D.Double(x + ring, y + ring, size - 2 * ring, size - 2 * ring));
            g.setColor(Color.WHITE);
            g.setFont(new Font(UiTheme.TITLE.getFamily(), Font.BOLD, (int) (size * 0.62)));
            FontMetrics fm = g.getFontMetrics();
            String s = "!";
            g.drawString(s, (float) (x + (size - fm.stringWidth(s)) / 2),
                    (float) (y + (size + fm.getAscent() - fm.getDescent()) / 2));
            g.dispose();
            return img;
        }
    }

    /**
     * הגלגל: מסגרת סגולה עם אייקון ושם האירוע. {@link #spin(SurpriseEvent, Runnable)} מחליף שמות מהר
     * ומאט, עד שהוא נעצר על האירוע שהוגרל, ואז "קופץ" קצת.
     */
    static final class Reel extends JComponent {

        private static final long serialVersionUID = 1L;
        private static final int STEPS = 16;

        private SurpriseEvent shown = SurpriseEvent.values()[0];
        private boolean settled;
        private double pop;
        private transient Timer timer;

        Reel() {
            setPreferredSize(new Dimension(440, 84));
            setMinimumSize(new Dimension(300, 84));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 84));
        }

        /** מסובב את הגלגל ועוצר על {@code result}; {@code onStop} נקרא כשהוא נעצר. */
        void spin(SurpriseEvent result, Runnable onStop) {
            SurpriseEvent[] all = SurpriseEvent.values();
            int start = (result.ordinal() + 1) % all.length;
            final int[] step = {0};
            timer = new Timer(45, null);
            timer.addActionListener(e -> {
                step[0]++;
                if (step[0] >= STEPS) {
                    shown = result;
                    timer.stop();
                    settle(onStop);
                } else {
                    shown = all[(start + step[0]) % all.length];
                    // מאט בהדרגה, כמו גלגל שנעצר
                    timer.setDelay(45 + (int) (step[0] * step[0] * 0.9));
                }
                repaint();
            });
            timer.start();
        }

        /** מציג את האירוע מיד, בלי סיבוב. */
        void show(SurpriseEvent event) {
            shown = event;
            settled = true;
            repaint();
        }

        private void settle(Runnable onStop) {
            settled = true;
            final long start = System.currentTimeMillis();
            Timer t = new Timer(16, null);
            t.addActionListener(e -> {
                double p = Math.min(1, (System.currentTimeMillis() - start) / 280.0);
                pop = Math.sin(p * Math.PI) * 0.06;
                repaint();
                if (p >= 1) {
                    t.stop();
                    pop = 0;
                    if (onStop != null) {
                        onStop.run();
                    }
                }
            });
            t.start();
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            ZebraArt.smooth(g);
            int w = getWidth();
            int h = getHeight();
            double scale = 1 + pop;
            g.translate(w / 2.0, h / 2.0);
            g.scale(scale, scale);
            g.translate(-w / 2.0, -h / 2.0);
            double pad = 6;
            RoundRectangle2D box = new RoundRectangle2D.Double(pad, pad, w - 2 * pad, h - 2 * pad, 18, 18);
            g.setColor(settled ? UiTheme.mix(UiTheme.SURPRISE, Color.WHITE, 0.12) : UiTheme.PAPER);
            g.fill(box);
            g.setStroke(new BasicStroke(settled ? 3f : 2f));
            g.setColor(settled ? UiTheme.SURPRISE : UiTheme.LINE);
            g.draw(box);
            // אייקון בעיגול סגול בצד ימין (תחילת השורה בעברית)
            double d = h - 2 * pad - 16;
            double cx = w - pad - 12 - d;
            double cy = pad + 8;
            g.setColor(UiTheme.SURPRISE);
            g.fill(new Ellipse2D.Double(cx, cy, d, d));
            paintIcon(g, shown, cx, cy, d, Color.WHITE);
            g.setColor(UiTheme.INK);
            g.setFont(UiTheme.HEADING.deriveFont(24f));
            FontMetrics fm = g.getFontMetrics();
            String name = shown.getHebrewName();
            float tx = (float) (cx - 18 - fm.stringWidth(name));
            g.drawString(name, tx, (float) (h / 2.0 + (fm.getAscent() - fm.getDescent()) / 2.0));
            if (settled) {
                String pct = shown.getPercent() + "%";
                g.setFont(UiTheme.SMALL);
                g.setColor(UiTheme.MUTED);
                g.drawString(pct, (float) (pad + 16), (float) (h / 2.0 + 5));
            }
            g.dispose();
        }
    }

    /** מצייר אייקון פשוט לכל אירוע, בתוך ריבוע (x, y, size). */
    static void paintIcon(Graphics2D g, SurpriseEvent e, double x, double y, double size, Color c) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.translate(x, y);
        g2.scale(size / 100.0, size / 100.0);
        g2.setColor(c);
        g2.setStroke(new BasicStroke(7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        switch (e) {
            case JUMP_TO_FINISH: {
                // בית עם חץ למעלה
                Path2D.Double house = new Path2D.Double();
                house.moveTo(24, 50);
                house.lineTo(50, 28);
                house.lineTo(76, 50);
                house.moveTo(32, 46);
                house.lineTo(32, 76);
                house.lineTo(68, 76);
                house.lineTo(68, 46);
                g2.draw(house);
                Path2D.Double arrow = new Path2D.Double();
                arrow.moveTo(50, 70);
                arrow.lineTo(50, 46);
                arrow.moveTo(41, 55);
                arrow.lineTo(50, 46);
                arrow.lineTo(59, 55);
                g2.draw(arrow);
                break;
            }
            case MOVE_OPPONENT: {
                // כלי וחץ אחורה
                g2.fill(ZebraArt.pawn(48, 22, 52));
                Path2D.Double arrow = new Path2D.Double();
                arrow.moveTo(40, 50);
                arrow.lineTo(16, 50);
                arrow.moveTo(26, 40);
                arrow.lineTo(16, 50);
                arrow.lineTo(26, 60);
                g2.draw(arrow);
                break;
            }
            case BACK_TO_BASE: {
                // טירה
                Path2D.Double castle = new Path2D.Double();
                castle.moveTo(26, 78);
                castle.lineTo(26, 32);
                castle.lineTo(36, 32);
                castle.lineTo(36, 42);
                castle.lineTo(45, 42);
                castle.lineTo(45, 32);
                castle.lineTo(55, 32);
                castle.lineTo(55, 42);
                castle.lineTo(64, 42);
                castle.lineTo(64, 32);
                castle.lineTo(74, 32);
                castle.lineTo(74, 78);
                castle.closePath();
                g2.draw(castle);
                g2.draw(new Arc2D.Double(42, 58, 16, 24, 0, 180, Arc2D.OPEN));
                break;
            }
            case BACK_THREE: {
                // חץ מעוגל אחורה ו-3
                g2.draw(new Arc2D.Double(26, 26, 48, 48, 200, -250, Arc2D.OPEN));
                Path2D.Double head = new Path2D.Double();
                head.moveTo(18, 46);
                head.lineTo(28, 58);
                head.lineTo(38, 46);
                g2.draw(head);
                g2.setFont(new Font(UiTheme.TITLE.getFamily(), Font.BOLD, 30));
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString("3", 50 - fm.stringWidth("3") / 2f, 61);
                break;
            }
            case LOSE_NEXT_TURN: {
                // שעון חול
                Path2D.Double glass = new Path2D.Double();
                glass.moveTo(30, 24);
                glass.lineTo(70, 24);
                glass.lineTo(52, 50);
                glass.lineTo(70, 76);
                glass.lineTo(30, 76);
                glass.lineTo(48, 50);
                glass.closePath();
                g2.draw(glass);
                g2.fill(new Rectangle2D.Double(38, 64, 24, 8));
                break;
            }
            case ULTRA_BOOST:
            default: {
                // ברק
                Path2D.Double bolt = new Path2D.Double();
                bolt.moveTo(56, 16);
                bolt.lineTo(28, 56);
                bolt.lineTo(48, 56);
                bolt.lineTo(42, 86);
                bolt.lineTo(72, 42);
                bolt.lineTo(52, 42);
                bolt.closePath();
                g2.fill(bolt);
                break;
            }
        }
        g2.dispose();
    }
}

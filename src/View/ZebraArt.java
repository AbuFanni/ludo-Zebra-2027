package View;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * הגרפיקה של הזהות של קבוצת Zebra: פסי זברה אורגניים והלוגו (כלי לודו מפוספס על רקע שמש).
 * <p>
 * הכל מצויר בקוד (Java2D), בלי תמונות חיצוניות, כך שהגרפיקה חדה בכל גודל מסך ולא תלויה בקבצים.
 * הפסים נוצרים מ-seed קבוע, ולכן הם נראים אותו דבר בכל הרצה.
 *
 * @author Nour Eldin Khatib (Zebra) – Iteration 1, SDP table 2
 */
public final class ZebraArt {

    private ZebraArt() {
    }

    /** מפעיל החלקת קצוות לציור. */
    public static void smooth(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    }

    /**
     * מצייר פסי זברה אורגניים על כל השטח (0,0)-(w,h).
     *
     * @param spacing המרחק בין פס לפס בפיקסלים
     * @param angle   זווית הפסים במעלות
     * @param seed    קובע את צורת הפסים; אותו seed נותן אותם פסים
     */
    public static void paintStripes(Graphics2D g, double w, double h, double spacing, double angle, long seed,
                                    Color ink) {
        AffineTransform old = g.getTransform();
        g.translate(w / 2.0, h / 2.0);
        g.rotate(Math.toRadians(angle));
        double half = Math.hypot(w, h) / 2.0 + spacing;
        g.setColor(ink);
        for (Shape s : stripes(half, spacing, seed)) {
            g.fill(s);
        }
        g.setTransform(old);
    }

    /** הפסים במערכת צירים שמרכזה (0,0), בריבוע שחצי צלעו half. */
    private static List<Shape> stripes(double half, double spacing, long seed) {
        Random rnd = new Random(seed);
        List<Shape> out = new ArrayList<Shape>();
        double step = Math.max(3, spacing / 6.0);
        for (double y = -half; y < half; y += spacing * (0.85 + 0.3 * rnd.nextDouble())) {
            double thick = spacing * (0.46 + 0.16 * rnd.nextDouble());
            double a1 = spacing * (0.22 + 0.3 * rnd.nextDouble());
            double f1 = 2 * Math.PI / (half * (0.9 + 1.1 * rnd.nextDouble()));
            double p1 = rnd.nextDouble() * 2 * Math.PI;
            double a2 = spacing * (0.05 + 0.1 * rnd.nextDouble());
            double f2 = f1 * (2.5 + 2 * rnd.nextDouble());
            double p2 = rnd.nextDouble() * 2 * Math.PI;
            double f3 = f1 * (1.5 + rnd.nextDouble());
            double p3 = rnd.nextDouble() * 2 * Math.PI;
            // חלק מהפסים מתחילים או נגמרים באמצע ומתחדדים, כמו בפרווה של זברה אמיתית
            double xs = rnd.nextDouble() < 0.3 ? -half * rnd.nextDouble() * 0.8 : -half;
            double xe = rnd.nextDouble() < 0.3 ? half * rnd.nextDouble() * 0.8 : half;
            if (xe - xs < spacing * 3) {
                xs = -half;
                xe = half;
            }
            double taper = spacing * 2.5;

            List<double[]> top = new ArrayList<double[]>();
            List<double[]> bottom = new ArrayList<double[]>();
            for (double x = xs; x <= xe + step; x += step) {
                double cx = Math.min(x, xe);
                double c = y + a1 * Math.sin(f1 * cx + p1) + a2 * Math.sin(f2 * cx + p2);
                double t = thick / 2 * (0.75 + 0.25 * Math.sin(f3 * cx + p3));
                double edge = 1;
                if (xs > -half) {
                    edge = Math.min(edge, (cx - xs) / taper);
                }
                if (xe < half) {
                    edge = Math.min(edge, (xe - cx) / taper);
                }
                edge = Math.max(0, Math.min(1, edge));
                t *= edge * edge * (3 - 2 * edge);
                top.add(new double[]{cx, c - t});
                bottom.add(new double[]{cx, c + t});
            }
            Path2D.Double p = new Path2D.Double();
            p.moveTo(top.get(0)[0], top.get(0)[1]);
            for (int i = 1; i < top.size(); i++) {
                p.lineTo(top.get(i)[0], top.get(i)[1]);
            }
            for (int i = bottom.size() - 1; i >= 0; i--) {
                p.lineTo(bottom.get(i)[0], bottom.get(i)[1]);
            }
            p.closePath();
            out.add(p);
        }
        return out;
    }

    /**
     * צורת כלי לודו בגובה h, כשהפינה השמאלית העליונה של המסגרת שלו ב-(x,y). הרוחב הוא 0.78h.
     */
    public static Shape pawn(double x, double y, double h) {
        double w = 0.78 * h;
        double cx = x + w / 2;
        Area a = new Area(new Ellipse2D.Double(cx - 0.19 * h, y, 0.38 * h, 0.38 * h));
        a.add(new Area(new RoundRectangle2D.Double(cx - 0.23 * h, y + 0.39 * h, 0.46 * h, 0.07 * h,
                0.07 * h, 0.07 * h)));
        Path2D.Double body = new Path2D.Double();
        body.moveTo(cx - 0.11 * h, y + 0.44 * h);
        body.curveTo(cx - 0.12 * h, y + 0.62 * h, cx - 0.2 * h, y + 0.76 * h, cx - 0.31 * h, y + 0.86 * h);
        body.lineTo(cx + 0.31 * h, y + 0.86 * h);
        body.curveTo(cx + 0.2 * h, y + 0.76 * h, cx + 0.12 * h, y + 0.62 * h, cx + 0.11 * h, y + 0.44 * h);
        body.closePath();
        a.add(new Area(body));
        a.add(new Area(new RoundRectangle2D.Double(x, y + 0.84 * h, w, 0.16 * h, 0.12 * h, 0.12 * h)));
        return a;
    }

    /**
     * הלוגו: עיגול שמש בצבע הדגש, ועליו כלי לודו לבן עם פסי זברה ומתאר שחור.
     *
     * @param size קוטר העיגול
     */
    public static void paintLogo(Graphics2D g, double x, double y, double size) {
        smooth(g);
        double ring = Math.max(1.5, size * 0.035);
        g.setColor(UiTheme.INK);
        g.fill(new Ellipse2D.Double(x, y, size, size));
        g.setColor(UiTheme.ACCENT);
        g.fill(new Ellipse2D.Double(x + ring, y + ring, size - 2 * ring, size - 2 * ring));

        double ph = size * 0.66;
        double px = x + (size - 0.78 * ph) / 2;
        double py = y + size * 0.15;
        Shape pawn = pawn(px, py, ph);
        g.setColor(UiTheme.PAPER);
        g.fill(pawn);
        Shape oldClip = g.getClip();
        g.clip(pawn);
        Graphics2D sg = (Graphics2D) g.create();
        sg.translate(px, py);
        paintStripes(sg, 0.78 * ph, ph, ph / 8.5, -24, 11L, UiTheme.INK);
        sg.dispose();
        g.setClip(oldClip);
        g.setColor(UiTheme.INK);
        g.setStroke(new BasicStroke((float) Math.max(1.2, size * 0.022), BasicStroke.CAP_ROUND,
                BasicStroke.JOIN_ROUND));
        g.draw(pawn);
    }

    /** @return הלוגו כתמונה בגודל size על size, לאייקון החלון */
    public static BufferedImage logoImage(int size) {
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        paintLogo(g, 0, 0, size);
        g.dispose();
        return img;
    }

    /** @return הלוגו בכמה גדלים, כדי שמערכת ההפעלה תבחר את המתאים לשורת המשימות */
    public static List<Image> windowIcons() {
        List<Image> icons = new ArrayList<Image>();
        for (int s : new int[]{16, 24, 32, 48, 64, 128, 256}) {
            icons.add(logoImage(s));
        }
        return icons;
    }
}

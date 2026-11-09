package View;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.JPanel;

import Model.Board;
import Model.Game;
import Model.GamePhase;
import Model.Move;
import Model.Pawn;
import Model.Player;
import Model.PlayerColor;
import Model.Position;
import Model.QuestionSquare;
import Model.Square;
import Model.SquareKind;

/**
 * מצייר את הלוח של הרמה, את הטירות ואת הכלים (FR-12), ומסמן אילו כלים יכולים לזוז (FR-23).
 * <p>
 * "המדריך" של העכבר לקוח מ-{@code Gameplay.MouseOverAndClickListener} בפרויקט Ludo-Griniaris-Game
 * (MIT): כשהעכבר עומד על כלי שיכול לזוז, משבצת היעד מקבלת מסגרת שחורה עבה. במקור כל תא היה
 * JButton שהחזיק את הכלים; כאן הלוח מצויר בפאנל אחד מתוך המודל, והלחיצה רק מדווחת ל-Controller.
 */
public class BoardPanel extends JPanel {

    /** מקבל את הלחיצות על כלים. מיושם ב-Controller. */
    public interface PawnClickListener {
        void pawnClicked(Pawn pawn);
    }

    private Game game;
    private PawnClickListener listener;
    private Pawn hovered;
    private final Map<Pawn, Rectangle> pawnBounds = new LinkedHashMap<Pawn, Rectangle>();
    private int cell;
    private int originX;
    private int originY;

    public BoardPanel() {
        setBackground(UiTheme.BACKGROUND);
        setPreferredSize(new Dimension(640, 640));
        MouseAdapter mouse = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                Pawn p = pawnAt(e.getPoint(), true);
                if (p != hovered) {
                    hovered = p;
                    setCursor(Cursor.getPredefinedCursor(p != null ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
                    repaint();
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (hovered != null) {
                    hovered = null;
                    repaint();
                }
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                Pawn p = pawnAt(e.getPoint(), false);
                if (p != null && listener != null) {
                    listener.pawnClicked(p);
                }
            }
        };
        addMouseListener(mouse);
        addMouseMotionListener(mouse);
    }

    /** מחבר את הלוח למשחק שמוצג. */
    public void setGame(Game game) {
        this.game = game;
        this.hovered = null;
        repaint();
    }

    public void setPawnClickListener(PawnClickListener listener) {
        this.listener = listener;
    }

    /** @return הכלי של השחקן הנוכחי בנקודה, או null */
    private Pawn pawnAt(Point point, boolean movableOnly) {
        if (game == null || game.getPhase() != GamePhase.MOVE) {
            return null;
        }
        Player current = game.getCurrentPlayer();
        Pawn found = null;
        for (Map.Entry<Pawn, Rectangle> e : pawnBounds.entrySet()) {
            Pawn p = e.getKey();
            if (p.getOwner() == current && e.getValue().contains(point)) {
                if (!movableOnly || game.legalMoveFor(p) != null) {
                    found = p; // הכלי העליון בציור
                }
            }
        }
        return found;
    }

    // ------------------------------------------------------------------ ציור

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        if (game == null) {
            return;
        }
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        Board board = game.getBoard();
        int n = board.getSize();
        cell = Math.max(8, Math.min(getWidth(), getHeight()) / n);
        originX = (getWidth() - cell * n) / 2;
        originY = (getHeight() - cell * n) / 2;

        g.setColor(UiTheme.BOARD_BG);
        g.fillRect(originX, originY, cell * n, cell * n);

        for (PlayerColor c : PlayerColor.values()) {
            paintBase(g, board, c);
        }
        paintCenter(g, board);
        for (Square s : board.getSquares()) {
            if (s.getKind() != SquareKind.FINISH) {
                paintSquare(g, board, s);
            }
        }
        g.setColor(UiTheme.LINE);
        g.setStroke(new BasicStroke(2f));
        g.drawRect(originX, originY, cell * n, cell * n);

        paintHoverTarget(g, board);
        paintPawns(g, board);
        g.dispose();
    }

    private Rectangle cellRect(Position p) {
        return new Rectangle(originX + p.getCol() * cell, originY + p.getRow() * cell, cell, cell);
    }

    private void paintBase(Graphics2D g, Board board, PlayerColor color) {
        Position o = board.getBaseOrigin(color);
        int size = Board.BASE_SIZE * cell;
        int x = originX + o.getCol() * cell;
        int y = originY + o.getRow() * cell;
        g.setColor(UiTheme.toAwt(color));
        g.fillRect(x, y, size, size);
        int inset = (int) (cell * 0.8);
        g.setColor(Color.WHITE);
        g.fillRoundRect(x + inset, y + inset, size - 2 * inset, size - 2 * inset, cell / 2, cell / 2);
        g.setColor(UiTheme.LINE);
        g.setStroke(new BasicStroke(1.5f));
        g.drawRect(x, y, size, size);
        for (int i = 0; i < 4; i++) {
            Point c = baseSlotCenter(board, color, i);
            int r = (int) (cell * 0.62);
            g.setColor(UiTheme.light(color));
            g.fill(new Ellipse2D.Double(c.x - r, c.y - r, 2 * r, 2 * r));
            g.setColor(UiTheme.toAwt(color).darker());
            g.draw(new Ellipse2D.Double(c.x - r, c.y - r, 2 * r, 2 * r));
        }
    }

    private Point baseSlotCenter(Board board, PlayerColor color, int slot) {
        Position o = board.getBaseOrigin(color);
        int dr = (slot / 2) * 2 + 2;
        int dc = (slot % 2) * 2 + 2;
        return new Point(originX + (o.getCol() + dc) * cell, originY + (o.getRow() + dr) * cell);
    }

    private void paintCenter(Graphics2D g, Board board) {
        int o = board.getDifficulty().getOuterRings();
        int x = originX + (o + 6) * cell;
        int y = originY + (o + 6) * cell;
        int s = 3 * cell;
        int cx = x + s / 2;
        int cy = y + s / 2;
        fillTriangle(g, UiTheme.toAwt(PlayerColor.GREEN), x, y, x, y + s, cx, cy);
        fillTriangle(g, UiTheme.toAwt(PlayerColor.YELLOW), x, y, x + s, y, cx, cy);
        fillTriangle(g, UiTheme.toAwt(PlayerColor.BLUE), x + s, y, x + s, y + s, cx, cy);
        fillTriangle(g, UiTheme.toAwt(PlayerColor.RED), x, y + s, x + s, y + s, cx, cy);
        g.setColor(UiTheme.LINE);
        g.setStroke(new BasicStroke(1.2f));
        g.drawRect(x, y, s, s);
    }

    private static void fillTriangle(Graphics2D g, Color color, int x1, int y1, int x2, int y2, int x3, int y3) {
        Polygon p = new Polygon(new int[]{x1, x2, x3}, new int[]{y1, y2, y3}, 3);
        g.setColor(color);
        g.fillPolygon(p);
        g.setColor(UiTheme.LINE);
        g.drawPolygon(p);
    }

    private void paintSquare(Graphics2D g, Board board, Square s) {
        Rectangle r = cellRect(s.getPosition());
        Color fill = Color.WHITE;
        switch (s.getKind()) {
            case START:
            case HOME_PATH:
                fill = UiTheme.toAwt(s.getOwner());
                break;
            case QUESTION:
                fill = UiTheme.question(((QuestionSquare) s).getLevel());
                break;
            case SURPRISE:
                fill = new Color(0xE9DCF3);
                break;
            default:
                break;
        }
        g.setColor(fill);
        g.fillRect(r.x, r.y, r.width, r.height);
        g.setColor(UiTheme.LINE);
        g.setStroke(new BasicStroke(1f));
        g.drawRect(r.x, r.y, r.width, r.height);

        switch (s.getKind()) {
            case START:
                drawCenteredText(g, "#", r, Color.WHITE, Font.BOLD, 0.62);
                break;
            case SAFE:
                drawStar(g, r);
                break;
            case ARROW:
                drawArrow(g, board, s, r);
                break;
            case QUESTION:
                drawCenteredText(g, "?", r, UiTheme.mix(UiTheme.question(((QuestionSquare) s).getLevel()),
                        Color.BLACK, 0.35), Font.BOLD, 0.62);
                break;
            case SURPRISE:
                drawCenteredText(g, "!", r, UiTheme.SURPRISE, Font.BOLD, 0.62);
                break;
            default:
                break;
        }
    }

    private void drawStar(Graphics2D g, Rectangle r) {
        Image star = Images.star();
        int pad = r.width / 7;
        if (star != null) {
            g.drawImage(star, r.x + pad, r.y + pad, r.width - 2 * pad, r.height - 2 * pad, null);
            return;
        }
        drawCenteredText(g, "★", r, UiTheme.MUTED, Font.PLAIN, 0.7);
    }

    private void drawArrow(Graphics2D g, Board board, Square s, Rectangle r) {
        List<Square> path = board.getPath(s.getOwner());
        int i = path.indexOf(s);
        Position a = s.getPosition();
        Position b = path.get(Math.min(i + 1, path.size() - 1)).getPosition();
        int dx = Integer.signum(b.getCol() - a.getCol());
        int dy = Integer.signum(b.getRow() - a.getRow());
        double cx = r.getCenterX();
        double cy = r.getCenterY();
        double len = r.width * 0.34;
        double w = r.width * 0.22;
        // חץ בכיוון (dx,dy)
        double tipX = cx + dx * len;
        double tipY = cy + dy * len;
        double backX = cx - dx * len;
        double backY = cy - dy * len;
        double px = -dy;
        double py = dx;
        double headBaseX = cx + dx * (len * 0.1);
        double headBaseY = cy + dy * (len * 0.1);
        g.setColor(UiTheme.toAwt(s.getOwner()));
        g.setStroke(new BasicStroke(Math.max(2f, r.width * 0.1f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawLine((int) backX, (int) backY, (int) headBaseX, (int) headBaseY);
        Polygon head = new Polygon();
        head.addPoint((int) tipX, (int) tipY);
        head.addPoint((int) (headBaseX + px * w), (int) (headBaseY + py * w));
        head.addPoint((int) (headBaseX - px * w), (int) (headBaseY - py * w));
        g.fillPolygon(head);
    }

    private static void drawCenteredText(Graphics2D g, String text, Rectangle r, Color color, int style, double scale) {
        Font f = UiTheme.BODY.deriveFont(style, (float) (r.height * scale));
        g.setFont(f);
        FontMetrics fm = g.getFontMetrics();
        int x = r.x + (r.width - fm.stringWidth(text)) / 2;
        int y = r.y + (r.height - fm.getHeight()) / 2 + fm.getAscent();
        g.setColor(color);
        g.drawString(text, x, y);
    }

    /** מסגרת שחורה עבה על משבצת היעד של הכלי שהעכבר עליו. */
    private void paintHoverTarget(Graphics2D g, Board board) {
        if (hovered == null || game.getPhase() != GamePhase.MOVE) {
            return;
        }
        Move m = game.legalMoveFor(hovered);
        if (m == null) {
            return;
        }
        Square target = board.getPath(hovered.getColor()).get(m.getTo());
        Rectangle r = targetRect(board, target);
        g.setColor(Color.BLACK);
        g.setStroke(new BasicStroke(Math.max(3f, cell * 0.14f)));
        g.drawRect(r.x + 1, r.y + 1, r.width - 2, r.height - 2);
    }

    private Rectangle targetRect(Board board, Square target) {
        return cellRect(target.getPosition());
    }

    private void paintPawns(Graphics2D g, Board board) {
        pawnBounds.clear();
        Set<Pawn> movable = new HashSet<Pawn>();
        if (game.getPhase() == GamePhase.MOVE) {
            for (Pawn p : game.getCurrentPlayer().getPawns()) {
                if (game.legalMoveFor(p) != null) {
                    movable.add(p);
                }
            }
        }

        Map<Square, List<Pawn>> bySquare = new LinkedHashMap<Square, List<Pawn>>();
        for (Player player : game.getPlayers()) {
            for (Pawn pawn : player.getPawns()) {
                if (pawn.isInBase()) {
                    Point c = baseSlotCenter(board, pawn.getColor(), pawn.getNumber());
                    int size = (int) (cell * 1.05);
                    drawPawn(g, pawn, new Rectangle(c.x - size / 2, c.y - size / 2, size, size), movable.contains(pawn));
                } else {
                    Square s = game.squareOf(pawn);
                    List<Pawn> list = bySquare.get(s);
                    if (list == null) {
                        list = new ArrayList<Pawn>();
                        bySquare.put(s, list);
                    }
                    list.add(pawn);
                }
            }
        }
        for (Map.Entry<Square, List<Pawn>> e : bySquare.entrySet()) {
            Rectangle r = cellRect(e.getKey().getPosition());
            List<Pawn> list = e.getValue();
            int k = list.size();
            if (k == 1) {
                int size = (int) (cell * 0.92);
                drawPawn(g, list.get(0), centered(r, size, 0, 0), movable.contains(list.get(0)));
            } else {
                int size = (int) (cell * 0.62);
                int off = (int) (cell * 0.2);
                int[][] offsets = {{-off, -off}, {off, off}, {off, -off}, {-off, off}};
                for (int i = 0; i < k; i++) {
                    int[] o = offsets[i % 4];
                    drawPawn(g, list.get(i), centered(r, size, o[0], o[1]), movable.contains(list.get(i)));
                }
                if (isBlock(list)) {
                    g.setColor(new Color(0, 0, 0, 160));
                    g.setStroke(new BasicStroke(Math.max(2f, cell * 0.08f)));
                    g.drawRect(r.x + 2, r.y + 2, r.width - 4, r.height - 4);
                }
            }
        }
    }

    /** Double Block: לפחות שני כלים של אותו שחקן במשבצת משותפת. */
    private static boolean isBlock(List<Pawn> pawns) {
        Set<Player> seen = new HashSet<Player>();
        for (Pawn p : pawns) {
            if (!seen.add(p.getOwner())) {
                return !p.getOwner().isDone();
            }
        }
        return false;
    }

    private static Rectangle centered(Rectangle r, int size, int dx, int dy) {
        return new Rectangle((int) r.getCenterX() - size / 2 + dx, (int) r.getCenterY() - size / 2 + dy, size, size);
    }

    private void drawPawn(Graphics2D g, Pawn pawn, Rectangle r, boolean movable) {
        if (movable) {
            int pad = Math.max(2, r.width / 8);
            g.setColor(new Color(255, 214, 0, 200));
            g.fill(new Ellipse2D.Double(r.x - pad, r.y - pad, r.width + 2 * pad, r.height + 2 * pad));
            g.setColor(Color.BLACK);
            g.setStroke(new BasicStroke(pawn == hovered ? 3f : 1.5f));
            g.draw(new Ellipse2D.Double(r.x - pad, r.y - pad, r.width + 2 * pad, r.height + 2 * pad));
        }
        paintPawnShape(g, UiTheme.toAwt(pawn.getColor()), r);
        pawnBounds.put(pawn, r);
    }

    /** כלי וקטורי: בסיס, גוף וראש בצבע השחקן, כך שהוא חד בכל גודל לוח. */
    static void paintPawnShape(Graphics2D g, Color color, Rectangle r) {
        double w = r.width;
        double h = r.height;
        double x = r.x;
        double y = r.y;
        Color dark = color.darker().darker();
        g.setColor(new Color(0, 0, 0, 60));
        g.fill(new Ellipse2D.Double(x + w * 0.16, y + h * 0.80, w * 0.68, h * 0.16));
        java.awt.geom.Path2D body = new java.awt.geom.Path2D.Double();
        body.moveTo(x + w * 0.22, y + h * 0.88);
        body.curveTo(x + w * 0.24, y + h * 0.62, x + w * 0.36, y + h * 0.50, x + w * 0.42, y + h * 0.44);
        body.lineTo(x + w * 0.58, y + h * 0.44);
        body.curveTo(x + w * 0.64, y + h * 0.50, x + w * 0.76, y + h * 0.62, x + w * 0.78, y + h * 0.88);
        body.closePath();
        g.setColor(color);
        g.fill(body);
        Ellipse2D head = new Ellipse2D.Double(x + w * 0.30, y + h * 0.10, w * 0.40, h * 0.40);
        g.fill(head);
        g.setColor(dark);
        g.setStroke(new BasicStroke((float) Math.max(1.2, w * 0.05)));
        g.draw(body);
        g.draw(head);
        g.setColor(new Color(255, 255, 255, 150));
        g.fill(new Ellipse2D.Double(x + w * 0.38, y + h * 0.17, w * 0.12, h * 0.12));
    }
}

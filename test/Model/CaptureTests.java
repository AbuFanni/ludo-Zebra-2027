package Model;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * בדיקות לאכילה ולמשבצות מוגנות (FR-29, FR-30, FR-31) — מטריצת הבדיקות של משימה 5:
 * אכילה רגילה, משבצת מוגנת, משבצת התחלה, חץ בצבע אחר, Double Block של יריב, קדימה ואחורה.
 * <p>
 * כל בדיקה מסומנת T5.xx כמו ברשומת הבדיקות של משימה 5 בדוח האיטרציה.
 * רצות יחד עם שאר בדיקות המודל ({@link ModelTests}), דרך build.sh / build.bat.
 *
 * @author Nimer Ziad (Zebra) – Iteration 1, SDP table 2
 */
final class CaptureTests {

    private CaptureTests() {
    }

    static void runAll() {
        ModelTests.run("T5.01 landing on a single opponent on a regular square captures it", CaptureTests::normalCapture);
        ModelTests.run("T5.02 no capture on a star; both pieces share it", CaptureTests::star);
        ModelTests.run("T5.03 no capture on another player's start square (#)", CaptureTests::otherStart);
        ModelTests.run("T5.04 no capture on a surprise square (medium board)", CaptureTests::surprise);
        ModelTests.run("T5.05 home lane and finish are protected and private", CaptureTests::homeLane);
        ModelTests.run("T5.06 capture on an arrow square of another color", CaptureTests::otherArrow);
        ModelTests.run("T5.07 capture on a question square (medium board)", CaptureTests::question);
        ModelTests.run("T5.08 opponent Double Block: cannot land on it, never captured", CaptureTests::opponentBlock);
        ModelTests.run("T5.09 capture also when stopped before a Double Block", CaptureTests::partialMoveCaptures);
        ModelTests.run("T5.10 capturing does not give an extra turn", CaptureTests::noExtraTurn);
        ModelTests.run("T5.11 capture with a 6: extra roll only because of the 6", CaptureTests::captureWithSix);
        ModelTests.run("T5.12 backward move: captureAt captures a single opponent", CaptureTests::backward);
        ModelTests.run("T5.13 backward move onto a protected square: no capture", CaptureTests::backwardProtected);
        ModelTests.run("T5.14 landing on own piece: no capture", CaptureTests::ownPiece);
        ModelTests.run("T5.15 4 players: only the piece on the target square is captured", CaptureTests::fourPlayers);
    }

    // ------------------------------------------------------------- עזר

    private static final class Dice implements Die {
        private final int[] values;
        private int i;

        Dice(int... values) {
            this.values = values;
        }

        @Override
        public int roll() {
            return values[i++ % values.length];
        }
    }

    private static Game game(Difficulty d, long seed, int[] rolls, PlayerColor... colors) {
        Board board = BoardBuilder.build(d, new Random(seed));
        List<Player> players = new ArrayList<Player>();
        for (int i = 0; i < colors.length; i++) {
            players.add(new Player("P" + (i + 1), colors[i], i));
        }
        return new Game(board, players, new Dice(rolls), () -> 0L);
    }

    private static Game easy(int... rolls) {
        return game(Difficulty.EASY, 7, rolls, PlayerColor.GREEN, PlayerColor.BLUE);
    }

    private static Player player(Game g, int i) {
        return g.getPlayers().get(i);
    }

    private static Pawn pawn(Game g, int player, int number) {
        return player(g, player).getPawns().get(number);
    }

    private static List<Square> path(Game g, PlayerColor c) {
        return g.getBoard().getPath(c);
    }

    /** מציב כלי של שחקן על משבצת (בכל צבע המשבצת במקום אחר במסלול). */
    private static void put(Game g, Pawn p, Square sq) {
        int idx = path(g, p.getColor()).indexOf(sq);
        if (idx < 0) {
            throw new AssertionError(p.getColor() + " path does not pass " + sq.getPosition());
        }
        p.setPathIndex(idx);
    }

    /** האינדקס הראשון במסלול של הירוק, מ-from והלאה, שהמשבצת בו מהסוג המבוקש. */
    private static int greenIndexOf(Game g, SquareKind kind, int from) {
        List<Square> p = path(g, PlayerColor.GREEN);
        for (int i = from; i < p.size(); i++) {
            if (p.get(i).getKind() == kind) {
                return i;
            }
        }
        throw new AssertionError("no " + kind + " on green path after " + from);
    }

    /** ירוק עומד roll צעדים לפני המשבצת, מטיל roll ונוחת עליה. */
    private static MoveResult greenLandsOn(Game g, Pawn green, Square target, int roll) {
        int t = path(g, PlayerColor.GREEN).indexOf(target);
        green.setPathIndex(t - roll);
        g.roll();
        Move m = g.legalMoveFor(green);
        ModelTests.check(m != null, "green can move to " + target.getPosition());
        ModelTests.checkEq(t, m.getTo(), "green lands on the target");
        return g.move(m);
    }

    /** לוח בינוני שבו יש משבצת מהסוג המבוקש על המסלול של הירוק וגם של הכחול, אחרי מקום 10. */
    private static Object[] mediumWith(SquareKind kind, int roll) {
        for (long seed = 0; seed < 200; seed++) {
            Game g = game(Difficulty.MEDIUM, seed, new int[]{roll}, PlayerColor.GREEN, PlayerColor.BLUE);
            List<Square> green = path(g, PlayerColor.GREEN);
            for (int i = 10; i < green.size(); i++) {
                Square s = green.get(i);
                if (s.getKind() == kind && path(g, PlayerColor.BLUE).contains(s)
                        && green.get(i - roll).getKind() != SquareKind.HOME_PATH) {
                    return new Object[]{g, s};
                }
            }
        }
        throw new AssertionError("no medium board with a shared " + kind);
    }

    // ------------------------------------------------------------- FR-29: אכילה

    private static void normalCapture() {
        Game g = easy(3);
        Square target = path(g, PlayerColor.GREEN).get(greenIndexOf(g, SquareKind.REGULAR, 10));
        Pawn blue = pawn(g, 1, 0);
        put(g, blue, target);
        MoveResult r = greenLandsOn(g, pawn(g, 0, 0), target, 3);
        ModelTests.checkEq(1, r.getCaptured().size(), "one piece captured");
        ModelTests.check(r.getCaptured().contains(blue), "the blue piece was captured");
        ModelTests.check(blue.isInBase(), "blue piece back in the castle");
        ModelTests.checkEq(target, g.squareOf(pawn(g, 0, 0)), "green stays on the square");
    }

    // ------------------------------------------------------------- FR-30: משבצות מוגנות

    private static void star() {
        Game g = easy(3);
        Square target = path(g, PlayerColor.GREEN).get(greenIndexOf(g, SquareKind.SAFE, 5));
        Pawn blue = pawn(g, 1, 0);
        put(g, blue, target);
        MoveResult r = greenLandsOn(g, pawn(g, 0, 0), target, 3);
        ModelTests.checkEq(0, r.getCaptured().size(), "no capture on a star");
        ModelTests.checkEq(2, g.pawnsAt(target).size(), "both pieces share the star");
    }

    private static void otherStart() {
        Game g = easy(3);
        Square blueStart = path(g, PlayerColor.BLUE).get(0);
        Pawn blue = pawn(g, 1, 0);
        blue.setPathIndex(0);
        MoveResult r = greenLandsOn(g, pawn(g, 0, 0), blueStart, 3);
        ModelTests.checkEq(0, r.getCaptured().size(), "no capture on blue's #");
        ModelTests.check(!blue.isInBase(), "blue piece stays on its #");
        ModelTests.checkEq(2, g.pawnsAt(blueStart).size(), "both pieces share the #");
    }

    private static void surprise() {
        Object[] gs = mediumWith(SquareKind.SURPRISE, 3);
        Game g = (Game) gs[0];
        Square target = (Square) gs[1];
        Pawn blue = pawn(g, 1, 0);
        put(g, blue, target);
        MoveResult r = greenLandsOn(g, pawn(g, 0, 0), target, 3);
        ModelTests.checkEq(0, r.getCaptured().size(), "no capture on a surprise square");
        ModelTests.check(!blue.isInBase(), "blue piece stays");
    }

    private static void homeLane() {
        Game g = easy(1);
        for (Square s : g.getBoard().getSquares()) {
            SquareKind k = s.getKind();
            if (k == SquareKind.HOME_PATH || k == SquareKind.FINISH) {
                ModelTests.check(s.isSafe(), k + " " + s.getPosition() + " is protected");
                ModelTests.check(s.isPrivate(), k + " " + s.getPosition() + " is private");
                for (PlayerColor c : PlayerColor.values()) {
                    if (c != s.getOwner()) {
                        ModelTests.check(!path(g, c).contains(s), c + " can never enter " + s.getOwner() + "'s home");
                    }
                }
            }
        }
    }

    // ------------------------------------------------------------- FR-31: חץ בצבע אחר

    private static void otherArrow() {
        Game g = easy(3);
        Square blueArrow = null;
        for (Square s : path(g, PlayerColor.GREEN)) {
            if (s.getKind() == SquareKind.ARROW && s.getOwner() == PlayerColor.BLUE) {
                blueArrow = s;
            }
        }
        ModelTests.check(blueArrow != null, "blue's arrow is on green's path");
        ModelTests.check(!blueArrow.isSafe(), "an arrow square is not protected");
        Pawn blue = pawn(g, 1, 0);
        put(g, blue, blueArrow);
        MoveResult r = greenLandsOn(g, pawn(g, 0, 0), blueArrow, 3);
        ModelTests.checkEq(1, r.getCaptured().size(), "capture on blue's arrow");
        ModelTests.check(blue.isInBase(), "blue piece back in the castle");
    }

    private static void question() {
        Object[] gs = mediumWith(SquareKind.QUESTION, 3);
        Game g = (Game) gs[0];
        Square target = (Square) gs[1];
        Pawn blue = pawn(g, 1, 0);
        put(g, blue, target);
        MoveResult r = greenLandsOn(g, pawn(g, 0, 0), target, 3);
        ModelTests.checkEq(1, r.getCaptured().size(), "capture on a question square");
        ModelTests.check(blue.isInBase(), "blue piece back in the castle");
    }

    // ------------------------------------------------------------- Double Block

    private static void opponentBlock() {
        Game g = easy(3);
        int t = greenIndexOf(g, SquareKind.REGULAR, 10);
        Square target = path(g, PlayerColor.GREEN).get(t);
        put(g, pawn(g, 1, 0), target);
        put(g, pawn(g, 1, 1), target);
        Pawn green = pawn(g, 0, 0);
        green.setPathIndex(t - 3);
        g.roll();
        Move m = g.legalMoveFor(green);
        ModelTests.check(m == null || m.getTo() < t, "green cannot land on the block");

        // גם אם כלי מגיע לשם בדרך אחרת (למשל אחורה), Double Block לא נאכל
        green.setPathIndex(t);
        ModelTests.checkEq(0, g.captureAt(green).size(), "a Double Block is never captured");
        ModelTests.check(!pawn(g, 1, 0).isInBase() && !pawn(g, 1, 1).isInBase(), "both blue pieces stay");
    }

    private static void partialMoveCaptures() {
        // ירוק, כחול וצהוב. כחול בונה Double Block; צהוב יחיד במשבצת שלפניו. ירוק מטיל 5, לא יכול
        // לזוז את כל הצעדים ונעצר לפני הבלוק — בדיוק על הצהוב — ואוכל אותו (FR-29: כל סוג תזוזה).
        Game g = game(Difficulty.EASY, 7, new int[]{5}, PlayerColor.GREEN, PlayerColor.BLUE, PlayerColor.YELLOW);
        List<Square> green = path(g, PlayerColor.GREEN);
        int i = 10;
        while (!(green.get(i + 3).getKind() == SquareKind.REGULAR && green.get(i + 4).getKind() == SquareKind.REGULAR)) {
            i++;
        }
        put(g, pawn(g, 1, 0), green.get(i + 4));
        put(g, pawn(g, 1, 1), green.get(i + 4));
        Pawn yellow = pawn(g, 2, 0);
        put(g, yellow, green.get(i + 3));
        Pawn me = pawn(g, 0, 0);
        me.setPathIndex(i);
        g.roll();
        Move m = g.legalMoveFor(me);
        ModelTests.check(m != null && m.isStoppedByBlock(), "green stops before the block");
        ModelTests.checkEq(i + 3, m.getTo(), "green stops on the yellow piece");
        MoveResult r = g.move(m);
        ModelTests.check(r.getCaptured().contains(yellow), "yellow captured");
    }

    // ------------------------------------------------------------- תור נוסף

    private static void noExtraTurn() {
        Game g = easy(3);
        Square target = path(g, PlayerColor.GREEN).get(greenIndexOf(g, SquareKind.REGULAR, 10));
        put(g, pawn(g, 1, 0), target);
        MoveResult r = greenLandsOn(g, pawn(g, 0, 0), target, 3);
        ModelTests.checkEq(1, r.getCaptured().size(), "captured");
        ModelTests.check(!r.isExtraTurn(), "no extra turn for capturing");
        ModelTests.checkEq(PlayerColor.BLUE, g.getCurrentPlayer().getColor(), "turn passed to blue");
    }

    private static void captureWithSix() {
        Game g = easy(6);
        Square target = path(g, PlayerColor.GREEN).get(greenIndexOf(g, SquareKind.REGULAR, 12));
        put(g, pawn(g, 1, 0), target);
        MoveResult r = greenLandsOn(g, pawn(g, 0, 0), target, 6);
        ModelTests.checkEq(1, r.getCaptured().size(), "captured");
        ModelTests.check(r.isExtraTurn(), "the 6 gives an extra roll");
        ModelTests.checkEq(PlayerColor.GREEN, g.getCurrentPlayer().getColor(), "still green");
    }

    // ------------------------------------------------------------- אחורה (FR-29, דרך captureAt)

    private static void backward() {
        Game g = easy(1);
        int t = greenIndexOf(g, SquareKind.REGULAR, 10);
        Square target = path(g, PlayerColor.GREEN).get(t);
        Pawn blue = pawn(g, 1, 0);
        put(g, blue, target);
        Pawn green = pawn(g, 0, 0);
        green.setPathIndex(t + 3); // הכלי היה 3 משבצות קדימה...
        green.setPathIndex(t);     // ...וזז 3 אחורה (כמו בתשובה שגויה לשאלה)
        List<Pawn> captured = g.captureAt(green);
        ModelTests.checkEq(1, captured.size(), "capture after moving backward");
        ModelTests.check(blue.isInBase(), "blue piece back in the castle");
    }

    private static void backwardProtected() {
        Game g = easy(1);
        int t = greenIndexOf(g, SquareKind.SAFE, 5);
        Square star = path(g, PlayerColor.GREEN).get(t);
        Pawn blue = pawn(g, 1, 0);
        put(g, blue, star);
        Pawn green = pawn(g, 0, 0);
        green.setPathIndex(t);
        ModelTests.checkEq(0, g.captureAt(green).size(), "no capture on a star when moving backward");
        ModelTests.check(!blue.isInBase(), "blue piece stays");
    }

    // ------------------------------------------------------------- עוד

    private static void ownPiece() {
        Game g = easy(3);
        Square target = path(g, PlayerColor.GREEN).get(greenIndexOf(g, SquareKind.REGULAR, 10));
        Pawn first = pawn(g, 0, 1);
        put(g, first, target);
        MoveResult r = greenLandsOn(g, pawn(g, 0, 0), target, 3);
        ModelTests.checkEq(0, r.getCaptured().size(), "own piece not captured");
        ModelTests.check(!first.isInBase(), "own piece stays");
        ModelTests.checkEq(2, g.pawnsAt(target).size(), "two green pieces on the square");
    }

    private static void fourPlayers() {
        Game g = game(Difficulty.EASY, 7, new int[]{3},
                PlayerColor.GREEN, PlayerColor.BLUE, PlayerColor.YELLOW, PlayerColor.RED);
        List<Square> green = path(g, PlayerColor.GREEN);
        int t = greenIndexOf(g, SquareKind.REGULAR, 20);
        Pawn blueOnTarget = pawn(g, 1, 0);
        put(g, blueOnTarget, green.get(t));
        Pawn yellowElsewhere = pawn(g, 2, 0);
        put(g, yellowElsewhere, green.get(greenIndexOf(g, SquareKind.REGULAR, t + 2)));
        Pawn redElsewhere = pawn(g, 3, 0);
        put(g, redElsewhere, green.get(greenIndexOf(g, SquareKind.REGULAR, 5)));
        MoveResult r = greenLandsOn(g, pawn(g, 0, 0), green.get(t), 3);
        ModelTests.checkEq(1, r.getCaptured().size(), "exactly one piece captured");
        ModelTests.check(blueOnTarget.isInBase(), "blue on the target captured");
        ModelTests.check(!yellowElsewhere.isInBase() && !redElsewhere.isInBase(), "other players untouched");
    }
}

package Model;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * בדיקות ליציאה מהטירה (FR-20), כולל הקשר לאכילה (כלי שנאכל צריך שוב 6) ול-FR-36.
 * <p>
 * כל בדיקה מסומנת T4.xx כמו ברשומת הבדיקות של משימה 4 בדוח האיטרציה.
 * רצות יחד עם שאר בדיקות המודל ({@link ModelTests}), דרך build.sh / build.bat.
 *
 * @author Nimer Ziad (Zebra) – Iteration 1, SDP table 2
 */
final class CastleExitTests {

    private CastleExitTests() {
    }

    static void runAll() {
        ModelTests.run("T4.01 rolls 1-5 with all pieces in the castle: no move, turn passes", CastleExitTests::noExitBelowSix);
        ModelTests.run("T4.02 roll 6 with all pieces in the castle: one exit move to #", CastleExitTests::exitOnSix);
        ModelTests.run("T4.03 the piece lands on its own start square, for every color", CastleExitTests::ownStartEveryColor);
        ModelTests.run("T4.04 roll 6 with pieces in castle and on board: both choices offered", CastleExitTests::choiceOffered);
        ModelTests.run("T4.05 choosing the board piece on a 6 moves it 6", CastleExitTests::chooseBoardPiece);
        ModelTests.run("T4.06 choosing the castle on a 6 brings a new piece out", CastleExitTests::chooseExit);
        ModelTests.run("T4.07 roll 5 with pieces in castle and on board: only the board move", CastleExitTests::onlyBoardBelowSix);
        ModelTests.run("T4.08 clicking any piece in the castle gives the exit move", CastleExitTests::anyCastlePiece);
        ModelTests.run("T4.09 a captured piece goes back to the castle and needs a 6 again", CastleExitTests::capturedNeedsSix);
        ModelTests.run("T4.10 after leaving the castle with a 6 the player rolls again", CastleExitTests::rollAgainAfterExit);
        ModelTests.run("T4.11 leaving onto # with a single opponent there: no capture, both stay", CastleExitTests::shareStart);
        ModelTests.run("T4.12 FR-36 hook: opponent block on # stops the exit, own pieces do not", CastleExitTests::blockHook);
        ModelTests.run("T4.13 all 4 pieces can leave; then no exit is offered", CastleExitTests::allFourLeave);
    }

    // ------------------------------------------------------------- עזר

    /** קובייה שמחזירה תוצאות קבועות מראש. */
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

    private static Game game(Die die, PlayerColor... colors) {
        Board board = BoardBuilder.build(Difficulty.EASY, new Random(7));
        List<Player> players = new ArrayList<Player>();
        for (int i = 0; i < colors.length; i++) {
            players.add(new Player("P" + (i + 1), colors[i], i));
        }
        return new Game(board, players, die, () -> 0L);
    }

    private static Game greenBlue(int... rolls) {
        return game(new Dice(rolls), PlayerColor.GREEN, PlayerColor.BLUE);
    }

    private static Player green(Game g) {
        return g.getPlayers().get(0);
    }

    private static Player blue(Game g) {
        return g.getPlayers().get(1);
    }

    /** האינדקס במסלול של color שבו נמצאת המשבצת. */
    private static int indexOn(Game g, PlayerColor color, Square square) {
        return g.getBoard().getPath(color).indexOf(square);
    }

    private static int exitMoves(Game g) {
        int n = 0;
        for (Move m : g.getLegalMoves()) {
            if (m.getFrom() == Pawn.IN_BASE) {
                n++;
            }
        }
        return n;
    }

    /** האינדקס הראשון במסלול של הירוק, מ-from והלאה, שהמשבצת בו רגילה. */
    private static int regularIndex(Game g, int from) {
        List<Square> path = g.getBoard().getPath(PlayerColor.GREEN);
        for (int i = from; i < path.size(); i++) {
            if (path.get(i).getKind() == SquareKind.REGULAR) {
                return i;
            }
        }
        throw new AssertionError("no regular square after " + from);
    }

    // ------------------------------------------------------------- בדיקות

    private static void noExitBelowSix() {
        for (int v = 1; v <= 5; v++) {
            Game g = greenBlue(v);
            RollResult r = g.roll();
            ModelTests.check(!r.hasMoves(), "roll " + v + " should give no move");
            ModelTests.checkEq(PlayerColor.BLUE, g.getCurrentPlayer().getColor(), "roll " + v + ": turn passed");
            for (Pawn p : green(g).getPawns()) {
                ModelTests.check(p.isInBase(), "roll " + v + ": piece left the castle");
            }
        }
    }

    private static void exitOnSix() {
        Game g = greenBlue(6);
        ModelTests.check(g.roll().hasMoves(), "6 should allow leaving the castle");
        ModelTests.checkEq(1, g.getLegalMoves().size(), "one exit move");
        Move m = g.getLegalMoves().get(0);
        ModelTests.checkEq(Pawn.IN_BASE, m.getFrom(), "from the castle");
        ModelTests.checkEq(0, m.getTo(), "to path index 0 (#)");
        g.move(m);
        ModelTests.checkEq(SquareKind.START, g.squareOf(m.getPawn()).getKind(), "piece is on a start square");
    }

    private static void ownStartEveryColor() {
        for (PlayerColor c : PlayerColor.values()) {
            PlayerColor other = c == PlayerColor.RED ? PlayerColor.GREEN : PlayerColor.RED;
            Game g = game(new Dice(6), c, other);
            g.roll();
            Move m = g.getLegalMoves().get(0);
            g.move(m);
            Square s = g.squareOf(m.getPawn());
            ModelTests.checkEq(SquareKind.START, s.getKind(), c + " lands on a start square");
            ModelTests.checkEq(c, s.getOwner(), c + " lands on its own start square");
        }
    }

    /** הכלי שעל הלוח הוא פעם הכלי הראשון ופעם האחרון, כדי שסדר הכלים לא ישפיע על הבחירה. */
    private static final int[] BOARD_PIECE = {0, Player.PAWNS - 1};

    private static void choiceOffered() {
        for (int which : BOARD_PIECE) {
            Game g = greenBlue(6);
            Pawn onBoard = green(g).getPawns().get(which);
            onBoard.setPathIndex(regularIndex(g, 3));
            g.roll();
            ModelTests.checkEq(1, exitMoves(g), "piece " + which + " on board: exit move offered");
            ModelTests.check(g.legalMoveFor(onBoard) != null, "piece " + which + " on board can move");
            ModelTests.check(g.hasExitAndBoardChoice(), "piece " + which + " on board: player has to choose");
        }
    }

    private static void chooseBoardPiece() {
        for (int which : BOARD_PIECE) {
            Game g = greenBlue(6);
            Pawn onBoard = green(g).getPawns().get(which);
            int from = regularIndex(g, 3);
            onBoard.setPathIndex(from);
            g.roll();
            g.move(g.legalMoveFor(onBoard));
            ModelTests.checkEq(from + 6, onBoard.getPathIndex(), "piece " + which + " moved 6");
            for (Pawn p : green(g).getPawns()) {
                if (p != onBoard) {
                    ModelTests.check(p.isInBase(), "castle pieces stay");
                }
            }
        }
    }

    private static void chooseExit() {
        Game g = greenBlue(6);
        Pawn onBoard = green(g).getPawns().get(0);
        int from = regularIndex(g, 3);
        onBoard.setPathIndex(from);
        g.roll();
        Pawn inCastle = green(g).getPawns().get(1);
        Move exit = g.legalMoveFor(inCastle);
        g.move(exit);
        ModelTests.checkEq(0, exit.getPawn().getPathIndex(), "new piece on #");
        ModelTests.checkEq(from, onBoard.getPathIndex(), "board piece did not move");
    }

    private static void onlyBoardBelowSix() {
        Game g = greenBlue(5);
        Pawn onBoard = green(g).getPawns().get(0);
        onBoard.setPathIndex(regularIndex(g, 3));
        g.roll();
        ModelTests.checkEq(0, exitMoves(g), "no exit on 5");
        ModelTests.checkEq(1, g.getLegalMoves().size(), "only the board move");
        ModelTests.check(!g.hasExitAndBoardChoice(), "no choice on 5");
    }

    private static void anyCastlePiece() {
        Game g = greenBlue(6);
        g.roll();
        Move exit = g.getLegalMoves().get(0);
        for (Pawn p : green(g).getPawns()) {
            ModelTests.checkEq(exit, g.legalMoveFor(p), "piece " + p.getNumber() + " gives the exit move");
        }
    }

    private static void capturedNeedsSix() {
        // ירוק אוכל את הכחול; אחר כך הכחול מטיל 5 (לא יוצא), הירוק 1, והכחול 6 (יוצא)
        Game g = greenBlue(2, 5, 1, 6);
        Pawn greenPawn = green(g).getPawns().get(0);
        Pawn bluePawn = blue(g).getPawns().get(0);
        int target = regularIndex(g, 10);
        greenPawn.setPathIndex(target - 2);
        bluePawn.setPathIndex(indexOn(g, PlayerColor.BLUE, g.getBoard().getPath(PlayerColor.GREEN).get(target)));
        g.roll();
        MoveResult capture = g.move(g.legalMoveFor(greenPawn));
        ModelTests.checkEq(1, capture.getCaptured().size(), "blue piece captured");
        ModelTests.check(bluePawn.isInBase(), "captured piece is back in the castle");

        ModelTests.checkEq(PlayerColor.BLUE, g.getCurrentPlayer().getColor(), "blue's turn");
        ModelTests.check(!g.roll().hasMoves(), "blue rolls 5: the captured piece cannot leave");
        ModelTests.check(bluePawn.isInBase(), "still in the castle after 5");

        g.roll(); // ירוק מטיל 1 ומזיז
        g.move(g.legalMoveFor(greenPawn));

        ModelTests.check(g.roll().hasMoves(), "blue rolls 6: may leave the castle");
        ModelTests.checkEq(1, exitMoves(g), "exit move offered to blue");
        g.move(g.legalMoveFor(bluePawn));
        ModelTests.checkEq(0, bluePawn.getPathIndex(), "captured piece is back on blue's #");
    }

    private static void rollAgainAfterExit() {
        Game g = greenBlue(6);
        g.roll();
        MoveResult r = g.move(g.getLegalMoves().get(0));
        ModelTests.check(r.isExtraTurn(), "extra roll after a 6");
        ModelTests.checkEq(PlayerColor.GREEN, g.getCurrentPlayer().getColor(), "still green's turn");
        ModelTests.checkEq(GamePhase.ROLL, g.getPhase(), "green rolls again");
    }

    private static void shareStart() {
        Game g = greenBlue(6);
        Square greenStart = g.getBoard().getPath(PlayerColor.GREEN).get(0);
        Pawn bluePawn = blue(g).getPawns().get(0);
        bluePawn.setPathIndex(indexOn(g, PlayerColor.BLUE, greenStart));
        g.roll();
        MoveResult r = g.move(g.getLegalMoves().get(0));
        ModelTests.checkEq(0, r.getCaptured().size(), "no capture on #");
        ModelTests.check(!bluePawn.isInBase(), "blue piece stays");
        ModelTests.checkEq(2, g.pawnsAt(greenStart).size(), "both pieces share #");
    }

    private static void blockHook() {
        Game g = greenBlue(6);
        Square greenStart = g.getBoard().getPath(PlayerColor.GREEN).get(0);
        ModelTests.check(g.canLeaveBase(green(g)), "free # : may leave");

        // שני כלים של הירוק עצמו על ה-#: עדיין מותר לצאת
        green(g).getPawns().get(0).setPathIndex(0);
        green(g).getPawns().get(1).setPathIndex(0);
        ModelTests.check(g.canLeaveBase(green(g)), "own pieces on # do not block");

        // Double Block של הכחול על ה-# של הירוק: אסור לצאת (FR-36)
        green(g).getPawns().get(0).setPathIndex(Pawn.IN_BASE);
        green(g).getPawns().get(1).setPathIndex(Pawn.IN_BASE);
        int idx = indexOn(g, PlayerColor.BLUE, greenStart);
        blue(g).getPawns().get(0).setPathIndex(idx);
        blue(g).getPawns().get(1).setPathIndex(idx);
        ModelTests.check(!g.canLeaveBase(green(g)), "opponent block on # stops the exit");
        ModelTests.check(!g.roll().hasMoves(), "no exit move offered");
    }

    /** שלושה כלים כבר על הלוח ואחד בטירה: ב-6 הרביעי יוצא, וב-6 הבא כבר אין יציאה. */
    private static void allFourLeave() {
        Game g = greenBlue(6, 6);
        List<Pawn> pawns = green(g).getPawns();
        pawns.get(0).setPathIndex(regularIndex(g, 5));
        pawns.get(1).setPathIndex(regularIndex(g, 15));
        pawns.get(2).setPathIndex(regularIndex(g, 25));
        Pawn last = pawns.get(3);

        g.roll();
        ModelTests.checkEq(1, exitMoves(g), "exit offered for the 4th piece");
        g.move(g.legalMoveFor(last));
        ModelTests.checkEq(0, last.getPathIndex(), "4th piece on #");

        ModelTests.checkEq(PlayerColor.GREEN, g.getCurrentPlayer().getColor(), "extra roll after the 6");
        g.roll();
        ModelTests.checkEq(0, exitMoves(g), "castle is empty: no exit offered");
        ModelTests.check(!g.getLegalMoves().isEmpty(), "board pieces can still move");
    }
}

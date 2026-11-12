package Model;

import static Model.DieAndTurnTests.expectThrows;
import static Model.DieAndTurnTests.game;
import static Model.ModelTests.check;
import static Model.ModelTests.checkEq;

import java.util.List;
import java.util.Random;

import Model.DieAndTurnTests.ScriptedDie;

/**
 * Tests for moving a piece (FR-21, FR-22) and for the legal-move check (FR-23).
 * <p>
 * Leaving the castle (FR-20) is covered by {@code CastleExitTests}. Each test name starts with the
 * FR it checks, as in the acceptance tests. The tests run together with the other model tests
 * ({@link ModelTests}) through build.sh / build.bat.
 *
 * @author Waseem Abu Fanna (Zebra) – Iteration 1, SDP table 2 (FR-20–FR-23)
 */
final class MovementTests {

    private MovementTests() {
    }

    static void runAll() {
        ModelTests.run("FR-21 one roll moves one piece all the steps; the other pieces stay", MovementTests::onePieceWholeRoll);
        ModelTests.run("FR-21 the steps are never split: every legal move is exactly the roll, for 1 to 6", MovementTests::neverSplit);
        ModelTests.run("FR-21 the piece follows its own path, not another color's", MovementTests::ownPath);
        ModelTests.run("FR-22 every path goes clockwise, on every level and for every color", MovementTests::clockwise);
        ModelTests.run("FR-22 every path is continuous: each square touches the next", MovementTests::continuous);
        ModelTests.run("FR-22 the path turns inward only at the player's own arrows (medium, hard)", MovementTests::arrowsOnPath);
        ModelTests.run("FR-22 passing or landing on its arrow takes the piece to the next track", MovementTests::arrowTurnsInward);
        ModelTests.run("FR-22 the last arrow leads into the home path (easy)", MovementTests::intoHomePath);
        ModelTests.run("FR-23 only pieces with a legal move are offered", MovementTests::onlyLegalOffered);
        ModelTests.run("FR-23 a move that is not on the legal list is refused and nothing changes", MovementTests::illegalRefused);
    }

    // ------------------------------------------------------------- helpers

    private static Pawn pawn(Game g, int player, int n) {
        return g.getPlayers().get(player).getPawns().get(n);
    }

    /** 0 for the outermost track, 1 for the next one, and the number of outer tracks for the regular board. */
    private static int trackOf(Board b, Position p) {
        int n = b.getSize();
        int edge = Math.min(Math.min(p.getRow(), p.getCol()), Math.min(n - 1 - p.getRow(), n - 1 - p.getCol()));
        return Math.min(edge, b.getDifficulty().getOuterRings());
    }

    /** The index of the last shared square of the path: the arrow before the home path. */
    private static int lastSharedIndex(List<Square> path) {
        int i = 0;
        while (path.get(i + 1).getKind() != SquareKind.HOME_PATH) {
            i++;
        }
        return i;
    }

    private static int firstOwnArrow(List<Square> path, PlayerColor color) {
        for (int i = 0; i < path.size(); i++) {
            if (path.get(i).getKind() == SquareKind.ARROW && path.get(i).getOwner() == color) {
                return i;
            }
        }
        throw new AssertionError("no arrow of " + color + " on its path");
    }

    // ------------------------------------------------------------- FR-21

    private static void onePieceWholeRoll() {
        Game g = game(new ScriptedDie(4), PlayerColor.GREEN, PlayerColor.BLUE);
        Pawn a = pawn(g, 0, 0);
        Pawn b = pawn(g, 0, 1);
        a.setPathIndex(3);
        b.setPathIndex(10);
        g.roll();
        List<Move> moves = g.getLegalMoves();
        checkEq(2, moves.size(), "both board pieces can move");
        for (Move m : moves) {
            checkEq(4, m.getTo() - m.getFrom(), m + ": all 4 steps");
            check(!m.isStoppedByBlock(), m + ": not stopped");
        }
        g.move(g.legalMoveFor(a));
        checkEq(7, a.getPathIndex(), "a moved 4");
        checkEq(10, b.getPathIndex(), "b did not move");
        check(pawn(g, 0, 2).isInBase() && pawn(g, 0, 3).isInBase(), "the castle pieces did not move");
        for (Pawn p : g.getPlayers().get(1).getPawns()) {
            check(p.isInBase(), "blue did not move");
        }
        checkEq(g.getBoard().getPath(PlayerColor.GREEN).get(7), g.squareOf(a), "a stands on square 7 of its path");
    }

    /**
     * One piece on the board at different places, every roll: the move is exactly the roll, and
     * a roll that goes past home is no move at all (FR-24).
     */
    private static void neverSplit() {
        int[] starts = {0, 5, 20, 40, -3};
        for (int value = 1; value <= 6; value++) {
            for (int s : starts) {
                Game g = game(new ScriptedDie(value), PlayerColor.GREEN, PlayerColor.BLUE);
                int finish = g.getBoard().getFinishIndex();
                int from = s >= 0 ? s : finish + s;
                Pawn a = pawn(g, 0, 0);
                a.setPathIndex(from);
                g.roll();
                Move m = g.legalMoveFor(a);
                String where = value + " from " + from;
                if (from + value <= finish) {
                    check(m != null, where + ": the piece can move");
                    checkEq(from + value, m.getTo(), where + ": exactly the roll");
                    check(!m.isStoppedByBlock(), where + ": not stopped");
                } else {
                    check(m == null, where + ": past home, no move");
                }
                for (Move other : g.getLegalMoves()) {
                    if (other.getFrom() != Pawn.IN_BASE) {
                        checkEq(value, other.getTo() - other.getFrom(), where + ": no split");
                    }
                }
            }
        }
    }

    /** Hard level, 4 players: blue's piece moves along blue's path. */
    private static void ownPath() {
        Game g = game(Difficulty.HARD, new ScriptedDie(5), PlayerColor.GREEN, PlayerColor.YELLOW, PlayerColor.BLUE,
                PlayerColor.RED);
        Board b = g.getBoard();
        Pawn p = pawn(g, 2, 0);
        p.setPathIndex(30);
        g.roll(); // green: 5 with all pieces in the castle, the turn passes
        g.roll(); // yellow: the same
        checkEq(PlayerColor.BLUE, g.getCurrentPlayer().getColor(), "blue's turn");
        g.roll();
        g.move(g.legalMoveFor(p));
        checkEq(35, p.getPathIndex(), "moved 5");
        checkEq(b.getPath(PlayerColor.BLUE).get(35), g.squareOf(p), "on square 35 of blue's path");
        check(b.getPath(PlayerColor.GREEN).get(35) != g.squareOf(p), "not on square 35 of green's path");
    }

    // ------------------------------------------------------------- FR-22

    /**
     * Twice the signed area of the shared part of each path, with rows going down: a path that
     * goes clockwise on the screen gives a positive number.
     */
    private static void clockwise() {
        for (Difficulty level : Difficulty.values()) {
            Board b = BoardBuilder.build(level, new Random(1));
            for (PlayerColor color : PlayerColor.values()) {
                List<Square> path = b.getPath(color);
                int last = lastSharedIndex(path);
                long area2 = 0;
                for (int i = 0; i <= last; i++) {
                    Position p = path.get(i).getPosition();
                    Position q = path.get(i == last ? 0 : i + 1).getPosition();
                    area2 += (long) p.getCol() * q.getRow() - (long) q.getCol() * p.getRow();
                }
                check(area2 > 0, level + " " + color + ": clockwise (signed area " + area2 + ")");
            }
        }
    }

    private static void continuous() {
        for (Difficulty level : Difficulty.values()) {
            Board b = BoardBuilder.build(level, new Random(1));
            for (PlayerColor color : PlayerColor.values()) {
                List<Square> path = b.getPath(color);
                String who = level + " " + color;
                checkEq(SquareKind.START, path.get(0).getKind(), who + ": starts at #");
                checkEq(SquareKind.FINISH, path.get(path.size() - 1).getKind(), who + ": ends at home");
                for (int i = 1; i < path.size(); i++) {
                    Position p = path.get(i - 1).getPosition();
                    Position q = path.get(i).getPosition();
                    // next to each other, straight or diagonally (the track turns diagonally at the
                    // inner corners of the cross, as on the real board)
                    int d = Math.max(Math.abs(p.getRow() - q.getRow()), Math.abs(p.getCol() - q.getCol()));
                    checkEq(1, d, who + ": square " + (i - 1) + " touches square " + i);
                }
            }
        }
    }

    /**
     * Medium and hard: after an arrow of the player's color the path is one track further in
     * (or in the home path, after the last one); after another color's arrow it stays on the same
     * track. 8 arrows on medium and 12 on hard: each path meets its own and passes the others.
     */
    private static void arrowsOnPath() {
        for (Difficulty level : new Difficulty[] {Difficulty.MEDIUM, Difficulty.HARD}) {
            Board b = BoardBuilder.build(level, new Random(1));
            int rings = level.getOuterRings();
            for (PlayerColor color : PlayerColor.values()) {
                List<Square> path = b.getPath(color);
                int own = 0;
                int others = 0;
                for (int i = 0; i < path.size() - 1; i++) {
                    Square s = path.get(i);
                    if (s.getKind() != SquareKind.ARROW) {
                        continue;
                    }
                    Square next = path.get(i + 1);
                    String at = level + " " + color + " square " + i;
                    if (s.getOwner() == color) {
                        own++;
                        if (next.getKind() == SquareKind.HOME_PATH) {
                            checkEq(rings, trackOf(b, s.getPosition()), at + ": the home arrow is on the regular board");
                        } else {
                            checkEq(trackOf(b, s.getPosition()) + 1, trackOf(b, next.getPosition()),
                                    at + ": own arrow leads one track inward");
                        }
                    } else {
                        others++;
                        checkEq(trackOf(b, s.getPosition()), trackOf(b, next.getPosition()),
                                at + ": another color's arrow is passed like a regular square");
                    }
                }
                checkEq(rings + 1, own, level + " " + color + ": own arrows on the path");
                checkEq(3 * (rings + 1), others, level + " " + color + ": other arrows passed");
            }
        }
    }

    /** Medium: one piece passes the first arrow with a 4, another lands on it with a 2 and then moves 1. */
    private static void arrowTurnsInward() {
        Game g = game(Difficulty.MEDIUM, new ScriptedDie(4, 1, 2, 1, 1), PlayerColor.GREEN, PlayerColor.BLUE);
        Board b = g.getBoard();
        List<Square> path = b.getPath(PlayerColor.GREEN);
        int arrow = firstOwnArrow(path, PlayerColor.GREEN);
        checkEq(0, trackOf(b, path.get(arrow).getPosition()), "the first own arrow is on the outer track");
        Pawn p = pawn(g, 0, 0);
        Pawn q = pawn(g, 0, 1);
        p.setPathIndex(arrow - 2);
        q.setPathIndex(arrow - 2);

        g.roll(); // green: 4
        g.move(g.legalMoveFor(p));
        checkEq(arrow + 2, p.getPathIndex(), "4 steps, through the arrow");
        checkEq(1, trackOf(b, g.squareOf(p).getPosition()), "p is on the next track");

        g.roll(); // blue: 1, all pieces in the castle, the turn passes
        g.roll(); // green: 2
        g.move(g.legalMoveFor(q));
        checkEq(arrow, q.getPathIndex(), "q landed on the arrow");
        checkEq(SquareKind.ARROW, g.squareOf(q).getKind(), "it is the arrow square");
        checkEq(PlayerColor.GREEN, g.squareOf(q).getOwner(), "green's arrow");

        g.roll(); // blue: 1
        g.roll(); // green: 1
        g.move(g.legalMoveFor(q));
        checkEq(1, trackOf(b, g.squareOf(q).getPosition()), "from the arrow q continues on the next track");
    }

    /** Easy: one piece passes the arrow with a 3, another lands on it with a 2 and then moves 1. */
    private static void intoHomePath() {
        Game g = game(new ScriptedDie(3, 1, 2, 1, 1), PlayerColor.GREEN, PlayerColor.BLUE);
        List<Square> path = g.getBoard().getPath(PlayerColor.GREEN);
        int arrow = lastSharedIndex(path);
        checkEq(SquareKind.ARROW, path.get(arrow).getKind(), "the home path starts after an arrow");
        checkEq(PlayerColor.GREEN, path.get(arrow).getOwner(), "green's arrow");
        Pawn p = pawn(g, 0, 0);
        Pawn q = pawn(g, 0, 1);
        p.setPathIndex(arrow - 1);
        q.setPathIndex(arrow - 2);

        g.roll(); // green: 3
        g.move(g.legalMoveFor(p));
        checkEq(arrow + 2, p.getPathIndex(), "passed the arrow into the home path");
        checkEq(SquareKind.HOME_PATH, g.squareOf(p).getKind(), "on the home path");
        checkEq(PlayerColor.GREEN, g.squareOf(p).getOwner(), "green's own home path");

        g.roll(); // blue: 1, the turn passes
        g.roll(); // green: 2
        g.move(g.legalMoveFor(q));
        checkEq(SquareKind.ARROW, g.squareOf(q).getKind(), "q landed on the arrow");

        g.roll(); // blue: 1
        g.roll(); // green: 1
        g.move(g.legalMoveFor(q));
        checkEq(SquareKind.HOME_PATH, g.squareOf(q).getKind(), "from the arrow into the home path");
    }

    // ------------------------------------------------------------- FR-23

    private static void onlyLegalOffered() {
        Game g = game(new ScriptedDie(4, 1, 6), PlayerColor.GREEN, PlayerColor.BLUE);
        int finish = g.getBoard().getFinishIndex();
        Pawn canMove = pawn(g, 0, 0);
        Pawn tooFar = pawn(g, 0, 1);
        Pawn inCastle = pawn(g, 0, 2);
        Pawn home = pawn(g, 0, 3);
        canMove.setPathIndex(3);
        tooFar.setPathIndex(finish - 2);
        home.setPathIndex(finish);

        g.roll(); // green: 4
        checkEq(1, g.getLegalMoves().size(), "one piece is offered");
        check(g.legalMoveFor(canMove) != null, "the piece that can move 4");
        check(g.legalMoveFor(tooFar) == null, "2 from home: 4 is too much");
        check(g.legalMoveFor(inCastle) == null, "castle piece: not a 6");
        check(g.legalMoveFor(home) == null, "a piece at home does not move");
        g.move(g.legalMoveFor(canMove));

        g.roll(); // blue: 1, the turn passes
        g.roll(); // green: 6
        checkEq(2, g.getLegalMoves().size(), "on a 6: the board piece and one exit from the castle");
        check(g.legalMoveFor(canMove) != null, "the board piece can move 6");
        check(g.legalMoveFor(inCastle) != null, "a castle piece may leave on a 6");
        check(g.legalMoveFor(tooFar) == null, "still too far");
        check(g.legalMoveFor(home) == null, "still at home");
    }

    private static void illegalRefused() {
        Game g = game(new ScriptedDie(4), PlayerColor.GREEN, PlayerColor.BLUE);
        int finish = g.getBoard().getFinishIndex();
        Pawn a = pawn(g, 0, 0);
        Pawn far = pawn(g, 0, 1);
        Pawn castle = pawn(g, 0, 2);
        Pawn blue = pawn(g, 1, 0);
        a.setPathIndex(3);
        far.setPathIndex(finish - 2);
        g.roll();
        int before = g.getLegalMoves().size();

        expectThrows(IllegalArgumentException.class,
                () -> g.move(new Move(far, finish - 2, finish + 2, false)), "past home");
        expectThrows(IllegalArgumentException.class, () -> g.move(new Move(a, 3, 8, false)), "5 steps on a 4");
        expectThrows(IllegalArgumentException.class,
                () -> g.move(new Move(castle, Pawn.IN_BASE, 0, false)), "leaving the castle without a 6");
        expectThrows(IllegalArgumentException.class,
                () -> g.move(new Move(blue, Pawn.IN_BASE, 0, false)), "an opponent's piece");

        checkEq(3, a.getPathIndex(), "a did not move");
        checkEq(finish - 2, far.getPathIndex(), "far did not move");
        check(castle.isInBase() && blue.isInBase(), "the castle pieces did not move");
        checkEq(GamePhase.MOVE, g.getPhase(), "still waiting for the move");
        checkEq(PlayerColor.GREEN, g.getCurrentPlayer().getColor(), "still green's turn");
        checkEq(before, g.getLegalMoves().size(), "the legal moves are unchanged");
        g.move(g.legalMoveFor(a));
        checkEq(7, a.getPathIndex(), "the legal move still works");
    }
}

package Model;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static Model.ModelTests.check;
import static Model.ModelTests.checkEq;

/**
 * Unit tests for US-6: exact entry home, forced moves and Double Block (FR-24, FR-25, FR-32 to
 * FR-37), plus capture on every level (FR-29, FR-30). Every test runs on all three levels, because
 * each level builds a different board (rings, arrows, random safe / question / surprise squares).
 * <p>
 * Tests that were already there (MovementTests, CaptureTests, CastleExitTests and the old tests in
 * ModelTests) check most of these rules on one board only; these tests check them on every level
 * and add the edge cases that had no test.
 *
 * @author Nimer Ziad (Zebra) – Iteration 2, US-6
 */
final class DoubleBlockTests {

    private DoubleBlockTests() {
    }

    static void runAll() {
        ModelTests.run("T10.01 FR-24 home only with the exact number, on every level", DoubleBlockTests::exactEntry);
        ModelTests.run("T10.02 FR-24 a piece one step from home moves only on a 1; otherwise the turn passes", DoubleBlockTests::oneStepFromHome);
        ModelTests.run("T10.03 FR-25 the move is forced even if it breaks the player's own Double Block", DoubleBlockTests::forcedBreaksOwnBlock);
        ModelTests.run("T10.04 FR-25 with a 6, breaking the block or leaving the castle: a move is still forced", DoubleBlockTests::forcedWithSix);
        ModelTests.run("T10.05 FR-32 two pieces of one player block on every kind of shared square", DoubleBlockTests::blockOnEveryKind);
        ModelTests.run("T10.06 FR-32 a block breaks only when one piece is left (3 -> 2 -> 1)", DoubleBlockTests::blockBreaksAtOne);
        ModelTests.run("T10.07 FR-32 two pieces of different players are not a block", DoubleBlockTests::mixedIsNoBlock);
        ModelTests.run("T10.08 FR-33 an opponent can neither pass nor land on a Double Block", DoubleBlockTests::cannotPassOrLand);
        ModelTests.run("T10.09 FR-34 the owner passes his own block and lands on it", DoubleBlockTests::ownerPassesAndLands);
        ModelTests.run("T10.10 FR-35 stop before the block only when no piece can move the whole roll", DoubleBlockTests::stopBeforeBlock);
        ModelTests.run("T10.11 FR-36 opponent block on # keeps the castle closed, own block does not", DoubleBlockTests::castleExitEveryLevel);
        ModelTests.run("T10.12 FR-37 an opponent leaves a safe square after a block formed on it", DoubleBlockTests::leaveSafeAfterBlock);
        ModelTests.run("T10.13 FR-29 FR-30 capture on a regular square, none on a safe one, on every level", DoubleBlockTests::captureEveryLevel);
    }

    // ------------------------------------------------------------- helpers

    /** A die that returns the given values in a loop. */
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

    /** Green (first to play) against blue. */
    private static Game greenBlue(Difficulty d, int... rolls) {
        return game(d, 7, rolls, PlayerColor.GREEN, PlayerColor.BLUE);
    }

    private static Pawn pawn(Game g, int player, int number) {
        return g.getPlayers().get(player).getPawns().get(number);
    }

    private static List<Square> path(Game g, PlayerColor c) {
        return g.getBoard().getPath(c);
    }

    /** Puts the piece on the square, wherever that square is on its own path. */
    private static void put(Game g, Pawn p, Square sq) {
        int idx = path(g, p.getColor()).indexOf(sq);
        if (idx < 0) {
            throw new AssertionError(p.getColor() + " path does not pass " + sq.getPosition());
        }
        p.setPathIndex(idx);
    }

    /**
     * A square of this kind on the shared track that both green and blue pass, at least {@code min}
     * steps into both paths, or null if this board has none.
     */
    private static Square shared(Game g, SquareKind kind, int min) {
        List<Square> green = path(g, PlayerColor.GREEN);
        List<Square> blue = path(g, PlayerColor.BLUE);
        for (int i = min; i < green.size(); i++) {
            Square s = green.get(i);
            if (s.getKind() == kind && !s.isPrivate() && blue.indexOf(s) >= min) {
                return s;
            }
        }
        return null;
    }

    private static Square sharedOrFail(Game g, SquareKind kind, int min) {
        Square s = shared(g, kind, min);
        if (s == null) {
            throw new AssertionError("no shared " + kind + " on the " + g.getBoard().getDifficulty() + " board");
        }
        return s;
    }

    /** An index on green's path, from {@code min} on, where this square and the next {@code clearAhead} are regular. */
    private static int greenRegular(Game g, int min, int clearAhead) {
        List<Square> p = path(g, PlayerColor.GREEN);
        for (int i = min; i + clearAhead < p.size(); i++) {
            boolean ok = true;
            for (int k = 0; k <= clearAhead && ok; k++) {
                Square s = p.get(i + k);
                ok = s.getKind() == SquareKind.REGULAR;
            }
            if (ok) {
                return i;
            }
        }
        throw new AssertionError("no plain stretch on green's path");
    }

    private static String on(Difficulty d) {
        return " (" + d + ")";
    }

    // ------------------------------------------------------------- FR-24

    /** From k steps before home: a roll of k enters, a roll of k + 1 is no move. */
    private static void exactEntry() {
        for (Difficulty d : Difficulty.values()) {
            for (int k = 1; k <= 5; k++) {
                Game g = greenBlue(d, k);
                int finish = g.getBoard().getFinishIndex();
                Pawn a = pawn(g, 0, 0);
                a.setPathIndex(finish - k);
                g.roll();
                MoveResult r = g.move(g.legalMoveFor(a));
                check(r.isReachedFinish(), k + " from home with " + k + " enters" + on(d));

                Game g2 = greenBlue(d, k + 1);
                Pawn b = pawn(g2, 0, 0);
                b.setPathIndex(finish - k);
                RollResult rr = g2.roll();
                check(g2.legalMoveFor(b) == null, k + " from home with " + (k + 1) + " does not move" + on(d));
                checkEq(finish - k, b.getPathIndex(), "the piece stays" + on(d));
                if (k + 1 < 6) {
                    check(!rr.hasMoves(), "no other move" + on(d)); // a 6 could still leave the castle
                }
            }
        }
    }

    /** One step from home with 2 to 5: no move, and the turn passes to blue (FR-19). */
    private static void oneStepFromHome() {
        for (Difficulty d : Difficulty.values()) {
            for (int roll = 2; roll <= 5; roll++) {
                Game g = greenBlue(d, roll);
                Pawn a = pawn(g, 0, 0);
                a.setPathIndex(g.getBoard().getFinishIndex() - 1);
                g.roll();
                checkEq(PlayerColor.BLUE, g.getCurrentPlayer().getColor(), "roll " + roll + ": turn passed" + on(d));
            }
        }
    }

    // ------------------------------------------------------------- FR-25

    /**
     * Green's only pieces on the board form a Double Block. A 3 gives only moves that break it,
     * and the game waits for one of them: there is no way to skip the move.
     */
    private static void forcedBreaksOwnBlock() {
        for (Difficulty d : Difficulty.values()) {
            Game g = greenBlue(d, 3);
            int at = greenRegular(g, 8, 4);
            Square blockSq = path(g, PlayerColor.GREEN).get(at);
            pawn(g, 0, 0).setPathIndex(at);
            pawn(g, 0, 1).setPathIndex(at);
            check(g.isBlockedFor(blockSq, g.getPlayers().get(1)), "green's block stands" + on(d));

            RollResult r = g.roll();
            check(r.hasMoves(), "there is a legal move" + on(d));
            checkEq(GamePhase.MOVE, g.getPhase(), "the game waits for a move" + on(d));
            checkEq(2, g.getLegalMoves().size(), "both block pieces may move" + on(d));
            boolean refused = false;
            try {
                g.roll();
            } catch (IllegalStateException e) {
                refused = true;
            }
            check(refused, "rolling again instead of moving is refused" + on(d));

            g.move(g.legalMoveFor(pawn(g, 0, 0)));
            check(!g.isBlockedFor(blockSq, g.getPlayers().get(1)), "the block is broken" + on(d));
        }
    }

    /** With a 6 the player may leave the castle or break the block, but must do one of them. */
    private static void forcedWithSix() {
        for (Difficulty d : Difficulty.values()) {
            Game g = greenBlue(d, 6);
            int at = greenRegular(g, 8, 7);
            pawn(g, 0, 0).setPathIndex(at);
            pawn(g, 0, 1).setPathIndex(at);
            g.roll();
            checkEq(GamePhase.MOVE, g.getPhase(), "a move is forced" + on(d));
            check(g.legalMoveFor(pawn(g, 0, 0)) != null, "breaking the block is offered" + on(d));
            check(g.legalMoveFor(pawn(g, 0, 2)) != null, "leaving the castle is offered" + on(d));
            check(g.hasExitAndBoardChoice(), "the player chooses between them" + on(d));
        }
    }

    // ------------------------------------------------------------- FR-32

    /** On every kind of shared square on the board: two blue pieces block green, not blue. */
    private static void blockOnEveryKind() {
        SquareKind[] kinds = {SquareKind.REGULAR, SquareKind.SAFE, SquareKind.START, SquareKind.ARROW,
                SquareKind.QUESTION, SquareKind.SURPRISE};
        for (Difficulty d : Difficulty.values()) {
            int tested = 0;
            for (SquareKind kind : kinds) {
                Game g = greenBlue(d, 1);
                Square s = shared(g, kind, 0);
                if (s == null) {
                    continue; // this level has no such shared square (e.g. no ? on easy)
                }
                Player green = g.getPlayers().get(0);
                Player blue = g.getPlayers().get(1);
                put(g, pawn(g, 1, 0), s);
                check(!g.isBlockedFor(s, green), "one piece is not a block on " + kind + on(d));
                put(g, pawn(g, 1, 1), s);
                check(g.isBlockedFor(s, green), "two pieces block on " + kind + on(d));
                check(!g.isBlockedFor(s, blue), "the block does not stop its owner on " + kind + on(d));
                tested++;
            }
            check(tested >= 3, "at least regular, safe and start were tested" + on(d));
        }
    }

    /** Three pieces: one leaves, still a block; the second leaves, the block is gone. */
    private static void blockBreaksAtOne() {
        for (Difficulty d : Difficulty.values()) {
            Game g = greenBlue(d, 2);
            Square s = sharedOrFail(g, SquareKind.REGULAR, 10);
            Player green = g.getPlayers().get(0);
            for (int i = 0; i < 3; i++) {
                put(g, pawn(g, 1, i), s);
            }
            check(g.isBlockedFor(s, green), "3 pieces block" + on(d));
            pawn(g, 1, 0).sendToBase();
            check(g.isBlockedFor(s, green), "2 pieces still block" + on(d));
            pawn(g, 1, 1).setPathIndex(pawn(g, 1, 1).getPathIndex() + 1);
            check(!g.isBlockedFor(s, green), "1 piece is no block" + on(d));

            // and green can now land on it and capture the single piece left (FR-29)
            Pawn a = pawn(g, 0, 0);
            a.setPathIndex(path(g, PlayerColor.GREEN).indexOf(s) - 2);
            g.roll();
            Move m = g.legalMoveFor(a);
            check(m != null && !m.isStoppedByBlock(), "green lands on the square" + on(d));
            MoveResult r = g.move(m);
            checkEq(1, r.getCaptured().size(), "the single piece is captured" + on(d));
        }
    }

    /** A blue and a yellow piece on one safe square do not stop green. */
    private static void mixedIsNoBlock() {
        for (Difficulty d : Difficulty.values()) {
            Game g = game(d, 7, new int[]{3}, PlayerColor.GREEN, PlayerColor.BLUE, PlayerColor.YELLOW);
            Square s = null;
            List<Square> green = path(g, PlayerColor.GREEN);
            for (int i = 10; i < green.size() && s == null; i++) {
                Square c = green.get(i);
                if (c.getKind() == SquareKind.SAFE && path(g, PlayerColor.BLUE).contains(c)
                        && path(g, PlayerColor.YELLOW).contains(c)) {
                    s = c;
                }
            }
            check(s != null, "a safe square all three pass" + on(d));
            put(g, pawn(g, 1, 0), s);
            put(g, pawn(g, 2, 0), s);
            check(!g.isBlockedFor(s, g.getPlayers().get(0)), "blue + yellow is not a block" + on(d));
            Pawn a = pawn(g, 0, 0);
            a.setPathIndex(green.indexOf(s) - 3);
            g.roll();
            Move m = g.legalMoveFor(a);
            check(m != null && m.getTo() == green.indexOf(s), "green lands with them" + on(d));
            MoveResult r = g.move(m);
            checkEq(0, r.getCaptured().size(), "no capture on a safe square" + on(d));
        }
    }

    // ------------------------------------------------------------- FR-33, FR-34, FR-35

    /** Blue block 2 ahead of green: a 2 (landing) and a 4 (passing) are both refused. */
    private static void cannotPassOrLand() {
        for (Difficulty d : Difficulty.values()) {
            for (int roll : new int[]{2, 4}) {
                Game g = greenBlue(d, roll);
                Square s = sharedOrFail(g, SquareKind.REGULAR, 10);
                put(g, pawn(g, 1, 0), s);
                put(g, pawn(g, 1, 1), s);
                int at = path(g, PlayerColor.GREEN).indexOf(s);
                Pawn a = pawn(g, 0, 0);
                a.setPathIndex(at - 2);
                Pawn free = pawn(g, 0, 1);
                free.setPathIndex(Math.max(0, at - 9)); // a free piece, so stopping early is not allowed
                g.roll();
                Move m = g.legalMoveFor(a);
                check(m == null, "roll " + roll + ": no move onto or past the block" + on(d));
                for (Move lm : g.getLegalMoves()) {
                    check(!(lm.getFrom() < at && lm.getTo() >= at), "no legal move reaches or crosses the block" + on(d));
                }
            }
        }
    }

    /** Green block: another green piece passes it (5) and lands on it (3, making 3 pieces). */
    private static void ownerPassesAndLands() {
        for (Difficulty d : Difficulty.values()) {
            for (int roll : new int[]{3, 5}) {
                Game g = greenBlue(d, roll);
                Square s = sharedOrFail(g, SquareKind.REGULAR, 10);
                put(g, pawn(g, 0, 0), s);
                put(g, pawn(g, 0, 1), s);
                int at = path(g, PlayerColor.GREEN).indexOf(s);
                Pawn c = pawn(g, 0, 2);
                c.setPathIndex(at - 3);
                g.roll();
                Move m = g.legalMoveFor(c);
                check(m != null, "roll " + roll + ": own block does not stop" + on(d));
                checkEq(at - 3 + roll, m.getTo(), "the whole roll" + on(d));
                check(!m.isStoppedByBlock(), "not a partial move" + on(d));
                g.move(m);
                if (roll == 3) {
                    checkEq(3, g.pawnsAt(s).size(), "three green pieces on the square" + on(d));
                    check(g.isBlockedFor(s, g.getPlayers().get(1)), "still a block for blue" + on(d));
                }
            }
        }
    }

    /**
     * Blue block 3 ahead of green, roll 5. With only that piece, it stops right before the block;
     * with another piece that can move the whole roll, stopping early is not offered.
     */
    private static void stopBeforeBlock() {
        for (Difficulty d : Difficulty.values()) {
            Game g = greenBlue(d, 5);
            Square s = sharedOrFail(g, SquareKind.REGULAR, 12);
            put(g, pawn(g, 1, 0), s);
            put(g, pawn(g, 1, 1), s);
            int at = path(g, PlayerColor.GREEN).indexOf(s);
            Pawn a = pawn(g, 0, 0);
            a.setPathIndex(at - 3);
            g.roll();
            Move m = g.legalMoveFor(a);
            check(m != null && m.isStoppedByBlock(), "only piece: partial move offered" + on(d));
            checkEq(at - 1, m.getTo(), "it stops on the square right before the block" + on(d));

            Game g2 = greenBlue(d, 5);
            Square s2 = sharedOrFail(g2, SquareKind.REGULAR, 12);
            put(g2, pawn(g2, 1, 0), s2);
            put(g2, pawn(g2, 1, 1), s2);
            int at2 = path(g2, PlayerColor.GREEN).indexOf(s2);
            pawn(g2, 0, 0).setPathIndex(at2 - 3);
            Pawn free = pawn(g2, 0, 1);
            free.setPathIndex(at2 + 1);
            g2.roll();
            check(g2.legalMoveFor(pawn(g2, 0, 0)) == null, "with a free piece, no partial move" + on(d));
            check(g2.legalMoveFor(free) != null, "the free piece must move" + on(d));
        }
    }

    // ------------------------------------------------------------- FR-36, FR-37

    private static void castleExitEveryLevel() {
        for (Difficulty d : Difficulty.values()) {
            Game g = greenBlue(d, 6);
            Square start = path(g, PlayerColor.GREEN).get(0);
            put(g, pawn(g, 1, 0), start);
            put(g, pawn(g, 1, 1), start);
            g.roll();
            check(g.legalMoveFor(pawn(g, 0, 0)) == null, "blue block on green's #: no exit" + on(d));

            Game g2 = greenBlue(d, 6);
            pawn(g2, 0, 0).setPathIndex(0);
            pawn(g2, 0, 1).setPathIndex(0);
            g2.roll();
            Move exit = g2.legalMoveFor(pawn(g2, 0, 2));
            check(exit != null && exit.getFrom() == Pawn.IN_BASE, "own pieces on #: exit allowed" + on(d));
        }
    }

    /**
     * A blue piece stands alone on a safe square. Green brings two pieces there (no capture, FR-30)
     * and forms a Double Block around it. On blue's turn that piece may still leave, but another
     * blue piece behind may not land on the square.
     */
    private static void leaveSafeAfterBlock() {
        for (Difficulty d : Difficulty.values()) {
            Game g = greenBlue(d, 3, 2);
            Square s = sharedOrFail(g, SquareKind.SAFE, 10);
            Pawn blue = pawn(g, 1, 0);
            put(g, blue, s);
            int gAt = path(g, PlayerColor.GREEN).indexOf(s);
            int bAt = path(g, PlayerColor.BLUE).indexOf(s);
            put(g, pawn(g, 0, 0), s);           // the first green piece is already there
            Pawn second = pawn(g, 0, 1);
            second.setPathIndex(gAt - 3);
            g.roll();                            // green rolls 3
            MoveResult r = g.move(g.legalMoveFor(second));
            checkEq(0, r.getCaptured().size(), "no capture on the safe square" + on(d));
            check(!blue.isInBase(), "the blue piece is still there" + on(d));
            check(g.isBlockedFor(s, g.getPlayers().get(1)), "green formed a block around it" + on(d));

            Pawn behind = pawn(g, 1, 1);
            behind.setPathIndex(bAt - 2);
            checkEq(PlayerColor.BLUE, g.getCurrentPlayer().getColor(), "blue's turn" + on(d));
            g.roll();                            // blue rolls 2
            Move leave = g.legalMoveFor(blue);
            check(leave != null, "the blue piece may leave the blocked square" + on(d));
            checkEq(bAt + 2, leave.getTo(), "it moves the whole roll" + on(d));
            check(g.legalMoveFor(behind) == null, "a blue piece behind cannot land on the block" + on(d));
            g.move(leave);
            check(g.isBlockedFor(s, g.getPlayers().get(1)), "the green block stays after it left" + on(d));
        }
    }

    // ------------------------------------------------------------- FR-29, FR-30

    private static void captureEveryLevel() {
        for (Difficulty d : Difficulty.values()) {
            Game g = greenBlue(d, 4);
            Square s = sharedOrFail(g, SquareKind.REGULAR, 10);
            Pawn blue = pawn(g, 1, 0);
            put(g, blue, s);
            Pawn a = pawn(g, 0, 0);
            a.setPathIndex(path(g, PlayerColor.GREEN).indexOf(s) - 4);
            g.roll();
            MoveResult r = g.move(g.legalMoveFor(a));
            check(blue.isInBase(), "single blue piece captured on a regular square" + on(d));
            checkEq(1, r.getCaptured().size(), "one capture" + on(d));

            Game g2 = greenBlue(d, 4);
            Square safe = sharedOrFail(g2, SquareKind.SAFE, 10);
            Pawn blue2 = pawn(g2, 1, 0);
            put(g2, blue2, safe);
            Pawn a2 = pawn(g2, 0, 0);
            a2.setPathIndex(path(g2, PlayerColor.GREEN).indexOf(safe) - 4);
            g2.roll();
            Move m2 = g2.legalMoveFor(a2);
            check(m2 != null, "green can land on the safe square" + on(d));
            g2.move(m2);
            check(!blue2.isInBase(), "no capture on a safe square" + on(d));
        }
    }
}

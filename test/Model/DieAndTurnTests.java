package Model;

import static Model.ModelTests.check;
import static Model.ModelTests.checkEq;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Tests for the step die (FR-15) and for passing the turn (FR-19).
 * <p>
 * Each test name starts with the FR it checks, as in the acceptance tests. The tests run together
 * with the other model tests ({@link ModelTests}) through build.sh / build.bat.
 *
 * @author Waseem Abu Fanna (Zebra) – Iteration 1, SDP table 2 (FR-15, FR-19)
 */
final class DieAndTurnTests {

    /** Chi-square limit for 5 degrees of freedom (6 faces) at significance level 0.001. */
    private static final double CHI_SQUARE_LIMIT = 20.515;

    private DieAndTurnTests() {
    }

    static void runAll() {
        ModelTests.run("FR-15 step die gives only 1 to 6, and every face comes up", DieAndTurnTests::onlyOneToSix);
        ModelTests.run("FR-15 step die: equal chance for every face (NFR-11: 5 x 60,000 rolls, chi-square)", DieAndTurnTests::equalChance);
        ModelTests.run("FR-15 one roll throws the die once, and the game reports the value", DieAndTurnTests::reportsValue);
        ModelTests.run("FR-19 roll 1 to 5 with all pieces in the castle: turn passes", DieAndTurnTests::passWithAllInCastle);
        ModelTests.run("FR-19 pieces on the board but none can move: turn passes", DieAndTurnTests::passWhenNoPieceCanMove);
        ModelTests.run("FR-19 piece right in front of a Double Block: turn passes", DieAndTurnTests::passWhenBlocked);
        ModelTests.run("FR-19 after a move with 1 to 5 the turn passes", DieAndTurnTests::passAfterMove);
        ModelTests.run("FR-19 a 6 with no move keeps the turn; the next 1 to 5 passes it", DieAndTurnTests::sixKeepsTurn);
        ModelTests.run("FR-19 turn order wraps from the last player to the first", DieAndTurnTests::orderWraps);
        ModelTests.run("FR-19 a player who finished is skipped", DieAndTurnTests::finishedSkipped);
        ModelTests.run("FR-19 the next player's sixes are counted from zero", DieAndTurnTests::sixCountResets);
        ModelTests.run("FR-19 no move before rolling, no second roll before moving", DieAndTurnTests::oneRollOneMove);
    }

    // ------------------------------------------------------------- helpers

    /** A die that returns preset values and counts how many times it was thrown. */
    static final class ScriptedDie implements Die {
        private final int[] values;
        int throwsCount;

        ScriptedDie(int... values) {
            this.values = values;
        }

        @Override
        public int roll() {
            return values[throwsCount++ % values.length];
        }
    }

    /** A game on the easy board, with the players in the given color order (P1, P2, ...). */
    static Game game(Die die, PlayerColor... colors) {
        return game(Difficulty.EASY, die, colors);
    }

    /** A game on the given level (board seed 7), with the players in the given color order. */
    static Game game(Difficulty level, Die die, PlayerColor... colors) {
        Board board = BoardBuilder.build(level, new Random(7));
        List<Player> players = new ArrayList<Player>();
        for (int i = 0; i < colors.length; i++) {
            players.add(new Player("P" + (i + 1), colors[i], i));
        }
        return new Game(board, players, die, () -> 0L);
    }

    private static Game greenBlue(int... rolls) {
        return game(new ScriptedDie(rolls), PlayerColor.GREEN, PlayerColor.BLUE);
    }

    private static PlayerColor turn(Game g) {
        return g.getCurrentPlayer().getColor();
    }

    /** Records the names of the players the turn passed to. */
    private static List<String> recordTurns(Game g) {
        final List<String> turns = new ArrayList<String>();
        g.addListener(new GameListener() {
            @Override
            public void onTurnChanged(Player current) {
                turns.add(current.getName());
            }
        });
        return turns;
    }

    static void expectThrows(Class<? extends Throwable> type, Runnable action, String what) {
        try {
            action.run();
        } catch (Throwable t) {
            check(type.isInstance(t), what + ": expected " + type.getSimpleName() + " but got " + t);
            return;
        }
        throw new AssertionError(what + ": expected " + type.getSimpleName() + ", nothing was thrown");
    }

    // ------------------------------------------------------------- FR-15

    private static void onlyOneToSix() {
        for (int seed = 0; seed < 10; seed++) {
            StepDie die = new StepDie(new Random(seed));
            boolean[] seen = new boolean[7];
            for (int i = 0; i < 6000; i++) {
                int v = die.roll();
                check(v >= 1 && v <= 6, "seed " + seed + ": rolled " + v);
                seen[v] = true;
            }
            for (int face = 1; face <= 6; face++) {
                check(seen[face], "seed " + seed + ": " + face + " never came up");
            }
        }
    }

    /**
     * Equal chance for every result (FR-15), measured as NFR-11 asks: 60,000 rolls for each of 5
     * seeds, and every face within 1 percentage point of one sixth. A chi-square test must also not
     * reject a uniform distribution.
     */
    private static void equalChance() {
        int rolls = 60000;
        double expected = rolls / 6.0;
        for (int seed = 1; seed <= 5; seed++) {
            StepDie die = new StepDie(new Random(seed));
            int[] count = new int[7];
            for (int i = 0; i < rolls; i++) {
                count[die.roll()]++;
            }
            double chiSquare = 0;
            for (int face = 1; face <= 6; face++) {
                double diff = count[face] - expected;
                chiSquare += diff * diff / expected;
                double share = count[face] / (double) rolls;
                check(Math.abs(share - 1 / 6.0) < 0.01, "seed " + seed + ": face " + face + " share " + share);
            }
            check(chiSquare < CHI_SQUARE_LIMIT, "seed " + seed + ": chi-square " + chiSquare + " >= " + CHI_SQUARE_LIMIT);
        }
    }

    private static void reportsValue() {
        ScriptedDie die = new ScriptedDie(4);
        Game g = game(die, PlayerColor.GREEN, PlayerColor.BLUE);
        final List<Integer> shown = new ArrayList<Integer>();
        g.addListener(new GameListener() {
            @Override
            public void onRolled(RollResult result) {
                shown.add(result.getValue());
            }
        });
        checkEq(0, g.getLastRoll(), "no value before the first roll");
        RollResult r = g.roll();
        checkEq(1, die.throwsCount, "die thrown once");
        checkEq(4, r.getValue(), "value in the result");
        checkEq(4, g.getLastRoll(), "last roll");
        checkEq("[4]", shown.toString(), "value sent to the screen");
    }

    // ------------------------------------------------------------- FR-19

    private static void passWithAllInCastle() {
        for (int v = 1; v <= 5; v++) {
            Game g = greenBlue(v);
            List<String> turns = recordTurns(g);
            RollResult r = g.roll();
            check(!r.hasMoves(), v + ": no move");
            check(!r.isRollAgain(), v + ": no extra roll");
            checkEq(PlayerColor.BLUE, turn(g), v + ": turn passed");
            checkEq(GamePhase.ROLL, g.getPhase(), v + ": blue rolls next");
            check(g.getLegalMoves().isEmpty(), v + ": no moves left over");
            checkEq("[P2]", turns.toString(), v + ": screen told once");
        }
    }

    /**
     * All pieces are 2 squares from home: 3 to 5 is too much (an exact roll is needed), and no piece
     * is in the castle.
     */
    private static void passWhenNoPieceCanMove() {
        for (int v = 3; v <= 5; v++) {
            Game g = greenBlue(v);
            int finish = g.getBoard().getFinishIndex();
            for (Pawn p : g.getPlayers().get(0).getPawns()) {
                p.setPathIndex(finish - 2);
            }
            RollResult r = g.roll();
            check(!r.hasMoves(), v + ": no move");
            checkEq(PlayerColor.BLUE, turn(g), v + ": turn passed");
        }
    }

    /**
     * The only piece on the board is on the square just before an opponent's Double Block: it cannot
     * pass the block, and stopping before it would mean not moving at all.
     */
    private static void passWhenBlocked() {
        Game g = greenBlue(4);
        Board b = g.getBoard();
        Square blockSquare = b.getPath(PlayerColor.GREEN).get(10);
        int blueIndex = b.getPath(PlayerColor.BLUE).indexOf(blockSquare);
        g.getPlayers().get(1).getPawns().get(0).setPathIndex(blueIndex);
        g.getPlayers().get(1).getPawns().get(1).setPathIndex(blueIndex);
        g.getPlayers().get(0).getPawns().get(0).setPathIndex(9);
        check(g.isBlockedFor(blockSquare, g.getPlayers().get(0)), "blue block in front of green");
        RollResult r = g.roll();
        check(!r.hasMoves(), "no move");
        checkEq(PlayerColor.BLUE, turn(g), "turn passed");
    }

    private static void passAfterMove() {
        for (int v = 1; v <= 5; v++) {
            Game g = greenBlue(v);
            Pawn p = g.getPlayers().get(0).getPawns().get(0);
            p.setPathIndex(5);
            g.roll();
            MoveResult res = g.move(g.legalMoveFor(p));
            check(!res.isExtraTurn(), v + ": no extra roll");
            checkEq(5 + v, p.getPathIndex(), v + ": moved " + v);
            checkEq(PlayerColor.BLUE, turn(g), v + ": turn passed");
        }
    }

    /**
     * All pieces are 1 square from home: a 6 with no move gives another roll (FR-17), and the 2
     * after it passes the turn.
     */
    private static void sixKeepsTurn() {
        Game g = greenBlue(6, 2);
        int finish = g.getBoard().getFinishIndex();
        for (Pawn p : g.getPlayers().get(0).getPawns()) {
            p.setPathIndex(finish - 1);
        }
        RollResult six = g.roll();
        check(!six.hasMoves() && six.isRollAgain(), "6 without a move rolls again");
        checkEq(PlayerColor.GREEN, turn(g), "turn kept after 6");
        RollResult two = g.roll();
        check(!two.hasMoves() && !two.isRollAgain(), "2 without a move");
        checkEq(PlayerColor.BLUE, turn(g), "turn passed after 2");
    }

    /**
     * 4 players, all in the castle, always rolling 1: the turn follows the entry order and wraps
     * back to the first player (FR-07).
     */
    private static void orderWraps() {
        Game g = game(new ScriptedDie(1), PlayerColor.GREEN, PlayerColor.YELLOW, PlayerColor.BLUE, PlayerColor.RED);
        List<String> turns = recordTurns(g);
        for (int i = 0; i < 5; i++) {
            g.roll();
        }
        checkEq("[P2, P3, P4, P1, P2]", turns.toString(), "turn order");
        checkEq(PlayerColor.YELLOW, turn(g), "back to the second player");
    }

    /**
     * 3 players. Yellow has one piece left, 1 square from home; it finishes, and later turns skip
     * yellow.
     */
    private static void finishedSkipped() {
        Game g = game(new ScriptedDie(1), PlayerColor.GREEN, PlayerColor.YELLOW, PlayerColor.BLUE);
        int finish = g.getBoard().getFinishIndex();
        Player yellow = g.getPlayers().get(1);
        for (Pawn p : yellow.getPawns()) {
            p.setPathIndex(finish);
        }
        yellow.getPawns().get(3).setPathIndex(finish - 1);

        g.roll(); // green: 1, all pieces in the castle
        checkEq(PlayerColor.YELLOW, turn(g), "yellow's turn");
        g.roll();
        MoveResult res = g.move(g.legalMoveFor(yellow.getPawns().get(3)));
        check(res.isPlayerFinished(), "yellow finished");
        check(!res.isGameOver(), "two players still play");
        checkEq(PlayerColor.BLUE, turn(g), "blue after yellow finished");
        g.roll(); // blue: 1, all pieces in the castle
        checkEq(PlayerColor.GREEN, turn(g), "green after blue");
        g.roll(); // green: 1
        checkEq(PlayerColor.BLUE, turn(g), "yellow skipped");
    }

    /**
     * Green rolls two 6s in a row and finishes, so the turn passes while green's count of 6s is at 2.
     * Yellow's first 6 must count as its first, not a third in a row (FR-18 counts each player
     * separately).
     */
    private static void sixCountResets() {
        Game g = game(new ScriptedDie(6), PlayerColor.GREEN, PlayerColor.YELLOW, PlayerColor.BLUE);
        int finish = g.getBoard().getFinishIndex();
        Player green = g.getPlayers().get(0);
        for (Pawn p : green.getPawns()) {
            p.setPathIndex(finish);
        }
        Pawn last = green.getPawns().get(3);
        last.setPathIndex(finish - 12);

        g.roll();
        check(g.move(g.legalMoveFor(last)).isExtraTurn(), "first 6: extra roll");
        g.roll();
        check(g.move(g.legalMoveFor(last)).isPlayerFinished(), "second 6: green finished");
        checkEq(PlayerColor.YELLOW, turn(g), "turn passed to yellow");

        RollResult yellowSix = g.roll();
        check(!yellowSix.isCancelledThirdSix(), "yellow's first 6 is not a third 6");
        check(yellowSix.hasMoves(), "yellow may leave the castle");
        checkEq(PlayerColor.YELLOW, turn(g), "still yellow");
    }

    private static void oneRollOneMove() {
        ScriptedDie die = new ScriptedDie(6);
        Game g = game(die, PlayerColor.GREEN, PlayerColor.BLUE);
        Pawn pawn = g.getPlayers().get(0).getPawns().get(0);
        expectThrows(IllegalStateException.class,
                () -> g.move(new Move(pawn, Pawn.IN_BASE, 0, false)), "move before rolling");
        g.roll();
        checkEq(GamePhase.MOVE, g.getPhase(), "waiting for a move");
        expectThrows(IllegalStateException.class, g::roll, "second roll before moving");
        checkEq(1, die.throwsCount, "die thrown only once");
        checkEq(PlayerColor.GREEN, turn(g), "still green");
    }
}

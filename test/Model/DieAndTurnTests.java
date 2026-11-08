package Model;

import static Model.ModelTests.check;
import static Model.ModelTests.checkEq;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Tests for the step die (FR-15).
 * <p>
 * Each test name starts with the FR it checks, as in the acceptance tests. The tests run together
 * with the other model tests ({@link ModelTests}) through build.sh / build.bat.
 *
 * @author Waseem Abu Fanna (Zebra) – Iteration 1, SDP table 2 (FR-15)
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
    }

    // ------------------------------------------------------------- helpers

    /** A die that returns preset values and counts how many times it was thrown. */
    private static final class ScriptedDie implements Die {
        private final int[] values;
        private int throwsCount;

        ScriptedDie(int... values) {
            this.values = values;
        }

        @Override
        public int roll() {
            return values[throwsCount++ % values.length];
        }
    }

    /** A game on the easy board, with the players in the given color order (P1, P2, ...). */
    private static Game game(Die die, PlayerColor... colors) {
        Board board = BoardBuilder.build(Difficulty.EASY, new Random(7));
        List<Player> players = new ArrayList<Player>();
        for (int i = 0; i < colors.length; i++) {
            players.add(new Player("P" + (i + 1), colors[i], i));
        }
        return new Game(board, players, die, () -> 0L);
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

}

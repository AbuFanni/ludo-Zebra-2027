package Model;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * בדיקות להגרלת משבצות ההגנה (וגם השאלה וההפתעה) על הלוח (FR-10, FR-11).
 * <p>
 * כל בדיקה מסומנת T3.xx כמו ברשומת הבדיקות של משימה 3 בדוח האיטרציה.
 * רצות יחד עם שאר בדיקות המודל ({@link ModelTests}), דרך build.sh / build.bat.
 *
 * @author Nimer Ziad (Zebra) – Iteration 1, SDP table 2
 */
final class BoardSpecialSquaresTests {

    private BoardSpecialSquaresTests() {
    }

    static void runAll() {
        ModelTests.run("T3.01 easy board has exactly 4 protection squares", BoardSpecialSquaresTests::easyFour);
        ModelTests.run("T3.02 medium 8 and hard 12 protection squares", BoardSpecialSquaresTests::mediumHard);
        ModelTests.run("T3.03 question and surprise counts per level (Table 4)", BoardSpecialSquaresTests::questionSurprise);
        ModelTests.run("T3.04 protection squares only on the shared path", BoardSpecialSquaresTests::onShared);
        ModelTests.run("T3.05 never on start, arrow, home lane or finish", BoardSpecialSquaresTests::neverOnFixed);
        ModelTests.run("T3.06 no two special squares on the same square", BoardSpecialSquaresTests::noOverlap);
        ModelTests.run("T3.07 adjacent protection squares are allowed", BoardSpecialSquaresTests::adjacentAllowed);
        ModelTests.run("T3.08 new placement in every game", BoardSpecialSquaresTests::newEachGame);
        ModelTests.run("T3.09 same random seed gives the same placement", BoardSpecialSquaresTests::sameSeed);
        ModelTests.run("T3.10 every free regular square can be chosen", BoardSpecialSquaresTests::everyFreeSquare);
        ModelTests.run("T3.11 every protection square is on at least 3 players' paths", BoardSpecialSquaresTests::onEveryPath);
        ModelTests.run("T3.12 protection squares are protected, regular squares are not", BoardSpecialSquaresTests::isSafe);
    }

    // ------------------------------------------------------------- עזר

    private static Board easy(long seed) {
        return BoardBuilder.build(Difficulty.EASY, new Random(seed));
    }

    private static Set<Position> positionsOf(Board b, SquareKind kind) {
        Set<Position> result = new HashSet<Position>();
        for (Square s : b.getSquares()) {
            if (s.getKind() == kind) {
                result.add(s.getPosition());
            }
        }
        return result;
    }

    /** משבצות המסלול המשותף שאינן התחלה או חץ: המקומות שבהם מותר להגריל משבצת מיוחדת. */
    private static Set<Position> freeShared(Board b) {
        Set<Position> result = new HashSet<Position>();
        for (Square s : b.getSharedTrack()) {
            if (s.getKind() != SquareKind.START && s.getKind() != SquareKind.ARROW) {
                result.add(s.getPosition());
            }
        }
        return result;
    }

    // ------------------------------------------------------------- בדיקות

    private static void easyFour() {
        for (long seed = 0; seed < 200; seed++) {
            ModelTests.checkEq(4, easy(seed).count(SquareKind.SAFE), "easy, seed " + seed);
        }
        ModelTests.checkEq(4, Difficulty.EASY.getSafeSquares(), "easy count in Difficulty");
    }

    private static void mediumHard() {
        for (long seed = 0; seed < 100; seed++) {
            ModelTests.checkEq(8, BoardBuilder.build(Difficulty.MEDIUM, new Random(seed)).count(SquareKind.SAFE),
                    "medium, seed " + seed);
            ModelTests.checkEq(12, BoardBuilder.build(Difficulty.HARD, new Random(seed)).count(SquareKind.SAFE),
                    "hard, seed " + seed);
        }
    }

    private static void questionSurprise() {
        int[][] expected = {{0, 0}, {6, 2}, {9, 3}}; // שאלות, הפתעות: קל, בינוני, קשה
        for (Difficulty d : Difficulty.values()) {
            for (long seed = 0; seed < 50; seed++) {
                Board b = BoardBuilder.build(d, new Random(seed));
                ModelTests.checkEq(expected[d.ordinal()][0], b.count(SquareKind.QUESTION), d + " questions");
                ModelTests.checkEq(expected[d.ordinal()][1], b.count(SquareKind.SURPRISE), d + " surprises");
            }
        }
    }

    private static void onShared() {
        for (Difficulty d : Difficulty.values()) {
            for (long seed = 0; seed < 100; seed++) {
                Board b = BoardBuilder.build(d, new Random(seed));
                Set<Position> shared = new HashSet<Position>();
                for (Square s : b.getSharedTrack()) {
                    shared.add(s.getPosition());
                }
                for (Position p : positionsOf(b, SquareKind.SAFE)) {
                    ModelTests.check(shared.contains(p), d + " seed " + seed + ": " + p + " not on shared path");
                }
            }
        }
    }

    private static void neverOnFixed() {
        for (Difficulty d : Difficulty.values()) {
            Board first = BoardBuilder.build(d, new Random(0));
            Set<Position> fixed = new HashSet<Position>();
            fixed.addAll(positionsOf(first, SquareKind.START));
            fixed.addAll(positionsOf(first, SquareKind.ARROW));
            fixed.addAll(positionsOf(first, SquareKind.HOME_PATH));
            fixed.addAll(positionsOf(first, SquareKind.FINISH));
            for (long seed = 0; seed < 100; seed++) {
                Board b = BoardBuilder.build(d, new Random(seed));
                for (Position p : positionsOf(b, SquareKind.SAFE)) {
                    ModelTests.check(!fixed.contains(p), d + " seed " + seed + ": protection on fixed square " + p);
                }
                ModelTests.checkEq(positionsOf(first, SquareKind.START), positionsOf(b, SquareKind.START),
                        d + " start squares do not move");
            }
        }
    }

    private static void noOverlap() {
        for (Difficulty d : Difficulty.values()) {
            for (long seed = 0; seed < 100; seed++) {
                Board b = BoardBuilder.build(d, new Random(seed));
                Set<Position> all = new HashSet<Position>();
                all.addAll(positionsOf(b, SquareKind.SAFE));
                all.addAll(positionsOf(b, SquareKind.QUESTION));
                all.addAll(positionsOf(b, SquareKind.SURPRISE));
                int expected = d.getSafeSquares() + 3 * d.getQuestionsPerLevel() + d.getSurpriseSquares();
                ModelTests.checkEq(expected, all.size(), d + " seed " + seed + " distinct special squares");
            }
        }
    }

    private static void adjacentAllowed() {
        for (long seed = 0; seed < 500; seed++) {
            List<Square> path = easy(seed).getPath(PlayerColor.GREEN);
            for (int i = 1; i < path.size(); i++) {
                if (path.get(i).getKind() == SquareKind.SAFE && path.get(i - 1).getKind() == SquareKind.SAFE) {
                    return; // נמצא לוח עם שתי משבצות הגנה צמודות
                }
            }
        }
        throw new AssertionError("no board with two adjacent protection squares in 500 games");
    }

    private static void newEachGame() {
        Set<Set<Position>> placements = new HashSet<Set<Position>>();
        for (long seed = 0; seed < 100; seed++) {
            placements.add(positionsOf(easy(seed), SquareKind.SAFE));
        }
        ModelTests.check(placements.size() >= 95, "only " + placements.size() + " different placements in 100 games");

        // כמו GameController: אותו Random לכל המשחקים, ובכל משחק הגרלה חדשה
        Random shared = new Random(42);
        Set<Position> game1 = positionsOf(BoardBuilder.build(Difficulty.EASY, shared), SquareKind.SAFE);
        Set<Position> game2 = positionsOf(BoardBuilder.build(Difficulty.EASY, shared), SquareKind.SAFE);
        ModelTests.check(!game1.equals(game2), "two games in a row got the same placement");
    }

    private static void sameSeed() {
        for (Difficulty d : Difficulty.values()) {
            Board a = BoardBuilder.build(d, new Random(123));
            Board b = BoardBuilder.build(d, new Random(123));
            ModelTests.checkEq(positionsOf(a, SquareKind.SAFE), positionsOf(b, SquareKind.SAFE), d + " protection");
            ModelTests.checkEq(positionsOf(a, SquareKind.QUESTION), positionsOf(b, SquareKind.QUESTION), d + " questions");
        }
    }

    private static void everyFreeSquare() {
        Set<Position> free = freeShared(easy(0));
        ModelTests.checkEq(52 - 4 - 4, free.size(), "easy: 52 path squares minus 4 starts and 4 arrows");
        Set<Position> chosen = new HashSet<Position>();
        for (long seed = 0; seed < 2000; seed++) {
            chosen.addAll(positionsOf(easy(seed), SquareKind.SAFE));
        }
        ModelTests.checkEq(free, chosen, "squares chosen at least once in 2000 games");
    }

    /**
     * כל משבצת הגנה נמצאת על המסלול של לפחות 3 מתוך 4 השחקנים. שחקן מדלג רק על המשבצת שמאחורי
     * ה-# שלו (הוא יוצא מהטירה ישר ל-#), ולכן משבצת הגנה שם רלוונטית לשלושת האחרים.
     */
    private static void onEveryPath() {
        for (long seed = 0; seed < 200; seed++) {
            Board b = easy(seed);
            for (Position p : positionsOf(b, SquareKind.SAFE)) {
                Square s = b.getSquare(p);
                int players = 0;
                for (PlayerColor c : PlayerColor.values()) {
                    if (b.getPath(c).contains(s)) {
                        players++;
                    }
                }
                ModelTests.check(players >= 3, "seed " + seed + ": only " + players + " players pass " + p);
            }
        }
    }

    private static void isSafe() {
        Board b = easy(3);
        for (Square s : b.getSquares()) {
            if (s.getKind() == SquareKind.SAFE) {
                ModelTests.check(s.isSafe(), "protection square " + s.getPosition() + " is not safe");
            }
            if (s.getKind() == SquareKind.REGULAR) {
                ModelTests.check(!s.isSafe(), "regular square " + s.getPosition() + " is safe");
            }
        }
    }
}

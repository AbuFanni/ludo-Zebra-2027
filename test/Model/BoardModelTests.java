package Model;

import static Model.ModelTests.check;
import static Model.ModelTests.checkEq;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * בדיקות למודל הלוח לכל רמה (FR-09): הלוח נבנה לפי טבלה 4 ב-SRS, עם משבצות התחלה, חצים ומסלולי בית
 * במקום קבוע, וכל הכלים בטירות בתחילת המשחק.
 * <p>
 * כל בדיקה מסומנת T7.xx כמו ברשומת הבדיקות. רצות יחד עם שאר בדיקות המודל ({@link ModelTests}),
 * דרך build.sh / build.bat.
 *
 * @author Nimer Ziad (Zebra) – Iteration 1, SDP table 2 (FR-09)
 */
final class BoardModelTests {

    /** Squares in each home lane, counting the finish (SRS Table 1: "6 squares in the player's color"). */
    private static final int HOME_LANE = 6;

    /** Path length of every color: easy 51 + 6 home; medium and hard also go around the outer rings. */
    private static final int[] PATH_LENGTH = {57, 116, 188};

    private BoardModelTests() {
    }

    static void runAll() {
        ModelTests.run("T7.01 FR-09 every level matches SRS Table 4 row by row", BoardModelTests::table4);
        ModelTests.run("T7.02 FR-09 every color has its own home lane of 6 squares ending at the finish", BoardModelTests::homeLanes);
        ModelTests.run("T7.03 FR-09 one # per color, first on its path, next to its own castle", BoardModelTests::startSquares);
        ModelTests.run("T7.04 FR-09 every color has 1/2/3 arrows, and its last arrow leads into its home lane", BoardModelTests::arrows);
        ModelTests.run("T7.05 FR-09 path length 57/116/188, the same for every color", BoardModelTests::pathLengths);
        ModelTests.run("T7.06 FR-09 #, arrows, home lanes and finish never move between games", BoardModelTests::fixedPlaces);
        ModelTests.run("T7.07 FR-09 every color's path is the green path turned by quarter turns", BoardModelTests::sameBoardForAll);
        ModelTests.run("T7.08 FR-09 at the start all pieces are in their castles", BoardModelTests::allInCastle);
        ModelTests.run("T7.09 FR-09 the board has only the shared track and the 4 home lanes", BoardModelTests::noExtraSquares);
    }

    // ------------------------------------------------------------- עזר

    private static Board board(Difficulty d) {
        return BoardBuilder.build(d, new Random(1));
    }

    private static int count(Board b, SquareKind kind, PlayerColor owner) {
        int n = 0;
        for (Square s : b.getSquares()) {
            if (s.getKind() == kind && s.getOwner() == owner) {
                n++;
            }
        }
        return n;
    }

    /** @return the positions of the squares on this color's path, in order */
    private static List<Position> positions(Board b, PlayerColor c) {
        List<Position> r = new ArrayList<Position>();
        for (Square s : b.getPath(c)) {
            r.add(s.getPosition());
        }
        return r;
    }

    // ------------------------------------------------------------- טבלה 4

    /**
     * SRS Table 4, column by column:
     * paths 52 / +64 / +72; protection and arrows 4/4, 8/8, 12/12; question and surprise none, 6 (2 per
     * question level) / 2, 9 (3 per level) / 3; 4 start squares. Grid 15, 17, 19 (one more ring each level).
     */
    private static void table4() {
        int[] grid = {15, 17, 19};
        int[] shared = {52, 52 + 64, 52 + 64 + 72};
        int[] protection = {4, 8, 12};
        int[] arrows = {4, 8, 12};
        int[] questions = {0, 6, 9};
        int[] surprises = {0, 2, 3};
        for (Difficulty d : Difficulty.values()) {
            int i = d.ordinal();
            for (int seed = 0; seed < 10; seed++) {
                Board b = BoardBuilder.build(d, new Random(seed));
                checkEq(d, b.getDifficulty(), d + " board level");
                checkEq(grid[i], b.getSize(), d + " grid");
                checkEq(shared[i], b.getSharedTrack().size(), d + " shared track");
                checkEq(protection[i], b.count(SquareKind.SAFE), d + " protection squares");
                checkEq(arrows[i], b.count(SquareKind.ARROW), d + " arrows");
                checkEq(questions[i], b.count(SquareKind.QUESTION), d + " question squares");
                checkEq(surprises[i], b.count(SquareKind.SURPRISE), d + " surprise squares");
                checkEq(4, b.count(SquareKind.START), d + " start squares");
                int[] perLevel = new int[QuestionLevel.values().length];
                for (Square s : b.getSquares()) {
                    if (s instanceof QuestionSquare) {
                        perLevel[((QuestionSquare) s).getLevel().ordinal()]++;
                    }
                }
                for (int level = 0; level < perLevel.length; level++) {
                    checkEq(questions[i] / 3, perLevel[level], d + " questions of level " + level);
                }
            }
        }
        // the rings, from the outside in: 72 (hard), 64 (medium and hard), 52 (every level)
        checkEq(52, board(Difficulty.EASY).getRingLength(0), "easy track");
        checkEq(64, board(Difficulty.MEDIUM).getRingLength(0), "medium outer ring");
        checkEq(52, board(Difficulty.MEDIUM).getRingLength(1), "medium inner track");
        checkEq(72, board(Difficulty.HARD).getRingLength(0), "hard outer ring");
        checkEq(64, board(Difficulty.HARD).getRingLength(1), "hard middle ring");
        checkEq(52, board(Difficulty.HARD).getRingLength(2), "hard inner track");
    }

    // ------------------------------------------------------------- מסלולי בית, התחלה, חצים

    private static void homeLanes() {
        for (Difficulty d : Difficulty.values()) {
            Board b = board(d);
            Set<Square> shared = new HashSet<Square>(b.getSharedTrack());
            for (PlayerColor c : PlayerColor.values()) {
                List<Square> path = b.getPath(c);
                List<Square> lane = path.subList(path.size() - HOME_LANE, path.size());
                for (int i = 0; i < HOME_LANE; i++) {
                    Square s = lane.get(i);
                    SquareKind expected = (i == HOME_LANE - 1) ? SquareKind.FINISH : SquareKind.HOME_PATH;
                    checkEq(expected, s.getKind(), d + " " + c + " home lane square " + i);
                    checkEq(c, s.getOwner(), d + " " + c + " home lane owner");
                    check(s.isPrivate(), d + " " + c + " home lane square " + i + " must be private");
                    check(!shared.contains(s), d + " " + c + " home lane square " + i + " is on the shared track");
                }
                checkEq(path.size() - 1, b.getFinishIndex(), d + " " + c + " finish index");
                checkEq(HOME_LANE - 1, count(b, SquareKind.HOME_PATH, c), d + " " + c + " home squares");
                checkEq(1, count(b, SquareKind.FINISH, c), d + " " + c + " finish squares");
            }
        }
    }

    /** On medium and hard the # is on the outer corner by the castle (SRS Table 4: "4, in the corner by the castle"). */
    private static void startSquares() {
        for (Difficulty d : Difficulty.values()) {
            Board b = board(d);
            int n = b.getSize();
            double middle = (n - 1) / 2.0;
            for (PlayerColor c : PlayerColor.values()) {
                Square start = b.getPath(c).get(0);
                checkEq(SquareKind.START, start.getKind(), d + " " + c + " path starts at #");
                checkEq(c, start.getOwner(), d + " " + c + " # owner");
                checkEq(1, count(b, SquareKind.START, c), d + " " + c + " one #");
                Position p = start.getPosition();
                Position castle = b.getBaseOrigin(c);
                check((p.getRow() < middle) == (castle.getRow() < middle)
                        && (p.getCol() < middle) == (castle.getCol() < middle),
                        d + " " + c + " # " + p + " is not on the side of its castle " + castle);
                if (d != Difficulty.EASY) {
                    check((p.getRow() == 0 || p.getRow() == n - 1) && (p.getCol() == 0 || p.getCol() == n - 1),
                            d + " " + c + " # " + p + " is not on a corner");
                }
            }
        }
    }

    private static void arrows() {
        for (Difficulty d : Difficulty.values()) {
            Board b = board(d);
            int perColor = d.getOuterRings() + 1;
            for (PlayerColor c : PlayerColor.values()) {
                checkEq(perColor, count(b, SquareKind.ARROW, c), d + " " + c + " arrows");
                List<Square> path = b.getPath(c);
                int onPath = 0;
                int last = -1;
                for (int i = 0; i < path.size(); i++) {
                    if (path.get(i).getKind() == SquareKind.ARROW && path.get(i).getOwner() == c) {
                        onPath++;
                        last = i;
                    }
                }
                checkEq(perColor, onPath, d + " " + c + " own arrows on own path");
                checkEq(SquareKind.HOME_PATH, path.get(last + 1).getKind(), d + " " + c + " last arrow leads home");
                checkEq(path.size() - HOME_LANE, last + 1, d + " " + c + " home lane right after the arrow");
            }
        }
    }

    private static void pathLengths() {
        for (Difficulty d : Difficulty.values()) {
            Board b = board(d);
            for (PlayerColor c : PlayerColor.values()) {
                checkEq(PATH_LENGTH[d.ordinal()], b.getPath(c).size(), d + " " + c + " path length");
                Set<Square> unique = new HashSet<Square>(b.getPath(c));
                checkEq(b.getPath(c).size(), unique.size(), d + " " + c + " path has no repeated square");
            }
        }
    }

    // ------------------------------------------------------------- מקום קבוע וסימטריה

    /** A text of every fixed square (kind, owner, place) and of every path. Random special squares are left out. */
    private static String fixedLayout(Board b) {
        StringBuilder sb = new StringBuilder();
        List<String> fixed = new ArrayList<String>();
        for (Square s : b.getSquares()) {
            SquareKind k = s.getKind();
            if (k == SquareKind.START || k == SquareKind.ARROW || k == SquareKind.HOME_PATH || k == SquareKind.FINISH) {
                fixed.add(k + "/" + s.getOwner() + "@" + s.getPosition());
            }
        }
        fixed.sort(null);
        sb.append(fixed);
        for (PlayerColor c : PlayerColor.values()) {
            sb.append('\n').append(c.name()).append(positions(b, c)).append(b.getBaseOrigin(c));
        }
        return sb.toString();
    }

    /**
     * 30 boards per level from widely spread seeds. Small seeds next to each other (0, 1, 2 ...) give
     * almost the same first random number in java.util.Random, so they could hide a square placed at random.
     */
    private static void fixedPlaces() {
        Random seeds = new Random(2026);
        for (Difficulty d : Difficulty.values()) {
            String first = fixedLayout(BoardBuilder.build(d, new Random(seeds.nextLong())));
            for (int i = 1; i <= 30; i++) {
                long seed = seeds.nextLong();
                checkEq(first, fixedLayout(BoardBuilder.build(d, new Random(seed))), d + " seed " + seed);
            }
        }
    }

    /** Each other color's path is the green path turned a different number of quarter turns. */
    private static void sameBoardForAll() {
        for (Difficulty d : Difficulty.values()) {
            Board b = board(d);
            int n = b.getSize();
            List<Square> green = b.getPath(PlayerColor.GREEN);
            Set<Integer> turnsUsed = new HashSet<Integer>();
            for (PlayerColor c : PlayerColor.values()) {
                List<Position> target = positions(b, c);
                int found = -1;
                for (int turns = 0; turns < 4 && found < 0; turns++) {
                    List<Position> turned = new ArrayList<Position>();
                    for (Square s : green) {
                        turned.add(s.getPosition().rotate(n, turns));
                    }
                    if (turned.equals(target)) {
                        found = turns;
                    }
                }
                check(found >= 0, d + " " + c + " path is not the green path turned");
                turnsUsed.add(found);
                List<Square> path = b.getPath(c);
                for (int i = 0; i < path.size(); i++) {
                    SquareKind gk = green.get(i).getKind();
                    if (gk == SquareKind.START || gk == SquareKind.ARROW || gk == SquareKind.HOME_PATH
                            || gk == SquareKind.FINISH) {
                        checkEq(gk, path.get(i).getKind(), d + " " + c + " square " + i + " kind");
                    }
                }
            }
            checkEq(4, turnsUsed.size(), d + " each color has its own quarter of the board");
        }
    }

    // ------------------------------------------------------------- כלים בטירה, משבצות מיותרות

    private static void allInCastle() {
        List<PlayerColor> colors = Arrays.asList(PlayerColor.values());
        for (Difficulty d : Difficulty.values()) {
            for (int count = 2; count <= 4; count++) {
                List<Player> players = new ArrayList<Player>();
                for (int i = 0; i < count; i++) {
                    players.add(new Player("P" + (i + 1), colors.get(i), i));
                }
                Game g = new Game(board(d), players, () -> 1, () -> 0L);
                for (Player p : g.getPlayers()) {
                    checkEq(Player.PAWNS, p.getPawns().size(), d + " " + p.getName() + " pieces");
                    for (Pawn pawn : p.getPawns()) {
                        check(pawn.isInBase(), d + " " + pawn + " should be in the castle");
                        check(g.squareOf(pawn) == null, d + " " + pawn + " should not be on the board");
                    }
                }
                for (Square s : g.getBoard().getSquares()) {
                    check(g.pawnsAt(s).isEmpty(), d + " square " + s.getPosition() + " should be empty");
                }
            }
        }
    }

    private static void noExtraSquares() {
        for (Difficulty d : Difficulty.values()) {
            Board b = board(d);
            Set<Square> expected = new HashSet<Square>(b.getSharedTrack());
            for (PlayerColor c : PlayerColor.values()) {
                List<Square> path = b.getPath(c);
                expected.addAll(path.subList(path.size() - HOME_LANE, path.size()));
                check(b.getSquares().containsAll(path), d + " " + c + " path uses a square that is not on the board");
            }
            checkEq(b.getSharedTrack().size() + 4 * HOME_LANE, expected.size(), d + " expected squares");
            checkEq(expected.size(), b.getSquares().size(), d + " squares on the board");
            check(expected.containsAll(b.getSquares()), d + " board has a square outside the tracks");
        }
    }
}

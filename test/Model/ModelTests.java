package Model;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * בדיקות יחידה למודל, בלי ספריות חיצוניות: מריצים עם {@code java Model.ModelTests}
 * (או דרך build.sh). כל בדיקה היא מתודה, וכישלון מדפיס מה ציפינו ומה קיבלנו.
 * <p>
 * אפשר להעביר את הבדיקות ל-JUnit 5 בלי לשנות את הלוגיקה: כל {@code check} הופך ל-{@code assertEquals}.
 */
public final class ModelTests {

    private static int passed;
    private static final List<String> failures = new ArrayList<String>();

    public static void main(String[] args) throws Exception {
        run("board sizes and track lengths", ModelTests::boardSizes);
        run("special squares per level", ModelTests::specialSquares);
        run("special squares only on shared track", ModelTests::specialsOnSharedTrack);
        run("paths rotate per color", ModelTests::pathsRotate);
        run("medium path: outer ring -> arrow -> inner", ModelTests::mediumPathUsesArrows);
        run("enter only with 6", ModelTests::enterOnlyWithSix);
        run("six gives extra roll", ModelTests::sixGivesExtraRoll);
        run("third six cancelled", ModelTests::thirdSixCancelled);
        run("six without move still rolls again", ModelTests::sixWithoutMoveRollsAgain);
        run("capture sends pawn to base", ModelTests::captureSendsToBase);
        run("no capture on safe square", ModelTests::noCaptureOnSafe);
        run("double block stops opponent", ModelTests::doubleBlockStops);
        run("full move preferred over partial", ModelTests::fullMovePreferred);
        run("own pawns pass own block", ModelTests::ownPawnsPassBlock);
        run("block on start square stops leaving base", ModelTests::blockOnStart);
        run("exact roll needed for finish", ModelTests::exactFinish);
        run("game over and ranking", ModelTests::gameOverRanking);
        run("random full games always finish", ModelTests::randomGamesFinish);
        run("setup validation", ModelTests::setupValidation);
        run("history csv format", ModelTests::historyCsv);
        SetupValidationTests.runAll();
        BoardSpecialSquaresTests.runAll();

        System.out.println();
        System.out.println(passed + " passed, " + failures.size() + " failed");
        for (String f : failures) {
            System.out.println("  FAIL " + f);
        }
        if (!failures.isEmpty()) {
            System.exit(1);
        }
    }

    // ------------------------------------------------------------- framework

    interface TestBody {
        void run() throws Exception;
    }

    static void run(String name, TestBody body) {
        try {
            body.run();
            passed++;
            System.out.println("  ok   " + name);
        } catch (Throwable t) {
            failures.add(name + ": " + t.getMessage());
            System.out.println("  FAIL " + name + ": " + t.getMessage());
        }
    }

    static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    static void checkEq(Object expected, Object actual, String what) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError(what + ": expected <" + expected + "> but was <" + actual + ">");
        }
    }

    /** קובייה שמחזירה תוצאות קבועות מראש. */
    private static final class ScriptedDie implements Die {
        private final int[] values;
        private int i;

        ScriptedDie(int... values) {
            this.values = values;
        }

        @Override
        public int roll() {
            return values[i++ % values.length];
        }
    }

    private static Game game(Difficulty d, Die die, PlayerColor... colors) {
        Board board = BoardBuilder.build(d, new Random(7));
        List<Player> players = new ArrayList<Player>();
        for (int i = 0; i < colors.length; i++) {
            players.add(new Player("P" + (i + 1), colors[i], i));
        }
        return new Game(board, players, die, () -> 0L);
    }

    /** האינדקס הראשון במסלול של הצבע, אחרי from, שהמשבצת בו מהסוג המבוקש ומשותפת. */
    private static int firstIndexOf(Board b, PlayerColor c, SquareKind kind, int from) {
        List<Square> path = b.getPath(c);
        for (int i = from; i < path.size(); i++) {
            if (path.get(i).getKind() == kind) {
                return i;
            }
        }
        throw new AssertionError("no " + kind + " after " + from);
    }

    /** האינדקס הבא במסלול של משבצת חץ בצבע של המסלול (חצים של צבעים אחרים הם משבצות רגילות). */
    private static int ownArrow(Board b, PlayerColor c, int from) {
        List<Square> path = b.getPath(c);
        for (int i = from; i < path.size(); i++) {
            if (path.get(i).getKind() == SquareKind.ARROW && path.get(i).getOwner() == c) {
                return i;
            }
        }
        throw new AssertionError("no own arrow after " + from);
    }

    /** האינדקס במסלול של other שבו נמצאת המשבצת path(c)[index]. */
    private static int indexOnPath(Board b, PlayerColor other, Square sq) {
        return b.getPath(other).indexOf(sq);
    }

    // ------------------------------------------------------------- board

    private static void boardSizes() {
        Board easy = BoardBuilder.build(Difficulty.EASY, new Random(1));
        Board medium = BoardBuilder.build(Difficulty.MEDIUM, new Random(1));
        Board hard = BoardBuilder.build(Difficulty.HARD, new Random(1));
        checkEq(15, easy.getSize(), "easy size");
        checkEq(17, medium.getSize(), "medium size");
        checkEq(19, hard.getSize(), "hard size");
        checkEq(52, easy.getSharedTrack().size(), "easy track");
        checkEq(52 + 64, medium.getSharedTrack().size(), "medium tracks");
        checkEq(52 + 64 + 72, hard.getSharedTrack().size(), "hard tracks");
        checkEq(64, medium.getRingLength(0), "medium outer ring");
        checkEq(72, hard.getRingLength(0), "hard outer ring");
        checkEq(52, hard.getRingLength(2), "hard inner track");
        // מסלול קל: 51 משבצות + 5 בית + סיום
        checkEq(57, easy.getPath(PlayerColor.GREEN).size(), "easy path length");
        for (Board b : Arrays.asList(easy, medium, hard)) {
            List<Square> path = b.getPath(PlayerColor.RED);
            checkEq(SquareKind.START, path.get(0).getKind(), "path starts at #");
            checkEq(SquareKind.FINISH, path.get(path.size() - 1).getKind(), "path ends at finish");
            Set<Square> unique = new HashSet<Square>(path);
            checkEq(path.size(), unique.size(), "path has no repeated square");
            for (int i = 1; i < path.size(); i++) {
                Position a = path.get(i - 1).getPosition();
                Position c = path.get(i).getPosition();
                int dist = Math.abs(a.getRow() - c.getRow()) + Math.abs(a.getCol() - c.getCol());
                // מעבר בין משבצות סמוכות, או אלכסון בפינת הצלב של הלוח הרגיל
                check(dist == 1 || (dist == 2 && a.getRow() != c.getRow() && a.getCol() != c.getCol()),
                        "path step " + a + "->" + c + " is not adjacent");
            }
        }
    }

    private static void specialSquares() {
        for (Difficulty d : Difficulty.values()) {
            for (int seed = 0; seed < 20; seed++) {
                Board b = BoardBuilder.build(d, new Random(seed));
                checkEq(d.getSafeSquares(), b.count(SquareKind.SAFE), d + " safe");
                checkEq(3 * d.getQuestionsPerLevel(), b.count(SquareKind.QUESTION), d + " questions");
                checkEq(d.getSurpriseSquares(), b.count(SquareKind.SURPRISE), d + " surprises");
                checkEq(4, b.count(SquareKind.START), d + " starts");
                checkEq(4 * (d.getOuterRings() + 1), b.count(SquareKind.ARROW), d + " arrows");
                checkEq(20, b.count(SquareKind.HOME_PATH), d + " home path");
                checkEq(4, b.count(SquareKind.FINISH), d + " finish");
            }
        }
        Board hard = BoardBuilder.build(Difficulty.HARD, new Random(3));
        int[] perLevel = new int[3];
        for (Square s : hard.getSquares()) {
            if (s instanceof QuestionSquare) {
                perLevel[((QuestionSquare) s).getLevel().ordinal()]++;
            }
        }
        checkEq("[3, 3, 3]", Arrays.toString(perLevel), "hard questions per level");
    }

    private static void specialsOnSharedTrack() {
        Board b = BoardBuilder.build(Difficulty.HARD, new Random(11));
        Set<Square> shared = new HashSet<Square>(b.getSharedTrack());
        for (Square s : b.getSquares()) {
            SquareKind k = s.getKind();
            if (k == SquareKind.SAFE || k == SquareKind.QUESTION || k == SquareKind.SURPRISE) {
                check(shared.contains(s), k + " at " + s.getPosition() + " is not on a shared track");
            }
        }
    }

    private static void pathsRotate() {
        Board b = BoardBuilder.build(Difficulty.EASY, new Random(1));
        checkEq(new Position(6, 1), b.getPath(PlayerColor.GREEN).get(0).getPosition(), "green #");
        checkEq(new Position(1, 8), b.getPath(PlayerColor.YELLOW).get(0).getPosition(), "yellow #");
        checkEq(new Position(8, 13), b.getPath(PlayerColor.BLUE).get(0).getPosition(), "blue #");
        checkEq(new Position(13, 6), b.getPath(PlayerColor.RED).get(0).getPosition(), "red #");
        checkEq(new Position(0, 0), b.getBaseOrigin(PlayerColor.GREEN), "green base");
        checkEq(new Position(9, 9), b.getBaseOrigin(PlayerColor.BLUE), "blue base");
    }

    private static void mediumPathUsesArrows() {
        Board b = BoardBuilder.build(Difficulty.MEDIUM, new Random(1));
        List<Square> path = b.getPath(PlayerColor.GREEN);
        checkEq(new Position(0, 0), path.get(0).getPosition(), "green # at outer corner");
        int arrow = ownArrow(b, PlayerColor.GREEN, 1);
        checkEq(new Position(7, 0), path.get(arrow).getPosition(), "outer arrow");
        checkEq(PlayerColor.GREEN, path.get(arrow).getOwner(), "arrow owner");
        checkEq(new Position(7, 1), path.get(arrow + 1).getPosition(), "enters inner board");
        Board hard = BoardBuilder.build(Difficulty.HARD, new Random(1));
        List<Square> hp = hard.getPath(PlayerColor.GREEN);
        int a1 = ownArrow(hard, PlayerColor.GREEN, 1);
        int a2 = ownArrow(hard, PlayerColor.GREEN, a1 + 1);
        checkEq(new Position(7, 0), hp.get(a1).getPosition(), "hard ring-1 arrow");
        checkEq(new Position(8, 1), hp.get(a2).getPosition(), "hard ring-2 arrow");
        checkEq(new Position(8, 2), hp.get(a2 + 1).getPosition(), "hard enters inner board");
    }

    // ------------------------------------------------------------- rules

    private static void enterOnlyWithSix() {
        Game g = game(Difficulty.EASY, new ScriptedDie(5), PlayerColor.GREEN, PlayerColor.BLUE);
        RollResult r = g.roll();
        check(!r.hasMoves(), "5 from base should have no move");
        checkEq(PlayerColor.BLUE, g.getCurrentPlayer().getColor(), "turn passed");

        Game g6 = game(Difficulty.EASY, new ScriptedDie(6), PlayerColor.GREEN, PlayerColor.BLUE);
        RollResult r6 = g6.roll();
        check(r6.hasMoves(), "6 should allow leaving base");
        checkEq(1, g6.getLegalMoves().size(), "one base move for all base pawns");
        Move m = g6.getLegalMoves().get(0);
        checkEq(0, m.getTo(), "goes to start square");
        g6.move(m);
        checkEq(0, m.getPawn().getPathIndex(), "pawn on #");
    }

    private static void sixGivesExtraRoll() {
        Game g = game(Difficulty.EASY, new ScriptedDie(6, 3), PlayerColor.GREEN, PlayerColor.BLUE);
        g.roll();
        MoveResult res = g.move(g.getLegalMoves().get(0));
        check(res.isExtraTurn(), "6 gives an extra roll");
        checkEq(PlayerColor.GREEN, g.getCurrentPlayer().getColor(), "same player");
        g.roll();
        g.move(g.getLegalMoves().get(0));
        checkEq(PlayerColor.BLUE, g.getCurrentPlayer().getColor(), "turn passes after 3");
    }

    private static void thirdSixCancelled() {
        Game g = game(Difficulty.EASY, new ScriptedDie(6, 6, 6), PlayerColor.GREEN, PlayerColor.BLUE);
        g.roll();
        g.move(g.getLegalMoves().get(0));
        g.roll();
        g.move(g.getLegalMoves().get(0));
        RollResult third = g.roll();
        check(third.isCancelledThirdSix(), "third six is cancelled");
        checkEq(PlayerColor.BLUE, g.getCurrentPlayer().getColor(), "turn passed");
    }

    private static void sixWithoutMoveRollsAgain() {
        Game g = game(Difficulty.EASY, new ScriptedDie(6), PlayerColor.GREEN, PlayerColor.BLUE);
        // כל הכלים של הירוק במרחק 3 מהבית: 6 גדול מדי
        int finish = g.getBoard().getFinishIndex();
        for (Pawn p : g.getPlayers().get(0).getPawns()) {
            p.setPathIndex(finish - 3);
        }
        RollResult r = g.roll();
        check(!r.hasMoves(), "no legal move");
        check(r.isRollAgain(), "6 rolls again");
        checkEq(PlayerColor.GREEN, g.getCurrentPlayer().getColor(), "same player");
        checkEq(GamePhase.ROLL, g.getPhase(), "roll phase");
    }

    private static void captureSendsToBase() {
        Game g = game(Difficulty.EASY, new ScriptedDie(2), PlayerColor.GREEN, PlayerColor.BLUE);
        Board b = g.getBoard();
        Pawn green = g.getPlayers().get(0).getPawns().get(0);
        Pawn blue = g.getPlayers().get(1).getPawns().get(0);
        int target = firstIndexOf(b, PlayerColor.GREEN, SquareKind.REGULAR, 5);
        green.setPathIndex(target - 2);
        // אם יש משבצת מוגנת בדרך זה לא משנה; חשוב רק שהיעד רגיל
        blue.setPathIndex(indexOnPath(b, PlayerColor.BLUE, b.getPath(PlayerColor.GREEN).get(target)));
        g.roll();
        Move m = g.legalMoveFor(green);
        check(m != null, "green can move");
        MoveResult res = g.move(m);
        checkEq(1, res.getCaptured().size(), "one captured");
        check(blue.isInBase(), "blue back in base");
    }

    private static void noCaptureOnSafe() {
        Game g = game(Difficulty.EASY, new ScriptedDie(2), PlayerColor.GREEN, PlayerColor.BLUE);
        Board b = g.getBoard();
        Pawn green = g.getPlayers().get(0).getPawns().get(0);
        Pawn blue = g.getPlayers().get(1).getPawns().get(0);
        int target = firstIndexOf(b, PlayerColor.GREEN, SquareKind.SAFE, 3);
        green.setPathIndex(target - 2);
        blue.setPathIndex(indexOnPath(b, PlayerColor.BLUE, b.getPath(PlayerColor.GREEN).get(target)));
        g.roll();
        MoveResult res = g.move(g.legalMoveFor(green));
        checkEq(0, res.getCaptured().size(), "nothing captured on a safe square");
        check(!blue.isInBase(), "blue stays");
    }

    private static void doubleBlockStops() {
        Game g = game(Difficulty.EASY, new ScriptedDie(5), PlayerColor.GREEN, PlayerColor.BLUE);
        Board b = g.getBoard();
        Pawn green = g.getPlayers().get(0).getPawns().get(0);
        Square blockSq = b.getPath(PlayerColor.GREEN).get(10);
        int blueIdx = indexOnPath(b, PlayerColor.BLUE, blockSq);
        g.getPlayers().get(1).getPawns().get(0).setPathIndex(blueIdx);
        g.getPlayers().get(1).getPawns().get(1).setPathIndex(blueIdx);
        green.setPathIndex(7);
        check(g.isBlockedFor(blockSq, g.getPlayers().get(0)), "block exists for green");
        g.roll();
        Move m = g.legalMoveFor(green);
        check(m != null, "green has a partial move");
        checkEq(9, m.getTo(), "stops before the block");
        check(m.isStoppedByBlock(), "marked as stopped by block");
    }

    private static void fullMovePreferred() {
        Game g = game(Difficulty.EASY, new ScriptedDie(5), PlayerColor.GREEN, PlayerColor.BLUE);
        Board b = g.getBoard();
        Pawn blocked = g.getPlayers().get(0).getPawns().get(0);
        Pawn free = g.getPlayers().get(0).getPawns().get(1);
        Square blockSq = b.getPath(PlayerColor.GREEN).get(10);
        int blueIdx = indexOnPath(b, PlayerColor.BLUE, blockSq);
        g.getPlayers().get(1).getPawns().get(0).setPathIndex(blueIdx);
        g.getPlayers().get(1).getPawns().get(1).setPathIndex(blueIdx);
        blocked.setPathIndex(7);
        free.setPathIndex(20);
        g.roll();
        check(g.legalMoveFor(blocked) == null, "blocked pawn may not do a partial move");
        check(g.legalMoveFor(free) != null, "free pawn must move");
    }

    private static void ownPawnsPassBlock() {
        Game g = game(Difficulty.EASY, new ScriptedDie(5), PlayerColor.GREEN, PlayerColor.BLUE);
        List<Pawn> mine = g.getPlayers().get(0).getPawns();
        mine.get(0).setPathIndex(10);
        mine.get(1).setPathIndex(10);
        mine.get(2).setPathIndex(7);
        g.roll();
        Move m = g.legalMoveFor(mine.get(2));
        checkEq(12, m.getTo(), "own block does not stop");
    }

    private static void blockOnStart() {
        Game g = game(Difficulty.EASY, new ScriptedDie(6), PlayerColor.GREEN, PlayerColor.BLUE);
        Board b = g.getBoard();
        Square greenStart = b.getPath(PlayerColor.GREEN).get(0);
        int idx = indexOnPath(b, PlayerColor.BLUE, greenStart);
        g.getPlayers().get(1).getPawns().get(0).setPathIndex(idx);
        g.getPlayers().get(1).getPawns().get(1).setPathIndex(idx);
        RollResult r = g.roll();
        check(!r.hasMoves(), "block on # keeps green in base");
    }

    private static void exactFinish() {
        Game g = game(Difficulty.EASY, new ScriptedDie(4), PlayerColor.GREEN, PlayerColor.BLUE);
        int finish = g.getBoard().getFinishIndex();
        Pawn p = g.getPlayers().get(0).getPawns().get(0);
        p.setPathIndex(finish - 3);
        RollResult r = g.roll();
        check(!r.hasMoves(), "4 is too much when 3 are left");
        checkEq(PlayerColor.BLUE, g.getCurrentPlayer().getColor(), "turn passed");
        Game g2 = game(Difficulty.EASY, new ScriptedDie(3), PlayerColor.GREEN, PlayerColor.BLUE);
        Pawn q = g2.getPlayers().get(0).getPawns().get(0);
        q.setPathIndex(finish - 3);
        g2.roll();
        MoveResult res = g2.move(g2.legalMoveFor(q));
        check(res.isReachedFinish(), "exact roll enters finish");
    }

    private static void gameOverRanking() {
        Game g = game(Difficulty.EASY, new ScriptedDie(1), PlayerColor.GREEN, PlayerColor.YELLOW, PlayerColor.BLUE);
        int finish = g.getBoard().getFinishIndex();
        List<Player> ps = g.getPlayers();
        for (int i = 0; i < 3; i++) {
            ps.get(0).getPawns().get(i).setPathIndex(finish);
            ps.get(1).getPawns().get(i).setPathIndex(finish);
        }
        ps.get(0).getPawns().get(3).setPathIndex(finish - 1);
        ps.get(1).getPawns().get(3).setPathIndex(finish - 1);
        g.roll();
        MoveResult r1 = g.move(g.legalMoveFor(ps.get(0).getPawns().get(3)));
        check(r1.isPlayerFinished(), "green finished");
        check(!r1.isGameOver(), "game continues with two players");
        checkEq(1, ps.get(0).getPlace(), "green first");
        g.roll();
        MoveResult r2 = g.move(g.legalMoveFor(ps.get(1).getPawns().get(3)));
        check(r2.isGameOver(), "game over when one player is left");
        checkEq("[P1, P2, P3]", g.getRanking().toString(), "ranking");
        checkEq(3, ps.get(2).getPlace(), "last player gets last place");
    }

    /** משחקים שלמים עם קובייה אקראית ומהלך אקראי: כל משחק מסתיים, בלי חריגות, עם דירוג מלא. */
    private static void randomGamesFinish() {
        PlayerColor[] all = PlayerColor.values();
        for (Difficulty d : Difficulty.values()) {
            for (int players = 2; players <= 4; players++) {
                for (int seed = 0; seed < 5; seed++) {
                    Random rnd = new Random(seed * 31 + players);
                    Board b = BoardBuilder.build(d, rnd);
                    List<Player> ps = new ArrayList<Player>();
                    for (int i = 0; i < players; i++) {
                        ps.add(new Player("P" + i, all[i], i));
                    }
                    Game g = new Game(b, ps, new StepDie(rnd), () -> 0L);
                    int steps = 0;
                    while (!g.isOver()) {
                        check(++steps < 200000, d + " game with " + players + " players did not finish");
                        if (g.getPhase() == GamePhase.ROLL) {
                            g.roll();
                        } else {
                            List<Move> moves = g.getLegalMoves();
                            g.move(moves.get(rnd.nextInt(moves.size())));
                        }
                    }
                    checkEq(players, g.getRanking().size(), "everyone ranked");
                }
            }
        }
    }

    // ------------------------------------------------------------- setup + history

    private static void setupValidation() {
        List<PlayerColor> two = Arrays.asList(PlayerColor.RED, PlayerColor.GREEN);
        check(GameSetupValidator.validate(2, Difficulty.EASY, Arrays.asList("Nour", "Wasim"), two).isValid(), "valid");
        check(GameSetupValidator.validate(1, Difficulty.EASY, Arrays.asList("a"), two).getGeneralError() != null, "1 player");
        check(GameSetupValidator.validate(2, null, Arrays.asList("a", "b"), two).getGeneralError() != null, "no level");
        SetupValidationResult r = GameSetupValidator.validate(2, Difficulty.EASY, Arrays.asList(" ", "a,b"), two);
        checkEq("יש להזין שם", r.getNameErrors().get(0), "empty name");
        checkEq("אסור להשתמש בפסיק בשם", r.getNameErrors().get(1), "comma");
        check(GameSetupValidator.validate(2, Difficulty.EASY, Arrays.asList("123456789012345", "b"), two).isValid(), "15 chars ok");
        check(GameSetupValidator.validate(2, Difficulty.EASY, Arrays.asList("1234567890123456", "b"), two).getNameErrors().containsKey(0), "16 chars");
        check(GameSetupValidator.validate(2, Difficulty.EASY, Arrays.asList("Nour", "nour"), two).getNameErrors().containsKey(1), "duplicate name");
        check(GameSetupValidator.validate(2, Difficulty.EASY, Arrays.asList("a", "b"), Arrays.asList(PlayerColor.RED, PlayerColor.RED)).getColorErrors().containsKey(1), "duplicate color");
        check(GameSetupValidator.validate(2, Difficulty.EASY, Arrays.asList("a", "b"), Arrays.asList(PlayerColor.RED, null)).getColorErrors().containsKey(1), "missing color");
        GameSettings s = GameSetupValidator.validate(3, Difficulty.HARD, Arrays.asList(" a ", "b", "c"),
                Arrays.asList(PlayerColor.BLUE, PlayerColor.RED, PlayerColor.GREEN)).getSettings();
        checkEq("a", s.getNames().get(0), "trimmed");
        checkEq(2, s.createPlayers().get(2).getTurnOrder(), "turn order by entry");
    }

    private static void historyCsv() throws Exception {
        File f = File.createTempFile("history", ".csv");
        f.delete();
        HistoryRepository repo = new HistoryRepository(f);
        repo.append(new GameRecord(repo.nextGameId(), "hard", "19/10/2026;12:01:00",
                Arrays.asList("Mor", "Noy", "Safa", "Muhammed"), "01:05:00"));
        repo.append(new GameRecord(repo.nextGameId(), "easy", "20/10/2026;09:00:05",
                Arrays.asList("נור", "וסים"), "00:12:30"));
        List<String> lines = Files.readAllLines(f.toPath(), StandardCharsets.UTF_8);
        checkEq(HistoryRepository.HEADER, lines.get(0), "header");
        checkEq("1,hard,19/10/2026;12:01:00,Mor,Noy,Safa,Muhammed,01:05:00", lines.get(1), "row 1");
        checkEq("2,easy,20/10/2026;09:00:05,נור,וסים,,,00:12:30", lines.get(2), "row 2 with empty places");
        String raw = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
        check(raw.contains("\r\n"), "CRLF line endings like the template");
        checkEq(2, repo.loadAll().size(), "reads back");
        checkEq("", repo.loadAll().get(1).getPlace(3), "empty third place");
        checkEq("01:01:01", HistoryRepository.formatDuration(3661000L), "duration");
        f.delete();
    }
}

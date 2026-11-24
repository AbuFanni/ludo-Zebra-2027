package Model;

import static Model.ModelTests.check;
import static Model.ModelTests.checkEq;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * בדיקות למודל השחקנים: סדר התורות (FR-07) ונעילת ההגדרות אחרי תחילת המשחק (FR-08).
 * <p>
 * המשחק נוצר כמו במשחק האמיתי: GameSetupValidator ← GameSettings ← createPlayers ← Game
 * (כמו ב-GameController.start). כל בדיקה מסומנת T6.xx כמו ברשומת הבדיקות.
 * רצות יחד עם שאר בדיקות המודל ({@link ModelTests}), דרך build.sh / build.bat.
 *
 * @author Nimer Ziad (Zebra) – Iteration 1, SDP table 2 (FR-04–FR-08)
 */
final class PlayerModelTests {

    private static final List<String> NAMES = Arrays.asList("Nimer", "Wasim", "Nour", "Mohamed");

    /** Colors in an order that is not the enum order, so the turn order cannot come from the color. */
    private static final List<PlayerColor> MIXED =
            Arrays.asList(PlayerColor.RED, PlayerColor.GREEN, PlayerColor.BLUE, PlayerColor.YELLOW);
    private static final List<PlayerColor> REVERSED =
            Arrays.asList(PlayerColor.YELLOW, PlayerColor.BLUE, PlayerColor.GREEN, PlayerColor.RED);

    private PlayerModelTests() {
    }

    static void runAll() {
        // FR-07: סדר התורות לפי סדר הזנת השחקנים
        ModelTests.run("T6.01 FR-07 players get their turn order from the entry order", PlayerModelTests::entryOrder);
        ModelTests.run("T6.02 FR-07 the first player entered rolls first", PlayerModelTests::firstEnteredStarts);
        ModelTests.run("T6.03 FR-07 4 players: turns go 1, 2, 3, 4, 1, 2", PlayerModelTests::fourPlayerTurns);
        ModelTests.run("T6.04 FR-07 2 and 3 players: turns follow the entry order", PlayerModelTests::twoAndThreePlayers);
        ModelTests.run("T6.05 FR-07 turn order does not depend on the colors", PlayerModelTests::notByColor);
        // FR-08: אי אפשר לשנות רמה, שם או צבע אחרי תחילת המשחק
        ModelTests.run("T6.06 FR-08 changing the screen's lists after start changes nothing", PlayerModelTests::inputListsCopied);
        ModelTests.run("T6.07 FR-08 the settings' names and colors cannot be edited", PlayerModelTests::settingsReadOnly);
        ModelTests.run("T6.08 FR-08 the game's player list cannot be edited", PlayerModelTests::playersReadOnly);
        ModelTests.run("T6.09 FR-08 no public method changes a name, color or level", PlayerModelTests::noSetters);
        ModelTests.run("T6.10 FR-08 level, names and colors stay the same during a game", PlayerModelTests::sameDuringGame);
    }

    // ------------------------------------------------------------- עזר

    /** Valid settings, made by the same Model check the setup screen uses. */
    private static GameSettings settings(Difficulty level, List<String> names, List<PlayerColor> colors) {
        SetupValidationResult r = GameSetupValidator.validate(names.size(), level, names, colors);
        check(r.isValid(), "setup should be valid: " + names + " " + colors);
        return r.getSettings();
    }

    /** A game started like GameController.start(), with the given die. */
    private static Game start(GameSettings s, Die die) {
        Board board = BoardBuilder.build(s.getDifficulty(), new Random(1));
        return new Game(board, s.createPlayers(), die, () -> 0L);
    }

    /**
     * Rolls a 1 the given number of times. All pieces are in the castle, so nobody can move and the
     * turn passes every time. Returns the names of the players who got the turn, in order.
     */
    private static List<String> turnsAfterOnes(Game g, int rolls) {
        final List<String> turns = new ArrayList<String>();
        g.addListener(new GameListener() {
            @Override
            public void onTurnChanged(Player current) {
                turns.add(current.getName());
            }
        });
        for (int i = 0; i < rolls; i++) {
            g.roll();
        }
        return turns;
    }

    private static List<String> names(List<Player> players) {
        List<String> r = new ArrayList<String>();
        for (Player p : players) {
            r.add(p.getName());
        }
        return r;
    }

    private static List<PlayerColor> colors(List<Player> players) {
        List<PlayerColor> r = new ArrayList<PlayerColor>();
        for (Player p : players) {
            r.add(p.getColor());
        }
        return r;
    }

    // ------------------------------------------------------------- FR-07

    private static void entryOrder() {
        List<Player> players = settings(Difficulty.EASY, NAMES, MIXED).createPlayers();
        checkEq(NAMES, names(players), "names in entry order");
        checkEq(MIXED, colors(players), "colors stay with their names");
        for (int i = 0; i < players.size(); i++) {
            checkEq(i, players.get(i).getTurnOrder(), "turn order of " + players.get(i).getName());
        }
    }

    private static void firstEnteredStarts() {
        Game g = start(settings(Difficulty.MEDIUM, NAMES, MIXED), () -> 1);
        checkEq("Nimer", g.getCurrentPlayer().getName(), "first to roll");
    }

    private static void fourPlayerTurns() {
        Game g = start(settings(Difficulty.EASY, NAMES, MIXED), () -> 1);
        checkEq(Arrays.asList("Wasim", "Nour", "Mohamed", "Nimer", "Wasim"),
                turnsAfterOnes(g, 5), "turns after 5 rolls of 1");
    }

    private static void twoAndThreePlayers() {
        Game two = start(settings(Difficulty.EASY, NAMES.subList(0, 2), MIXED.subList(0, 2)), () -> 1);
        checkEq(Arrays.asList("Wasim", "Nimer", "Wasim", "Nimer"), turnsAfterOnes(two, 4), "2 players");
        Game three = start(settings(Difficulty.HARD, NAMES.subList(0, 3), MIXED.subList(0, 3)), () -> 1);
        checkEq(Arrays.asList("Wasim", "Nour", "Nimer", "Wasim"), turnsAfterOnes(three, 4), "3 players");
    }

    /** Same players with opposite color orders: the turns follow the names both times. */
    private static void notByColor() {
        List<String> expected = Arrays.asList("Wasim", "Nour", "Mohamed", "Nimer");
        checkEq(expected, turnsAfterOnes(start(settings(Difficulty.EASY, NAMES, MIXED), () -> 1), 4), "mixed colors");
        checkEq(expected, turnsAfterOnes(start(settings(Difficulty.EASY, NAMES, REVERSED), () -> 1), 4), "reversed colors");
    }

    // ------------------------------------------------------------- FR-08

    /** The screen's lists are changed after the game started: the settings and the game keep the old values. */
    private static void inputListsCopied() {
        List<String> screenNames = new ArrayList<String>(NAMES);
        List<PlayerColor> screenColors = new ArrayList<PlayerColor>(MIXED);
        GameSettings s = settings(Difficulty.HARD, screenNames, screenColors);
        Game g = start(s, () -> 1);

        screenNames.set(0, "Changed");
        screenColors.set(0, PlayerColor.YELLOW);
        screenNames.add("Fifth");

        checkEq(NAMES, s.getNames(), "settings names");
        checkEq(MIXED, s.getColors(), "settings colors");
        checkEq(NAMES, names(g.getPlayers()), "game names");
        checkEq(MIXED, colors(g.getPlayers()), "game colors");
        checkEq(Difficulty.HARD, g.getBoard().getDifficulty(), "game level");
    }

    private static void settingsReadOnly() {
        GameSettings s = settings(Difficulty.EASY, NAMES, MIXED);
        check(refused(() -> s.getNames().set(0, "Changed")), "names.set must be refused");
        check(refused(() -> s.getNames().add("Fifth")), "names.add must be refused");
        check(refused(() -> s.getColors().set(0, PlayerColor.YELLOW)), "colors.set must be refused");
        check(refused(() -> s.getColors().remove(0)), "colors.remove must be refused");
        checkEq(NAMES, s.getNames(), "names unchanged");
        checkEq(MIXED, s.getColors(), "colors unchanged");
    }

    private static void playersReadOnly() {
        List<Player> given = settings(Difficulty.EASY, NAMES, MIXED).createPlayers();
        Game g = new Game(BoardBuilder.build(Difficulty.EASY, new Random(1)), given, () -> 1, () -> 0L);
        Player intruder = new Player("Intruder", PlayerColor.RED, 0);

        given.set(0, intruder);
        given.remove(1);
        checkEq(NAMES, names(g.getPlayers()), "game keeps its own copy of the list");
        check(refused(() -> g.getPlayers().set(0, intruder)), "players.set must be refused");
        check(refused(() -> g.getPlayers().add(intruder)), "players.add must be refused");
        check(refused(() -> g.getPlayers().remove(0)), "players.remove must be refused");
    }

    /**
     * Guards the design: name, color, turn order and level are final fields, and the classes that hold
     * them have no public set... method. If someone adds one later, this test fails.
     */
    private static void noSetters() throws Exception {
        Class<?>[] holders = {Player.class, GameSettings.class, Game.class, Board.class};
        for (Class<?> c : holders) {
            for (Method m : c.getMethods()) {
                check(!m.getName().startsWith("set"), c.getSimpleName() + " has public " + m.getName() + "()");
            }
        }
        checkFinal(Player.class, "name", "color", "turnOrder");
        checkFinal(GameSettings.class, "difficulty", "names", "colors");
        checkFinal(Game.class, "board", "players");
        checkFinal(Board.class, "difficulty");
    }

    /** A real game with a real die: after every roll and move, nothing in the settings has changed. */
    private static void sameDuringGame() {
        for (Difficulty level : Difficulty.values()) {
            Game g = start(settings(level, NAMES, MIXED), new StepDie(new Random(level.ordinal() + 3)));
            Board board = g.getBoard();
            for (int step = 0; step < 300 && !g.isOver(); step++) {
                g.roll();
                if (g.getPhase() == GamePhase.MOVE) {
                    g.move(g.getLegalMoves().get(0));
                }
                check(g.getBoard() == board, level + ": board replaced at step " + step);
                checkEq(level, g.getBoard().getDifficulty(), level + ": level at step " + step);
                checkEq(NAMES, names(g.getPlayers()), level + ": names at step " + step);
                checkEq(MIXED, colors(g.getPlayers()), level + ": colors at step " + step);
            }
        }
    }

    // ------------------------------------------------------------- עזר לבדיקות FR-08

    /** @return true if the action was refused with UnsupportedOperationException */
    private static boolean refused(Runnable action) {
        try {
            action.run();
            return false;
        } catch (UnsupportedOperationException expected) {
            return true;
        }
    }

    private static void checkFinal(Class<?> c, String... fields) throws Exception {
        for (String name : fields) {
            Field f = c.getDeclaredField(name);
            check(Modifier.isFinal(f.getModifiers()), c.getSimpleName() + "." + name + " must be final");
        }
    }
}

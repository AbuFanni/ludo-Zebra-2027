package Model;

import static Model.ModelTests.check;
import static Model.ModelTests.checkEq;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * בדיקות יחידה למשבצות ההפתעה ולסיום המשחק – איטרציה 2, US-8 ו-US-9 (FR-44 עד FR-54, NFR-11 חלק 2).
 * כל בדיקה מסומנת T9.xx, ומזהה הדרישה שהיא בודקת מופיע בשם שלה.
 * <p>
 * כדי שהבדיקות יהיו קבועות, האירוע וקוביית ה-UltraBoost נקבעים מראש ({@link SurpriseDeck}),
 * והלוח נבחר מ-seeds קבועים כך שמשבצת ההפתעה נמצאת במקום שמתאים לבדיקה.
 *
 * @author Nour Eldin Khatib (Zebra) – Iteration 2, US-8, US-9
 */
final class SurpriseTests {

    private SurpriseTests() {
    }

    static void runAll() {
        ModelTests.run("T9.01 FR-44 surprise percentages: 10/10/10/30/30/10", SurpriseTests::percentages);
        ModelTests.run("T9.02 NFR-11 10,000 surprise draws within 2 percentage points", SurpriseTests::distribution);
        ModelTests.run("T9.03 FR-44 landing on ! draws an event and waits", SurpriseTests::landingDraws);
        ModelTests.run("T9.04 FR-44 game without surprises treats ! as a safe square", SurpriseTests::noDeck);
        ModelTests.run("T9.05 FR-45 jump to finish, no exact number needed", SurpriseTests::jumpToFinish);
        ModelTests.run("T9.06 FR-45 FR-53 jump with the last pawn ranks the player", SurpriseTests::jumpFinishesPlayer);
        ModelTests.run("T9.07 FR-48 back to the castle", SurpriseTests::backToBase);
        ModelTests.run("T9.08 FR-49 back 3 steps", SurpriseTests::backThree);
        ModelTests.run("T9.09 FR-49 FR-26 back 3 stops on the start square", SurpriseTests::backThreeStopsAtStart);
        ModelTests.run("T9.10 FR-49 FR-29 back 3 captures a single opponent", SurpriseTests::backThreeCaptures);
        ModelTests.run("T9.11 FR-49 FR-33 back 3 stops before a Double Block", SurpriseTests::backThreeBlocked);
        ModelTests.run("T9.12 FR-50 lose the next turn", SurpriseTests::loseNextTurn);
        ModelTests.run("T9.13 FR-50 losing the next turn cancels the extra roll from 6", SurpriseTests::loseTurnCancelsSix);
        ModelTests.run("T9.14 FR-17 other events keep the extra roll from 6", SurpriseTests::otherEventsKeepSix);
        ModelTests.run("T9.15 FR-46 opponent moves back half a new roll, rounded up", SurpriseTests::moveOpponentHalf);
        ModelTests.run("T9.16 FR-46 FR-47 only opponents with a pawn on the shared track", SurpriseTests::opponentTargets);
        ModelTests.run("T9.17 FR-47 event cancelled when no opponent can be moved", SurpriseTests::moveOpponentCancelled);
        ModelTests.run("T9.18 FR-46 own pawn or pawn off the track is rejected", SurpriseTests::moveOpponentRejects);
        ModelTests.run("T9.19 FR-51 UltraBoost: steps = step die x (UltraBoost - 1)", SurpriseTests::ultraBoostSteps);
        ModelTests.run("T9.20 FR-51 UltraBoost 6 can take a pawn out of the castle", SurpriseTests::ultraBoostLeavesBase);
        ModelTests.run("T9.21 FR-52 UltraBoost 6 gives an extra roll, not counted in the 6 streak", SurpriseTests::ultraBoostSix);
        ModelTests.run("T9.22 FR-16 UltraBoost 1 (factor 0) moves nothing", SurpriseTests::ultraBoostZero);
        ModelTests.run("T9.23 FR-44 events cannot be skipped or run out of phase", SurpriseTests::outOfPhase);
        ModelTests.run("T9.24 FR-27 back 3 onto a ? square asks the pawn's owner", SurpriseTests::backThreeOntoQuestion);
        ModelTests.run("T9.25 FR-53 finished player is skipped in the turn order", SurpriseTests::finishedSkipped);
        ModelTests.run("T9.26 FR-54 one player left: game over, last place, time stops", SurpriseTests::gameOver);
        ModelTests.run("T9.27 random medium/hard games with surprises always finish", SurpriseTests::randomGamesFinish);
    }

    // ------------------------------------------------------------- עזר

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

    private static SurpriseDeck deck(SurpriseEvent event, int... ultraBoost) {
        int[] u = ultraBoost.length == 0 ? new int[]{1} : ultraBoost;
        return new SurpriseDeck(() -> event, new ScriptedDie(u));
    }

    private static Game game(Board b, SurpriseDeck deck, int... rolls) {
        List<Player> players = new ArrayList<Player>();
        players.add(new Player("Green", PlayerColor.GREEN, 0));
        players.add(new Player("Blue", PlayerColor.BLUE, 1));
        return new Game(b, players, new ScriptedDie(rolls), () -> 0L, null, deck);
    }

    private static Pawn pawn(Game g, int player, int n) {
        return g.getPlayers().get(player).getPawns().get(n);
    }

    private static Square green(Board b, int index) {
        return b.getPath(PlayerColor.GREEN).get(index);
    }

    private static boolean isSurprise(Board b, int index) {
        return green(b, index) instanceof SurpriseSquare;
    }

    private static boolean plain(Board b, int index) {
        Square s = green(b, index);
        return s.getKind() == SquareKind.REGULAR;
    }

    private static boolean onBluePath(Board b, int greenIndex) {
        return b.getPath(PlayerColor.BLUE).indexOf(green(b, greenIndex)) > 0;
    }

    private static void putBlue(Game g, Board b, int n, int greenIndex) {
        pawn(g, 1, n).setPathIndex(b.getPath(PlayerColor.BLUE).indexOf(green(b, greenIndex)));
    }

    interface Finder {
        /** @return אינדקס משבצת הפתעה מתאימה במסלול הירוק, או -1 */
        int find(Board b);
    }

    private static final class Spot {
        final Board board;
        final int index;

        Spot(Board board, int index) {
            this.board = board;
            this.index = index;
        }
    }

    /** עובר על seeds קבועים עד שנמצא לוח שבו יש משבצת הפתעה שמתאימה לבדיקה. */
    private static Spot spot(Difficulty d, Finder f) {
        for (int seed = 1; seed <= 20000; seed++) {
            Board b = BoardBuilder.build(d, new Random(seed));
            int i = f.find(b);
            if (i >= 0) {
                return new Spot(b, i);
            }
        }
        throw new AssertionError("no board found for the scenario");
    }

    /** משבצת הפתעה שאפשר להגיע אליה ב-dist צעדים, וששלוש המשבצות שלפניה רגילות. */
    private static Finder reachable(int dist) {
        return b -> {
            int shared = b.getSharedTrack().size();
            for (int i = Math.max(4, dist); i < shared - 2; i++) {
                if (isSurprise(b, i) && plain(b, i - 1) && plain(b, i - 2) && plain(b, i - 3)
                        && (dist < 4 || plain(b, i - dist))) {
                    return i;
                }
            }
            return -1;
        };
    }

    /** מביא את הכלי הירוק הראשון למשבצת ההפתעה בהטלה של dist, ומחזיר את המשחק בשלב ההפתעה. */
    private static Game land(Spot s, SurpriseDeck deck, int dist, int... laterRolls) {
        int[] rolls = new int[laterRolls.length + 1];
        rolls[0] = dist;
        System.arraycopy(laterRolls, 0, rolls, 1, laterRolls.length);
        Game g = game(s.board, deck, rolls);
        pawn(g, 0, 0).setPathIndex(s.index - dist);
        g.roll();
        g.move(g.legalMoveFor(pawn(g, 0, 0)));
        checkEq(GamePhase.SURPRISE, g.getPhase(), "surprise phase after landing");
        return g;
    }

    // ------------------------------------------------------------- הגרלה

    private static void percentages() {
        Map<SurpriseEvent, Integer> expected = new EnumMap<SurpriseEvent, Integer>(SurpriseEvent.class);
        expected.put(SurpriseEvent.JUMP_TO_FINISH, 10);
        expected.put(SurpriseEvent.MOVE_OPPONENT, 10);
        expected.put(SurpriseEvent.BACK_TO_BASE, 10);
        expected.put(SurpriseEvent.BACK_THREE, 30);
        expected.put(SurpriseEvent.LOSE_NEXT_TURN, 30);
        expected.put(SurpriseEvent.ULTRA_BOOST, 10);
        int sum = 0;
        for (SurpriseEvent e : SurpriseEvent.values()) {
            checkEq(expected.get(e), e.getPercent(), e + " percent");
            sum += e.getPercent();
        }
        checkEq(100, sum, "sum of percentages");
    }

    private static void distribution() {
        for (int seed = 1; seed <= 5; seed++) {
            Random r = new Random(seed);
            Map<SurpriseEvent, Integer> counts = new EnumMap<SurpriseEvent, Integer>(SurpriseEvent.class);
            int n = 10000;
            for (int i = 0; i < n; i++) {
                SurpriseEvent e = SurpriseDeck.drawWeighted(r);
                Integer c = counts.get(e);
                counts.put(e, c == null ? 1 : c + 1);
            }
            for (SurpriseEvent e : SurpriseEvent.values()) {
                Integer c = counts.get(e);
                double percent = 100.0 * (c == null ? 0 : c) / n;
                check(Math.abs(percent - e.getPercent()) <= 2.0,
                        "seed " + seed + ": " + e + " came " + percent + "%, expected " + e.getPercent() + "% ± 2");
            }
        }
    }

    // ------------------------------------------------------------- נחיתה

    private static void landingDraws() {
        Spot s = spot(Difficulty.MEDIUM, reachable(2));
        Game g = game(s.board, deck(SurpriseEvent.BACK_THREE), 2);
        final List<SurpriseEvent> heard = new ArrayList<SurpriseEvent>();
        g.addListener(new GameListener() {
            @Override
            public void onSurprise(SurpriseEvent event, Pawn pawn) {
                heard.add(event);
            }
        });
        Pawn p = pawn(g, 0, 0);
        p.setPathIndex(s.index - 2);
        g.roll();
        MoveResult r = g.move(g.legalMoveFor(p));
        checkEq(GamePhase.SURPRISE, g.getPhase(), "phase");
        checkEq(SurpriseEvent.BACK_THREE, g.getPendingSurprise(), "pending event");
        check(g.getSurprisePawn() == p, "the pawn on the ! square");
        checkEq(1, heard.size(), "listener told once");
        checkEq("Green", g.getCurrentPlayer().getName(), "turn not passed yet");
        check(!r.isExtraTurn() && !r.isGameOver(), "no end-of-turn yet");
        try {
            g.roll();
            throw new AssertionError("rolled while a surprise waits");
        } catch (IllegalStateException expected) {
            // ok
        }
    }

    private static void noDeck() {
        Spot s = spot(Difficulty.MEDIUM, reachable(2));
        Game g = game(s.board, null, 2);
        Pawn p = pawn(g, 0, 0);
        p.setPathIndex(s.index - 2);
        g.roll();
        g.move(g.legalMoveFor(p));
        checkEq(GamePhase.ROLL, g.getPhase(), "no surprise phase");
        checkEq("Blue", g.getCurrentPlayer().getName(), "turn passed");
        check(g.getPendingSurprise() == null, "no pending surprise");
    }

    // ------------------------------------------------------------- אירועים

    private static void jumpToFinish() {
        Spot s = spot(Difficulty.MEDIUM, reachable(2));
        Game g = land(s, deck(SurpriseEvent.JUMP_TO_FINISH), 2);
        Pawn p = pawn(g, 0, 0);
        SurpriseResult r = g.resolveSurprise();
        checkEq(s.board.getFinishIndex(), p.getPathIndex(), "pawn in the finish");
        check(r.isReachedFinish(), "reached finish");
        check(!r.isPlayerFinished(), "other pawns still out");
        checkEq("Blue", g.getCurrentPlayer().getName(), "turn passed (entering home gives no extra roll)");
    }

    private static void jumpFinishesPlayer() {
        Spot s = spot(Difficulty.MEDIUM, reachable(2));
        Game g = game(s.board, deck(SurpriseEvent.JUMP_TO_FINISH), 2);
        for (int i = 1; i < Player.PAWNS; i++) {
            pawn(g, 0, i).setPathIndex(s.board.getFinishIndex());
        }
        Pawn p = pawn(g, 0, 0);
        p.setPathIndex(s.index - 2);
        g.roll();
        g.move(g.legalMoveFor(p));
        SurpriseResult r = g.resolveSurprise();
        check(r.isPlayerFinished(), "player finished");
        checkEq(1, g.getPlayers().get(0).getPlace(), "first place");
        check(r.isGameOver() && g.isOver(), "two players: the game is over");
        checkEq(2, g.getPlayers().get(1).getPlace(), "the other player is last");
    }

    private static void backToBase() {
        Spot s = spot(Difficulty.MEDIUM, reachable(2));
        Game g = land(s, deck(SurpriseEvent.BACK_TO_BASE), 2);
        Pawn p = pawn(g, 0, 0);
        SurpriseResult r = g.resolveSurprise();
        check(p.isInBase(), "pawn back in the castle");
        checkEq(Pawn.IN_BASE, r.getTo(), "result says castle");
        checkEq("Blue", g.getCurrentPlayer().getName(), "turn passed");
    }

    private static void backThree() {
        Spot s = spot(Difficulty.MEDIUM, reachable(2));
        Game g = land(s, deck(SurpriseEvent.BACK_THREE), 2);
        Pawn p = pawn(g, 0, 0);
        SurpriseResult r = g.resolveSurprise();
        checkEq(s.index - 3, p.getPathIndex(), "3 steps back");
        checkEq(AnswerResult.Limit.NONE, r.getLimit(), "no limit");
        checkEq(GamePhase.ROLL, g.getPhase(), "back to rolling");
        checkEq("Blue", g.getCurrentPlayer().getName(), "turn passed");
    }

    private static void backThreeStopsAtStart() {
        Spot s = spot(Difficulty.HARD, b -> {
            for (int i = 1; i <= 2; i++) {
                if (isSurprise(b, i)) {
                    return i;
                }
            }
            return -1;
        });
        Game g = game(s.board, deck(SurpriseEvent.BACK_THREE), s.index);
        Pawn p = pawn(g, 0, 0);
        p.setPathIndex(0);
        g.roll();
        g.move(g.legalMoveFor(p));
        SurpriseResult r = g.resolveSurprise();
        checkEq(0, p.getPathIndex(), "stopped on the start square");
        checkEq(AnswerResult.Limit.START_SQUARE, r.getLimit(), "limit");
    }

    private static void backThreeCaptures() {
        Spot s = spot(Difficulty.MEDIUM, b -> {
            int i = reachable(2).find(b);
            return i >= 0 && onBluePath(b, i - 3) ? i : -1;
        });
        Game g = game(s.board, deck(SurpriseEvent.BACK_THREE), 2);
        Pawn p = pawn(g, 0, 0);
        Pawn blue = pawn(g, 1, 0);
        p.setPathIndex(s.index - 2);
        putBlue(g, s.board, 0, s.index - 3);
        g.roll();
        g.move(g.legalMoveFor(p));
        SurpriseResult r = g.resolveSurprise();
        checkEq(1, r.getCaptured().size(), "captured");
        check(blue.isInBase(), "blue back in its castle");
    }

    private static void backThreeBlocked() {
        Spot s = spot(Difficulty.MEDIUM, b -> {
            int i = reachable(1).find(b);
            return i >= 0 && onBluePath(b, i - 2) ? i : -1;
        });
        Game g = game(s.board, deck(SurpriseEvent.BACK_THREE), 1);
        Pawn p = pawn(g, 0, 0);
        p.setPathIndex(s.index - 1);
        putBlue(g, s.board, 0, s.index - 2);
        putBlue(g, s.board, 1, s.index - 2);
        g.roll();
        g.move(g.legalMoveFor(p));
        SurpriseResult r = g.resolveSurprise();
        checkEq(s.index - 1, p.getPathIndex(), "stopped before the block");
        checkEq(AnswerResult.Limit.DOUBLE_BLOCK, r.getLimit(), "limit");
        check(!pawn(g, 1, 0).isInBase() && !pawn(g, 1, 1).isInBase(), "block not captured");
    }

    private static void loseNextTurn() {
        Spot s = spot(Difficulty.MEDIUM, reachable(2));
        // ירוק נוחת ב-2; כחול מטיל 1 בלי כלים על הלוח; התור של ירוק מדולג; כחול מטיל שוב
        Game g = land(s, deck(SurpriseEvent.LOSE_NEXT_TURN), 2, 1, 1);
        final List<Player> skipped = new ArrayList<Player>();
        g.addListener(new GameListener() {
            @Override
            public void onTurnSkipped(Player player) {
                skipped.add(player);
            }
        });
        g.resolveSurprise();
        check(g.losesNextTurn(g.getPlayers().get(0)), "green marked");
        checkEq("Blue", g.getCurrentPlayer().getName(), "turn passed");
        g.roll(); // כחול: 1 ואין מהלך
        checkEq(1, skipped.size(), "one turn skipped");
        check(skipped.get(0) == g.getPlayers().get(0), "green skipped");
        checkEq("Blue", g.getCurrentPlayer().getName(), "blue plays again");
        check(!g.losesNextTurn(g.getPlayers().get(0)), "only one turn is lost");
        g.roll();
        checkEq("Green", g.getCurrentPlayer().getName(), "green back in the order");
    }

    private static void loseTurnCancelsSix() {
        Spot s = spot(Difficulty.MEDIUM, reachable(6));
        Game g = land(s, deck(SurpriseEvent.LOSE_NEXT_TURN), 6);
        SurpriseResult r = g.resolveSurprise();
        check(!r.isExtraTurn(), "no extra roll");
        checkEq("Blue", g.getCurrentPlayer().getName(), "turn passed despite the 6");
    }

    private static void otherEventsKeepSix() {
        Spot s = spot(Difficulty.MEDIUM, reachable(6));
        Game g = land(s, deck(SurpriseEvent.BACK_TO_BASE), 6);
        SurpriseResult r = g.resolveSurprise();
        check(r.isExtraTurn(), "extra roll kept");
        checkEq("Green", g.getCurrentPlayer().getName(), "green rolls again");
        checkEq(GamePhase.ROLL, g.getPhase(), "roll phase");
    }

    private static Spot opponentSpot() {
        return spot(Difficulty.MEDIUM, b -> {
            int i = reachable(2).find(b);
            if (i < 0) {
                return -1;
            }
            // משבצת רגילה במסלול הכחול, שיש לפניה 3 משבצות רגילות לאחור
            List<Square> blue = b.getPath(PlayerColor.BLUE);
            for (int j = 10; j < 30; j++) {
                boolean ok = blue.get(j).getKind() == SquareKind.REGULAR;
                for (int k = 1; k <= 3 && ok; k++) {
                    ok = blue.get(j - k).getKind() == SquareKind.REGULAR;
                }
                if (ok && blue.get(j) != b.getPath(PlayerColor.GREEN).get(i)) {
                    return i;
                }
            }
            return -1;
        });
    }

    private static int blueSpot(Board b) {
        List<Square> blue = b.getPath(PlayerColor.BLUE);
        for (int j = 10; j < 30; j++) {
            boolean ok = blue.get(j).getKind() == SquareKind.REGULAR;
            for (int k = 1; k <= 3 && ok; k++) {
                ok = blue.get(j - k).getKind() == SquareKind.REGULAR;
            }
            if (ok) {
                return j;
            }
        }
        throw new AssertionError("no blue spot");
    }

    private static void moveOpponentHalf() {
        int[][] cases = {{1, 1}, {2, 1}, {3, 2}, {4, 2}, {5, 3}, {6, 3}};
        Spot s = opponentSpot();
        int j = blueSpot(s.board);
        for (int[] c : cases) {
            Game g = land(s, deck(SurpriseEvent.MOVE_OPPONENT), 2, c[0]);
            Pawn blue = pawn(g, 1, 0);
            blue.setPathIndex(j);
            checkEq(1, g.surpriseOpponents().size(), "one opponent to choose");
            SurpriseResult r = g.moveOpponent(blue);
            checkEq(c[0], r.getStepRoll(), "new step roll");
            checkEq(j - c[1], blue.getPathIndex(), "roll " + c[0] + " moves back " + c[1]);
            check(r.getMovedPawn() == blue, "moved pawn");
            checkEq("Blue", g.getCurrentPlayer().getName(), "turn passed");
        }
    }

    private static void opponentTargets() {
        Spot s = opponentSpot();
        Game g = land(s, deck(SurpriseEvent.MOVE_OPPONENT), 2);
        Player blue = g.getPlayers().get(1);
        // בטירה, על משבצת ההתחלה, במסלול הבית – לא; על המסלול המשותף – כן
        pawn(g, 1, 1).setPathIndex(0);
        pawn(g, 1, 2).setPathIndex(s.board.getFinishIndex() - 1);
        pawn(g, 1, 3).setPathIndex(blueSpot(s.board));
        List<Pawn> targets = g.surpriseTargets(blue);
        checkEq(1, targets.size(), "one pawn on the shared track");
        check(targets.get(0) == pawn(g, 1, 3), "the pawn on the shared track");
        check(!g.surpriseOpponents().contains(g.getPlayers().get(0)), "own player is not an opponent");
    }

    private static void moveOpponentCancelled() {
        Spot s = spot(Difficulty.MEDIUM, reachable(2));
        Game g = land(s, deck(SurpriseEvent.MOVE_OPPONENT), 2);
        check(g.surpriseOpponents().isEmpty(), "blue has no pawn on the track");
        SurpriseResult r = g.resolveSurprise();
        check(r.isCancelled(), "cancelled");
        checkEq("Blue", g.getCurrentPlayer().getName(), "turn passed");
    }

    private static void moveOpponentRejects() {
        Spot s = opponentSpot();
        Game g = land(s, deck(SurpriseEvent.MOVE_OPPONENT), 2);
        pawn(g, 1, 0).setPathIndex(blueSpot(s.board));
        try {
            g.moveOpponent(pawn(g, 0, 1));
            throw new AssertionError("own pawn accepted");
        } catch (IllegalArgumentException expected) {
            // ok
        }
        try {
            g.moveOpponent(pawn(g, 1, 1));
            throw new AssertionError("pawn in the castle accepted");
        } catch (IllegalArgumentException expected) {
            // ok
        }
        try {
            g.resolveSurprise();
            throw new AssertionError("resolved without choosing although an opponent can be moved");
        } catch (IllegalStateException expected) {
            // ok
        }
        checkEq(GamePhase.SURPRISE, g.getPhase(), "still waiting for a choice");
    }

    private static void ultraBoostSteps() {
        Spot s = spot(Difficulty.MEDIUM, reachable(2));
        // הטלה מיוחדת: קוביית צעדים 2, UltraBoost 4 → 2 × 3 = 6 צעדים
        Game g = land(s, deck(SurpriseEvent.ULTRA_BOOST, 4), 2, 2);
        Pawn p = pawn(g, 0, 0);
        int from = p.getPathIndex();
        SurpriseResult r = g.rollUltraBoost();
        checkEq(2, r.getStepRoll(), "step die");
        checkEq(4, r.getUltraBoostRoll(), "UltraBoost");
        checkEq(6, r.getUltraBoostSteps(), "steps");
        check(r.hasMoves() && r.isContinues(), "player chooses a pawn");
        checkEq(GamePhase.MOVE, g.getPhase(), "move phase");
        checkEq(6, g.getSpecialSteps(), "special steps shown");
        Move m = g.legalMoveFor(p);
        check(m != null, "pawn can move");
        checkEq(from + 6, m.getTo(), "move uses the special steps");
        g.move(m);
        checkEq("Blue", g.getCurrentPlayer().getName(), "turn passed");
        checkEq(-1, g.getSpecialSteps(), "special roll over");
    }

    private static void ultraBoostLeavesBase() {
        Spot s = spot(Difficulty.MEDIUM, reachable(2));
        Game g = land(s, deck(SurpriseEvent.ULTRA_BOOST, 3), 2, 6);
        g.rollUltraBoost();
        Move out = g.legalMoveFor(pawn(g, 0, 1));
        check(out != null && out.getFrom() == Pawn.IN_BASE, "can take a pawn out on 6");
        checkEq(0, out.getTo(), "to the start square");
    }

    private static void ultraBoostSix() {
        Spot s = spot(Difficulty.MEDIUM, reachable(6));
        // 6 לנחיתה (רצף 1), הטלה מיוחדת 6, ואז 6 (רצף 2) ו-6 (רצף 3 – מבוטל)
        Game g = land(s, deck(SurpriseEvent.ULTRA_BOOST, 2), 6, 6, 6, 6);
        g.rollUltraBoost();
        g.move(g.getLegalMoves().get(0));
        checkEq("Green", g.getCurrentPlayer().getName(), "extra roll after the special 6");
        checkEq(GamePhase.ROLL, g.getPhase(), "roll phase");
        RollResult second = g.roll();
        check(!second.isCancelledThirdSix(), "special 6 is not counted in the streak");
        g.move(g.getLegalMoves().get(0));
        RollResult third = g.roll();
        check(third.isCancelledThirdSix(), "streak was not reset by the special roll");
    }

    private static void ultraBoostZero() {
        Spot s = spot(Difficulty.MEDIUM, reachable(2));
        Game g = land(s, deck(SurpriseEvent.ULTRA_BOOST, 1), 2, 5);
        int before = pawn(g, 0, 0).getPathIndex();
        SurpriseResult r = g.rollUltraBoost();
        checkEq(0, r.getUltraBoostSteps(), "factor 0");
        check(!r.hasMoves(), "no move");
        checkEq(before, pawn(g, 0, 0).getPathIndex(), "pawn did not move");
        checkEq("Blue", g.getCurrentPlayer().getName(), "turn passed");
    }

    private static void outOfPhase() {
        Spot s = spot(Difficulty.MEDIUM, reachable(2));
        Game g = game(s.board, deck(SurpriseEvent.BACK_THREE), 2);
        try {
            g.resolveSurprise();
            throw new AssertionError("resolved without a surprise");
        } catch (IllegalStateException expected) {
            // ok
        }
        Game u = land(s, deck(SurpriseEvent.ULTRA_BOOST, 2), 2, 1);
        try {
            u.resolveSurprise();
            throw new AssertionError("UltraBoost resolved without the special roll");
        } catch (IllegalStateException expected) {
            // ok
        }
        try {
            u.move(u.getLegalMoves().isEmpty() ? null : u.getLegalMoves().get(0));
            throw new AssertionError("moved while the surprise waits");
        } catch (IllegalStateException expected) {
            // ok
        }
    }

    private static void backThreeOntoQuestion() {
        Spot s = spot(Difficulty.MEDIUM, b -> {
            int shared = b.getSharedTrack().size();
            for (int i = 5; i < shared - 2; i++) {
                if (isSurprise(b, i) && plain(b, i - 1) && plain(b, i - 2)
                        && green(b, i - 3) instanceof QuestionSquare) {
                    return i;
                }
            }
            return -1;
        });
        List<Question> qs = new ArrayList<Question>();
        int id = 1;
        for (QuestionLevel l : QuestionLevel.values()) {
            qs.add(QuestionFactory.forLevel(l).create(id++, "Q", java.util.Arrays.asList("a", "b", "c", "d"), 0));
        }
        List<Player> players = new ArrayList<Player>();
        players.add(new Player("Green", PlayerColor.GREEN, 0));
        players.add(new Player("Blue", PlayerColor.BLUE, 1));
        Game g = new Game(s.board, players, new ScriptedDie(2), () -> 0L, new QuestionDeck(qs, new Random(1)),
                deck(SurpriseEvent.BACK_THREE));
        Pawn p = pawn(g, 0, 0);
        p.setPathIndex(s.index - 2);
        g.roll();
        g.move(g.legalMoveFor(p));
        SurpriseResult r = g.resolveSurprise();
        check(r.isContinues(), "turn continues");
        checkEq(GamePhase.QUESTION, g.getPhase(), "question asked right away");
        check(g.getQuestionPawn() == p, "the owner answers");
    }

    // ------------------------------------------------------------- סיום

    private static void finishedSkipped() {
        Board b = BoardBuilder.build(Difficulty.EASY, new Random(3));
        List<Player> players = new ArrayList<Player>();
        players.add(new Player("Green", PlayerColor.GREEN, 0));
        players.add(new Player("Blue", PlayerColor.BLUE, 1));
        players.add(new Player("Red", PlayerColor.RED, 2));
        Game g = new Game(b, players, new ScriptedDie(1), () -> 0L, null, null);
        // לירוק נשאר כלי אחד, צעד אחד לפני הבית
        for (int i = 1; i < Player.PAWNS; i++) {
            g.getPlayers().get(0).getPawns().get(i).setPathIndex(b.getFinishIndex());
        }
        Pawn last = g.getPlayers().get(0).getPawns().get(0);
        last.setPathIndex(b.getFinishIndex() - 1);
        g.roll();
        MoveResult r = g.move(g.legalMoveFor(last));
        check(r.isPlayerFinished(), "green finished");
        checkEq(1, g.getPlayers().get(0).getPlace(), "first place");
        check(!r.isGameOver(), "two players still playing");
        checkEq("Blue", g.getCurrentPlayer().getName(), "blue");
        g.roll(); // כחול: 1, אין כלים על הלוח
        checkEq("Red", g.getCurrentPlayer().getName(), "red");
        g.roll();
        checkEq("Blue", g.getCurrentPlayer().getName(), "green is skipped");
    }

    private static void gameOver() {
        Board b = BoardBuilder.build(Difficulty.HARD, new Random(5));
        List<Player> players = new ArrayList<Player>();
        players.add(new Player("Green", PlayerColor.GREEN, 0));
        players.add(new Player("Blue", PlayerColor.BLUE, 1));
        final long[] now = {1000L};
        final List<List<Player>> told = new ArrayList<List<Player>>();
        Game real = new Game(b, players, new ScriptedDie(1), () -> now[0], null, null);
        for (int i = 1; i < Player.PAWNS; i++) {
            real.getPlayers().get(0).getPawns().get(i).setPathIndex(b.getFinishIndex());
        }
        Pawn last = real.getPlayers().get(0).getPawns().get(0);
        last.setPathIndex(b.getFinishIndex() - 1);
        real.addListener(new GameListener() {
            @Override
            public void onGameOver(List<Player> ranking) {
                told.add(ranking);
            }
        });
        now[0] = 61000L;
        real.roll();
        MoveResult r = real.move(real.legalMoveFor(last));
        check(r.isGameOver() && real.isOver(), "game over with one player left");
        checkEq(GamePhase.OVER, real.getPhase(), "over phase");
        checkEq(1, told.size(), "listeners told once");
        List<Player> ranking = told.get(0);
        checkEq(2, ranking.size(), "everyone ranked");
        checkEq("Green", ranking.get(0).getName(), "winner");
        checkEq("Blue", ranking.get(1).getName(), "last player gets the last place");
        checkEq(2, ranking.get(1).getPlace(), "last place number");
        checkEq(60000L, real.getElapsedMillis(), "duration");
        now[0] = 999999L;
        checkEq(60000L, real.getElapsedMillis(), "time stops at the end");
        checkEq(Difficulty.HARD, real.getBoard().getDifficulty(), "difficulty for the end screen");
    }

    // ------------------------------------------------------------- עמידות

    private static void randomGamesFinish() {
        Map<SurpriseEvent, Integer> seen = new EnumMap<SurpriseEvent, Integer>(SurpriseEvent.class);
        for (Difficulty d : new Difficulty[]{Difficulty.MEDIUM, Difficulty.HARD}) {
            for (int seed = 0; seed < 15; seed++) {
                Random r = new Random(seed);
                Board b = BoardBuilder.build(d, r);
                List<Player> players = new ArrayList<Player>();
                PlayerColor[] colors = PlayerColor.values();
                for (int i = 0; i < colors.length; i++) {
                    players.add(new Player("P" + i, colors[i], i));
                }
                Game g = new Game(b, players, new StepDie(r), () -> 0L,
                        new QuestionDeck(DefaultQuestions.all(), r), new SurpriseDeck(r));
                for (int step = 0; !g.isOver(); step++) {
                    check(step < 500000, d + " seed " + seed + " did not finish");
                    switch (g.getPhase()) {
                        case ROLL:
                            g.roll();
                            break;
                        case MOVE:
                            List<Move> moves = g.getLegalMoves();
                            g.move(moves.get(r.nextInt(moves.size())));
                            break;
                        case QUESTION:
                            g.answer(r.nextInt(Question.ANSWERS));
                            break;
                        case SURPRISE:
                            SurpriseEvent e = g.getPendingSurprise();
                            Integer c = seen.get(e);
                            seen.put(e, c == null ? 1 : c + 1);
                            if (e == SurpriseEvent.ULTRA_BOOST) {
                                g.rollUltraBoost();
                            } else if (e == SurpriseEvent.MOVE_OPPONENT && !g.surpriseOpponents().isEmpty()) {
                                List<Player> opp = g.surpriseOpponents();
                                List<Pawn> targets = g.surpriseTargets(opp.get(r.nextInt(opp.size())));
                                g.moveOpponent(targets.get(r.nextInt(targets.size())));
                            } else {
                                g.resolveSurprise();
                            }
                            break;
                        default:
                            break;
                    }
                }
                checkEq(players.size(), g.getRanking().size(), "everyone ranked");
            }
        }
        checkEq(SurpriseEvent.values().length, seen.size(), "every event happened at least once");
    }
}

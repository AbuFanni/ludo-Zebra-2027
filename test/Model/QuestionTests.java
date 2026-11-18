package Model;

import static Model.ModelTests.check;
import static Model.ModelTests.checkEq;

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
 * בדיקות לשאלות (US-7): בדיקת תשובה וניקוד לפי רמה, יצירת שאלות לפי רמה (Factory Method), בלי
 * חזרות, קובץ חסר או פגום, ותנועת הכלי במשחק אחרי תשובה (FR-38 עד FR-43, FR-74).
 * <p>
 * כל בדיקה מסומנת T8.xx כמו ברשומת הבדיקות. רצות יחד עם שאר בדיקות המודל ({@link ModelTests}).
 * בבדיקות המשחק הלוח נבחר לפי seed קבוע שבו משבצת השאלה נמצאת במקום שהבדיקה צריכה, כך שהתוצאה
 * תמיד זהה.
 *
 * @author Mohamed Shini (Zebra) – Iteration 2, US-7 (FR-38–FR-43)
 */
final class QuestionTests {

    private QuestionTests() {
    }

    static void runAll() {
        // בדיקת תשובה וניקוד (FR-40, FR-41)
        ModelTests.run("T8.01 FR-40 correct answer moves forward 1/2/3 by level", QuestionTests::correctSteps);
        ModelTests.run("T8.02 FR-40 wrong answer moves back the same 1/2/3", QuestionTests::wrongSteps);
        ModelTests.run("T8.03 FR-41 exactly one answer is correct, and the check names it", QuestionTests::oneCorrect);
        ModelTests.run("T8.04 FR-38 answer outside 1-4 is rejected", QuestionTests::choiceRange);
        // Factory Method
        ModelTests.run("T8.05 Factory Method: one creator per level, questions get its level", QuestionTests::factoryPerLevel);
        ModelTests.run("T8.06 Factory Method rejects broken questions (FR-43, FR-71)", QuestionTests::factoryRejects);
        // בלי חזרות (FR-42)
        ModelTests.run("T8.07 FR-42 no question repeats before all of its level were shown", QuestionTests::noRepeatInRound);
        ModelTests.run("T8.08 FR-42 last question of a round is not the first of the next", QuestionTests::noRepeatAcrossRounds);
        ModelTests.run("T8.09 FR-42 each level has its own deck", QuestionTests::separateLevels);
        // קובץ השאלות (FR-43, FR-74)
        ModelTests.run("T8.10 FR-43 questions file round trip keeps every field (UTF-8)", QuestionTests::fileRoundTrip);
        ModelTests.run("T8.11 FR-43 missing questions file is an error", QuestionTests::missingFile);
        ModelTests.run("T8.12 FR-43 damaged line is an error with its line number", QuestionTests::damagedLines);
        ModelTests.run("T8.13 FR-43 file with no questions at a level is an error", QuestionTests::missingLevel);
        ModelTests.run("T8.14 FR-43 comments, blank lines and BOM are skipped", QuestionTests::commentsSkipped);
        ModelTests.run("T8.15 FR-74 initial bank: 30 questions, 10 per level, valid in a file", QuestionTests::defaultBank);
        // במשחק (FR-38 עד FR-41, FR-26, FR-27, FR-29, FR-33)
        ModelTests.run("T8.16 FR-38 landing on ? waits for the pawn owner's answer", QuestionTests::landingAsks);
        ModelTests.run("T8.17 FR-40 correct answer moves forward, then the turn passes", QuestionTests::gameCorrect);
        ModelTests.run("T8.18 FR-40 FR-29 wrong answer moves back and can capture", QuestionTests::gameWrong);
        ModelTests.run("T8.19 FR-39 capture on ? happens first, wrong answer keeps it", QuestionTests::captureBeforeQuestion);
        ModelTests.run("T8.20 FR-26 going back stops at the start square", QuestionTests::backStopsAtStart);
        ModelTests.run("T8.21 FR-33 answer move stops before a Double Block", QuestionTests::stopsBeforeBlock);
        ModelTests.run("T8.22 FR-27 landing on another ? asks again in the same turn", QuestionTests::chainedQuestion);
        ModelTests.run("T8.23 FR-17 a 6 still gives an extra roll after the question", QuestionTests::sixKeepsExtraRoll);
        ModelTests.run("T8.24 game without questions treats ? as a regular square", QuestionTests::noDeck);
        ModelTests.run("T8.25 answering outside the question phase is rejected", QuestionTests::answerOnlyWhenAsked);
        ModelTests.run("T8.26 random medium/hard games with random answers always finish", QuestionTests::randomGamesFinish);
    }

    // ------------------------------------------------------------- עזר

    private static Question q(int id, QuestionLevel level, int correct) {
        return QuestionFactory.forLevel(level).create(id, "Question " + id,
                Arrays.asList("A" + id, "B" + id, "C" + id, "D" + id), correct);
    }

    /** חפיסה עם שאלה אחת בכל רמה, והתשובה הנכונה היא תמיד הראשונה. */
    private static QuestionDeck oneEach() {
        return new QuestionDeck(Arrays.asList(
                q(1, QuestionLevel.EASY, 0), q(2, QuestionLevel.MEDIUM, 0), q(3, QuestionLevel.HARD, 0)), new Random(1));
    }

    private static final int RIGHT = 0;
    private static final int WRONG = 1;

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

    private static Game game(Board b, QuestionDeck deck, int... rolls) {
        List<Player> players = new ArrayList<Player>();
        players.add(new Player("Green", PlayerColor.GREEN, 0));
        players.add(new Player("Blue", PlayerColor.BLUE, 1));
        return new Game(b, players, new ScriptedDie(rolls), () -> 0L, deck);
    }

    private static Pawn pawn(Game g, int player, int n) {
        return g.getPlayers().get(player).getPawns().get(n);
    }

    private static QuestionLevel levelAt(Board b, int index) {
        Square s = b.getPath(PlayerColor.GREEN).get(index);
        return s instanceof QuestionSquare ? ((QuestionSquare) s).getLevel() : null;
    }

    private static boolean isQuestion(Board b, int index) {
        return levelAt(b, index) != null;
    }

    /** אין משבצת שאלה בטווח [from, to] במסלול הירוק. */
    private static boolean noQuestions(Board b, int from, int to) {
        for (int i = Math.max(0, from); i <= to; i++) {
            if (isQuestion(b, i)) {
                return false;
            }
        }
        return true;
    }

    /** @return true אם המשבצת במסלול הירוק נמצאת גם במסלול הכחול, כדי שאפשר לשים עליה כלי כחול */
    private static boolean onBluePath(Board b, int greenIndex) {
        return b.getPath(PlayerColor.BLUE).indexOf(b.getPath(PlayerColor.GREEN).get(greenIndex)) >= 0;
    }

    private static void putBlue(Game g, Board b, int n, int greenIndex) {
        pawn(g, 1, n).setPathIndex(b.getPath(PlayerColor.BLUE).indexOf(b.getPath(PlayerColor.GREEN).get(greenIndex)));
    }

    interface Finder {
        /** @return אינדקס משבצת השאלה שמתאימה לבדיקה במסלול הירוק, או -1 */
        int find(Board b);
    }

    /** לוח ומשבצת שאלה שמתאימים לבדיקה. */
    private static final class Spot {
        final Board board;
        final int index;
        final QuestionLevel level;

        Spot(Board board, int index) {
            this.board = board;
            this.index = index;
            this.level = levelAt(board, index);
        }

        int steps() {
            return level.getSteps();
        }
    }

    /** עובר על seeds קבועים עד שנמצא לוח שבו יש משבצת שאלה שמתאימה לבדיקה. */
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

    /** משבצת שאלה שאפשר להגיע אליה ב-2 ולזוז ממנה steps קדימה ואחורה בלי משבצות שאלה אחרות. */
    private static int plainQuestion(Board b) {
        int shared = b.getSharedTrack().size();
        for (int i = 4; i < shared - 4; i++) {
            QuestionLevel l = levelAt(b, i);
            if (l == null) {
                continue;
            }
            int s = l.getSteps();
            if (i - Math.max(2, s) >= 0 && noQuestions(b, i - Math.max(2, s), i - 1) && noQuestions(b, i + 1, i + s)) {
                return i;
            }
        }
        return -1;
    }

    // ------------------------------------------------------------- בדיקת תשובה

    private static void correctSteps() {
        int[] expected = {1, 2, 3};
        QuestionLevel[] levels = QuestionLevel.values();
        for (int i = 0; i < levels.length; i++) {
            AnswerCheck c = q(10 + i, levels[i], 2).check(2);
            check(c.isCorrect(), levels[i] + " correct");
            checkEq(expected[i], c.getSteps(), levels[i] + " steps");
            checkEq(expected[i], c.getSignedSteps(), levels[i] + " signed steps");
        }
    }

    private static void wrongSteps() {
        int[] expected = {-1, -2, -3};
        QuestionLevel[] levels = QuestionLevel.values();
        for (int i = 0; i < levels.length; i++) {
            AnswerCheck c = q(10 + i, levels[i], 2).check(3);
            check(!c.isCorrect(), levels[i] + " wrong");
            checkEq(expected[i], c.getSignedSteps(), levels[i] + " signed steps");
        }
    }

    private static void oneCorrect() {
        for (int correct = 0; correct < 4; correct++) {
            Question question = q(1, QuestionLevel.MEDIUM, correct);
            int right = 0;
            for (int choice = 0; choice < 4; choice++) {
                AnswerCheck c = question.check(choice);
                checkEq(choice, c.getChosenIndex(), "chosen index");
                checkEq(question.getAnswers().get(correct), c.getCorrectAnswer(), "correct answer shown");
                if (c.isCorrect()) {
                    right++;
                    checkEq(correct, choice, "the correct choice");
                }
            }
            checkEq(1, right, "exactly one correct answer");
        }
    }

    private static void choiceRange() {
        Question question = q(1, QuestionLevel.EASY, 0);
        for (int bad : new int[]{-1, 4, 99}) {
            try {
                question.check(bad);
                throw new AssertionError("choice " + bad + " accepted");
            } catch (IllegalArgumentException expected) {
                // ok
            }
        }
    }

    // ------------------------------------------------------------- Factory Method

    private static void factoryPerLevel() {
        Set<Class<?>> creators = new HashSet<Class<?>>();
        for (QuestionLevel level : QuestionLevel.values()) {
            QuestionFactory f = QuestionFactory.forLevel(level);
            check(f == QuestionFactory.forLevel(level), level + " same creator every time");
            creators.add(f.getClass());
            Question question = f.create(7, "  text  ", Arrays.asList(" a ", "b", "c", "d"), 3);
            checkEq(level, question.getLevel(), "level");
            checkEq("text", question.getText(), "text trimmed");
            checkEq("a", question.getAnswers().get(0), "answer trimmed");
            checkEq("d", question.getCorrectAnswer(), "correct answer");
        }
        checkEq(3, creators.size(), "a different creator for each level");
    }

    private static void factoryRejects() {
        QuestionFactory f = QuestionFactory.forLevel(QuestionLevel.HARD);
        List<String> ok = Arrays.asList("a", "b", "c", "d");
        Object[][] bad = {
                {0, "t", ok, 0, "id 0"},
                {1, "", ok, 0, "empty text"},
                {1, "   ", ok, 0, "blank text"},
                {1, null, ok, 0, "null text"},
                {1, "t", Arrays.asList("a", "b", "c"), 0, "3 answers"},
                {1, "t", Arrays.asList("a", "b", "c", "d", "e"), 0, "5 answers"},
                {1, "t", Arrays.asList("a", "", "c", "d"), 0, "empty answer"},
                {1, "t", Arrays.asList("a", "b", "A ", "d"), 0, "same answer twice"},
                {1, "t", ok, -1, "correct -1"},
                {1, "t", ok, 4, "correct 4"},
                {1, "a\tb", ok, 0, "tab in text"},
                {1, "t", Arrays.asList("a", "b\nx", "c", "d"), 0, "line break in answer"},
        };
        for (Object[] row : bad) {
            try {
                @SuppressWarnings("unchecked")
                List<String> answers = (List<String>) row[2];
                f.create((Integer) row[0], (String) row[1], answers, (Integer) row[3]);
                throw new AssertionError("accepted: " + row[4]);
            } catch (IllegalArgumentException expected) {
                check(expected.getMessage() != null && !expected.getMessage().isEmpty(), "message for " + row[4]);
            }
        }
    }

    // ------------------------------------------------------------- בלי חזרות

    private static List<Question> levelQuestions(QuestionLevel level, int n, int firstId) {
        List<Question> list = new ArrayList<Question>();
        for (int i = 0; i < n; i++) {
            list.add(q(firstId + i, level, 0));
        }
        return list;
    }

    private static void noRepeatInRound() {
        List<Question> all = levelQuestions(QuestionLevel.MEDIUM, 10, 1);
        QuestionDeck deck = new QuestionDeck(all, new Random(3));
        for (int round = 0; round < 3; round++) {
            Set<Integer> seen = new HashSet<Integer>();
            for (int i = 0; i < 10; i++) {
                check(seen.add(deck.next(QuestionLevel.MEDIUM).getId()), "repeat inside round " + round);
            }
            checkEq(10, seen.size(), "every question shown in round " + round);
        }
    }

    private static void noRepeatAcrossRounds() {
        for (int seed = 0; seed < 200; seed++) {
            QuestionDeck deck = new QuestionDeck(levelQuestions(QuestionLevel.EASY, 3, 1), new Random(seed));
            Question prev = null;
            for (int i = 0; i < 30; i++) {
                Question next = deck.next(QuestionLevel.EASY);
                check(next != prev, "same question twice in a row (seed " + seed + ")");
                prev = next;
            }
        }
    }

    private static void separateLevels() {
        List<Question> all = new ArrayList<Question>(levelQuestions(QuestionLevel.EASY, 2, 1));
        all.addAll(levelQuestions(QuestionLevel.HARD, 4, 10));
        QuestionDeck deck = new QuestionDeck(all, new Random(5));
        checkEq(2, deck.size(QuestionLevel.EASY), "easy size");
        checkEq(0, deck.size(QuestionLevel.MEDIUM), "medium size");
        checkEq(4, deck.size(QuestionLevel.HARD), "hard size");
        for (int i = 0; i < 8; i++) {
            checkEq(QuestionLevel.HARD, deck.next(QuestionLevel.HARD).getLevel(), "hard deck gives hard");
        }
        checkEq(QuestionLevel.EASY, deck.next(QuestionLevel.EASY).getLevel(), "easy deck gives easy");
        checkEq(1, deck.remaining(QuestionLevel.EASY), "easy round not affected by hard");
        try {
            deck.next(QuestionLevel.MEDIUM);
            throw new AssertionError("medium with no questions");
        } catch (IllegalStateException expected) {
            // ok
        }
    }

    // ------------------------------------------------------------- קובץ

    private static File tempFile() throws Exception {
        File f = Files.createTempFile("zebra-questions", ".dat").toFile();
        f.deleteOnExit();
        return f;
    }

    private static void write(File f, String... lines) throws Exception {
        Files.write(f.toPath(), Arrays.asList(lines), StandardCharsets.UTF_8);
    }

    private static void expectDamaged(File f, String what, String mustContain) {
        try {
            new QuestionRepository(f).loadAll();
            throw new AssertionError("accepted: " + what);
        } catch (QuestionFileException expected) {
            if (mustContain != null) {
                check(expected.getMessage().contains(mustContain),
                        what + ": message should contain <" + mustContain + "> but was <" + expected.getMessage() + ">");
            }
        }
    }

    private static void fileRoundTrip() throws Exception {
        File f = tempFile();
        List<Question> original = Arrays.asList(
                QuestionFactory.forLevel(QuestionLevel.EASY).create(1, "מה זה MVC?",
                        Arrays.asList("Model-View-Controller", "ב", "ג, עם פסיק", "ד"), 0),
                q(2, QuestionLevel.MEDIUM, 2), q(3, QuestionLevel.HARD, 3));
        QuestionRepository repo = new QuestionRepository(f);
        repo.saveAll(original);
        List<Question> loaded = repo.loadAll();
        checkEq(original.size(), loaded.size(), "count");
        for (int i = 0; i < original.size(); i++) {
            Question a = original.get(i);
            Question b = loaded.get(i);
            checkEq(a.getId(), b.getId(), "id");
            checkEq(a.getLevel(), b.getLevel(), "level");
            checkEq(a.getText(), b.getText(), "text");
            checkEq(a.getAnswers(), b.getAnswers(), "answers");
            checkEq(a.getCorrectIndex(), b.getCorrectIndex(), "correct");
        }
    }

    private static void missingFile() throws Exception {
        File f = tempFile();
        check(f.delete(), "temp file deleted");
        QuestionRepository repo = new QuestionRepository(f);
        check(!repo.exists(), "file does not exist");
        expectDamaged(f, "missing file", f.getName());
    }

    private static void damagedLines() throws Exception {
        String e = "1\tEASY\tq\ta\tb\tc\td\t1";
        String m = "2\tMEDIUM\tq\ta\tb\tc\td\t2";
        String h = "3\tHARD\tq\ta\tb\tc\td\t3";
        String[][] cases = {
                {"too few fields", "4\tEASY\tq\ta\tb\tc\t1"},
                {"too many fields", "4\tEASY\tq\ta\tb\tc\td\t1\tx"},
                {"unknown level", "4\tEXPERT\tq\ta\tb\tc\td\t1"},
                {"id not a number", "x\tEASY\tq\ta\tb\tc\td\t1"},
                {"correct not a number", "4\tEASY\tq\ta\tb\tc\td\tA"},
                {"correct out of range", "4\tEASY\tq\ta\tb\tc\td\t5"},
                {"empty answer", "4\tEASY\tq\ta\t\tc\td\t1"},
                {"duplicate id", "1\tEASY\tq2\ta\tb\tc\td\t1"},
        };
        File f = tempFile();
        for (String[] c : cases) {
            write(f, QuestionRepository.HEADER, e, m, h, c[1]);
            expectDamaged(f, c[0], "שורה 5");
        }
    }

    private static void missingLevel() throws Exception {
        File f = tempFile();
        write(f, "1\tEASY\tq\ta\tb\tc\td\t1", "2\tHARD\tq\ta\tb\tc\td\t1");
        expectDamaged(f, "no medium questions", QuestionLevel.MEDIUM.getHebrewName());
        write(f, "");
        expectDamaged(f, "empty file", null);
    }

    private static void commentsSkipped() throws Exception {
        File f = tempFile();
        write(f, "﻿# header", "", "1\tEASY\tq\ta\tb\tc\td\t1", "   ", "# note",
                "2\tMEDIUM\tq\ta\tb\tc\td\t2", "3\tHARD\tq\ta\tb\tc\td\t3");
        checkEq(3, new QuestionRepository(f).loadAll().size(), "questions read");
    }

    private static void defaultBank() throws Exception {
        List<Question> bank = DefaultQuestions.all();
        checkEq(30, bank.size(), "questions in the initial bank");
        Set<Integer> ids = new HashSet<Integer>();
        Set<Integer> correctPositions = new HashSet<Integer>();
        for (Question question : bank) {
            check(ids.add(question.getId()), "unique id " + question.getId());
            correctPositions.add(question.getCorrectIndex());
        }
        for (QuestionLevel level : QuestionLevel.values()) {
            checkEq(10, QuestionRepository.count(bank, level), "questions at level " + level);
        }
        checkEq(4, correctPositions.size(), "the correct answer is not always in the same place");

        File f = tempFile();
        check(f.delete(), "temp file deleted");
        QuestionRepository repo = new QuestionRepository(f);
        check(repo.createIfMissing(bank), "file created on first run");
        check(!repo.createIfMissing(bank), "existing file is not overwritten");
        checkEq(30, repo.loadAll().size(), "initial bank read back");
    }

    // ------------------------------------------------------------- במשחק

    private static void landingAsks() {
        Spot s = spot(Difficulty.MEDIUM, QuestionTests::plainQuestion);
        Game g = game(s.board, oneEach(), 2);
        Pawn green = pawn(g, 0, 0);
        green.setPathIndex(s.index - 2);
        g.roll();
        MoveResult r = g.move(g.legalMoveFor(green));
        check(r.getQuestion() != null, "move result has the question");
        checkEq(s.level, r.getQuestion().getLevel(), "question at the square's level");
        checkEq(GamePhase.QUESTION, g.getPhase(), "phase");
        check(g.getPendingQuestion() == r.getQuestion(), "pending question");
        check(g.getQuestionPawn() == green, "the pawn on the square");
        checkEq("Green", g.getCurrentPlayer().getName(), "turn not passed yet");
        check(!r.isExtraTurn() && !r.isGameOver(), "no end-of-turn yet");
        try {
            g.roll();
            throw new AssertionError("rolled while a question waits");
        } catch (IllegalStateException expected) {
            // ok
        }
    }

    private static void gameCorrect() {
        Spot s = spot(Difficulty.MEDIUM, QuestionTests::plainQuestion);
        Game g = game(s.board, oneEach(), 2);
        Pawn green = pawn(g, 0, 0);
        green.setPathIndex(s.index - 2);
        g.roll();
        g.move(g.legalMoveFor(green));
        AnswerResult r = g.answer(RIGHT);
        check(r.getCheck().isCorrect(), "correct");
        checkEq(s.index + s.steps(), green.getPathIndex(), "moved forward by level");
        checkEq(s.steps(), r.getStepsMoved(), "steps moved");
        checkEq(AnswerResult.Limit.NONE, r.getLimit(), "no limit");
        check(r.getNextQuestion() == null, "no next question");
        checkEq(GamePhase.ROLL, g.getPhase(), "back to rolling");
        checkEq("Blue", g.getCurrentPlayer().getName(), "turn passed");
        check(g.getPendingQuestion() == null, "no pending question");
    }

    private static void gameWrong() {
        Spot s = spot(Difficulty.HARD, b -> {
            int i = plainQuestion(b);
            if (i < 0) {
                return -1;
            }
            int back = i - levelAt(b, i).getSteps();
            return !b.getPath(PlayerColor.GREEN).get(back).isSafe() && onBluePath(b, back) ? i : -1;
        });
        int back = s.index - s.steps();
        Game g = game(s.board, oneEach(), 2);
        Pawn green = pawn(g, 0, 0);
        Pawn blue = pawn(g, 1, 0);
        green.setPathIndex(s.index - 2);
        // כלי יחיד של יריב במשבצת שהכלי חוזר אליה: נאכל גם בתנועה אחורה (FR-29)
        putBlue(g, s.board, 0, back);
        g.roll();
        g.move(g.legalMoveFor(green));
        AnswerResult r = g.answer(WRONG);
        check(!r.getCheck().isCorrect(), "wrong");
        checkEq(back, green.getPathIndex(), "moved back by level");
        checkEq(-s.steps(), r.getStepsMoved(), "steps moved");
        checkEq(1, r.getCaptured().size(), "captured going back");
        check(blue.isInBase(), "blue back in its castle");
        checkEq("Blue", g.getCurrentPlayer().getName(), "turn passed");
    }

    private static void captureBeforeQuestion() {
        Spot s = spot(Difficulty.MEDIUM, b -> {
            int i = plainQuestion(b);
            return i >= 0 && onBluePath(b, i) ? i : -1;
        });
        Game g = game(s.board, oneEach(), 2);
        Pawn green = pawn(g, 0, 0);
        Pawn blue = pawn(g, 1, 0);
        green.setPathIndex(s.index - 2);
        putBlue(g, s.board, 0, s.index);
        g.roll();
        MoveResult r = g.move(g.legalMoveFor(green));
        checkEq(1, r.getCaptured().size(), "captured before the question");
        check(blue.isInBase(), "blue in castle while the question is shown");
        g.answer(WRONG);
        check(blue.isInBase(), "wrong answer does not cancel the capture");
    }

    private static void backStopsAtStart() {
        // משבצת שאלה קרובה להתחלה: אחורה יש פחות צעדים מרמת השאלה
        Spot s = spot(Difficulty.HARD, b -> {
            for (int i = 1; i < 3; i++) {
                QuestionLevel l = levelAt(b, i);
                if (l != null && l.getSteps() > i && noQuestions(b, 0, i - 1)) {
                    return i;
                }
            }
            return -1;
        });
        // מגיעים למשבצת השאלה ב-1 או 2 צעדים ממשבצת ההתחלה
        Game g = game(s.board, oneEach(), s.index);
        Pawn green = pawn(g, 0, 0);
        green.setPathIndex(0);
        g.roll();
        g.move(g.legalMoveFor(green));
        checkEq(GamePhase.QUESTION, g.getPhase(), "question asked");
        AnswerResult r = g.answer(WRONG);
        checkEq(0, green.getPathIndex(), "stopped on the start square");
        checkEq(AnswerResult.Limit.START_SQUARE, r.getLimit(), "limit");
        checkEq(-s.index, r.getStepsMoved(), "moved back only to the start square");
    }

    private static void stopsBeforeBlock() {
        // שאלה בינונית או קשה, ו-Double Block של כחול שתי משבצות אחריה
        Spot s = spot(Difficulty.HARD, b -> {
            int i = plainQuestion(b);
            return i >= 0 && levelAt(b, i).getSteps() >= 2 && onBluePath(b, i + 2) ? i : -1;
        });
        Game g = game(s.board, oneEach(), 2);
        Pawn green = pawn(g, 0, 0);
        green.setPathIndex(s.index - 2);
        putBlue(g, s.board, 0, s.index + 2);
        putBlue(g, s.board, 1, s.index + 2);
        g.roll();
        g.move(g.legalMoveFor(green));
        AnswerResult r = g.answer(RIGHT);
        checkEq(s.index + 1, green.getPathIndex(), "stopped on the square before the block");
        checkEq(AnswerResult.Limit.DOUBLE_BLOCK, r.getLimit(), "limit");
        check(r.getCaptured().isEmpty(), "nothing captured");
    }

    private static void chainedQuestion() {
        // תשובה נכונה מביאה את הכלי בדיוק למשבצת שאלה נוספת
        Spot s = spot(Difficulty.HARD, b -> {
            int shared = b.getSharedTrack().size();
            for (int i = 4; i < shared - 4; i++) {
                QuestionLevel l = levelAt(b, i);
                if (l != null && noQuestions(b, i - 2, i - 1) && noQuestions(b, i + 1, i + l.getSteps() - 1)
                        && isQuestion(b, i + l.getSteps())) {
                    return i;
                }
            }
            return -1;
        });
        int second = s.index + s.steps();
        Game g = game(s.board, oneEach(), 2);
        Pawn green = pawn(g, 0, 0);
        green.setPathIndex(s.index - 2);
        g.roll();
        g.move(g.legalMoveFor(green));
        AnswerResult first = g.answer(RIGHT);
        checkEq(second, green.getPathIndex(), "on the second question square");
        check(first.getNextQuestion() != null, "next question");
        checkEq(levelAt(s.board, second), first.getNextQuestion().getLevel(), "level of the second square");
        checkEq(GamePhase.QUESTION, g.getPhase(), "still the question phase");
        checkEq("Green", g.getCurrentPlayer().getName(), "same turn");
        check(!first.isExtraTurn(), "turn not finished");
        g.answer(WRONG);
        checkEq(second - levelAt(s.board, second).getSteps(), green.getPathIndex(), "second answer moved it back");
    }

    private static void sixKeepsExtraRoll() {
        Spot s = spot(Difficulty.MEDIUM, b -> {
            int i = plainQuestion(b);
            return i >= 6 && noQuestions(b, i - 6, i - 1) ? i : -1;
        });
        Game g = game(s.board, oneEach(), 6);
        Pawn green = pawn(g, 0, 0);
        green.setPathIndex(s.index - 6);
        g.roll();
        g.move(g.legalMoveFor(green));
        checkEq(GamePhase.QUESTION, g.getPhase(), "question asked");
        AnswerResult r = g.answer(RIGHT);
        check(r.isExtraTurn(), "extra roll after the 6");
        checkEq(GamePhase.ROLL, g.getPhase(), "rolling again");
        checkEq("Green", g.getCurrentPlayer().getName(), "same player rolls again");
    }

    private static void noDeck() {
        Spot s = spot(Difficulty.MEDIUM, QuestionTests::plainQuestion);
        Game g = game(s.board, null, 2);
        Pawn green = pawn(g, 0, 0);
        green.setPathIndex(s.index - 2);
        g.roll();
        MoveResult r = g.move(g.legalMoveFor(green));
        check(r.getQuestion() == null, "no question");
        checkEq(GamePhase.ROLL, g.getPhase(), "phase");
        checkEq("Blue", g.getCurrentPlayer().getName(), "turn passed");
    }

    private static void answerOnlyWhenAsked() {
        Spot s = spot(Difficulty.MEDIUM, QuestionTests::plainQuestion);
        Game g = game(s.board, oneEach(), 2);
        try {
            g.answer(0);
            throw new AssertionError("answered without a question");
        } catch (IllegalStateException expected) {
            // ok
        }
        Pawn green = pawn(g, 0, 0);
        green.setPathIndex(s.index - 2);
        g.roll();
        g.move(g.legalMoveFor(green));
        try {
            g.answer(4);
            throw new AssertionError("answer 5 accepted");
        } catch (IllegalArgumentException expected) {
            // ok
        }
        checkEq(GamePhase.QUESTION, g.getPhase(), "a bad answer leaves the question open");
        g.answer(RIGHT);
        try {
            g.answer(RIGHT);
            throw new AssertionError("answered twice");
        } catch (IllegalStateException expected) {
            // ok
        }
    }

    private static void randomGamesFinish() {
        for (Difficulty d : new Difficulty[]{Difficulty.MEDIUM, Difficulty.HARD}) {
            for (int seed = 0; seed < 10; seed++) {
                Random r = new Random(seed);
                Board b = BoardBuilder.build(d, r);
                List<Player> players = new ArrayList<Player>();
                PlayerColor[] colors = PlayerColor.values();
                for (int i = 0; i < colors.length; i++) {
                    players.add(new Player("P" + i, colors[i], i));
                }
                Game g = new Game(b, players, new StepDie(r), () -> 0L, new QuestionDeck(DefaultQuestions.all(), r));
                int questions = 0;
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
                            questions++;
                            g.answer(r.nextInt(Question.ANSWERS));
                            break;
                        default:
                            break;
                    }
                }
                check(questions > 0, d + " seed " + seed + ": questions were asked");
                checkEq(players.size(), g.getRanking().size(), "everyone ranked");
            }
        }
    }
}

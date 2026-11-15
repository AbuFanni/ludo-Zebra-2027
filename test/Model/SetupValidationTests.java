package Model;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * בדיקות למסך הגדרת המשחק: שם, צבע והודעות שגיאה (FR-04, FR-05, FR-06).
 * <p>
 * כל בדיקה מסומנת T2.xx כמו ברשומת הבדיקות של משימה 2 בדוח האיטרציה.
 * רצות יחד עם שאר בדיקות המודל ({@link ModelTests}), דרך build.sh / build.bat.
 *
 * @author Nimer Ziad (Zebra) – Iteration 1, SDP table 2
 */
final class SetupValidationTests {

    private static final String ENTER_NAME = "יש להזין שם";
    private static final String TOO_LONG = "השם ארוך מדי – עד 15 תווים";
    private static final String NO_COMMA = "אסור להשתמש בפסיק בשם";
    private static final String CHOOSE_COLOR = "יש לבחור צבע";

    private static final List<PlayerColor> TWO = Arrays.asList(PlayerColor.RED, PlayerColor.GREEN);
    private static final List<PlayerColor> FOUR =
            Arrays.asList(PlayerColor.RED, PlayerColor.GREEN, PlayerColor.YELLOW, PlayerColor.BLUE);

    private SetupValidationTests() {
    }

    static void runAll() {
        // FR-04: שם
        ModelTests.run("T2.01 name of 1 character is valid", SetupValidationTests::oneChar);
        ModelTests.run("T2.02 name of exactly 15 characters is valid", SetupValidationTests::fifteenChars);
        ModelTests.run("T2.03 Hebrew name of 15 characters is valid", SetupValidationTests::hebrewFifteen);
        ModelTests.run("T2.04 name of 16 characters is rejected", SetupValidationTests::sixteenChars);
        ModelTests.run("T2.05 empty name is rejected", SetupValidationTests::emptyName);
        ModelTests.run("T2.06 name of only spaces is rejected", SetupValidationTests::spacesOnly);
        ModelTests.run("T2.07 spaces around the name are removed", SetupValidationTests::trimmed);
        ModelTests.run("T2.08 comma at start, middle or end is rejected", SetupValidationTests::comma);
        ModelTests.run("T2.09 same name twice is rejected", SetupValidationTests::duplicateExact);
        ModelTests.run("T2.10 same name in other letter case is rejected", SetupValidationTests::duplicateCase);
        ModelTests.run("T2.11 same name with extra spaces is rejected", SetupValidationTests::duplicateSpaces);
        ModelTests.run("T2.12 duplicate names the earlier player", SetupValidationTests::duplicateNamesPlayer);
        ModelTests.run("T2.13 similar but different names are valid", SetupValidationTests::similarNames);
        ModelTests.run("T2.14 length counts characters, not Java chars", SetupValidationTests::emojiLength);
        // FR-05: צבע
        ModelTests.run("T2.15 exactly 4 colors: red, green, yellow, blue", SetupValidationTests::fourColors);
        ModelTests.run("T2.16 4 players with 4 different colors are valid", SetupValidationTests::fourPlayersValid);
        ModelTests.run("T2.17 same color twice is rejected on the later player", SetupValidationTests::duplicateColor);
        ModelTests.run("T2.18 missing color is rejected", SetupValidationTests::missingColor);
        // FR-06: אין התחלה עם שדה שגוי, הודעה ליד כל שדה
        ModelTests.run("T2.19 every wrong field gets its own message", SetupValidationTests::allErrorsTogether);
        ModelTests.run("T2.20 invalid setup gives no settings (game cannot start)", SetupValidationTests::noSettings);
        ModelTests.run("T2.21 hidden rows beyond the player count are ignored", SetupValidationTests::hiddenRows);
        ModelTests.run("T2.22 valid setup keeps names and colors in entry order", SetupValidationTests::validOrder);
    }

    // ------------------------------------------------------------- עזר

    private static SetupValidationResult two(String a, String b) {
        return GameSetupValidator.validate(2, Difficulty.EASY, Arrays.asList(a, b), TWO);
    }

    private static String repeat(String s, int times) {
        return String.join("", Collections.nCopies(times, s));
    }

    // ------------------------------------------------------------- FR-04

    private static void oneChar() {
        ModelTests.check(two("A", "B").isValid(), "names A and B should be valid");
    }

    private static void fifteenChars() {
        ModelTests.check(two("abcdefghijklmno", "B").isValid(), "15 characters should be valid");
    }

    private static void hebrewFifteen() {
        String name = "אבגדהוזחטיכלמנס";
        ModelTests.checkEq(15, GameSetupValidator.nameLength(name), "Hebrew name length");
        ModelTests.check(two(name, "B").isValid(), "15 Hebrew characters should be valid");
    }

    private static void sixteenChars() {
        ModelTests.checkEq(TOO_LONG, two("abcdefghijklmnop", "B").getNameErrors().get(0), "16 characters");
    }

    private static void emptyName() {
        ModelTests.checkEq(ENTER_NAME, two("", "B").getNameErrors().get(0), "empty name");
        ModelTests.checkEq(ENTER_NAME, two(null, "B").getNameErrors().get(0), "missing name");
    }

    private static void spacesOnly() {
        ModelTests.checkEq(ENTER_NAME, two("     ", "B").getNameErrors().get(0), "spaces only");
    }

    private static void trimmed() {
        SetupValidationResult r = two("  Nimer  ", "abcdefghijklmno   ");
        ModelTests.check(r.isValid(), "spaces around the name are not counted");
        ModelTests.checkEq("Nimer", r.getSettings().getNames().get(0), "stored name");
        ModelTests.checkEq("abcdefghijklmno", r.getSettings().getNames().get(1), "stored name");
    }

    private static void comma() {
        ModelTests.checkEq(NO_COMMA, two(",Nimer", "B").getNameErrors().get(0), "comma at start");
        ModelTests.checkEq(NO_COMMA, two("Ni,mer", "B").getNameErrors().get(0), "comma in middle");
        ModelTests.checkEq(NO_COMMA, two("Nimer,", "B").getNameErrors().get(0), "comma at end");
        ModelTests.checkEq(NO_COMMA, two("A", ",").getNameErrors().get(1), "only a comma");
    }

    private static void duplicateExact() {
        SetupValidationResult r = two("Nimer", "Nimer");
        ModelTests.check(!r.getNameErrors().containsKey(0), "first player keeps the name");
        ModelTests.checkEq("השם כבר נבחר ע\"י שחקן 1", r.getNameErrors().get(1), "second player");
    }

    private static void duplicateCase() {
        ModelTests.check(two("nimer", "NIMER").getNameErrors().containsKey(1), "nimer / NIMER");
    }

    private static void duplicateSpaces() {
        ModelTests.check(two("Nimer", "  Nimer ").getNameErrors().containsKey(1), "Nimer / '  Nimer '");
    }

    private static void duplicateNamesPlayer() {
        SetupValidationResult r = GameSetupValidator.validate(4, Difficulty.EASY,
                Arrays.asList("Wasim", "Nour", "Mohamed", "nour"), FOUR);
        ModelTests.checkEq(1, r.getNameErrors().size(), "only one name error");
        ModelTests.checkEq("השם כבר נבחר ע\"י שחקן 2", r.getNameErrors().get(3), "player 4 message");
    }

    private static void similarNames() {
        ModelTests.check(two("Nimer", "Nimer2").isValid(), "Nimer / Nimer2");
        ModelTests.check(two("Nimer Z", "Nimer").isValid(), "Nimer Z / Nimer");
    }

    private static void emojiLength() {
        String smile = "😀"; // אימוג'י אחד = שני char ב-Java
        ModelTests.checkEq(1, GameSetupValidator.nameLength(smile), "one emoji is one character");
        ModelTests.check(two(repeat(smile, 15), "B").isValid(), "15 emoji should be valid");
        ModelTests.checkEq(TOO_LONG, two(repeat(smile, 16), "B").getNameErrors().get(0), "16 emoji");
    }

    // ------------------------------------------------------------- FR-05

    private static void fourColors() {
        ModelTests.checkEq(4, PlayerColor.values().length, "number of colors");
        ModelTests.check(Arrays.asList(PlayerColor.values()).containsAll(FOUR), "red, green, yellow, blue");
    }

    private static void fourPlayersValid() {
        SetupValidationResult r = GameSetupValidator.validate(4, Difficulty.EASY,
                Arrays.asList("Wasim", "Mohamed", "Nimer", "Nour"), FOUR);
        ModelTests.check(r.isValid(), "4 players with 4 colors should be valid");
    }

    private static void duplicateColor() {
        SetupValidationResult r = GameSetupValidator.validate(3, Difficulty.EASY,
                Arrays.asList("A", "B", "C"), Arrays.asList(PlayerColor.BLUE, PlayerColor.RED, PlayerColor.BLUE));
        ModelTests.check(!r.getColorErrors().containsKey(0), "player 1 keeps blue");
        ModelTests.check(!r.getColorErrors().containsKey(1), "player 2 is fine");
        ModelTests.checkEq("הצבע כבר נבחר ע\"י שחקן 1", r.getColorErrors().get(2), "player 3 message");
    }

    private static void missingColor() {
        SetupValidationResult r = GameSetupValidator.validate(2, Difficulty.EASY,
                Arrays.asList("A", "B"), Arrays.asList(PlayerColor.RED, null));
        ModelTests.checkEq(CHOOSE_COLOR, r.getColorErrors().get(1), "missing color");
    }

    // ------------------------------------------------------------- FR-06

    private static void allErrorsTogether() {
        SetupValidationResult r = GameSetupValidator.validate(4, Difficulty.EASY,
                Arrays.asList("", "a,b", "abcdefghijklmnop", "OK"),
                Arrays.asList(PlayerColor.RED, PlayerColor.RED, null, PlayerColor.BLUE));
        ModelTests.checkEq(ENTER_NAME, r.getNameErrors().get(0), "row 1 name");
        ModelTests.checkEq(NO_COMMA, r.getNameErrors().get(1), "row 2 name");
        ModelTests.checkEq(TOO_LONG, r.getNameErrors().get(2), "row 3 name");
        ModelTests.check(!r.getNameErrors().containsKey(3), "row 4 name is fine");
        ModelTests.checkEq("הצבע כבר נבחר ע\"י שחקן 1", r.getColorErrors().get(1), "row 2 color");
        ModelTests.checkEq(CHOOSE_COLOR, r.getColorErrors().get(2), "row 3 color");
        ModelTests.check(!r.getColorErrors().containsKey(3), "row 4 color is fine");
    }

    private static void noSettings() {
        SetupValidationResult r = two("Nimer", "Nimer");
        ModelTests.check(!r.isValid(), "should be invalid");
        ModelTests.checkEq(null, r.getSettings(), "no settings, so the game cannot start");
    }

    private static void hiddenRows() {
        SetupValidationResult r = GameSetupValidator.validate(2, Difficulty.EASY,
                Arrays.asList("A", "B", "", "a,b"), Arrays.asList(PlayerColor.RED, PlayerColor.GREEN, PlayerColor.RED, null));
        ModelTests.check(r.isValid(), "rows 3-4 are not used with 2 players");
        ModelTests.checkEq(2, r.getSettings().getPlayerCount(), "player count");
    }

    private static void validOrder() {
        SetupValidationResult r = GameSetupValidator.validate(3, Difficulty.MEDIUM,
                Arrays.asList("Nour", "Nimer", "Wasim"),
                Arrays.asList(PlayerColor.YELLOW, PlayerColor.RED, PlayerColor.GREEN));
        GameSettings s = r.getSettings();
        ModelTests.checkEq(Arrays.asList("Nour", "Nimer", "Wasim"), s.getNames(), "names");
        ModelTests.checkEq(Arrays.asList(PlayerColor.YELLOW, PlayerColor.RED, PlayerColor.GREEN), s.getColors(), "colors");
        ModelTests.checkEq(Difficulty.MEDIUM, s.getDifficulty(), "difficulty");
    }
}

package Model;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static Model.PlayerColor.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * בדיקות יחידה ל-{@link GameSetupValidator} – FR-03 עד FR-08.
 */
class GameSetupValidatorTest {

    private final GameSetupValidator v = new GameSetupValidator();

    private SetupValidationResult run(int n, Difficulty d, List<String> names, List<PlayerColor> colors) {
        return v.validate(n, d, names, colors);
    }

    @Test
    void validTwoPlayers_createsLockedSettingsInEntryOrder() {
        SetupValidationResult r = run(2, Difficulty.EASY, List.of("נור", "וסים"), List.of(RED, BLUE));
        assertTrue(r.isValid());
        GameSettings s = r.getSettings();
        assertEquals(Difficulty.EASY, s.difficulty());
        assertEquals("נור", s.players().get(0).name());
        assertEquals(1, s.players().get(1).turnOrder());                       // FR-07
        assertThrows(UnsupportedOperationException.class, () -> s.players().clear()); // FR-08
    }

    @Test
    void playerCountOutsideTwoToFour_isRejected() {                             // FR-03
        assertFalse(run(1, Difficulty.EASY, List.of("a"), List.of(RED)).isValid());
        assertFalse(run(5, Difficulty.EASY, List.of("a", "b", "c", "d", "e"),
                List.of(RED, GREEN, YELLOW, BLUE, RED)).isValid());
    }

    @Test
    void missingDifficulty_isRejected() {
        SetupValidationResult r = run(2, null, List.of("a", "b"), List.of(RED, BLUE));
        assertTrue(r.errorFor(SetupValidationResult.DIFFICULTY).isPresent());
    }

    @Test
    void nameRules_emptyLongCommaDuplicate() {                                  // FR-04
        SetupValidationResult r = run(4, Difficulty.HARD,
                List.of("   ", "1234567890123456", "a,b", "Nour"), List.of(RED, GREEN, YELLOW, BLUE));
        assertEquals(GameSetupValidator.ERR_NAME_EMPTY, r.errorFor("name.0").orElseThrow());
        assertEquals(GameSetupValidator.ERR_NAME_LONG, r.errorFor("name.1").orElseThrow());
        assertEquals(GameSetupValidator.ERR_NAME_COMMA, r.errorFor("name.2").orElseThrow());
        assertTrue(r.errorFor("name.3").isEmpty());

        SetupValidationResult dup = run(2, Difficulty.EASY, List.of("Nour", " nour "), List.of(RED, BLUE));
        assertTrue(dup.errorFor("name.0").isEmpty());
        assertTrue(dup.errorFor("name.1").orElseThrow().contains("שחקן 1"));
    }

    @Test
    void fifteenCharacterName_isAccepted() {
        assertTrue(run(2, Difficulty.MEDIUM, List.of("123456789012345", "b"), List.of(RED, BLUE)).isValid());
    }

    @Test
    void duplicateOrMissingColor_isRejected() {                                 // FR-05
        SetupValidationResult r = run(3, Difficulty.EASY, List.of("a", "b", "c"),
                Arrays.asList(RED, RED, null));
        assertTrue(r.errorFor("color.0").isEmpty());
        assertTrue(r.errorFor("color.1").orElseThrow().contains("שחקן 1"));
        assertEquals(GameSetupValidator.ERR_COLOR_EMPTY, r.errorFor("color.2").orElseThrow());
    }

    @Test
    void invalidResult_doesNotGiveSettings() {                                  // FR-06
        SetupValidationResult r = run(2, Difficulty.EASY, List.of("", "b"), List.of(RED, BLUE));
        assertThrows(IllegalStateException.class, r::getSettings);
    }

    @Test
    void onlyFirstNPlayersAreChecked() {
        SetupValidationResult r = run(2, Difficulty.EASY, List.of("a", "b", "", ""),
                List.of(RED, BLUE, RED, RED));
        assertTrue(r.isValid());
        assertEquals(2, r.getSettings().players().size());
    }
}

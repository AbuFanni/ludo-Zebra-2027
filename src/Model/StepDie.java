package Model;

import java.util.Random;

/**
 * קוביית צעדים רגילה: 1 עד 6 בהסתברות שווה (FR-15).
 * מבוסס על {@code GameplayAndLogic.Dice} בפרויקט Ludo-Griniaris-Game (MIT), בלי האייקון (הוא ב-View).
 */
public class StepDie implements Die {

    private final Random random;

    public StepDie(Random random) {
        this.random = random;
    }

    @Override
    public int roll() {
        return random.nextInt(6) + 1;
    }
}

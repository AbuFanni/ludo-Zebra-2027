package Model;

import java.util.Random;

/**
 * Strategy: מאיפה מגיעים אירועי ההפתעה וקוביית ה-UltraBoost של אירוע ה-UltraBoost.
 * במשחק האירוע מוגרל לפי האחוזים ב-{@link SurpriseEvent} (FR-44); בבדיקות אפשר לקבוע אירוע
 * וקובייה, כמו ש-{@link Die} מאפשר קובייה קבועה.
 *
 * @author Nour Eldin Khatib (Zebra) – Iteration 2, US-8 (FR-44, FR-51, NFR-11)
 */
public final class SurpriseDeck {

    /** מגריל אירוע אחד. */
    public interface Drawer {
        SurpriseEvent draw();
    }

    private final Drawer drawer;
    private final Die ultraBoost;

    /**
     * @param drawer     מאיפה מגיע האירוע
     * @param ultraBoost קוביית ה-UltraBoost (1 עד 6) לאירוע ה-UltraBoost
     */
    public SurpriseDeck(Drawer drawer, Die ultraBoost) {
        this.drawer = drawer;
        this.ultraBoost = ultraBoost;
    }

    /** חפיסה אקראית: אירוע לפי האחוזים, ו-UltraBoost בהסתברות שווה לכל תוצאה (FR-15). */
    public SurpriseDeck(Random random) {
        this(() -> drawWeighted(random), () -> random.nextInt(6) + 1);
    }

    public SurpriseEvent draw() {
        return drawer.draw();
    }

    /** @return תוצאת קוביית ה-UltraBoost, 1 עד 6 */
    public int rollUltraBoost() {
        return ultraBoost.roll();
    }

    /**
     * מגריל אירוע לפי האחוזים: מספר אקראי 0 עד 99, והאירוע שהטווח המצטבר שלו מכיל אותו.
     */
    public static SurpriseEvent drawWeighted(Random random) {
        int r = random.nextInt(100);
        int sum = 0;
        for (SurpriseEvent e : SurpriseEvent.values()) {
            sum += e.getPercent();
            if (r < sum) {
                return e;
            }
        }
        throw new IllegalStateException("Surprise percentages do not add up to 100");
    }
}

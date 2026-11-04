package Model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * שחקן במשחק: שם, צבע, מקום בסדר התורות וארבעה כלים.
 * <p>
 * מבוסס על {@code GameplayAndLogic.Player} בפרויקט Ludo-Griniaris-Game (MIT). השם והצבע לא משתנים
 * אחרי שהמשחק התחיל (FR-08).
 */
public class Player {

    /** מספר הכלים לכל שחקן. */
    public static final int PAWNS = 4;

    private final String name;
    private final PlayerColor color;
    private final int turnOrder;
    private final List<Pawn> pawns;
    private int place;

    /**
     * @param name      שם השחקן
     * @param color     הצבע שלו
     * @param turnOrder המקום בסדר התורות, מ-0 (FR-07)
     */
    public Player(String name, PlayerColor color, int turnOrder) {
        this.name = name;
        this.color = color;
        this.turnOrder = turnOrder;
        List<Pawn> list = new ArrayList<Pawn>();
        for (int i = 0; i < PAWNS; i++) {
            list.add(new Pawn(this, i));
        }
        this.pawns = Collections.unmodifiableList(list);
    }

    public String getName() {
        return name;
    }

    public PlayerColor getColor() {
        return color;
    }

    public int getTurnOrder() {
        return turnOrder;
    }

    public List<Pawn> getPawns() {
        return pawns;
    }

    /** @return המקום בדירוג (1 ומעלה), או 0 אם השחקן עדיין משחק */
    public int getPlace() {
        return place;
    }

    void setPlace(int place) {
        this.place = place;
    }

    /** @return true אם השחקן סיים (כל הכלים בבית, או שנקבע לו המקום האחרון) */
    public boolean isDone() {
        return place > 0;
    }

    @Override
    public String toString() {
        return name;
    }
}

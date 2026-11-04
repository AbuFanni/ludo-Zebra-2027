package Model;

/**
 * משבצת על הלוח.
 * <p>
 * מבוסס על היררכיית התאים {@code Cells.Cell} בפרויקט Ludo-Griniaris-Game של Alexandros Plessias
 * (רישיון MIT, https://github.com/AlexandrosPlessias/Ludo-Griniaris-Game). שם כל תא היה
 * {@code JButton} שמחזיק את הכלים שעליו. כאן המשבצת היא אובייקט מודל בלבד, בלי ממשק (MVC):
 * היא יודעת רק מה הסוג שלה, למי היא שייכת והאם היא מוגנת. מיקום הכלים נשמר בכלים עצמם.
 */
public abstract class Square {

    private final Position position;
    private final PlayerColor owner;

    /**
     * @param position המיקום בלוח
     * @param owner    השחקן שהמשבצת שייכת לו, או null למשבצת משותפת
     */
    protected Square(Position position, PlayerColor owner) {
        this.position = position;
        this.owner = owner;
    }

    public Position getPosition() {
        return position;
    }

    /** @return הצבע שהמשבצת שייכת לו, או null */
    public PlayerColor getOwner() {
        return owner;
    }

    /** @return סוג המשבצת, לתצוגה ולבדיקות */
    public abstract SquareKind getKind();

    /** @return true אם אי אפשר לאכול כלי שעומד על המשבצת */
    public abstract boolean isSafe();

    /**
     * @return true אם רק הכלים של הבעלים יכולים להיכנס למשבצת (מסלול הבית ובית הסיום)
     */
    public boolean isPrivate() {
        return false;
    }

    @Override
    public String toString() {
        return getKind() + position.toString();
    }
}

package Model;

/**
 * Factory Method: יוצר את אובייקט המשבצת המתאים לפי הסוג.
 * כל יצירת משבצת עוברת כאן, כך שהוספת סוג משבצת חדש משנה מקום אחד בלבד.
 */
public final class SquareFactory {

    private SquareFactory() {
    }

    /**
     * @param kind     סוג המשבצת
     * @param position המיקום בלוח
     * @param owner    הבעלים (למשבצת התחלה, חץ, מסלול בית וסיום), אחרת null
     * @param level    רמת השאלה (רק למשבצת שאלה), אחרת null
     * @return משבצת חדשה
     */
    public static Square create(SquareKind kind, Position position, PlayerColor owner, QuestionLevel level) {
        switch (kind) {
            case SAFE:
                return new SafeSquare(position);
            case START:
                return new StartSquare(position, owner);
            case ARROW:
                return new ArrowSquare(position, owner);
            case HOME_PATH:
                return new HomePathSquare(position, owner);
            case FINISH:
                return new FinishSquare(position, owner);
            case QUESTION:
                return new QuestionSquare(position, level);
            case SURPRISE:
                return new SurpriseSquare(position);
            case REGULAR:
            default:
                return new RegularSquare(position);
        }
    }
}

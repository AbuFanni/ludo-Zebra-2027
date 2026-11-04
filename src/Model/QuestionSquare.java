package Model;

/**
 * משבצת שאלה (?) ברמה מסוימת. היא לא מוגנת.
 * באיטרציה 1 היא מוצגת על הלוח ומתנהגת כמשבצת רגילה; הצגת השאלה מתווספת באיטרציה 2.
 */
public class QuestionSquare extends Square {

    private final QuestionLevel level;

    public QuestionSquare(Position position, QuestionLevel level) {
        super(position, null);
        this.level = level;
    }

    public QuestionLevel getLevel() {
        return level;
    }

    @Override
    public SquareKind getKind() {
        return SquareKind.QUESTION;
    }

    @Override
    public boolean isSafe() {
        return false;
    }
}

package Controller;

import Model.GameSetupValidator;
import Model.SetupValidationResult;
import View.GameSetupPanel;

/**
 * מחבר את מסך ההגדרה למודל: קורא את השדות, מבקש מהמודל לבדוק אותם,
 * ומציג שגיאות ליד השדות (FR-06) או מתחיל משחק.
 */
public class SetupController {

    private final GameSetupPanel view;
    private final MainController main;
    private final GameSetupValidator validator = new GameSetupValidator();

    /**
     * @param view מסך ההגדרה
     * @param main ה-Controller הראשי, שמתחיל את המשחק
     */
    public SetupController(GameSetupPanel view, MainController main) {
        this.view = view;
        this.main = main;
        view.onStart(e -> submit());
        view.onBack(e -> main.showMainMenu());
    }

    /** בודק את השדות ומתחיל משחק רק אם הכול תקין. */
    void submit() {
        SetupValidationResult result = validator.validate(
                view.getPlayerCount(), view.getDifficulty(), view.getNames(), view.getColors());
        if (result.isValid()) {
            view.clearErrors();
            main.startGame(result.getSettings());
        } else {
            view.showErrors(result);
        }
    }
}

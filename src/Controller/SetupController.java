package Controller;

import Model.GameSetupValidator;
import Model.SetupValidationResult;
import View.GameSetupPanel;

/**
 * מסך ההגדרה: שולח את השדות לבדיקה במודל, ומציג את השגיאות או מתחיל משחק (FR-03 עד FR-08).
 *
 * @author Nour Eldin Khatib (Zebra) – Iteration 1, SDP table 2
 */
public class SetupController {

    private final GameSetupPanel view;
    private final MainController main;

    public SetupController(GameSetupPanel view, MainController main) {
        this.view = view;
        this.main = main;
    }

    void bind() {
        view.getStartButton().addActionListener(e -> onStart());
        view.getBackButton().addActionListener(e -> main.showMenu());
        view.onInputChanged(this::onInputChanged);
    }

    /**
     * אחרי שהוצגו שגיאות, כל שינוי בשדות בודק שוב ומעדכן את ההודעות, כך שהודעה נעלמת ברגע
     * שהשדה תוקן (FR-06). לפני הלחיצה הראשונה על "התחל משחק" לא מציגים שגיאות על טופס ריק.
     *
     * @author Nimer Ziad (Zebra) – Iteration 1, SDP table 2
     */
    private void onInputChanged() {
        if (!view.hasErrors()) {
            return;
        }
        SetupValidationResult result = GameSetupValidator.validate(
                view.getPlayerCount(), view.getDifficulty(), view.getNames(), view.getColors());
        if (result.isValid()) {
            view.clearErrors();
        } else {
            view.showErrors(result, false);
        }
    }

    private void onStart() {
        SetupValidationResult result = GameSetupValidator.validate(
                view.getPlayerCount(), view.getDifficulty(), view.getNames(), view.getColors());
        if (result.isValid()) {
            view.clearErrors();
            main.startGame(result.getSettings());
        } else {
            view.showErrors(result);
        }
    }
}

package Controller;

import java.io.IOException;

import Model.HistoryRepository;
import View.HistoryPanel;

/**
 * מסך ההיסטוריה: קורא את קובץ ה-CSV ומציג אותו.
 */
public class HistoryController {

    private final HistoryPanel view;
    private final HistoryRepository history;

    public HistoryController(HistoryPanel view, HistoryRepository history) {
        this.view = view;
        this.history = history;
    }

    /** טוען מחדש את הקובץ. */
    public void refresh() {
        try {
            view.showRecords(history.loadAll(), history.getFile().getName());
        } catch (IOException e) {
            view.showError("לא הצלחנו לקרוא את " + history.getFile().getName() + ": " + e.getMessage());
        }
    }
}

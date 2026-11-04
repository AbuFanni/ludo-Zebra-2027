package View;

import java.awt.BorderLayout;
import java.awt.ComponentOrientation;
import java.awt.FlowLayout;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import Model.Difficulty;
import Model.GameRecord;

/**
 * מסך היסטוריית המשחקים: טבלה של כל המשחקים שנשמרו בקובץ ה-CSV.
 */
public class HistoryPanel extends JPanel {

    private static final String[] COLUMNS = {
            "מס'", "רמה", "תאריך ושעה", "מקום 1", "מקום 2", "מקום 3", "מקום 4", "משך"
    };

    private final DefaultTableModel model = new DefaultTableModel(COLUMNS, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(model);
    private final JLabel status = UiTheme.label(" ", UiTheme.SMALL, UiTheme.MUTED);
    private final JButton backButton = UiTheme.secondaryButton("חזרה לתפריט");

    public HistoryPanel() {
        setLayout(new BorderLayout(0, 12));
        setBackground(UiTheme.BACKGROUND);
        setBorder(new EmptyBorder(24, 40, 24, 40));
        add(UiTheme.label("היסטוריית משחקים", UiTheme.HEADING, UiTheme.PRIMARY), BorderLayout.NORTH);

        table.setFont(UiTheme.BODY);
        table.setRowHeight(28);
        table.getTableHeader().setFont(UiTheme.BODY.deriveFont(java.awt.Font.BOLD));
        table.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(JLabel.CENTER);
        table.setDefaultRenderer(Object.class, center);
        table.getColumnModel().getColumn(0).setPreferredWidth(50);
        table.getColumnModel().getColumn(2).setPreferredWidth(200);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        add(scroll, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        bottom.add(status, BorderLayout.LINE_START);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        buttons.setOpaque(false);
        buttons.add(backButton);
        bottom.add(buttons, BorderLayout.LINE_END);
        add(bottom, BorderLayout.SOUTH);
        applyComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
    }

    /** ממלא את הטבלה, מהמשחק האחרון לראשון. */
    public void showRecords(List<GameRecord> records, String fileName) {
        model.setRowCount(0);
        for (int i = records.size() - 1; i >= 0; i--) {
            GameRecord r = records.get(i);
            Difficulty d = Difficulty.fromCsvName(r.getDifficulty());
            model.addRow(new Object[]{
                    r.getGameId(), d == null ? r.getDifficulty() : d.getHebrewName(), r.getTimestamp().replace(";", "  "),
                    r.getPlace(1), r.getPlace(2), r.getPlace(3), r.getPlace(4), r.getDuration()
            });
        }
        status.setText(records.isEmpty() ? "עדיין לא נשמרו משחקים (" + fileName + ")"
                : records.size() + " משחקים · " + fileName);
    }

    /** מציג שגיאת קריאה של הקובץ. */
    public void showError(String message) {
        model.setRowCount(0);
        status.setText(message);
    }

    public JButton getBackButton() {
        return backButton;
    }
}

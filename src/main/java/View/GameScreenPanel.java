package View;

import Model.GameSettings;
import Model.Player;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.ComponentOrientation;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionListener;

/**
 * מסך המשחק – מעטפת לאיטרציה 1.
 * <p>
 * הלוח עצמו, הסימנים, התור והשעון (FR-12, FR-13) הם המשימה של מוחמד; הוא מחליף את
 * {@link #setBoard(javax.swing.JComponent)} ברכיב הלוח שלו. כרגע מוצגים השחקנים והרמה,
 * וכפתור "יציאה לתפריט" שמפעיל את אישור היציאה (FR-02).
 */
public class GameScreenPanel extends JPanel {

    private final JLabel header = UiTheme.label(" ", UiTheme.HEADING, UiTheme.PRIMARY);
    private final JPanel playersPanel = new JPanel(new GridLayout(1, 0, 12, 0));
    private final JPanel boardHolder = new JPanel(new BorderLayout());
    private final JButton exitButton = UiTheme.secondaryButton("יציאה לתפריט");

    /** בונה את מעטפת מסך המשחק. */
    public GameScreenPanel() {
        setLayout(new BorderLayout(0, 12));
        setBackground(UiTheme.BACKGROUND);
        setBorder(new EmptyBorder(16, 24, 16, 24));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(header, BorderLayout.LINE_START);
        JPanel exitWrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        exitWrap.setOpaque(false);
        exitWrap.add(exitButton);
        top.add(exitWrap, BorderLayout.LINE_END);

        playersPanel.setOpaque(false);
        JPanel north = new JPanel(new BorderLayout(0, 10));
        north.setOpaque(false);
        north.add(top, BorderLayout.NORTH);
        north.add(playersPanel, BorderLayout.SOUTH);
        add(north, BorderLayout.NORTH);

        boardHolder.setBackground(java.awt.Color.WHITE);
        boardHolder.setBorder(BorderFactory.createLineBorder(new java.awt.Color(0xD0C8B8)));
        JLabel placeholder = new JLabel("כאן יוצג לוח המשחק (מסך המשחק – בפיתוח)", SwingConstants.CENTER);
        placeholder.setFont(UiTheme.SUBTITLE);
        placeholder.setForeground(UiTheme.MUTED);
        boardHolder.add(placeholder, BorderLayout.CENTER);
        add(boardHolder, BorderLayout.CENTER);

        applyComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
    }

    /**
     * מציג את פרטי המשחק שהתחיל.
     *
     * @param settings ההגדרות הנעולות מהמסך הקודם
     */
    public void showGame(GameSettings settings) {
        header.setText("רמה: " + settings.difficulty().getHebrewName());
        header.setForeground(UiTheme.toAwt(settings.difficulty()).darker());
        playersPanel.removeAll();
        for (Player p : settings.players()) {
            JLabel l = new JLabel((p.turnOrder() + 1) + ". " + p.name(), SwingConstants.CENTER);
            l.setFont(UiTheme.BODY.deriveFont(java.awt.Font.BOLD));
            l.setOpaque(true);
            l.setBackground(UiTheme.toAwt(p.color()));
            l.setForeground(java.awt.Color.WHITE);
            l.setBorder(new EmptyBorder(8, 12, 8, 12));
            playersPanel.add(l);
        }
        playersPanel.applyComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        revalidate();
        repaint();
    }

    /**
     * מחליף את אזור הלוח ברכיב אחר (לשימוש מסך המשחק המלא).
     *
     * @param board רכיב הלוח
     */
    public void setBoard(javax.swing.JComponent board) {
        boardHolder.removeAll();
        boardHolder.add(board, BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    /** @param l פעולה ל"יציאה לתפריט" */
    public void onExitToMenu(ActionListener l) {
        exitButton.addActionListener(l);
    }
}

package View;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Image;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

import Model.Difficulty;
import Model.Game;
import Model.Pawn;
import Model.Player;

/**
 * מסך המשחק: הלוח, השחקן שתורו, הקובייה, השעון, מצב השחקנים ויומן הפעולות (FR-12, FR-13).
 * <p>
 * מבוסס על {@code WindowInterface.GamePanel} בפרויקט המקור (פאנל מידע עם תור, קובייה ויומן הודעות
 * לצד הלוח). כאן המסך רק מציג; ה-Controller מעדכן אותו ומאזין לכפתורים.
 */
public class GameScreenPanel extends JPanel {

    private final BoardPanel boardPanel = new BoardPanel();
    private final JLabel levelLabel = UiTheme.label(" ", UiTheme.HEADING, UiTheme.PRIMARY);
    private final JLabel clockLabel = UiTheme.label("00:00:00", UiTheme.SUBTITLE, UiTheme.TEXT);
    private final JLabel turnLabel = new JLabel(" ", SwingConstants.CENTER);
    private final JLabel diceLabel = new JLabel("", SwingConstants.CENTER);
    private final JLabel hintLabel = UiTheme.label(" ", UiTheme.SMALL, UiTheme.MUTED);
    private final JButton rollButton = UiTheme.primaryButton("הטל קובייה");
    private final JButton exitButton = UiTheme.secondaryButton("יציאה לתפריט");
    private final JPanel playersPanel = new JPanel();
    private final JTextArea log = new JTextArea();

    public GameScreenPanel() {
        setLayout(new BorderLayout(12, 0));
        setBackground(UiTheme.BACKGROUND);
        setBorder(new EmptyBorder(12, 12, 12, 12));

        add(boardPanel, BorderLayout.CENTER);

        JPanel side = new JPanel();
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        side.setBackground(UiTheme.SURFACE);
        side.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xE0D8C8)), new EmptyBorder(14, 14, 14, 14)));
        side.setPreferredSize(new Dimension(300, 600));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(levelLabel, BorderLayout.LINE_START);
        top.add(clockLabel, BorderLayout.LINE_END);
        addRow(side, top);
        side.add(Box.createVerticalStrut(12));

        turnLabel.setFont(UiTheme.HEADING);
        turnLabel.setOpaque(true);
        turnLabel.setForeground(Color.WHITE);
        turnLabel.setBorder(new EmptyBorder(8, 10, 8, 10));
        addRow(side, turnLabel);
        side.add(Box.createVerticalStrut(10));

        diceLabel.setPreferredSize(new Dimension(110, 110));
        addRow(side, diceLabel);
        side.add(Box.createVerticalStrut(8));
        addRow(side, rollButton);
        side.add(Box.createVerticalStrut(4));
        addRow(side, hintLabel);
        side.add(Box.createVerticalStrut(12));

        playersPanel.setOpaque(false);
        playersPanel.setLayout(new GridLayout(0, 1, 0, 4));
        addRow(side, playersPanel);
        side.add(Box.createVerticalStrut(10));

        log.setEditable(false);
        log.setLineWrap(true);
        log.setWrapStyleWord(true);
        log.setFont(UiTheme.SMALL);
        log.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        JScrollPane scroll = new JScrollPane(log);
        scroll.setPreferredSize(new Dimension(270, 180));
        scroll.setAlignmentX(Component.CENTER_ALIGNMENT);
        side.add(scroll);
        side.add(Box.createVerticalStrut(10));
        addRow(side, exitButton);

        add(side, BorderLayout.LINE_END);
        applyComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
    }

    private static void addRow(JPanel parent, java.awt.Container c) {
        if (c instanceof javax.swing.JComponent) {
            ((javax.swing.JComponent) c).setAlignmentX(Component.CENTER_ALIGNMENT);
        }
        c.setMaximumSize(new Dimension(Integer.MAX_VALUE, c.getPreferredSize().height));
        parent.add(c);
    }

    /** מכין את המסך למשחק חדש. */
    public void showGame(Game game) {
        boardPanel.setGame(game);
        Difficulty d = game.getBoard().getDifficulty();
        levelLabel.setText("רמה: " + d.getHebrewName());
        log.setText("");
        diceLabel.setIcon(null);
        diceLabel.setText("");
        hintLabel.setText(" ");
        updatePlayers(game);
        showTurn(game.getCurrentPlayer());
    }

    /** מציג את השחקן שתורו. */
    public void showTurn(Player player) {
        turnLabel.setText("תור: " + player.getName());
        turnLabel.setBackground(UiTheme.toAwt(player.getColor()));
        turnLabel.setForeground(player.getColor() == Model.PlayerColor.YELLOW ? UiTheme.TEXT : Color.WHITE);
    }

    /** מציג את תוצאת הקובייה, או מנקה אותה (0). */
    public void showDice(int value) {
        if (value < 1) {
            diceLabel.setIcon(null);
            diceLabel.setText("");
            return;
        }
        Image img = Images.dice(value);
        if (img != null) {
            diceLabel.setIcon(new ImageIcon(img.getScaledInstance(96, 96, Image.SCALE_SMOOTH)));
            diceLabel.setText("");
        } else {
            diceLabel.setIcon(null);
            diceLabel.setText(String.valueOf(value));
            diceLabel.setFont(UiTheme.TITLE);
        }
    }

    /** מעדכן את רשימת השחקנים: כמה כלים בבית, ומקום למי שסיים. */
    public void updatePlayers(Game game) {
        playersPanel.removeAll();
        List<Player> players = game.getPlayers();
        for (Player p : players) {
            int home = 0;
            for (Pawn pawn : p.getPawns()) {
                if (game.isFinished(pawn)) {
                    home++;
                }
            }
            String status = p.getPlace() > 0 ? "מקום " + p.getPlace() : home + "/4 בבית";
            JLabel l = new JLabel((p.getTurnOrder() + 1) + ". " + p.getName() + "  ·  " + status);
            l.setFont(UiTheme.BODY);
            l.setIcon(new ColorDot(UiTheme.toAwt(p.getColor())));
            l.setIconTextGap(8);
            l.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
            playersPanel.add(l);
        }
        playersPanel.revalidate();
        playersPanel.repaint();
    }

    /** מוסיף שורה ליומן הפעולות. */
    public void appendLog(String line) {
        if (log.getText().isEmpty()) {
            log.setText(line);
        } else {
            log.append("\n" + line);
        }
        log.setCaretPosition(log.getDocument().getLength());
    }

    public void setClock(String text) {
        clockLabel.setText(text);
    }

    public void setHint(String text) {
        hintLabel.setText(text == null || text.isEmpty() ? " " : text);
    }

    public void setRollEnabled(boolean enabled) {
        rollButton.setEnabled(enabled);
    }

    public BoardPanel getBoardPanel() {
        return boardPanel;
    }

    public JButton getRollButton() {
        return rollButton;
    }

    public JButton getExitButton() {
        return exitButton;
    }

    /** נקודת צבע קטנה ליד שם השחקן. */
    private static final class ColorDot implements javax.swing.Icon {
        private final Color color;

        ColorDot(Color color) {
            this.color = color;
        }

        @Override
        public void paintIcon(Component c, java.awt.Graphics g, int x, int y) {
            g.setColor(color);
            g.fillOval(x, y, 14, 14);
            g.setColor(color.darker());
            g.drawOval(x, y, 14, 14);
        }

        @Override
        public int getIconWidth() {
            return 15;
        }

        @Override
        public int getIconHeight() {
            return 15;
        }
    }
}

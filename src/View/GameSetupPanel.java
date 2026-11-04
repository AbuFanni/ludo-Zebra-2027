package View;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;

import Model.Difficulty;
import Model.GameSetupValidator;
import Model.PlayerColor;
import Model.SetupValidationResult;

/**
 * מסך הגדרת משחק: מספר שחקנים, רמה, ושם וצבע לכל שחקן, עם הודעת שגיאה ליד כל שדה שגוי
 * (FR-03 עד FR-06).
 * <p>
 * מבוסס על {@code WindowInterface.SettingsPanel} בפרויקט המקור (שדות כינוי ו-ComboBox של צבע לכל
 * שחקן). הבדיקה עצמה עברה ל-{@link GameSetupValidator} במודל, והמסך רק מציג את התוצאה.
 *
 * @author Nour Eldin Khatib (Zebra) – Iteration 1, SDP table 2
 */
public class GameSetupPanel extends JPanel {

    private static final PlayerColor[] DEFAULT_COLORS = {
            PlayerColor.RED, PlayerColor.GREEN, PlayerColor.YELLOW, PlayerColor.BLUE
    };

    private final JComboBox<Integer> playerCountBox = new JComboBox<Integer>(new Integer[]{2, 3, 4});
    private final Map<Difficulty, JRadioButton> difficultyButtons = new EnumMap<Difficulty, JRadioButton>(Difficulty.class);
    private final List<JTextField> nameFields = new ArrayList<JTextField>();
    private final List<JComboBox<PlayerColor>> colorBoxes = new ArrayList<JComboBox<PlayerColor>>();
    private final List<JLabel> nameErrors = new ArrayList<JLabel>();
    private final List<JLabel> colorErrors = new ArrayList<JLabel>();
    private final List<List<Component>> rows = new ArrayList<List<Component>>();
    private final JLabel generalError = UiTheme.label(" ", UiTheme.SMALL, UiTheme.ERROR);
    private final JButton startButton = UiTheme.primaryButton("התחל משחק");
    private final JButton backButton = UiTheme.secondaryButton("חזרה לתפריט");
    private final Border normalBorder;
    private final Border errorBorder = BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UiTheme.ERROR, 2), BorderFactory.createEmptyBorder(4, 6, 4, 6));

    public GameSetupPanel() {
        setLayout(new BorderLayout(0, 12));
        setBackground(UiTheme.BACKGROUND);
        setBorder(new EmptyBorder(24, 40, 24, 40));

        JLabel title = UiTheme.label("הגדרת משחק חדש", UiTheme.HEADING, UiTheme.PRIMARY);
        add(title, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(UiTheme.SURFACE);
        form.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xE0D8C8)), new EmptyBorder(18, 24, 18, 24)));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 8, 6, 8);
        c.anchor = GridBagConstraints.LINE_START;

        int row = 0;
        c.gridx = 0;
        c.gridy = row;
        form.add(UiTheme.label("מספר שחקנים", UiTheme.BODY, UiTheme.TEXT), c);
        c.gridx = 1;
        playerCountBox.setFont(UiTheme.BODY);
        playerCountBox.setPreferredSize(new Dimension(80, 32));
        form.add(playerCountBox, c);
        row++;

        c.gridx = 0;
        c.gridy = row;
        form.add(UiTheme.label("רמת קושי", UiTheme.BODY, UiTheme.TEXT), c);
        JPanel levels = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        levels.setOpaque(false);
        ButtonGroup group = new ButtonGroup();
        for (Difficulty d : Difficulty.values()) {
            JRadioButton rb = new JRadioButton(d.getHebrewName() + " – " + boardLabel(d));
            rb.setFont(UiTheme.BODY);
            rb.setOpaque(false);
            group.add(rb);
            levels.add(rb);
            difficultyButtons.put(d, rb);
        }
        c.gridx = 1;
        c.gridwidth = 2;
        form.add(levels, c);
        c.gridwidth = 1;
        row++;

        c.gridx = 1;
        c.gridy = row;
        form.add(UiTheme.label("שם (עד 15 תווים)", UiTheme.SMALL, UiTheme.MUTED), c);
        c.gridx = 2;
        form.add(UiTheme.label("צבע", UiTheme.SMALL, UiTheme.MUTED), c);
        row++;

        JTextField probe = new JTextField();
        normalBorder = probe.getBorder();
        for (int i = 0; i < 4; i++) {
            JLabel playerLabel = UiTheme.label("שחקן " + (i + 1), UiTheme.BODY, UiTheme.TEXT);
            JTextField name = new JTextField(16);
            name.setFont(UiTheme.BODY);
            name.setPreferredSize(new Dimension(240, 34));
            JComboBox<PlayerColor> color = new JComboBox<PlayerColor>(PlayerColor.values());
            color.setFont(UiTheme.BODY);
            color.setPreferredSize(new Dimension(150, 34));
            color.setRenderer(new ColorRenderer());
            JLabel nameErr = errorLabel();
            JLabel colorErr = errorLabel();

            c.gridy = row;
            c.gridx = 0;
            form.add(playerLabel, c);
            c.gridx = 1;
            JPanel nameStack = stack(name, nameErr);
            form.add(nameStack, c);
            c.gridx = 2;
            JPanel colorStack = stack(color, colorErr);
            form.add(colorStack, c);
            row++;

            nameFields.add(name);
            colorBoxes.add(color);
            nameErrors.add(nameErr);
            colorErrors.add(colorErr);
            List<Component> parts = new ArrayList<Component>();
            parts.add(playerLabel);
            parts.add(nameStack);
            parts.add(colorStack);
            rows.add(parts);
        }
        c.gridy = row;
        c.gridx = 0;
        c.gridwidth = 3;
        form.add(generalError, c);

        JPanel formWrap = new JPanel(new FlowLayout(FlowLayout.CENTER));
        formWrap.setOpaque(false);
        formWrap.add(form);
        add(formWrap, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 14, 0));
        buttons.setOpaque(false);
        buttons.add(startButton);
        buttons.add(backButton);
        add(buttons, BorderLayout.SOUTH);

        playerCountBox.addActionListener(e -> updateVisibleRows());
        applyComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        reset();
    }

    private static String boardLabel(Difficulty d) {
        switch (d) {
            case EASY:
                return "לוח רגיל";
            case MEDIUM:
                return "מסלול חיצוני נוסף";
            case HARD:
            default:
                return "שני מסלולים חיצוניים";
        }
    }

    private static JPanel stack(Component field, JLabel error) {
        JPanel p = new JPanel(new BorderLayout(0, 2));
        p.setOpaque(false);
        p.add(field, BorderLayout.CENTER);
        p.add(error, BorderLayout.SOUTH);
        return p;
    }

    private static JLabel errorLabel() {
        JLabel l = UiTheme.label(" ", UiTheme.SMALL, UiTheme.ERROR);
        l.setPreferredSize(new Dimension(240, 18));
        return l;
    }

    /** מחזיר לברירת המחדל: 2 שחקנים, רמה קלה, שמות ריקים, צבע שונה לכל שורה. */
    public void reset() {
        playerCountBox.setSelectedItem(2);
        difficultyButtons.get(Difficulty.EASY).setSelected(true);
        for (int i = 0; i < nameFields.size(); i++) {
            nameFields.get(i).setText("");
            colorBoxes.get(i).setSelectedItem(DEFAULT_COLORS[i]);
        }
        clearErrors();
        updateVisibleRows();
    }

    private void updateVisibleRows() {
        int count = getPlayerCount();
        for (int i = 0; i < rows.size(); i++) {
            for (Component part : rows.get(i)) {
                part.setVisible(i < count);
            }
        }
        revalidate();
        repaint();
    }

    public int getPlayerCount() {
        Object v = playerCountBox.getSelectedItem();
        return v instanceof Integer ? (Integer) v : 2;
    }

    /** @return הרמה שנבחרה, או null */
    public Difficulty getDifficulty() {
        for (Map.Entry<Difficulty, JRadioButton> e : difficultyButtons.entrySet()) {
            if (e.getValue().isSelected()) {
                return e.getKey();
            }
        }
        return null;
    }

    public List<String> getNames() {
        List<String> names = new ArrayList<String>();
        for (JTextField f : nameFields) {
            names.add(f.getText());
        }
        return names;
    }

    public List<PlayerColor> getColors() {
        List<PlayerColor> colors = new ArrayList<PlayerColor>();
        for (JComboBox<PlayerColor> b : colorBoxes) {
            colors.add((PlayerColor) b.getSelectedItem());
        }
        return colors;
    }

    /** מנקה את כל הודעות השגיאה. */
    public void clearErrors() {
        for (int i = 0; i < nameErrors.size(); i++) {
            nameErrors.get(i).setText(" ");
            colorErrors.get(i).setText(" ");
            nameFields.get(i).setBorder(normalBorder);
        }
        generalError.setText(" ");
    }

    /**
     * מציג כל שגיאה ליד השדה שלה ומעביר את הפוקוס לשדה השגוי הראשון (FR-06).
     */
    public void showErrors(SetupValidationResult result) {
        clearErrors();
        Component firstBad = null;
        for (Map.Entry<Integer, String> e : result.getNameErrors().entrySet()) {
            int i = e.getKey();
            nameErrors.get(i).setText(e.getValue());
            nameFields.get(i).setBorder(errorBorder);
            if (firstBad == null) {
                firstBad = nameFields.get(i);
            }
        }
        for (Map.Entry<Integer, String> e : result.getColorErrors().entrySet()) {
            colorErrors.get(e.getKey()).setText(e.getValue());
            if (firstBad == null) {
                firstBad = colorBoxes.get(e.getKey());
            }
        }
        if (result.getGeneralError() != null) {
            generalError.setText(result.getGeneralError());
        }
        if (firstBad != null) {
            firstBad.requestFocusInWindow();
        }
    }

    public JButton getStartButton() {
        return startButton;
    }

    public JButton getBackButton() {
        return backButton;
    }

    /** מציג ב-ComboBox ריבוע צבע ליד שם הצבע. */
    private static final class ColorRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            JLabel l = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            l.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
            if (value instanceof PlayerColor) {
                PlayerColor pc = (PlayerColor) value;
                l.setText(pc.getHebrewName());
                l.setIcon(new SwatchIcon(UiTheme.toAwt(pc)));
                l.setIconTextGap(8);
            }
            return l;
        }
    }

    /** ריבוע צבע קטן. */
    private static final class SwatchIcon implements javax.swing.Icon {
        private final Color color;

        SwatchIcon(Color color) {
            this.color = color;
        }

        @Override
        public void paintIcon(Component c, java.awt.Graphics g, int x, int y) {
            g.setColor(color);
            g.fillRoundRect(x, y, 14, 14, 4, 4);
            g.setColor(color.darker());
            g.drawRoundRect(x, y, 14, 14, 4, 4);
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

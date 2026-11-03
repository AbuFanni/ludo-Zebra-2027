package View;

import Model.Difficulty;
import Model.GameSetupValidator;
import Model.PlayerColor;
import Model.SetupValidationResult;

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
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import javax.swing.Icon;

/**
 * מסך הגדרת משחק (FR-03 עד FR-06): מספר שחקנים, רמה, ושם וצבע לכל שחקן.
 * <p>
 * לכל שדה יש תווית שגיאה צמודה, שמוצגת כשהבדיקה במודל נכשלת (FR-06).
 * המסך לא בודק בעצמו – הוא מעביר את הערכים ל-Controller ומציג את התוצאה.
 */
public class GameSetupPanel extends JPanel {

    private static final PlayerColor[] DEFAULT_COLORS = PlayerColor.values();

    private final JComboBox<Integer> playerCountBox = new JComboBox<>(new Integer[]{2, 3, 4});
    private final Map<Difficulty, JRadioButton> difficultyButtons = new EnumMap<>(Difficulty.class);
    private final ButtonGroup difficultyGroup = new ButtonGroup();
    private final JLabel countError = errorLabel();
    private final JLabel difficultyError = errorLabel();

    private final List<JLabel> playerLabels = new ArrayList<>();
    private final List<JTextField> nameFields = new ArrayList<>();
    private final List<JComboBox<PlayerColor>> colorBoxes = new ArrayList<>();
    private final List<JLabel> nameErrors = new ArrayList<>();
    private final List<JLabel> colorErrors = new ArrayList<>();

    private final JButton startButton = UiTheme.primaryButton("התחל משחק");
    private final JButton backButton = UiTheme.secondaryButton("חזרה");

    /** בונה את מסך ההגדרה עם ערכי ברירת מחדל: 2 שחקנים, רמה קלה, צבעים שונים. */
    public GameSetupPanel() {
        setLayout(new BorderLayout());
        setBackground(UiTheme.BACKGROUND);
        setBorder(new EmptyBorder(24, 40, 24, 40));

        JLabel title = UiTheme.label("הגדרת משחק חדש", UiTheme.HEADING, UiTheme.PRIMARY);
        title.setBorder(new EmptyBorder(0, 0, 16, 0));
        add(title, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 8, 6, 8);
        c.anchor = GridBagConstraints.LINE_START;
        int row = 0;

        // מספר שחקנים
        c.gridx = 0; c.gridy = row;
        form.add(UiTheme.label("מספר שחקנים:", UiTheme.BODY, UiTheme.TEXT), c);
        c.gridx = 1;
        playerCountBox.setFont(UiTheme.BODY);
        playerCountBox.setPreferredSize(new Dimension(80, 32));
        form.add(playerCountBox, c);
        c.gridx = 2;
        form.add(countError, c);
        row++;

        // רמת קושי
        c.gridx = 0; c.gridy = row;
        form.add(UiTheme.label("רמת קושי:", UiTheme.BODY, UiTheme.TEXT), c);
        JPanel levels = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        levels.setOpaque(false);
        for (Difficulty d : Difficulty.values()) {
            JRadioButton rb = new JRadioButton(d.getHebrewName());
            rb.setFont(UiTheme.BODY);
            rb.setOpaque(false);
            rb.setForeground(UiTheme.toAwt(d).darker());
            difficultyGroup.add(rb);
            difficultyButtons.put(d, rb);
            levels.add(rb);
        }
        c.gridx = 1;
        form.add(levels, c);
        c.gridx = 2;
        form.add(difficultyError, c);
        row++;

        // כותרות עמודות
        c.gridy = row; c.insets = new Insets(18, 8, 4, 8);
        c.gridx = 0; form.add(UiTheme.label("שחקן", UiTheme.SMALL, UiTheme.MUTED), c);
        c.gridx = 1; form.add(UiTheme.label("שם (עד 15 תווים, בלי פסיק)", UiTheme.SMALL, UiTheme.MUTED), c);
        c.gridx = 2; form.add(UiTheme.label("צבע", UiTheme.SMALL, UiTheme.MUTED), c);
        c.insets = new Insets(6, 8, 6, 8);
        row++;

        for (int i = 0; i < GameSetupValidator.MAX_PLAYERS; i++) {
            JTextField name = new JTextField(14);
            name.setFont(UiTheme.BODY);
            name.setHorizontalAlignment(SwingConstants.RIGHT);
            name.getAccessibleContext().setAccessibleName("שם שחקן " + (i + 1));

            JComboBox<PlayerColor> color = new JComboBox<>(PlayerColor.values());
            color.setFont(UiTheme.BODY);
            color.setRenderer(new ColorRenderer());
            color.setPreferredSize(new Dimension(130, 32));
            color.getAccessibleContext().setAccessibleName("צבע שחקן " + (i + 1));

            JLabel nameErr = errorLabel();
            JLabel colorErr = errorLabel();

            c.gridy = row;
            c.gridx = 0;
            JLabel playerLabel = UiTheme.label("שחקן " + (i + 1), UiTheme.BODY.deriveFont(java.awt.Font.BOLD), UiTheme.TEXT);
            form.add(playerLabel, c);
            c.gridx = 1;
            form.add(stack(name, nameErr), c);
            c.gridx = 2;
            form.add(stack(color, colorErr), c);

            playerLabels.add(playerLabel);
            nameFields.add(name);
            colorBoxes.add(color);
            nameErrors.add(nameErr);
            colorErrors.add(colorErr);
            row++;
        }

        JPanel formWrap = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        formWrap.setOpaque(false);
        formWrap.add(form);
        add(formWrap, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        buttons.setOpaque(false);
        startButton.setPreferredSize(new Dimension(200, 48));
        buttons.add(startButton);
        buttons.add(backButton);
        buttons.setBorder(new EmptyBorder(16, 0, 0, 0));
        add(buttons, BorderLayout.SOUTH);

        playerCountBox.addActionListener(e -> updateVisibleRows());
        applyComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        reset();
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

    /** מחזיר את המסך לברירת המחדל: 2 שחקנים, רמה קלה, שמות ריקים, צבעים שונים. */
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
        for (int i = 0; i < nameFields.size(); i++) {
            boolean visible = i < count;
            nameFields.get(i).getParent().setVisible(visible);
            colorBoxes.get(i).getParent().setVisible(visible);
            playerLabels.get(i).setVisible(visible);
        }
        revalidate();
        repaint();
    }

    /** @return מספר השחקנים שנבחר */
    public int getPlayerCount() {
        Object v = playerCountBox.getSelectedItem();
        return v instanceof Integer n ? n : 0;
    }

    /** @return הרמה שנבחרה, או {@code null} */
    public Difficulty getDifficulty() {
        for (Map.Entry<Difficulty, JRadioButton> e : difficultyButtons.entrySet()) {
            if (e.getValue().isSelected()) {
                return e.getKey();
            }
        }
        return null;
    }

    /** @return השמות כפי שהוזנו, לפי סדר השורות */
    public List<String> getNames() {
        List<String> names = new ArrayList<>();
        nameFields.forEach(f -> names.add(f.getText()));
        return names;
    }

    /** @return הצבעים שנבחרו, לפי סדר השורות */
    public List<PlayerColor> getColors() {
        List<PlayerColor> colors = new ArrayList<>();
        colorBoxes.forEach(b -> colors.add((PlayerColor) b.getSelectedItem()));
        return colors;
    }

    /** מנקה את כל הודעות השגיאה. */
    public void clearErrors() {
        countError.setText(" ");
        difficultyError.setText(" ");
        nameErrors.forEach(l -> l.setText(" "));
        colorErrors.forEach(l -> l.setText(" "));
        nameFields.forEach(f -> f.setBorder(UIManagerBorder.normal()));
    }

    /**
     * מציג ליד כל שדה לא תקין מה לתקן (FR-06), ומעביר את הפוקוס לשדה השגוי הראשון.
     *
     * @param result תוצאת הבדיקה מהמודל
     */
    public void showErrors(SetupValidationResult result) {
        clearErrors();
        result.errorFor(SetupValidationResult.PLAYER_COUNT).ifPresent(countError::setText);
        result.errorFor(SetupValidationResult.DIFFICULTY).ifPresent(difficultyError::setText);
        Component firstInvalid = null;
        for (int i = 0; i < nameFields.size(); i++) {
            String nameMsg = result.errorFor(SetupValidationResult.nameField(i)).orElse(null);
            if (nameMsg != null) {
                nameErrors.get(i).setText(nameMsg);
                nameFields.get(i).setBorder(UIManagerBorder.error());
                if (firstInvalid == null) {
                    firstInvalid = nameFields.get(i);
                }
            }
            String colorMsg = result.errorFor(SetupValidationResult.colorField(i)).orElse(null);
            if (colorMsg != null) {
                colorErrors.get(i).setText(colorMsg);
                if (firstInvalid == null) {
                    firstInvalid = colorBoxes.get(i);
                }
            }
        }
        if (firstInvalid != null) {
            firstInvalid.requestFocusInWindow();
        }
    }

    /** @param l פעולה ל"התחל משחק" */
    public void onStart(ActionListener l) {
        startButton.addActionListener(l);
    }

    /** @param l פעולה ל"חזרה" */
    public void onBack(ActionListener l) {
        backButton.addActionListener(l);
    }

    /** מציג ריבוע צבע ליד שם הצבע ברשימה. */
    private static final class ColorRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            JLabel l = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            l.setHorizontalAlignment(SwingConstants.RIGHT);
            l.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
            if (value instanceof PlayerColor pc) {
                l.setText(pc.getHebrewName());
                l.setIcon(new Swatch(UiTheme.toAwt(pc)));
                l.setIconTextGap(8);
            }
            return l;
        }
    }

    /** ריבוע צבע קטן. */
    private record Swatch(Color color) implements Icon {
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            g.setColor(color);
            g.fillRoundRect(x, y, 14, 14, 4, 4);
            g.setColor(color.darker());
            g.drawRoundRect(x, y, 14, 14, 4, 4);
        }

        @Override
        public int getIconWidth() {
            return 14;
        }

        @Override
        public int getIconHeight() {
            return 14;
        }
    }

    /** מסגרות לשדה טקסט: רגילה ומסומנת כשגויה. */
    private static final class UIManagerBorder {
        static javax.swing.border.Border normal() {
            return BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(0xB0B0B0)),
                    BorderFactory.createEmptyBorder(4, 6, 4, 6));
        }

        static javax.swing.border.Border error() {
            return BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(UiTheme.ERROR, 2),
                    BorderFactory.createEmptyBorder(3, 5, 3, 5));
        }
    }
}

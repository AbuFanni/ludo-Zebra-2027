package View;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Window;
import java.awt.geom.RoundRectangle2D;
import java.util.function.IntFunction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import Model.AnswerCheck;
import Model.AnswerResult;
import Model.Pawn;
import Model.Player;
import Model.Question;
import Model.QuestionLevel;

/**
 * חלון השאלה (FR-38, FR-41): רמת השאלה בצבע שלה, מי עונה, השאלה ו-4 כפתורי תשובה, בלי הגבלת זמן.
 * <p>
 * אחרי הבחירה התשובה נשלחת מיד למשחק והכלי זז על הלוח מאחורי החלון. הכפתורים ננעלים, התשובה
 * שנבחרה נצבעת בירוק או באדום, אחרי טעות מסומנת גם התשובה הנכונה, וההודעה מתארת את התנועה שקרתה
 * בפועל – כולל עצירה במשבצת ההתחלה או לפני Double Block. אחר כך מופיע כפתור "המשך".
 *
 * @author Mohamed Shini (Zebra) – Iteration 2, US-7 (FR-38, FR-41)
 */
public final class QuestionDialog {

    private static final Color CORRECT = new Color(0x1E8E3E);
    private static final Color WRONG = UiTheme.ERROR;
    private static final int TEXT_WIDTH = 470;

    private final Question question;
    private final IntFunction<AnswerResult> onAnswer;
    private final JDialog dialog;
    private final AnswerButton[] buttons = new AnswerButton[Question.ANSWERS];
    private final JLabel feedback = new JLabel(" ");
    private final JButton continueButton = UiTheme.primaryButton("המשך");
    private int chosen = -1;
    private AnswerResult result;

    private QuestionDialog(Component parent, Question question, Player answerer, IntFunction<AnswerResult> onAnswer) {
        this.question = question;
        this.onAnswer = onAnswer;
        Window owner = parent == null ? null : SwingUtilities.getWindowAncestor(parent);
        if (owner == null && parent instanceof Window) {
            owner = (Window) parent;
        }
        QuestionLevel level = question.getLevel();
        dialog = new JDialog(owner, "שאלה " + level.getHebrewName(), JDialog.DEFAULT_MODALITY_TYPE);
        // חייבים לענות: אי אפשר לסגור את החלון בלי לבחור תשובה
        dialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBackground(UiTheme.SURFACE);
        root.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        JPanel header = new JPanel(new BorderLayout(12, 0));
        header.setOpaque(false);
        JLabel badge = new JLabel("שאלה " + level.getHebrewName() + "  ·  " + steps(level.getSteps()));
        badge.setFont(UiTheme.BUTTON);
        badge.setForeground(UiTheme.TEXT);
        badge.setOpaque(true);
        badge.setBackground(UiTheme.question(level));
        badge.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
        header.add(badge, BorderLayout.LINE_START);
        JLabel who = new JLabel("עונה: " + answerer.getName());
        who.setFont(UiTheme.BODY);
        who.setForeground(UiTheme.MUTED);
        who.setIcon(new Dot(UiTheme.toAwt(answerer.getColor())));
        who.setIconTextGap(8);
        header.add(who, BorderLayout.LINE_END);
        add(root, header);
        root.add(Box.createVerticalStrut(16));

        JTextArea text = new JTextArea(question.getText());
        text.setEditable(false);
        text.setFocusable(false);
        text.setLineWrap(true);
        text.setWrapStyleWord(true);
        text.setOpaque(false);
        text.setFont(UiTheme.SUBTITLE.deriveFont(Font.BOLD));
        text.setForeground(UiTheme.TEXT);
        text.setSize(new Dimension(TEXT_WIDTH, Short.MAX_VALUE));
        text.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        add(root, text);
        root.add(Box.createVerticalStrut(16));

        JPanel answers = new JPanel(new GridLayout(0, 1, 0, 8));
        answers.setOpaque(false);
        for (int i = 0; i < Question.ANSWERS; i++) {
            final int index = i;
            // סימני RLM שומרים את המספר מימין גם כשהתשובה מתחילה במילה באנגלית
            buttons[i] = new AnswerButton("‏" + (i + 1) + ".‏  " + question.getAnswers().get(i));
            buttons[i].addActionListener(e -> choose(index));
            answers.add(buttons[i]);
        }
        add(root, answers);
        root.add(Box.createVerticalStrut(14));

        feedback.setFont(UiTheme.BODY);
        add(root, feedback);
        root.add(Box.createVerticalStrut(12));

        continueButton.setVisible(false);
        continueButton.addActionListener(e -> dialog.dispose());
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        bottom.add(continueButton, BorderLayout.LINE_START);
        add(root, bottom);

        root.applyComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        dialog.setContentPane(root);
        dialog.getRootPane().setDefaultButton(null);
        dialog.pack();
        dialog.setResizable(false);
        dialog.setLocationRelativeTo(owner);
    }

    /**
     * מציג את השאלה ומחכה לתשובה. בלחיצה על תשובה היא נשלחת מיד ל-onAnswer (שמזיז את הכלי), והחלון
     * נשאר פתוח עם המשוב עד שלוחצים "המשך".
     *
     * @param parent   החלון הראשי
     * @param question השאלה
     * @param answerer מי שעונה: בעל הכלי (FR-27)
     * @param onAnswer מקבל את האינדקס שנבחר (0 עד 3), מבצע את התשובה במשחק ומחזיר מה קרה
     * @return מה קרה אחרי התשובה
     */
    public static AnswerResult ask(Component parent, Question question, Player answerer,
                                   IntFunction<AnswerResult> onAnswer) {
        QuestionDialog d = new QuestionDialog(parent, question, answerer, onAnswer);
        d.dialog.setVisible(true);
        return d.result;
    }

    /**
     * לחיצה על תשובה: שולח אותה למשחק, נועל את הכפתורים, מראה אם צדקו, מה הייתה התשובה הנכונה
     * (FR-41), ואיך הכלי זז בפועל (FR-40).
     */
    private void choose(int index) {
        if (chosen >= 0) {
            return;
        }
        chosen = index;
        result = onAnswer.apply(index);
        AnswerCheck check = result.getCheck();
        for (int i = 0; i < buttons.length; i++) {
            buttons[i].lock(i == question.getCorrectIndex() ? AnswerButton.State.CORRECT
                    : i == index ? AnswerButton.State.WRONG : AnswerButton.State.OTHER);
        }
        StringBuilder msg = new StringBuilder();
        if (check.isCorrect()) {
            feedback.setForeground(CORRECT);
            msg.append("תשובה נכונה! ");
        } else {
            feedback.setForeground(WRONG);
            msg.append("תשובה שגויה. התשובה הנכונה: ").append(check.getCorrectAnswer()).append(". ");
        }
        msg.append(describeMove(result));
        for (Pawn eaten : result.getCaptured()) {
            msg.append(" הכלי אכל כלי של ").append(eaten.getOwner().getName()).append(", והוא חזר לטירה.");
        }
        if (result.isReachedFinish()) {
            msg.append(" הכלי הגיע לבית!");
        }
        if (result.getNextQuestion() != null) {
            msg.append(" הכלי נחת על עוד משבצת שאלה – שאלה נוספת אחרי \"המשך\".");
        }
        feedback.setText(html(msg.toString(), true));
        continueButton.setVisible(true);
        dialog.getRootPane().setDefaultButton(continueButton);
        dialog.pack();
        continueButton.requestFocusInWindow();
    }

    private static String steps(int n) {
        return n == 1 ? "צעד אחד" : n + " צעדים";
    }

    /**
     * מתאר איך הכלי זז בפועל אחרי התשובה. משמש גם את חלון השאלה וגם את יומן הפעולות, כך ששניהם
     * תמיד אומרים אותו דבר.
     */
    public static String describeMove(AnswerResult r) {
        int moved = Math.abs(r.getStepsMoved());
        switch (r.getLimit()) {
            case FINISH_NOT_EXACT:
                return "הכלי לא זז: נכנסים לבית רק במספר מדויק.";
            case DOUBLE_BLOCK:
                if (moved == 0) {
                    return "הכלי לא זז: Double Block חוסם את הדרך.";
                }
                return (r.getStepsMoved() > 0 ? "הכלי התקדם " : "הכלי חזר ") + steps(moved)
                        + (r.getStepsMoved() > 0 ? "" : " אחורה") + " ונעצר לפני Double Block.";
            case START_SQUARE:
                return moved == 0 ? "הכלי כבר במשבצת ההתחלה, ולא חוזר מעבר לה."
                        : "הכלי חזר " + steps(moved) + " אחורה, עד משבצת ההתחלה.";
            case NONE:
            default:
                return r.getStepsMoved() > 0 ? "הכלי התקדם " + steps(moved) + "."
                        : "הכלי חזר " + steps(moved) + " אחורה.";
        }
    }

    private static String html(String text, boolean bold) {
        String safe = text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        return "<html><div dir='rtl' style='width:" + TEXT_WIDTH + "px;text-align:right'>"
                + (bold ? "<b>" + safe + "</b>" : safe) + "</div></html>";
    }

    private static void add(JPanel parent, JComponent c) {
        c.setAlignmentX(Component.CENTER_ALIGNMENT);
        parent.add(c);
    }

    /** כפתור תשובה: לבן עם מסגרת, ואחרי הבחירה ירוק לתשובה הנכונה ואדום לתשובה השגויה שנבחרה. */
    private static final class AnswerButton extends JButton {

        private static final long serialVersionUID = 1L;

        enum State { OPEN, CORRECT, WRONG, OTHER }

        private State state = State.OPEN;

        AnswerButton(String text) {
            super(html(text, false));
            setFont(UiTheme.BODY);
            setHorizontalAlignment(RIGHT);
            setFocusPainted(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setOpaque(false);
            setRolloverEnabled(true);
            setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        void lock(State s) {
            state = s;
            setCursor(Cursor.getDefaultCursor());
            setRolloverEnabled(false);
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            ZebraArt.smooth(g2);
            RoundRectangle2D.Double shape = new RoundRectangle2D.Double(1, 1, getWidth() - 3, getHeight() - 3, 12, 12);
            Color fill;
            Color line;
            Color text;
            switch (state) {
                case CORRECT:
                    fill = CORRECT;
                    line = CORRECT;
                    text = Color.WHITE;
                    break;
                case WRONG:
                    fill = WRONG;
                    line = WRONG;
                    text = Color.WHITE;
                    break;
                case OTHER:
                    fill = UiTheme.SURFACE;
                    line = new Color(0xC9C9C2);
                    text = UiTheme.MUTED;
                    break;
                case OPEN:
                default:
                    fill = getModel().isPressed() ? new Color(0xE4E4DE)
                            : getModel().isRollover() ? new Color(0xF4F4EF) : UiTheme.SURFACE;
                    line = getModel().isRollover() ? UiTheme.ACCENT : UiTheme.INK;
                    text = UiTheme.TEXT;
                    break;
            }
            g2.setColor(fill);
            g2.fill(shape);
            g2.setColor(line);
            g2.setStroke(new BasicStroke(1.6f));
            g2.draw(shape);
            g2.dispose();
            setForeground(text);
            super.paintComponent(g);
        }
    }

    /** נקודת צבע של השחקן שעונה. */
    private static final class Dot implements javax.swing.Icon {
        private final Color color;

        Dot(Color color) {
            this.color = color;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
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

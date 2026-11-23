package View;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Window;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import Model.AnswerResult;
import Model.Pawn;
import Model.Player;
import Model.SurpriseEvent;
import Model.SurpriseResult;

/**
 * חלון ההפתעה (טבלה 2 ב-SRS): האירוע שהוגרל, על מי הוא חל, ובחירת יריב וכלי או כפתור הטלה כשצריך
 * (FR-44 עד FR-52).
 * <p>
 * אי אפשר לוותר על האירוע (FR-44): אין כפתור ביטול, וסגירת החלון לא עושה כלום. אחרי שהאירוע מתבצע
 * החלון מראה מה קרה, ו"המשך" סוגר אותו. החלון לא מכיר את המשחק: ה-Controller מעביר לו את הפעולות.
 *
 * @author Nour Eldin Khatib (Zebra) – Iteration 2, US-8 (FR-44 עד FR-52)
 */
public final class SurpriseDialog {

    private static final int TEXT_WIDTH = 440;

    /** הפעולות שהחלון יכול לבקש מה-Controller. */
    public interface Actions {
        /** מבצע אירוע שלא דורש בחירה, או מבטל "הזזת כלי יריב" כשאין יריב (FR-47). */
        SurpriseResult resolve();

        /** "הזזת כלי יריב" בכלי שנבחר (FR-46). */
        SurpriseResult moveOpponent(Pawn target);

        /** הטלת UltraBoost (FR-51). */
        SurpriseResult rollUltraBoost();
    }

    private final JDialog dialog;
    private final JPanel choices = new JPanel();
    private final JLabel feedback = new JLabel(" ");
    private final JLabel description = new JLabel();
    private final SurpriseArt.Reel reel = new SurpriseArt.Reel();
    private final JButton actionButton;
    private final JButton continueButton = UiTheme.primaryButton("המשך");
    private final Actions actions;
    private SurpriseResult result;

    private SurpriseDialog(Component parent, SurpriseEvent event, Pawn pawn,
                           Map<Player, List<Pawn>> opponents, Actions actions) {
        this.actions = actions;
        Window owner = parent == null ? null : SwingUtilities.getWindowAncestor(parent);
        if (owner == null && parent instanceof Window) {
            owner = (Window) parent;
        }
        dialog = new JDialog(owner, "הפתעה!", JDialog.DEFAULT_MODALITY_TYPE);
        // אין אפשרות לוותר על האירוע (FR-44)
        dialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBackground(UiTheme.SURFACE);
        root.setBorder(BorderFactory.createEmptyBorder(18, 28, 22, 28));

        // שורת הכותרת: "הפתעה!" ועל מי האירוע חל
        JPanel header = new JPanel(new BorderLayout(12, 0));
        header.setOpaque(false);
        JLabel title = UiTheme.label("הפתעה!", UiTheme.TITLE.deriveFont(28f), UiTheme.INK);
        header.add(title, BorderLayout.LINE_START);
        Player player = pawn.getOwner();
        JLabel who = new JLabel("חל על: " + player.getName());
        who.setFont(UiTheme.BODY);
        who.setForeground(UiTheme.MUTED);
        who.setIcon(new Dot(UiTheme.toAwt(player.getColor())));
        who.setIconTextGap(8);
        header.add(who, BorderLayout.LINE_END);
        add(root, header);
        root.add(Box.createVerticalStrut(12));

        // הגלגל שמראה את ההגרלה (FR-44); האירוע עצמו כבר הוגרל במודל
        add(root, reel);
        root.add(Box.createVerticalStrut(12));

        description.setFont(UiTheme.SUBTITLE);
        // הטקסט נמצא כבר עכשיו (כדי שגודל החלון יהיה נכון), אבל בצבע הרקע עד שהגלגל נעצר
        description.setForeground(UiTheme.SURFACE);
        description.setText(html(event.getDescription(), false));
        add(root, description);
        root.add(Box.createVerticalStrut(14));

        choices.setOpaque(false);
        choices.setLayout(new BoxLayout(choices, BoxLayout.Y_AXIS));
        add(root, choices);

        if (event == SurpriseEvent.ULTRA_BOOST) {
            actionButton = UiTheme.primaryButton("הטל קוביות");
            actionButton.addActionListener(e -> done(this.actions.rollUltraBoost()));
        } else if (event == SurpriseEvent.MOVE_OPPONENT && !opponents.isEmpty()) {
            actionButton = null;
            showOpponents(opponents);
        } else {
            String label = event == SurpriseEvent.MOVE_OPPONENT ? "אין יריב עם כלי במסלול – המשך" : "בצע";
            actionButton = UiTheme.primaryButton(label);
            actionButton.addActionListener(e -> done(this.actions.resolve()));
        }

        root.add(Box.createVerticalStrut(10));
        feedback.setFont(UiTheme.BODY);
        feedback.setForeground(UiTheme.TEXT);
        add(root, feedback);
        root.add(Box.createVerticalStrut(12));

        continueButton.setVisible(false);
        continueButton.addActionListener(e -> dialog.dispose());
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
        bottom.setOpaque(false);
        if (actionButton != null) {
            bottom.add(actionButton);
        }
        bottom.add(continueButton);
        add(root, bottom);

        JPanel frame = new JPanel(new BorderLayout());
        frame.setBackground(UiTheme.SURFACE);
        frame.setBorder(BorderFactory.createLineBorder(UiTheme.INK, 2));
        frame.add(new SurpriseArt.Header(), BorderLayout.NORTH);
        frame.add(root, BorderLayout.CENTER);
        frame.applyComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        dialog.setContentPane(frame);
        if (actionButton != null) {
            dialog.getRootPane().setDefaultButton(actionButton);
        }
        dialog.pack();
        dialog.setResizable(false);
        dialog.setLocationRelativeTo(owner);

        // עד שהגלגל נעצר אי אפשר ללחוץ כלום; אחר כך מופיע ההסבר והכפתורים נפתחים
        setEnabledDeep(choices, false);
        if (actionButton != null) {
            actionButton.setEnabled(false);
        }
        final SurpriseEvent shownEvent = event;
        Runnable reveal = () -> {
            description.setForeground(UiTheme.TEXT);
            setEnabledDeep(choices, true);
            if (actionButton != null) {
                actionButton.setEnabled(true);
                actionButton.requestFocusInWindow();
            }
        };
        dialog.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowOpened(java.awt.event.WindowEvent e) {
                if (animate) {
                    reel.spin(shownEvent, reveal);
                } else {
                    reel.show(shownEvent);
                    reveal.run();
                }
            }
        });
    }

    /** אנימציות פעילות; בבדיקות אוטומטיות אפשר לכבות. */
    static boolean animate = !Boolean.getBoolean("ludo.noAnimations");

    private static void setEnabledDeep(java.awt.Container c, boolean enabled) {
        for (Component k : c.getComponents()) {
            k.setEnabled(enabled);
            if (k instanceof java.awt.Container) {
                setEnabledDeep((java.awt.Container) k, enabled);
            }
        }
    }

    /**
     * מציג את האירוע ומחכה עד שהוא מתבצע והשחקן לוחץ "המשך".
     *
     * @param parent    החלון הראשי
     * @param event     האירוע שהוגרל
     * @param pawn      הכלי שנחת על משבצת ההפתעה; האירוע חל על בעליו (FR-27)
     * @param opponents ב"הזזת כלי יריב": היריבים שאפשר לבחור והכלים שלהם במסלול המשותף (FR-46, FR-47)
     * @param actions   הפעולות במשחק
     * @return מה קרה
     */
    public static SurpriseResult show(Component parent, SurpriseEvent event, Pawn pawn,
                                      Map<Player, List<Pawn>> opponents, Actions actions) {
        SurpriseDialog d = new SurpriseDialog(parent, event, pawn, opponents, actions);
        d.dialog.setVisible(true);
        return d.result;
    }

    /** "הזזת כלי יריב": כפתור לכל יריב שיש לו כלי להזיז, ואחרי הבחירה כפתור לכל כלי שלו (FR-46, FR-47). */
    private void showOpponents(Map<Player, List<Pawn>> opponents) {
        choices.removeAll();
        choices.add(label("בחרו יריב:"));
        JPanel row = new JPanel(new GridLayout(1, 0, 8, 0));
        row.setOpaque(false);
        for (Map.Entry<Player, List<Pawn>> entry : opponents.entrySet()) {
            Player opponent = entry.getKey();
            JButton b = UiTheme.secondaryButton(opponent.getName());
            b.setIcon(new Dot(UiTheme.toAwt(opponent.getColor())));
            b.setPreferredSize(new java.awt.Dimension(150, 46));
            b.addActionListener(e -> showPawns(opponents, opponent, entry.getValue()));
            row.add(b);
        }
        choices.add(row);
        refresh();
    }

    private void showPawns(Map<Player, List<Pawn>> opponents, Player opponent, List<Pawn> pawns) {
        choices.removeAll();
        choices.add(label("בחרו כלי של " + opponent.getName() + ":"));
        JPanel row = new JPanel(new GridLayout(0, 2, 8, 8));
        row.setOpaque(false);
        for (Pawn p : pawns) {
            // סימני RLM שומרים על סדר המספרים בטקסט העברי
            JButton b = UiTheme.secondaryButton("\u200Fכלי " + (p.getNumber() + 1) + " \u200F– " + p.getPathIndex()
                    + " צעדים מההתחלה");
            b.setIcon(new Dot(UiTheme.toAwt(p.getColor())));
            b.setPreferredSize(new java.awt.Dimension(210, 46));
            b.addActionListener(e -> done(actions.moveOpponent(p)));
            row.add(b);
        }
        choices.add(row);
        if (opponents.size() > 1) {
            JButton back = UiTheme.quietButton("יריב אחר");
            back.addActionListener(e -> showOpponents(opponents));
            choices.add(back);
        }
        refresh();
    }

    /** האירוע בוצע: נועל את הבחירה, מראה מה קרה ומציג "המשך". */
    private void done(SurpriseResult r) {
        result = r;
        choices.removeAll();
        if (actionButton != null) {
            actionButton.setVisible(false);
        }
        feedback.setText(html(describe(r), true));
        continueButton.setVisible(true);
        dialog.getRootPane().setDefaultButton(continueButton);
        refresh();
        continueButton.requestFocusInWindow();
    }

    private void refresh() {
        choices.applyComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        choices.revalidate();
        choices.repaint();
        dialog.pack();
    }

    /**
     * מתאר מה קרה באירוע. משמש גם את החלון וגם את יומן הפעולות, כך ששניהם אומרים אותו דבר.
     */
    public static String describe(SurpriseResult r) {
        Player owner = r.getPawn().getOwner();
        StringBuilder sb = new StringBuilder();
        switch (r.getEvent()) {
            case JUMP_TO_FINISH:
                sb.append("הכלי של ").append(owner.getName()).append(" קפץ לבית הסיום!");
                break;
            case BACK_TO_BASE:
                sb.append("הכלי של ").append(owner.getName()).append(" חזר לטירה.");
                break;
            case BACK_THREE:
                sb.append(backText("הכלי של " + owner.getName(), r, 3));
                break;
            case LOSE_NEXT_TURN:
                sb.append(owner.getName()).append(" מפסיד את התור הבא.");
                break;
            case MOVE_OPPONENT:
                if (r.isCancelled()) {
                    sb.append("לאף יריב אין כלי במסלול המשותף, ולכן האירוע בוטל.");
                } else {
                    int want = (r.getStepRoll() + 1) / 2;
                    sb.append("יצא ").append(r.getStepRoll()).append(" בקוביית הצעדים. ")
                            .append(backText("הכלי של " + r.getMovedPawn().getOwner().getName(), r, want));
                }
                break;
            case ULTRA_BOOST:
            default:
                sb.append("בקוביית הצעדים יצא ").append(r.getStepRoll()).append(" וב-UltraBoost יצא ")
                        .append(r.getUltraBoostRoll()).append(", ולכן זזים ").append(steps(r.getUltraBoostSteps()))
                        .append('.');
                if (r.hasMoves()) {
                    sb.append(" בחרו כלי מסומן על הלוח.");
                } else if (r.getUltraBoostSteps() == 0 && r.getStepRoll() != 6) {
                    sb.append(" במקדם 0 אף כלי לא זז.");
                } else {
                    sb.append(" אין מהלך חוקי.");
                }
                break;
        }
        for (Pawn eaten : r.getCaptured()) {
            sb.append(" נאכל כלי של ").append(eaten.getOwner().getName()).append(", והוא חזר לטירה.");
        }
        if (r.isPlayerFinished()) {
            sb.append(' ').append(owner.getName()).append(" סיים במקום ").append(owner.getPlace()).append('!');
        }
        if (r.isContinues() && !r.hasMoves()) {
            sb.append(" הכלי נחת על משבצת מיוחדת – היא מופעלת אחרי \"המשך\".");
        }
        if (r.isExtraTurn()) {
            sb.append(" יצא 6: ממשיכים בהטלה נוספת.");
        }
        return sb.toString();
    }

    private static String backText(String who, SurpriseResult r, int wanted) {
        int moved = r.getFrom() - r.getTo();
        if (r.getLimit() == AnswerResult.Limit.DOUBLE_BLOCK) {
            return moved == 0 ? who + " לא זז: Double Block חוסם את הדרך."
                    : who + " חזר " + steps(moved) + " ונעצר לפני Double Block.";
        }
        if (r.getLimit() == AnswerResult.Limit.START_SQUARE || moved < wanted) {
            return moved == 0 ? who + " כבר במשבצת ההתחלה, ולא חוזר מעבר לה."
                    : who + " חזר " + steps(moved) + ", עד משבצת ההתחלה.";
        }
        return who + " חזר " + steps(moved) + " אחורה.";
    }

    private static String steps(int n) {
        return n == 1 ? "צעד אחד" : n + " צעדים";
    }

    private static JLabel label(String text) {
        JLabel l = new JLabel(text);
        l.setFont(UiTheme.BODY);
        l.setForeground(UiTheme.MUTED);
        l.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
        l.setAlignmentX(Component.RIGHT_ALIGNMENT);
        return l;
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

    /** נקודת צבע של שחקן. */
    static final class Dot implements Icon {
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

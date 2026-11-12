package Model;

/**
 * תוצאה של הטלת קובייה בתור.
 */
public final class RollResult {

    private final Player player;
    private final int value;
    private final boolean cancelledThirdSix;
    private final boolean hasMoves;
    private final boolean rollAgain;

    RollResult(Player player, int value, boolean cancelledThirdSix, boolean hasMoves, boolean rollAgain) {
        this.player = player;
        this.value = value;
        this.cancelledThirdSix = cancelledThirdSix;
        this.hasMoves = hasMoves;
        this.rollAgain = rollAgain;
    }

    /** @return השחקן שהטיל */
    public Player getPlayer() {
        return player;
    }

    public int getValue() {
        return value;
    }

    /** @return true if this was a third 6 in a row: it is cancelled and the turn passes (FR-18) */
    public boolean isCancelledThirdSix() {
        return cancelledThirdSix;
    }

    /**
     * @return true if the player has at least one legal move. If false and there is no extra roll,
     *         the turn has already passed (FR-19).
     */
    public boolean hasMoves() {
        return hasMoves;
    }

    /** @return true if there is no legal move but a 6 was rolled, so the player rolls again (FR-17) */
    public boolean isRollAgain() {
        return rollAgain;
    }
}

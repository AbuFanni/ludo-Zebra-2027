package Model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * הגדרות משחק תקינות: הרמה, ושם וצבע לכל שחקן לפי סדר התורות.
 * נוצר רק ב-{@link GameSetupValidator} אחרי שכל השדות תקינים, ואינו ניתן לשינוי (FR-08).
 */
public final class GameSettings {

    private final Difficulty difficulty;
    private final List<String> names;
    private final List<PlayerColor> colors;

    GameSettings(Difficulty difficulty, List<String> names, List<PlayerColor> colors) {
        this.difficulty = difficulty;
        this.names = Collections.unmodifiableList(new ArrayList<String>(names));
        this.colors = Collections.unmodifiableList(new ArrayList<PlayerColor>(colors));
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }

    public int getPlayerCount() {
        return names.size();
    }

    public List<String> getNames() {
        return names;
    }

    public List<PlayerColor> getColors() {
        return colors;
    }

    /** @return שחקנים חדשים לפי סדר ההזנה (FR-07) */
    public List<Player> createPlayers() {
        List<Player> players = new ArrayList<Player>();
        for (int i = 0; i < names.size(); i++) {
            players.add(new Player(names.get(i), colors.get(i), i));
        }
        return players;
    }
}

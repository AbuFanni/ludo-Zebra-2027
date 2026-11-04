package Model;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * שמירה וקריאה של היסטוריית המשחקים בקובץ CSV, לפי התבנית הקבועה של הקורס
 * (History-DataBase Tempalte - Game.csv). הכותרת והסדר של העמודות לא משתנים.
 * <p>
 * Repository: שאר המערכת לא יודעת איך הנתונים נשמרים, רק קוראת ל-{@link #save} ול-{@link #loadAll}.
 */
public class HistoryRepository {

    /** שורת הכותרת, כמו בתבנית של הקורס. */
    public static final String HEADER = "gameID,difficulty,timestamp(DD/MM/YYYY; HH:MM:SS),1st_place_name,"
            + "2nd_place_name,3rd_place_name,4rd_place_name,game_duration(HH:MM:SS)";

    /** שם הקובץ שנשמר ליד קובץ ההרצה. */
    public static final String DEFAULT_FILE = "History-DataBase.csv";

    private static final String NEWLINE = "\r\n";
    private final File file;

    public HistoryRepository(File file) {
        this.file = file;
    }

    public File getFile() {
        return file;
    }

    /** @return כל המשחקים שנשמרו, לפי הסדר בקובץ. קובץ חסר או שורה פגומה מדולגים. */
    public List<GameRecord> loadAll() throws IOException {
        List<GameRecord> records = new ArrayList<GameRecord>();
        if (!file.exists()) {
            return records;
        }
        BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8));
        try {
            String line;
            boolean first = true;
            while ((line = in.readLine()) != null) {
                if (first) {
                    first = false;
                    if (line.startsWith("﻿")) {
                        line = line.substring(1);
                    }
                    if (line.startsWith("gameID")) {
                        continue;
                    }
                }
                if (line.trim().isEmpty()) {
                    continue;
                }
                String[] f = line.split(",", -1);
                if (f.length < 8) {
                    continue;
                }
                int id;
                try {
                    id = Integer.parseInt(f[0].trim());
                } catch (NumberFormatException e) {
                    continue;
                }
                List<String> places = Arrays.asList(f[3].trim(), f[4].trim(), f[5].trim(), f[6].trim());
                records.add(new GameRecord(id, f[1].trim(), f[2].trim(), places, f[7].trim()));
            }
        } finally {
            in.close();
        }
        return records;
    }

    /** @return מספר המשחק הבא: הגדול ביותר בקובץ ועוד 1 */
    public int nextGameId() throws IOException {
        int max = 0;
        for (GameRecord r : loadAll()) {
            max = Math.max(max, r.getGameId());
        }
        return max + 1;
    }

    /**
     * מוסיף משחק שהסתיים לסוף הקובץ, ויוצר את הקובץ עם הכותרת אם הוא לא קיים.
     *
     * @return השורה שנשמרה
     */
    public GameRecord save(Game game) throws IOException {
        List<String> places = new ArrayList<String>();
        for (Player p : game.getRanking()) {
            places.add(p.getName());
        }
        GameRecord record = new GameRecord(nextGameId(), game.getBoard().getDifficulty().getCsvName(),
                formatTimestamp(game.getStartMillis()), places, formatDuration(game.getElapsedMillis()));
        append(record);
        return record;
    }

    /** מוסיף שורה לקובץ. */
    public void append(GameRecord r) throws IOException {
        boolean exists = file.exists() && file.length() > 0;
        File parent = file.getAbsoluteFile().getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        Writer out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file, true), StandardCharsets.UTF_8));
        try {
            if (!exists) {
                out.write(HEADER + NEWLINE);
            }
            StringBuilder sb = new StringBuilder();
            sb.append(r.getGameId()).append(',').append(r.getDifficulty()).append(',').append(r.getTimestamp());
            for (int place = 1; place <= 4; place++) {
                sb.append(',').append(r.getPlace(place));
            }
            sb.append(',').append(r.getDuration());
            out.write(sb.toString() + NEWLINE);
        } finally {
            out.close();
        }
    }

    /** @return התאריך והשעה בפורמט של התבנית, למשל 19/10/2026;12:01:00 */
    public static String formatTimestamp(long millis) {
        return new SimpleDateFormat("dd/MM/yyyy;HH:mm:ss").format(new Date(millis));
    }

    /** @return משך בפורמט HH:MM:SS */
    public static String formatDuration(long millis) {
        long total = Math.max(0, millis / 1000);
        return String.format("%02d:%02d:%02d", total / 3600, (total / 60) % 60, total % 60);
    }
}

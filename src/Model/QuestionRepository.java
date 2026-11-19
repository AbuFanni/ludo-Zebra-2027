package Model;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * קריאה וכתיבה של מאגר השאלות בקובץ {@value #DEFAULT_FILE} (SRS טבלה 3).
 * <p>
 * Repository: שאר המערכת לא יודעת איך השאלות נשמרות. כל שורה היא שאלה אחת, והשדות מופרדים בטאב:
 * <pre>מזהה  רמה(EASY/MEDIUM/HARD)  טקסט  תשובה1  תשובה2  תשובה3  תשובה4  נכונה(1-4)</pre>
 * שורה ריקה או שורה שמתחילה ב-# מדולגת. כל שאלה נוצרת דרך {@link QuestionFactory}, ולכן שאלה
 * פגומה בקובץ מתגלה בקריאה.
 * <p>
 * הצפנת הקובץ (NFR-14) מתווספת באיטרציה 3 (US-11) בתוך {@link #encode} ו-{@link #decode} בלבד.
 *
 * @author Mohamed Shini (Zebra) – Iteration 2, US-7 (FR-43)
 */
public class QuestionRepository {

    /** שם הקובץ שנשמר ליד קובץ ההרצה. */
    public static final String DEFAULT_FILE = "questions.dat";

    /** השורה הראשונה בקובץ. */
    static final String HEADER = "# Ludo Zebra questions: id, level, text, 4 answers, correct (1-4), tab separated";

    private static final int FIELDS = 8;
    private static final String NEWLINE = "\r\n";

    private final File file;

    public QuestionRepository(File file) {
        this.file = file;
    }

    public File getFile() {
        return file;
    }

    public boolean exists() {
        return file.isFile();
    }

    /**
     * קורא את כל השאלות. הקובץ תקין רק אם כל שורה בו תקינה, המזהים לא חוזרים, ויש לפחות שאלה
     * אחת בכל רמה (FR-43).
     *
     * @return השאלות לפי הסדר בקובץ
     * @throws QuestionFileException אם הקובץ חסר או פגום, עם הסבר בעברית
     */
    public List<Question> loadAll() throws QuestionFileException {
        if (!file.isFile()) {
            throw new QuestionFileException("קובץ השאלות " + file.getName() + " לא נמצא.");
        }
        List<Question> questions = new ArrayList<Question>();
        Set<Integer> ids = new HashSet<Integer>();
        try (BufferedReader in = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            int lineNo = 0;
            while ((line = in.readLine()) != null) {
                lineNo++;
                line = decode(line);
                if (lineNo == 1 && line.startsWith("﻿")) {
                    line = line.substring(1);
                }
                if (line.trim().isEmpty() || line.startsWith("#")) {
                    continue;
                }
                Question q = parse(line, lineNo);
                if (!ids.add(q.getId())) {
                    throw damaged(lineNo, "המזהה " + q.getId() + " כבר קיים");
                }
                questions.add(q);
            }
        } catch (QuestionFileException e) {
            throw e;
        } catch (IOException e) {
            throw new QuestionFileException("לא הצלחנו לקרוא את קובץ השאלות: " + e.getMessage(), e);
        }
        for (QuestionLevel level : QuestionLevel.values()) {
            if (count(questions, level) == 0) {
                throw new QuestionFileException("קובץ השאלות פגום: אין בו שאלות ברמה "
                        + level.getHebrewName() + ".");
            }
        }
        return questions;
    }

    /** כותב את כל השאלות לקובץ, במקום התוכן הקודם. */
    public void saveAll(List<Question> questions) throws IOException {
        File parent = file.getAbsoluteFile().getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IOException("Cannot create " + parent);
        }
        try (Writer out = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
            out.write(encode(HEADER));
            out.write(NEWLINE);
            for (Question q : questions) {
                out.write(encode(format(q)));
                out.write(NEWLINE);
            }
        }
    }

    /**
     * יוצר את הקובץ עם המאגר ההתחלתי אם הוא עוד לא קיים (בהפעלה הראשונה, FR-74).
     *
     * @return true אם הקובץ נוצר עכשיו
     */
    public boolean createIfMissing(List<Question> initial) throws IOException {
        if (file.exists()) {
            return false;
        }
        saveAll(initial);
        return true;
    }

    // ---------------------------------------------------------------- פורמט

    static String format(Question q) {
        StringBuilder sb = new StringBuilder();
        sb.append(q.getId()).append('\t').append(q.getLevel().name()).append('\t').append(q.getText());
        for (String a : q.getAnswers()) {
            sb.append('\t').append(a);
        }
        sb.append('\t').append(q.getCorrectIndex() + 1);
        return sb.toString();
    }

    static Question parse(String line, int lineNo) throws QuestionFileException {
        String[] f = line.split("\t", -1);
        if (f.length != FIELDS) {
            throw damaged(lineNo, "צריכים להיות " + FIELDS + " שדות ויש " + f.length);
        }
        int id;
        int correct;
        try {
            id = Integer.parseInt(f[0].trim());
            correct = Integer.parseInt(f[7].trim());
        } catch (NumberFormatException e) {
            throw damaged(lineNo, "המזהה והתשובה הנכונה חייבים להיות מספרים");
        }
        QuestionLevel level;
        try {
            level = QuestionLevel.valueOf(f[1].trim());
        } catch (IllegalArgumentException e) {
            throw damaged(lineNo, "רמה לא מוכרת: " + f[1]);
        }
        try {
            return QuestionFactory.forLevel(level).create(id, f[2], Arrays.asList(f[3], f[4], f[5], f[6]), correct - 1);
        } catch (IllegalArgumentException e) {
            throw damaged(lineNo, e.getMessage());
        }
    }

    private static QuestionFileException damaged(int lineNo, String why) {
        return new QuestionFileException("קובץ השאלות פגום (שורה " + lineNo + "): " + why + ".");
    }

    static int count(List<Question> questions, QuestionLevel level) {
        int n = 0;
        for (Question q : questions) {
            if (q.getLevel() == level) {
                n++;
            }
        }
        return n;
    }

    /** המקום היחיד שבו תתווסף הצפנה לשורה לפני כתיבה (NFR-14, איטרציה 3). */
    private static String encode(String line) {
        return line;
    }

    /** הפעולה ההפוכה ל-{@link #encode}. */
    private static String decode(String line) {
        return line;
    }
}

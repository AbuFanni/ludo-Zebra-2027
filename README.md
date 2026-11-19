# Ludo Zebra

פרויקט Ludo של קבוצת Zebra, קורס הנדסת תוכנה ואבטחת איכות, אוניברסיטת חיפה.
Java (Swing), ארכיטקטורת MVC בחבילות `Model`, `View` ו-`Controller`.

בסיס הקוד: [Ludo-Griniaris-Game](https://github.com/AlexandrosPlessias/Ludo-Griniaris-Game) של Alexandros Plessias
(רישיון MIT). מה נלקח ומה שונה: [NOTICE.md](NOTICE.md).

## הרצה

- **Windows:** לחיצה כפולה על `run.bat` (או `java -jar Ludo-Zebra.jar`).
- **macOS / Linux:** `./run.sh`.
- צריך Java 17 ומעלה (NFR-05), להורדה: https://adoptium.net. עם Java ישנה יותר מופיעה השגיאה `UnsupportedClassVersionError`.
- ה-JAR משתמש רק בנתיבים יחסיים לתיקייה שלו (NFR-16), ולכן אפשר להעתיק את התיקייה לכל מחשב.
- קובץ ההיסטוריה `History-DataBase.csv` נוצר ליד ה-JAR בסוף המשחק הראשון.

## בנייה

- **Eclipse:** File → Import → Existing Projects into Workspace, ובוחרים את התיקייה. מקורות ב-`src`, בדיקות ב-`test`.
  הרצה: `Controller.Main`. ייצוא JAR: File → Export → Runnable JAR file.
- **שורת פקודה:** צריך JDK 17 ומעלה. `./build.sh` (macOS, Linux, Git Bash) או `build.bat` (Windows)
  מקמפל ל-Java 17, מריץ את בדיקות המודל ובונה את `Ludo-Zebra.jar`.
  אם `javac` לא ב-PATH, מגדירים קודם `JAVA_HOME` לתיקיית ה-JDK.

## מבנה הקוד וחלוקה לפי ה-SDP

כל אחד מושך את הקבצים של המשימות שלו לענף משלו ופותח PR, כך שהתרומה נרשמת לו ב-GitHub.

| חבילה | קבצים | דרישות | אחראי |
| --- | --- | --- | --- |
| `View` | `MainFrame`, `MainMenuPanel`, `GameSetupPanel`, `PlaceholderPanel`, `Dialogs`, `UiTheme` | FR-01 עד FR-06 | נור |
| `Controller` | `Main`, `MainController`, `SetupController` | FR-01, FR-02 | נור |
| `Model` | `Player`, `PlayerColor`, `GameSettings`, `GameSetupValidator`, `SetupValidationResult` | FR-04 עד FR-08 | נימר |
| `Model` | `Difficulty`, `Position`, `Board`, `BoardBuilder`, `Square` והיורשים, `SquareKind`, `SquareFactory`, `QuestionLevel` | FR-09 | נימר |
| `View` | `BoardPanel`, `GameScreenPanel`, `Images` | FR-12, FR-13, FR-23 (סימון) | מוחמד |
| `Model` | `Game`, `GamePhase`, `Pawn`, `Move`, `MoveResult`, `RollResult`, `GameListener`, `Die`, `StepDie` | FR-15, FR-19 עד FR-23 | וסים |
| `Controller` | `GameController` | חיבור מסך המשחק למודל | מוחמד, וסים |
| `Model` / `View` / `Controller` | `HistoryRepository`, `GameRecord`, `HistoryPanel`, `HistoryController` | היסטוריה ב-CSV (מעבר לאיטרציה 1) | לפי חלוקת איטרציה 3 |
| — | `build.sh`, `build.bat`, `run.bat`, `run.sh` | NFR-08 (JAR שרץ על כל מחשב) | וסים (Scrum master) |
| `test` | `ModelTests` | בדיקות יחידה | כל אחד לחלק שלו |

## תבניות עיצוב

- **MVC:** המודל לא מכיר את Swing. ה-View מצייר ומדווח על לחיצות, וה-Controller מחליט מה קורה.
- **Observer:** `Game` מודיע ל-`GameListener` (ה-`GameController`) על הטלה, מהלך, תור וסיום.
- **Factory Method:** `SquareFactory` יוצר את סוג המשבצת הנכון, ו-`BoardBuilder.build(difficulty)` יוצר את הלוח
  לפי רמת הקושי (המקום שה-SDP הגדיר ליצירת לוח לפי רמה).
- **Builder:** `BoardBuilder` בונה את הלוח של כל רמה שלב אחר שלב.
- **Strategy:** `Die` מאפשר להחליף קובייה (רגילה, Boost, UltraBoost, או קבועה בבדיקות).
- **Repository:** `HistoryRepository` מסתיר את פורמט קובץ ה-CSV משאר המערכת.

## מה כבר עובד (איטרציה 1)

- מסך פתיחה, ניווט, ואישור לפני יציאה מהמשחק או מהתוכנה.
- הגדרת משחק: 2 עד 4 שחקנים, רמה, שם וצבע, עם שגיאה ליד כל שדה.
- שלושת הלוחות: 52 משבצות בקל, ועוד 64 בבינוני, ועוד 64 ו-72 בקשה, עם משבצות חץ.
  משבצות ההגנה, השאלה וההפתעה מוגרלות בכל משחק (4/8/12 הגנה, 0/6/9 שאלות, 0/2/3 הפתעות).
- קוביית צעדים, יציאה ב-6, תור נוסף ב-6, ביטול 6 שלישי, אכילה, משבצות מוגנות,
  Double Block, כניסה לבית בתוצאה מדויקת, סיום ודירוג.
- סימון הכלים שיכולים לזוז, מסגרת על משבצת היעד, והסבר כשבוחרים כלי שלא יכול לזוז.
- שמירת המשחק ב-CSV לפי התבנית של הקורס, ומסך היסטוריה.

## מה בהמשך

- איטרציה 2: שאלות על משבצת `?`, הפתעות, קוביות Boost ו-UltraBoost.
- איטרציה 3: אשף ניהול שאלות עם הרשאת מנהל.

## כלים

- נור נעזר בכלי הבינה המלאכותית Claude של Anthropic בכתיבת החלקים שלו: מסכי הפתיחה וההגדרה, העיצוב של Zebra, הבדיקות והתיעוד. כל הקוד נבדק והורץ לפני המיזוג.
- בסיס הקוד: Ludo-Griniaris-Game (רישיון MIT), ראו `NOTICE.md`.
- כל חבר צוות מוסיף כאן את הכלים שבהם השתמש.

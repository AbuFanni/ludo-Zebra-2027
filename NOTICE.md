# מקור הקוד (Third-party notice)

בסיס הפרויקט הוא **Ludo-Griniaris-Game** של Alexandros Plessias:
https://github.com/AlexandrosPlessias/Ludo-Griniaris-Game
רישיון MIT. נוסח הרישיון המקורי נמצא ב-`THIRD_PARTY_LICENSE.txt`.

השימוש בקוד מ-GitHub אושר ע"י המתרגל עלי אבו סאלח (4.11.2026), בתנאי שהמקור מצוין בדוח המערכת ובהגשה,
ושכל הקוד בנוי לפי MVC ועומד בדרישות.

## מה לקחנו מהמקור

| במקור | אצלנו | מה נשאר ומה השתנה |
| --- | --- | --- |
| `Cells.Cell` והיורשים (`StarCell`, `ColoredSimpleCell`, `FinalCell`...) | `Model.Square` והיורשים | הרעיון של היררכיית משבצות. במקור כל תא היה `JButton`; אצלנו המשבצת היא אובייקט מודל בלי ממשק, ונוצרת דרך `SquareFactory`. |
| `GameBoardData.GameBoardMap` (לוח צלב בגודל משתנה) | `Model.Board`, `Model.BoardBuilder` | הרעיון של בניית לוח צלב וקואורדינטות. אצלנו נוספו המסלולים החיצוניים (64 ו-72 משבצות), משבצות חץ, והגרלת משבצות הגנה, שאלה והפתעה לפי מסמך הפרויקט. |
| `Gameplay.greenPath()/redPath()/...` | `BoardBuilder` | מסלול לכל צבע כרשימת משבצות. במקור ארבעה מסלולים נכתבו ידנית; אצלנו המסלול של הירוק נבנה פעם אחת ומסובב ב-90 מעלות לשאר הצבעים. |
| `Gameplay.checkValidMoves()`, `mouseClicked()`, `isGameFinished()` | `Model.Game` | בדיקת חסימה משבצת אחר משבצת, אכילה והחזרה, תור נוסף, סיום. החוקים הותאמו לפרויקט שלנו: יציאה ב-6 (במקור 5), Double Block לפי תשובות המתרגלים, 6 שלישי מבוטל, מקום אחרון לשחקן שנשאר. הלוגיקה הוצאה מתוך מאזין העכבר למודל נקי עם Observer (`GameListener`). |
| `Pawn`, `Player`, `Dice` | `Model.Pawn`, `Model.Player`, `Model.Die`/`StepDie` | בלי אייקונים וצבעי AWT במודל. הקובייה היא Strategy. |
| `Gameplay.MouseOverAndClickListener` | `View.BoardPanel` | "מדריך העכבר": מסגרת שחורה על משבצת היעד כשהעכבר על כלי שיכול לזוז. |
| `SettingsPanel`, `GamePanel`, `WelcomePanel`, `WindowInterf` | `View.GameSetupPanel`, `GameScreenPanel`, `MainMenuPanel`, `MainFrame` | מבנה המסכים. המסכים נכתבו מחדש בעברית מימין לשמאל, וכל ההחלטות עברו ל-Controller. |
| `Icons/Dice/*`, `Icons/GameBoard/star.png`, `Icons/ludoLogo.png` | `src/View/images/` | התמונות של הקוביות, הכוכב והלוגו (בלי שינוי). |

## מה כתבנו בעצמנו

- החלוקה ל-`Model`, `View` ו-`Controller` של כל הקוד.
- שלוש רמות הקושי והלוחות שלהן, כולל המסלולים החיצוניים והחצים.
- בדיקת מסך ההגדרה עם הודעה ליד כל שדה (`GameSetupValidator`).
- שמירת ההיסטוריה בקובץ ה-CSV של הקורס ומסך ההיסטוריה.
- בדיקות היחידה (`test/Model/ModelTests.java`).

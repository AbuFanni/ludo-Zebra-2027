package Model;

/**
 * Strategy: קובייה. המשחק לא יודע איזו קובייה מוטלת, ולכן אפשר להחליף קובייה
 * (קוביית צעדים, Boost, UltraBoost, או קובייה קבועה בבדיקות) בלי לשנות את {@link Game}.
 */
public interface Die {

    /** @return תוצאת ההטלה */
    int roll();
}

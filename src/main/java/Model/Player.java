package Model;

/**
 * שחקן במשחק. אובייקט בלתי ניתן לשינוי: אחרי תחילת המשחק אי אפשר לשנות שם או צבע (FR-08).
 *
 * @param name      שם השחקן, 1–15 תווים, בלי פסיק (FR-04)
 * @param color     צבע השחקן (FR-05)
 * @param turnOrder מקום בסדר התורות, מ-0, לפי סדר ההזנה (FR-07)
 */
public record Player(String name, PlayerColor color, int turnOrder) {
}

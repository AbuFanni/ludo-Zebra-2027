package View;

import java.awt.Image;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import javax.swing.ImageIcon;

/**
 * טוען את התמונות של המשחק מתוך קובץ ההרצה (תיקייה View/images).
 * <p>
 * התמונות של הקוביות, הכוכב והלוגו לקוחות מהפרויקט Ludo-Griniaris-Game של Alexandros Plessias
 * (רישיון MIT). אם תמונה חסרה, מוחזר null והמסך מצייר צורה פשוטה במקומה.
 */
public final class Images {

    private static final Map<String, Image> CACHE = new HashMap<String, Image>();

    private Images() {
    }

    /** @return התמונה, או null אם היא לא נמצאה */
    public static Image get(String name) {
        if (CACHE.containsKey(name)) {
            return CACHE.get(name);
        }
        URL url = Images.class.getResource("/View/images/" + name);
        Image img = url == null ? null : new ImageIcon(url).getImage();
        CACHE.put(name, img);
        return img;
    }

    /** @return תמונת הקובייה לתוצאה 1 עד 6 */
    public static Image dice(int value) {
        return get("dice_" + value + ".png");
    }

    public static Image star() {
        return get("star.png");
    }

    public static Image logo() {
        return get("ludoLogo.png");
    }

    /** @return אייקון בגודל נתון, או null */
    public static ImageIcon icon(Image img, int w, int h) {
        if (img == null) {
            return null;
        }
        return new ImageIcon(img.getScaledInstance(w, h, Image.SCALE_SMOOTH));
    }
}

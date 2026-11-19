package Model;

import java.io.IOException;

/**
 * קובץ השאלות חסר או פגום (FR-43). ההודעה מסבירה מה הבעיה, כדי להציג אותה למשתמש.
 *
 * @author Mohamed Shini (Zebra) – Iteration 2, US-7 (FR-43)
 */
public class QuestionFileException extends IOException {

    private static final long serialVersionUID = 1L;

    public QuestionFileException(String message) {
        super(message);
    }

    public QuestionFileException(String message, Throwable cause) {
        super(message, cause);
    }
}

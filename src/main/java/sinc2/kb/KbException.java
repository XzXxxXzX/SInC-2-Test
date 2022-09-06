package sinc2.kb;

/**
 * Exception class thrown when errors occur within KB operations.
 *
 * @since 2.0
 */
public class KbException extends Exception {
    public KbException() {
    }

    public KbException(String message) {
        super(message);
    }

    public KbException(String message, Throwable cause) {
        super(message, cause);
    }

    public KbException(Throwable cause) {
        super(cause);
    }

    public KbException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}

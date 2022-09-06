package sinc.common;

public class SincException extends Exception {
    public SincException() {
    }

    public SincException(String message) {
        super(message);
    }

    public SincException(String message, Throwable cause) {
        super(message, cause);
    }

    public SincException(Throwable cause) {
        super(cause);
    }

    public SincException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}

package sinc.common;

public class InterruptedSignal extends Exception {
    public InterruptedSignal() {
    }

    public InterruptedSignal(String message) {
        super(message);
    }

    public InterruptedSignal(String message, Throwable cause) {
        super(message, cause);
    }

    public InterruptedSignal(Throwable cause) {
        super(cause);
    }

    public InterruptedSignal(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}

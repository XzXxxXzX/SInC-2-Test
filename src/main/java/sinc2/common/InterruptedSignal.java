package sinc2.common;

/**
 * Interrupted exception thrown when the workflow of SInC is interrupted. On receiving the interruption signal, SInC
 * stops the current rule mining procedure and compress the KB by the current hypothesis set.
 *
 * @since 1.0
 */
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

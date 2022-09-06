package sinc2.exp;

import sinc2.common.SincException;

/**
 * @since 2.0
 */
public class ExperimentException extends SincException {
    public ExperimentException() {
    }

    public ExperimentException(String message) {
        super(message);
    }

    public ExperimentException(String message, Throwable cause) {
        super(message, cause);
    }

    public ExperimentException(Throwable cause) {
        super(cause);
    }

    public ExperimentException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}

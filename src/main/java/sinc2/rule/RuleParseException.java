package sinc2.rule;

import sinc2.common.SincException;

/**
 * Exception thrown when errors occur during Horn rule parsing
 *
 * @since 2.0
 */
public class RuleParseException extends SincException {
    public RuleParseException() {
    }

    public RuleParseException(String message) {
        super(message);
    }

    public RuleParseException(String message, Throwable cause) {
        super(message, cause);
    }

    public RuleParseException(Throwable cause) {
        super(cause);
    }

    public RuleParseException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}

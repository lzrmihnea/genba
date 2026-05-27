package eu.px.genba.common.exception;

import lombok.Getter;

/**
 * Root domain exception. Subclasses carry an i18n key and an HTTP status hint;
 * {@link eu.px.genba.common.GlobalExceptionHandler} maps them to the
 * {@link eu.px.genba.common.ApiError} envelope.
 */
@Getter
public abstract class GenbaException extends RuntimeException {

    private final String messageKey;
    private final String errorCode;

    protected GenbaException(String messageKey, String errorCode) {
        super(messageKey);
        this.messageKey = messageKey;
        this.errorCode = errorCode;
    }

    protected GenbaException(String messageKey, String errorCode, Throwable cause) {
        super(messageKey, cause);
        this.messageKey = messageKey;
        this.errorCode = errorCode;
    }
}

package eu.px.genba.common.exception;

import lombok.Getter;

/**
 * Root domain exception. Subclasses carry an i18n key, a machine error code,
 * and an HTTP status; {@link eu.px.genba.common.GlobalExceptionHandler} maps
 * them to the {@link eu.px.genba.common.ApiError} envelope.
 *
 * <p>Status defaults to 400. Subclasses that want 409/422/etc. pass it to the
 * status-carrying constructor. Exceptions with dedicated handlers (auth, not-
 * found) ignore this field.
 */
@Getter
public abstract class GenbaException extends RuntimeException {

    private final String messageKey;
    private final String errorCode;
    private final int httpStatus;

    protected GenbaException(String messageKey, String errorCode) {
        this(messageKey, errorCode, 400);
    }

    protected GenbaException(String messageKey, String errorCode, int httpStatus) {
        super(messageKey);
        this.messageKey = messageKey;
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }

    protected GenbaException(String messageKey, String errorCode, Throwable cause) {
        super(messageKey, cause);
        this.messageKey = messageKey;
        this.errorCode = errorCode;
        this.httpStatus = 400;
    }
}

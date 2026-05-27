package eu.px.genba.common.exception;

public class TokenInvalidException extends GenbaException {

    public TokenInvalidException() {
        super("auth.error.tokenInvalid", "TOKEN_INVALID");
    }

    public TokenInvalidException(Throwable cause) {
        super("auth.error.tokenInvalid", "TOKEN_INVALID", cause);
    }
}

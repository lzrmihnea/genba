package eu.px.genba.common.exception;

public class InvalidCredentialsException extends GenbaException {

    public InvalidCredentialsException() {
        super("auth.error.invalidCredentials", "INVALID_CREDENTIALS");
    }
}

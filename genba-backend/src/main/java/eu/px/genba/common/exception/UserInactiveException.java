package eu.px.genba.common.exception;

public class UserInactiveException extends GenbaException {

    public UserInactiveException() {
        super("auth.error.userInactive", "USER_INACTIVE");
    }
}

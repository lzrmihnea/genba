package eu.px.genba.common.exception;

public class NotFoundException extends GenbaException {

    public NotFoundException() {
        this("common.error.notFound");
    }

    public NotFoundException(String messageKey) {
        super(messageKey, "NOT_FOUND");
    }
}

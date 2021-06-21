package attini.role.mapper.domain.exceptions;

public class InvalidEventPayloadException extends IllegalArgumentException {
    public InvalidEventPayloadException(String s) {
        super(s);
    }

    public InvalidEventPayloadException() {
        super();
    }

    public InvalidEventPayloadException(Exception e) {
        super(e);
    }

    public InvalidEventPayloadException(String s, Exception e) {
        super(s, e);
    }
}

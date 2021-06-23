package aws.sso.role.names.mapper.domain.exceptions;

public class WrongEventTypeException extends IllegalArgumentException {

    public WrongEventTypeException(String s) {
        super(s);
    }
}

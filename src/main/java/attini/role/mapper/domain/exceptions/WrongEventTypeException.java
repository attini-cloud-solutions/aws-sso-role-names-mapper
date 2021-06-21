package attini.role.mapper.domain.exceptions;

public class WrongEventTypeException extends IllegalArgumentException {

    public WrongEventTypeException(String s) {
        super(s);
    }
}

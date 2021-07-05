package aws.sso.role.names.mapper.domain.exceptions;

public class CouldNotGetParametersException extends RuntimeException{

    public CouldNotGetParametersException(String message, Throwable cause) {
        super(message, cause);
    }
}

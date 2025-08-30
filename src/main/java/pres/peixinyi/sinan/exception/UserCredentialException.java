package pres.peixinyi.sinan.exception;

public class UserCredentialException extends RuntimeException {
    public UserCredentialException(String message) {
        super(message);
    }
    public UserCredentialException(String message, Throwable cause) {
        super(message, cause);
    }
}


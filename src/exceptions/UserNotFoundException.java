package exceptions;

public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(String userID) {
        super("User account not found: " + userID);
    }
}

package exceptions;

public class ItemNotFoundException extends RuntimeException {
    public ItemNotFoundException(String itemID) {
        super("Library item not found: " + itemID);
    }
}

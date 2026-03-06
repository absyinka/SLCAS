package exceptions;

public class ItemNotAvailableException extends RuntimeException {
    public ItemNotAvailableException(String itemID) {
        super("Library item is not available: " + itemID);
    }
}

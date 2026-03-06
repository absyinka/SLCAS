package model;

/**
 * Interface representing any library item that can be borrowed and returned.
 */
public interface Borrowable {
    /**
     * Attempts to borrow this item for the given user.
     * @param user the user borrowing the item
     * @return true if successfully borrowed
     */
    boolean borrow(UserAccount user);

    /**
     * Returns this item, making it available again.
     */
    void returnItem();

    /**
     * @return true if the item is currently available to borrow
     */
    boolean isAvailable();
}

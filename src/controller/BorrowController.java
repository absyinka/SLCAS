package controller;

import exceptions.ItemNotFoundException;
import exceptions.UserNotFoundException;
import model.*;

import java.util.List;

/**
 * Manages all borrow, return, reservation, and fine operations.
 */
public class BorrowController {

    /** Fine rate: $0.50 per day overdue, accumulated recursively. */
    public static final double FINE_PER_DAY = 0.50;

    public enum BorrowResult { SUCCESS, QUEUED, USER_NOT_FOUND, ITEM_NOT_FOUND }
    public enum ReturnResult { SUCCESS, NOT_BORROWED, ITEM_NOT_FOUND, USER_NOT_FOUND }

    private final LibraryDatabase db;

    public BorrowController(LibraryDatabase db) {
        this.db = db;
    }

    // ── Borrow ───────────────────────────────────────────────────────────────

    /**
     * Borrows an item for a user.
     * Returns SUCCESS if available, QUEUED if waitlisted, or an error result.
     */
    public BorrowResult borrowItem(String userID, String itemID) {
        UserAccount user = db.findUserByID(userID);
        if (user == null) return BorrowResult.USER_NOT_FOUND;

        LibraryItem item = db.findItemByID(itemID);
        if (item == null) return BorrowResult.ITEM_NOT_FOUND;

        if (item instanceof Borrowable) {
            Borrowable borrowable = (Borrowable) item;
            if (borrowable.isAvailable()) {
                borrowable.borrow(user);
                user.addBorrowedItem(item);
                db.recordAccess(item);
                return BorrowResult.SUCCESS;
            } else {
                // Item is taken — add user to reservation queue
                db.enqueueReservation(new ReservationEntry(itemID, userID));
                return BorrowResult.QUEUED;
            }
        }
        return BorrowResult.ITEM_NOT_FOUND;
    }

    // ── Return ───────────────────────────────────────────────────────────────

    /**
     * Returns an item from a user.
     * If another user is waiting in the reservation queue, automatically
     * assigns the item to that next user.
     *
     * @return a ReturnResult; call getNextReservation() after SUCCESS to
     *         check if the item was auto-assigned.
     */
    public ReturnResult returnItem(String userID, String itemID) {
        UserAccount user = db.findUserByID(userID);
        if (user == null) return ReturnResult.USER_NOT_FOUND;

        LibraryItem item = db.findItemByID(itemID);
        if (item == null) return ReturnResult.ITEM_NOT_FOUND;

        if (!user.hasBorrowed(itemID)) return ReturnResult.NOT_BORROWED;

        // Return the item
        ((Borrowable) item).returnItem();
        user.removeBorrowedItem(itemID);

        // Auto-assign to next user in queue (if any)
        ReservationEntry next = db.dequeueReservation(itemID);
        if (next != null) {
            UserAccount nextUser = db.findUserByID(next.getUserID());
            if (nextUser != null) {
                ((Borrowable) item).borrow(nextUser);
                nextUser.addBorrowedItem(item);
                db.recordAccess(item);
            }
        }

        return ReturnResult.SUCCESS;
    }

    /**
     * Peeks at the next reservation for an item without consuming it.
     * Used by the GUI to show "assigned to [user]" messages after a return.
     */
    public String getNextReservationUserID(String itemID) {
        for (ReservationEntry e : db.getReservationQueue()) {
            if (e.getItemID().equals(itemID)) return e.getUserID();
        }
        return null;
    }

    // ── Fine Computation — RECURSIVE ─────────────────────────────────────────

    /**
     * Recursively computes the total overdue fine for a user across all
     * currently borrowed items.
     *
     * @param userID the user's ID
     * @return total fine in dollars
     */
    public double computeOverdueFine(String userID) {
        UserAccount user = db.findUserByID(userID);
        if (user == null) throw new UserNotFoundException(userID);
        List<LibraryItem> borrowed = user.getBorrowedItems();
        return computeFineRecursive(user, borrowed, 0);
    }

    /**
     * Tail-recursive accumulator: walks the borrowed list by index.
     * Base case: index >= list size → return accumulated total.
     */
    private double computeFineRecursive(UserAccount user,
                                        List<LibraryItem> items,
                                        int index) {
        if (index >= items.size()) return 0.0;           // base case
        BorrowRecord rec = user.getOpenRecord(items.get(index).getItemID());
        double fine = (rec != null) ? rec.daysOverdue() * FINE_PER_DAY : 0.0;
        return fine + computeFineRecursive(user, items, index + 1); // recurse
    }

    // ── Overdue users ─────────────────────────────────────────────────────────

    /** Returns all users who currently have at least one overdue item. */
    public List<UserAccount> getOverdueUsers() {
        java.util.List<UserAccount> result = new java.util.ArrayList<>();
        for (UserAccount u : db.getUserAccounts()) {
            if (!u.getOverdueItems().isEmpty()) result.add(u);
        }
        return result;
    }
}

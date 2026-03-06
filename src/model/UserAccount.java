package model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a library member (student or admin).
 */
public class UserAccount {

    public enum Role { STUDENT, ADMIN }

    private final String        userID;
    private       String        name;
    private       String        email;
    private       Role          role;

    /** Items the user currently has on loan. */
    private final List<LibraryItem>  borrowedItems    = new ArrayList<>();

    /** Full history of every borrow transaction. */
    private final List<BorrowRecord> borrowingHistory = new ArrayList<>();

    // Standard borrow period in days
    public static final int BORROW_DAYS = 14;

    public UserAccount(String userID, String name, String email, Role role) {
        this.userID = userID;
        this.name   = name;
        this.email  = email;
        this.role   = role;
    }

    // ── Borrow / Return helpers (called by BorrowController) ─────────────────

    public void addBorrowedItem(LibraryItem item) {
        borrowedItems.add(item);
        borrowingHistory.add(new BorrowRecord(
                item.getItemID(),
                item.getTitle(),
                LocalDate.now(),
                LocalDate.now().plusDays(BORROW_DAYS)
        ));
    }

    public boolean removeBorrowedItem(String itemID) {
        boolean removed = borrowedItems.removeIf(i -> i.getItemID().equals(itemID));
        if (removed) {
            // mark the open record as returned
            for (BorrowRecord r : borrowingHistory) {
                if (r.getItemID().equals(itemID) && r.getReturnDate() == null) {
                    r.markReturned(LocalDate.now());
                    break;
                }
            }
        }
        return removed;
    }

    public boolean hasBorrowed(String itemID) {
        return borrowedItems.stream().anyMatch(i -> i.getItemID().equals(itemID));
    }

    /** Returns items currently overdue. */
    public List<LibraryItem> getOverdueItems() {
        List<LibraryItem> overdue = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (LibraryItem item : borrowedItems) {
            BorrowRecord rec = getOpenRecord(item.getItemID());
            if (rec != null && today.isAfter(rec.getDueDate())) {
                overdue.add(item);
            }
        }
        return overdue;
    }

    /** Finds the open (not-yet-returned) BorrowRecord for a given item. */
    public BorrowRecord getOpenRecord(String itemID) {
        for (BorrowRecord r : borrowingHistory) {
            if (r.getItemID().equals(itemID) && r.getReturnDate() == null) {
                return r;
            }
        }
        return null;
    }

    // ── Getters & Setters ────────────────────────────────────────────────────

    public String             getUserID()         { return userID; }
    public String             getName()           { return name; }
    public String             getEmail()          { return email; }
    public Role               getRole()           { return role; }
    public List<LibraryItem>  getBorrowedItems()  { return borrowedItems; }
    public List<BorrowRecord> getBorrowingHistory(){ return borrowingHistory; }

    public void setName(String name)   { this.name  = name; }
    public void setEmail(String email) { this.email = email; }
    public void setRole(Role role)     { this.role  = role; }

    // ── JSON ─────────────────────────────────────────────────────────────────

    public String toJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"userID\":\"").append(userID).append("\",");
        sb.append("\"name\":\"").append(name.replace("\"", "\\\"")).append("\",");
        sb.append("\"email\":\"").append(email.replace("\"", "\\\"")).append("\",");
        sb.append("\"role\":\"").append(role.name()).append("\",");

        // borrowed item IDs only (items themselves live in the catalogue)
        sb.append("\"borrowedItemIDs\":[");
        for (int i = 0; i < borrowedItems.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append("\"").append(borrowedItems.get(i).getItemID()).append("\"");
        }
        sb.append("],");

        // full borrow history
        sb.append("\"borrowingHistory\":[");
        for (int i = 0; i < borrowingHistory.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(borrowingHistory.get(i).toJson());
        }
        sb.append("]");

        sb.append("}");
        return sb.toString();
    }

    @Override
    public String toString() {
        return String.format("UserAccount{id=%s, name=%s, role=%s, borrowed=%d}",
                userID, name, role, borrowedItems.size());
    }
}

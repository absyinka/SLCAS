package model;

import java.time.LocalDate;

/**
 * Immutable record of a single borrow transaction.
 * Stored in UserAccount.borrowingHistory.
 */
public class BorrowRecord {

    private final String    itemID;
    private final String    itemTitle;
    private final LocalDate borrowDate;
    private final LocalDate dueDate;
    private       LocalDate returnDate; // null until returned

    public BorrowRecord(String itemID, String itemTitle, LocalDate borrowDate, LocalDate dueDate) {
        this.itemID     = itemID;
        this.itemTitle  = itemTitle;
        this.borrowDate = borrowDate;
        this.dueDate    = dueDate;
        this.returnDate = null;
    }

    // ── Accessors ────────────────────────────────────────────────────────────

    public String    getItemID()     { return itemID; }
    public String    getItemTitle()  { return itemTitle; }
    public LocalDate getBorrowDate() { return borrowDate; }
    public LocalDate getDueDate()    { return dueDate; }
    public LocalDate getReturnDate() { return returnDate; }

    public void markReturned(LocalDate date) { this.returnDate = date; }

    public boolean isOverdue() {
        if (returnDate != null) return false;          // already returned
        return LocalDate.now().isAfter(dueDate);
    }

    public long daysOverdue() {
        if (!isOverdue()) return 0;
        return java.time.temporal.ChronoUnit.DAYS.between(dueDate, LocalDate.now());
    }

    // ── JSON ─────────────────────────────────────────────────────────────────

    public String toJson() {
        return "{" +
               "\"itemID\":\"" + itemID + "\"," +
               "\"itemTitle\":\"" + itemTitle.replace("\"", "\\\"") + "\"," +
               "\"borrowDate\":\"" + borrowDate + "\"," +
               "\"dueDate\":\"" + dueDate + "\"," +
               "\"returnDate\":\"" + (returnDate != null ? returnDate : "") + "\"" +
               "}";
    }

    @Override
    public String toString() {
        return String.format("BorrowRecord{item=%s, borrowed=%s, due=%s, returned=%s}",
                itemID, borrowDate, dueDate, returnDate != null ? returnDate : "NOT YET");
    }
}

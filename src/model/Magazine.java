package model;

/**
 * Represents a magazine in the library catalogue.
 */
public class Magazine extends LibraryItem implements Borrowable {

    private int    issueNumber;
    private String month;

    public Magazine(String itemID, String title, String author, int year,
                    String category, int issueNumber, String month) {
        super(itemID, title, author, year, category);
        this.issueNumber = issueNumber;
        this.month       = month;
    }

    // ── Borrowable ──────────────────────────────────────────────────────────

    @Override
    public boolean borrow(UserAccount user) {
        if (!isAvailable()) return false;
        setAvailable(false);
        return true;
    }

    @Override
    public void returnItem() {
        setAvailable(true);
    }

    // ── LibraryItem abstract ─────────────────────────────────────────────────

    @Override
    public String getType() { return "Magazine"; }

    @Override
    public String getSummary() {
        return String.format("Magazine: \"%s\" | Issue #%d (%s %d) | Publisher: %s",
                getTitle(), issueNumber, month, getYear(), getAuthor());
    }

    @Override
    public String toJsonFields() {
        return "\"issueNumber\":" + issueNumber + "," +
               "\"month\":\"" + escape(month) + "\"";
    }

    // ── Getters & Setters ───────────────────────────────────────────────────

    public int    getIssueNumber()           { return issueNumber; }
    public String getMonth()                 { return month; }

    public void setIssueNumber(int n)        { this.issueNumber = n; }
    public void setMonth(String month)       { this.month = month; }
}

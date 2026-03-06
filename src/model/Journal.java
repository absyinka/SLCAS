package model;

/**
 * Represents an academic journal in the library catalogue.
 */
public class Journal extends LibraryItem implements Borrowable {

    private int     volume;
    private String  issueDate;
    private boolean peerReviewed;

    public Journal(String itemID, String title, String author, int year,
                   String category, int volume, String issueDate, boolean peerReviewed) {
        super(itemID, title, author, year, category);
        this.volume       = volume;
        this.issueDate    = issueDate;
        this.peerReviewed = peerReviewed;
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
    public String getType() { return "Journal"; }

    @Override
    public String getSummary() {
        return String.format("Journal: \"%s\" | Vol. %d | %s | Peer-reviewed: %s | Author: %s",
                getTitle(), volume, issueDate, peerReviewed ? "Yes" : "No", getAuthor());
    }

    @Override
    public String toJsonFields() {
        return "\"volume\":" + volume + "," +
               "\"issueDate\":\"" + escape(issueDate) + "\"," +
               "\"peerReviewed\":" + peerReviewed;
    }

    // ── Getters & Setters ───────────────────────────────────────────────────

    public int     getVolume()               { return volume; }
    public String  getIssueDate()            { return issueDate; }
    public boolean isPeerReviewed()          { return peerReviewed; }

    public void setVolume(int volume)        { this.volume = volume; }
    public void setIssueDate(String date)    { this.issueDate = date; }
    public void setPeerReviewed(boolean pr)  { this.peerReviewed = pr; }
}

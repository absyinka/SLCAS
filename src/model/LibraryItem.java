package model;

/**
 * Abstract base class for all library items.
 * Subclasses: Book, Magazine, Journal.
 */
public abstract class LibraryItem {

    private String itemID;
    private String title;
    private String author;
    private int year;
    private String category;
    private boolean available;

    public LibraryItem(String itemID, String title, String author, int year, String category) {
        this.itemID   = itemID;
        this.title    = title;
        this.author   = author;
        this.year     = year;
        this.category = category;
        this.available = true;
    }

    // ── Abstract ────────────────────────────────────────────────────────────

    /** Returns the concrete type label: "Book", "Magazine", or "Journal". */
    public abstract String getType();

    /** Returns a human-readable one-line summary of this item. */
    public abstract String getSummary();

    /** Serialises type-specific fields into a JSON fragment (no braces). */
    public abstract String toJsonFields();

    // ── Getters & Setters ───────────────────────────────────────────────────

    public String getItemID()   { return itemID; }
    public String getTitle()    { return title; }
    public String getAuthor()   { return author; }
    public int    getYear()     { return year; }
    public String getCategory() { return category; }

    public boolean isAvailable()              { return available; }
    public void    setAvailable(boolean flag) { this.available = flag; }

    public void setTitle(String title)    { this.title  = title; }
    public void setAuthor(String author)  { this.author = author; }
    public void setYear(int year)         { this.year   = year; }
    public void setCategory(String cat)   { this.category = cat; }

    // ── JSON helpers ────────────────────────────────────────────────────────

    /**
     * Produces a full JSON object for this item including the common fields
     * plus whatever toJsonFields() adds for the concrete subclass.
     */
    public String toJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"itemID\":\"").append(escape(itemID)).append("\",");
        sb.append("\"type\":\"").append(escape(getType())).append("\",");
        sb.append("\"title\":\"").append(escape(title)).append("\",");
        sb.append("\"author\":\"").append(escape(author)).append("\",");
        sb.append("\"year\":").append(year).append(",");
        sb.append("\"category\":\"").append(escape(category)).append("\",");
        sb.append("\"available\":").append(available);
        String extra = toJsonFields();
        if (extra != null && !extra.isEmpty()) {
            sb.append(",").append(extra);
        }
        sb.append("}");
        return sb.toString();
    }

    // ── Utilities ───────────────────────────────────────────────────────────

    /** Escapes backslash and double-quote for JSON string values. */
    protected static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    @Override
    public String toString() {
        return String.format("[%s] %s — %s (%d) | %s | %s",
                getType(), itemID, title, year, author,
                available ? "Available" : "Borrowed");
    }
}

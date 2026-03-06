package model;

/**
 * Represents a book in the library catalogue.
 */
public class Book extends LibraryItem implements Borrowable {

    private String isbn;
    private String edition;
    private String genre;

    public Book(String itemID, String title, String author, int year,
                String category, String isbn, String edition, String genre) {
        super(itemID, title, author, year, category);
        this.isbn    = isbn;
        this.edition = edition;
        this.genre   = genre;
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
    public String getType() { return "Book"; }

    @Override
    public String getSummary() {
        return String.format("Book: \"%s\" by %s (%d) | ISBN: %s | Edition: %s | Genre: %s",
                getTitle(), getAuthor(), getYear(), isbn, edition, genre);
    }

    @Override
    public String toJsonFields() {
        return "\"isbn\":\"" + escape(isbn) + "\"," +
               "\"edition\":\"" + escape(edition) + "\"," +
               "\"genre\":\"" + escape(genre) + "\"";
    }

    // ── Getters & Setters ───────────────────────────────────────────────────

    public String getIsbn()    { return isbn; }
    public String getEdition() { return edition; }
    public String getGenre()   { return genre; }

    public void setIsbn(String isbn)       { this.isbn    = isbn; }
    public void setEdition(String edition) { this.edition = edition; }
    public void setGenre(String genre)     { this.genre   = genre; }
}

package controller;

import exceptions.InvalidDataException;
import exceptions.ItemNotFoundException;
import model.*;
import utils.FileHandler;
import utils.IDGenerator;
import utils.SortingUtils;

import java.util.List;

/**
 * Primary orchestrator for the SLCAS backend.
 *
 * The GUI team creates one instance of this class at startup and calls
 * the methods listed in the Integration Contract (CLAUDE.md).
 *
 * LibraryDatabase is owned here and injected into all sub-controllers.
 */
public class LibraryManager {

    private static final String DATA_FILE = "data/library_data.json";

    private final LibraryDatabase  db;
    private final FileHandler      fileHandler;
    private final SearchEngine     searchEngine;
    private final BorrowController borrowController;
    private final ReportGenerator  reportGenerator;
    private final SortingUtils     sortingUtils;

    public LibraryManager() {
        this.db              = new LibraryDatabase();
        this.fileHandler     = new FileHandler();
        this.searchEngine    = new SearchEngine(db);
        this.borrowController = new BorrowController(db);
        this.reportGenerator = new ReportGenerator(db);
        this.sortingUtils    = new SortingUtils();
    }

    // ── Startup ──────────────────────────────────────────────────────────────

    /**
     * Loads persisted data from disk. Call once at application startup.
     */
    public void initialize() {
        fileHandler.loadData(db, DATA_FILE);
    }

    // ── Item Management ───────────────────────────────────────────────────────

    /**
     * Adds a new item to the catalogue.
     * Assigns a fresh ID if the item's ID is null/empty.
     * Pushes an ADD action onto the undo stack.
     */
    public void addItem(LibraryItem item) {
        if (item == null) throw new InvalidDataException("Item cannot be null");
        if (item.getTitle() == null || item.getTitle().isBlank())
            throw new InvalidDataException("Item title cannot be empty");

        if (item.getItemID() == null || item.getItemID().isBlank()) {
            // Use reflection-free approach: set via a cast won't work since itemID is final
            // Items must be constructed with an ID. GUI should call IDGenerator first.
            throw new InvalidDataException("Item must have a valid ID — use IDGenerator.generateItemID()");
        }

        db.addItem(item);
        db.pushAdminAction(new AdminAction(AdminAction.Type.ADD, item));
    }

    /**
     * Convenience factory: creates and adds a Book, assigning a new ID automatically.
     */
    public Book addBook(String title, String author, int year, String category,
                        String isbn, String edition, String genre) {
        validate(title, author, year);
        Book book = new Book(IDGenerator.generateItemID(), title, author, year,
                             category, isbn, edition, genre);
        db.addItem(book);
        db.pushAdminAction(new AdminAction(AdminAction.Type.ADD, book));
        return book;
    }

    public Magazine addMagazine(String title, String author, int year, String category,
                                int issueNumber, String month) {
        validate(title, author, year);
        Magazine mag = new Magazine(IDGenerator.generateItemID(), title, author, year,
                                    category, issueNumber, month);
        db.addItem(mag);
        db.pushAdminAction(new AdminAction(AdminAction.Type.ADD, mag));
        return mag;
    }

    public Journal addJournal(String title, String author, int year, String category,
                              int volume, String issueDate, boolean peerReviewed) {
        validate(title, author, year);
        Journal journal = new Journal(IDGenerator.generateItemID(), title, author, year,
                                      category, volume, issueDate, peerReviewed);
        db.addItem(journal);
        db.pushAdminAction(new AdminAction(AdminAction.Type.ADD, journal));
        return journal;
    }

    /**
     * Deletes an item by ID. Pushes a DELETE action for potential undo.
     * @throws ItemNotFoundException if the ID does not exist
     */
    public void deleteItem(String itemID) {
        LibraryItem item = db.findItemByID(itemID);
        if (item == null) throw new ItemNotFoundException(itemID);
        db.pushAdminAction(new AdminAction(AdminAction.Type.DELETE, item));
        db.removeItem(itemID);
    }

    /**
     * Undoes the last admin ADD or DELETE action.
     * @return a human-readable description of the undone action, or null if stack empty
     */
    public String undoLastAction() {
        AdminAction action = db.popAdminAction();
        if (action == null) return null;

        switch (action.getType()) {
            case ADD:    db.removeItem(action.getItem().getItemID()); break;
            case DELETE: db.addItem(action.getItem());                break;
        }
        return "Undone: " + action.getDescription();
    }

    // ── Queries ──────────────────────────────────────────────────────────────

    public List<LibraryItem> getAllItems() { return db.getItemList(); }

    public LibraryItem getItemByID(String itemID) {
        LibraryItem item = db.findItemByID(itemID);
        if (item == null) throw new ItemNotFoundException(itemID);
        return item;
    }

    public List<UserAccount> getAllUsers() { return db.getUserAccounts(); }

    public List<UserAccount> getOverdueUsers() {
        return borrowController.getOverdueUsers();
    }

    // ── User Management ───────────────────────────────────────────────────────

    public UserAccount addUser(String name, String email, UserAccount.Role role) {
        if (name == null || name.isBlank())  throw new InvalidDataException("Name cannot be empty");
        if (email == null || email.isBlank()) throw new InvalidDataException("Email cannot be empty");
        UserAccount user = new UserAccount(IDGenerator.generateUserID(), name, email, role);
        db.addUser(user);
        return user;
    }

    // ── Persistence ───────────────────────────────────────────────────────────

    /**
     * Persists all data to disk. Call on app shutdown or manual save.
     */
    public void saveAll() {
        fileHandler.saveData(db, DATA_FILE);
    }

    // ── Sub-controller accessors (GUI may call these directly) ────────────────

    public SearchEngine      getSearchEngine()      { return searchEngine; }
    public BorrowController  getBorrowController()  { return borrowController; }
    public ReportGenerator   getReportGenerator()   { return reportGenerator; }
    public SortingUtils      getSortingUtils()       { return sortingUtils; }
    public FileHandler       getFileHandler()        { return fileHandler; }
    public LibraryDatabase   getDatabase()           { return db; }

    // ── Validation helper ─────────────────────────────────────────────────────

    private void validate(String title, String author, int year) {
        if (title  == null || title.isBlank())  throw new InvalidDataException("Title cannot be empty");
        if (author == null || author.isBlank()) throw new InvalidDataException("Author cannot be empty");
        if (year < 1000 || year > 9999)         throw new InvalidDataException("Year must be a 4-digit number");
    }
}

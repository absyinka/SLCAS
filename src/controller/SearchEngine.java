package controller;

import model.LibraryItem;
import model.LibraryDatabase;
import utils.SortingUtils.SortField;

import java.util.ArrayList;
import java.util.List;

/**
 * Provides three search strategies over the library catalogue.
 *
 * SearchField options: TITLE | AUTHOR | TYPE
 *
 * Algorithm selection:
 *   - If the catalogue is sorted by the same field → Binary Search  O(log n)
 *   - Otherwise                                    → Linear Search  O(n)
 *   - recursiveSearch() is always available for demonstration
 */
public class SearchEngine {

    public enum SearchField { TITLE, AUTHOR, TYPE }

    private final LibraryDatabase db;

    /** Tracks which field the catalogue is currently sorted by (null = unsorted). */
    private SortField currentSortField = null;

    public SearchEngine(LibraryDatabase db) {
        this.db = db;
    }

    public void setCurrentSortField(SortField field) {
        this.currentSortField = field;
    }

    // ── Main entry point (GUI calls this) ────────────────────────────────────

    /**
     * Searches the catalogue for items matching the query on the given field.
     * Automatically picks binary or linear search based on sort state.
     */
    public List<LibraryItem> search(String query, SearchField field) {
        if (query == null || query.isBlank()) return new ArrayList<>(db.getItemList());

        String q = query.trim().toLowerCase();

        // Binary search is only applicable for TITLE/AUTHOR when sorted by same field
        if (field == SearchField.TITLE && currentSortField == SortField.TITLE) {
            return binarySearch(db.getItemList(), q, SearchField.TITLE);
        }
        if (field == SearchField.AUTHOR && currentSortField == SortField.AUTHOR) {
            return binarySearch(db.getItemList(), q, SearchField.AUTHOR);
        }

        // Default: linear search (works on any order, supports partial matches)
        return linearSearch(db.getItemList(), q, field);
    }

    // ── Retrieve a single item by ID (used internally) ───────────────────────

    public LibraryItem getByID(String itemID) {
        return db.findItemByID(itemID);
    }

    // ── Linear Search  O(n) ──────────────────────────────────────────────────

    /**
     * Scans the list from front to back; supports partial/case-insensitive match.
     */
    public List<LibraryItem> linearSearch(List<LibraryItem> items,
                                          String query,
                                          SearchField field) {
        List<LibraryItem> results = new ArrayList<>();
        String q = query.toLowerCase();
        for (LibraryItem item : items) {
            if (matches(item, q, field)) results.add(item);
        }
        return results;
    }

    // ── Binary Search  O(log n) ──────────────────────────────────────────────

    /**
     * Binary search — requires the list to be sorted by the target field.
     * Returns all items whose field starts with the query (prefix match).
     */
    public List<LibraryItem> binarySearch(List<LibraryItem> items,
                                          String query,
                                          SearchField field) {
        List<LibraryItem> results = new ArrayList<>();
        if (items.isEmpty()) return results;

        int lo = 0, hi = items.size() - 1;

        // Find any matching index via binary search
        int matchIdx = -1;
        while (lo <= hi) {
            int mid = (lo + hi) / 2;
            String val = getFieldValue(items.get(mid), field).toLowerCase();
            int cmp = val.compareTo(query);
            if (val.startsWith(query)) { matchIdx = mid; break; }
            else if (cmp < 0) lo = mid + 1;
            else              hi = mid - 1;
        }

        if (matchIdx < 0) return results;

        // Expand left from matchIdx
        int i = matchIdx;
        while (i >= 0 && getFieldValue(items.get(i), field).toLowerCase().startsWith(query)) {
            results.add(0, items.get(i--));
        }
        // Expand right from matchIdx + 1
        i = matchIdx + 1;
        while (i < items.size() && getFieldValue(items.get(i), field).toLowerCase().startsWith(query)) {
            results.add(items.get(i++));
        }
        return results;
    }

    // ── Recursive Search  O(n) — RECURSIVE ──────────────────────────────────

    /**
     * Index-based recursive scan. Demonstrates recursion as required by the spec.
     * Supports partial/case-insensitive match on TITLE or AUTHOR.
     */
    public List<LibraryItem> recursiveSearch(List<LibraryItem> items,
                                             String query,
                                             SearchField field) {
        List<LibraryItem> results = new ArrayList<>();
        if (items == null || items.isEmpty() || query == null) return results;
        recursiveSearchHelper(items, query.toLowerCase(), field, 0, results);
        return results;
    }

    /** Tail-recursive helper that walks the list by index. */
    private void recursiveSearchHelper(List<LibraryItem> items, String query,
                                       SearchField field, int index,
                                       List<LibraryItem> results) {
        if (index >= items.size()) return;           // base case: exhausted list
        if (matches(items.get(index), query, field)) {
            results.add(items.get(index));
        }
        recursiveSearchHelper(items, query, field, index + 1, results); // recurse
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private boolean matches(LibraryItem item, String query, SearchField field) {
        return getFieldValue(item, field).toLowerCase().contains(query);
    }

    private String getFieldValue(LibraryItem item, SearchField field) {
        switch (field) {
            case TITLE:  return item.getTitle();
            case AUTHOR: return item.getAuthor();
            case TYPE:   return item.getType();
            default:     return item.getTitle();
        }
    }
}

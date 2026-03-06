package controller;

import model.*;

import java.util.*;

/**
 * Generates tabular reports for the admin dashboard.
 *
 * Each report method returns List<String[]> — rows of string columns —
 * so the GUI can feed them directly into a JTable model.
 *
 * Recursive component: countCategory() uses index-based recursion.
 */
public class ReportGenerator {

    private final LibraryDatabase db;

    public ReportGenerator(LibraryDatabase db) {
        this.db = db;
    }

    // ── 1. Most Borrowed Report ───────────────────────────────────────────────

    /**
     * Ranks items by how many times they appear in all users' borrow histories.
     * Columns: Rank | Item ID | Title | Type | Borrow Count
     */
    public List<String[]> getMostBorrowedReport() {
        // Count borrow occurrences per itemID across all history records
        Map<String, Integer> countMap = new LinkedHashMap<>();
        Map<String, String>  titleMap = new LinkedHashMap<>();
        Map<String, String>  typeMap  = new LinkedHashMap<>();

        for (UserAccount user : db.getUserAccounts()) {
            for (BorrowRecord rec : user.getBorrowingHistory()) {
                String id = rec.getItemID();
                countMap.put(id, countMap.getOrDefault(id, 0) + 1);
                titleMap.putIfAbsent(id, rec.getItemTitle());
            }
        }
        // Also tag type from live catalogue
        for (LibraryItem item : db.getItemList()) {
            typeMap.put(item.getItemID(), item.getType());
        }

        // Sort entries by count descending (manual insertion sort on entry list)
        List<Map.Entry<String, Integer>> entries = new ArrayList<>(countMap.entrySet());
        for (int i = 1; i < entries.size(); i++) {
            Map.Entry<String, Integer> key = entries.get(i);
            int j = i - 1;
            while (j >= 0 && entries.get(j).getValue() < key.getValue()) {
                entries.set(j + 1, entries.get(j));
                j--;
            }
            entries.set(j + 1, key);
        }

        // Build rows
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"Rank", "Item ID", "Title", "Type", "Borrow Count"});
        int rank = 1;
        for (Map.Entry<String, Integer> e : entries) {
            rows.add(new String[]{
                    String.valueOf(rank++),
                    e.getKey(),
                    titleMap.getOrDefault(e.getKey(), "Unknown"),
                    typeMap.getOrDefault(e.getKey(), "Unknown"),
                    String.valueOf(e.getValue())
            });
        }
        return rows;
    }

    // ── 2. Overdue Users Report ───────────────────────────────────────────────

    /**
     * Lists all users with at least one overdue item.
     * Columns: User ID | Name | Email | Overdue Count | Estimated Fine ($)
     */
    public List<String[]> getOverdueUsersReport() {
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"User ID", "Name", "Email", "Overdue Items", "Estimated Fine ($)"});

        for (UserAccount user : db.getUserAccounts()) {
            List<LibraryItem> overdue = user.getOverdueItems();
            if (!overdue.isEmpty()) {
                double fine = computeTotalFine(user);
                rows.add(new String[]{
                        user.getUserID(),
                        user.getName(),
                        user.getEmail(),
                        String.valueOf(overdue.size()),
                        String.format("%.2f", fine)
                });
            }
        }
        return rows;
    }

    private double computeTotalFine(UserAccount user) {
        double total = 0;
        for (LibraryItem item : user.getOverdueItems()) {
            BorrowRecord rec = user.getOpenRecord(item.getItemID());
            if (rec != null) total += rec.daysOverdue() * BorrowController.FINE_PER_DAY;
        }
        return total;
    }

    // ── 3. Category Distribution Report — RECURSIVE ──────────────────────────

    /**
     * Counts how many items exist per type (Book / Magazine / Journal) and
     * per category string.
     * Columns: Group | Count | Percentage
     */
    public List<String[]> getCategoryReport() {
        List<LibraryItem> items = db.getItemList();
        int total = items.size();

        String[] types = {"Book", "Magazine", "Journal"};
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"Type / Category", "Count", "Percentage"});

        for (String type : types) {
            int count = countCategory(items, type, 0); // recursive count
            double pct = total > 0 ? (count * 100.0 / total) : 0;
            rows.add(new String[]{type, String.valueOf(count),
                                  String.format("%.1f%%", pct)});
        }

        // Also count distinct category strings
        Set<String> categories = new LinkedHashSet<>();
        for (LibraryItem it : items) categories.add(it.getCategory());
        for (String cat : categories) {
            int count = countByCategory(items, cat, 0);
            double pct = total > 0 ? (count * 100.0 / total) : 0;
            rows.add(new String[]{"  › " + cat, String.valueOf(count),
                                  String.format("%.1f%%", pct)});
        }
        return rows;
    }

    /**
     * Recursively counts items whose type matches.
     * Base case: index >= list size → return 0.
     */
    private int countCategory(List<LibraryItem> items, String type, int index) {
        if (index >= items.size()) return 0;                       // base case
        int match = items.get(index).getType().equals(type) ? 1 : 0;
        return match + countCategory(items, type, index + 1);      // recurse
    }

    /**
     * Recursively counts items whose category string matches.
     */
    private int countByCategory(List<LibraryItem> items, String category, int index) {
        if (index >= items.size()) return 0;
        int match = items.get(index).getCategory().equalsIgnoreCase(category) ? 1 : 0;
        return match + countByCategory(items, category, index + 1);
    }
}

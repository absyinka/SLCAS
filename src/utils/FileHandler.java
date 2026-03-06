package utils;

import exceptions.FileIOException;
import model.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles all file I/O for SLCAS.
 *
 * Persistence format: a single JSON file with two top-level arrays:
 *   { "items": [...], "users": [...] }
 *
 * No external libraries used — JSON is built and parsed manually.
 */
public class FileHandler {

    // ── Save ─────────────────────────────────────────────────────────────────

    public void saveData(LibraryDatabase db, String filePath) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");

        // ── items ──
        sb.append("  \"items\": [\n");
        List<LibraryItem> items = db.getItemList();
        for (int i = 0; i < items.size(); i++) {
            sb.append("    ").append(items.get(i).toJson());
            if (i < items.size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append("  ],\n");

        // ── users ──
        sb.append("  \"users\": [\n");
        List<UserAccount> users = db.getUserAccounts();
        for (int i = 0; i < users.size(); i++) {
            sb.append("    ").append(users.get(i).toJson());
            if (i < users.size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append("  ]\n");

        sb.append("}");

        try {
            Path path = Paths.get(filePath);
            if (path.getParent() != null) Files.createDirectories(path.getParent());
            Files.writeString(path, sb.toString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new FileIOException("Could not write to " + filePath, e);
        }
    }

    // ── Load ─────────────────────────────────────────────────────────────────

    public void loadData(LibraryDatabase db, String filePath) {
        Path path = Paths.get(filePath);
        if (!Files.exists(path)) return; // first run — nothing to load

        String json;
        try {
            json = Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new FileIOException("Could not read " + filePath, e);
        }

        // ── parse items ──
        String itemsArray = extractArray(json, "items");
        if (itemsArray != null) {
            for (String obj : splitObjects(itemsArray)) {
                LibraryItem item = parseItem(obj.trim());
                if (item != null) db.addItem(item);
            }
        }

        // ── parse users ──
        String usersArray = extractArray(json, "users");
        if (usersArray != null) {
            for (String obj : splitObjects(usersArray)) {
                UserAccount user = parseUser(obj.trim(), db);
                if (user != null) db.addUser(user);
            }
        }

        // Seed IDGenerator so new IDs don't clash
        int maxItem = 0, maxUser = 0;
        for (LibraryItem it : db.getItemList())
            maxItem = Math.max(maxItem, IDGenerator.parseSequence(it.getItemID()));
        for (UserAccount u : db.getUserAccounts())
            maxUser = Math.max(maxUser, IDGenerator.parseSequence(u.getUserID()));
        IDGenerator.seed(maxItem, maxUser);
    }

    // ── CSV Import ────────────────────────────────────────────────────────────

    /**
     * Imports items from a CSV file.
     * Expected columns: type,title,author,year,category,[type-specific fields...]
     * Returns the number of items successfully imported.
     */
    public int importCSV(String filePath, LibraryDatabase db) {
        int count = 0;
        try (BufferedReader br = new BufferedReader(
                new FileReader(filePath, StandardCharsets.UTF_8))) {
            String line = br.readLine(); // skip header
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                String[] cols = line.split(",", -1);
                if (cols.length < 5) continue;
                try {
                    String type     = cols[0].trim();
                    String title    = cols[1].trim();
                    String author   = cols[2].trim();
                    int    year     = Integer.parseInt(cols[3].trim());
                    String category = cols[4].trim();
                    String id       = IDGenerator.generateItemID();
                    LibraryItem item = null;

                    switch (type) {
                        case "Book":
                            item = new Book(id, title, author, year, category,
                                    get(cols, 5, ""), get(cols, 6, ""), get(cols, 7, ""));
                            break;
                        case "Magazine":
                            item = new Magazine(id, title, author, year, category,
                                    parseInt(get(cols, 5, "0")), get(cols, 6, ""));
                            break;
                        case "Journal":
                            item = new Journal(id, title, author, year, category,
                                    parseInt(get(cols, 5, "0")), get(cols, 6, ""),
                                    Boolean.parseBoolean(get(cols, 7, "false")));
                            break;
                    }
                    if (item != null) { db.addItem(item); count++; }
                } catch (Exception ignored) { /* skip malformed row */ }
            }
        } catch (IOException e) {
            throw new FileIOException("Could not read CSV: " + filePath, e);
        }
        return count;
    }

    // ── CSV Export ────────────────────────────────────────────────────────────

    /**
     * Exports the current catalogue to a CSV file.
     */
    public void exportCSV(String filePath, LibraryDatabase db) {
        StringBuilder sb = new StringBuilder();
        sb.append("type,title,author,year,category,available,extra1,extra2,extra3\n");
        for (LibraryItem item : db.getItemList()) {
            sb.append(item.getType()).append(",")
              .append(csvEscape(item.getTitle())).append(",")
              .append(csvEscape(item.getAuthor())).append(",")
              .append(item.getYear()).append(",")
              .append(csvEscape(item.getCategory())).append(",")
              .append(item.isAvailable());
            if (item instanceof Book) {
                Book b = (Book) item;
                sb.append(",").append(csvEscape(b.getIsbn()))
                  .append(",").append(csvEscape(b.getEdition()))
                  .append(",").append(csvEscape(b.getGenre()));
            } else if (item instanceof Magazine) {
                Magazine m = (Magazine) item;
                sb.append(",").append(m.getIssueNumber())
                  .append(",").append(csvEscape(m.getMonth())).append(",");
            } else if (item instanceof Journal) {
                Journal j = (Journal) item;
                sb.append(",").append(j.getVolume())
                  .append(",").append(csvEscape(j.getIssueDate()))
                  .append(",").append(j.isPeerReviewed());
            }
            sb.append("\n");
        }
        try {
            Path path = Paths.get(filePath);
            if (path.getParent() != null) Files.createDirectories(path.getParent());
            Files.writeString(path, sb.toString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new FileIOException("Could not write CSV: " + filePath, e);
        }
    }

    // ── Manual JSON parsing helpers ───────────────────────────────────────────

    /** Extracts the raw content of a top-level JSON array by key. */
    private String extractArray(String json, String key) {
        String marker = "\"" + key + "\"";
        int keyIdx = json.indexOf(marker);
        if (keyIdx < 0) return null;
        int start = json.indexOf('[', keyIdx + marker.length());
        if (start < 0) return null;
        int depth = 0, i = start;
        for (; i < json.length(); i++) {
            if (json.charAt(i) == '[') depth++;
            else if (json.charAt(i) == ']') { depth--; if (depth == 0) break; }
        }
        return json.substring(start + 1, i); // inner content without [ ]
    }

    /** Splits a flat JSON array body into individual object strings. */
    private List<String> splitObjects(String arrayBody) {
        List<String> objects = new ArrayList<>();
        int depth = 0, start = -1;
        for (int i = 0; i < arrayBody.length(); i++) {
            char c = arrayBody.charAt(i);
            if (c == '{') { if (depth == 0) start = i; depth++; }
            else if (c == '}') { depth--; if (depth == 0 && start >= 0) { objects.add(arrayBody.substring(start, i + 1)); start = -1; } }
        }
        return objects;
    }

    /** Extracts a string value from a JSON object string by key. */
    private String extractString(String obj, String key) {
        String marker = "\"" + key + "\":\"";
        int idx = obj.indexOf(marker);
        if (idx < 0) return "";
        int start = idx + marker.length();
        StringBuilder sb = new StringBuilder();
        boolean escape = false;
        for (int i = start; i < obj.length(); i++) {
            char c = obj.charAt(i);
            if (escape) { sb.append(c); escape = false; }
            else if (c == '\\') { escape = true; }
            else if (c == '"') break;
            else sb.append(c);
        }
        return sb.toString();
    }

    /** Extracts a numeric (non-quoted) value from a JSON object string by key. */
    private String extractNumber(String obj, String key) {
        String marker = "\"" + key + "\":";
        int idx = obj.indexOf(marker);
        if (idx < 0) return "0";
        int start = idx + marker.length();
        // skip whitespace
        while (start < obj.length() && obj.charAt(start) == ' ') start++;
        int end = start;
        while (end < obj.length() && (Character.isDigit(obj.charAt(end)) || obj.charAt(end) == '-')) end++;
        return obj.substring(start, end);
    }

    /** Extracts a boolean value from a JSON object string by key. */
    private boolean extractBoolean(String obj, String key) {
        String marker = "\"" + key + "\":";
        int idx = obj.indexOf(marker);
        if (idx < 0) return false;
        String rest = obj.substring(idx + marker.length()).trim();
        return rest.startsWith("true");
    }

    private LibraryItem parseItem(String obj) {
        try {
            String type     = extractString(obj, "type");
            String itemID   = extractString(obj, "itemID");
            String title    = extractString(obj, "title");
            String author   = extractString(obj, "author");
            int    year     = parseInt(extractNumber(obj, "year"));
            String category = extractString(obj, "category");
            boolean avail   = extractBoolean(obj, "available");

            LibraryItem item;
            switch (type) {
                case "Book":
                    item = new Book(itemID, title, author, year, category,
                            extractString(obj, "isbn"),
                            extractString(obj, "edition"),
                            extractString(obj, "genre"));
                    break;
                case "Magazine":
                    item = new Magazine(itemID, title, author, year, category,
                            parseInt(extractNumber(obj, "issueNumber")),
                            extractString(obj, "month"));
                    break;
                case "Journal":
                    item = new Journal(itemID, title, author, year, category,
                            parseInt(extractNumber(obj, "volume")),
                            extractString(obj, "issueDate"),
                            extractBoolean(obj, "peerReviewed"));
                    break;
                default: return null;
            }
            item.setAvailable(avail);
            return item;
        } catch (Exception e) {
            return null; // skip corrupt entry
        }
    }

    private UserAccount parseUser(String obj, LibraryDatabase db) {
        try {
            String userID = extractString(obj, "userID");
            String name   = extractString(obj, "name");
            String email  = extractString(obj, "email");
            String roleStr = extractString(obj, "role");
            UserAccount.Role role = UserAccount.Role.valueOf(roleStr);
            UserAccount user = new UserAccount(userID, name, email, role);

            // Re-link borrowed items from the catalogue
            String idsBlock = extractArray(obj, "borrowedItemIDs");
            if (idsBlock != null) {
                for (String id : idsBlock.split(",")) {
                    id = id.trim().replace("\"", "");
                    if (id.isEmpty()) continue;
                    LibraryItem item = db.findItemByID(id);
                    if (item != null) user.getBorrowedItems().add(item);
                }
            }

            // Re-create borrow history records
            String histBlock = extractArray(obj, "borrowingHistory");
            if (histBlock != null) {
                for (String recObj : splitObjects(histBlock)) {
                    try {
                        String iid        = extractString(recObj, "itemID");
                        String ititle     = extractString(recObj, "itemTitle");
                        LocalDate bDate   = LocalDate.parse(extractString(recObj, "borrowDate"));
                        LocalDate dDate   = LocalDate.parse(extractString(recObj, "dueDate"));
                        String retStr     = extractString(recObj, "returnDate");
                        BorrowRecord rec  = new BorrowRecord(iid, ititle, bDate, dDate);
                        if (!retStr.isEmpty()) rec.markReturned(LocalDate.parse(retStr));
                        user.getBorrowingHistory().add(rec);
                    } catch (Exception ignored) {}
                }
            }
            return user;
        } catch (Exception e) {
            return null;
        }
    }

    // ── tiny helpers ─────────────────────────────────────────────────────────

    private int    parseInt(String s) { try { return Integer.parseInt(s.trim()); } catch (Exception e) { return 0; } }
    private String get(String[] a, int i, String def) { return (i < a.length) ? a[i].trim() : def; }
    private String csvEscape(String s) {
        if (s == null) return "";
        if (s.contains(",") || s.contains("\"")) return "\"" + s.replace("\"", "\"\"") + "\"";
        return s;
    }
}

# SLCAS — Developer Manual

This document explains the internal architecture, design decisions, data flow, and extension points of the Smart Library Circulation & Automation System. It is intended for developers joining the project or maintaining the codebase.

---

## Table of Contents

1. [Architecture Overview](#1-architecture-overview)
2. [Layer Breakdown](#2-layer-breakdown)
3. [Model Layer — Data Structures](#3-model-layer--data-structures)
4. [Controller Layer — Business Logic](#4-controller-layer--business-logic)
5. [Utils Layer](#5-utils-layer)
6. [GUI Layer](#6-gui-layer)
7. [Data Flow: Key Operations](#7-data-flow-key-operations)
8. [Algorithms Explained](#8-algorithms-explained)
9. [JSON Persistence Format](#9-json-persistence-format)
10. [Exception Handling](#10-exception-handling)
11. [Adding New Features](#11-adding-new-features)
12. [Common Pitfalls](#12-common-pitfalls)

---

## 1. Architecture Overview

SLCAS uses a strict **3-layer architecture**. Each layer communicates only with the layer directly below it.

```
┌──────────────────────────────────────────────┐
│              GUI LAYER  (gui/)               │
│  MainWindow, ViewItemsPanel, BorrowPanel,    │
│  AdminPanel, SearchSortPanel                 │
└───────────────────┬──────────────────────────┘
                    │  calls methods on
┌───────────────────▼──────────────────────────┐
│          CONTROLLER LAYER  (controller/)     │
│  LibraryManager  ←→  BorrowController        │
│  SearchEngine    ←→  ReportGenerator         │
└───────────────────┬──────────────────────────┘
                    │  reads/writes
┌───────────────────▼──────────────────────────┐
│            MODEL LAYER  (model/)             │
│  LibraryDatabase  (ArrayList, Queue,         │
│  Stack, Array)                               │
│  LibraryItem → Book / Magazine / Journal     │
│  UserAccount                                 │
└───────────────────┬──────────────────────────┘
                    │  serialised by
┌───────────────────▼──────────────────────────┐
│            UTILS LAYER  (utils/)             │
│  FileHandler  |  IDGenerator  |  SortingUtils│
└──────────────────────────────────────────────┘
```

**Key rules:**
- GUI classes never access `LibraryDatabase` directly.
- Controllers never import anything from `gui/`.
- All external GUI calls go through `LibraryManager` (the single facade).
- No static mutable globals. `LibraryDatabase` is instantiated once inside `LibraryManager` and injected into sub-controllers by constructor.

---

## 2. Layer Breakdown

### Entry Point — `Main.java`

```java
LibraryManager manager = new LibraryManager();  // boots all controllers
manager.initialize();                            // loads data/library_data.json
SwingUtilities.invokeLater(() -> new MainWindow(manager).setVisible(true));
```

The entire application is wired through `LibraryManager`. `Main.java` creates exactly one instance of it and passes it to the GUI. Everything else flows from there.

---

## 3. Model Layer — Data Structures

### 3.1 Class Hierarchy

```
LibraryItem  (abstract)
    ├── Book     implements Borrowable
    ├── Magazine implements Borrowable
    └── Journal  implements Borrowable

UserAccount
    ├── List<LibraryItem>  borrowedItems
    └── List<BorrowRecord> borrowingHistory

LibraryDatabase
    ├── ArrayList<LibraryItem>       itemList
    ├── Queue<ReservationEntry>      reservationQueue
    ├── Stack<AdminAction>           adminActionStack
    └── LibraryItem[]                frequentCache [10]
```

### 3.2 `LibraryItem` (Abstract Class)

Every item in the catalogue extends `LibraryItem`. It enforces three abstract methods that all subclasses must provide:

| Method | Purpose |
|---|---|
| `getType()` | Returns `"Book"`, `"Magazine"`, or `"Journal"` — used for JSON deserialization |
| `getSummary()` | Returns a human-readable one-line description |
| `toJsonFields()` | Returns the type-specific JSON key-value pairs (no braces) |

The base class provides `toJson()` which wraps common fields and calls `toJsonFields()` to append subclass-specific fields. This is the **Template Method** pattern.

```java
// LibraryItem.toJson() calls toJsonFields() internally
public String toJson() {
    // ... common fields ...
    String extra = toJsonFields();   // implemented by Book/Magazine/Journal
    if (extra != null && !extra.isEmpty()) sb.append(",").append(extra);
    // ...
}
```

### 3.3 `Borrowable` Interface

```java
public interface Borrowable {
    boolean borrow(UserAccount user);  // returns false if already borrowed
    void    returnItem();
    boolean isAvailable();
}
```

All three item types implement this. The `borrow()` method only sets `available = false` — it does **not** add itself to the user's list. That is the responsibility of `BorrowController`, which calls both `item.borrow(user)` and `user.addBorrowedItem(item)`. This separation keeps the model unaware of business rules.

### 3.4 `LibraryDatabase` — Data Structure Details

| Field | Type | Why This Structure |
|---|---|---|
| `itemList` | `ArrayList<LibraryItem>` | O(1) indexed access; dynamic size; supports sorting in-place |
| `reservationQueue` | `Queue<ReservationEntry>` (LinkedList) | FIFO — first user to request gets first pick when item returns |
| `adminActionStack` | `Stack<AdminAction>` | LIFO — undo reverses the most recent action first |
| `frequentCache` | `LibraryItem[10]` | Fixed-size; O(1) read; tracks most-accessed items |

#### The MFA Cache (`frequentCache`)

`recordAccess(item)` is called every time an item is borrowed. The cache works as follows:

1. If the item is already in the cache, increment its count.
2. If an empty slot exists, place it there.
3. If the cache is full, evict the entry with the lowest access count (LFU-style).

This gives the `ReportGenerator` fast access to frequently borrowed items without scanning the full catalogue.

### 3.5 `UserAccount`

Users own two lists:

- **`borrowedItems`** — items currently on loan (live references to `LibraryItem` objects in the catalogue)
- **`borrowingHistory`** — complete transaction log (`BorrowRecord` per transaction, never deleted)

`addBorrowedItem(item)` appends to both lists simultaneously. `removeBorrowedItem(itemID)` removes from `borrowedItems` and calls `markReturned()` on the matching open `BorrowRecord`.

### 3.6 Helper Records

| Class | Fields | Used By |
|---|---|---|
| `BorrowRecord` | itemID, itemTitle, borrowDate, dueDate, returnDate | `UserAccount.borrowingHistory` |
| `AdminAction` | Type (ADD/DELETE), LibraryItem | `LibraryDatabase.adminActionStack` |
| `ReservationEntry` | itemID, userID, timestamp | `LibraryDatabase.reservationQueue` |

`BorrowRecord.daysOverdue()` uses `ChronoUnit.DAYS.between(dueDate, LocalDate.now())` and returns 0 if the item has been returned or is not yet overdue.

---

## 4. Controller Layer — Business Logic

### 4.1 `LibraryManager` — The Facade

`LibraryManager` is the **single point of contact** for the GUI. It:
- Owns the `LibraryDatabase` instance
- Constructs and holds all sub-controllers (injecting the database into each)
- Provides typed factory methods for adding each item type
- Manages the undo stack via `pushAdminAction()` / `popAdminAction()`

```java
// Correct pattern — GUI creates one manager, accesses everything through it
LibraryManager manager = new LibraryManager();
manager.initialize();

// Get sub-controllers when needed
BorrowController bc = manager.getBorrowController();
SearchEngine se      = manager.getSearchEngine();
```

**Why a facade?** The GUI team needs a stable, simple API. If the internals change (e.g. search moves to a different class), the facade absorbs the change and the GUI code stays the same.

### 4.2 `BorrowController`

#### Borrow Flow

```
borrowItem(userID, itemID)
    ├── findUser → null? → return USER_NOT_FOUND
    ├── findItem → null? → return ITEM_NOT_FOUND
    ├── item.isAvailable()?
    │       YES → item.borrow(user)
    │             user.addBorrowedItem(item)
    │             db.recordAccess(item)          ← updates MFA cache
    │             return SUCCESS
    │       NO  → db.enqueueReservation(...)
    │             return QUEUED
```

#### Return Flow

```
returnItem(userID, itemID)
    ├── findUser / findItem / hasBorrowed checks
    ├── item.returnItem()                        ← sets available = true
    ├── user.removeBorrowedItem(itemID)          ← marks BorrowRecord returned
    ├── db.dequeueReservation(itemID)            ← find next waiting user
    │       found → nextUser.addBorrowedItem(item)
    │               item.borrow(nextUser)        ← immediately re-borrows
    └── return SUCCESS
```

#### Fine Computation (Recursive)

```java
private double computeFineRecursive(UserAccount user, List<LibraryItem> items, int index) {
    if (index >= items.size()) return 0.0;          // base case
    BorrowRecord rec = user.getOpenRecord(items.get(index).getItemID());
    double fine = (rec != null) ? rec.daysOverdue() * FINE_PER_DAY : 0.0;
    return fine + computeFineRecursive(user, items, index + 1);  // recurse
}
```

Rate: `$0.50` per day, defined in `BorrowController.FINE_PER_DAY`.

### 4.3 `SearchEngine`

The engine tracks the current sort state via `currentSortField`. Call `setCurrentSortField(field)` after every sort operation (the `SearchSortPanel` does this automatically).

| Method | When It's Used | Match Type |
|---|---|---|
| `linearSearch()` | Catalogue is unsorted | Partial / case-insensitive contains |
| `binarySearch()` | Sorted by the same field | Prefix match |
| `recursiveSearch()` | Always available | Partial / case-insensitive contains |

`search(query, field)` picks automatically:
```java
if (field == TITLE && currentSortField == SortField.TITLE) → binarySearch
if (field == AUTHOR && currentSortField == SortField.AUTHOR) → binarySearch
else → linearSearch
```

### 4.4 `ReportGenerator`

All three reports return `List<String[]>` where **row 0 is always the header**. The GUI reads `data.get(0)` as column headers and `data.subList(1, data.size())` as data rows.

```java
// Example usage in GUI
List<String[]> report = manager.getReportGenerator().getMostBorrowedReport();
String[] headers = report.get(0);
Object[][] rows = report.subList(1, report.size()).toArray(Object[][]::new);
```

Category counting is recursive:

```java
private int countCategory(List<LibraryItem> items, String type, int index) {
    if (index >= items.size()) return 0;                    // base case
    int match = items.get(index).getType().equals(type) ? 1 : 0;
    return match + countCategory(items, type, index + 1);  // recurse
}
```

---

## 5. Utils Layer

### 5.1 `IDGenerator`

Generates sequential IDs: `ITEM-0001`, `ITEM-0002`, …, `USR-0001`, etc.

**Critical:** After loading from file, call `seed(maxItemSeq, maxUserSeq)` to advance the counters past existing IDs. `FileHandler.loadData()` does this automatically. Forgetting to seed after a custom load will cause duplicate IDs.

```java
IDGenerator.seed(maxItem, maxUser);   // called inside FileHandler.loadData()
```

### 5.2 `SortingUtils`

`sort()` returns a **new list** — the original `db.itemList` is not touched. This is intentional: sorting for display does not reorder the database. If you need the database itself sorted (for binary search to work), do:

```java
List<LibraryItem> sorted = sortingUtils.sort(manager.getAllItems(), field, algo);
// notify search engine
manager.getSearchEngine().setCurrentSortField(field);
// Note: db.itemList itself remains in insertion order
```

This means binary search works on the *sorted copy* returned by `sort()` only if you pass that copy to `binarySearch()` directly. The current `SearchSortPanel` passes `manager.getAllItems()` (the original) to the table, and `search()` operates on `db.getItemList()`. **Limitation:** binary search will be triggered by `currentSortField` but will operate on the original unsorted list, giving incorrect results unless the database list itself is sorted.

> **To-do for future improvement:** Either sort `db.itemList` in-place, or have `SearchEngine` operate on a sorted snapshot that `SortingUtils` maintains.

**Algorithm complexity reference:**

| Algorithm | Best | Average | Worst | Space |
|---|---|---|---|---|
| Selection Sort | O(n²) | O(n²) | O(n²) | O(1) |
| Insertion Sort | O(n) | O(n²) | O(n²) | O(1) |
| Merge Sort | O(n log n) | O(n log n) | O(n log n) | O(n) |
| Quick Sort | O(n log n) | O(n log n) | O(n²) | O(log n) |

### 5.3 `FileHandler`

#### Manual JSON parsing

There is no JSON library. The parser uses string scanning:

- `extractArray(json, key)` — finds `"key": [...]` and returns the inner content
- `splitObjects(arrayBody)` — splits a flat array body into individual `{...}` strings by tracking brace depth
- `extractString(obj, key)` — finds `"key":"value"` and reads until the closing `"`, handling escape sequences
- `extractNumber(obj, key)` — finds `"key":123` and reads digit characters
- `extractBoolean(obj, key)` — checks if the value after `"key":` starts with `true`

This is intentionally simple. It handles the exact JSON that `toJson()` produces. It will fail on:
- Nested objects within field values
- JSON with different whitespace formatting from an external editor
- Arrays within field values other than `borrowedItemIDs` and `borrowingHistory`

#### Item deserialization

The `"type"` field in each item JSON object is read first to determine which subclass constructor to call:

```java
switch (type) {
    case "Book":     return new Book(...);
    case "Magazine": return new Magazine(...);
    case "Journal":  return new Journal(...);
    default:         return null;  // unknown type — skipped silently
}
```

#### User deserialization

Users are loaded after items. `borrowedItemIDs` is a JSON array of ID strings; each is looked up via `db.findItemByID()` to restore live object references. This means **items must always be loaded before users**.

---

## 6. GUI Layer

### 6.1 Lifecycle

```
Main.main()
    └── new LibraryManager() + initialize()
    └── SwingUtilities.invokeLater()
            └── new MainWindow(manager)
                    ├── buildUI()
                    │     ├── new ViewItemsPanel(manager, this)
                    │     ├── new BorrowPanel(manager, this)
                    │     ├── new AdminPanel(manager, this)
                    │     └── new SearchSortPanel(manager, this)
                    └── startOverdueTimer()
```

Each panel receives two references: `LibraryManager manager` (for backend calls) and `MainWindow mainWindow` (to update the status bar and trigger cross-panel refreshes).

### 6.2 Cross-Panel Communication

Panels do not talk to each other directly. When an action in one panel should refresh another, it calls `mainWindow.refreshCatalogue()`, which delegates to `viewItemsPanel.refresh()`.

```java
// In BorrowPanel, after a successful borrow:
mainWindow.refreshCatalogue();   // updates ViewItemsPanel table
mainWindow.setStatus("Borrowed: " + iid + " → " + uid);
```

### 6.3 Advanced GUI Techniques

#### Custom Renderer (`ViewItemsPanel.AvailabilityRenderer`)

Extends `DefaultTableCellRenderer`. Overrides `getTableCellRendererComponent()` to check column 6 ("Available") of each row:

```java
String avail = (String) table.getModel().getValueAt(row, 6);
if (!isSelected && "No".equals(avail)) {
    c.setBackground(new Color(255, 220, 220));  // light red
    c.setForeground(new Color(180, 0, 0));
}
```

Applied to **all** columns of the table (not just the "Available" column), so the entire row changes colour.

#### Dynamic Form Fields (`AdminPanel.updateDynamicFields()`)

Three `JPanel` instances (`bookExtra`, `magExtra`, `journalExtra`) are always in the layout but toggled visible based on the type `JComboBox`:

```java
private void updateDynamicFields() {
    String type = (String) typeCombo.getSelectedItem();
    bookExtra.setVisible("Book".equals(type));
    magExtra.setVisible("Magazine".equals(type));
    journalExtra.setVisible("Journal".equals(type));
    revalidate();   // re-compute layout
    repaint();
}
```

The `ActionListener` on `typeCombo` calls this every time the selection changes.

#### Overdue Timer (`MainWindow`)

```java
Timer timer = new Timer(60_000, e -> checkOverdue());
timer.setInitialDelay(5_000);   // first check 5 seconds after launch
timer.start();
```

`checkOverdue()` calls `manager.getOverdueUsers()` and updates the status bar. It never pops a blocking dialog (would interrupt user flow) — only the status bar label changes. The Admin panel's "Overdue Users" report button provides the full detail.

### 6.4 Event Handling Summary

| Component | Event | Handler |
|---|---|---|
| Type combo (AdminPanel) | `ActionListener` | `updateDynamicFields()` |
| Borrow button | `ActionListener` | `doBorrow()` |
| Return button | `ActionListener` | `doReturn()` |
| Search button | `ActionListener` | `doSearch()` |
| Search field | `ActionListener` (Enter key) | `doSearch()` |
| Sort button | `ActionListener` | `doSort()` |
| Table (ViewItems) | `MouseListener` double-click | `showDetail()` |
| Tab change | `ChangeListener` | `viewItemsPanel.refresh()` |
| Window close | `WindowListener` | `onExit()` with save prompt |
| Timer | `ActionListener` | `checkOverdue()` |

---

## 7. Data Flow: Key Operations

### Startup

```
Main.main()
  → LibraryManager()
      → new LibraryDatabase()
      → new SearchEngine(db)
      → new BorrowController(db)
      → new ReportGenerator(db)
      → new SortingUtils()
      → new FileHandler()
  → manager.initialize()
      → fileHandler.loadData(db, "data/library_data.json")
          → parse items → db.addItem() for each
          → parse users → db.addUser() for each
          → IDGenerator.seed(maxItem, maxUser)
  → new MainWindow(manager)
      → all panels constructed
      → viewItemsPanel.refresh() → manager.getAllItems() → populate table
```

### Adding an Item

```
AdminPanel.doAddItem()
  → validate inputs (title, author, year, category non-empty; year 4 digits)
  → manager.addBook(title, author, year, category, isbn, edition, genre)
      → validate(title, author, year)
      → new Book(IDGenerator.generateItemID(), ...)
      → db.addItem(book)                      ← ArrayList.add()
      → db.pushAdminAction(new AdminAction(ADD, book))  ← Stack.push()
  → mainWindow.setStatus("Book added: ...")
  → mainWindow.refreshCatalogue()             ← ViewItemsPanel reloads table
```

### Undoing a Delete

```
AdminPanel.doUndo()
  → manager.undoLastAction()
      → action = db.popAdminAction()          ← Stack.pop()
      → action.type == DELETE?
          → db.addItem(action.getItem())      ← re-adds to ArrayList
      → return "Undone: DELETE — Book [ITEM-0005] ..."
  → JOptionPane.showMessageDialog(result)
  → mainWindow.refreshCatalogue()
```

---

## 8. Algorithms Explained

### Merge Sort (recursive)

Used in `SortingUtils.mergeSort()`. Divides the list in half recursively until each sub-list has one element (trivially sorted), then merges pairs back together in order.

```
mergeSort([E, B, D, A, C])
  mergeSort([E, B])
    mergeSort([E]) → [E]
    mergeSort([B]) → [B]
    merge([E], [B]) → [B, E]
  mergeSort([D, A, C])
    mergeSort([D]) → [D]
    mergeSort([A, C])
      mergeSort([A]) → [A]
      mergeSort([C]) → [C]
      merge([A], [C]) → [A, C]
    merge([D], [A, C]) → [A, C, D]
  merge([B, E], [A, C, D]) → [A, B, C, D, E]
```

The `merge()` helper creates temporary sublists (copies of the left and right halves) and walks two pointers to merge them back into the original list positions.

### Quick Sort (recursive)

Uses the last element as the pivot. `partition()` moves all elements smaller than the pivot to the left, then places the pivot at its final position. Both sides are then sorted recursively.

```
quickSort([E, B, D, A, C])  pivot = C
  partition → [B, A, C, E, D]  (C is now at index 2)
  quickSort([B, A])  pivot = A → [A, B]
  quickSort([E, D])  pivot = D → [D, E]
  result: [A, B, C, D, E]
```

### Binary Search

Requires the list to be sorted by the target field. Finds the midpoint, compares, and narrows the search window. Returns all items whose field **starts with** the query (prefix match):

```
binarySearch(["Clean Code", "Database Concepts", "Design Patterns", "Java"], "de")
  lo=0 hi=3 mid=1 → "database concepts" < "de" → lo=2
  lo=2 hi=3 mid=2 → "design patterns".startsWith("de") ✓ → matchIdx=2
  expand left:  index 1 → "database concepts".startsWith("de") ✓
  expand right: index 3 → "java".startsWith("de") ✗
  result: ["Database Concepts", "Design Patterns"]
```

### Recursive Search

An index-based recursive scan equivalent to linear search. Exists to satisfy the course requirement for a distinct recursive search component:

```java
recursiveSearchHelper(items, "clean", TITLE, 0, results)
  index 0: "Clean Code".toLowerCase().contains("clean") → add
  recursiveSearchHelper(items, "clean", TITLE, 1, results)
    index 1: "Database Concepts" → no match
    recursiveSearchHelper(items, "clean", TITLE, 2, results)
      ...
      index N: base case → return
```

---

## 9. JSON Persistence Format

### Full schema

```json
{
  "items": [
    {
      "itemID":   "ITEM-0001",
      "type":     "Book",
      "title":    "Clean Code",
      "author":   "Robert C. Martin",
      "year":     2008,
      "category": "Software Engineering",
      "available": true,
      "isbn":     "978-0132350884",
      "edition":  "1st",
      "genre":    "Programming"
    },
    {
      "itemID":      "ITEM-0006",
      "type":        "Magazine",
      "title":       "IEEE Spectrum",
      "author":      "IEEE",
      "year":        2024,
      "category":    "Technology",
      "available":   true,
      "issueNumber": 61,
      "month":       "January"
    },
    {
      "itemID":      "ITEM-0009",
      "type":        "Journal",
      "title":       "Journal of Software Engineering Research",
      "author":      "Dr. A. Kumar",
      "year":        2023,
      "category":    "Software Engineering",
      "available":   true,
      "volume":      15,
      "issueDate":   "2023-06-01",
      "peerReviewed": true
    }
  ],
  "users": [
    {
      "userID":  "USR-0001",
      "name":    "Alice Johnson",
      "email":   "alice@university.edu",
      "role":    "STUDENT",
      "borrowedItemIDs": ["ITEM-0001"],
      "borrowingHistory": [
        {
          "itemID":     "ITEM-0001",
          "itemTitle":  "Clean Code",
          "borrowDate": "2026-02-20",
          "dueDate":    "2026-03-06",
          "returnDate": ""
        }
      ]
    }
  ]
}
```

### Parsing order dependency

Users reference items by ID in `borrowedItemIDs`. The parser **must** load all items before loading users, so that `db.findItemByID()` can resolve the references. This order is hard-coded in `FileHandler.loadData()` and must not be changed.

### Adding a new item type

If you add a fourth item type (e.g. `DVD`):

1. Create `model/DVD.java` extending `LibraryItem` implementing `Borrowable`.
2. Implement `getType()` → `"DVD"`, `getSummary()`, `toJsonFields()`.
3. In `FileHandler.parseItem()`, add `case "DVD": return new DVD(...)`.
4. In `AdminPanel`, add `"DVD"` to the `typeCombo` options and a `dvdExtra` panel.
5. In `LibraryManager`, add `addDVD(...)` factory method.

No other files need to change.

---

## 10. Exception Handling

All custom exceptions extend `RuntimeException` (unchecked). The backend throws them; the GUI catches them.

| Exception | Thrown By | Caught By |
|---|---|---|
| `ItemNotFoundException` | `LibraryManager.deleteItem()`, `getItemByID()` | `AdminPanel`, `ViewItemsPanel` |
| `ItemNotAvailableException` | Declared but not thrown by default — future use | GUI |
| `UserNotFoundException` | `BorrowController.computeOverdueFine()` | GUI fine display |
| `InvalidDataException` | `LibraryManager.validate()`, factory methods | `AdminPanel.doAddItem()` |
| `FileIOException` | `FileHandler.saveData()`, `loadData()`, CSV methods | `AdminPanel` CSV handlers |

**GUI catch pattern:**

```java
try {
    manager.deleteItem(id);
} catch (ItemNotFoundException ex) {
    JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
} catch (Exception ex) {
    JOptionPane.showMessageDialog(this, "Unexpected error: " + ex.getMessage(),
            "Error", JOptionPane.ERROR_MESSAGE);
}
```

Never catch `Exception` silently (empty catch block). Always surface errors to the user via `JOptionPane` or the status bar.

---

## 11. Adding New Features

### New report type

1. Add a method to `ReportGenerator` returning `List<String[]>` (row 0 = headers).
2. Add a button to `AdminPanel.buildReportsSection()`.
3. Wire the button to call `showReport(title, manager.getReportGenerator().yourNewReport())`.

Nothing else changes.

### New sort field (e.g. Category)

1. Add `CATEGORY` to `SortingUtils.SortField`.
2. Add the comparator case in `SortingUtils.comparatorFor()`.
3. Add `"Category"` to `sortFieldCb` in `SearchSortPanel` and handle it in `toSortField()`.

### New search field (e.g. Year)

1. Add `YEAR` to `SearchEngine.SearchField`.
2. Add the field value case in `SearchEngine.getFieldValue()`.
3. Add `"Year"` to `searchFieldCb` in `SearchSortPanel` and handle it in `toSearchField()`.

### Persistent reservation queue

Currently the reservation queue is in-memory only (lost on restart). To persist it:

1. Add `toJson()` to `ReservationEntry`.
2. In `FileHandler.saveData()`, serialize `db.getReservationQueue()` as a `"reservations"` array.
3. In `FileHandler.loadData()`, parse the array and call `db.enqueueReservation()` for each.

---

## 12. Common Pitfalls

| Pitfall | Cause | Fix |
|---|---|---|
| Duplicate IDs after restart | Forgot to call `IDGenerator.seed()` after loading | Already handled in `FileHandler.loadData()` — do not bypass it |
| Binary search returns wrong results | Catalogue not sorted by the searched field | Always call `manager.getSearchEngine().setCurrentSortField(field)` after sorting |
| `NullPointerException` in report | `getMostBorrowedReport()` called with empty history | Reports handle empty data — returns header row only |
| JSON file corrupt after crash | Save interrupted mid-write | `FileHandler` writes to the target path directly; consider writing to a `.tmp` file then renaming for atomicity |
| Stack overflow on recursive sort | Very large list (>5000 items) with quick sort worst-case | Use merge sort for large data sets; quick sort degrades to O(n²) on sorted input |
| `revalidate()` not called after dynamic field toggle | Panel layout not updated | `AdminPanel.updateDynamicFields()` always calls `revalidate()` + `repaint()` — copy this pattern |
| GUI updates from background thread | Timer or non-EDT thread modifying Swing components | Wrap any model update that triggers a GUI refresh in `SwingUtilities.invokeLater()` |

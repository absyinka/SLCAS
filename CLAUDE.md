# SLCAS – Smart Library Circulation & Automation System
## Project Memory (CLAUDE.md)

### Course
COS 202 — MIVA Open University, 2025

### Language & Framework
- **Language:** Java
- **GUI:** Java Swing (separate team)
- **Persistence:** JSON (manual, no external libs)

---

## Project Structure
```
SLCAS/
├── CLAUDE.md                    ← this file
├── SLCAS_Application_Flow.md   ← full design doc
├── src/
│   ├── model/
│   │   ├── LibraryItem.java         [DONE] abstract base class
│   │   ├── Borrowable.java          [DONE] interface
│   │   ├── Book.java                [DONE]
│   │   ├── Magazine.java            [DONE]
│   │   ├── Journal.java             [DONE]
│   │   ├── UserAccount.java         [DONE]
│   │   ├── LibraryDatabase.java     [DONE]
│   │   ├── BorrowRecord.java        [DONE] helper record
│   │   ├── AdminAction.java         [DONE] helper record
│   │   └── ReservationEntry.java    [DONE] helper record
│   ├── controller/
│   │   ├── LibraryManager.java      [DONE] orchestrator
│   │   ├── SearchEngine.java        [DONE] linear/binary/recursive
│   │   ├── BorrowController.java    [DONE] borrow/return/queue/fines
│   │   └── ReportGenerator.java     [DONE] reports
│   ├── utils/
│   │   ├── IDGenerator.java         [DONE]
│   │   ├── SortingUtils.java        [DONE] 4 sort algorithms
│   │   └── FileHandler.java         [DONE] JSON persistence
│   ├── exceptions/
│   │   ├── ItemNotFoundException.java       [DONE]
│   │   ├── ItemNotAvailableException.java   [DONE]
│   │   ├── UserNotFoundException.java       [DONE]
│   │   ├── InvalidDataException.java        [DONE]
│   │   └── FileIOException.java             [DONE]
│   └── gui/                         [GUI TEAM — not started here]
└── data/
    └── library_data.json            ← runtime persistence file
```

---

## Architecture Decisions
- No external dependencies (no Maven, no Gson). JSON is built/parsed manually.
- All sort algorithms hand-coded in `SortingUtils.java` (no Collections.sort).
- `LibraryDatabase` is a singleton passed by reference — no static globals.
- Controller classes receive `LibraryDatabase` via constructor injection.
- GUI calls only methods listed in the Integration Contract below.

---

## Integration Contract (GUI Team API)
| Action | Call | Returns |
|---|---|---|
| Get all items | `libraryManager.getAllItems()` | `List<LibraryItem>` |
| Add item | `libraryManager.addItem(item)` | `void` |
| Delete item | `libraryManager.deleteItem(id)` | `void` |
| Undo | `libraryManager.undoLastAction()` | `String` (description) |
| Save | `libraryManager.saveAll()` | `void` |
| Get overdue users | `libraryManager.getOverdueUsers()` | `List<UserAccount>` |
| Borrow | `borrowController.borrowItem(uid, iid)` | `BorrowResult` |
| Return | `borrowController.returnItem(uid, iid)` | `ReturnResult` |
| Search | `searchEngine.search(query, field)` | `List<LibraryItem>` |
| Sort | `sortingUtils.sort(list, field, algo)` | `List<LibraryItem>` |
| Most borrowed report | `reportGenerator.getMostBorrowedReport()` | `List<String[]>` |
| Overdue users report | `reportGenerator.getOverdueUsersReport()` | `List<String[]>` |
| Category report | `reportGenerator.getCategoryReport()` | `List<String[]>` |
| Import CSV | `fileHandler.importCSV(path, db)` | `int` |
| Export CSV | `fileHandler.exportCSV(path, db)` | `void` |

---

## Key Enums
- `SortField`: TITLE, AUTHOR, YEAR
- `SortAlgorithm`: SELECTION, INSERTION, MERGE, QUICK
- `SearchField`: TITLE, AUTHOR, TYPE
- `BorrowResult`: SUCCESS, QUEUED, USER_NOT_FOUND, ITEM_NOT_FOUND
- `ReturnResult`: SUCCESS, NOT_BORROWED, ITEM_NOT_FOUND, USER_NOT_FOUND
- `AdminActionType`: ADD, DELETE

---

## Recursive Components Implemented
1. `SortingUtils.mergeSort()` — divide & conquer sort
2. `SortingUtils.quickSort()` — recursive quicksort
3. `SearchEngine.recursiveSearch()` — index-based recursive scan
4. `ReportGenerator.countCategory()` — category distribution count
5. `BorrowController.computeOverdueFine()` — recursive fine accumulation

---

## Data File
- Path: `data/library_data.json`
- Format: `{ "items": [...], "users": [...] }`
- Item JSON includes `"type"` field ("Book"/"Magazine"/"Journal") for correct deserialization

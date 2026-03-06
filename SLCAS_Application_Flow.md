# SLCAS – Smart Library Circulation & Automation System
## Application Flow Document
**Course:** COS 202 | **Version:** 1.0

---

## Table of Contents
1. [System Overview](#1-system-overview)
2. [Project Structure](#2-project-structure)
3. [Architecture Diagram](#3-architecture-diagram)
4. [Backend Flow](#4-backend-flow)
   - 4.1 [Data Models (model/)](#41-data-models-model)
   - 4.2 [Controllers (controller/)](#42-controllers-controller)
   - 4.3 [Utilities (utils/)](#43-utilities-utils)
   - 4.4 [Data Structures Used](#44-data-structures-used)
   - 4.5 [Algorithms](#45-algorithms)
   - 4.6 [Persistence (File I/O)](#46-persistence-file-io)
5. [GUI Flow](#5-gui-flow)
   - 5.1 [Main Window](#51-main-window)
   - 5.2 [View Items Panel](#52-view-items-panel)
   - 5.3 [Borrow / Return Panel](#53-borrow--return-panel)
   - 5.4 [Admin Panel](#54-admin-panel)
   - 5.5 [Search & Sort Panel](#55-search--sort-panel)
6. [Feature-by-Feature Flow](#6-feature-by-feature-flow)
   - 6.1 [Add Library Item](#61-add-library-item)
   - 6.2 [Borrow an Item](#62-borrow-an-item)
   - 6.3 [Return an Item](#63-return-an-item)
   - 6.4 [Reservation / Waitlist Queue](#64-reservation--waitlist-queue)
   - 6.5 [Search Flow](#65-search-flow)
   - 6.6 [Sort Flow](#66-sort-flow)
   - 6.7 [Undo Last Admin Action](#67-undo-last-admin-action)
   - 6.8 [Overdue Reminders (Timer)](#68-overdue-reminders-timer)
   - 6.9 [Reports Generation](#69-reports-generation)
   - 6.10 [Save & Load Data](#610-save--load-data)
7. [Integration Points (Backend ↔ GUI)](#7-integration-points-backend--gui)
8. [Error Handling Strategy](#8-error-handling-strategy)
9. [Team Responsibilities Summary](#9-team-responsibilities-summary)

---

## 1. System Overview

SLCAS is a **Java-based desktop application** for a university library. It manages books, magazines, and journals; handles borrowing and returning; maintains a reservation queue; and provides automated overdue reminders — all through an interactive Swing GUI.

```
User Roles
├── Student/Member  →  Borrow, Return, Search, Reserve
└── Admin/Librarian →  Add/Delete items, View Reports, Undo actions, Import/Export data
```

---

## 2. Project Structure

```
SLCAS/
├── model/
│   ├── LibraryItem.java       (Abstract base class)
│   ├── Book.java              (Subclass of LibraryItem + Borrowable)
│   ├── Magazine.java          (Subclass of LibraryItem + Borrowable)
│   ├── Journal.java           (Subclass of LibraryItem + Borrowable)
│   ├── Borrowable.java        (Interface)
│   ├── UserAccount.java       (User entity with borrowing history)
│   └── LibraryDatabase.java   (Central in-memory store)
│
├── controller/
│   ├── LibraryManager.java    (Orchestrates all operations)
│   ├── SearchEngine.java      (Search algorithms)
│   ├── BorrowController.java  (Borrow/Return/Queue logic)
│   └── ReportGenerator.java   (Report computation)
│
├── gui/
│   ├── MainWindow.java        (Root JFrame, tab container)
│   ├── ViewItemsPanel.java    (Browse catalogue)
│   ├── BorrowPanel.java       (Borrow & Return tab)
│   ├── AdminPanel.java        (Admin operations tab)
│   └── SearchSortPanel.java   (Search & Sort tab)
│
└── utils/
    ├── IDGenerator.java       (Unique ID generation)
    ├── FileHandler.java       (JSON/text file I/O)
    └── SortingUtils.java      (Sorting algorithm implementations)
```

---

## 3. Architecture Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                        GUI LAYER                            │
│  MainWindow                                                 │
│  ┌──────────────┬───────────────┬──────────┬─────────────┐  │
│  │ ViewItems    │ Borrow/Return │  Admin   │ Search/Sort │  │
│  │ Panel        │ Panel         │  Panel   │ Panel       │  │
│  └──────┬───────┴───────┬───────┴────┬─────┴──────┬──────┘  │
│         │               │            │            │          │
└─────────┼───────────────┼────────────┼────────────┼──────────┘
          │  (calls)      │  (calls)   │  (calls)  │  (calls)
┌─────────▼───────────────▼────────────▼────────────▼──────────┐
│                    CONTROLLER LAYER                           │
│   LibraryManager  ←→  BorrowController  ←→  SearchEngine     │
│                   ←→  ReportGenerator                        │
└─────────────────────────┬─────────────────────────────────────┘
                          │  (reads/writes)
┌─────────────────────────▼─────────────────────────────────────┐
│                      MODEL LAYER                              │
│   LibraryDatabase  (ArrayList, Queue, Stack, Array cache)     │
│   LibraryItem  →  Book / Magazine / Journal                   │
│   UserAccount  (borrowing history list)                       │
└─────────────────────────┬─────────────────────────────────────┘
                          │  (persists to / loads from)
┌─────────────────────────▼─────────────────────────────────────┐
│                      UTILS LAYER                              │
│   FileHandler (JSON/text)  |  IDGenerator  |  SortingUtils    │
└───────────────────────────────────────────────────────────────┘
```

---

## 4. Backend Flow

> **Backend Team owns everything in `model/`, `controller/`, and `utils/`.**
> The GUI team calls controller methods and reads model objects — they never touch the database or algorithms directly.

---

### 4.1 Data Models (model/)

#### `LibraryItem` (Abstract Class)
```
LibraryItem
├── Fields: itemID, title, author, year, category, isAvailable
├── Abstract Methods: getType(), getSummary()
└── Concrete Methods: getters/setters, toString()
```

#### `Borrowable` (Interface)
```
Borrowable
├── borrow(UserAccount user) : boolean
├── returnItem()             : void
└── isAvailable()            : boolean
```

#### `Book`, `Magazine`, `Journal` (Subclasses)
```
Book         extends LibraryItem, implements Borrowable
├── Extra fields: ISBN, edition, genre
└── Implements: borrow(), returnItem(), getType() = "Book"

Magazine     extends LibraryItem, implements Borrowable
├── Extra fields: issueNumber, month
└── Implements: borrow(), returnItem(), getType() = "Magazine"

Journal      extends LibraryItem, implements Borrowable
├── Extra fields: volume, issueDate, peerReviewed
└── Implements: borrow(), returnItem(), getType() = "Journal"
```

#### `UserAccount`
```
UserAccount
├── Fields: userID, name, email, role (STUDENT | ADMIN)
├── borrowedItems     : List<LibraryItem>
├── borrowingHistory  : List<BorrowRecord>
└── overdueItems()    : List<LibraryItem>   ← recursive charge computation
```

#### `LibraryDatabase` (Central Store)
```
LibraryDatabase
├── itemList         : ArrayList<LibraryItem>    ← main catalogue
├── reservationQueue : Queue<ReservationEntry>   ← waitlist per item
├── adminActionStack : Stack<AdminAction>         ← undo history
├── frequentCache    : LibraryItem[10]            ← fixed-size MFA cache
└── userAccounts     : ArrayList<UserAccount>
```

---

### 4.2 Controllers (controller/)

#### `LibraryManager` (Primary Orchestrator)
```
Flow:
  Application Start
      │
      ▼
  LibraryManager.initialize()
      │── FileHandler.loadData()  →  populates LibraryDatabase
      │── IDGenerator.init()
      └── returns ready state to GUI

  LibraryManager public API (called by GUI):
      ├── addItem(LibraryItem)         →  add to ArrayList, push to Stack
      ├── deleteItem(String itemID)    →  remove from ArrayList, push to Stack
      ├── undoLastAction()             →  pop from Stack, reverse operation
      ├── getAllItems()                →  returns ArrayList<LibraryItem>
      ├── getItemByID(String id)       →  delegates to SearchEngine
      └── saveAll()                   →  delegates to FileHandler
```

#### `BorrowController`
```
Flow:
  borrowItem(userID, itemID)
      │
      ├── Check item availability (isAvailable())
      │       ├── TRUE  → item.borrow(user), update frequentCache, return SUCCESS
      │       └── FALSE → add to reservationQueue, return QUEUED
      │
  returnItem(userID, itemID)
      │
      ├── item.returnItem()  →  set isAvailable = true
      ├── Check reservationQueue for this itemID
      │       └── if next user in queue → auto-notify / auto-assign
      └── return SUCCESS

  computeOverdueFine(userID)          ← RECURSIVE
      │
      └── recursively sum fines across borrowedItems past due date
```

#### `SearchEngine`
```
Search decision logic:
      │
      ├── Is catalogue sorted?
      │       ├── YES → Binary Search  (O log n)
      │       └── NO  → Linear Search  (O n)
      │
      ├── Linear Search
      │       └── iterate ArrayList, match on title / author / type
      │
      ├── Binary Search
      │       └── requires sorted list; compare midpoint, recurse halves
      │
      └── Recursive Search (title/author)
              └── recursiveSearch(list, query, index)
                      ├── base case: index >= list.size() → return null
                      ├── match?    → return item
                      └── else      → recursiveSearch(list, query, index+1)
```

#### `ReportGenerator`
```
generateMostBorrowedReport()
    └── iterate userAccounts → aggregate borrow counts → sort → return top N

generateOverdueUsersReport()
    └── iterate userAccounts → filter overdueItems() not empty → return list

generateCategoryDistributionReport()
    └── recursively count items per category in LibraryDatabase
            recursiveCount(list, category, index)
                ├── base: index >= size → return 0
                ├── match category → 1 + recursiveCount(..., index+1)
                └── no match       → recursiveCount(..., index+1)
```

---

### 4.3 Utilities (utils/)

#### `IDGenerator`
```
generateItemID()   →  "ITEM-" + timestamp + random suffix
generateUserID()   →  "USR-"  + timestamp + random suffix
```

#### `FileHandler`
```
saveData(LibraryDatabase db, String filePath)
    └── serialize ArrayList<LibraryItem> + UserAccounts → JSON/text file

loadData(String filePath) → LibraryDatabase
    └── parse JSON/text → reconstruct objects → return populated database

importCSV(File f)    →  bulk add items from external CSV
exportCSV(File f)    →  write current catalogue to CSV
```

#### `SortingUtils`
```
sort(List<LibraryItem>, SortField, SortAlgorithm)
    │
    ├── SortField:     TITLE | AUTHOR | YEAR
    ├── SortAlgorithm: SELECTION | INSERTION | MERGE | QUICK
    │
    ├── selectionSort(list, comparator)
    ├── insertionSort(list, comparator)
    ├── mergeSort(list, comparator)      ← RECURSIVE, recommended
    └── quickSort(list, lo, hi, comparator)  ← RECURSIVE
```

---

### 4.4 Data Structures Used

| Structure | Location | Purpose |
|-----------|----------|---------|
| `ArrayList<LibraryItem>` | `LibraryDatabase.itemList` | Main catalogue — dynamic add/remove/search |
| `Queue<ReservationEntry>` | `LibraryDatabase.reservationQueue` | FIFO waitlist for unavailable items |
| `Stack<AdminAction>` | `LibraryDatabase.adminActionStack` | Undo history for admin operations |
| `LibraryItem[10]` (Array) | `LibraryDatabase.frequentCache` | Fixed-size Most Frequently Accessed cache |
| `List<BorrowRecord>` | `UserAccount.borrowingHistory` | Per-user borrow log |

---

### 4.5 Algorithms

#### Search Algorithms
| Algorithm | When Used | Complexity |
|-----------|-----------|------------|
| Linear Search | Unsorted catalogue | O(n) |
| Binary Search | After catalogue is sorted | O(log n) |
| Recursive Search | Any state (fallback/demo) | O(n) |

#### Sorting Algorithms
| Algorithm | Best For | Complexity |
|-----------|----------|------------|
| Selection Sort | Small lists / demo | O(n²) |
| Insertion Sort | Nearly-sorted data | O(n²) best O(n) |
| Merge Sort | General purpose (recommended) | O(n log n) |
| Quick Sort | General purpose | O(n log n) avg |

#### Recursive Components
1. `mergeSort()` — divide-and-conquer catalogue sort
2. `recursiveSearch()` — search by title/author
3. `recursiveCategoryCount()` — report generation
4. `computeOverdueFine()` — accumulate fine across items

---

### 4.6 Persistence (File I/O)

```
Application Startup
    │
    └── FileHandler.loadData("library_data.json")
            ├── If file exists  → parse → populate LibraryDatabase
            └── If not found    → initialize empty LibraryDatabase

Application Shutdown / Manual Save
    │
    └── FileHandler.saveData(db, "library_data.json")
            └── serialize all items + user accounts → write file

Import (Admin action)
    └── FileHandler.importCSV(chosenFile) → bulk add to database

Export (Admin action)
    └── FileHandler.exportCSV(chosenFile) → write catalogue to CSV
```

---

## 5. GUI Flow

> **GUI Team owns everything in `gui/`.**
> GUI components call controller methods and display returned model data. No business logic lives in the GUI layer.

---

### 5.1 Main Window

```
Application Entry Point: Main.java
    │
    └── new LibraryManager()           ← backend init
    └── new MainWindow(libraryManager) ← GUI init
            │
            ├── JFrame setup (title, size, layout: BorderLayout)
            ├── JMenuBar (File → Save, Import, Export, Exit)
            │              (Help → About)
            ├── JTabbedPane (CardLayout / tabs)
            │       ├── Tab 0: "View Items"    → ViewItemsPanel
            │       ├── Tab 1: "Borrow/Return" → BorrowPanel
            │       ├── Tab 2: "Admin"         → AdminPanel
            │       └── Tab 3: "Search & Sort" → SearchSortPanel
            ├── StatusBar (JLabel at bottom — shows system messages)
            └── Timer (fires every 60s → checks overdue items → updates StatusBar)
```

---

### 5.2 View Items Panel

```
ViewItemsPanel  (BorderLayout)
    │
    ├── NORTH: filter bar
    │       ├── JComboBox: type filter (All | Book | Magazine | Journal)
    │       └── JButton: "Refresh"
    │
    ├── CENTER: JTable (custom renderer — highlights unavailable items in red)
    │       Columns: ID | Title | Author | Year | Type | Available
    │       Data source: libraryManager.getAllItems()
    │
    └── SOUTH: JLabel (item count display)

Events:
    ├── ComboBox change → filter table rows by type
    ├── Refresh button  → re-fetch from libraryManager.getAllItems()
    └── Row selection   → show item detail in popup dialog
```

---

### 5.3 Borrow / Return Panel

```
BorrowPanel  (GridBagLayout)
    │
    ├── Borrow Section
    │       ├── JTextField: User ID input
    │       ├── JTextField: Item ID input
    │       ├── JButton: "Borrow"
    │       └── Status label (shows: SUCCESS / QUEUED / ERROR)
    │
    └── Return Section
            ├── JTextField: User ID input
            ├── JTextField: Item ID input
            ├── JButton: "Return"
            └── Status label

Events:
    ├── "Borrow" click
    │       ├── validate inputs (not empty, valid IDs)
    │       ├── call borrowController.borrowItem(userID, itemID)
    │       ├── SUCCESS → update status label, refresh ViewItemsPanel table
    │       └── QUEUED  → show dialog "Item unavailable — added to waitlist"
    │
    └── "Return" click
            ├── validate inputs
            ├── call borrowController.returnItem(userID, itemID)
            ├── SUCCESS → update status label, refresh table
            └── if next-in-queue → show dialog "Item reassigned to [user]"
```

---

### 5.4 Admin Panel

```
AdminPanel  (BorderLayout + BoxLayout for form area)
    │
    ├── Add Item Form (BoxLayout vertical)
    │       ├── JComboBox: type (Book | Magazine | Journal)
    │       ├── JTextField: title, author, year (+ type-specific fields shown dynamically)
    │       ├── JButton: "Add Item"
    │       └── JButton: "Clear Form"
    │
    ├── Delete Item
    │       ├── JTextField: Item ID to delete
    │       └── JButton: "Delete"  (with confirm dialog)
    │
    ├── Undo
    │       └── JButton: "Undo Last Action"
    │
    ├── Import / Export
    │       ├── JButton: "Import CSV" → JFileChooser
    │       └── JButton: "Export CSV" → JFileChooser
    │
    └── Reports Section
            ├── JButton: "Most Borrowed"          → opens report dialog
            ├── JButton: "Overdue Users"           → opens report dialog
            └── JButton: "Category Distribution"  → opens report dialog

Events:
    ├── "Add Item" click
    │       ├── validate all fields (input validation with dialog popup on error)
    │       ├── call libraryManager.addItem(newItem)
    │       └── show confirmation, refresh ViewItemsPanel
    │
    ├── "Delete" click
    │       ├── show JOptionPane confirm dialog
    │       ├── call libraryManager.deleteItem(itemID)
    │       └── push action to undo stack (handled by LibraryManager internally)
    │
    ├── "Undo Last Action" click
    │       ├── call libraryManager.undoLastAction()
    │       └── show result in status bar
    │
    ├── Type ComboBox change
    │       └── dynamically show/hide type-specific form fields (Advanced GUI technique)
    │
    └── Report buttons
            └── call reportGenerator.generate*Report()
            └── display in JDialog with JTable or JTextArea
```

---

### 5.5 Search & Sort Panel

```
SearchSortPanel  (BorderLayout)
    │
    ├── NORTH: Search bar
    │       ├── JTextField: search query
    │       ├── JComboBox: search field (Title | Author | Type)
    │       └── JButton: "Search"
    │
    ├── CENTER: JTable (results display — reuses item table model)
    │
    └── SOUTH: Sort controls
            ├── JComboBox: sort field (Title | Author | Year)
            ├── JComboBox: sort algorithm (Selection | Insertion | Merge | Quick)
            └── JButton: "Sort"

Events:
    ├── "Search" click
    │       ├── get query + field from inputs
    │       ├── call searchEngine.search(query, field)
    │       └── display results in CENTER table
    │
    └── "Sort" click
            ├── get field + algorithm from dropdowns
            ├── call sortingUtils.sort(getAllItems(), field, algorithm)
            ├── update table with sorted results
            └── update status bar: "Sorted by [field] using [algorithm]"
```

---

## 6. Feature-by-Feature Flow

### 6.1 Add Library Item

```
Admin clicks "Add Item"
    │
    ├── GUI: collect form values, validate (non-empty, year is numeric)
    │       └── validation fails → JOptionPane.showMessageDialog(error)
    │
    ├── GUI → LibraryManager.addItem(item)
    │
    ├── Backend:
    │       ├── IDGenerator.generateItemID()  → assign unique ID
    │       ├── LibraryDatabase.itemList.add(item)
    │       ├── adminActionStack.push(new AdminAction(ADD, item))
    │       └── return success
    │
    └── GUI: refresh ViewItemsPanel table, show status bar message
```

### 6.2 Borrow an Item

```
User enters userID + itemID → clicks "Borrow"
    │
    ├── GUI: validate inputs
    │
    ├── GUI → BorrowController.borrowItem(userID, itemID)
    │
    ├── Backend:
    │       ├── Find item in LibraryDatabase (SearchEngine.getByID)
    │       ├── Find user in userAccounts
    │       ├── item.isAvailable()?
    │       │       ├── YES:
    │       │       │     ├── item.borrow(user) → isAvailable = false
    │       │       │     ├── user.borrowedItems.add(item)
    │       │       │     ├── user.borrowingHistory.add(new BorrowRecord)
    │       │       │     ├── update frequentCache (if access count qualifies)
    │       │       │     └── return BorrowResult.SUCCESS
    │       │       └── NO:
    │       │             ├── reservationQueue.offer(new ReservationEntry(user, item))
    │       │             └── return BorrowResult.QUEUED
    │
    └── GUI: display result → refresh table
```

### 6.3 Return an Item

```
User enters userID + itemID → clicks "Return"
    │
    ├── GUI: validate inputs
    │
    ├── GUI → BorrowController.returnItem(userID, itemID)
    │
    ├── Backend:
    │       ├── item.returnItem() → isAvailable = true
    │       ├── remove from user.borrowedItems
    │       ├── compute fine if overdue (computeOverdueFine — recursive)
    │       ├── poll reservationQueue for this itemID
    │       │       └── if next user found → auto-borrow for them, notify GUI
    │       └── return ReturnResult (SUCCESS, fine amount, next-user info)
    │
    └── GUI: show fine dialog if applicable, show reassignment notice, refresh table
```

### 6.4 Reservation / Waitlist Queue

```
Queue: FIFO — LinkedList<ReservationEntry>
    │
    ├── Entry structure: { itemID, userID, timestamp }
    │
    ├── Enqueue: when borrowItem() returns QUEUED
    │
    └── Dequeue: when returnItem() is called
            └── poll() → get next waiting user → auto-trigger borrow flow
```

### 6.5 Search Flow

```
User enters query + field → clicks "Search"
    │
    ├── GUI → SearchEngine.search(query, field)
    │
    ├── Backend decision:
    │       ├── Is list currently sorted by the same field?
    │       │       ├── YES → binarySearch(query)    ← O(log n)
    │       │       └── NO  → linearSearch(query)    ← O(n)
    │       │
    │       └── Both can fall back to recursiveSearch() for demonstration
    │
    └── GUI: populate results table with returned List<LibraryItem>
```

### 6.6 Sort Flow

```
Admin/User selects field + algorithm → clicks "Sort"
    │
    ├── GUI → SortingUtils.sort(items, SortField.TITLE, SortAlgorithm.MERGE)
    │
    ├── Backend:
    │       ├── mergeSort (example):
    │       │     mergeSort(list, lo, hi, comparator)
    │       │         ├── base: lo >= hi → return
    │       │         ├── mid = (lo + hi) / 2
    │       │         ├── mergeSort(list, lo, mid, comparator)   ← recursive
    │       │         ├── mergeSort(list, mid+1, hi, comparator) ← recursive
    │       │         └── merge(list, lo, mid, hi, comparator)
    │       │
    │       └── LibraryDatabase.itemList is now sorted (in-place)
    │
    └── GUI: refresh table with sorted order, update status bar
```

### 6.7 Undo Last Admin Action

```
Admin clicks "Undo Last Action"
    │
    ├── GUI → LibraryManager.undoLastAction()
    │
    ├── Backend:
    │       ├── adminActionStack.isEmpty()? → return NO_ACTION
    │       ├── action = adminActionStack.pop()
    │       ├── action.type == ADD    → remove item from database
    │       └── action.type == DELETE → re-add item to database
    │
    └── GUI: refresh table, show status bar "Undo: [action description]"
```

### 6.8 Overdue Reminders (Timer)

```
MainWindow Timer (fires every 60 seconds)
    │
    └── Timer ActionListener triggered
            │
            ├── GUI → LibraryManager.getOverdueItems()
            │
            ├── Backend: iterate all userAccounts
            │       └── for each user → check borrowedItems due dates
            │               └── overdue? → add to overdue list
            │
            └── GUI:
                    ├── StatusBar → "⚠ X item(s) overdue"
                    └── Optional: JOptionPane notification for admin
```

### 6.9 Reports Generation

```
Admin clicks a Report button
    │
    ├── "Most Borrowed" → ReportGenerator.generateMostBorrowedReport()
    │       ├── Aggregate borrow counts across all BorrowRecords
    │       ├── Sort by count descending
    │       └── Return List<ReportEntry>
    │
    ├── "Overdue Users" → ReportGenerator.generateOverdueUsersReport()
    │       └── Filter users where overdueItems().size() > 0
    │
    └── "Category Distribution" → ReportGenerator.generateCategoryDistributionReport()
            └── recursiveCategoryCount() for each type (Book, Magazine, Journal)

GUI: display each report in a JDialog with JTable
```

### 6.10 Save & Load Data

```
On Startup:
    LibraryManager.initialize()
        └── FileHandler.loadData("library_data.json")
                ├── Parse JSON array of items → reconstruct LibraryItem subclasses
                ├── Parse JSON array of users → reconstruct UserAccount objects
                └── Re-populate LibraryDatabase

On Save (File → Save or shutdown hook):
    LibraryManager.saveAll()
        └── FileHandler.saveData(db, "library_data.json")
                ├── Serialize all LibraryItems (type field preserved for correct deserialization)
                └── Serialize all UserAccounts with borrowing history

On Import (Admin → Import CSV):
    GUI: JFileChooser → get File
    FileHandler.importCSV(file) → parse → addItem() for each row

On Export (Admin → Export CSV):
    GUI: JFileChooser → get File
    FileHandler.exportCSV(file, db.itemList)
```

---

## 7. Integration Points (Backend ↔ GUI)

These are the exact method calls that connect GUI actions to backend logic. The GUI team must call these; the backend team must implement them.

| GUI Action | Controller Method | Returns |
|---|---|---|
| Refresh catalogue | `libraryManager.getAllItems()` | `List<LibraryItem>` |
| Add item | `libraryManager.addItem(item)` | `void` / throws |
| Delete item | `libraryManager.deleteItem(id)` | `boolean` |
| Undo | `libraryManager.undoLastAction()` | `AdminAction` or null |
| Borrow | `borrowController.borrowItem(uid, iid)` | `BorrowResult` enum |
| Return | `borrowController.returnItem(uid, iid)` | `ReturnResult` |
| Search | `searchEngine.search(query, field)` | `List<LibraryItem>` |
| Sort | `sortingUtils.sort(list, field, algo)` | `List<LibraryItem>` (sorted) |
| Overdue check | `libraryManager.getOverdueItems()` | `List<UserAccount>` |
| Report: most borrowed | `reportGenerator.generateMostBorrowedReport()` | `List<ReportEntry>` |
| Report: overdue users | `reportGenerator.generateOverdueUsersReport()` | `List<UserAccount>` |
| Report: categories | `reportGenerator.generateCategoryDistributionReport()` | `Map<String, Integer>` |
| Save | `libraryManager.saveAll()` | `void` |
| Load | `libraryManager.initialize()` | `void` |
| Import CSV | `fileHandler.importCSV(file)` | `int` (items added) |
| Export CSV | `fileHandler.exportCSV(file, items)` | `void` |

---

## 8. Error Handling Strategy

All backend methods should throw typed exceptions. GUI catches and shows dialogs.

```
Custom Exceptions (backend):
    ├── ItemNotFoundException      (search / borrow by invalid ID)
    ├── ItemNotAvailableException  (borrow when unavailable — soft, triggers queue)
    ├── UserNotFoundException      (invalid user ID)
    ├── InvalidDataException       (malformed input — caught before hitting backend)
    └── FileIOException            (load/save failure)

GUI error handling pattern:
    try {
        borrowController.borrowItem(userID, itemID);
    } catch (ItemNotFoundException e) {
        JOptionPane.showMessageDialog(this, "Item not found: " + itemID, "Error", ERROR);
    } catch (Exception e) {
        JOptionPane.showMessageDialog(this, "Unexpected error: " + e.getMessage(), ...);
    }
```

---

## 9. Team Responsibilities Summary

### Backend Team

| Responsibility | Files |
|---|---|
| Define class hierarchy | `model/LibraryItem.java`, `Book.java`, `Magazine.java`, `Journal.java`, `Borrowable.java` |
| User management | `model/UserAccount.java` |
| Central data store | `model/LibraryDatabase.java` |
| Orchestration | `controller/LibraryManager.java` |
| Borrow/Return/Queue | `controller/BorrowController.java` |
| Search algorithms | `controller/SearchEngine.java` |
| Report generation | `controller/ReportGenerator.java` |
| File I/O | `utils/FileHandler.java` |
| ID generation | `utils/IDGenerator.java` |
| Sort algorithms | `utils/SortingUtils.java` |
| Exception classes | (inline or separate `exceptions/` package) |
| **Deliverable** | All controller/model/utils classes compiled and unit-tested independently |

### GUI Team

| Responsibility | Files |
|---|---|
| Application entry point | `Main.java` |
| Main window & tab layout | `gui/MainWindow.java` |
| Catalogue view | `gui/ViewItemsPanel.java` |
| Borrow/Return UI | `gui/BorrowPanel.java` |
| Admin operations UI | `gui/AdminPanel.java` |
| Search & Sort UI | `gui/SearchSortPanel.java` |
| Overdue timer UI | Inside `MainWindow.java` |
| Report dialogs | Inside `AdminPanel.java` (or separate dialog classes) |
| Custom table renderers | As needed per panel |
| **Deliverable** | Fully wired Swing UI that calls backend API from Section 7 |

### Shared / Both Teams

| Task |
|---|
| Agree on method signatures in Section 7 before coding starts |
| Define data transfer objects (or pass model objects directly) |
| Integration testing: GUI + backend end-to-end |
| JSON schema for persistence file |

---

*Document prepared from SLCAS project specification — COS 202, MIVA Open University, 2025.*

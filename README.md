# SLCAS — Smart Library Circulation & Automation System

A fully featured university library management application built in **Java** with a **Java Swing** GUI.
Developed for **COS 202 — MIVA Open University, 2025**.

---

## Features

| Category | Features |
|---|---|
| **Catalogue** | Add Books, Magazines, and Journals; delete items; undo last action |
| **Borrowing** | Borrow and return items; automatic reservation waitlist queue |
| **Search** | Search by title, author, or type using linear, binary, or recursive search |
| **Sorting** | Sort by title, author, or year using Selection, Insertion, Merge, or Quick Sort |
| **Fines** | Automatic overdue fine computation ($0.50/day, recursive accumulation) |
| **Reports** | Most borrowed items; users with overdue items; category distribution |
| **Persistence** | Automatic JSON save/load on startup and shutdown; no database required |
| **Import/Export** | Bulk import items from CSV; export full catalogue to CSV |
| **Notifications** | Periodic overdue check (every 60 seconds) displayed in the status bar |

---

## Requirements

- **Java 11 or higher** (uses `java.time`, `Files.writeString`)
- No build tools required (no Maven, no Gradle)
- No external libraries (no Gson, no Jackson, no JDBC)

---

## Building

From the project root directory:

```bash
# Create the output directory (first time only)
mkdir -p out

# Compile all source files
javac -d out -sourcepath src $(find src -name "*.java" | tr '\n' ' ')
```

On Windows Command Prompt (if not using bash):

```cmd
for /R src %f in (*.java) do set FILES=%FILES% %f
javac -d out %FILES%
```

---

## Running

```bash
java -cp out Main
```

The application loads `data/library_data.json` automatically on startup. A seed file with 15 sample items and 5 sample users is included.

---

## Project Structure

```
SLCAS/
├── README.md
├── DEVELOPER_MANUAL.md
├── SLCAS_Application_Flow.md      ← full design and flow document
│
├── src/
│   ├── Main.java                  ← application entry point
│   │
│   ├── model/                     ← data layer
│   │   ├── LibraryItem.java       ← abstract base class
│   │   ├── Borrowable.java        ← interface
│   │   ├── Book.java
│   │   ├── Magazine.java
│   │   ├── Journal.java
│   │   ├── UserAccount.java
│   │   ├── LibraryDatabase.java   ← central in-memory store
│   │   ├── BorrowRecord.java      ← borrow transaction record
│   │   ├── AdminAction.java       ← undo stack entry
│   │   └── ReservationEntry.java  ← waitlist queue entry
│   │
│   ├── controller/                ← business logic
│   │   ├── LibraryManager.java    ← main orchestrator (GUI entry point)
│   │   ├── SearchEngine.java      ← linear / binary / recursive search
│   │   ├── BorrowController.java  ← borrow / return / fines
│   │   └── ReportGenerator.java   ← admin reports
│   │
│   ├── utils/
│   │   ├── IDGenerator.java       ← unique ID generation
│   │   ├── SortingUtils.java      ← 4 sort algorithms (hand-coded)
│   │   └── FileHandler.java       ← JSON + CSV persistence
│   │
│   ├── exceptions/
│   │   ├── ItemNotFoundException.java
│   │   ├── ItemNotAvailableException.java
│   │   ├── UserNotFoundException.java
│   │   ├── InvalidDataException.java
│   │   └── FileIOException.java
│   │
│   └── gui/
│       ├── MainWindow.java        ← root JFrame, tabs, menu, timer
│       ├── ViewItemsPanel.java    ← browse catalogue (Tab 1)
│       ├── BorrowPanel.java       ← borrow & return (Tab 2)
│       ├── AdminPanel.java        ← admin operations (Tab 3)
│       └── SearchSortPanel.java   ← search & sort (Tab 4)
│
├── data/
│   └── library_data.json          ← persistence file (auto-created on save)
│
└── out/                           ← compiled .class files (gitignore this)
```

---

## Application Tabs

### Tab 1 — View Items
Browse the full catalogue with type filtering (All / Book / Magazine / Journal). Unavailable (borrowed) items are highlighted in red. Double-click any row to view the full item details.

### Tab 2 — Borrow / Return
Enter a **User ID** and **Item ID** to borrow or return an item. If an item is already borrowed, the user is automatically added to the reservation waitlist. When an item is returned, it is auto-assigned to the next user in the queue.

### Tab 3 — Admin
- **Add Item** — form with dynamic fields (fields change based on type selection)
- **Delete / Undo** — delete an item by ID; undo the last add or delete
- **Register User** — create a new student or admin user account
- **Reports** — view most borrowed items, overdue users, and category distribution in a dialog table
- **Import / Export CSV** — bulk load items from a CSV file or export the catalogue

### Tab 4 — Search & Sort
Search the catalogue by title, author, or type. Sort by title, author, or year using any of the four algorithms. The search engine automatically uses binary search after a sort is applied.

---

## Keyboard Shortcuts

| Shortcut | Action |
|---|---|
| `Ctrl + S` | Save data to file |
| `Alt + F4` | Exit (with save prompt) |
| `Alt + F` | Open File menu |
| `Alt + H` | Open Help menu |
| `Enter` (in search box) | Trigger search |

---

## Data Persistence

Data is stored in `data/library_data.json`. The format is:

```json
{
  "items": [
    {
      "itemID": "ITEM-0001",
      "type": "Book",
      "title": "Clean Code",
      "author": "Robert C. Martin",
      "year": 2008,
      "category": "Software Engineering",
      "available": true,
      "isbn": "978-0132350884",
      "edition": "1st",
      "genre": "Programming"
    }
  ],
  "users": [
    {
      "userID": "USR-0001",
      "name": "Alice Johnson",
      "email": "alice@university.edu",
      "role": "STUDENT",
      "borrowedItemIDs": [],
      "borrowingHistory": []
    }
  ]
}
```

The file is loaded at startup and saved when:
- The user selects **File → Save** (`Ctrl+S`)
- The user chooses **"Yes"** on the exit dialog

---

## CSV Import Format

To import items in bulk, prepare a CSV with the following header and columns:

```
type,title,author,year,category,extra1,extra2,extra3
```

| Column | Book | Magazine | Journal |
|---|---|---|---|
| extra1 | isbn | issueNumber | volume |
| extra2 | edition | month | issueDate |
| extra3 | genre | _(blank)_ | peerReviewed (true/false) |

Example:

```csv
type,title,author,year,category,extra1,extra2,extra3
Book,Clean Code,Robert C. Martin,2008,Software Engineering,978-0132350884,1st,Programming
Magazine,IEEE Spectrum,IEEE,2024,Technology,61,January,
Journal,ACM Transactions,ACM,2022,Computer Science,18,2022-09-15,true
```

---

## Sample User IDs and Item IDs (Seed Data)

| User ID | Name | Role |
|---|---|---|
| USR-0001 | Alice Johnson | STUDENT |
| USR-0002 | Bob Smith | STUDENT |
| USR-0003 | Dr. Carol White | ADMIN |
| USR-0004 | David Brown | STUDENT |
| USR-0005 | Emma Wilson | STUDENT |

Item IDs range from `ITEM-0001` to `ITEM-0015`. Use these to test borrow and return operations immediately on first run.

---

## Course Compliance Checklist

| Requirement | Implementation |
|---|---|
| Abstract class | `LibraryItem` |
| Interface | `Borrowable` |
| Subclasses | `Book`, `Magazine`, `Journal` |
| Polymorphism | `LibraryManager` processes any `LibraryItem` |
| Encapsulation | All fields private with getters/setters |
| `ArrayList` | `LibraryDatabase.itemList` |
| `Queue` | `LibraryDatabase.reservationQueue` (LinkedList) |
| `Stack` | `LibraryDatabase.adminActionStack` |
| Fixed array | `LibraryDatabase.frequentCache[10]` |
| Linear search | `SearchEngine.linearSearch()` |
| Binary search | `SearchEngine.binarySearch()` |
| Recursive search | `SearchEngine.recursiveSearch()` |
| Selection sort | `SortingUtils.selectionSort()` |
| Insertion sort | `SortingUtils.insertionSort()` |
| Merge sort | `SortingUtils.mergeSort()` _(recursive)_ |
| Quick sort | `SortingUtils.quickSort()` _(recursive)_ |
| Recursive component | 5 total (merge sort, quick sort, search, category count, fine) |
| Event-driven GUI | All panels use `ActionListener`, `MouseListener`, `ChangeListener` |
| Tabbed panels | 4 tabs in `MainWindow` |
| Timer | 60-second overdue notification in `MainWindow` |
| File persistence | JSON via `FileHandler` |
| Custom exceptions | 5 exception classes in `exceptions/` |
| Input validation | `JOptionPane` error dialogs on all forms |
| Custom renderer | `AvailabilityRenderer` in `ViewItemsPanel` |
| Dynamic components | Type-sensitive form fields in `AdminPanel` |
| File chooser | `JFileChooser` for CSV import/export |
| Keyboard shortcuts | `Ctrl+S`, `Alt+F4`, menu mnemonics |
| Tooltips | All buttons and input fields |

---

## Authors

COS 202 Project Team — MIVA Open University, 2025

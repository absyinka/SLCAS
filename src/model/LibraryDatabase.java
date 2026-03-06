package model;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Stack;

/**
 * Central in-memory data store for the library system.
 *
 * Data structures used:
 *   ArrayList   – main item catalogue (dynamic, indexed)
 *   Queue       – reservation / waitlist (FIFO, per-item)
 *   Stack       – admin undo history (LIFO)
 *   Array       – fixed-size Most-Frequently-Accessed cache (MFA)
 */
public class LibraryDatabase {

    // ── Core collections ─────────────────────────────────────────────────────

    /** Main catalogue – all library items. */
    private final List<LibraryItem>       itemList          = new ArrayList<>();

    /** Registered users / members. */
    private final List<UserAccount>       userAccounts      = new ArrayList<>();

    /** Reservation / waitlist queue. Entries ordered by arrival time. */
    private final Queue<ReservationEntry> reservationQueue  = new LinkedList<>();

    /** Undo stack – last admin ADD or DELETE can be reversed. */
    private final Stack<AdminAction>      adminActionStack  = new Stack<>();

    /** Fixed-size Most-Frequently-Accessed item cache (max 10 slots). */
    private static final int              CACHE_SIZE        = 10;
    private final LibraryItem[]           frequentCache     = new LibraryItem[CACHE_SIZE];
    private final int[]                   accessCounts      = new int[CACHE_SIZE];

    // ── Item catalogue ────────────────────────────────────────────────────────

    public void addItem(LibraryItem item) {
        itemList.add(item);
    }

    public boolean removeItem(String itemID) {
        return itemList.removeIf(i -> i.getItemID().equals(itemID));
    }

    public LibraryItem findItemByID(String itemID) {
        for (LibraryItem item : itemList) {
            if (item.getItemID().equals(itemID)) return item;
        }
        return null;
    }

    public List<LibraryItem> getItemList() { return itemList; }

    // ── User accounts ─────────────────────────────────────────────────────────

    public void addUser(UserAccount user) {
        userAccounts.add(user);
    }

    public boolean removeUser(String userID) {
        return userAccounts.removeIf(u -> u.getUserID().equals(userID));
    }

    public UserAccount findUserByID(String userID) {
        for (UserAccount u : userAccounts) {
            if (u.getUserID().equals(userID)) return u;
        }
        return null;
    }

    public List<UserAccount> getUserAccounts() { return userAccounts; }

    // ── Reservation queue ─────────────────────────────────────────────────────

    public void enqueueReservation(ReservationEntry entry) {
        reservationQueue.offer(entry);
    }

    /**
     * Polls the queue for the next reservation matching the given itemID.
     * Non-matching entries are preserved back in the queue.
     */
    public ReservationEntry dequeueReservation(String itemID) {
        int size = reservationQueue.size();
        for (int i = 0; i < size; i++) {
            ReservationEntry entry = reservationQueue.poll();
            if (entry != null && entry.getItemID().equals(itemID)) {
                return entry;
            }
            if (entry != null) reservationQueue.offer(entry); // keep non-matching
        }
        return null;
    }

    public boolean hasReservation(String itemID) {
        for (ReservationEntry e : reservationQueue) {
            if (e.getItemID().equals(itemID)) return true;
        }
        return false;
    }

    public Queue<ReservationEntry> getReservationQueue() { return reservationQueue; }

    // ── Undo stack ────────────────────────────────────────────────────────────

    public void pushAdminAction(AdminAction action) { adminActionStack.push(action); }
    public AdminAction popAdminAction()             { return adminActionStack.isEmpty() ? null : adminActionStack.pop(); }
    public boolean     hasUndoActions()             { return !adminActionStack.isEmpty(); }
    public Stack<AdminAction> getAdminActionStack() { return adminActionStack; }

    // ── MFA cache ─────────────────────────────────────────────────────────────

    /**
     * Records an access for the given item and updates the fixed-size cache.
     * If the item is already in the cache its count is incremented;
     * otherwise it replaces the slot with the lowest count.
     */
    public void recordAccess(LibraryItem item) {
        // Check if already cached
        for (int i = 0; i < CACHE_SIZE; i++) {
            if (frequentCache[i] != null && frequentCache[i].getItemID().equals(item.getItemID())) {
                accessCounts[i]++;
                return;
            }
        }
        // Find an empty slot first
        for (int i = 0; i < CACHE_SIZE; i++) {
            if (frequentCache[i] == null) {
                frequentCache[i] = item;
                accessCounts[i]  = 1;
                return;
            }
        }
        // Evict the slot with the lowest access count
        int minIdx = 0;
        for (int i = 1; i < CACHE_SIZE; i++) {
            if (accessCounts[i] < accessCounts[minIdx]) minIdx = i;
        }
        frequentCache[minIdx] = item;
        accessCounts[minIdx]  = 1;
    }

    public LibraryItem[] getFrequentCache()  { return frequentCache; }
    public int[]         getAccessCounts()   { return accessCounts; }
}

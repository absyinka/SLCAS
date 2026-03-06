package utils;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Generates unique IDs for items and users.
 * Format:  ITEM-0001, ITEM-0002 ... / USR-0001, USR-0002 ...
 */
public class IDGenerator {

    private static final AtomicInteger itemCounter = new AtomicInteger(0);
    private static final AtomicInteger userCounter = new AtomicInteger(0);

    /**
     * Seeds the counters so IDs don't collide after loading from file.
     * Call once during LibraryManager.initialize() after data is loaded.
     */
    public static void seed(int maxItemSeq, int maxUserSeq) {
        if (maxItemSeq > itemCounter.get()) itemCounter.set(maxItemSeq);
        if (maxUserSeq > userCounter.get()) userCounter.set(maxUserSeq);
    }

    public static String generateItemID() {
        return String.format("ITEM-%04d", itemCounter.incrementAndGet());
    }

    public static String generateUserID() {
        return String.format("USR-%04d", userCounter.incrementAndGet());
    }

    /**
     * Parses the numeric sequence from an ID string, e.g. "ITEM-0042" → 42.
     * Returns 0 if the ID does not match the expected format.
     */
    public static int parseSequence(String id) {
        if (id == null) return 0;
        int dash = id.lastIndexOf('-');
        if (dash < 0) return 0;
        try {
            return Integer.parseInt(id.substring(dash + 1));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}

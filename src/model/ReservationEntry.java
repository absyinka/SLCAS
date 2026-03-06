package model;

import java.time.LocalDateTime;

/**
 * A single entry in the reservation/waitlist queue for a borrowed item.
 */
public class ReservationEntry {

    private final String        itemID;
    private final String        userID;
    private final LocalDateTime timestamp;

    public ReservationEntry(String itemID, String userID) {
        this.itemID    = itemID;
        this.userID    = userID;
        this.timestamp = LocalDateTime.now();
    }

    public String        getItemID()    { return itemID; }
    public String        getUserID()    { return userID; }
    public LocalDateTime getTimestamp() { return timestamp; }

    // ── JSON ─────────────────────────────────────────────────────────────────

    public String toJson() {
        return "{" +
               "\"itemID\":\"" + itemID + "\"," +
               "\"userID\":\"" + userID + "\"," +
               "\"timestamp\":\"" + timestamp + "\"" +
               "}";
    }

    @Override
    public String toString() {
        return String.format("Reservation{item=%s, user=%s, at=%s}", itemID, userID, timestamp);
    }
}

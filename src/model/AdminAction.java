package model;

/**
 * Records a single admin operation so it can be undone via the undo stack.
 */
public class AdminAction {

    public enum Type { ADD, DELETE }

    private final Type        type;
    private final LibraryItem item;
    private final String      description;

    public AdminAction(Type type, LibraryItem item) {
        this.type  = type;
        this.item  = item;
        this.description = type.name() + " — " + item.getType()
                           + " [" + item.getItemID() + "] \"" + item.getTitle() + "\"";
    }

    public Type        getType()        { return type; }
    public LibraryItem getItem()        { return item; }
    public String      getDescription() { return description; }

    @Override
    public String toString() { return description; }
}

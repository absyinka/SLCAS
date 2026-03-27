package gui;

import controller.BorrowController.BorrowResult;
import controller.BorrowController.ReturnResult;
import controller.LibraryManager;
import model.LibraryItem;
import model.UserAccount;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tab 1 — Borrow & Return.
 *
 * Layout: GridBagLayout
 * Two titled sections: Borrow and Return.
 * Each has User ID + Item ID dropdown selectors and a submit button.
 */
public class BorrowPanel extends JPanel {

    private final LibraryManager manager;
    private final MainWindow     mainWindow;

    // Borrow fields
    private final JComboBox<String> borrowUserCombo = new JComboBox<>();
    private final JComboBox<String> borrowItemCombo = new JComboBox<>();
    private final JLabel            borrowStatus    = new JLabel(" ");
    private final Map<String, String> userIdMap     = new HashMap<>(); // name → ID
    private final Map<String, String> itemIdMap     = new HashMap<>(); // title → ID

    // Return fields
    private final JComboBox<String> returnUserCombo = new JComboBox<>();
    private final JComboBox<String> returnItemCombo = new JComboBox<>();
    private final JLabel            returnStatus    = new JLabel(" ");
    private final Map<String, String> returnUserIdMap = new HashMap<>(); // name → ID
    private final Map<String, String> returnItemIdMap = new HashMap<>(); // title → ID

    public BorrowPanel(LibraryManager manager, MainWindow mainWindow) {
        this.manager    = manager;
        this.mainWindow = mainWindow;
        setLayout(new GridBagLayout());
        populateDropdowns();
        buildUI();
    }

    // ── Layout ────────────────────────────────────────────────────────────────

    private void buildUI() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets  = new Insets(12, 30, 12, 30);
        gbc.fill    = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;

        // Row 0 — Borrow section
        gbc.gridx = 0; gbc.gridy = 0;
        add(buildBorrowSection(), gbc);

        // Row 1 — Return section
        gbc.gridy = 1;
        add(buildReturnSection(), gbc);

        // Row 2 — vertical filler
        gbc.gridy = 2; gbc.weighty = 1;
        add(new JPanel(), gbc);
    }

    private JPanel buildBorrowSection() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Borrow an Item",
                TitledBorder.LEFT, TitledBorder.TOP,
                new Font("SansSerif", Font.BOLD, 13), new Color(30, 100, 180)));

        GridBagConstraints g = formGbc();

        addFormRow(panel, g, 0, "Borrower Name:", borrowUserCombo, "Select the borrower's name");
        addFormRow(panel, g, 1, "Item Name:", borrowItemCombo, "Select the item to borrow");

        JButton borrowBtn = new JButton("Borrow");
        borrowBtn.setBackground(new Color(30, 100, 180));
        borrowBtn.setForeground(Color.WHITE);
        borrowBtn.setFont(new Font("SansSerif", Font.BOLD, 12));
        borrowBtn.setToolTipText("Borrow the specified item for this user");
        borrowBtn.addActionListener(e -> doBorrow());

        g.gridx = 1; g.gridy = 2;
        panel.add(borrowBtn, g);

        g.gridx = 0; g.gridy = 3; g.gridwidth = 2;
        borrowStatus.setFont(new Font("SansSerif", Font.ITALIC, 12));
        panel.add(borrowStatus, g);

        return panel;
    }

    private JPanel buildReturnSection() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Return an Item",
                TitledBorder.LEFT, TitledBorder.TOP,
                new Font("SansSerif", Font.BOLD, 13), new Color(0, 140, 70)));

        GridBagConstraints g = formGbc();

        addFormRow(panel, g, 0, "Borrower Name:", returnUserCombo, "Select the user returning the item");
        addFormRow(panel, g, 1, "Item Name:", returnItemCombo, "Select the item being returned");

        JButton returnBtn = new JButton("Return");
        returnBtn.setBackground(new Color(0, 140, 70));
        returnBtn.setForeground(Color.WHITE);
        returnBtn.setFont(new Font("SansSerif", Font.BOLD, 12));
        returnBtn.setToolTipText("Process the return of this item");
        returnBtn.addActionListener(e -> doReturn());

        g.gridx = 1; g.gridy = 2;
        panel.add(returnBtn, g);

        g.gridx = 0; g.gridy = 3; g.gridwidth = 2;
        returnStatus.setFont(new Font("SansSerif", Font.ITALIC, 12));
        panel.add(returnStatus, g);

        return panel;
    }

    // ── Actions ───────────────────────────────────────────────────────────────

    private void doBorrow() {
        String selectedUser = (String) borrowUserCombo.getSelectedItem();
        String selectedItem = (String) borrowItemCombo.getSelectedItem();

        if (selectedUser == null || selectedItem == null || userIdMap.isEmpty() || itemIdMap.isEmpty()) {
            showError("Please select both a borrower and an item.");
            return;
        }

        String uid = userIdMap.get(selectedUser);
        String iid = itemIdMap.get(selectedItem);

        BorrowResult result = manager.getBorrowController().borrowItem(uid, iid);
        switch (result) {
            case SUCCESS:
                borrowStatus.setForeground(new Color(0, 130, 0));
                borrowStatus.setText("✔  Item borrowed successfully.");
                mainWindow.setStatus("Borrowed: " + iid + " → " + uid);
                mainWindow.refreshCatalogue();
                refreshDropdowns();
                borrowUserCombo.setSelectedIndex(0);
                borrowItemCombo.setSelectedIndex(0);
                break;
            case QUEUED:
                borrowStatus.setForeground(new Color(200, 120, 0));
                borrowStatus.setText("⏳  Item unavailable — added to waitlist.");
                mainWindow.setStatus("Queued: " + uid + " is waiting for " + iid);
                borrowUserCombo.setSelectedIndex(0);
                borrowItemCombo.setSelectedIndex(0);
                break;
            case USER_NOT_FOUND:
                borrowStatus.setForeground(Color.RED);
                borrowStatus.setText("✘  User ID not found: " + uid);
                break;
            case ITEM_NOT_FOUND:
                borrowStatus.setForeground(Color.RED);
                borrowStatus.setText("✘  Item ID not found: " + iid);
                break;
        }
    }

    private void doReturn() {
        String selectedUser = (String) returnUserCombo.getSelectedItem();
        String selectedItem = (String) returnItemCombo.getSelectedItem();

        if (selectedUser == null || selectedItem == null || returnUserIdMap.isEmpty() || returnItemIdMap.isEmpty()) {
            showError("Please select both a borrower and an item.");
            return;
        }

        String uid = returnUserIdMap.get(selectedUser);
        String iid = returnItemIdMap.get(selectedItem);

        ReturnResult result = manager.getBorrowController().returnItem(uid, iid);
        switch (result) {
            case SUCCESS:
                returnStatus.setForeground(new Color(0, 130, 0));

                // Check if auto-assigned to next user in queue
                String nextUser = manager.getBorrowController().getNextReservationUserID(iid);
                if (nextUser != null) {
                    returnStatus.setText("✔  Returned. Auto-assigned to waiting user: " + nextUser);
                } else {
                    returnStatus.setText("✔  Item returned successfully.");
                }
                mainWindow.setStatus("Returned: " + iid + " from " + uid);
                mainWindow.refreshCatalogue();
                refreshDropdowns();
                returnUserCombo.setSelectedIndex(0);
                returnItemCombo.setSelectedIndex(0);
                break;
            case NOT_BORROWED:
                returnStatus.setForeground(Color.RED);
                returnStatus.setText("✘  This user has not borrowed item: " + iid);
                break;
            case USER_NOT_FOUND:
                returnStatus.setForeground(Color.RED);
                returnStatus.setText("✘  User ID not found: " + uid);
                break;
            case ITEM_NOT_FOUND:
                returnStatus.setForeground(Color.RED);
                returnStatus.setText("✘  Item ID not found: " + iid);
                break;
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Populates dropdown menus with current users and items from the database.
     */
    private void populateDropdowns() {
        userIdMap.clear();
        itemIdMap.clear();
        returnUserIdMap.clear();
        returnItemIdMap.clear();

        borrowUserCombo.removeAllItems();
        borrowItemCombo.removeAllItems();
        returnUserCombo.removeAllItems();
        returnItemCombo.removeAllItems();

        // Add placeholder options
        borrowUserCombo.addItem("-- Select Borrower --");
        borrowItemCombo.addItem("-- Select Item --");
        returnUserCombo.addItem("-- Select Borrower --");
        returnItemCombo.addItem("-- Select Item --");

        // Populate users
        List<UserAccount> users = manager.getAllUsers();
        for (UserAccount user : users) {
            String name = user.getName();
            String id = user.getUserID();
            userIdMap.put(name, id);
            returnUserIdMap.put(name, id);
            borrowUserCombo.addItem(name);
            returnUserCombo.addItem(name);
        }

        // Populate items
        List<LibraryItem> items = manager.getAllItems();
        for (LibraryItem item : items) {
            String title = item.getTitle();
            String id = item.getItemID();
            itemIdMap.put(title, id);
            returnItemIdMap.put(title, id);
            borrowItemCombo.addItem(title);
            returnItemCombo.addItem(title);
        }
    }

    /**
     * Refreshes dropdowns after borrow/return operations.
     */
    private void refreshDropdowns() {
        // Reset to first option
        borrowUserCombo.setSelectedIndex(0);
        borrowItemCombo.setSelectedIndex(0);
        returnUserCombo.setSelectedIndex(0);
        returnItemCombo.setSelectedIndex(0);
        
        // Repopulate with latest data
        populateDropdowns();
    }

    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Input Error", JOptionPane.WARNING_MESSAGE);
    }

    private void addFormRow(JPanel panel, GridBagConstraints g,
                            int row, String label, JComboBox<String> combo, String tooltip) {
        g.gridx = 0; g.gridy = row; g.gridwidth = 1; g.weightx = 0;
        panel.add(new JLabel(label), g);
        g.gridx = 1; g.weightx = 1;
        combo.setToolTipText(tooltip);
        panel.add(combo, g);
    }

    private GridBagConstraints formGbc() {
        GridBagConstraints g = new GridBagConstraints();
        g.insets  = new Insets(6, 10, 6, 10);
        g.fill    = GridBagConstraints.HORIZONTAL;
        g.anchor  = GridBagConstraints.WEST;
        return g;
    }
}

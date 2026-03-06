package gui;

import controller.BorrowController.BorrowResult;
import controller.BorrowController.ReturnResult;
import controller.LibraryManager;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;

/**
 * Tab 1 — Borrow & Return.
 *
 * Layout: GridBagLayout
 * Two titled sections: Borrow and Return.
 * Each has User ID + Item ID text fields and a submit button.
 */
public class BorrowPanel extends JPanel {

    private final LibraryManager manager;
    private final MainWindow     mainWindow;

    // Borrow fields
    private final JTextField borrowUserField = new JTextField(15);
    private final JTextField borrowItemField = new JTextField(15);
    private final JLabel     borrowStatus   = new JLabel(" ");

    // Return fields
    private final JTextField returnUserField = new JTextField(15);
    private final JTextField returnItemField = new JTextField(15);
    private final JLabel     returnStatus   = new JLabel(" ");

    public BorrowPanel(LibraryManager manager, MainWindow mainWindow) {
        this.manager    = manager;
        this.mainWindow = mainWindow;
        setLayout(new GridBagLayout());
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

        addFormRow(panel, g, 0, "User ID:", borrowUserField, "Enter the borrower's user ID (e.g. USR-0001)");
        addFormRow(panel, g, 1, "Item ID:", borrowItemField, "Enter the item ID to borrow (e.g. ITEM-0001)");

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

        addFormRow(panel, g, 0, "User ID:", returnUserField, "Enter the user ID returning the item");
        addFormRow(panel, g, 1, "Item ID:", returnItemField, "Enter the item ID being returned");

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
        String uid = borrowUserField.getText().trim();
        String iid = borrowItemField.getText().trim();

        if (uid.isEmpty() || iid.isEmpty()) {
            showError("Please enter both User ID and Item ID.");
            return;
        }

        BorrowResult result = manager.getBorrowController().borrowItem(uid, iid);
        switch (result) {
            case SUCCESS:
                borrowStatus.setForeground(new Color(0, 130, 0));
                borrowStatus.setText("✔  Item borrowed successfully.");
                mainWindow.setStatus("Borrowed: " + iid + " → " + uid);
                mainWindow.refreshCatalogue();
                clearBorrowFields();
                break;
            case QUEUED:
                borrowStatus.setForeground(new Color(200, 120, 0));
                borrowStatus.setText("⏳  Item unavailable — added to waitlist.");
                mainWindow.setStatus("Queued: " + uid + " is waiting for " + iid);
                clearBorrowFields();
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
        String uid = returnUserField.getText().trim();
        String iid = returnItemField.getText().trim();

        if (uid.isEmpty() || iid.isEmpty()) {
            showError("Please enter both User ID and Item ID.");
            return;
        }

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
                clearReturnFields();
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

    private void clearBorrowFields() {
        borrowUserField.setText("");
        borrowItemField.setText("");
    }

    private void clearReturnFields() {
        returnUserField.setText("");
        returnItemField.setText("");
    }

    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Input Error", JOptionPane.WARNING_MESSAGE);
    }

    private void addFormRow(JPanel panel, GridBagConstraints g,
                            int row, String label, JTextField field, String tooltip) {
        g.gridx = 0; g.gridy = row; g.gridwidth = 1; g.weightx = 0;
        panel.add(new JLabel(label), g);
        g.gridx = 1; g.weightx = 1;
        field.setToolTipText(tooltip);
        panel.add(field, g);
    }

    private GridBagConstraints formGbc() {
        GridBagConstraints g = new GridBagConstraints();
        g.insets  = new Insets(6, 10, 6, 10);
        g.fill    = GridBagConstraints.HORIZONTAL;
        g.anchor  = GridBagConstraints.WEST;
        return g;
    }
}

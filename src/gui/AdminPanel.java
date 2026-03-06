package gui;

import controller.LibraryManager;
import model.*;
import utils.IDGenerator;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.util.List;

/**
 * Tab 2 — Admin Operations.
 *
 * Sections (BoxLayout vertical):
 *  1. Add Item form  — dynamic fields based on type selection
 *  2. Delete Item
 *  3. Undo last action
 *  4. Add User
 *  5. Reports
 *  6. Import / Export CSV
 */
public class AdminPanel extends JPanel {

    private final LibraryManager manager;
    private final MainWindow     mainWindow;

    // ── Add-item form fields ──
    private final JComboBox<String> typeCombo = new JComboBox<>(
            new String[]{"Book", "Magazine", "Journal"});
    private final JTextField titleField    = new JTextField(20);
    private final JTextField authorField   = new JTextField(20);
    private final JTextField yearField     = new JTextField(6);
    private final JTextField categoryField = new JTextField(14);

    // Book-specific
    private final JTextField isbnField    = new JTextField(14);
    private final JTextField editionField = new JTextField(10);
    private final JTextField genreField   = new JTextField(12);
    private final JPanel     bookExtra    = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));

    // Magazine-specific
    private final JTextField issueNumField = new JTextField(6);
    private final JTextField monthField    = new JTextField(10);
    private final JPanel     magExtra      = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));

    // Journal-specific
    private final JTextField volumeField    = new JTextField(6);
    private final JTextField issueDateField = new JTextField(12);
    private final JCheckBox  peerReviewedCb = new JCheckBox("Peer-reviewed");
    private final JPanel     journalExtra   = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));

    // Delete field
    private final JTextField deleteIdField = new JTextField(12);

    // User form fields
    private final JTextField userNameField  = new JTextField(14);
    private final JTextField userEmailField = new JTextField(18);
    private final JComboBox<String> userRoleCb = new JComboBox<>(new String[]{"STUDENT", "ADMIN"});

    public AdminPanel(LibraryManager manager, MainWindow mainWindow) {
        this.manager    = manager;
        this.mainWindow = mainWindow;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        buildExtraFields();
        buildUI();
        updateDynamicFields();
    }

    // ── Layout ────────────────────────────────────────────────────────────────

    private void buildUI() {
        add(buildAddItemSection());
        add(Box.createVerticalStrut(8));
        add(buildDeleteUndoSection());
        add(Box.createVerticalStrut(8));
        add(buildUserSection());
        add(Box.createVerticalStrut(8));
        add(buildReportsSection());
        add(Box.createVerticalStrut(8));
        add(buildImportExportSection());
        add(Box.createVerticalGlue());
    }

    private JPanel buildAddItemSection() {
        JPanel panel = titledPanel("Add Library Item");
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        // Row 1: type selector
        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        row1.add(new JLabel("Type:"));
        typeCombo.setToolTipText("Select item type — fields will change dynamically");
        typeCombo.addActionListener(e -> updateDynamicFields());
        row1.add(typeCombo);
        panel.add(row1);

        // Row 2: common fields
        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        row2.add(new JLabel("Title:"));      titleField.setToolTipText("Item title");    row2.add(titleField);
        row2.add(new JLabel("Author:"));     authorField.setToolTipText("Author/Publisher"); row2.add(authorField);
        row2.add(new JLabel("Year:"));       yearField.setToolTipText("Publication year (4 digits)"); row2.add(yearField);
        row2.add(new JLabel("Category:"));   categoryField.setToolTipText("Genre / subject area"); row2.add(categoryField);
        panel.add(row2);

        // Row 3: dynamic extra fields
        panel.add(bookExtra);
        panel.add(magExtra);
        panel.add(journalExtra);

        // Row 4: buttons
        JPanel row4 = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        JButton addBtn = new JButton("Add Item");
        addBtn.setBackground(new Color(30, 100, 180));
        addBtn.setForeground(Color.WHITE);
        addBtn.setToolTipText("Add this item to the library catalogue");
        addBtn.addActionListener(e -> doAddItem());

        JButton clearBtn = new JButton("Clear");
        clearBtn.setToolTipText("Clear the form");
        clearBtn.addActionListener(e -> clearAddForm());

        row4.add(addBtn);
        row4.add(clearBtn);
        panel.add(row4);

        return panel;
    }

    private void buildExtraFields() {
        // Book extra
        bookExtra.add(new JLabel("ISBN:"));       isbnField.setToolTipText("ISBN number");    bookExtra.add(isbnField);
        bookExtra.add(new JLabel("Edition:"));    editionField.setToolTipText("Edition");      bookExtra.add(editionField);
        bookExtra.add(new JLabel("Genre:"));      genreField.setToolTipText("Book genre");     bookExtra.add(genreField);

        // Magazine extra
        magExtra.add(new JLabel("Issue #:"));    issueNumField.setToolTipText("Issue number"); magExtra.add(issueNumField);
        magExtra.add(new JLabel("Month:"));       monthField.setToolTipText("Publication month"); magExtra.add(monthField);

        // Journal extra
        journalExtra.add(new JLabel("Volume:")); volumeField.setToolTipText("Volume number"); journalExtra.add(volumeField);
        journalExtra.add(new JLabel("Date:"));   issueDateField.setToolTipText("Issue date"); journalExtra.add(issueDateField);
        journalExtra.add(peerReviewedCb);        peerReviewedCb.setToolTipText("Tick if peer-reviewed");
    }

    private JPanel buildDeleteUndoSection() {
        JPanel panel = titledPanel("Delete / Undo");
        panel.setLayout(new FlowLayout(FlowLayout.LEFT, 8, 6));

        panel.add(new JLabel("Item ID to delete:"));
        deleteIdField.setToolTipText("Enter the exact item ID to remove");
        panel.add(deleteIdField);

        JButton deleteBtn = new JButton("Delete");
        deleteBtn.setForeground(Color.WHITE);
        deleteBtn.setBackground(new Color(190, 30, 30));
        deleteBtn.setToolTipText("Permanently remove item (can be undone)");
        deleteBtn.addActionListener(e -> doDelete());
        panel.add(deleteBtn);

        panel.add(Box.createHorizontalStrut(24));

        JButton undoBtn = new JButton("↩  Undo Last Action");
        undoBtn.setToolTipText("Reverse the last Add or Delete operation");
        undoBtn.addActionListener(e -> doUndo());
        panel.add(undoBtn);

        return panel;
    }

    private JPanel buildUserSection() {
        JPanel panel = titledPanel("Register New User");
        panel.setLayout(new FlowLayout(FlowLayout.LEFT, 8, 6));

        panel.add(new JLabel("Name:"));  userNameField.setToolTipText("Full name");  panel.add(userNameField);
        panel.add(new JLabel("Email:")); userEmailField.setToolTipText("Email address"); panel.add(userEmailField);
        panel.add(new JLabel("Role:"));  panel.add(userRoleCb);

        JButton addUserBtn = new JButton("Register");
        addUserBtn.setToolTipText("Create a new user account");
        addUserBtn.addActionListener(e -> doAddUser());
        panel.add(addUserBtn);

        return panel;
    }

    private JPanel buildReportsSection() {
        JPanel panel = titledPanel("Reports");
        panel.setLayout(new FlowLayout(FlowLayout.LEFT, 8, 6));

        JButton mostBorrowedBtn = new JButton("Most Borrowed");
        mostBorrowedBtn.setToolTipText("Items ranked by borrow frequency");
        mostBorrowedBtn.addActionListener(e ->
                showReport("Most Borrowed Items",
                        manager.getReportGenerator().getMostBorrowedReport()));

        JButton overdueBtn = new JButton("Overdue Users");
        overdueBtn.setToolTipText("Users who have overdue items");
        overdueBtn.addActionListener(e ->
                showReport("Users with Overdue Items",
                        manager.getReportGenerator().getOverdueUsersReport()));

        JButton categoryBtn = new JButton("Category Distribution");
        categoryBtn.setToolTipText("Item counts by type and subject category");
        categoryBtn.addActionListener(e ->
                showReport("Category Distribution",
                        manager.getReportGenerator().getCategoryReport()));

        panel.add(mostBorrowedBtn);
        panel.add(overdueBtn);
        panel.add(categoryBtn);
        return panel;
    }

    private JPanel buildImportExportSection() {
        JPanel panel = titledPanel("Import / Export");
        panel.setLayout(new FlowLayout(FlowLayout.LEFT, 8, 6));

        JButton importBtn = new JButton("Import CSV…");
        importBtn.setToolTipText("Bulk-add items from a CSV file");
        importBtn.addActionListener(e -> doImportCSV());

        JButton exportBtn = new JButton("Export CSV…");
        exportBtn.setToolTipText("Export the current catalogue to CSV");
        exportBtn.addActionListener(e -> doExportCSV());

        panel.add(importBtn);
        panel.add(exportBtn);
        return panel;
    }

    // ── Dynamic form fields ───────────────────────────────────────────────────

    /** Shows/hides the type-specific extra fields based on the type combo selection. */
    private void updateDynamicFields() {
        String type = (String) typeCombo.getSelectedItem();
        bookExtra.setVisible("Book".equals(type));
        magExtra.setVisible("Magazine".equals(type));
        journalExtra.setVisible("Journal".equals(type));
        revalidate();
        repaint();
    }

    // ── Actions ───────────────────────────────────────────────────────────────

    private void doAddItem() {
        String title    = titleField.getText().trim();
        String author   = authorField.getText().trim();
        String yearStr  = yearField.getText().trim();
        String category = categoryField.getText().trim();
        String type     = (String) typeCombo.getSelectedItem();

        // Input validation
        if (title.isEmpty() || author.isEmpty() || yearStr.isEmpty() || category.isEmpty()) {
            showError("Title, Author, Year, and Category are required.");
            return;
        }
        int year;
        try {
            year = Integer.parseInt(yearStr);
            if (year < 1000 || year > 9999) throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            showError("Year must be a valid 4-digit number.");
            return;
        }

        try {
            switch (type) {
                case "Book":
                    manager.addBook(title, author, year, category,
                            isbnField.getText().trim(),
                            editionField.getText().trim(),
                            genreField.getText().trim());
                    break;
                case "Magazine":
                    int issueNum = parseIntField(issueNumField.getText().trim(), "Issue #");
                    manager.addMagazine(title, author, year, category,
                            issueNum, monthField.getText().trim());
                    break;
                case "Journal":
                    int volume = parseIntField(volumeField.getText().trim(), "Volume");
                    manager.addJournal(title, author, year, category,
                            volume, issueDateField.getText().trim(),
                            peerReviewedCb.isSelected());
                    break;
            }
            mainWindow.setStatus(type + " added: \"" + title + "\"");
            mainWindow.refreshCatalogue();
            clearAddForm();
            JOptionPane.showMessageDialog(this,
                    type + " \"" + title + "\" added to the catalogue.",
                    "Item Added", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            showError(ex.getMessage());
        }
    }

    private void doDelete() {
        String id = deleteIdField.getText().trim();
        if (id.isEmpty()) { showError("Please enter an Item ID."); return; }

        int confirm = JOptionPane.showConfirmDialog(this,
                "Delete item " + id + "? This can be undone via Undo.",
                "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;

        try {
            manager.deleteItem(id);
            deleteIdField.setText("");
            mainWindow.setStatus("Deleted item: " + id);
            mainWindow.refreshCatalogue();
        } catch (Exception ex) {
            showError(ex.getMessage());
        }
    }

    private void doUndo() {
        String result = manager.undoLastAction();
        if (result == null) {
            JOptionPane.showMessageDialog(this, "Nothing to undo.", "Undo",
                    JOptionPane.INFORMATION_MESSAGE);
        } else {
            mainWindow.setStatus(result);
            mainWindow.refreshCatalogue();
            JOptionPane.showMessageDialog(this, result, "Undo Successful",
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void doAddUser() {
        String name  = userNameField.getText().trim();
        String email = userEmailField.getText().trim();
        String role  = (String) userRoleCb.getSelectedItem();

        if (name.isEmpty() || email.isEmpty()) {
            showError("Name and email are required.");
            return;
        }
        try {
            UserAccount user = manager.addUser(name, email, UserAccount.Role.valueOf(role));
            userNameField.setText("");
            userEmailField.setText("");
            mainWindow.setStatus("User registered: " + user.getUserID() + " — " + name);
            JOptionPane.showMessageDialog(this,
                    "User registered!\nID: " + user.getUserID() + "\nName: " + name,
                    "User Added", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            showError(ex.getMessage());
        }
    }

    // ── Reports ───────────────────────────────────────────────────────────────

    private void showReport(String title, List<String[]> data) {
        if (data.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No data available.", title,
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        String[] headers = data.get(0);
        Object[][] rows = new Object[data.size() - 1][];
        for (int i = 1; i < data.size(); i++) rows[i - 1] = data.get(i);

        DefaultTableModel model = new DefaultTableModel(rows, headers) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable reportTable = new JTable(model);
        reportTable.setFont(new Font("SansSerif", Font.PLAIN, 12));
        reportTable.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        reportTable.setRowHeight(22);

        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this),
                title, true);
        dialog.setLayout(new BorderLayout());
        dialog.add(new JScrollPane(reportTable), BorderLayout.CENTER);

        JButton closeBtn = new JButton("Close");
        closeBtn.addActionListener(e -> dialog.dispose());
        JPanel south = new JPanel();
        south.add(closeBtn);
        dialog.add(south, BorderLayout.SOUTH);

        dialog.setSize(620, 380);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    // ── CSV Import / Export (also called from MainWindow menu) ────────────────

    public void doImportCSV() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("CSV Files (*.csv)", "csv"));
        chooser.setDialogTitle("Import Items from CSV");
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;

        File file = chooser.getSelectedFile();
        try {
            int count = manager.getFileHandler()
                    .importCSV(file.getAbsolutePath(), manager.getDatabase());
            mainWindow.setStatus("Imported " + count + " item(s) from " + file.getName());
            mainWindow.refreshCatalogue();
            JOptionPane.showMessageDialog(this,
                    count + " item(s) imported successfully.", "Import Complete",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            showError("Import failed: " + ex.getMessage());
        }
    }

    public void doExportCSV() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("CSV Files (*.csv)", "csv"));
        chooser.setDialogTitle("Export Catalogue to CSV");
        chooser.setSelectedFile(new File("library_export.csv"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;

        File file = chooser.getSelectedFile();
        String path = file.getAbsolutePath();
        if (!path.endsWith(".csv")) path += ".csv";
        try {
            manager.getFileHandler().exportCSV(path, manager.getDatabase());
            mainWindow.setStatus("Exported catalogue to " + file.getName());
            JOptionPane.showMessageDialog(this,
                    "Catalogue exported to:\n" + path, "Export Complete",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            showError("Export failed: " + ex.getMessage());
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void clearAddForm() {
        titleField.setText(""); authorField.setText(""); yearField.setText("");
        categoryField.setText(""); isbnField.setText(""); editionField.setText("");
        genreField.setText(""); issueNumField.setText(""); monthField.setText("");
        volumeField.setText(""); issueDateField.setText("");
        peerReviewedCb.setSelected(false);
    }

    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }

    private int parseIntField(String value, String fieldName) {
        if (value.isEmpty()) return 0;
        try { return Integer.parseInt(value); }
        catch (NumberFormatException e) { throw new IllegalArgumentException(fieldName + " must be a number."); }
    }

    private JPanel titledPanel(String title) {
        JPanel p = new JPanel();
        p.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), title,
                TitledBorder.LEFT, TitledBorder.TOP,
                new Font("SansSerif", Font.BOLD, 12)));
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, p.getPreferredSize().height + 80));
        return p;
    }
}

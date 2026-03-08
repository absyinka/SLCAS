package gui;

import controller.LibraryManager;
import model.UserAccount;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.util.List;
import java.util.Objects;

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

        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        row1.add(new JLabel("Type:"));
        typeCombo.addActionListener(e -> updateDynamicFields());
        row1.add(typeCombo);
        panel.add(row1);

        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        row2.add(new JLabel("Title:"));      row2.add(titleField);
        row2.add(new JLabel("Author:"));     row2.add(authorField);
        row2.add(new JLabel("Year:"));       row2.add(yearField);
        row2.add(new JLabel("Category:"));   row2.add(categoryField);
        panel.add(row2);

        panel.add(bookExtra);
        panel.add(magExtra);
        panel.add(journalExtra);

        JPanel row4 = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        JButton addBtn = new JButton("Add Item");
        addBtn.setBackground(new Color(30, 100, 180));
        addBtn.setForeground(Color.WHITE);
        addBtn.addActionListener(e -> doAddItem());

        JButton clearBtn = new JButton("Clear");
        clearBtn.addActionListener(e -> clearAddForm());

        row4.add(addBtn);
        row4.add(clearBtn);
        panel.add(row4);

        return panel;
    }

    private void buildExtraFields() {
        bookExtra.add(new JLabel("ISBN:"));       bookExtra.add(isbnField);
        bookExtra.add(new JLabel("Edition:"));    bookExtra.add(editionField);
        bookExtra.add(new JLabel("Genre:"));      bookExtra.add(genreField);

        magExtra.add(new JLabel("Issue #:"));    magExtra.add(issueNumField);
        magExtra.add(new JLabel("Month:"));       magExtra.add(monthField);

        journalExtra.add(new JLabel("Volume:")); journalExtra.add(volumeField);
        journalExtra.add(new JLabel("Date:"));   journalExtra.add(issueDateField);
        journalExtra.add(peerReviewedCb);
    }

    private JPanel buildDeleteUndoSection() {
        JPanel panel = titledPanel("Delete / Undo");
        panel.setLayout(new FlowLayout(FlowLayout.LEFT, 8, 6));

        panel.add(new JLabel("Item ID to delete:"));
        panel.add(deleteIdField);

        JButton deleteBtn = new JButton("Delete");
        deleteBtn.setForeground(Color.WHITE);
        deleteBtn.setBackground(new Color(190, 30, 30));
        deleteBtn.addActionListener(e -> doDelete());
        panel.add(deleteBtn);

        panel.add(Box.createHorizontalStrut(24));

        JButton undoBtn = new JButton("↩  Undo Last Action");
        undoBtn.addActionListener(e -> doUndo());
        panel.add(undoBtn);

        return panel;
    }

    private JPanel buildUserSection() {
        JPanel panel = titledPanel("Register New User");
        panel.setLayout(new FlowLayout(FlowLayout.LEFT, 8, 6));

        panel.add(new JLabel("Name:"));  panel.add(userNameField);
        panel.add(new JLabel("Email:")); panel.add(userEmailField);
        panel.add(new JLabel("Role:"));  panel.add(userRoleCb);

        JButton addUserBtn = new JButton("Register");
        addUserBtn.addActionListener(e -> doAddUser());
        panel.add(addUserBtn);

        return panel;
    }

    private JPanel buildReportsSection() {
        JPanel panel = titledPanel("Reports");
        panel.setLayout(new FlowLayout(FlowLayout.LEFT, 8, 6));

        JButton mostBorrowedBtn = new JButton("Most Borrowed");
        mostBorrowedBtn.addActionListener(e ->
                showReport("Most Borrowed Items",
                        manager.getReportGenerator().getMostBorrowedReport()));

        JButton overdueBtn = new JButton("Overdue Users");
        overdueBtn.addActionListener(e ->
                showReport("Users with Overdue Items",
                        manager.getReportGenerator().getOverdueUsersReport()));

        JButton categoryBtn = new JButton("Category Distribution");
        categoryBtn.addActionListener(e ->
                showReport("Category Distribution",
                        manager.getReportGenerator().getCategoryReport()));

        // Updated: Added User Directory Button
        JButton userDirBtn = new JButton("View User Directory");
        userDirBtn.addActionListener(e -> showUserDirectory());

        panel.add(mostBorrowedBtn);
        panel.add(overdueBtn);
        panel.add(categoryBtn);
        panel.add(userDirBtn);
        return panel;
    }

    private JPanel buildImportExportSection() {
        JPanel panel = titledPanel("Import / Export");
        panel.setLayout(new FlowLayout(FlowLayout.LEFT, 8, 6));

        JButton importBtn = new JButton("Import CSV…");
        importBtn.addActionListener(e -> doImportCSV());

        JButton exportBtn = new JButton("Export CSV…");
        exportBtn.addActionListener(e -> doExportCSV());

        panel.add(importBtn);
        panel.add(exportBtn);
        return panel;
    }

    private void updateDynamicFields() {
        String type = (String) typeCombo.getSelectedItem();
        bookExtra.setVisible("Book".equals(type));
        magExtra.setVisible("Magazine".equals(type));
        journalExtra.setVisible("Journal".equals(type));
        revalidate();
        repaint();
    }

    private void doAddItem() {
        String title    = titleField.getText().trim();
        String author   = authorField.getText().trim();
        String yearStr  = yearField.getText().trim();
        String category = categoryField.getText().trim();
        String type     = (String) typeCombo.getSelectedItem();

        if (title.isEmpty() || author.isEmpty() || yearStr.isEmpty() || category.isEmpty()) {
            showError("Title, Author, Year, and Category are required.");
            return;
        }
        int year;
        try {
            year = Integer.parseInt(yearStr);
        } catch (NumberFormatException ex) {
            showError("Year must be a valid number.");
            return;
        }

        try {
            switch (Objects.requireNonNull(type)) {
                case "Book" -> manager.addBook(title, author, year, category,
                        isbnField.getText().trim(), editionField.getText().trim(), genreField.getText().trim());
                case "Magazine" -> manager.addMagazine(title, author, year, category,
                        parseIntField(issueNumField.getText().trim(), "Issue #"), monthField.getText().trim());
                case "Journal" -> manager.addJournal(title, author, year, category,
                        parseIntField(volumeField.getText().trim(), "Volume"), issueDateField.getText().trim(), peerReviewedCb.isSelected());
            }
            mainWindow.refreshCatalogue();
            clearAddForm();
            JOptionPane.showMessageDialog(this, type + " added successfully.");
        } catch (Exception ex) { showError(ex.getMessage()); }
    }

    private void doDelete() {
        String id = deleteIdField.getText().trim();
        if (id.isEmpty()) { showError("Please enter an Item ID."); return; }
        try {
            manager.deleteItem(id);
            mainWindow.refreshCatalogue();
            deleteIdField.setText("");
        } catch (Exception ex) { showError(ex.getMessage()); }
    }

    private void doUndo() {
        String result = manager.undoLastAction();
        if (result != null) {
            mainWindow.refreshCatalogue();
            JOptionPane.showMessageDialog(this, result);
        } else {
            JOptionPane.showMessageDialog(this, "Nothing to undo.");
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
            JOptionPane.showMessageDialog(this, "User registered: " + user.getUserID());
        } catch (Exception ex) { showError(ex.getMessage()); }
    }

    private void showReport(String title, List<String[]> data) {
        if (data.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No data available.", title, JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        String[] headers = data.get(0);
        Object[][] rows = new Object[data.size() - 1][];
        for (int i = 1; i < data.size(); i++) rows[i - 1] = data.get(i);

        DefaultTableModel model = new DefaultTableModel(rows, headers) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable reportTable = new JTable(model);
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), title, true);
        dialog.add(new JScrollPane(reportTable));
        dialog.setSize(600, 400);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    // New Helper: showUserDirectory
    private void showUserDirectory() {
        String directory = manager.getReportGenerator().generateUserDirectory(manager.getDatabase());
        JTextArea textArea = new JTextArea(directory);
        textArea.setEditable(false);
        textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(500, 400));
        JOptionPane.showMessageDialog(this, scrollPane, "User Directory", JOptionPane.PLAIN_MESSAGE);
    }

    public void doImportCSV() {
        JFileChooser chooser = new JFileChooser();
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                manager.getFileHandler().importCSV(chooser.getSelectedFile().getAbsolutePath(), manager.getDatabase());
                mainWindow.refreshCatalogue();
            } catch (Exception ex) { showError(ex.getMessage()); }
        }
    }

    public void doExportCSV() {
        JFileChooser chooser = new JFileChooser();
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                manager.getFileHandler().exportCSV(chooser.getSelectedFile().getAbsolutePath(), manager.getDatabase());
            } catch (Exception ex) { showError(ex.getMessage()); }
        }
    }

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
        try { return Integer.parseInt(value); }
        catch (Exception e) { throw new IllegalArgumentException(fieldName + " must be a number."); }
    }

    private JPanel titledPanel(String title) {
        JPanel p = new JPanel();
        p.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), title, TitledBorder.LEFT, TitledBorder.TOP, new Font("SansSerif", Font.BOLD, 12)));
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, p.getPreferredSize().height + 50));
        return p;
    }
}
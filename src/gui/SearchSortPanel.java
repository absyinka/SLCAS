package gui;

import controller.LibraryManager;
import controller.SearchEngine.SearchField;
import model.LibraryItem;
import utils.SortingUtils.SortAlgorithm;
import utils.SortingUtils.SortField;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * Tab 3 — Search & Sort.
 *
 * NORTH  : search bar (query field + field selector + button)
 * CENTER : results JTable
 * SOUTH  : sort controls (field selector + algorithm selector + button)
 */
public class SearchSortPanel extends JPanel {

    private static final String[] COLUMNS =
            {"Item ID", "Type", "Title", "Author", "Year", "Category", "Available"};

    private final LibraryManager manager;
    private final MainWindow     mainWindow;

    // Search controls
    private final JTextField        queryField    = new JTextField(22);
    private final JComboBox<String> searchFieldCb = new JComboBox<>(
            new String[]{"Title", "Author", "Type"});

    // Sort controls
    private final JComboBox<String> sortFieldCb = new JComboBox<>(
            new String[]{"Title", "Author", "Year"});
    private final JComboBox<String> sortAlgoCb  = new JComboBox<>(
            new String[]{"Merge Sort", "Quick Sort", "Insertion Sort", "Selection Sort"});

    // Results table
    private final DefaultTableModel tableModel = new DefaultTableModel(COLUMNS, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable  table      = new JTable(tableModel);
    private final JLabel  resultLabel = new JLabel("Results: —");

    public SearchSortPanel(LibraryManager manager, MainWindow mainWindow) {
        this.manager    = manager;
        this.mainWindow = mainWindow;
        setLayout(new BorderLayout(0, 4));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        buildUI();
    }

    // ── Layout ────────────────────────────────────────────────────────────────

    private void buildUI() {
        add(buildNorth(), BorderLayout.NORTH);
        add(buildCenter(), BorderLayout.CENTER);
        add(buildSouth(), BorderLayout.SOUTH);
    }

    private JPanel buildNorth() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        panel.setBorder(BorderFactory.createTitledBorder("Search Catalogue"));

        queryField.setToolTipText("Enter search term");
        panel.add(new JLabel("Query:"));
        panel.add(queryField);
        panel.add(new JLabel("Field:"));
        panel.add(searchFieldCb);

        JButton searchBtn = new JButton("Search");
        searchBtn.setToolTipText("Search catalogue using the selected field");
        searchBtn.addActionListener(e -> doSearch());
        panel.add(searchBtn);

        JButton clearBtn = new JButton("Clear");
        clearBtn.setToolTipText("Show full catalogue");
        clearBtn.addActionListener(e -> {
            queryField.setText("");
            loadAll();
        });
        panel.add(clearBtn);

        panel.add(Box.createHorizontalStrut(16));
        resultLabel.setForeground(Color.GRAY);
        panel.add(resultLabel);

        // Allow pressing Enter in the query field to trigger search
        queryField.addActionListener(e -> doSearch());

        return panel;
    }

    private JScrollPane buildCenter() {
        table.setRowHeight(24);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setFont(new Font("SansSerif", Font.PLAIN, 12));
        table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));

        int[] widths = {90, 80, 260, 160, 55, 110, 75};
        for (int i = 0; i < widths.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
        return new JScrollPane(table);
    }

    private JPanel buildSouth() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        panel.setBorder(BorderFactory.createTitledBorder("Sort Catalogue"));

        panel.add(new JLabel("Sort by:"));
        panel.add(sortFieldCb);
        panel.add(new JLabel("Algorithm:"));
        panel.add(sortAlgoCb);

        JButton sortBtn = new JButton("Sort");
        sortBtn.setToolTipText("Sort the full catalogue and display result");
        sortBtn.addActionListener(e -> doSort());
        panel.add(sortBtn);

        return panel;
    }

    // ── Actions ───────────────────────────────────────────────────────────────

    private void doSearch() {
        String query = queryField.getText().trim();
        SearchField field = toSearchField((String) searchFieldCb.getSelectedItem());

        List<LibraryItem> results = manager.getSearchEngine().search(query, field);
        populateTable(results);
        resultLabel.setText("Results: " + results.size());
        mainWindow.setStatus("Search complete — " + results.size() + " item(s) found.");
    }

    private void doSort() {
        SortField     field = toSortField((String) sortFieldCb.getSelectedItem());
        SortAlgorithm algo  = toAlgorithm((String) sortAlgoCb.getSelectedItem());

        List<LibraryItem> sorted = manager.getSortingUtils()
                .sort(manager.getAllItems(), field, algo);

        // Tell SearchEngine the catalogue is now sorted by this field
        manager.getSearchEngine().setCurrentSortField(field);

        populateTable(sorted);
        resultLabel.setText("Results: " + sorted.size());
        mainWindow.setStatus("Sorted by " + field + " using " + algo + ".");
    }

    private void loadAll() {
        List<LibraryItem> all = manager.getAllItems();
        populateTable(all);
        resultLabel.setText("Results: " + all.size());
    }

    // ── Table population ──────────────────────────────────────────────────────

    private void populateTable(List<LibraryItem> items) {
        tableModel.setRowCount(0);
        for (LibraryItem item : items) {
            tableModel.addRow(new Object[]{
                    item.getItemID(),
                    item.getType(),
                    item.getTitle(),
                    item.getAuthor(),
                    item.getYear(),
                    item.getCategory(),
                    item.isAvailable() ? "Yes" : "No"
            });
        }
    }

    // ── Enum mappers ──────────────────────────────────────────────────────────

    private SearchField toSearchField(String s) {
        switch (s) {
            case "Author": return SearchField.AUTHOR;
            case "Type":   return SearchField.TYPE;
            default:       return SearchField.TITLE;
        }
    }

    private SortField toSortField(String s) {
        switch (s) {
            case "Author": return SortField.AUTHOR;
            case "Year":   return SortField.YEAR;
            default:       return SortField.TITLE;
        }
    }

    private SortAlgorithm toAlgorithm(String s) {
        switch (s) {
            case "Quick Sort":     return SortAlgorithm.QUICK;
            case "Insertion Sort": return SortAlgorithm.INSERTION;
            case "Selection Sort": return SortAlgorithm.SELECTION;
            default:               return SortAlgorithm.MERGE;
        }
    }
}

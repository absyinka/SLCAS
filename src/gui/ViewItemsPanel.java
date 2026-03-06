package gui;

import controller.LibraryManager;
import model.LibraryItem;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;

/**
 * Tab 0 — Browse the library catalogue.
 *
 * Features:
 *  - JTable with custom renderer (unavailable rows tinted red)
 *  - Type filter combo box
 *  - Refresh button
 *  - Double-click row → item detail dialog
 *  - Item count label
 */
public class ViewItemsPanel extends JPanel {

    private static final String[] COLUMNS =
            {"Item ID", "Type", "Title", "Author", "Year", "Category", "Available"};

    private static final String[] TYPE_FILTERS =
            {"All Types", "Book", "Magazine", "Journal"};

    private final LibraryManager manager;
    private final MainWindow     mainWindow;

    private final DefaultTableModel tableModel = new DefaultTableModel(COLUMNS, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable  table  = new JTable(tableModel);
    private final JLabel  countLabel = new JLabel("Items: 0");
    private final JComboBox<String> typeFilter = new JComboBox<>(TYPE_FILTERS);

    public ViewItemsPanel(LibraryManager manager, MainWindow mainWindow) {
        this.manager    = manager;
        this.mainWindow = mainWindow;
        setLayout(new BorderLayout(0, 0));
        buildUI();
        refresh();
    }

    // ── Layout ────────────────────────────────────────────────────────────────

    private void buildUI() {
        // ── NORTH: filter bar ──
        JPanel north = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        north.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY));
        north.add(new JLabel("Filter by type:"));
        north.add(typeFilter);

        JButton refreshBtn = new JButton("⟳ Refresh");
        refreshBtn.setToolTipText("Reload the catalogue from memory");
        refreshBtn.addActionListener(e -> refresh());
        north.add(refreshBtn);

        north.add(Box.createHorizontalStrut(20));
        countLabel.setForeground(Color.GRAY);
        north.add(countLabel);
        add(north, BorderLayout.NORTH);

        // ── CENTER: table ──
        table.setRowHeight(24);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
        table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        table.setFont(new Font("SansSerif", Font.PLAIN, 12));

        // Column widths
        int[] widths = {90, 80, 260, 160, 55, 110, 75};
        for (int i = 0; i < widths.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }

        // Custom renderer — highlight unavailable rows
        TableCellRenderer renderer = new AvailabilityRenderer();
        for (int i = 0; i < COLUMNS.length; i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(renderer);
        }

        // Double-click → detail dialog
        table.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) showDetail();
            }
        });

        add(new JScrollPane(table), BorderLayout.CENTER);

        // ── Filter listener ──
        typeFilter.addActionListener(e -> refresh());
    }

    // ── Data loading ──────────────────────────────────────────────────────────

    public void refresh() {
        String filter = (String) typeFilter.getSelectedItem();
        List<LibraryItem> items = manager.getAllItems();
        tableModel.setRowCount(0);

        int shown = 0;
        for (LibraryItem item : items) {
            if (!"All Types".equals(filter) && !item.getType().equals(filter)) continue;
            tableModel.addRow(new Object[]{
                    item.getItemID(),
                    item.getType(),
                    item.getTitle(),
                    item.getAuthor(),
                    item.getYear(),
                    item.getCategory(),
                    item.isAvailable() ? "Yes" : "No"
            });
            shown++;
        }
        countLabel.setText("Items: " + shown + (shown != items.size()
                ? " (filtered from " + items.size() + ")" : ""));
    }

    // ── Detail dialog ─────────────────────────────────────────────────────────

    private void showDetail() {
        int row = table.getSelectedRow();
        if (row < 0) return;
        String id = (String) tableModel.getValueAt(row, 0);
        try {
            LibraryItem item = manager.getItemByID(id);
            JOptionPane.showMessageDialog(this,
                    item.getSummary() + "\n\nStatus: " + (item.isAvailable() ? "Available" : "Currently borrowed"),
                    "Item Details", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            mainWindow.setStatus("Error: " + ex.getMessage());
        }
    }

    // ── Custom renderer ───────────────────────────────────────────────────────

    /** Colours the row pink when the item is unavailable (borrowed). */
    private static class AvailabilityRenderer extends DefaultTableCellRenderer {
        private static final Color UNAVAILABLE_BG = new Color(255, 220, 220);
        private static final Color UNAVAILABLE_FG = new Color(180, 0, 0);

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int col) {
            Component c = super.getTableCellRendererComponent(
                    table, value, isSelected, hasFocus, row, col);
            String avail = (String) table.getModel().getValueAt(row, 6);
            if (!isSelected) {
                if ("No".equals(avail)) {
                    c.setBackground(UNAVAILABLE_BG);
                    c.setForeground(UNAVAILABLE_FG);
                } else {
                    c.setBackground(Color.WHITE);
                    c.setForeground(Color.BLACK);
                }
            }
            return c;
        }
    }
}

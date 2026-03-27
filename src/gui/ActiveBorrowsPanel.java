package gui;

import controller.LibraryManager;
import model.BorrowRecord;
import model.LibraryItem;
import model.UserAccount;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.time.LocalDate;
import java.util.List;

/**
 * Tab — Active Borrows.
 *
 * Shows every item currently on loan: who holds it, borrow date,
 * due date, and whether it is overdue.
 * Overdue rows are highlighted red; on-time rows are white.
 */
public class ActiveBorrowsPanel extends JPanel {

    private static final String[] COLUMNS =
            {"Borrower", "User ID", "Item Title", "Item ID", "Type", "Borrow Date", "Due Date", "Status"};

    private final LibraryManager manager;

    private final DefaultTableModel tableModel = new DefaultTableModel(COLUMNS, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable table     = new JTable(tableModel);
    private final JLabel countLabel = new JLabel("Active borrows: 0");

    public ActiveBorrowsPanel(LibraryManager manager) {
        this.manager = manager;
        setLayout(new BorderLayout(0, 0));
        buildUI();
        refresh();
    }

    // ── Layout ────────────────────────────────────────────────────────────────

    private void buildUI() {
        // ── NORTH: toolbar ──
        JPanel north = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        north.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY));

        JButton refreshBtn = new JButton("⟳ Refresh");
        refreshBtn.setToolTipText("Reload active borrows");
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

        int[] widths = {130, 80, 240, 90, 80, 100, 100, 90};
        for (int i = 0; i < widths.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }

        TableCellRenderer renderer = new StatusRenderer();
        for (int i = 0; i < COLUMNS.length; i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(renderer);
        }

        add(new JScrollPane(table), BorderLayout.CENTER);
    }

    // ── Data loading ──────────────────────────────────────────────────────────

    public void refresh() {
        tableModel.setRowCount(0);
        int count = 0;
        LocalDate today = LocalDate.now();

        List<UserAccount> users = manager.getAllUsers();
        for (UserAccount user : users) {
            List<LibraryItem> held = user.getBorrowedItems();
            for (LibraryItem item : held) {
                BorrowRecord rec = user.getOpenRecord(item.getItemID());
                String borrowDate = rec != null ? rec.getBorrowDate().toString() : "—";
                String dueDate    = rec != null ? rec.getDueDate().toString()    : "—";
                String status;
                if (rec != null && today.isAfter(rec.getDueDate())) {
                    long days = rec.daysOverdue();
                    status = "Overdue " + days + "d";
                } else {
                    status = "On time";
                }
                tableModel.addRow(new Object[]{
                        user.getName(),
                        user.getUserID(),
                        item.getTitle(),
                        item.getItemID(),
                        item.getType(),
                        borrowDate,
                        dueDate,
                        status
                });
                count++;
            }
        }
        countLabel.setText("Active borrows: " + count);
    }

    // ── Custom renderer ───────────────────────────────────────────────────────

    /** Highlights overdue rows in red. */
    private static class StatusRenderer extends DefaultTableCellRenderer {
        private static final Color OVERDUE_BG = new Color(255, 220, 220);
        private static final Color OVERDUE_FG = new Color(180, 0, 0);

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int col) {
            Component c = super.getTableCellRendererComponent(
                    table, value, isSelected, hasFocus, row, col);
            String status = (String) table.getModel().getValueAt(row, 7);
            if (!isSelected) {
                if (status != null && status.startsWith("Overdue")) {
                    c.setBackground(OVERDUE_BG);
                    c.setForeground(OVERDUE_FG);
                } else {
                    c.setBackground(Color.WHITE);
                    c.setForeground(Color.BLACK);
                }
            }
            return c;
        }
    }
}

package gui;

import controller.LibraryManager;
import model.UserAccount;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;

/**
 * Root application window.
 *
 * Layout  : BorderLayout
 * Centre  : JTabbedPane (4 tabs)
 * South   : status bar
 * Menu    : File (Save | Import | Export | Exit), Help (About)
 * Timer   : fires every 60 s to poll for overdue items
 */
public class MainWindow extends JFrame {

    private final LibraryManager manager;

    private final JLabel statusBar = new JLabel(" Ready");

    private ViewItemsPanel   viewItemsPanel;
    private BorrowPanel      borrowPanel;
    private AdminPanel       adminPanel;
    private SearchSortPanel  searchSortPanel;

    public MainWindow(LibraryManager manager) {
        this.manager = manager;
        buildUI();
        startOverdueTimer();
    }

    // ── UI Construction ───────────────────────────────────────────────────────

    private void buildUI() {
        setTitle("SLCAS — Smart Library Circulation & Automation System");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setSize(1050, 680);
        setMinimumSize(new Dimension(800, 520));
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // ── Menu bar ──
        setJMenuBar(buildMenuBar());

        // ── Tabs ──
        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("SansSerif", Font.PLAIN, 13));

        viewItemsPanel  = new ViewItemsPanel(manager, this);
        borrowPanel     = new BorrowPanel(manager, this);
        adminPanel      = new AdminPanel(manager, this);
        searchSortPanel = new SearchSortPanel(manager, this);

        tabs.addTab("📚  View Items",    viewItemsPanel);
        tabs.addTab("🔄  Borrow / Return", borrowPanel);
        tabs.addTab("🛠  Admin",          adminPanel);
        tabs.addTab("🔍  Search & Sort",  searchSortPanel);

        // Refresh ViewItems whenever the user switches to that tab
        tabs.addChangeListener(e -> {
            if (tabs.getSelectedComponent() == viewItemsPanel) {
                viewItemsPanel.refresh();
            }
        });

        add(tabs, BorderLayout.CENTER);

        // ── Status bar ──
        statusBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY),
                BorderFactory.createEmptyBorder(3, 8, 3, 8)));
        statusBar.setFont(new Font("SansSerif", Font.PLAIN, 12));
        add(statusBar, BorderLayout.SOUTH);

        // ── Close hook ──
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { onExit(); }
        });
    }

    private JMenuBar buildMenuBar() {
        JMenuBar bar = new JMenuBar();

        // File menu
        JMenu fileMenu = new JMenu("File");
        fileMenu.setMnemonic(KeyEvent.VK_F);

        JMenuItem saveItem = new JMenuItem("Save", KeyEvent.VK_S);
        saveItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK));
        saveItem.addActionListener(e -> {
            manager.saveAll();
            setStatus("Data saved successfully.");
        });

        JMenuItem importItem = new JMenuItem("Import CSV…", KeyEvent.VK_I);
        importItem.addActionListener(e -> adminPanel.doImportCSV());

        JMenuItem exportItem = new JMenuItem("Export CSV…", KeyEvent.VK_E);
        exportItem.addActionListener(e -> adminPanel.doExportCSV());

        JMenuItem exitItem = new JMenuItem("Exit", KeyEvent.VK_X);
        exitItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F4, InputEvent.ALT_DOWN_MASK));
        exitItem.addActionListener(e -> onExit());

        fileMenu.add(saveItem);
        fileMenu.addSeparator();
        fileMenu.add(importItem);
        fileMenu.add(exportItem);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);

        // Help menu
        JMenu helpMenu = new JMenu("Help");
        helpMenu.setMnemonic(KeyEvent.VK_H);
        JMenuItem aboutItem = new JMenuItem("About", KeyEvent.VK_A);
        aboutItem.addActionListener(e -> JOptionPane.showMessageDialog(this,
                "Smart Library Circulation & Automation System\n" +
                "COS 202 — MIVA Open University, 2025\n\n" +
                "Backend: model / controller / utils\n" +
                "GUI: Swing (Java)",
                "About SLCAS", JOptionPane.INFORMATION_MESSAGE));
        helpMenu.add(aboutItem);

        bar.add(fileMenu);
        bar.add(helpMenu);
        return bar;
    }

    // ── Overdue Timer ─────────────────────────────────────────────────────────

    private void startOverdueTimer() {
        // Fire every 60 seconds (60_000 ms)
        Timer timer = new Timer(60_000, e -> checkOverdue());
        timer.setInitialDelay(5_000); // first check 5 s after launch
        timer.start();
    }

    private void checkOverdue() {
        List<UserAccount> overdue = manager.getOverdueUsers();
        if (overdue.isEmpty()) {
            setStatus("No overdue items.");
        } else {
            setStatus("⚠  " + overdue.size() + " user(s) have overdue items.");
        }
    }

    // ── Shared helpers (panels call these) ───────────────────────────────────

    public void setStatus(String message) {
        statusBar.setText("  " + message);
    }

    public void refreshCatalogue() {
        viewItemsPanel.refresh();
    }

    // ── Exit ──────────────────────────────────────────────────────────────────

    private void onExit() {
        int choice = JOptionPane.showConfirmDialog(this,
                "Save data before exiting?", "Exit SLCAS",
                JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (choice == JOptionPane.YES_OPTION) {
            manager.saveAll();
            dispose();
        } else if (choice == JOptionPane.NO_OPTION) {
            dispose();
        }
        // CANCEL → do nothing (stay open)
    }
}

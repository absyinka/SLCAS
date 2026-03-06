import controller.LibraryManager;
import gui.MainWindow;

import javax.swing.SwingUtilities;

/**
 * Application entry point.
 * Boots the backend then hands off to the Swing event dispatch thread.
 */
public class Main {
    public static void main(String[] args) {
        LibraryManager manager = new LibraryManager();
        manager.initialize();                      // load persisted data

        SwingUtilities.invokeLater(() -> {
            MainWindow window = new MainWindow(manager);
            window.setVisible(true);
        });
    }
}

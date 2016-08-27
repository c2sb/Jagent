package rebound.jagent.ui.gui.common;

import javax.swing.*;

/**
 * Utility functions common to GUI components.
 */
public class GUI {

    /**
     * Attempts to set up the look and feel for the Java UI to match the operating system (including display scaling).
     * If this fails, a stack trace is printed and nothing happens.
     */
    public static void setupLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}

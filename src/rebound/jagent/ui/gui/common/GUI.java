package rebound.jagent.ui.gui.common;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

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

    /**
     * Constructs a JMenuItem with the given name, hotkey, and callback.
     *
     * @param name Text string that will be displayed as the menu item.
     * @param keyCode The hotkey for the menu item.
     * @param modifiers The hotkey modifier that will be applied to the menu item, in addition to the default toolkit
     *                  menu shortcut key.
     * @param actionListener The callback that will be executed when the menu item is activated.
     * @return JMenuItem with name, accelerator, and action listener applied.
     */
    public static JMenuItem createMenuItem(final String name, final int keyCode, final int modifiers, final ActionListener actionListener) {
        final JMenuItem menuItem = new JMenuItem(name);
        menuItem.setAccelerator(KeyStroke.getKeyStroke(keyCode, modifiers | Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        menuItem.addActionListener(actionListener);
        return menuItem;
    }

    /**
     * @see #createMenuItem(String, int, int, ActionListener)
     */
    public static JMenuItem createMenuItem(final String name, final int keyCode, final ActionListener actionListener) {
        return createMenuItem(name, keyCode, 0, actionListener);
    }

    /**
     * @see #createMenuItem(String, int, int, ActionListener)
     */
    public static JMenuItem createMenuItem(final String name, final ActionListener actionListener) {
        final JMenuItem menuItem = new JMenuItem(name);
        menuItem.addActionListener(actionListener);
        return menuItem;
    }

}

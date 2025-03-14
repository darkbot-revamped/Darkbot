package com.github.manolo8.darkbot.gui.utils.table;

import com.github.manolo8.darkbot.core.manager.HeroManager;
import com.github.manolo8.darkbot.gui.utils.PopupMenuListenerAdapter;
import eu.darkbot.api.game.items.SelectableItem;

import javax.swing.*;
import javax.swing.event.PopupMenuEvent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class LaserNpcInfoEditor extends TableDelegateEditor<JLabel> {

    private static final ToolTipManager TOOLTIPS = ToolTipManager.sharedInstance();
    private final JPopupMenu popupMenu = new JPopupMenu();
    private SelectableItem.Laser selectedLaser;
    private int tooltipDelay = -1;

    public LaserNpcInfoEditor() {
        super(new JLabel());

        popupMenu.addPopupMenuListener(new PopupMenuListenerAdapter() {
            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                if (tooltipDelay != -1) {
                    TOOLTIPS.setInitialDelay(tooltipDelay); // Restore original delay
                }
            }
        });
    }

    @Override
    protected void setValue(Object val) {
        if (val instanceof SelectableItem.Laser) {
            selectedLaser = (SelectableItem.Laser) val;
            delegate.setText(selectedLaser.toString());
        } else {
            selectedLaser = null;
            delegate.setText("None");
        }
    }

    @Override
    protected Object getValue() {
        return selectedLaser;
    }

    public void startEditing(JTable table, Object value, boolean isSelected, int row, int column) {
        SwingUtilities.invokeLater(() -> {
            if (!table.isShowing()) return;

            // Rebuild the menu every time it's opened
            rebuildPopupMenu();

            Rectangle cellRect = table.getCellRect(row, column, false);
            int x = cellRect.x;
            int y = cellRect.y + cellRect.height;

            popupMenu.show(table, x, y);
        });
    }

    private void rebuildPopupMenu() {
        popupMenu.removeAll(); // Clear old items

        // "None" option to clear selection
        JMenuItem noneItem = new JMenuItem("None");
        noneItem.addActionListener(e -> {
            selectedLaser = null;
            delegate.setText("None");
            stopCellEditing();
        });
        popupMenu.add(noneItem);
        popupMenu.addSeparator();

        // Dynamically add all available lasers
        for (SelectableItem.Laser laser : HeroManager.instance.main.config.BOT_SETTINGS.BOT_GUI.LASER) {
            JMenuItem menuItem = new JMenuItem(laser.toString());
            menuItem.setToolTipText("Select laser: " + laser);
            menuItem.addActionListener(new AmmoSelectionHandler(laser));
            popupMenu.add(menuItem);
        }
    }


    private class AmmoSelectionHandler implements ActionListener {
        private final SelectableItem.Laser laser;

        public AmmoSelectionHandler(SelectableItem.Laser laser) {
            this.laser = laser;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            selectedLaser = laser;
            delegate.setText(laser.toString());
            stopCellEditing();
        }
    }
}


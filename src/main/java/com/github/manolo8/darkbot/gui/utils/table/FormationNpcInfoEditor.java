package com.github.manolo8.darkbot.gui.utils.table;

import com.github.manolo8.darkbot.gui.utils.PopupMenuListenerAdapter;
import eu.darkbot.api.game.items.SelectableItem;

import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.event.PopupMenuEvent;

import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class FormationNpcInfoEditor extends TableDelegateEditor<JLabel> {

    private static final ToolTipManager TOOLTIPS = ToolTipManager.sharedInstance();
    private final JPopupMenu popupMenu = new JPopupMenu();
    private SelectableItem.Formation selectedFormation;
    private int tooltipDelay = -1;

    public FormationNpcInfoEditor() {
        super(new JLabel());

        popupMenu.addPopupMenuListener(new PopupMenuListenerAdapter() {
            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                if (tooltipDelay != -1) {
                    TOOLTIPS.setInitialDelay(tooltipDelay); // Restore original delay
                }
            }
        });

        // "None" option to clear selection
        JMenuItem noneItem = new JMenuItem("None");
        noneItem.addActionListener(e -> {
            selectedFormation = null;
            delegate.setText("None");
            stopCellEditing();
        });
        popupMenu.add(noneItem);
        popupMenu.addSeparator();

        // Add all formations as selectable menu items
        for (SelectableItem.Formation formation : SelectableItem.Formation.values()) {
            JMenuItem menuItem = new JMenuItem(formation.toString());
            menuItem.setToolTipText("Select formation: " + formation); // Set tooltip
            menuItem.addActionListener(new FormationSelectionHandler(formation));
            popupMenu.add(menuItem);
        }
    }

    @Override
    protected void setValue(Object val) {
        if (val instanceof SelectableItem.Formation) {
            selectedFormation = (SelectableItem.Formation) val;
            delegate.setText(selectedFormation.toString());
        } else {
            selectedFormation = null;
            delegate.setText("None");
        }
    }

    @Override
    protected Object getValue() {
        return selectedFormation;
    }

    public void startEditing(JTable table, Object value, boolean isSelected, int row, int column) {
        SwingUtilities.invokeLater(() -> {
            if (!table.isShowing()) return;

            Rectangle cellRect = table.getCellRect(row, column, false);
            int x = cellRect.x;
            int y = cellRect.y + cellRect.height;

            popupMenu.show(table, x, y);
        });
    }

    private class FormationSelectionHandler implements ActionListener {
        private final SelectableItem.Formation formation;

        public FormationSelectionHandler(SelectableItem.Formation formation) {
            this.formation = formation;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            selectedFormation = formation;
            delegate.setText(formation.toString());
            stopCellEditing();
        }
    }
}


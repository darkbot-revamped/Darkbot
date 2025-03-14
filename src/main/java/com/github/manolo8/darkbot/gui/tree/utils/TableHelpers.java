package com.github.manolo8.darkbot.gui.tree.utils;

import com.github.manolo8.darkbot.config.BoxInfo;
import com.github.manolo8.darkbot.config.NpcInfo;
import com.github.manolo8.darkbot.gui.utils.MultiTableRowSorter;
import com.github.manolo8.darkbot.gui.utils.table.ExtraNpcInfoEditor;
import com.github.manolo8.darkbot.gui.utils.table.FormationNpcInfoEditor;
import eu.darkbot.api.config.ConfigSetting;
import eu.darkbot.api.config.annotations.Table;
import eu.darkbot.api.game.items.SelectableItem;
import eu.darkbot.api.managers.ConfigAPI;
import eu.darkbot.api.managers.StarSystemAPI;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

public class TableHelpers {

    public static class MapPickerBuilder implements Table.ControlBuilder<NpcInfo> {
        private final StarSystemAPI starSystemAPI;

        public MapPickerBuilder(StarSystemAPI starSystemAPI) {
            this.starSystemAPI = starSystemAPI;
        }

        @Override
        public JComponent create(JTable table, ConfigSetting<Map<String, NpcInfo>> setting) {
            MultiTableRowSorter<NpcTableModel> sorter = setting.getMetadata("table.rowSorter");
            if (sorter == null) throw new UnsupportedOperationException("Required metadata missing");

            ComboBoxModel<String> model = setting.getOrCreateMetadata("table.mapFilter", () -> {
                NpcMapComboBoxModel m = new NpcMapComboBoxModel(starSystemAPI, maps -> sorter.allRowsChanged());
                setting.addListener(m);
                m.accept(setting.getValue());

                // Register selection of maps as a filter
                sorter.putRowFilter("map", new RowFilter<>() {
                    @Override
                    public boolean include(Entry<? extends NpcTableModel, ? extends Integer> entry) {
                        if (m.getSelectedMaps().isEmpty()) return true;
                        NpcTableModel.NpcRow row = (NpcTableModel.NpcRow) entry.getModel().getRow(entry.getIdentifier());
                        return row.getInfos().stream().anyMatch(npc -> !Collections.disjoint(npc.mapList, m.getSelectedMaps()));
                    }
                });

                return m;
            });

            return new JComboBox<>(model);
        }
    }


    public static class NpcTableDecorator implements Table.Decorator<NpcInfo> {

        private final ConfigAPI configAPI;

        public NpcTableDecorator(ConfigAPI configAPI) {
            this.configAPI = configAPI;
        }

        @Override
        public void handle(JTable jTable, JScrollPane jScrollPane,
                           @Nullable JPanel jPanel,
                           ConfigSetting<Map<String, NpcInfo>> setting) {

            NpcTableModel tableModel = setting.getMetadata("table.tableModel");
            if (tableModel == null) throw new UnsupportedOperationException("Table model must not be null");

            ConfigSetting<Boolean> group = configAPI.requireConfig(setting.getParent(), "group_npcs");
            group.addListener(tableModel);
            tableModel.accept(group.getValue());

            jTable.getRowSorter().setSortKeys(Arrays.asList(new RowSorter.SortKey(3, SortOrder.DESCENDING),
                    new RowSorter.SortKey(2, SortOrder.ASCENDING),
                    new RowSorter.SortKey(0, SortOrder.DESCENDING)));

            jTable.setDefaultEditor(SelectableItem.Formation.class, new FormationNpcInfoEditor());
            jTable.setDefaultEditor(NpcInfo.ExtraNpcInfo.class, new ExtraNpcInfoEditor());
        }
    }

    public static class BoxInfoDecorator implements Table.Decorator<BoxInfo> {

        @Override
        public void handle(JTable jTable, JScrollPane jScrollPane,
                           @Nullable JPanel jPanel,
                           ConfigSetting<Map<String, BoxInfo>> setting) {

            jTable.getRowSorter().setSortKeys(Arrays.asList(new RowSorter.SortKey(1, SortOrder.DESCENDING),
                    new RowSorter.SortKey(3, SortOrder.ASCENDING),
                    new RowSorter.SortKey(0, SortOrder.DESCENDING)));
        }
    }

}

package com.github.manolo8.darkbot.gui.tree.editors;

import com.github.manolo8.darkbot.gui.AdvancedConfig;
import com.github.manolo8.darkbot.gui.tree.utils.SizedLabel;

import eu.darkbot.api.config.ConfigSetting;
import eu.darkbot.api.config.types.ShipMode;
import eu.darkbot.api.config.util.OptionEditor;
import eu.darkbot.api.game.items.SelectableItem.Formation;
import eu.darkbot.api.managers.HeroAPI;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ItemEvent;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class NewShipModeEditor extends JPanel implements OptionEditor<ShipMode> {
    private HeroAPI.Configuration config;

    private final List<ConfigButton> configButtons = Arrays.stream(HeroAPI.Configuration.values())
            .filter(c -> c != HeroAPI.Configuration.UNKNOWN)
            .map(config -> new ConfigButton(this, config)).collect(Collectors.toList());

    private Formation formation;

    private final JComboBox<Formation> comboBox = new JComboBox<>(Formation.values());

    public NewShipModeEditor() {
        super(new FlowLayout(FlowLayout.LEFT, 0, 0));
        setOpaque(false);

        for (ConfigButton configButton : this.configButtons) {
            this.add(configButton);
        }
        this.comboBox.addItemListener(item -> {
            if (item.getStateChange() == ItemEvent.SELECTED) {
                setFormation((Formation) item.getItem());
            }
        });
        this.add(new SizedLabel("  "));
        this.add(this.comboBox);
    }

    public JComponent getEditorComponent(ConfigSetting<ShipMode> shipConfig) {
        ShipMode value = shipConfig.getValue();
        setConfig(value.getConfiguration());
        setFormation(value.getFormation());
        return this;
    }

    @Override
    public ShipMode getEditorValue() {
        return ShipMode.of(config, formation);
    }

    public Dimension getReservedSize() {
        return new Dimension(250, 0);
    }

    private void setConfig(HeroAPI.Configuration config) {
        this.config = config;
        this.configButtons.forEach(Component::repaint);
    }

    private void setFormation(Formation formation) {
        this.formation = formation;
        this.comboBox.setSelectedItem(formation);
    }

    private class ConfigButton extends JButton {
        private final NewShipModeEditor shipMode;
        private final HeroAPI.Configuration config;

        ConfigButton(NewShipModeEditor shipMode, HeroAPI.Configuration config) {
            super(String.valueOf(config.ordinal()));
            this.shipMode = shipMode;
            this.config = config;
            //noinspection SuspiciousNameCombination
            setPreferredSize(new Dimension(AdvancedConfig.EDITOR_HEIGHT, AdvancedConfig.EDITOR_HEIGHT));

            addActionListener(a -> setConfig(config));
        }

        @Override
        public boolean isDefaultButton() {
            return this.shipMode.config == config;
        }
    }
}

package com.github.manolo8.darkbot.gui.tree.editors;

import com.github.manolo8.darkbot.config.Config;
import com.github.manolo8.darkbot.gui.AdvancedConfig;
import com.github.manolo8.darkbot.gui.tree.utils.SizedLabel;
import eu.darkbot.api.config.ConfigSetting;
import eu.darkbot.api.config.types.ShipMode;
import eu.darkbot.api.config.util.OptionEditor;
import eu.darkbot.api.managers.HeroAPI;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Deprecated
public class ShipModeEditor extends JPanel implements OptionEditor<ShipMode> {

    private HeroAPI.Configuration config;

    private final List<ConfigButton> configButtons = Arrays.stream(HeroAPI.Configuration.values())
            .filter(c -> c != HeroAPI.Configuration.UNKNOWN)
            .map(ConfigButton::new).collect(Collectors.toList());

    private final CharacterEditor formationField = new CharacterEditor();

    public ShipModeEditor() {
        super(new FlowLayout(FlowLayout.LEFT, 0, 0));
        setOpaque(false);

        for (ConfigButton configButton : configButtons) {
            add(configButton);
            configButton.addKeyListener(formationField); // Relay key presses to formation
        }
        add(new SizedLabel("      Formation  "));
        add(formationField);
    }


    @Override
    public JComponent getEditorComponent(ConfigSetting<ShipMode> mode) {
        ShipMode value = mode.getValue();

        setConfig(value.getConfiguration());

        if (value instanceof Config.ShipConfig) {
            formationField.setValue(((Config.ShipConfig) value).FORMATION);
        } else {
            // TODO: show a formation selection dropdown?
            //  we cannot use old & new formats interchangeably here, because
            //  converting key to formation or formation to key can only be done while the
            //  bot is running. If running in no-op mode it's impossible to convert.
            //  Old format must stay old format not to break plugins, and new format can't be
            //  expressed with old format because it is unknown before runtime.
            //  The only real solution is to have different editors for old & new format,
            //  one that is key based, and one that is formation based.
            formationField.setValue(null);
        }

        return this;
    }

    private void setConfig(HeroAPI.Configuration config) {
        this.config = config;
        configButtons.forEach(ConfigButton::repaint);
    }

    @Override
    public ShipMode getEditorValue() {
        return new Config.ShipConfig(config.ordinal(), formationField.getEditorValue());
    }

    @Override
    public Dimension getPreferredSize() {
        return AdvancedConfig.forcePreferredHeight(super.getPreferredSize());
    }

    @Override
    public Dimension getReservedSize() {
        return new Dimension(250, 0);
    }

    private class ConfigButton extends JButton {
        private final HeroAPI.Configuration config;

        ConfigButton(HeroAPI.Configuration config) {
            super(String.valueOf(config.ordinal()));
            this.config = config;
            //noinspection SuspiciousNameCombination
            setPreferredSize(new Dimension(AdvancedConfig.EDITOR_HEIGHT, AdvancedConfig.EDITOR_HEIGHT));

            addActionListener(a -> setConfig(config));
        }

        @Override
        public boolean isDefaultButton() {
            return ShipModeEditor.this.config == config;
        }
    }

}

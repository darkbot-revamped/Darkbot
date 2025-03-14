package com.github.manolo8.darkbot.config.types.suppliers;

import com.github.manolo8.darkbot.core.manager.HeroManager;
import eu.darkbot.api.config.annotations.Dropdown;
import eu.darkbot.api.game.items.SelectableItem;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

public class LaserSupplier implements Dropdown.Options<SelectableItem.Laser>{
    @Override
    public Collection<SelectableItem.Laser> options() {
        return HeroManager.instance.main.config.BOT_SETTINGS.BOT_GUI.LASER;
    }

    @Override
    public @NotNull String getText(SelectableItem.@Nullable Laser option) {
        return Dropdown.Options.super.getText(option);
    }

    @Override
    public @Nullable String getTooltip(SelectableItem.@Nullable Laser option) {
        return Dropdown.Options.super.getTooltip(option);
    }
}

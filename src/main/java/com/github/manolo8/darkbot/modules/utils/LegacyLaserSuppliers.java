package com.github.manolo8.darkbot.modules.utils;

import com.github.manolo8.darkbot.Main;
import com.github.manolo8.darkbot.config.Config;
import com.github.manolo8.darkbot.config.NpcExtra;
import eu.darkbot.api.config.ConfigSetting;
import eu.darkbot.api.extensions.Feature;
import eu.darkbot.api.extensions.selectors.LaserSelector;
import eu.darkbot.api.extensions.selectors.PrioritizedSupplier;
import eu.darkbot.api.game.entities.Npc;
import eu.darkbot.api.game.items.Item;
import eu.darkbot.api.game.items.ItemFlag;
import eu.darkbot.api.game.items.SelectableItem;
import eu.darkbot.api.managers.ConfigAPI;
import eu.darkbot.api.managers.HeroAPI;
import eu.darkbot.api.managers.HeroItemsAPI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class LegacyLaserSuppliers {

    @Feature(name = "Default laser selector", description = "Default ammo selection manager")
    public static class DefaultLaserSupplier implements LaserSelector, PrioritizedSupplier<SelectableItem.Laser> {

        private final HeroAPI hero;
        private final HeroItemsAPI items;
        private final ConfigSetting<SelectableItem.Laser> ammoLaser;

        public DefaultLaserSupplier(HeroAPI hero, HeroItemsAPI items, ConfigAPI config) {
            this.hero = hero;
            this.items = items;
            this.ammoLaser = config.requireConfig("loot.laser");
        }

        @Override
        public @NotNull PrioritizedSupplier<SelectableItem.Laser> getLaserSupplier() {
            return this;
        }

        @Override
        public SelectableItem.Laser get() {
            Npc target = hero.getLocalTargetAs(Npc.class);

            if (target != null) {
                Optional<SelectableItem.Laser> ammo = target.getInfo().getAmmo();
                if (ammo.isPresent()) return ammo.get();
            }

            return ammoLaser.getValue();
        }
    }

    @Feature(name = "Rsb laser selector", description = "Rsb ammo selection manager")
    public static class RsbLaserSupplier implements LaserSelector, PrioritizedSupplier<SelectableItem.Laser> {

        private final HeroAPI hero;
        private final ConfigSetting<Config.Loot.Rsb> rsbSettings;
        private final HeroItemsAPI heroItems;

        private SelectableItem.Laser laser;

        public RsbLaserSupplier(HeroAPI hero, ConfigAPI config, HeroItemsAPI heroItems) {
            this.hero = hero;
            this.rsbSettings = config.requireConfig("loot.rsb");
            this.heroItems = heroItems;
        }

        @Override
        public @NotNull PrioritizedSupplier<SelectableItem.Laser> getLaserSupplier() {
            return this;
        }

        @Override
        public SelectableItem.Laser get() {
            return shouldRsb() ? laser : null;
        }

        @Override
        public @Nullable Priority getPriority() {
            return Priority.MODERATE;
        }

        private boolean shouldRsb() {
            Npc target = hero.getLocalTargetAs(Npc.class);

            Config.Loot.Rsb RSB = rsbSettings.getValue();
            if (target == null || !RSB.ENABLED || RSB.KEY == null || !target.getInfo().hasExtraFlag(NpcExtra.USE_RSB))
                return false;

            Item i = heroItems.getItem(RSB.KEY);
            laser = i != null ? i.getAs(SelectableItem.Laser.class) : null;

            return laser != null && heroItems.getItem(i, ItemFlag.USABLE, ItemFlag.READY)
                    .filter(item -> item.getQuantity() > 50)
                    .isPresent();
        }
    }

    @Feature(name = "Sab laser selector", description = "Sab ammo selection manager")
    public static class SabLaserSupplier implements LaserSelector, PrioritizedSupplier<SelectableItem.Laser> {

        private final ConfigSetting<Config.Loot.Sab> sabSettings;
        private final Main main;
        private final HeroItemsAPI heroItems;
        private final HeroAPI hero;

        private SelectableItem.Laser laser;

        public SabLaserSupplier(ConfigAPI config, HeroAPI hero, Main main, HeroItemsAPI heroItems) {
            this.hero = hero;
            this.sabSettings = config.requireConfig("loot.sab");
            this.main = main;
            this.heroItems = heroItems;
        }

        @Override
        public @NotNull PrioritizedSupplier<SelectableItem.Laser> getLaserSupplier() {
            return this;
        }

        @Override
        public SelectableItem.Laser get() {
            return shouldSab() ? laser : null;
        }

        @Override
        public @Nullable Priority getPriority() {
            return Priority.LOW;
        }

        private boolean shouldSab() {
            Npc target = hero.getLocalTargetAs(Npc.class);

            Config.Loot.Sab SAB = sabSettings.getValue();
            if (target == null || !SAB.ENABLED
                    || target.getInfo().hasExtraFlag(NpcExtra.NO_SAB)
                    || hero.getHealth().shieldPercent() > SAB.PERCENT
                    || target.getHealth().getShield() <= SAB.NPC_AMOUNT
                    || (SAB.CONDITION != null && !SAB.CONDITION.get(main).toBoolean()))
                return false;

            return heroItems.getItem(SelectableItem.Laser.SAB_50, ItemFlag.USABLE, ItemFlag.READY)
                    .filter(item -> item.getQuantity() > 50)
                    .isPresent();
        }
    }
}

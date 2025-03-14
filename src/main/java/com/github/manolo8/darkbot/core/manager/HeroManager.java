package com.github.manolo8.darkbot.core.manager;

import com.github.manolo8.darkbot.Main;
import com.github.manolo8.darkbot.config.Config;
import com.github.manolo8.darkbot.config.LegacyShipMode;
import com.github.manolo8.darkbot.config.NpcExtra;
import com.github.manolo8.darkbot.core.BotInstaller;
import com.github.manolo8.darkbot.core.entities.Entity;
import com.github.manolo8.darkbot.core.entities.Npc;
import com.github.manolo8.darkbot.core.entities.Pet;
import com.github.manolo8.darkbot.core.entities.Player;
import com.github.manolo8.darkbot.core.entities.Ship;
import com.github.manolo8.darkbot.core.itf.Manager;
import com.github.manolo8.darkbot.core.objects.Map;
import com.github.manolo8.darkbot.core.objects.facades.SettingsProxy;
import com.github.manolo8.darkbot.core.utils.Drive;
import com.github.manolo8.darkbot.extensions.features.Feature;
import com.github.manolo8.darkbot.extensions.features.handlers.ShipModeSelectorHandler;
import eu.darkbot.api.config.types.ShipMode;
import eu.darkbot.api.extensions.selectors.PrioritizedSupplier;
import eu.darkbot.api.extensions.selectors.ShipModeSelector;
import eu.darkbot.api.game.entities.Portal;
import eu.darkbot.api.game.items.Item;
import eu.darkbot.api.game.items.ItemCategory;
import eu.darkbot.api.game.items.ItemFlag;
import eu.darkbot.api.game.items.SelectableItem;
import eu.darkbot.api.game.other.GameMap;
import eu.darkbot.api.game.other.Lockable;
import eu.darkbot.api.managers.HeroAPI;
import eu.darkbot.api.managers.HeroItemsAPI;
import eu.darkbot.api.utils.Inject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

import java.util.Collection;
import java.util.Optional;

import static com.github.manolo8.darkbot.Main.API;
import static com.github.manolo8.darkbot.core.objects.facades.SettingsProxy.KeyBind.ATTACK_LASER;
import static com.github.manolo8.darkbot.core.objects.facades.SettingsProxy.KeyBind.ATTACK_ROCKET;
import static com.github.manolo8.darkbot.core.objects.facades.SettingsProxy.KeyBind.JUMP_GATE;
import static com.github.manolo8.darkbot.core.objects.facades.SettingsProxy.KeyBind.TOGGLE_CONFIG;

public class HeroManager extends Player implements Manager, HeroAPI {

    @Deprecated(forRemoval = true)
    public static HeroManager instance;
    public final Main main;
    public final Pet pet;
    public final Drive drive;

    private final SettingsManager settings;
    private final SettingsProxy keybinds;
    private final Collection<? extends Portal> portals;
    private final HeroItemsAPI items;

    private final ShipModeSelectorHandler shipModeHandler;
    private final MutableShipMode shipMode;

    public long nextCpuMapDuration;
    public Map map, nextMap, nextCpuMap;

    @Deprecated
    public Ship target;
    private Ship lastTarget; // Copy of target, to detect when it has been modified
    private Lockable localTarget;
    public int config;

    private long staticAddress;
    private Entity inGameTarget;
    private Configuration configuration = Configuration.UNKNOWN;
    private Character formationChar; // Only present for legacy formation changes
    private long configTime;
    private long formationTime;
    private long portalTime;

    public HeroManager(Main main,
                       SettingsManager settingsManager,
                       MapManager mapManager,
                       Drive drive,
                       FacadeManager facadeManager,
                       StarManager star,
                       HeroItemsAPI items,
                       ShipModeSelectorHandler shipModeHandler) {
        instance = this;

        this.main = super.main = main;
        added(main);
        this.settings = settingsManager;
        this.keybinds = facadeManager.settings;
        this.portals = mapManager.entities.getPortals();
        this.drive = drive;
        main.status.add(drive::toggleRunning);
        this.pet = new Pet(main);
        this.pet.main = main;
        this.map = this.nextMap = this.nextCpuMap = star.byId(-1);

        this.items = items;

        this.shipModeHandler = shipModeHandler;
        this.shipMode = new MutableShipMode(items);
    }

    @Override
    public void install(BotInstaller botInstaller) {
        botInstaller.screenManagerAddress.add(value -> staticAddress = value + 240);
        botInstaller.invalid.add(v -> {
            if (v) locationInfo.update(0);
        });
    }

    public void tick() {
        long address = API.readLong(staticAddress);
        if (this.address != address) update(address);

        update();

        drive.checkMove();

        if (main.isRunning())
            setConfigAndFormation(shipModeHandler.getBest());
    }

    @Override
    public void update() {
        super.update();
        config = settings.config;
        configuration = Configuration.of(config);

        long petAddress = API.readLong(address + 176);
        if (petAddress != pet.address) pet.update(petAddress);
        pet.update();

        long targetPtr = API.readLong(main.mapManager.mapAddress, 128, 40);

        if (targetPtr == 0) inGameTarget = null;
        else if (targetPtr == petAddress) inGameTarget = pet;
        else if (inGameTarget == null || inGameTarget.address != targetPtr)
            inGameTarget = main.mapManager.entities.findEntityByAddress(targetPtr);

        if (lastTarget != target) setLocalTarget(target);
    }

    @Override
    public void update(long address) {
        super.update(address);

        pet.update(API.readLong(address + 176));
        id = API.readInt(address + 56);
    }

    public boolean hasTarget() {
        return this.localTarget != null && localTarget.isValid();
    }

    public long timeTo(double distance) {
        return super.timeTo(distance);
    }

    @Deprecated
    public void jumpPortal(com.github.manolo8.darkbot.core.entities.Portal portal) {
        jumpPortal((Portal) portal);
    }

    private boolean clickPortal;
    private Portal lastPortal;
    public void jumpPortal(Portal portal) {
        if (!main.guiManager.canJumpPortal()) return;
        if (!portal.isValid()) return;
        long timeSinceLastJump = System.currentTimeMillis() - portalTime;
        if (timeSinceLastJump < 500) return; // Minimum delay
        if ((timeSinceLastJump > 20000 || isNotJumping(portal)) &&
                (portal.isSelectable() || portals.stream().noneMatch(p -> p != portal && p.isSelectable()))) {

            // If we tried to jump the same portal under 30s ago, try to toggle between click/key
            clickPortal = lastPortal == portal && timeSinceLastJump < 30_000 && portal.isSelectable() && !clickPortal;

            if (clickPortal) portal.trySelect(false);
            else keybinds.pressKeybind(JUMP_GATE);

            lastPortal = portal;
            portalTime = System.currentTimeMillis();
        }
    }

    // Consider not jumping if still on current map and nextMap is either unset or not the portal target map
    private boolean isNotJumping(Portal portal) {
        return !portal.isJumping() && map.id == settings.currMap &&
                (settings.nextMap == -1 || portal.getTargetMap().map(m -> m.getId() != settings.nextMap).orElse(true));
    }

    public boolean attackMode() {
        return setMode(main.config.GENERAL.OFFENSIVE);
    }

    public boolean attackMode(Npc target) {
        if (target == null) return attackMode();
        Config.ConfigFormation config = this.main.config.GENERAL.OFFENSIVE;

        boolean otherConfig = target.npcInfo.extra.has(NpcExtra.OPPOSITE_CONFIG);

        HeroAPI.Configuration newConfig = HeroAPI.Configuration.of(
                otherConfig ? (config.getConfiguration().ordinal() % 2) + 1 : config.getConfiguration().ordinal()
        );

        SelectableItem.Formation formation = target.npcInfo.attackFormation != null ?
                target.npcInfo.attackFormation :
                config.getFormation();

        return setMode(new ShipMode.ShipModeImpl(newConfig, formation));
    }

    public boolean runMode() {
        return setMode(this.main.config.GENERAL.RUN);
    }

    public boolean roamMode() {
        return setMode(main.config.GENERAL.ROAM);
    }

    @Deprecated
    public boolean setMode(Config.ShipConfig config) {
        return setMode(config.CONFIG, config.FORMATION);
    }

    @Deprecated
    public boolean setMode(int con, Character form) {
        shipMode.setLegacy(con, form);
        return isInMode(shipMode);
    }

    @Deprecated
    public boolean isInMode(Config.ShipConfig config) {
        return isInMode((ShipMode) config);
    }

    @Deprecated
    public boolean isInMode(int config, Character formation) {
        if (this.config == config) {
            if (this.formationChar == formation) return true;

            if (formation != null)
                return Optional.ofNullable(items.getItem(formation, ItemCategory.DRONE_FORMATIONS))
                        .map(i -> i.getAs(SelectableItem.Formation.class))
                        .map(f -> f == getFormation()).orElse(false);
        }

        return false;
    }

    /**
     * @param entity the target entity to set
     * @deprecated use {@link #setLocalTarget(Lockable)} instead
     */
    @Deprecated
    public void setTarget(Ship entity) {
        this.localTarget = this.target = this.lastTarget = entity;
    }

    private void setConfigAndFormation(ShipMode mode) {
        if (mode.getConfiguration() == Configuration.UNKNOWN)
            throw new IllegalStateException("Passed UNKNOWN configuration! Use only FIRST or SECOND, " +
                    "last supplier used: " + shipModeHandler.getLastUsedSupplier().getClass());

        if (mode.getConfiguration() != null &&
                mode.getConfiguration() != getConfiguration()) toggleConfiguration();

        if (mode instanceof LegacyShipMode && ((LegacyShipMode) mode).isLegacyFormation())
            setFormationLegacy(((LegacyShipMode) mode).getLegacyFormation());
        else setFormation(mode.getFormation());
    }

    private void toggleConfiguration() {
        if (System.currentTimeMillis() - configTime <= 5500L) return;

        if (keybinds.pressKeybind(TOGGLE_CONFIG))
            this.configTime = System.currentTimeMillis();
        //else toggle config key bind is missing?
    }

    private void setFormation(SelectableItem.Formation formation) {
        if (formation == null || formation == getFormation()) return;

        main.facadeManager.slotBars.useItem(formation, 2000, ItemFlag.NOT_SELECTED)
                .ifSuccessful(r -> formationTime = System.currentTimeMillis());
    }

    @Deprecated
    private void setFormationLegacy(Character formation) {
        if ((this.formationChar != formation && System.currentTimeMillis() - formationTime > 3500L)
                || System.currentTimeMillis() - formationTime > 60_000) { // re-click formation after 60sec

            if (main.facadeManager.slotBars.useItem(this.formationChar = formation))
                this.formationTime = System.currentTimeMillis();
        }
    }

    @Override
    public @UnknownNullability Lockable getLocalTarget() {
        return localTarget;
    }

    @Override
    public void setLocalTarget(@Nullable Lockable lockable) {
        if (lockable instanceof Ship) target = lastTarget = (Ship) lockable;
        else target = lastTarget = null;

        this.localTarget = lockable;
    }

    @Nullable
    @Override
    public Entity getTarget() {
        return inGameTarget;
    }

    @Override
    public GameMap getMap() {
        return map;
    }

    public GameMap getNextMap() {
        return nextMap;
    }

    @Override
    public Configuration getConfiguration() {
        return configuration;
    }

    @Override
    public boolean isInMode(@NotNull ShipMode mode) {
        if (mode instanceof LegacyShipMode && ((LegacyShipMode) mode).isLegacyFormation()) {
            return mode.getConfiguration() == getConfiguration() &&
                    (mode.getFormation() == getFormation() || formationChar == ((LegacyShipMode) mode).getLegacyFormation());
        } else {
            return mode.getConfiguration() == getConfiguration() && mode.getFormation() == getFormation();
        }
    }

    @Override
    public boolean setMode(@NotNull ShipMode mode) {
        this.shipMode.set(mode);
        return isInMode(mode);
    }

    @Override
    public boolean setAttackMode(eu.darkbot.api.game.entities.Npc target) {
        return attackMode((Npc) target); //todo change with predefined ShipMode by user & set into selector
    }

    @Override
    public boolean setRoamMode() {
        return roamMode(); //todo change with predefined ShipMode by user & set into selector
    }

    @Override
    public boolean setRunMode() {
        return runMode(); //todo change with predefined ShipMode by user & set into selector
    }

    @Override
    public boolean triggerLaserAttack() {
        return keybinds.pressKeybind(ATTACK_LASER);
    }

    @Override
    public boolean launchRocket() {
        return keybinds.pressKeybind(ATTACK_ROCKET);
    }

    @Override
    public SelectableItem.Laser getLaser() {
        return items.getItems(ItemCategory.LASERS).stream()
                .filter(Item::isSelected)
                .findFirst()
                .map(item -> item.getAs(SelectableItem.Laser.class))
                .orElse(null);
    }

    @Override
    public SelectableItem.Rocket getRocket() {
        return items.getItems(ItemCategory.ROCKETS).stream()
                .filter(Item::isSelected)
                .findFirst()
                .map(item -> item.getAs(SelectableItem.Rocket.class))
                .orElse(null);
    }

    @Override
    public boolean hasPet() {
        return pet.isValid();
    }

    @Override
    public Optional<eu.darkbot.api.game.entities.Pet> getPet() {
        return hasPet() ? Optional.of(pet) : Optional.empty();
    }

    private static class MutableShipMode implements LegacyShipMode {
        private final HeroItemsAPI items;
        private Configuration configuration;
        private SelectableItem.Formation formation;

        private Character toSelectChar;
        private boolean isLegacyFormation;

        public MutableShipMode(HeroItemsAPI items) {
            this.items = items;
        }

        @Override
        public Configuration getConfiguration() {
            return configuration;
        }

        @Override
        public SelectableItem.Formation getFormation() {
            return formation;
        }

        public void set(ShipMode other) {
            this.configuration = other.getConfiguration();
            if (other instanceof LegacyShipMode) {
                this.toSelectChar = ((LegacyShipMode) other).getLegacyFormation();

                setLegacy();
            } else {
                this.formation = other.getFormation();
                this.toSelectChar = null;
                this.isLegacyFormation = false;
            }
        }

        public void setLegacy(int configuration, Character toSelectChar) {
            this.configuration = Configuration.of(configuration);
            this.toSelectChar = toSelectChar;

            setLegacy();
        }

        @Deprecated
        private void setLegacy() {
            Item item = items.getItem(toSelectChar, ItemCategory.DRONE_FORMATIONS);
            this.formation = item == null ? null : item.getAs(SelectableItem.Formation.class);

            this.isLegacyFormation = formation == null;
        }

        @Override
        public @Nullable Character getLegacyFormation() {
            return toSelectChar;
        }

        @Override
        public boolean isLegacyFormation() {
            return isLegacyFormation;
        }
    }

    @Feature(name = "Default Ship Mode Supplier", description = "Sets the fallback ship mode")
    public static class DefaultShipModeSupplier implements ShipModeSelector, PrioritizedSupplier<ShipMode> {

        private HeroManager hero;

        @Inject
        public void setHero(HeroManager hero) {
            this.hero = hero;
        }

        @Override
        public @NotNull PrioritizedSupplier<ShipMode> getShipModeSupplier() {
            return this;
        }

        @Override
        public ShipMode get() {
            return hero.shipMode; //default ship mode
        }
    }
}

package com.github.manolo8.darkbot.core.manager;

import com.github.manolo8.darkbot.Main;
import com.github.manolo8.darkbot.core.BotInstaller;
import com.github.manolo8.darkbot.core.api.Capability;
import com.github.manolo8.darkbot.core.itf.Manager;
import com.github.manolo8.darkbot.core.objects.Gui;
import com.github.manolo8.darkbot.core.objects.facades.SettingsProxy;
import com.github.manolo8.darkbot.core.objects.facades.SlotBarsProxy;
import com.github.manolo8.darkbot.core.objects.facades.StatsProxy;
import com.github.manolo8.darkbot.core.objects.gui.ChatGui;
import com.github.manolo8.darkbot.core.objects.gui.DiminishQuestGui;
import com.github.manolo8.darkbot.core.objects.gui.DispatchIconGui;
import com.github.manolo8.darkbot.core.objects.gui.DispatchIconOkGui;
import com.github.manolo8.darkbot.core.objects.gui.DispatchPopupRewardGui;
import com.github.manolo8.darkbot.core.objects.gui.GateSpinnerGui;
import com.github.manolo8.darkbot.core.objects.gui.LogoutGui;
import com.github.manolo8.darkbot.core.objects.gui.OreTradeGui;
import com.github.manolo8.darkbot.core.objects.gui.RefinementGui;
import com.github.manolo8.darkbot.core.objects.gui.SettingsGui;
import com.github.manolo8.darkbot.core.objects.gui.TargetedOfferGui;
import com.github.manolo8.darkbot.core.objects.swf.FlashMap;
import eu.darkbot.api.PluginAPI;
import eu.darkbot.api.game.other.Area;
import eu.darkbot.api.managers.GameScreenAPI;
import eu.darkbot.util.Timer;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.Predicate;

import static com.github.manolo8.darkbot.Main.API;

public class GuiManager implements Manager, GameScreenAPI {

    private final Main main;
    private final PluginAPI pluginAPI;
    private final SlotBarsProxy slotBarsProxy;
    private final SettingsProxy settingsProxy;
    private final StatsProxy statsProxy;
    private final RepairManager repairManager;

    private final FlashMap<String, Gui> guis = FlashMap.of(String.class, Gui.class).noAuto();

    private long reconnectTime;
    private long lastDeath = -1;
    private long lastRepairAttempt;
    private long validTime;
    private long guiAddress;

    public final Gui lostConnection;
    public final Gui connecting;
    public final Gui quests;
    public final Gui monthlyDeluxe;
    public final Gui returnLogin;
    public final Gui minimap;
    public final Gui targetedOffers;
    public final LogoutGui logout;
    public final Gui eventProgress;
    public final Gui eternalGate;
    public final Gui blacklightGate;
    public final Gui astralGate;
    public final Gui astralSelection;
    public final Gui seasonPass;
    public final RefinementGui refinement;
    public final PetManager pet;
    public final OreTradeGui oreTrade;
    public final GroupManager group;
    public final SettingsGui settingsGui;
    public final ChatGui chat;

    public final Gui assembly;

    private final Gui commandCenter;

    public final Timer loggedInTimer = Timer.get(15_000);
    private LoadStatus checks = LoadStatus.WAITING;

    private enum LoadStatus {
        WAITING(gm -> gm.main.hero.address != 0 && !gm.connecting.isVisible()),
        AFTER_LOGIN(gm -> {
            gm.loggedInTimer.activate();
            return true;
        }),
        CLICKING_ONE(gm -> {
            if (gm.loggedInTimer.getRemainingFuse() > 13000) return false;
            Character keyBind = gm.main.facadeManager.slotBars.getKeyBind(gm.main.hero.getFormation());
//            API.keyboardClick(keyBind == null ? gm.main.config.LOOT.AMMO_KEY : keyBind, false);
            return true;
        }),
        CLICKING_TWO(gm -> {
            if (gm.loggedInTimer.getRemainingFuse() > 12000) return false;
            Character keyBind = gm.main.facadeManager.slotBars.getKeyBind(gm.main.hero.getFormation());
//            API.keyboardClick(keyBind == null ? gm.main.config.LOOT.AMMO_KEY : keyBind, false);
            return true;
        }),
        CLICKING_THREE(gm -> {
            if (gm.loggedInTimer.getRemainingFuse() > 11000) return false;
            Character keyBind = gm.main.facadeManager.slotBars.getKeyBind(gm.main.hero.getFormation());
//            API.keyboardClick(keyBind == null ? gm.main.config.LOOT.AMMO_KEY : keyBind, false);
            return true;
        }),
        DONE(q -> false);

        final Predicate<GuiManager> canAdvance;

        LoadStatus(Predicate<GuiManager> next) {
            this.canAdvance = next;
        }
    }

    private final GuiCloser guiCloser;

    public int deaths;

    private boolean needRefresh;

    public GuiManager(Main main, PluginAPI pluginAPI, RepairManager repairManager) {
        this.main = main;
        this.pluginAPI = pluginAPI;
        this.slotBarsProxy = pluginAPI.requireInstance(SlotBarsProxy.class);
        this.settingsProxy = pluginAPI.requireInstance(SettingsProxy.class);
        this.statsProxy = pluginAPI.requireInstance(StatsProxy.class);
        this.repairManager = repairManager;

        this.validTime = System.currentTimeMillis();

        this.pet = register("pet", PetManager.class);
        this.oreTrade = register("ore_trade", OreTradeGui.class);
        this.group = register("group", GroupManager.class);

        this.main.status.add(value -> validTime = System.currentTimeMillis());
        this.lostConnection = register("lost_connection");
        this.connecting = register("connection");
        this.quests = register("quests");
        this.monthlyDeluxe = register("monthly_deluxe");
        this.returnLogin = register("returnee_login");
        this.minimap = register("minimap");
        this.targetedOffers = register("targetedOffers", TargetedOfferGui.class);
        this.logout = register("logout", LogoutGui.class);
        this.eventProgress = register("eventProgress");
        this.eternalGate = register("eternal_gate");
        this.blacklightGate = register("eternal_blacklight");
        this.astralGate = register("rogue_lite");
        this.astralSelection = register("rogue_lite_selection");
        this.seasonPass = register("seasonPass");
        this.refinement = register("refinement", RefinementGui.class);
        this.chat = register("chat", ChatGui.class);
        this.settingsGui = register("settings", SettingsGui.class);

        register("dispatch", DispatchManager.class);
        register("dispatch_popup_reward_list", DispatchPopupRewardGui.class);
        register("popup_generic_icon", DispatchIconGui.class);
        register("popup_generic_icon_ok", DispatchIconOkGui.class);
        this.assembly = register("assembly");

        register("ggBuilder", GateSpinnerGui.class);
        register("diminish_quests", DiminishQuestGui.class);
        register("refinement_count");

        this.commandCenter = register("command_center");

        this.guiCloser = new GuiCloser(quests, monthlyDeluxe, returnLogin);
    }

    private Gui register(String key) {
        return register(key, Gui.class);
    }

    private <T extends Gui> T register(String key, Class<T> gui) {
        return guis.putUpdatable(key, pluginAPI.requireInstance(gui));
    }

    @Override
    public void install(BotInstaller botInstaller) {
        botInstaller.invalid.add(value -> {
            if (!value) {
                validTime = System.currentTimeMillis();
                checks = LoadStatus.WAITING;
                guiCloser.reset();
            }
            connecting.reset();
            API.resetCache();
        });

        botInstaller.guiManagerAddress.add(value -> {
            guiAddress = value;
            guis.update(API.readLong(guiAddress + 112));

            checks = LoadStatus.WAITING;
            guiCloser.reset();
        });
    }

    public long getAddress() {
        return guiAddress;
    }

    public void tick() {
        guis.update();

        if (checks != LoadStatus.DONE && checks.canAdvance.test(this))
            checks = LoadStatus.values()[checks.ordinal() + 1];

        if (main.isRunning()) {
            // GuiCloser closes just once per restart, targeted & cmd center can appear after port jumps
            guiCloser.tick();

            targetedOffers.show(false);
            commandCenter.show(false);
        }

        this.deaths = repairManager.getDeathAmount();
    }

    private void tryReconnect(Gui gui) {
        if (System.currentTimeMillis() - reconnectTime > 5000) {
            reconnectTime = System.currentTimeMillis();
            if (logout.visible) {
                triggerRefresh("Triggering refresh: reconnect while logout is visible", true);
            } else {
                gui.click(46, 180);
            }
        }
    }

    public boolean canJumpPortal() {
        return loggedInTimer.isInactive();
    }

    public boolean tryRevive() {
        if (repairManager.setBeforeReviveTime())
            return false;
        if (System.currentTimeMillis() - lastRepairAttempt <= 10000) return false;

        if (repairManager.tryRevive()) {
            lastRepairAttempt = System.currentTimeMillis();
            if (main.config.MISCELLANEOUS.DRONE_REPAIR_PERCENTAGE != 0) this.main.backpage.checkDronesAfterKill();
        } else return false;

        return true;
    }

    private void checkInvalid() {
        if (System.currentTimeMillis() - validTime > 90_000 + (main.hero.map.id == -1 ? 180_000 : 0)) {
            triggerRefresh("Triggering refresh: gui manger was invalid for too long. " +
                    "(Make sure your hp fills up, equip an auto-repair CPU if you're missing one)", true);
        }
    }

    private void triggerRefresh(String reason, boolean clearCache) {
        System.out.println(reason);
        if (clearCache)
            clearCache();

        API.handleRefresh();
        validTime = System.currentTimeMillis();
    }

    public boolean canTickModule() {
        // visible var is sometimes false even if lost connection window is visible
        if (lostConnection.address > 0) {
            //Wait 2.5 seconds to reconnect
            if (lostConnection.lastUpdatedOver(2500)) {
                tryReconnect(lostConnection);
                checkInvalid();
            }
            return false;
        } else if (connecting.visible) {

            if (connecting.lastUpdatedOver(30000)) {
                triggerRefresh("Triggering refresh: connection window stuck for too long", true);
                connecting.reset();
            }

            return false;
        }

        // If logout is being shown without us having clicked, it's a DO bug, hide it
        if (logout.visible &&
                logout.isAnimationDone() &&
                logout.getLastShown() < System.currentTimeMillis() - 30000L) {
            logout.show(false);
            return false;
        }

        if (repairManager.isDestroyed()) {
            this.needRefresh = true;
            main.hero.drive.stop(false);

            if (lastDeath == -1) {
                lastDeath = System.currentTimeMillis();
            }

            if (!tryRevive()) {
                // Refresh if stuck on revive for 90 seconds, revive locations may have different cooldowns
                if ((System.currentTimeMillis() - lastDeath - main.config.GENERAL.SAFETY.WAIT_BEFORE_REVIVE * 1000L) > 90_000) {
                    triggerRefresh("Triggering refresh: stuck on revive for too long!", false);
                }

                return false;
            }

            if (main.config.GENERAL.SAFETY.MAX_DEATHS != -1 &&
                    repairManager.getDeathAmount() >= main.config.GENERAL.SAFETY.MAX_DEATHS) main.setRunning(false);
            else checkInvalid();

            return false;
        } else {
            lastDeath = -1;
        }

        HeroManager hero = main.hero;
        if (this.needRefresh && System.currentTimeMillis() - lastRepairAttempt > 5_000) {
            this.needRefresh = false;
            if (main.config.MISCELLANEOUS.REFRESH_AFTER_REVIVE) {
                triggerRefresh("Triggering refresh: refreshing after death", false);
                return false;
            }
        }
        if (System.currentTimeMillis() - lastRepairAttempt < main.config.GENERAL.SAFETY.WAIT_AFTER_REVIVE * 1000L) {
            validTime = System.currentTimeMillis();
            return false;
        } else if (hero.locationInfo.isLoaded()
                && (hero.locationInfo.isMoving() || System.currentTimeMillis() - hero.drive.lastMoved > 20 * 1000)
                && (hero.health.hpIncreasedIn(30_000) || hero.health.hpDecreasedIn(30_000) || hero.health.hpPercent() == 1 || (hero.hasTarget() && hero.isAttacking(hero.getLocalTarget())))
                && (hero.health.shIncreasedIn(30_000) || hero.health.shDecreasedIn(30_000) || hero.health.shieldPercent() == 1 || hero.health.shieldPercent() == 0)) {
            validTime = System.currentTimeMillis();
        }

        checkInvalid();

        return main.hero.locationInfo.isLoaded();
    }

    private void clearCache() {
        if (main.config.BOT_SETTINGS.API_CONFIG.CLEAR_CACHE_ON_STUCK &&
                API.hasCapability(Capability.HANDLER_CLEAR_CACHE))
            API.clearCache(".*");
    }

    @Override
    public Area.Rectangle getViewBounds() {
        return main.mapManager.screenBound;
    }

    @Override
    public Collection<? extends eu.darkbot.api.game.other.Gui> getGuis() {
        return guis.getValueList();
    }

    @Override
    public @Nullable Gui getGui(String key) {
        return guis.getOrWrapper(key);
    }

    @Override
    public int getFps() {
        return statsProxy.getFps();
    }

    @Override
    public int getMemory() {
        return statsProxy.getMemory();
    }

    @Override
    public void zoomIn() {
        settingsProxy.getCharacterOf(SettingsProxy.KeyBind.ZOOM_IN)
                .ifPresent(API::keyboardClick);
    }

    @Override
    public void zoomOut() {
        settingsProxy.getCharacterOf(SettingsProxy.KeyBind.ZOOM_OUT)
                .ifPresent(API::keyboardClick);
    }

    @Override
    public void focusOnChat() {
        settingsProxy.getCharacterOf(SettingsProxy.KeyBind.FOCUS_CHAT)
                .ifPresent(API::keyboardClick);
    }

    @Override
    public void toggleMonitoring() {
        settingsProxy.getCharacterOf(SettingsProxy.KeyBind.TOGGLE_MONITORING)
                .ifPresent(API::keyboardClick);
    }

    @Override
    public void toggleWindows() {
        settingsProxy.getCharacterOf(SettingsProxy.KeyBind.TOGGLE_WINDOWS)
                .ifPresent(API::keyboardClick);
    }

    @Override
    public void toggleCategoryBar(boolean visible) {
        settingsProxy.getCharacterOf(SettingsProxy.KeyBind.TOGGLE_CATEGORYBAR)
                .filter(c -> slotBarsProxy.isCategoryBarVisible() != visible)
                .ifPresent(API::keyboardClick);
    }

    @Override
    public void toggleProActionBar(boolean visible) {
        settingsProxy.getCharacterOf(SettingsProxy.KeyBind.TOGGLE_PRO_ACTION)
                .filter(c -> slotBarsProxy.proActionBar.address != 0 && slotBarsProxy.isProActionBarVisible() != visible)
                .ifPresent(API::keyboardClick);
    }

    private static class GuiCloser {
        private final Gui[] managedGuis;
        private final boolean[] closed;

        public GuiCloser(Gui... managedGuis) {
            this.managedGuis = managedGuis;
            this.closed = new boolean[managedGuis.length];
        }

        public void tick() {
            for (int i = 0; i < managedGuis.length; i++) {
                if (closed[i]) continue;

                Gui gui = managedGuis[i];
                if (gui.lastUpdatedOver(5000) && gui.show(false)) {
                    closed[i] = true;
                }
            }
        }

        public void reset() {
            Arrays.fill(closed, false);
        }

    }

}

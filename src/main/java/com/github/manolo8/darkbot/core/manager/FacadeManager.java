package com.github.manolo8.darkbot.core.manager;

import com.github.manolo8.darkbot.core.BotInstaller;
import com.github.manolo8.darkbot.core.itf.Manager;
import com.github.manolo8.darkbot.core.itf.Updatable;
import com.github.manolo8.darkbot.core.objects.facades.AssemblyMediator;
import com.github.manolo8.darkbot.core.objects.facades.AstralGateProxy;
import com.github.manolo8.darkbot.core.objects.facades.BoosterProxy;
import com.github.manolo8.darkbot.core.objects.facades.ChatProxy;
import com.github.manolo8.darkbot.core.objects.facades.ChrominProxy;
import com.github.manolo8.darkbot.core.objects.facades.DiminishQuestMediator;
import com.github.manolo8.darkbot.core.objects.facades.DispatchGateProxy;
import com.github.manolo8.darkbot.core.objects.facades.DispatchProxy;
import com.github.manolo8.darkbot.core.objects.facades.DispatchRetrieverProxy;
import com.github.manolo8.darkbot.core.objects.facades.EscortProxy;
import com.github.manolo8.darkbot.core.objects.facades.EternalBlacklightProxy;
import com.github.manolo8.darkbot.core.objects.facades.EternalGateProxy;
import com.github.manolo8.darkbot.core.objects.facades.FrozenLabyrinthProxy;
import com.github.manolo8.darkbot.core.objects.facades.GalaxyBuilderProxy;
import com.github.manolo8.darkbot.core.objects.facades.GauntletPlutusProxy;
import com.github.manolo8.darkbot.core.objects.facades.HighlightProxy;
import com.github.manolo8.darkbot.core.objects.facades.InventoryProxy;
import com.github.manolo8.darkbot.core.objects.facades.LogMediator;
import com.github.manolo8.darkbot.core.objects.facades.NpcEventProxy;
import com.github.manolo8.darkbot.core.objects.facades.SeassonPassMediator;
import com.github.manolo8.darkbot.core.objects.facades.QuestProxy;
import com.github.manolo8.darkbot.core.objects.facades.SettingsProxy;
import com.github.manolo8.darkbot.core.objects.facades.ShipWarpProxy;
import com.github.manolo8.darkbot.core.objects.facades.SlotBarsProxy;
import com.github.manolo8.darkbot.core.objects.facades.SpaceMapWindowProxy;
import com.github.manolo8.darkbot.core.objects.facades.StatsProxy;
import com.github.manolo8.darkbot.core.objects.facades.WorldBossOverviewProxy;
import com.github.manolo8.darkbot.core.objects.swf.FlashMap;
import eu.darkbot.api.PluginAPI;
import eu.darkbot.api.managers.NpcEventAPI;

import java.util.HashMap;
import java.util.Map;

import static com.github.manolo8.darkbot.Main.API;

public class FacadeManager implements Manager, eu.darkbot.api.API.Singleton, NpcEventAPI {
    private final FlashMap<String, Updatable> commands = FlashMap.of(String.class, Updatable.class).noAuto();
    private final FlashMap<String, Updatable> proxies = FlashMap.of(String.class, Updatable.class).noAuto();
    private final FlashMap<String, Updatable> mediators = FlashMap.of(String.class, Updatable.class).noAuto();

    private final PluginAPI pluginAPI;

    public final LogMediator log;
    public final ChatProxy chat;
    public final StatsProxy stats;
    public final EscortProxy escort;
    public final BoosterProxy booster;
    public final SettingsProxy settings;
    public final SlotBarsProxy slotBars;
    public final FrozenLabyrinthProxy labyrinth;
    public final EternalGateProxy eternalGate;
    public final EternalBlacklightProxy blacklightGate;
    public final ChrominProxy chrominEvent;
    public final AstralGateProxy astralGate;
    public final HighlightProxy highlight;
    public final SpaceMapWindowProxy spaceMapWindowProxy;
    public final GauntletPlutusProxy plutus;
    public final NpcEventProxy npcEventProxy;
    public final WorldBossOverviewProxy worldBossOverview;
    public final Updatable group;
    public final Updatable groupMediator;
    public final ShipWarpProxy shipWarpProxy;

    private final Map<EventType, NpcEventProxy> npcEvents = new HashMap<>();

    public FacadeManager(PluginAPI pluginApi) {
        this.pluginAPI = pluginApi;

        this.log            = registerMediator("LogWindowMediator",   LogMediator.class);
        this.chat           = registerProxy("ChatProxy",              ChatProxy.class);
        this.stats          = registerProxy("StatsProxy",             StatsProxy.class);
        this.escort         = registerProxy("payload_escort",         EscortProxy.class);
        this.booster        = registerProxy("BoosterProxy",           BoosterProxy.class);
        this.settings       = registerProxy("SettingsWindowFUIProxy", SettingsProxy.class);
        this.slotBars       = registerProxy("ItemsControlMenuProxy",  SlotBarsProxy.class);
        this.labyrinth      = registerProxy("frozen_labyrinth",       FrozenLabyrinthProxy.class);
        this.eternalGate    = registerProxy("eternal_gate",           EternalGateProxy.class);
        this.blacklightGate = registerProxy("eternal_blacklight",     EternalBlacklightProxy.class);
        this.chrominEvent   = registerProxy("chrominEvent",           ChrominProxy.class);
        this.astralGate     = registerProxy("rogue_lite",             AstralGateProxy.class);
        this.highlight      = registerProxy("HighlightProxy",         HighlightProxy.class);
        this.spaceMapWindowProxy = registerProxy("spacemap",          SpaceMapWindowProxy.class);
        this.plutus         = registerProxy("plutus",                 GauntletPlutusProxy.class);
        this.worldBossOverview = registerProxy("worldBoss_overview",  WorldBossOverviewProxy.class);
        this.group          = registerProxy("GroupProxy",             Updatable.NoOp.class);
        this.groupMediator  = registerMediator("GroupSystemMediator", Updatable.NoOp.class);
        this.shipWarpProxy  = registerProxy("ship_warp",              ShipWarpProxy.class);

        registerProxy("dispatch", DispatchProxy.class);
        registerProxy("dispatch_retriever", DispatchRetrieverProxy.class);
        registerProxy("dispatch_gate", DispatchGateProxy.class);
        registerProxy("ggBuilder", GalaxyBuilderProxy.class);
        registerMediator("AssemblyWindowMediator", AssemblyMediator.class);
        registerProxy("InventoryProxy", InventoryProxy.class);
        registerProxy("QuestProxy", QuestProxy.class);
        registerMediator("diminish_quests", DiminishQuestMediator.class);
        registerMediator("seasonPass", SeassonPassMediator.class);

        npcEvents.put(EventType.GENERIC, this.npcEventProxy = registerProxy("npc_event", NpcEventProxy.class));
        npcEvents.put(EventType.AGATUS, registerProxy("agatus_event", NpcEventProxy.class));
    }

    @Override
    public NpcEvent getEvent(EventType eventType) {
        return npcEvents.get(eventType);
    }

    private <T extends Updatable> T registerCommand(String key, Class<T> commandClass) {
        return this.commands.putUpdatable(key, pluginAPI.requireInstance(commandClass));
    }

    private <T extends Updatable> T registerProxy(String key, Class<T> proxyClass) {
        return this.proxies.putUpdatable(key, pluginAPI.requireInstance(proxyClass));
    }

    private <T extends Updatable> T registerMediator(String key, Class<T> mediatorClass) {
        return this.mediators.putUpdatable(key, pluginAPI.requireInstance(mediatorClass));
    }

    @Override
    public void install(BotInstaller botInstaller) {
        botInstaller.mainAddress.add(mainAddr -> {
            long facade = API.readLong(mainAddr + 544);

            commands.update(API.readLong(facade, 0x28, 0x20));
            proxies.update(API.readLong(facade, 0x38, 0x30));
            mediators.update(API.readLong(facade, 0x40, 0x38));
        });
    }

    public void tick() {
        // Currently commands are not used by the bot and they represent
        // a decently big cpu chunk in ticking. Leaving them out reduces tick time significantly.
        //commands.update();

        proxies.update();
        mediators.update();
    }
}

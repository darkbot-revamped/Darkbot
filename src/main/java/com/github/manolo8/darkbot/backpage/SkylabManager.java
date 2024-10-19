package com.github.manolo8.darkbot.backpage;

import com.github.manolo8.darkbot.utils.Time;
import com.google.gson.Gson;
import eu.darkbot.api.API;
import eu.darkbot.api.managers.BackpageAPI;
import eu.darkbot.api.managers.OreAPI;
import eu.darkbot.util.http.Http;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import us.codecraft.xsoup.XPathEvaluator;
import us.codecraft.xsoup.Xsoup;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static eu.darkbot.api.managers.OreAPI.Ore.DURANIUM;
import static eu.darkbot.api.managers.OreAPI.Ore.ENDURIUM;
import static eu.darkbot.api.managers.OreAPI.Ore.PROMERIUM;
import static eu.darkbot.api.managers.OreAPI.Ore.PROMETID;
import static eu.darkbot.api.managers.OreAPI.Ore.PROMETIUM;
import static eu.darkbot.api.managers.OreAPI.Ore.SEPROM;
import static eu.darkbot.api.managers.OreAPI.Ore.TERBIUM;
import static eu.darkbot.api.managers.OreAPI.Ore.XENOMIT;

public class SkylabManager implements API.Singleton {
    public static final List<OreAPI.Ore> SKYLAB_ORES = List.of(PROMETIUM, ENDURIUM, TERBIUM, PROMETID, DURANIUM, XENOMIT, PROMERIUM, SEPROM);
    public final Collector PROMETIUM_COLLECTOR = new Collector(PROMETIUM);
    public final Collector ENDURIUM_COLLECTOR = new Collector(ENDURIUM);
    public final Collector TERBIUM_COLLECTOR = new Collector(TERBIUM);
    public final Refinery PROMETID_REFINERY = new Refinery(PROMETID, PROMETIUM, ENDURIUM);
    public final Refinery DURANIUM_REFINERY = new Refinery(DURANIUM, ENDURIUM, TERBIUM);
    public final Refinery PROMERIUM_REFINERY = new Refinery(PROMERIUM, PROMETID, DURANIUM);
    public final Refinery SEPROM_REFINERY = new Refinery(SEPROM, PROMERIUM);
    public final AbstractModule XENO_MODULE = new XenoModule();
    public final SolarModule SOLAR_MODULE = new SolarModule();
    public final TransportModule TRANSPORT_MODULE = new TransportModule();
    public final AbstractModule STORAGE_MODULE = new StorageModule();
    public final AbstractModule BASE_MODULE = new BaseModule();
    public final List<AbstractModule> SKYLAB_MODULES = List.of(PROMETIUM_COLLECTOR, ENDURIUM_COLLECTOR, TERBIUM_COLLECTOR, PROMETID_REFINERY, DURANIUM_REFINERY, PROMERIUM_REFINERY, SEPROM_REFINERY, XENO_MODULE, SOLAR_MODULE, TRANSPORT_MODULE, STORAGE_MODULE, BASE_MODULE);
    public long lastRefresh = -1;

    private static final Pattern CURRENT_TIME_PATTERN = Pattern.compile("currentTime = (\\d+)");
    private static final Pattern RELOAD_TOKEN_PATTERN = Pattern.compile("var rid = '([a-z0-9]+)'");
    private static final Pattern USER_CREDITS_PATTERN = Pattern.compile("var userCredits = '([.\\d]+)'");
    private static final Pattern USER_URIDIUM_PATTERN = Pattern.compile("var userUridium = '([.\\d]+)'");
    private static final Pattern TIMER_PATTERN = Pattern.compile("tmp\\.init\\(\\s*'\\w*',\\s*(\\d+),\\s*(\\d+)\\s*\\);");

    private final BackpageAPI backpageAPI;
    private final Map<OreAPI.Ore, Integer> skylabResources = new HashMap<>();
    private final Map<String, XPathEvaluator> xPathCache = new HashMap<>();
    private long currentTime = -1, userCredits = -1, userUridium = -1;
    private String token;

    public SkylabManager(BackpageAPI backpageAPI) {
        this.backpageAPI = backpageAPI;
        SKYLAB_ORES.forEach(ore -> skylabResources.put(ore, 0));
    }

    public void refresh() {
        skylabCheck("");
    }

    public boolean refreshRequired() {
        if (lastRefresh + Time.MINUTE > System.currentTimeMillis()) return false;
        if (lastRefresh + 15 * Time.MINUTE < System.currentTimeMillis()) return true;
        return SKYLAB_MODULES.stream().anyMatch(AbstractModule::isTimerLeft);
    }

    public Map<OreAPI.Ore, Integer> getSkylabResources() {
        return Collections.unmodifiableMap(skylabResources);
    }

    private void skylabCheck(String getParams) {
        processRequest(backpageAPI.getHttp("indexInternal.es?action=internalSkylab"+getParams, 5000));
    }

    private void processRequest(Http http) {
        try {
            parseDocument(http.consumeInputStream(inputStream -> Jsoup.parse(inputStream, "UTF-8", http.getUrl().toURI().toString())));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void parseDocument(Document doc) {
        lastRefresh = System.currentTimeMillis();
        Matcher matcher;
        for (String script : xPath("//div[@class=realContainer]/script[@language='javascript']/html()").evaluate(doc).list()) {
            matcher = CURRENT_TIME_PATTERN.matcher(script);
            if (matcher.find()) currentTime = Long.parseLong(matcher.group(1).replaceAll("\\D", "")) * Time.SECOND;
            matcher = RELOAD_TOKEN_PATTERN.matcher(script);
            if (matcher.find()) token = matcher.group(1);
            matcher = USER_CREDITS_PATTERN.matcher(script);
            if (matcher.find()) userCredits = Long.parseLong(matcher.group(1).replaceAll("\\D", ""));
            matcher = USER_URIDIUM_PATTERN.matcher(script);
            if (matcher.find()) userUridium = Long.parseLong(matcher.group(1).replaceAll("\\D", ""));
        }

        Element skylab = xPath("//[@id=skylab]").evaluate(doc).getElements().first();
        skylabResources.putAll(SKYLAB_ORES.stream()
                .collect(Collectors.toMap(it -> it, it-> parseInt(xPath("/[@id*='"+it.getName()+"']/div/text()").evaluate(skylab).get()))));

        parseModule(BASE_MODULE, skylab);
        parseModule(SOLAR_MODULE, skylab);
        parseModule(TRANSPORT_MODULE, skylab);
        parseModule(STORAGE_MODULE, skylab);
        parseModule(PROMETIUM_COLLECTOR, skylab);
        parseModule(ENDURIUM_COLLECTOR, skylab);
        parseModule(TERBIUM_COLLECTOR, skylab);
        parseModule(PROMETID_REFINERY, skylab);
        parseModule(DURANIUM_REFINERY, skylab);
        parseModule(PROMERIUM_REFINERY, skylab);
        parseModule(SEPROM_REFINERY, skylab);
        parseModule(XENO_MODULE, skylab);
    }

    private void parseModule(AbstractModule module, Element container) {
        Elements elements = xPath("/[@id=modules]/[@id=module_"+module.getContainerKey()+"_large]").evaluate(container).getElements();
        Element currentModule = elements.first();
        Element footer = xPath("//[@class=skylab_module_footer]").evaluate(currentModule).getElements().first();

        if (module instanceof Refinery) {
            Refinery refinery = (Refinery) module;
            refinery.efficiency = parseInt(xPath("//[@class=module_info_efficiency]/text()").evaluate(footer).get());
            refinery.countProduce = parseInt(xPath("//[@id*=content]/[@id*=overview_large]//[@class=ore_"+refinery.oreProduce.getName()+"]/strong/text()")
                    .evaluate(currentModule).get().replaceAll("\\D", ""));
            refinery.oreConsume.keySet().forEach(ore -> refinery.oreConsume.put(ore, parseInt(xPath("//[@id*=content]/[@id*=overview_large]//[@class=ore_"+ore.getName()+"]/strong/text()")
                    .evaluate(currentModule).get().replaceAll("\\D", ""))));
        }

        if (module instanceof Collector) {
            Collector collector = (Collector) module;
            collector.countProduce = parseInt(xPath("//[@id*=content]//[@class*=collector_info]/text()").evaluate(currentModule).get().split("-")[1]);
            collector.activeRobots = parseInt(xPath("//[@id=productivity_container]//[@id*=skylabActiveRobots]/text()").evaluate(currentModule).get());
            collector.pendingRobots = parseInt(xPath("//[@id=productivity_container]//[@id*=skylabPendingRobots]/text()").evaluate(currentModule).get());
            collector.robotBonus = parseInt(xPath("//[@id=productivity_container]//[@id*=skylabRobotBonus]/text()").evaluate(currentModule).get());
        }

        if (module instanceof SolarModule) {
            SolarModule solar = (SolarModule) module;
            solar.producingEnergy = parseInt(xPath("//[@class='power skylab_font_power']/text()").evaluate(elements.prev().first()).get().split("/")[1]);
        }

        module.level = parseInt(xPath("//[@class*=level_icon]/text()").evaluate(footer).get());
        module.powerUsage = parseInt(xPath("//[@class=module_info_power_usage]/text()").evaluate(footer).get());
        module.hasMaxLevel = xPath("//[@class=upgrade_container_max]/text()").evaluate(currentModule).get() != null;
        String state = xPath("//[@class*=module_info_active_state]/img/@src").evaluate(footer).get();
        switch (state.substring(state.lastIndexOf('/')+7, state.indexOf('?')-4)) {
            case "on":
                module.isActive = true;
                module.isToggleStateAvailable = true;
                break;
            case "off":
                module.isActive = false;
                module.isToggleStateAvailable = true;
                break;
            case "disabled":
                module.isToggleStateAvailable = false;
                module.isActive = true;
                break;
        }
        String timerConfig = xPath("//script[@language='javascript']/html()").evaluate(currentModule).get();
        timerConfig = timerConfig != null ? timerConfig.replaceAll("\\s+|\\n", " ") : "";
        Matcher matcher = TIMER_PATTERN.matcher(timerConfig);
        if (matcher.find() && matcher.group().contains(module.getContainerKey())) {
            module.timerStartAt = System.currentTimeMillis() + (Long.parseLong(matcher.group(1)) * Time.SECOND - currentTime);
            module.timerEndAt = System.currentTimeMillis() + (Long.parseLong(matcher.group(2)) * Time.SECOND - currentTime);
            module.hasTimer = true;
        } else {
            module.timerStartAt = -1;
            module.timerEndAt = -1;
            module.hasTimer = false;
        }
    }

    private int parseInt(Object str) {
        if(str == null) return -1;
        return Integer.parseInt(str.toString().replaceAll("\\D", ""));
    }

    private XPathEvaluator xPath(String xPath) {
        return xPathCache.computeIfAbsent(xPath, Xsoup::compile);
    }
    public abstract class AbstractModule {

        protected int level;
        protected boolean isActive;
        protected int powerUsage;
        protected boolean isToggleStateAvailable;
        protected boolean hasTimer;
        protected boolean hasMaxLevel;
        protected long timerStartAt;

        protected long timerEndAt;

        protected final void runSubAction(String subAction) {
            skylabCheck("&subaction=" + subAction + "&construction=" + getContainerKey() + "&reloadToken=" + token);
        }

        protected abstract String getContainerKey();

        public final void upgrade() {
            if(!hasMaxLevel && !hasTimer) runSubAction("upgrade");
        }

        public final void instantUpgrade() {
            if(!hasMaxLevel && !hasTimer) runSubAction("instantUpgrade");
        }

        public final void instantFinishUpgrade() {
            if(!hasMaxLevel && hasTimer) runSubAction("instantFinishUpgrade");
        }

        public final void abortUpgrade() {
            if(!hasMaxLevel && hasTimer) runSubAction("abortUpgrade");
        }

        public final void toggleState() {
            if (isToggleStateAvailable) runSubAction(isActive ? "setInactive" : "setActive");
        }

        public final boolean isActive() {
            return isActive;
        }

        public final boolean isTimerLeft() {
            return hasTimer && timerEndAt + Time.MINUTE < System.currentTimeMillis();
        }

        public final boolean hasTimer() {
            return hasTimer;
        }

        public final boolean hasMaxLevel() {
            return hasMaxLevel;
        }
        public final int getPowerUsage() {
            return powerUsage;
        }

    }
    public class Collector extends AbstractModule {
        public final OreAPI.Ore oreProduce;
        private int countProduce;
        private int activeRobots;
        private int pendingRobots;
        private int robotBonus;
        private Collector(OreAPI.Ore produce) {
            this.oreProduce = produce;
        }

        protected String getContainerKey() {
            return oreProduce.getName() + getClass().getSimpleName();
        }

        public void buyRobot(boolean isAdvanced) {
            try {
                String json = backpageAPI.postHttp("ajax/skylab.php", 5000)
                        .setRawParam("command", "buySkylabRobot")
                        .setRawParam("type", isAdvanced ? "2" : "1")
                        .setRawParam("construction", getContainerKey())
                        .getContent();
                try {
                    Map robots = (Map) new Gson().fromJson(json, Map.class).get("robots");
                    activeRobots = parseInt(robots.get("activeRobots"));
                    pendingRobots = parseInt(robots.get("pendingRobots"));
                    robotBonus = parseInt(robots.get("robotBonus"));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        public int getCountProduce() {
            return countProduce;
        }

        public int getPendingRobots() {
            return pendingRobots;
        }

        public int getRobotBonus() {
            return robotBonus;
        }
        public int getActiveRobots() {
            return activeRobots;
        }

    }
    public class Refinery extends AbstractModule {
        public final OreAPI.Ore oreProduce;
        private int countProduce;
        private int efficiency;
        private final Map<OreAPI.Ore, Integer> oreConsume = new HashMap<>();

        private Refinery(OreAPI.Ore produce, OreAPI.Ore... consume) {
            this.oreProduce = produce;
            Arrays.stream(consume).forEach(it-> oreConsume.put(it, 0));
        }
        @Override
        protected String getContainerKey() {
            return oreProduce.getName()+getClass().getSimpleName();
        }

    }
    public class XenoModule extends AbstractModule {
        @Override
        protected String getContainerKey() {
            return "xenoModule";
        }

    }
    public class SolarModule extends AbstractModule {

        private int producingEnergy;

        protected String getContainerKey() {
            return "solarModule";
        }
        public int getProducingEnergy() {
            return producingEnergy;
        }

    }
    public class TransportModule extends AbstractModule {
        protected String getContainerKey() {
            return "transportModule";
        }

        public void sendCargo(Map<OreAPI.Ore, Integer> payload, boolean isInstant) {
            if(token == null) return;
            Http http = backpageAPI.postHttp("indexInternal.es", 5000);
            http.setRawParam("reloadToken", token)
                    .setRawParam("action", "internalSkylab")
                    .setRawParam("subaction", "startTransport")
                    .setRawParam("mode", isInstant ? (hasTimer ? "instantFinish" : "fast") : "normal")
                    .setRawParam("construction", "TRANSPORT_MODULE");
            if(!hasTimer)
                SKYLAB_ORES.forEach(it -> http.setRawParam("count_" + it.getName(), payload.getOrDefault(it, 0)));
            processRequest(http);
        }
        public boolean isBusy() {
            return hasTimer && timerEndAt > System.currentTimeMillis();
        }

    }
    public class StorageModule extends AbstractModule {
        protected String getContainerKey() {
            return "storageModule";
        }

    }
    public class BaseModule extends AbstractModule {
        protected String getContainerKey() {
            return "baseModule";
        }

    }
}

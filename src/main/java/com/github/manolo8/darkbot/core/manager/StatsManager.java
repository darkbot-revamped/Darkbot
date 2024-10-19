package com.github.manolo8.darkbot.core.manager;

import com.github.manolo8.darkbot.Main;
import com.github.manolo8.darkbot.core.BotInstaller;
import com.github.manolo8.darkbot.core.itf.Manager;
import com.github.manolo8.darkbot.core.itf.NativeUpdatable;
import com.github.manolo8.darkbot.core.utils.TimeSeriesImpl;
import com.github.manolo8.darkbot.gui.utils.Strings;
import com.github.manolo8.darkbot.modules.DisconnectModule;
import com.github.manolo8.darkbot.utils.I18n;
import eu.darkbot.api.game.stats.Stats;
import eu.darkbot.api.managers.EventBrokerAPI;
import eu.darkbot.api.managers.StatsAPI;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.DoubleConsumer;
import java.util.function.Supplier;

import static com.github.manolo8.darkbot.Main.API;

public class StatsManager implements Manager, StatsAPI, NativeUpdatable {

    private final Main main;
    private final EventBrokerAPI eventBroker;

    private long address;

    public int userId;
    public volatile String sid;
    public volatile String instance;

    private final Map<StatKey, StatImpl> statistics = new HashMap<>();

    private final StatImpl runtime;
    private final StatImpl credits, uridium, experience, honor, cargo, maxCargo, novaEnergy, teleportBonus;
    private final AverageStats cpuStat, pingStat, tickStat, memoryStat;

    @Getter
    private boolean premium;

    public StatsManager(Main main, EventBrokerAPI eventBroker) {
        this.main = main;
        this.eventBroker = eventBroker;

        registerImpl(Stats.Bot.RUNTIME, runtime = createStat());

        registerImpl(Stats.General.CREDITS, credits = createStat());
        registerImpl(Stats.General.URIDIUM, uridium = createStat());
        registerImpl(Stats.General.EXPERIENCE, experience = createStat());
        registerImpl(Stats.General.HONOR, honor = createStat());
        registerImpl(Stats.General.CARGO, cargo = createStat());
        registerImpl(Stats.General.MAX_CARGO, maxCargo = createStat());
        registerImpl(Stats.General.NOVA_ENERGY, novaEnergy = createStat());
        registerImpl(Stats.General.TELEPORT_BONUS_AMOUNT, teleportBonus = createStat());

        registerImpl(Stats.Bot.PING, pingStat = new AverageStats());
        registerImpl(Stats.Bot.TICK_TIME, tickStat = new AverageStats());
        registerImpl(Stats.Bot.MEMORY, memoryStat = new AverageStats());
        registerImpl(Stats.Bot.CPU, cpuStat = new AverageStats());

        for (Stats.BootyKey key : Stats.BootyKey.values()) {
            registerImpl(key, createStat());
        }
    }

    @Override
    public void install(BotInstaller botInstaller) {
        botInstaller.invalid.add(value -> {
            address = 0;
            userId = 0;
        });
        botInstaller.heroInfoAddress.add(value -> address = value);
    }

    public void tick() {
        updateNonZero(runtime, System.currentTimeMillis());

        if (address == 0) return;

        updateNonZero(credits, readDouble(0x168));
        updateNonZero(uridium, readDouble(0x170));
        updateNonZero(experience, readDouble(0x180));
        checkHonor(updateNonZero(honor, readDouble(0x188)));

        cargo.track(readIntHolder(0x138));
        maxCargo.track(readIntHolder(0x140));

        sid = readString(0xd0);
        userId = readInt(0x30);
        if (main.settingsManager.getAddress() != 0) {
            instance = main.settingsManager.readString(0x298);
        }

        novaEnergy.track(readInt(0x108, 0x28));
        teleportBonus.track(readInt(0x50));
        premium = readBoolean(0xF0, 0x20);

        for (BootyKeyType key: BootyKeyType.VALUES)
            track(key.getStatKey(), readInt(key.getOffset()));
    }

    @Override
    public Stat getStat(Key key) {
        return statistics.get(StatKey.of(key));
    }

    @Override
    public Stat registerStat(Key key) {
        if (key.namespace() == null) throw new UnsupportedOperationException();
        StatKey statKey = StatKey.of(key);
        return statistics.computeIfAbsent(statKey, k -> createStat());
    }

    private void registerImpl(Key key, StatImpl stat) {
        statistics.put(StatKey.of(key), stat);
    }

    public static Collection<? extends StatsAPI.Key> getStatKeys() {
        return Collections.unmodifiableSet(Main.INSTANCE.statsManager.statistics.keySet());
    }

    @Override
    public void setStatValue(Key key, double v) {
        if (key.namespace() == null) throw new UnsupportedOperationException();
        track(StatKey.of(key), v);
    }

    private void track(StatKey key, double v) {
        StatImpl stat = statistics.get(key);
        if (stat != null) stat.track(v);
    }

    private double updateNonZero(StatImpl stat, double value) {
        if (value == 0) return 0;
        return stat.track(value);
    }

    public void tickAverageStats(double tickTime) {
        int p = getPing();
        if (p > 0) pingStat.track(p);

        cpuStat.track(API.getCpuUsage());
        tickStat.track(tickTime);
        memoryStat.track(API.getMemoryUsage());
    }

    private void checkHonor(double honorDiff) {
        if (honorDiff > -10_000) return;

        System.out.println("Paused bot, lost " + honorDiff + " honor.");
        double friendlies = Math.log(Math.abs(honorDiff) / 100) / Math.log(2);
        boolean isExact = Math.abs(friendlies - Math.round(friendlies)) < 0.01;
        System.out.println("Look like " + friendlies + " friendly kills, credible & pausing: " + isExact);

        if (!main.config.MISCELLANEOUS.HONOR_LOST_EXACT || isExact)
            main.setModule(new DisconnectModule(null, I18n.get("module.disconnect.reason.honor")));
    }

    private boolean updateStats() {
        return main.isRunning() || main.config.MISCELLANEOUS.UPDATE_STATS_WHILE_PAUSED;
    }

    public void resetValues() {
        statistics.values().forEach(StatImpl::reset);
        eventBroker.sendEvent(new StatsResetEvent());
    }

    @Override
    public int getPing() {
        return main.pingManager.ping;
    }

    @Override
    public Duration getRunningTime() {
        return Duration.ofMillis((long) runtime.getEarned());
    }

    @Override
    public void resetStats() {
        resetValues();
    }

    public AverageStats getCpuStats() {
        return cpuStat;
    }

    public AverageStats getPingStats() {
        return pingStat;
    }

    public AverageStats getTickStats() {
        return tickStat;
    }

    public AverageStats getMemoryStats() {
        return memoryStat;
    }

    @Override
    public long getAddress() {
        return address;
    }

    @Value
    @Accessors(fluent = true)
    private static class StatKey implements StatsAPI.Key {
        String namespace;
        String category;
        String name;

        public static StatKey of(StatsAPI.Key key) {
            return new StatKey(key.namespace(), key.category(), key.name());
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof StatsAPI.Key)) return false;
            StatsAPI.Key other = (StatsAPI.Key) o;
            return Objects.equals(namespace, other.namespace())
                    && Objects.equals(category, other.category())
                    && Objects.equals(name, other.name());
        }

        @Override
        public int hashCode() {
            int result = Objects.hashCode(namespace);
            result = 31 * result + Objects.hashCode(category);
            result = 31 * result + Objects.hashCode(name);
            return result;
        }
    }

    private StatImpl createStat() {
        return new StatImpl(this::updateStats);
    }

    @Getter
    @RequiredArgsConstructor
    private static class StatImpl implements Stat {
        private final Supplier<Boolean> trackDiff;
        protected double initial = Double.NaN;
        protected double earned, spent;
        protected double current;
        protected TimeSeriesImpl timeSeries = new TimeSeriesImpl();

        protected double track(double value) {
            double diff = value - this.current;

            if (Double.isNaN(initial)) {
                initial = value;
            } else if (trackDiff.get()) {
                if (diff > 0) earned += diff;
                else spent -= diff;
            }
            current = value;
            timeSeries.track(earned - spent);

            return diff;
        }

        private void reset() {
            earned = spent = 0;
        }

        @Override
        public @Nullable TimeSeries getTimeSeries() {
            return timeSeries;
        }
    }

    public static class AverageStats extends StatImpl {

        @Getter
        private double average, max = Double.MIN_VALUE;
        private long lastTime = System.currentTimeMillis();

        private DoubleConsumer onChange;

        public AverageStats() {
            super(() -> true);
        }

        protected double track(double value) {
            long now = System.currentTimeMillis();
            double diff = super.track(value);

            double adjustFactor = (now - lastTime) / 10_000d;
            average += adjustFactor * (value - average); // 1s of data would be 1/10th of the avg
            max += adjustFactor * 0.2 * (average - max); // 1s of data would be 1/50th of adjustment towards avg
            max = Math.max(max, value); // If this IS a max, keep it

            if (diff != 0 && onChange != null) {
                onChange.accept(value);
            }
            lastTime = now;
            return diff;
        }

        public void setListener(DoubleConsumer onChange) {
            this.onChange = onChange;
        }

        @Override
        public String toString() {
            return "Max=" + Strings.ONE_PLACE_FORMAT.format(getMax()) +
                    "\nAverage=" + Strings.ONE_PLACE_FORMAT.format(getAverage());
        }
    }

    @Getter
    private enum BootyKeyType {
        GREEN(0x54, Stats.BootyKey.GREEN),
        BLUE(0x58, Stats.BootyKey.BLUE),
        RED(0x5c, Stats.BootyKey.RED),
        SILVER(0x60, Stats.BootyKey.SILVER),
        APOCALYPSE(0x64, Stats.BootyKey.APOCALYPSE),
        PROMETHEUS(0x68, Stats.BootyKey.PROMETHEUS),
        OBSIDIAN_MICROCHIP(0x6c, Stats.BootyKey.OBSIDIAN_MICROCHIP),
        BLACK_LIGHT_CODE(0x70, Stats.BootyKey.BLACK_LIGHT_CODE),
        BLACK_LIGHT_DECODER(0x74, Stats.BootyKey.BLACK_LIGHT_DECODER),
        PROSPEROUS_FRAGMENT(0x78, Stats.BootyKey.PROSPEROUS_FRAGMENT),
        ASTRAL(0x7c, Stats.BootyKey.ASTRAL),
        ASTRAL_SUPREME(0x80, Stats.BootyKey.ASTRAL_SUPREME),
        EMPYRIAN(0x84, Stats.BootyKey.EMPYRIAN),
        LUCENT(0x88, Stats.BootyKey.LUCENT),
        PERSEUS(0x8c, Stats.BootyKey.PERSEUS);

        private static final BootyKeyType[] VALUES = values();

        private final int offset;
        private final StatKey statKey;

        BootyKeyType(int offset, StatsAPI.Key key) {
            this.offset = offset;
            this.statKey = StatKey.of(key);
        }

    }
}

package com.github.manolo8.darkbot.config;

import com.github.manolo8.darkbot.Main;
import eu.darkbot.api.config.annotations.Configuration;
import eu.darkbot.api.config.annotations.Option;
import eu.darkbot.api.config.types.NpcFlag;
import eu.darkbot.api.game.items.ItemCategory;
import eu.darkbot.api.game.items.SelectableItem;
import eu.darkbot.api.managers.HeroItemsAPI;
import eu.darkbot.util.StringUtils;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Configuration("config.loot.npc_table")
public class NpcInfo implements eu.darkbot.api.config.types.NpcInfo {

    public double radius;
    public int priority;
    public boolean kill;
    @Option("config.loot.npc_table.attack_key")
    public Character attackKey;
    @Option("config.loot.npc_table.attack_formation")
    public SelectableItem.Formation attackFormation = SelectableItem.Formation.STANDARD;
    public ExtraNpcInfo extra = new ExtraNpcInfo();

    public transient String name;
    public transient String fuzzyName;
    public transient int npcId;
    public @Option.Ignore Set<Integer> mapList = new HashSet<>();

    private static transient final Map<String, NpcExtraFlag> LEGACY_FLAGS = new LinkedHashMap<>();
    private static transient final Map<String, NpcExtraFlag> NEW_FLAGS = new LinkedHashMap<>();

    public static transient final Map<String, NpcExtraFlag> NPC_FLAGS = new LinkedHashMap<>();

    public static void setNpcFlags(Stream<NpcExtraFlag> flags, boolean legacy) {
        Map<String, NpcExtraFlag> map = legacy ? LEGACY_FLAGS : NEW_FLAGS;
        map.clear();
        flags.forEach(flag -> map.put(flag.getId(), flag));

        NPC_FLAGS.clear();
        NPC_FLAGS.putAll(LEGACY_FLAGS);
        NPC_FLAGS.putAll(NEW_FLAGS);
    }

    public void set(Double radius, Integer priority, Boolean kill, Character attackKey, ExtraNpcInfo extra) {
        set(radius, priority, kill, attackKey, null, extra);
    }

    public void set(Double radius, Integer priority, Boolean kill, Character attackKey, SelectableItem.Formation attackFormation, ExtraNpcInfo extra) {
        if (radius != null) this.radius = radius;
        if (priority != null) this.priority = priority;
        if (kill != null) this.kill = kill;
        if (attackKey != null) this.attackKey = attackKey;
        if (attackFormation != null) this.attackFormation = attackFormation;
        if (extra != null) this.extra = extra;
    }

    public void copyOf(NpcInfo other) {
        this.radius = other.radius;
        this.priority = other.priority;
        this.kill = other.kill;
        this.attackKey = other.attackKey;
        this.attackFormation = other.attackFormation;
        this.extra.flags = new HashSet<>(other.extra.flags);
    }

    @Override
    public boolean getShouldKill() {
        return kill;
    }

    @Override
    public void setShouldKill(boolean kill) {
        this.kill = kill;
    }

    @Override
    public int getPriority() {
        return priority;
    }

    @Override
    public void setPriority(int priority) {
        this.priority = priority;
    }

    @Override
    public double getRadius() {
        return radius;
    }

    @Override
    public void setRadius(double radius) {
        this.radius = radius;
    }

    @Override
    public Optional<SelectableItem.Laser> getAmmo() {
        return getHeroItems().getItem(attackKey, ItemCategory.LASERS, SelectableItem.Laser.class);
    }

    @Override
    public Optional<SelectableItem.Formation> getFormation() {
        return Optional.of(attackFormation);
    }

    private static HeroItemsAPI getHeroItems() {
        return Main.INSTANCE.facadeManager.slotBars;
    }

    @Override
    public boolean hasExtraFlag(Enum<?> flag) {
        return extra.has(getId(flag));
    }

    @Override
    public void setExtraFlag(Enum<?> flag, boolean active) {
        extra.set(getId(flag), active);
    }

    @Override
    public Set<Integer> getMapIds() {
        return Collections.unmodifiableSet(mapList);
    }

    @Override
    public void addMapId(int mapId) {
        mapList.add(mapId);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getFuzzyName() {
        return fuzzyName == null ? fuzzyName = StringUtils.fuzzyNpcName(getName()) : fuzzyName;
    }

    // Keep a cache of the last searched id.
    // If called repeatedly in a loop for the same flag, it avoids allocations for the string concat of class + name.
    private static String lastSearchedId = "";

    public static String getId(Enum<?> flag) {
        String lastId = NpcInfo.lastSearchedId;

        // Legacy backwards compat. When using the new NpcFlag, check for old NpcExtra id
        String className = flag instanceof NpcFlag ? NpcExtra.class.getCanonicalName()
                : flag.getClass().getCanonicalName();
        String name = flag.name();

        if (lastId.length() == (className.length() + name.length()) &&
                lastId.startsWith(className) && lastId.endsWith(name))
            return lastId;

        return lastSearchedId = className.concat(name);
    }

    public static class ExtraNpcInfo {
        private Set<String> flags = new HashSet<>();

        public ExtraNpcInfo() {}

        public ExtraNpcInfo(NpcExtraFlag... flags) {
            for (NpcExtraFlag flag : flags) set(flag, true);
        }

        public boolean has(NpcExtraFlag flag) {
            return has(flag.getId());
        }

        public boolean has(String flagId) {
            return flags.contains(flagId);
        }

        public void set(NpcExtraFlag flag, boolean set) {
            set(flag.getId(), set);
        }

        public void set(String flagId, boolean set) {
            if (set) flags.add(flagId);
            else flags.remove(flagId);
        }

        public void copy(ExtraNpcInfo other) {
            if (this == other) return;
            this.flags.clear();
            this.flags.addAll(other.flags);
        }

        public String toString() {
            if (flags.isEmpty()) return "";

            return NPC_FLAGS.entrySet().stream()
                    .filter(e -> flags.contains(e.getKey()))
                    .map(Map.Entry::getValue)
                    .map(NpcExtraFlag::getShortName)
                    .collect(Collectors.joining(","));
        }
    }

}

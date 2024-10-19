package com.github.manolo8.darkbot.config.actions.values;

import com.github.manolo8.darkbot.Main;
import com.github.manolo8.darkbot.config.actions.Parser;
import com.github.manolo8.darkbot.config.actions.SyntaxException;
import com.github.manolo8.darkbot.config.actions.Value;
import com.github.manolo8.darkbot.config.actions.ValueData;
import com.github.manolo8.darkbot.config.actions.parser.ParseUtil;
import com.github.manolo8.darkbot.config.actions.parser.Values;
import com.github.manolo8.darkbot.core.manager.StatsManager;

import eu.darkbot.api.managers.StatsAPI;
import eu.darkbot.api.managers.StatsAPI.Key;

import org.jetbrains.annotations.Nullable;

import java.util.Locale;

@ValueData(name = "stat-type", description = "Gets a certain Stat type from a bot", example = "stat-type(experience, earned)")
public class StatTypeValue implements Value<Number>, Parser {
    private StatsAPI.Key key;
    private StatsAPI.Stat stat;
    private StatData dataType;

    @Override
    public @Nullable Number get(Main main) {
        if (stat == null && key != null) stat = main.statsManager.getStat(key);
        if (dataType == null || key == null || stat == null) {
            return null;
        }

        switch (dataType) {
            case INITIAL: return stat.getInitial();
            case CURRENT: return stat.getCurrent();
            case EARNED: return stat.getEarned();
            case SPENT: return stat.getSpent();
            case DIFFERENCE: return stat.getEarned() - stat.getSpent();
            default: throw new IllegalStateException("Undefined operation " + dataType);
        }
    }

    private StatsAPI.Key getKeyFromString(String token) {
        String[] tokenParts = token.split(":", 3);

        String statNamespace = tokenParts.length >= 2 ? tokenParts[0] : null;
        String statCategory = tokenParts.length == 3 ? tokenParts[1] : null;
        String statKey = tokenParts[tokenParts.length - 1];

        return StatsManager.getStatKeys().stream()
                .filter(s -> statNamespace == null || s.namespace().equals(statNamespace))
                .filter(s -> statCategory == null || s.category().equals(statCategory))
                .filter(s -> statKey == null || s.name().equals(statKey))
                .findFirst().orElse(null);
    }

    public enum StatData {
        INITIAL,
        CURRENT,
        EARNED,
        SPENT,
        DIFFERENCE;

        @Override
        public String toString() {
            return name().toLowerCase(Locale.ROOT);
        }

        public static StatData of(String sd) {
            for (StatData statData : StatData.values()) {
                if (statData.toString().equalsIgnoreCase(sd)) {
                    return statData;
                }
            }
            return null;
        }
    }

    @Override
    public String toString() {
        return "stat-type(" + key.name().toLowerCase(Locale.ROOT) + "," + dataType + ")";
    }

    private String getKeyFormatted(Key keyToFormat) {
        return (keyToFormat.namespace() != null ? keyToFormat.namespace() + ":" : "") + keyToFormat.category() + ":"
                + keyToFormat.name();
    }

    @Override
    public String parse(String str) throws SyntaxException {
        String[] params = str.split(" *, *", 2);

        key = getKeyFromString(params[0].trim());
        stat = null;

        if (key == null) {
            throw new SyntaxException("Unknown stat-type: '" + params[0] + "'", str, Values.getMeta(getClass()),
                    StatsManager.getStatKeys().stream()
                            .map(this::getKeyFormatted)
                            .toArray(String[]::new));
        }

        params = ParseUtil.separate(params, getClass(), ",").split("\\)", 2);

        dataType = StatData.of(params[0].trim());

        if (dataType == null) {
            throw new SyntaxException("Unknown data-type: '" + params[0] + "'", params[0], StatData.class);
        }

        return ParseUtil.separate(params, getClass(), ")");
    }

}
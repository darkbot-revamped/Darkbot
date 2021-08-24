package com.github.manolo8.darkbot.config.tree;

import com.github.manolo8.darkbot.config.tree.handlers.SettingHandlerFactory;
import com.github.manolo8.darkbot.config.types.Num;
import com.github.manolo8.darkbot.gui.tree.EditorProvider;
import eu.darkbot.api.API;
import eu.darkbot.api.config.annotations.Configuration;
import eu.darkbot.api.config.annotations.Option;
import eu.darkbot.api.extensions.PluginInfo;
import eu.darkbot.api.managers.I18nAPI;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class ConfigBuilder implements API.Singleton {

    private final I18nAPI i18n;
    private final EditorProvider editors;
    private final SettingHandlerFactory settingHandlerFactory;

    public ConfigBuilder(I18nAPI i18n,
                         EditorProvider editors,
                         SettingHandlerFactory settingHandlerFactory) {
        this.i18n = i18n;
        this.editors = editors;
        this.settingHandlerFactory = settingHandlerFactory;
    }

    public <T> ConfigSetting<T> of(Class<T> type, @Nullable PluginInfo namespace) {
        Configuration cfg = type.getAnnotation(Configuration.class);
        String baseKey = "config";
        boolean allOptions = false;

        if (cfg != null) {
            baseKey = cfg.value();
            allOptions = cfg.allOptions();
        }

        return new Builder(namespace, baseKey, allOptions).build(type);
    }

    private class Builder {
        private final PluginInfo namespace;
        private final String baseKey;
        private final boolean allConfig;

        public Builder(PluginInfo namespace,
                       String baseKey,
                       boolean allConfig) {
            this.namespace = namespace;
            this.baseKey = baseKey;
            this.allConfig = allConfig;
        }

        public <T> ConfigSetting.Root<T> build(Class<T> type) {
            return new ConfigSetting.Root<T>(
                    baseKey,
                    i18n.getOrDefault(namespace, baseKey, "Root"),
                    i18n.getOrDefault(namespace, baseKey + ".desc", null),
                    type,
                    parent -> getChildren(parent, type));
        }

        private Map<String, ConfigSetting<?>> getChildren(ConfigSetting.Parent<?> p, Class<?> type) {
            return Arrays.stream(type.getDeclaredFields())
                    .filter(this::participates)
                    .collect(Collectors.toMap(
                            f -> f.getName().toLowerCase(Locale.ROOT),
                            f -> createConfig(p, f)));
        }

        private ConfigSetting<?> createConfig(ConfigSetting.Parent<?> parent, Field field) {
            Class<?> type = field.getType();

            String key = parent.getKey() + "." + field.getName().toLowerCase(Locale.ROOT),
                    name, description;

            com.github.manolo8.darkbot.config.types.Option legacyOption
                    = field.getAnnotation(com.github.manolo8.darkbot.config.types.Option.class);

            if (legacyOption != null) {
                if (!legacyOption.key().isEmpty()) key = legacyOption.key();

                name = i18n.getOrDefault(namespace, key, legacyOption.value());
                description = i18n.getOrDefault(namespace, key + ".desc", legacyOption.description());
            } else {
                Option option = field.getAnnotation(Option.class);
                if (option != null && !option.value().isEmpty()) key = option.value();

                name = i18n.getOrDefault(namespace, key, field.getName());
                description = i18n.getOrDefault(namespace, key + ".desc", null);
            }


            // If we know for sure it is a leaf, we ignore trying to make an intermediate
            // If the intermediate turns out not to have any children, discard it
            // and go back to it being a leaf node
            if (!isLeaf(field)) {
                ConfigSetting.Intermediate<?> inter = new ConfigSetting.Intermediate<>(parent,
                        key, name, description, type, p -> getChildren(p, type));
                if (!inter.getChildren().isEmpty())
                    return inter;
            }

            return new ConfigSetting.Leaf<>(parent, key, name, description, type,
                    settingHandlerFactory.getHandler(field));
        }

        /**
         * @param field the field to check
         * @return true if this field participates in the configuration tree, false otherwise
         */
        private boolean participates(Field field) {
            if (!field.isAccessible()) return false;
            if (field.getAnnotation(Option.Ignore.class) != null) return false;
            if (field.getAnnotation(Option.class) != null) return true;
            return allConfig;
        }

        /**
         * @param field the field to check
         * @return true if this is a leaf node, no more children under this, false otherwise
         */
        private boolean isLeaf(Field field) {
            Class<?> type = field.getType();
            return type.isPrimitive() || type.isInterface() || settingHandlerFactory.hasHandler(type);
        }

    }

}

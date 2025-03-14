package com.github.manolo8.darkbot.gui.tree;

import com.github.manolo8.darkbot.config.Config;
import com.github.manolo8.darkbot.config.ImageWrapper;
import com.github.manolo8.darkbot.config.tree.ConfigField;
import com.github.manolo8.darkbot.config.types.Editor;
import com.github.manolo8.darkbot.gui.tree.editors.BooleanEditor;
import com.github.manolo8.darkbot.gui.tree.editors.CharacterEditor;
import com.github.manolo8.darkbot.gui.tree.editors.ColorEditor;
import com.github.manolo8.darkbot.gui.tree.editors.ConditionEditor;
import com.github.manolo8.darkbot.gui.tree.editors.DropdownEditor;
import com.github.manolo8.darkbot.gui.tree.editors.FileEditor;
import com.github.manolo8.darkbot.gui.tree.editors.FontEditor;
import com.github.manolo8.darkbot.gui.tree.editors.ImagePicker;
import com.github.manolo8.darkbot.gui.tree.editors.MultiDropdownEditor;
import com.github.manolo8.darkbot.gui.tree.editors.NewShipModeEditor;
import com.github.manolo8.darkbot.gui.tree.editors.NumberEditor;
import com.github.manolo8.darkbot.gui.tree.editors.PlayerTagEditor;
import com.github.manolo8.darkbot.gui.tree.editors.RangeEditor;
import com.github.manolo8.darkbot.gui.tree.editors.ShipModeEditor;
import com.github.manolo8.darkbot.gui.tree.editors.StringEditor;
import com.github.manolo8.darkbot.gui.tree.editors.TableEditor;
import eu.darkbot.api.PluginAPI;
import eu.darkbot.api.config.ConfigSetting;
import eu.darkbot.api.config.types.Condition;
import eu.darkbot.api.config.types.PercentRange;
import eu.darkbot.api.config.types.PlayerTag;
import eu.darkbot.api.config.types.ShipMode;
import eu.darkbot.api.config.util.OptionEditor;
import eu.darkbot.api.utils.Inject;
import java.awt.Color;
import java.awt.Font;
import java.io.File;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@SuppressWarnings({"unchecked", "rawtypes"})
public class EditorProvider {

    private final PluginAPI api;
    private final LegacyEditorManager legacy;

    // object type class -> option editor class
    private final Map<Class<?>, Class<? extends OptionEditor>> defaultEditors = new HashMap<>();

    // metadata key -> option editor class
    private final Map<String, Class<? extends OptionEditor>> metadataEditors = new LinkedHashMap<>();

    // option editor class -> option editor instance
    private final Map<Class<? extends OptionEditor>, OptionEditor<?>> instances = new HashMap<>();


    @Inject
    public EditorProvider(PluginAPI api,
                          LegacyEditorManager legacy) {
        this.api = api;
        this.legacy = legacy;

        defaultEditors.put(Boolean.class, BooleanEditor.class);
        defaultEditors.put(Integer.class, NumberEditor.class);
        defaultEditors.put(Double.class, NumberEditor.class);
        defaultEditors.put(Character.class, CharacterEditor.class);
        defaultEditors.put(Color.class, ColorEditor.class);
        defaultEditors.put(Font.class, FontEditor.class);
        defaultEditors.put(String.class, StringEditor.class);
        defaultEditors.put(File.class, FileEditor.class);

        defaultEditors.put(Condition.class, ConditionEditor.class);
        defaultEditors.put(com.github.manolo8.darkbot.config.actions.Condition.class, ConditionEditor.class);

        defaultEditors.put(Config.PercentRange.class, RangeEditor.class);
        defaultEditors.put(PercentRange.class, RangeEditor.class);

        defaultEditors.put(PlayerTag.class, PlayerTagEditor.class);
        defaultEditors.put(com.github.manolo8.darkbot.config.PlayerTag.class, PlayerTagEditor.class);

        defaultEditors.put(ShipMode.class, ShipModeEditor.class);
        defaultEditors.put(Config.ShipConfig.class, ShipModeEditor.class);
        defaultEditors.put(ShipMode.ShipModeImpl.class, NewShipModeEditor.class);

        defaultEditors.put(ImageWrapper.class, ImagePicker.class);

        metadataEditors.put("isPercent", NumberEditor.class);
        metadataEditors.put("isTable", TableEditor.class);
        metadataEditors.put("isDropdown", DropdownEditor.class);
        metadataEditors.put("isMultiDropdown", MultiDropdownEditor.class);
    }

    public EditorProvider(EditorProvider shared) {
        this(shared.api, new LegacyEditorManager(shared.legacy));
    }

    public <T> OptionEditor<T> getEditor(ConfigSetting<T> setting) {
        // Legacy handler should take care of this field
        Field field = setting.getMetadata("field");
        if (field != null && field.isAnnotationPresent(Editor.class) &&
                !legacy.isRemoved(field.getAnnotation(Editor.class).value())) return null;

        // Specific editor class requested
        Class<? extends OptionEditor> editor = setting.getMetadata("editor");

        // Metadata based editors
        if (editor == null)
            editor = metadataEditors.entrySet().stream()
                    .filter(e -> setting.getMetadata(e.getKey()) != null)
                    .map(Map.Entry::getValue)
                    .findFirst()
                    .orElse(null);

        // Default editors
        if (editor == null) editor = defaultEditors.get(setting.getType());

        if (editor == null) return null;
        return (OptionEditor<T>) instances.computeIfAbsent(editor, api::requireInstance);
    }

    public com.github.manolo8.darkbot.gui.tree.OptionEditor getLegacyEditor(ConfigField field) {
        return legacy.getEditor(field);
    }


}

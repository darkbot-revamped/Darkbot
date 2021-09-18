package com.github.manolo8.darkbot.config.types.suppliers;

import com.github.manolo8.darkbot.gui.utils.Strings;
import com.github.manolo8.darkbot.utils.I18n;
import eu.darkbot.api.config.annotations.Dropdown;

import java.util.Collection;
import java.util.List;
import java.util.Locale;

public class LanguageSupplier implements Dropdown.Options<Locale> {

    private static final List<Locale> LOCALES = I18n.SUPPORTED_LOCALES;

    @Override
    public String getText(Locale value) {
        return Strings.capitalize(value.getDisplayName());
    }

    @Override
    public Collection<Locale> options() {
        return LOCALES;
    }
}

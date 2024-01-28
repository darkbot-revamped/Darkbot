package com.github.manolo8.darkbot.core.objects.facades;

import com.github.manolo8.darkbot.Main;
import com.github.manolo8.darkbot.core.itf.Updatable;
import com.github.manolo8.darkbot.core.objects.swf.FlashMap;
import com.github.manolo8.darkbot.core.utils.ByteUtils;
import eu.darkbot.api.API;

public class HighlightProxy extends Updatable implements API.Singleton {

    private final FlashMap<String, Long> proxyDictionary = FlashMap.of(String.class, Long.class);
    private final FlashMap<String, Long> highlightItems = FlashMap.of(String.class, Long.class);

    private boolean attacking;

    @Override
    public void update() {
        long data = Main.API.readLong(address + 48) & ByteUtils.ATOM_MASK;
        proxyDictionary.update(data);

        // category bar have only highlighted items from current tab
        this.attacking = checkAttacking("categoryBar") || checkAttacking("standardSlotBar")
                || checkAttacking("premiumSlotBar") || checkAttacking("proActionBar");
    }

    private boolean checkAttacking(String key) {
        long categoryHighlightItem = proxyDictionary.getOrDefault(key, 0L);

        if (ByteUtils.isValidPtr(categoryHighlightItem)) {
            highlightItems.update(categoryHighlightItem);
            for (Long addr : highlightItems.values()) {
                if (Main.API.readInt(addr + 0x38) > 0)
                    return true;
            }
        }
        return false;
    }

    public boolean isAttacking() {
        return attacking;
    }
}
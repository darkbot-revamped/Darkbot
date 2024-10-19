package com.github.manolo8.darkbot.core.objects.facades;

import com.github.manolo8.darkbot.core.itf.Updatable;

import eu.darkbot.api.API;
import lombok.Getter;

import static com.github.manolo8.darkbot.Main.API;

@Getter
public class QuestGiverMediator extends Updatable implements API.Singleton {

    private double positionX;
    private double positionY;
    private boolean focused;

    @Override
    public void update() {
        if (address == 0) {
            return;
        }

        long data = API.readAtom(address + 0x30);

        this.positionX = API.readDouble(data + 0x2F8);
        this.positionY = API.readDouble(data + 0x300);

        this.focused = API.readBoolean(data + 0x49C);
    }

}

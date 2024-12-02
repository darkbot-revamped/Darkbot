package com.github.manolo8.darkbot.core.objects.facades;

import com.github.manolo8.darkbot.core.itf.Updatable;
import com.github.manolo8.darkbot.core.objects.swf.FlashList;
import eu.darkbot.api.API;
import eu.darkbot.api.managers.ShipWarpAPI;
import lombok.Getter;
import lombok.ToString;

@Getter
public class ShipWarpProxy extends Updatable implements API.Singleton {
    private boolean isNearSpaceStation;
    private long changed;
    private final FlashList<Ship> shipsList = FlashList.ofVector(Ship::new);

    @Override
    public void update() {
        isNearSpaceStation = readBoolean(0x30, 0x20);
        changed = readLong(0x28);
        shipsList.update(readAtom(0x30, 0x30));
    }

    @Getter
    @ToString
    public static class Ship extends Auto implements ShipWarpAPI.Ship {
        private int shipId;
        private int warpCost;
        private boolean freeWarp;
        private int favouriteIndex;
        private String shipTypeId;
        private String shipName;

        @Override
        public void update() {
            this.shipId = readInt(0x20);
            this.warpCost = readInt(0x24);
            this.freeWarp = readInt(0x28) == 0;
            this.favouriteIndex = readInt(0x2C);
            this.shipTypeId = readString(0x30);
            this.shipName = readString(0x38);
        }
    }
}

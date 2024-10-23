package com.github.manolo8.darkbot.core.objects.facades;

import com.github.manolo8.darkbot.core.itf.Updatable;
import com.github.manolo8.darkbot.core.objects.swf.FlashList;

import lombok.Getter;
import lombok.ToString;

public class AstralGateSelectionProxy extends Updatable {

    @Getter
    private final FlashList<AstralShip> astralShips = FlashList.ofVector(AstralShip::new);

    @Override
    public void update() {
        if (address == 0) {
            return;
        }

        astralShips.update(readAtom(0x30, 0x48));
    }


    @Getter
    @ToString
    public static class AstralShip extends Auto {
        private int hp;
        private int shield;
        private int speed;
        private boolean selected;
        private String roleType;
        private String shipId;

        @Override
        public void update(long address) {
            super.update(address);
        
            this.hp = readInt(0x20);
            this.shield = readInt(0x24);
            this.speed = readInt(0x2c);
            this.roleType = readString(0x38);

            this.shipId = readString(0x40, 0x48);
        }

        @Override
        public void update() {
            if (address == 0) {
                return;
            }

            this.selected = readBoolean(0x30, 0x20);
        }
    }
}
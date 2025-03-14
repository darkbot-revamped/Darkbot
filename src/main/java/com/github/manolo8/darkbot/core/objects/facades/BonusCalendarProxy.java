package com.github.manolo8.darkbot.core.objects.facades;

import com.github.manolo8.darkbot.core.itf.Updatable;
import com.github.manolo8.darkbot.core.objects.swf.FlashList;
import eu.darkbot.api.managers.BonusCalendarAPI;
import lombok.Getter;
import lombok.ToString;

@Getter
public class BonusCalendarProxy extends Updatable implements BonusCalendarAPI {
    private int daysClaimed;
    private boolean claimable;
    private final FlashList<RewardLoot> rewardList = FlashList.ofVector(RewardLoot::new);

    @Override
    public void update() {
        daysClaimed = readInt(0x30, 0x40);
        rewardList.update(readAtom(0x30, 0x58));
        claimable = readBoolean(0x30, 0x50, 0x20);
    }

    @Getter
    @ToString
    private static class RewardLoot extends Auto implements BonusCalendarAPI.RewardList {
        private String lootId;
        private int amount;

        @Override
        public void update() {
            this.amount = readInt(0x20);
            this.lootId = readString(0x30);
        }
    }
}

package com.github.manolo8.darkbot.core.objects.facades;

import com.github.manolo8.darkbot.core.itf.Updatable;
import com.github.manolo8.darkbot.core.objects.swf.FlashList;
import eu.darkbot.api.API;
import eu.darkbot.api.managers.ReturneeAPI;
import lombok.Getter;
import lombok.ToString;

@Getter
public class ReturneeLoginProxy extends Updatable implements API.Singleton{

    private boolean claimable = false;
    private final FlashList<RewardList> rewardList = FlashList.ofVector(RewardList::new);

    @Override
    public void update() {
        this.claimable = readBoolean(0x30, 0x40, 0x20);
        this.rewardList.update(readAtom(0x30, 0x48));
    }

    @Getter
    @ToString
    private static class RewardList extends Auto implements ReturneeAPI.LoginRewardList {
        private String lootId;
        private double amount;

        @Override
        public void update() {
            this.amount = readDouble(0x70);
            this.lootId = readString(0x48);
        }
    }
}

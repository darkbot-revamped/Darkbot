package com.github.manolo8.darkbot.core.objects.facades;

import com.github.manolo8.darkbot.core.itf.Updatable;
import com.github.manolo8.darkbot.core.objects.swf.FlashList;
import eu.darkbot.api.API;
import eu.darkbot.api.managers.ReturneeAPI;
import lombok.Getter;
import lombok.ToString;

@Getter
public class ReturneeCalendarProxy extends Updatable implements API.Singleton {
    private int daysClaimed = -1;
    private boolean claimable = false;
    private final FlashList<CalendarList> calendarList = FlashList.ofVector(CalendarList::new);

    @Override
    public void update() {
        this.daysClaimed = readInt(0x30, 0x40);
        this.claimable = readBoolean(0x30, 0x50, 0x20);
        this.calendarList.update(readAtom(0x30, 0x58));
    }

    @Getter
    @ToString
    private static class CalendarList extends Auto implements ReturneeAPI.CalendarRewardList {
        private String lootId;
        private int amount;
        private boolean claimed;

        @Override
        public void update() {
            this.amount = readInt(0x20);
            this.lootId = readString(0x30);
            this.claimed = readBoolean(0x2C);
        }

        @Override
        public boolean getClaimed() {
            return claimed;
        }
    }
}

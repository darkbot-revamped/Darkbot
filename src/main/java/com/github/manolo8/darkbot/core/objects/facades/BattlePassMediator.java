package com.github.manolo8.darkbot.core.objects.facades;

import com.github.manolo8.darkbot.core.itf.Updatable;
import com.github.manolo8.darkbot.core.objects.facades.QuestProxy.Quest;
import com.github.manolo8.darkbot.core.objects.swf.FlashList;

import eu.darkbot.api.API;
import eu.darkbot.api.managers.QuestAPI;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.ToString;

import static com.github.manolo8.darkbot.Main.API;

import java.util.List;

import org.jetbrains.annotations.Nullable;

public class BattlePassMediator extends Updatable implements API.Singleton {
    private final FlashList<BattlePassObjetive> allObjetives = FlashList.ofVector(BattlePassObjetive.class);

    @Getter
    private boolean battlePassPurchased = false;

    @Getter
    private boolean hasRewardsToCollect = false;

    @Getter
    private int objectivesCompleted;

    @Getter
    private int totalObjectives;

    private boolean objectivesUpdated = false;
    private long data = 0;

    @Override
    public void update() {
        if (address == 0) {
            return;
        }

        this.objectivesUpdated = false;

        this.data = API.readAtom(address + 0x58);

        this.battlePassPurchased = API.readBoolean(this.data + 0x40);
        this.hasRewardsToCollect = API.readBoolean(this.data + 0x44);
        this.objectivesCompleted = API.readInt(this.data + 0x48);
        this.totalObjectives = API.readInt(this.data + 0x4c);
    }

    public @Nullable List<? extends BattlePassObjetive> getObjetives() {
        if (!objectivesUpdated && data != 0) {
            objectivesUpdated = true;
            this.allObjetives.update(API.readAtom(data + 0x60));
        }

        return this.allObjetives;
    }

    @Getter
    @ToString
    public static class BattlePassObjetive extends Updatable implements API.Singleton {

        private int objetiveId;
        private String title;
        private int status;

        @Getter(AccessLevel.NONE)
        private final QuestProxy.Quest quest = new Quest();

        @Override
        public void update(long address) {
            super.update(address);

            this.objetiveId = readInt(0x24);
            this.title = readString(0x48);

            this.quest.update(readAtom(0x70));
        }

        @Override
        public void update() {
            if (address == 0) {
                return;
            }

            this.status = readInt(0x58, 0x20);

            this.quest.update();
        }

        public @Nullable QuestAPI.Quest getQuest() {
            return this.quest.address == 0 ? null : quest;
        }
    }

}

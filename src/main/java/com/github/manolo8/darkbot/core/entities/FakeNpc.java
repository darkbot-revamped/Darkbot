package com.github.manolo8.darkbot.core.entities;

import com.github.manolo8.darkbot.Main;
import com.github.manolo8.darkbot.config.NpcInfo;
import com.github.manolo8.darkbot.core.entities.fake.FakeEntities;
import com.github.manolo8.darkbot.core.utils.Location;

/**
 * Represents a Pet ping FakeNpc, this is a singleton and any other fake npc should use {@link Npc.Fake} instead
 */
public class FakeNpc extends Npc {
    private long pingAlive = 0;

    public FakeNpc(Main main) {
        super(FakeEntities.allocateFakeId());
        super.removed = true;
        super.address = -1;
        super.main = main;
    }

    @Override
    public boolean isAttacking(Ship other) {
        return false;
    }

    @Override
    public boolean isAiming(Ship other) {
        return false;
    }

    @Override
    public boolean isInvalid(long mapAddress) {
        return false;
    }

    @Override
    public boolean trySelect(boolean tryAttack) {
        return false; // Always fail to lock
    }

    @Override public void update() {}
    @Override public void update(long address) {}

    @Override
    public void removed() {
        super.removed();
        pingAlive = 0;
    }

    public boolean isPingAlive() {
        return pingAlive > System.currentTimeMillis();
    }

    public void set(Location loc, NpcInfo type) {
        if (loc == null || (loc.x == 0 && loc.y == 0)) {
            if (!isPingAlive()) removed();
            return;
        }
        removed = false;
        pingAlive = System.currentTimeMillis() + 1_000; // delay between locator pings may be 500ms + game ping ms
        locationInfo.updatePosition(loc.x, loc.y);
        npcInfo = type;
    }

}

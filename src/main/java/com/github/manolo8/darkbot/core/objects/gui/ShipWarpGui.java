package com.github.manolo8.darkbot.core.objects.gui;

import com.github.manolo8.darkbot.core.objects.Gui;
import com.github.manolo8.darkbot.core.objects.facades.ShipWarpProxy;
import eu.darkbot.api.managers.BotAPI;
import eu.darkbot.api.managers.ShipWarpAPI;
import eu.darkbot.util.Timer;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class ShipWarpGui extends Gui implements ShipWarpAPI {
    private final BotAPI bot;
    private final ShipWarpProxy shipWarpProxy;

    private final Timer guiUsed = Timer.getRandom(19_000, 1000);

    @Override
    public void update() {
        super.update();
        // Last gui usage >20s ago, close gui
        if (bot.isRunning() && guiUsed.isInactive()) {
            this.show(false);
        }
    }

    @Override
    public void updateShipList() {
        if (this.show(true)) {
            this.shipWarpProxy.update();
        }
    }

    @Override
    public boolean isNearSpaceStation() {
        return this.shipWarpProxy.isNearSpaceStation();
    }

    @Override
    public List<? extends Ship> getShips() {
        return shipWarpProxy.getShipsList();
    }

    public void clickShip(ShipWarpProxy.Ship ship) {
        if (this.show(true)) {
            this.clickShip(shipWarpProxy.getShipsList().indexOf(ship));
        }
    }

    public void clickShip(int index) {
        if (index < 0) return;
        if (index >= shipWarpProxy.getShipsList().size()) return;
        if (this.show(true)) {
            this.click(104 + (index * 100), 97);
        }
    }

    public void clickWarp() {
        if (this.show(true)) {
            this.click(330, 210);
        }
    }
}

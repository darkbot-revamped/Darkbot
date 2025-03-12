package com.github.manolo8.darkbot.core.manager;

import com.github.manolo8.darkbot.core.objects.Gui;
import com.github.manolo8.darkbot.core.objects.facades.ReturneeCalendarProxy;
import com.github.manolo8.darkbot.core.objects.facades.ReturneeLoginProxy;
import eu.darkbot.api.managers.BotAPI;
import eu.darkbot.api.managers.ReturneeAPI;
import eu.darkbot.util.Timer;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class ReturneeManager extends Gui implements ReturneeAPI {
    private final ReturneeLoginProxy returneeLoginProxy;
    private final ReturneeCalendarProxy returneeCalendarProxy;
    private final BotAPI bot;

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
    public boolean show(boolean value) {
        if (value) guiUsed.activate();
        return super.show(value);
    }

    public boolean clickOverview() {
        if (show(true)) {
            this.click(320, 37);
            return true;
        }
        return false;
    }

    public boolean clickDailyLogin() {
        if (show(true)) {
            this.click(420, 37);
            return true;
        }
        return false;
    }

    public boolean clickClaim() {
        if (show(true)) {
            this.click(300, 410);
            return true;
        }
        return false;
    }

    @Override
    public boolean isLoginClaimable() {
        return returneeLoginProxy.isClaimable();
    }

    @Override
    public List<? extends LoginRewardList> getLoginRewardList() {
        return returneeLoginProxy.getRewardList();
    }

    @Override
    public boolean isCalendarClaimable() {
        return returneeCalendarProxy.isClaimable();
    }

    @Override
    public List<? extends CalendarRewardList> getCalendarRewardList() {
        return returneeCalendarProxy.getCalendarList();
    }
}

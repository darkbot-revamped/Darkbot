package com.github.manolo8.darkbot.modules.utils;

import com.github.manolo8.darkbot.Main;
import com.github.manolo8.darkbot.config.Config;
import com.github.manolo8.darkbot.config.NpcExtra;
import com.github.manolo8.darkbot.core.api.Capability;
import com.github.manolo8.darkbot.core.entities.FakeNpc;
import com.github.manolo8.darkbot.core.entities.Npc;
import com.github.manolo8.darkbot.core.manager.EffectManager;
import com.github.manolo8.darkbot.core.manager.HeroManager;
import com.github.manolo8.darkbot.core.manager.MapManager;
import com.github.manolo8.darkbot.core.objects.facades.SettingsProxy;
import com.github.manolo8.darkbot.core.objects.slotbars.CategoryBar;
import com.github.manolo8.darkbot.core.utils.Drive;
import com.github.manolo8.darkbot.extensions.features.Feature;
import com.github.manolo8.darkbot.extensions.features.handlers.LaserSelectorHandler;
import eu.darkbot.api.extensions.selectors.LaserSelector;
import eu.darkbot.api.extensions.selectors.PrioritizedSupplier;
import eu.darkbot.api.game.items.Item;
import eu.darkbot.api.game.items.SelectableItem;
import eu.darkbot.api.game.other.Lockable;
import eu.darkbot.api.managers.AttackAPI;
import eu.darkbot.api.managers.HeroAPI;
import eu.darkbot.api.managers.HeroItemsAPI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.github.manolo8.darkbot.Main.API;
import static com.github.manolo8.darkbot.core.objects.facades.SettingsProxy.KeyBind.ATTACK_LASER;

public class NpcAttacker implements AttackAPI {

    // This is a very, very dirty variable for backwards compatibility.
    // Newer api assumes AttackAPI to be a singleton, so there will be a single
    // instance used by all features, and that same instance is available in the
    // PrioritizedSuppliers for lasers.
    // However, legacy features will continue to call new NpcAttacker creating their own
    // instance, for those cases, this variable is set right before calling to get what ammo
    // should be used, so that suppliers check for flags against the right attacker.
    private static NpcAttacker caller;

    protected Main main;
    protected MapManager mapManager;
    protected HeroManager hero;
    protected Drive drive;
    protected final SettingsProxy keybinds;
    protected final CategoryBar bar;

    private final HeroItemsAPI items;
    private final LaserSelectorHandler laserHandler;

    public Npc target;
    protected Long ability; // Time at which offensive ability can be triggered

    // General delays
    protected long clickDelay; // When can we mouse-click again? (lock)
    protected long laserTime; // When can we cast attack again? (ctrl or attack key)

    // Attacking bug fixing
    protected long isAttacking; // Time until we consider the attack valid (not-bugged)
    protected int fixedTimes; // Amount of times we bugged

    protected boolean firstAttack; // If the initial attack was cast

    protected SelectableItem.Laser lastShot;

    // This is now always unset, kept for plugin backwards compat
    protected @Deprecated boolean sab, rsb;
    protected @Deprecated long usedRsb;

    public NpcAttacker(Main main) {
        this.main = main;
        this.mapManager = main.mapManager;
        this.hero = main.hero;
        this.drive = hero.drive;
        this.keybinds = main.facadeManager.settings;
        this.bar = main.facadeManager.slotBars.categoryBar;

        this.items = main.pluginAPI.getAPI(HeroItemsAPI.class);
        this.laserHandler = main.pluginAPI.requireInstance(LaserSelectorHandler.class);
    }

    public String status() {
        return target != null ? "Killing npc" + (hero.isAttacking(target) ? " S" : "") + (ability != null ? " A" : "") : "Idle";
    }

    public boolean castingAbility() {
        return ability != null;
    }

    public boolean hasTarget() {
        return target != null && !target.removed;
    }

    public boolean isBugged() {
        return fixedTimes > 5;
    }

    public void doKillTargetTick() {
        if (target == null || target instanceof FakeNpc) return;
        if (!mapManager.isTarget(target)) {
            lockAndSetTarget();
            return;
        }

        if (ability != null && ability < System.currentTimeMillis()) {
            if (target.health.maxHp < main.config.LOOT.SHIP_ABILITY_MIN) ability = null;
            else if (hero.locationInfo.distance(target) < 575) {
                API.keyboardClick(main.config.LOOT.SHIP_ABILITY);
                ability = null;
            }
        }

        tryAttackOrFix();
    }

    void lockAndSetTarget() {
        if (hero.getLocalTarget() == target && firstAttack) {
            // On npc death, lock goes away before the npc does, sometimes the bot would try to lock the dead npc.
            // This adds a bit of delay when any cause makes you lose the lock, until you try to re-lock.
            clickDelay = System.currentTimeMillis();
        }

        fixedTimes = 0;
        laserTime = 0;
        firstAttack = false;
        if (hero.locationInfo.distance(target) < 800 && System.currentTimeMillis() - clickDelay > 500) {
            hero.setLocalTarget(target);
            target.trySelect(false);
            clickDelay = System.currentTimeMillis();
            if (main.config.LOOT.SHIP_ABILITY != null) ability = clickDelay + 4000;
        }
    }

    protected void tryAttackOrFix() {
        if (System.currentTimeMillis() < laserTime) return;

        if (!firstAttack) {
            firstAttack = true;
            sendAttack(1500, 5000, true);
        } else if (getPreviousAttackLaser() != getAttackLaser()) {
            sendAttack(250, 5000, true);
        } else if (!hero.isAttacking(target) || !hero.isAiming(target)) {
            sendAttack(1500, 5000, false);
        } else if (target.health.hpDecreasedIn(1500) || target.hasEffect(EffectManager.Effect.NPC_ISH)
                || hero.locationInfo.distance(target) > 700) {
            isAttacking = Math.max(isAttacking, System.currentTimeMillis() + 2000);
        } else if (System.currentTimeMillis() > isAttacking) {
            sendAttack(1500, ++fixedTimes * 3000L, false);
        }
    }

    private void sendAttack(long minWait, long bugTime, boolean normal) {
        laserTime = System.currentTimeMillis() + minWait;
        isAttacking = Math.max(isAttacking, laserTime + bugTime);
        if (normal) items.useItem(lastShot = getAttackLaser());
        else if (API.hasCapability(Capability.ALL_KEYBINDS_SUPPORT))
            keybinds.pressKeybind(ATTACK_LASER);
        else target.trySelect(true);
    }

    public double modifyRadius(double radius) {
        if (target.health.hpPercent() < 0.25 && target.npcInfo.extra.has(NpcExtra.AGGRESSIVE_FOLLOW)) radius *= 0.75;
        if (target != hero.getLocalTarget() || !hero.isAttacking(target) || castingAbility()) radius = Math.min(550, radius);
        else if (!target.locationInfo.isMoving() || target.health.hpPercent() < 0.25) radius = Math.min(600, radius);

        return radius + bar.findItemById("ability_zephyr_mmt").map(i -> i.quantity).orElse(0d) * 5;
    }

    private SelectableItem.Laser getAttackLaser() {
        caller = this;
        SelectableItem.Laser laser = laserHandler.getBest();
        caller = null;

        if (laser != null) {
            return laser;
        }

        return this.target == null || this.target.npcInfo.attackLaser == null ?
                main.config.LOOT.LASER : this.target.npcInfo.attackLaser;
    }

    private SelectableItem.Laser getPreviousAttackLaser() {
        return lastShot;
    }

    @Override
    public @Nullable Lockable getTarget() {
        return target;
    }

    @Override
    public void setTarget(@Nullable Lockable target) {
        if (target != null && !(target instanceof Npc))
            throw new IllegalArgumentException("Only NPC attacking is supported by this implementation");
        this.target = (Npc) target;
    }

    @Override
    public boolean isLocked() {
        return mapManager.isTarget(target);
    }

    @Override
    public void tryLockTarget() {
        if (!mapManager.isTarget(target)) lockAndSetTarget();
    }

    @Override
    public boolean isAttacking() {
        return hero.isAttacking(target);
    }

    @Override
    public void tryLockAndAttack() {
        doKillTargetTick();
    }

    @Override
    public void stopAttack() {
        if (System.currentTimeMillis() < laserTime) return;
        if (isAttacking()) {
            laserTime = System.currentTimeMillis() + 1500;
            if (API.hasCapability(Capability.ALL_KEYBINDS_SUPPORT))
                API.keyboardClick(keybinds.getCharCode(ATTACK_LASER));
            else items.useItem(getPreviousAttackLaser());
        }
    }

    @Override
    public String getStatus() {
        return status();
    }

    @Override
    public boolean isCastingAbility() {
        return castingAbility();
    }

    public static AttackAPI getCallerOr(AttackAPI api) {
        return caller != null ? caller : api;
    }

    @Feature(name = "RSB supplier", description = "Supplies RSB ammo if enabled & it's time")
    public static class RsbSupplier implements LaserSelector, PrioritizedSupplier<SelectableItem.Laser> {

        private final Main main;
        private final AttackAPI attacker;
        private final HeroItemsAPI items;

        private Item rsbItem;

        private long usedRsb = 0;

        public RsbSupplier(Main main, AttackAPI attacker, HeroItemsAPI items) {
            this.main = main;
            this.attacker = attacker;
            this.items = items;
        }

        @Override
        public @NotNull PrioritizedSupplier<SelectableItem.Laser> getLaserSupplier() {
            return this;
        }

        @Override
        public SelectableItem.Laser get() {
            if (!shouldRsb()) return null;
            else return rsbItem != null ? rsbItem.getAs(SelectableItem.Laser.class) : null;
        }

        private boolean shouldRsb() {
            if (!main.config.LOOT.RSB.ENABLED || main.config.LOOT.RSB.KEY == null
                    || !getCallerOr(attacker).hasExtraFlag(NpcExtra.USE_RSB)) return false;

            rsbItem = items.getItem(main.config.LOOT.RSB.KEY);
            boolean isReady = rsbItem != null && rsbItem.isUsable() && rsbItem.isReady();

            if (isReady && usedRsb < System.currentTimeMillis() - 1000) usedRsb = System.currentTimeMillis();
            return rsbItem != null && usedRsb > System.currentTimeMillis() - 50;
        }

        @Override
        public @Nullable Priority getPriority() {
            return Priority.MODERATE;
        }
    }

    @Feature(name = "SAB supplier", description = "Supplies SAB ammo if enabled & should enable")
    public static class SabSupplier implements LaserSelector, PrioritizedSupplier<SelectableItem.Laser> {

        private final Main main;
        private final HeroAPI hero;
        private final AttackAPI attacker;
        private final HeroItemsAPI items;

        public SabSupplier(Main main, HeroAPI hero, AttackAPI attacker, HeroItemsAPI items) {
            this.main = main;
            this.hero = hero;
            this.attacker = attacker;
            this.items = items;
        }

        @Override
        public @NotNull PrioritizedSupplier<SelectableItem.Laser> getLaserSupplier() {
            return this;
        }

        @Override
        public SelectableItem.Laser get() {
            if (!shouldSab()) return null;
            return SelectableItem.Laser.SAB_50;
        }

        private boolean shouldSab() {
            if (!main.config.LOOT.SAB.ENABLED || getCallerOr(attacker).hasExtraFlag(NpcExtra.NO_SAB)) return false;

            Config.Loot.Sab SAB = main.config.LOOT.SAB;
            return hero.getHealth().shieldPercent() <= SAB.PERCENT
                    && getCallerOr(attacker).getTarget().getHealth().getShield() > SAB.NPC_AMOUNT
                    && (SAB.CONDITION == null || SAB.CONDITION.get(main).toBoolean());
        }

        @Override
        public @Nullable Priority getPriority() {
            return Priority.LOW;
        }
    }

}

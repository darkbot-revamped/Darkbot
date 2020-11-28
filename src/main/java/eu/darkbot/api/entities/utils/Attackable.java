package eu.darkbot.api.entities.utils;

import eu.darkbot.api.entities.Entity;
import eu.darkbot.api.objects.Health;
import eu.darkbot.api.objects.Info;

public interface Attackable extends Entity, Health, Info {

    /**
     * Check lock type for this {@link Attackable}
     *
     * @return {@link Lock}
     */
    Lock getLockType();

    /**
     * @return speed of the {@link Attackable} in-game.
     */
    int getSpeed();

    /**
     * @return angle of the {@link Attackable} in-game as radians.
     */
    double getAngle();

    /**
     * Represents lock types in-game.
     */
    enum Lock {

        /**
         * Unknown
         */
        UNKNOWN,

        /**
         * Owned by {@link eu.darkbot.api.managers.HeroAPI}.
         */
        RED,

        /**
         * Owned by someone else.
         */
        GRAY_LIGHT,

        /**
         * Citadel's draw fire ability. ?
         */
        PURPLE,

        /**
         * ?
         */
        GRAY_DARK;

        public static Lock getType(int lockId) {
            if (lockId >= values().length || lockId < 0) return UNKNOWN;
            return values()[lockId];
        }
    }
}

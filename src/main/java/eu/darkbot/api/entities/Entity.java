package eu.darkbot.api.entities;

import eu.darkbot.api.entities.utils.EntityEffect;
import eu.darkbot.api.objects.LocationInfo;

public interface Entity {
    /**
     * @return id of entity
     */
    int getId();

    /**
     * @return memory address
     */
    long getAddress();

    /**
     * Checks that entity is removed.
     * If (isRemoved() == true) then this entity wont be updated anymore.
     */
    boolean isRemoved();

    /**
     * @return {@link LocationInfo}
     */
    LocationInfo getLocationInfo();

    /**
     * Checks that entity have effect
     * @param effect to check
     * @return true if current entity have indicated effect
     */
    boolean hasEffect(EntityEffect effect);
    boolean hasEffect(int effectId);

    /**
     * Sets metadata key with given value and stores it only for current entity.
     * Can be used for custom timers, checks, predicates etc.
     *
     * @param key your unique key
     * @param value to be put with your key
     * @return result of {@link java.util.Map#put(Object, Object)}
     */
    Object setMetadata(String key, Object value);

    /**
     * Returns value associated with key or null if key doesnt exists.
     */
    Object getMetadata(String key);

    /**
     * Clicks on entity only if distance is lower than 800.
     *
     * @param doubleClick should be double clicked
     * @return true on successful click
     */
    boolean clickOnEntity(boolean doubleClick);
}

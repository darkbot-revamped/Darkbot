package com.github.manolo8.darkbot.core;

import com.github.manolo8.darkbot.core.api.GameAPI;
import com.github.manolo8.darkbot.core.entities.Box;
import com.github.manolo8.darkbot.core.entities.Entity;
import com.github.manolo8.darkbot.core.objects.slotbars.Item;
import eu.darkbot.api.game.other.Locatable;
import eu.darkbot.api.managers.MemoryAPI;
import eu.darkbot.api.managers.OreAPI;
import eu.darkbot.api.managers.WindowAPI;
import org.intellij.lang.annotations.Language;

import java.util.function.LongPredicate;

public interface IDarkBotAPI extends WindowAPI, MemoryAPI {

    void tick();
    int getRefreshCount();

    void createWindow();
    void setSize(int width, int height);

    boolean isValid();
    boolean isInitiallyShown();
    long getMemoryUsage();
    double getCpuUsage();
    String getVersion();

    void mouseMove(int x, int y);
    void mouseDown(int x, int y);
    void mouseUp(int x, int y);
    void mouseClick(int x, int y);

    @Deprecated
    default void keyboardClick(char btn) {
        rawKeyboardClick(Character.toUpperCase(btn));
    }
    @Deprecated
    void directKeyClick(Character character);

    void rawKeyboardClick(char btn);

    default void keyboardClick(Character ch) {
        if (ch != null) rawKeyboardClick(ch);
    }

    @Override
    default void keyClick(int keyCode) {
        rawKeyboardClick((char) keyCode);
    }

    void sendText(String string);

    double readMemoryDouble(long address);
    default double readMemoryDouble(long address, int... offsets) {
        for (int i = 0; i < offsets.length - 1; i++) address = readMemoryLong(address + offsets[i]);
        return readMemoryDouble(address + offsets[offsets.length - 1]);
    }

    long readMemoryLong(long address);
    default long readMemoryLong(long address, int... offsets) {
        for (int offset : offsets) address = readMemoryLong(address + offset);
        return address;
    }

    int readMemoryInt(long address);
    default int readMemoryInt(long address, int... offsets) {
        for (int i = 0; i < offsets.length - 1; i++) address = readMemoryLong(address + offsets[i]);
        return readMemoryInt(address + offsets[offsets.length - 1]);
    }

    boolean readMemoryBoolean(long address);
    default boolean readMemoryBoolean(long address, int... offsets) {
        for (int i = 0; i < offsets.length - 1; i++) address = readMemoryLong(address + offsets[i]);
        return readMemoryBoolean(address + offsets[offsets.length - 1]);
    }

    String readMemoryString(long address);
    String readMemoryStringFallback(long address, String fallback);
    default String readMemoryString(long address, int... offsets) {
        for (int offset : offsets) address = readMemoryLong(address + offset);
        return readMemoryString(address);
    }
    default String readMemoryStringFallback(long address, String fallback, int... offsets) {
        for (int offset : offsets) address = readMemoryLong(address + offset);
        return readMemoryStringFallback(address, fallback);
    }

    byte[] readMemory(long address, int length);
    default void readMemory(long address, byte[] buffer) {
        readMemory(address, buffer, buffer.length);
    }
    void readMemory(long address, byte[] buffer, int length);

    void writeMemoryInt(long address, int value);
    void writeMemoryLong(long address, long value);
    void writeMemoryDouble(long address, double value);

    long[] queryMemoryInt(int value, int maxQuantity);
    long[] queryMemoryLong(long value, int maxQuantity);
    long[] queryMemory(byte[] query, int maxQuantity);
    long queryMemory(byte... query);

    long searchClassClosure(LongPredicate pattern);

    void setVisible(boolean visible);
    default void setMinimized(boolean minimized) {
        setVisible(!minimized);
    }

    default void setVisible(boolean visible, boolean fullyHideEnabled) {
        if (fullyHideEnabled) setMinimized(!visible);
        else setVisible(visible);
    }

    void handleRefresh();

    void handleRelogin();

    void resetCache();

    boolean hasCapability(GameAPI.Capability capability);

    // Direct game access
    void setMaxFps(int maxCps);

    void lockEntity(int id);

    void selectEntity(Entity entity);

    void moveShip(Locatable destination);

    void collectBox(Box box);

    void refine(long refineUtilAddress, OreAPI.Ore ore, int amount);

    long callMethod(int index, long... arguments);
    boolean callMethodAsync(int index, long... arguments);
    boolean useItem(Item item);
    boolean isUseItemSupported();

    void postActions(long... actions);
    void pasteText(String text, long... actions);

    // Handler API
    void clearCache(@Language("RegExp") String pattern);
    void emptyWorkingSet();
    void setLocalProxy(int port);
    void setPosition(int x, int y);
    void setFlashOcxPath(String path);
    void setUserInput(boolean enable);
    void setClientSize(int width, int height);
    void setMinClientSize(int width, int height);
    void setTransparency(int transparency);
    void setVolume(int volume); // 0 - 100

    // LOW = 0, MEDIUM = 1, HIGH = 2, BEST = 3, AUTO_LOW = 4, AUTO_HIGH = 5
    void setQuality(GameAPI.Handler.GameQuality quality);

    //MemoryAPI
    @Override
    default int readInt(long address) {
        return readMemoryInt(address);
    }

    @Override
    default long readLong(long address) {
        return readMemoryLong(address);
    }

    @Override
    default double readDouble(long address) {
        return readMemoryDouble(address);
    }

    @Override
    default boolean readBoolean(long address) {
        return readMemoryBoolean(address);
    }

    @Override
    default String readString(long address) {
        return readMemoryString(address);
    }

    @Override
    default byte[] readBytes(long address, int length) {
        return readMemory(address, length);
    }

    @Override
    void replaceInt(long address, int oldValue, int newValue);

    @Override
    void replaceLong(long address, long oldValue, long newValue);

    @Override
    void replaceDouble(long address, double oldValue, double newValue);

    @Override
    void replaceBoolean(long address, boolean oldValue, boolean newValue);

    @Override
    default void writeInt(long address, int value) {
        writeMemoryInt(address, value);
    }

    @Override
    default void writeLong(long address, long value) {
        writeMemoryLong(address, value);
    }

    @Override
    default void writeDouble(long address, double value) {
        writeMemoryDouble(address, value);
    }

    @Override
    default void writeBoolean(long address, boolean value) {
        writeInt(address, value ? 1 : 0);
    }

    @Override
    default long[] searchInt(int value, int maxSize) {
        return queryMemoryInt(value, maxSize);
    }

    @Override
    default long[] searchLong(long value, int maxSize) {
        return queryMemoryLong(value, maxSize);
    }

    @Override
    default long[] searchPattern(int maxSize, byte... pattern) {
        return queryMemory(pattern, maxSize);
    }

}

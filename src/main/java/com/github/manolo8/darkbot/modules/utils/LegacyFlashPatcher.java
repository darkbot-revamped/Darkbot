package com.github.manolo8.darkbot.modules.utils;

import com.github.manolo8.darkbot.Main;
import com.github.manolo8.darkbot.core.api.DarkBoatAdapter;
import com.github.manolo8.darkbot.core.api.DarkBoatHookAdapter;
import com.github.manolo8.darkbot.gui.utils.Popups;
import com.github.manolo8.darkbot.utils.FileUtils;
import com.github.manolo8.darkbot.utils.I18n;
import com.github.manolo8.darkbot.utils.LibSetup;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class LegacyFlashPatcher {

    private static final Path TMP_SCRIPT = Paths.get("FlashPatcher.bat");

    private final List<Fixer> FIXERS = Arrays.asList(
            new FlashInstaller(),
            new FlashConfigSetter());

    public void runPatcher() {
        /*if (!(Main.API instanceof DarkBoatAdapter || Main.API instanceof DarkBoatHookAdapter)) {
            // Linux APIs or kekka player do not require this at all. Only darkboat requires it.
            return;
        }*/
        List<Fixer> needFixing = FIXERS.stream().filter(Fixer::needsFix).collect(Collectors.toList());
        Collections.reverse(needFixing); //for a better output in prompt if mms need fix too

        if (needFixing.isEmpty()) return;

        Popups.of(I18n.get("flash.fix.warn.title"), I18n.get("flash.fix.warn.content"),
                        JOptionPane.INFORMATION_MESSAGE)
                .showSync();

        List<String> script = new ArrayList<>();
        script.add("@echo off");
        script.add("chcp 65001");
        for (Fixer fixer : needFixing) script.addAll(fixer.script()); //TODO make loop for force check if sha256 is ok
        script.add("exit");

        try {
            Files.write(TMP_SCRIPT, script);
            Runtime.getRuntime().exec("powershell start -verb runas './FlashPatcher.bat' -wait").waitFor();

            File tmpFile = new File(TMP_SCRIPT.toUri());
            if(tmpFile.delete()){
                System.out.println("Tmp script deleted and patch applied");
            } else {
                System.out.println("Failed to delete tmp script");
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    protected void cleanupCache() {
        try {
            // Delete cookies
            Runtime.getRuntime().exec("RunDll32.exe InetCpl.cpl,ClearMyTracksByProcess 2");
            // Delete temp files
            Runtime.getRuntime().exec("RunDll32.exe InetCpl.cpl,ClearMyTracksByProcess 8");
            // Delete form data
            Runtime.getRuntime().exec("RunDll32.exe InetCpl.cpl,ClearMyTracksByProcess 16");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private interface Fixer {
        boolean needsFix();

        List<String> script();
    }

    private static class FlashInstaller implements Fixer {
        private static final Path
                FLASH_DIR = Paths.get(System.getenv("APPDATA"), "DarkBot", "Flash"),
                FLASH_OCX = FLASH_DIR.resolve("Flash.ocx"),
                REG_SVR = Paths.get(System.getenv("WINDIR"), "SysWOW64", "regsvr32");

        public boolean needsFix() {
            return LibSetup.downloadLib("Flash.ocx", FLASH_OCX);
        }

        public List<String> script() {
            FileUtils.ensureDirectoryExists(FLASH_DIR);

            try (InputStream in = new URL("https://darkbot.eu/downloads/Flash.ocx").openStream()) {
                Files.copy(in, FLASH_OCX, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                e.printStackTrace();
            }

            return Arrays.asList(
                    "echo \"" + I18n.get("flash.fix.apply.patch_flash.content") + "\"",
                    "\"" + REG_SVR.toAbsolutePath() + "\"  \"" + FLASH_OCX.toAbsolutePath() + "\"");
        }
    }

    private static class FlashConfigSetter implements Fixer {
        private static final Path
                FLASH_FOLDER = Paths.get(System.getenv("WINDIR"), "SysWOW64", "Macromed", "Flash"),
                FLASH_CONFIG = FLASH_FOLDER.resolve("mms.cfg"),
                TMP_CONFIG = Paths.get("mms.cfg");
        private static final List<String> CONFIG_CONTENT = Arrays.asList(
                "EnableAllowList=1",
                "AllowListPreview=1",
                "AllowListRootMovieOnly=1",
                "AllowListUrlPattern=https://*.bpsecure.com/",
                "SilentAutoUpdateEnable=1",
                "EnableWhiteList=1",
                "WhiteListPreview=1",
                "WhiteListRootMovieOnly=1",
                "WhiteListUrlPattern=https://*.bpsecure.com/",
                "AutoUpdateDisable=1",
                "EOLUninstallDisable=1");

        public boolean needsFix() {
            if (!Files.exists(FLASH_FOLDER) || !Files.exists(FLASH_CONFIG)) return true;
            try {
                return !Files.readAllLines(FLASH_CONFIG).equals(CONFIG_CONTENT);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        public List<String> script() {
            try {
                Files.write(TMP_CONFIG, CONFIG_CONTENT);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            return Arrays.asList(
                    "echo \"" + I18n.get("flash.fix.apply.patch_mms.content") + "\"",
                    "mkdir \"" + FLASH_FOLDER.toAbsolutePath() + "\"",
                    "move \"" + TMP_CONFIG.toAbsolutePath() + "\" \"" + FLASH_CONFIG.toAbsolutePath() + "\"");
        }
    }

}

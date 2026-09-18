package com.example.tracers;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Настройки TracerMod. Сохраняются в config/tracermod.properties,
 * поэтому переживают перезапуск игры.
 *
 * Целей игроков здесь намеренно нет и не будет — только животные
 * и (по желанию) враждебные мобы.
 */
public class TracerConfig {
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("tracermod.properties");

    public boolean enabled = false;
    public int radius = 64;
    public int colorMode = 0; // 0=красный 1=зелёный 2=синий 3=жёлтый
    public boolean targetAnimals = true;
    public boolean targetMonsters = false;

    public static TracerConfig load() {
        TracerConfig cfg = new TracerConfig();
        if (Files.exists(PATH)) {
            Properties p = new Properties();
            try (Reader r = Files.newBufferedReader(PATH, StandardCharsets.UTF_8)) {
                p.load(r);
                cfg.enabled = Boolean.parseBoolean(p.getProperty("enabled", "false"));
                cfg.radius = Integer.parseInt(p.getProperty("radius", "64"));
                cfg.colorMode = Integer.parseInt(p.getProperty("colorMode", "0"));
                cfg.targetAnimals = Boolean.parseBoolean(p.getProperty("targetAnimals", "true"));
                cfg.targetMonsters = Boolean.parseBoolean(p.getProperty("targetMonsters", "false"));
            } catch (IOException | NumberFormatException ignored) {
                // используем значения по умолчанию, если файл повреждён
            }
        }
        return cfg;
    }

    public void save() {
        Properties p = new Properties();
        p.setProperty("enabled", Boolean.toString(enabled));
        p.setProperty("radius", Integer.toString(radius));
        p.setProperty("colorMode", Integer.toString(colorMode));
        p.setProperty("targetAnimals", Boolean.toString(targetAnimals));
        p.setProperty("targetMonsters", Boolean.toString(targetMonsters));
        try {
            Files.createDirectories(PATH.getParent());
            try (Writer w = Files.newBufferedWriter(PATH, StandardCharsets.UTF_8)) {
                p.store(w, "TracerMod settings");
            }
        } catch (IOException ignored) {
        }
    }

    public String colorName() {
        switch (colorMode) {
            case 1: return "Зелёный";
            case 2: return "Синий";
            case 3: return "Жёлтый";
            default: return "Красный";
        }
    }

    /** Циклически переключает набор целей: Животные -> Животные+мобы -> Мобы -> Животные */
    public void cycleTargetMode() {
        if (targetAnimals && !targetMonsters) {
            targetMonsters = true; // животные + мобы
        } else if (targetAnimals) {
            targetAnimals = false; // только мобы
        } else {
            targetAnimals = true;
            targetMonsters = false; // назад к животным
        }
    }

    public String targetModeName() {
        if (targetAnimals && targetMonsters) return "Животные + мобы";
        if (targetMonsters) return "Только враждебные мобы";
        return "Только животные";
    }
}

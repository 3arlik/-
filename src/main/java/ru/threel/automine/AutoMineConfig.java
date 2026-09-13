package ru.threel.automine;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Block;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

// Конфиг: список целевых блоков (по id реестра) + настройки безопасности.
// Хранится в config/automine.json, чтобы не терять выбор между запусками.
public class AutoMineConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("automine.json");

    public boolean enabled = false;
    public int radius = 24;          // радиус поиска блоков
    public int reach = 4;            // дальность добычи
    public Set<String> targetBlockIds = new HashSet<>();

    // блоки-опасности рядом с которыми не подходим и не копаем
    public static final String[] HAZARDS = {
            "minecraft:lava", "minecraft:water", "minecraft:cactus",
            "minecraft:magma_block", "minecraft:fire", "minecraft:slime_block"
    };

    private static AutoMineConfig instance;

    public static AutoMineConfig get() {
        if (instance == null) instance = load();
        return instance;
    }

    public boolean isTarget(Block block) {
        Identifier id = Registries.BLOCK.getId(block);
        return targetBlockIds.contains(id.toString());
    }

    public void save() {
        try {
            Files.createDirectories(FILE.getParent());
            try (Writer w = Files.newBufferedWriter(FILE, StandardCharsets.UTF_8)) {
                GSON.toJson(this, w);
            }
        } catch (IOException e) {
            System.err.println("[AutoMine] Не удалось сохранить конфиг: " + e.getMessage());
        }
    }

    private static AutoMineConfig load() {
        if (Files.exists(FILE)) {
            try (Reader r = Files.newBufferedReader(FILE, StandardCharsets.UTF_8)) {
                AutoMineConfig cfg = GSON.fromJson(r, AutoMineConfig.class);
                if (cfg != null) return cfg;
            } catch (IOException e) {
                System.err.println("[AutoMine] Не удалось загрузить конфиг: " + e.getMessage());
            }
        }
        return new AutoMineConfig();
    }
}

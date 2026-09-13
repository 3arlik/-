package ru.threel.automine;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import ru.threel.automine.gui.BlockSelectScreen;
import ru.threel.automine.logic.MiningTask;

public class AutoMineClient implements ClientModInitializer {

    // хоткей открытия окна выбора блоков (по умолчанию — апостроф)
    private static final KeyBinding OPEN_GUI = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.automine.open",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_APOSTROPHE,
            "category.automine"
    ));

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (OPEN_GUI.wasPressed()) {
                if (client.currentScreen == null) {
                    client.setScreen(new BlockSelectScreen(null));
                }
            }
            MiningTask.tick(client);
        });
    }
}

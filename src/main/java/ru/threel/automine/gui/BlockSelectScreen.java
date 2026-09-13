package ru.threel.automine.gui;

import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ElementListWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import ru.threel.automine.AutoMineConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

// Окно выбора целевых блоков: поиск по имени + чекбокс на каждый.
// Открывается по хоткею (см. AutoMineClient).
public class BlockSelectScreen extends Screen {

    private final Screen parent;
    private TextFieldWidget searchBox;
    private BlockList list;
    private ButtonWidget toggleButton;

    public BlockSelectScreen(Screen parent) {
        super(Text.of("AutoMine — выбор блоков"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        AutoMineConfig cfg = AutoMineConfig.get();

        searchBox = new TextFieldWidget(textRenderer, width / 2 - 100, 20, 200, 20, Text.of("Поиск"));
        searchBox.setChangedListener(s -> refreshList(s));
        addSelectableChild(searchBox);
        setInitialFocus(searchBox);

        list = new BlockList(client, width, height - 80, 48, 24);
        addSelectableChild(list);
        refreshList("");

        toggleButton = ButtonWidget.builder(
                Text.of(cfg.enabled ? "Автодобыча: ВКЛ" : "Автодобыча: ВЫКЛ"),
                b -> {
                    cfg.enabled = !cfg.enabled;
                    b.setMessage(Text.of(cfg.enabled ? "Автодобыча: ВКЛ" : "Автодобыча: ВЫКЛ"));
                    cfg.save();
                }
        ).dimensions(width / 2 - 100, height - 28, 95, 20).build();
        addDrawableChild(toggleButton);

        addDrawableChild(ButtonWidget.builder(Text.of("Закрыть"), b -> close())
                .dimensions(width / 2 + 5, height - 28, 95, 20).build());
    }

    private void refreshList(String filter) {
        String f = filter.toLowerCase();
        List<Identifier> ids = Registries.BLOCK.getIds().stream()
                .filter(id -> id.getPath().contains(f))
                .sorted()
                .collect(Collectors.toList());
        list.setEntries(ids);
    }

    @Override
    public void render(net.minecraft.client.gui.DrawContext ctx, int mouseX, int mouseY, float delta) {
        renderBackground(ctx, mouseX, mouseY, delta);
        list.render(ctx, mouseX, mouseY, delta);
        super.render(ctx, mouseX, mouseY, delta);
        searchBox.render(ctx, mouseX, mouseY, delta);
    }

    @Override
    public void close() {
        AutoMineConfig.get().save();
        client.setScreen(parent);
    }

    // список записей блоков
    private static class BlockList extends ElementListWidget<BlockList.Entry> {
        BlockList(MinecraftClient client, int width, int height, int y, int itemHeight) {
            super(client, width, height, y, itemHeight);
        }

        void setEntries(List<Identifier> ids) {
            clearEntries();
            for (Identifier id : ids) addEntry(new Entry(id));
        }

        class Entry extends ElementListWidget.Entry<Entry> {
            private final Identifier id;
            private final ButtonWidget checkbox;

            Entry(Identifier id) {
                this.id = id;
                AutoMineConfig cfg = AutoMineConfig.get();
                boolean selected = cfg.targetBlockIds.contains(id.toString());
                this.checkbox = ButtonWidget.builder(labelFor(selected), b -> {
                    boolean nowSelected = !cfg.targetBlockIds.contains(id.toString());
                    if (nowSelected) cfg.targetBlockIds.add(id.toString());
                    else cfg.targetBlockIds.remove(id.toString());
                    b.setMessage(labelFor(nowSelected));
                }).dimensions(0, 0, 220, 20).build();
            }

            private Text labelFor(boolean selected) {
                return Text.of((selected ? "[x] " : "[ ] ") + id.getPath());
            }

            @Override
            public List<? extends net.minecraft.client.gui.Element> children() {
                return List.of(checkbox);
            }

            @Override
            public List<? extends net.minecraft.client.gui.Selectable> selectableChildren() {
                return List.of(checkbox);
            }

            @Override
            public void render(net.minecraft.client.gui.DrawContext ctx, int index, int y, int x,
                                int entryWidth, int entryHeight, int mouseX, int mouseY,
                                boolean hovered, float tickDelta) {
                checkbox.setX(x);
                checkbox.setY(y);
                checkbox.setWidth(entryWidth - 10);
                checkbox.render(ctx, mouseX, mouseY, tickDelta);
            }
        }
    }
}

package com.example.tracers.gui;

import com.example.tracers.TracerConfig;
import com.example.tracers.TracerModClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/**
 * Простой экран настроек на ванильных виджетах (без сторонних библиотек
 * конфигов) — открывается через Mod Menu кнопкой "Настроить" у мода.
 */
public class TracerConfigScreen extends Screen {
    private final Screen parent;

    public TracerConfigScreen(Screen parent) {
        super(Text.literal("Настройки TracerMod"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        TracerConfig cfg = TracerModClient.CONFIG;
        int centerX = this.width / 2;
        int y = this.height / 2 - 70;

        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Трассировка: " + (cfg.enabled ? "ВКЛ" : "ВЫКЛ")),
                btn -> {
                    cfg.enabled = !cfg.enabled;
                    cfg.save();
                    btn.setMessage(Text.literal("Трассировка: " + (cfg.enabled ? "ВКЛ" : "ВЫКЛ")));
                }).dimensions(centerX - 100, y, 200, 20).build());

        y += 24;
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Радиус: " + cfg.radius),
                btn -> {
                    cfg.radius = (cfg.radius == 64) ? 128 : (cfg.radius == 128) ? 256 : 64;
                    cfg.save();
                    btn.setMessage(Text.literal("Радиус: " + cfg.radius));
                }).dimensions(centerX - 100, y, 200, 20).build());

        y += 24;
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Цвет: " + cfg.colorName()),
                btn -> {
                    cfg.colorMode = (cfg.colorMode + 1) % 4;
                    cfg.save();
                    btn.setMessage(Text.literal("Цвет: " + cfg.colorName()));
                }).dimensions(centerX - 100, y, 200, 20).build());

        y += 24;
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Цели: " + cfg.targetModeName()),
                btn -> {
                    cfg.cycleTargetMode();
                    cfg.save();
                    btn.setMessage(Text.literal("Цели: " + cfg.targetModeName()));
                }).dimensions(centerX - 100, y, 200, 20).build());

        y += 32;
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Готово"),
                btn -> this.close()
        ).dimensions(centerX - 100, y, 200, 20).build());
    }

    @Override
    public void close() {
        this.client.setScreen(parent);
    }
}

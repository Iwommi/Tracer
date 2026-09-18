package com.example.tracers;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

public class TracerModClient implements ClientModInitializer {

    // Единый объект настроек, доступный из экрана конфига и из команд чата
    public static final TracerConfig CONFIG = TracerConfig.load();

    @Override
    public void onInitializeClient() {
        registerChatCommands();
        registerRenderer();
    }

    private void registerChatCommands() {
        ClientSendMessageEvents.ALLOW_CHAT.register(message -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player == null) return true;

            String msg = message.trim().toLowerCase();
            switch (msg) {
                case ".tracer":
                    CONFIG.enabled = !CONFIG.enabled;
                    CONFIG.save();
                    client.player.sendMessage(Text.literal("§6[TracerMod] §fТрассировка: "
                            + (CONFIG.enabled ? "§aВКЛЮЧЕНА" : "§cВЫКЛЮЧЕНА")), false);
                    return false;

                case ".tracedist":
                    CONFIG.radius = (CONFIG.radius == 64) ? 128 : (CONFIG.radius == 128) ? 256 : 64;
                    CONFIG.save();
                    client.player.sendMessage(Text.literal("§6[TracerMod] §fРадиус: §e" + CONFIG.radius), false);
                    return false;

                case ".tracermode":
                    CONFIG.colorMode = (CONFIG.colorMode + 1) % 4;
                    CONFIG.save();
                    client.player.sendMessage(Text.literal("§6[TracerMod] §fЦвет: §e" + CONFIG.colorName()), false);
                    return false;

                case ".tracetype":
                    CONFIG.cycleTargetMode();
                    CONFIG.save();
                    client.player.sendMessage(Text.literal("§6[TracerMod] §fЦели: §e" + CONFIG.targetModeName()), false);
                    return false;

                default:
                    return true;
            }
        });
    }

    private void registerRenderer() {
        // ПРИМЕЧАНИЕ: этот блок написан в "классическом" стиле рендера через
        // Tessellator/RenderSystem.setShader(...), который использовался в
        // предыдущих версиях 1.21.x. Minecraft 1.21.11 переработал рендер
        // мира на систему кастомных RenderPipeline с разделением на фазы
        // "extraction/drawing" (см. docs.fabricmc.net -> Rendering in the World
        // для твоей версии). Если IDE подчеркнёт красным Tessellator.begin(...)
        // или RenderSystem.setShader(...) — значит на твоей сборке 1.21.11 этот
        // старый способ уже убран, и этот метод нужно переписать под новый
        // RenderPipeline (могу помочь отдельно, но гарантированно проверить
        // компиляцию я тут не могу — нет доступа к билд-зависимостям игры).
        WorldRenderEvents.LAST.register(context -> {
            if (!CONFIG.enabled) return;
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player == null || client.world == null) return;

            MatrixStack matrices = context.matrixStack();
            Vec3d cameraPos = context.camera().getPos();

            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.LINES, VertexFormats.POSITION_COLOR);

            RenderSystem.disableDepthTest();
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShader(GameRenderer::getPositionColorProgram);
            RenderSystem.lineWidth(2.5f);

            float r = 1.0f, g = 0.3f, b = 0.3f;
            if (CONFIG.colorMode == 1) { r = 0.3f; g = 1.0f; b = 0.3f; }
            else if (CONFIG.colorMode == 2) { r = 0.3f; g = 0.3f; b = 1.0f; }
            else if (CONFIG.colorMode == 3) { r = 1.0f; g = 1.0f; b = 0.3f; }

            Vec3d startPos = client.player.getCameraPosVec(context.tickCounter().getTickDelta(true));

            for (Entity entity : client.world.getEntities()) {
                if (entity == client.player) continue;
                if (!isValidTarget(entity)) continue;

                double dist = client.player.squaredDistanceTo(entity);
                if (dist > (double) CONFIG.radius * CONFIG.radius) continue;

                Vec3d endPos = entity.getPos().add(0, entity.getHeight() / 2.0, 0);

                buffer.vertex(matrices.peek().getPositionMatrix(),
                                (float) (startPos.x - cameraPos.x),
                                (float) (startPos.y - cameraPos.y),
                                (float) (startPos.z - cameraPos.z))
                        .color(r, g, b, 1.0f);

                buffer.vertex(matrices.peek().getPositionMatrix(),
                                (float) (endPos.x - cameraPos.x),
                                (float) (endPos.y - cameraPos.y),
                                (float) (endPos.z - cameraPos.z))
                        .color(r, g, b, 1.0f);
            }

            BufferRenderer.drawWithGlobalProgram(buffer.end());
            RenderSystem.enableDepthTest();
            RenderSystem.disableBlend();
        });
    }

    /**
     * Игроки здесь никогда не считаются валидной целью — трейсер работает
     * только по животным и, опционально, по враждебным мобам.
     */
    private boolean isValidTarget(Entity entity) {
        boolean isAnimal = entity instanceof AnimalEntity;
        boolean isMonster = entity instanceof HostileEntity;
        return (CONFIG.targetAnimals && isAnimal) || (CONFIG.targetMonsters && isMonster);
    }
}

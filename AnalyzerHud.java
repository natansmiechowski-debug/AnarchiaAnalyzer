package pl.anarchia.analyzer;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.RenderTickCounter;

import java.util.Locale;

public class AnalyzerHud {

    private static final int X = 6;
    private static final int Y = 6;
    private static final int PADDING = 5;
    private static final int LINE_HEIGHT = 11;
    private static final int BACKGROUND = 0x90000000;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int GRAY = 0xFFAAAAAA;

    public static void register() {
        HudRenderCallback.EVENT.register(AnalyzerHud::render);
    }

    private static void render(DrawContext context, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        String targetName = AnarchiaAnalyzer.getTargetName();

        if (targetName == null || client.player == null || client.world == null
                || client.options.hudHidden) {
            return;
        }

        TextRenderer font = client.textRenderer;
        AbstractClientPlayerEntity target = AnarchiaAnalyzer.findPlayer(client, targetName);

        if (target == null) {
            drawOutOfRange(context, font, targetName);
            return;
        }

        String hpLine = String.format(Locale.US, "HP: %.1f / %.1f", target.getHealth(), target.getMaxHealth());
        int trapChance = AnarchiaAnalyzer.TRAP_DETECTOR.getTrapChance(targetName);
        CombatAnalyzer.Result result = CombatAnalyzer.compare(client.player, target);

        String title = "Cel: " + targetName;
        String trapLine = "Traper: " + trapChance + "%";
        String fightLine = "Walka: " + result.label;

        int width = Math.max(Math.max(font.getWidth(title), font.getWidth(hpLine)),
                Math.max(font.getWidth(trapLine), font.getWidth(fightLine))) + PADDING * 2;
        int height = LINE_HEIGHT * 4 + PADDING * 2;

        context.fill(X, Y, X + width, Y + height, BACKGROUND);

        int textX = X + PADDING;
        int textY = Y + PADDING;

        context.drawText(font, title, textX, textY, WHITE, true);
        context.drawText(font, hpLine, textX, textY + LINE_HEIGHT, hpColor(target), true);
        context.drawText(font, trapLine, textX, textY + LINE_HEIGHT * 2, trapColor(trapChance), true);
        context.drawText(font, fightLine, textX, textY + LINE_HEIGHT * 3, result.color, true);
    }

    private static void drawOutOfRange(DrawContext context, TextRenderer font, String name) {
        String title = "Cel: " + name;
        String info = "Poza zasięgiem renderowania";

        int width = Math.max(font.getWidth(title), font.getWidth(info)) + PADDING * 2;
        int height = LINE_HEIGHT * 2 + PADDING * 2;

        context.fill(X, Y, X + width, Y + height, BACKGROUND);
        context.drawText(font, title, X + PADDING, Y + PADDING, WHITE, true);
        context.drawText(font, info, X + PADDING, Y + PADDING + LINE_HEIGHT, GRAY, true);
    }

    private static int hpColor(AbstractClientPlayerEntity target) {
        float ratio = target.getHealth() / Math.max(1f, target.getMaxHealth());
        if (ratio > 0.66f) return 0xFF55FF55;
        if (ratio > 0.33f) return 0xFFFFFF55;
        return 0xFFFF5555;
    }

    private static int trapColor(int chance) {
        if (chance >= 60) return 0xFFFF5555;
        if (chance >= 25) return 0xFFFFFF55;
        return 0xFF55FF55;
    }
}

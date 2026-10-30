package pl.anarchia.analyzer;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.command.CommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Locale;
import java.util.function.Consumer;

public class AnarchiaAnalyzer implements ClientModInitializer {

    public static final String MOD_ID = "anarchiaanalyzer";
    private static final String DOT_PREFIX = ".check";

    public static final TrapDetector TRAP_DETECTOR = new TrapDetector();

    private static volatile String targetName = null;

    @Override
    public void onInitializeClient() {
        ClientCommandRegistrationCallback.EVENT.register(
                (dispatcher, registryAccess) -> registerSlashCommand(dispatcher));

        ClientSendMessageEvents.ALLOW_CHAT.register(this::handleDotCommand);

        TRAP_DETECTOR.register();
        AnalyzerHud.register();

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            targetName = null;
            TRAP_DETECTOR.clear();
        });
    }

    private void registerSlashCommand(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(ClientCommandManager.literal("check")
                .executes(ctx -> {
                    executeCheck(null, text -> ctx.getSource().sendFeedback(text));
                    return 1;
                })
                .then(ClientCommandManager.argument("nick", StringArgumentType.word())
                        .suggests((ctx, builder) -> CommandSource.suggestMatching(
                                ctx.getSource().getPlayerNames(), builder))
                        .executes(ctx -> {
                            String nick = StringArgumentType.getString(ctx, "nick");
                            executeCheck(nick, text -> ctx.getSource().sendFeedback(text));
                            return 1;
                        })));
    }

    private boolean handleDotCommand(String message) {
        String trimmed = message.trim();
        if (!trimmed.toLowerCase(Locale.ROOT).startsWith(DOT_PREFIX)) {
            return true;
        }
        if (trimmed.length() > DOT_PREFIX.length()
                && !Character.isWhitespace(trimmed.charAt(DOT_PREFIX.length()))) {
            return true;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        client.inGameHud.getChatHud().addToMessageHistory(message);

        String[] parts = trimmed.split("\\s+");
        String nick = parts.length >= 2 ? parts[1] : null;
        executeCheck(nick, text -> {
            if (client.player != null) {
                client.player.sendMessage(text, false);
            }
        });
        return false;
    }

    private void executeCheck(String nick, Consumer<Text> feedback) {
        if (nick == null) {
            targetName = null;
            feedback.accept(prefix().append(Text.literal("Cel wyczyszczony.").formatted(Formatting.GRAY)));
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        AbstractClientPlayerEntity found = findPlayer(client, nick);
        if (found == null) {
            feedback.accept(prefix().append(Text.literal(
                    "Nie znaleziono gracza \"" + nick + "\" w zasięgu renderowania.")
                    .formatted(Formatting.RED)));
            return;
        }

        targetName = found.getGameProfile().getName();
        feedback.accept(prefix().append(Text.literal("Namierzono: ").formatted(Formatting.GRAY))
                .append(Text.literal(targetName).formatted(Formatting.GREEN)));
    }

    private static net.minecraft.text.MutableText prefix() {
        return Text.literal("[AnarchiaAnalyzer] ").formatted(Formatting.GOLD);
    }

    public static String getTargetName() {
        return targetName;
    }

    public static AbstractClientPlayerEntity findPlayer(MinecraftClient client, String nick) {
        if (client.world == null || nick == null) {
            return null;
        }
        for (AbstractClientPlayerEntity player : client.world.getPlayers()) {
            if (player.getGameProfile().getName().equalsIgnoreCase(nick)) {
                return player;
            }
        }
        return null;
    }
}

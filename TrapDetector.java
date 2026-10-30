package pl.anarchia.analyzer;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TrapDetector {

    private static final List<String> KEYWORDS = List.of(
            "tpa", "teleport", "darmowe", "sojusz", "rozdaje", "sklep", "bezpiecznie");

    private static final Pattern CHAT_FORMAT = Pattern.compile("^(.{1,80}?)\\s*[»>:]\\s*(.+)$");

    private final Map<String, Integer> suspiciousCounts = new HashMap<>();

    public void register() {
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (!overlay) {
                handleGameMessage(message.getString());
            }
        });

        ClientReceiveMessageEvents.CHAT.register((message, signedMessage, sender, params, timestamp) -> {
            if (sender != null) {
                processMessage(sender.getName(), message.getString());
            }
        });
    }

    private void handleGameMessage(String raw) {
        Matcher matcher = CHAT_FORMAT.matcher(raw);
        if (!matcher.matches()) {
            return;
        }
        String senderPart = matcher.group(1);
        String body = matcher.group(2);

        String sender = detectSender(senderPart);
        if (sender != null) {
            processMessage(sender, body);
        }
    }

    private String detectSender(String senderPart) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getNetworkHandler() == null) {
            return null;
        }
        String best = null;
        for (PlayerListEntry entry : client.getNetworkHandler().getPlayerList()) {
            String name = entry.getProfile().getName();
            if (containsWord(senderPart, name) && (best == null || name.length() > best.length())) {
                best = name;
            }
        }
        return best;
    }

    private static boolean containsWord(String text, String name) {
        Pattern p = Pattern.compile("(?<![A-Za-z0-9_])" + Pattern.quote(name) + "(?![A-Za-z0-9_])",
                Pattern.CASE_INSENSITIVE);
        return p.matcher(text).find();
    }

    private void processMessage(String sender, String body) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null && client.player.getGameProfile().getName().equalsIgnoreCase(sender)) {
            return;
        }

        String lower = body.toLowerCase(Locale.ROOT);
        for (String keyword : KEYWORDS) {
            if (lower.contains(keyword)) {
                suspiciousCounts.merge(sender.toLowerCase(Locale.ROOT), 1, Integer::sum);
                return;
            }
        }
    }

    public int getSuspiciousCount(String nick) {
        return suspiciousCounts.getOrDefault(nick.toLowerCase(Locale.ROOT), 0);
    }

    public int getTrapChance(String nick) {
        return switch (getSuspiciousCount(nick)) {
            case 0 -> 0;
            case 1 -> 25;
            case 2 -> 60;
            default -> 95;
        };
    }

    public void clear() {
        suspiciousCounts.clear();
    }
}

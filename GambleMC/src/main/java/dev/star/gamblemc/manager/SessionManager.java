package dev.star.gamblemc.manager;

import dev.star.gamblemc.GambleMC;
import dev.star.gamblemc.game.GameSession;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SessionManager {

    private final GambleMC plugin;
    private final Map<UUID, GameSession> activeSessions = new HashMap<>();
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    public SessionManager(GambleMC plugin) {
        this.plugin = plugin;
    }

    public boolean hasSession(Player player) {
        return activeSessions.containsKey(player.getUniqueId());
    }

    public GameSession getSession(Player player) {
        return activeSessions.get(player.getUniqueId());
    }

    public void addSession(Player player, GameSession session) {
        activeSessions.put(player.getUniqueId(), session);
    }

    public void removeSession(Player player) {
        GameSession session = activeSessions.remove(player.getUniqueId());
        if (session != null) {
            session.cleanup();
        }
        // Apply cooldown
        int cooldownSecs = plugin.getConfig().getInt("cooldown-seconds", 5);
        if (cooldownSecs > 0) {
            cooldowns.put(player.getUniqueId(), System.currentTimeMillis() + (cooldownSecs * 1000L));
        }
    }

    public boolean isOnCooldown(Player player) {
        if (player.hasPermission("gamblemc.bypass.cooldown")) return false;
        Long end = cooldowns.get(player.getUniqueId());
        if (end == null) return false;
        return System.currentTimeMillis() < end;
    }

    public long getRemainingCooldown(Player player) {
        Long end = cooldowns.get(player.getUniqueId());
        if (end == null) return 0;
        long remaining = end - System.currentTimeMillis();
        return remaining > 0 ? (remaining / 1000) + 1 : 0;
    }

    public Collection<GameSession> getAllSessions() {
        return activeSessions.values();
    }

    public void closeAll() {
        for (GameSession session : activeSessions.values()) {
            session.cleanup();
        }
        activeSessions.clear();
    }
}

package dev.star.gamblemc.manager;

import dev.star.gamblemc.GambleMC;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerDataManager {

    private final GambleMC plugin;
    private final File dataFile;
    private FileConfiguration dataConfig;

    // In-memory cache
    private final Map<UUID, PlayerStats> statsCache = new HashMap<>();

    public PlayerDataManager(GambleMC plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "playerdata.yml");
        loadData();
    }

    private void loadData() {
        if (!dataFile.exists()) {
            try {
                plugin.getDataFolder().mkdirs();
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create playerdata.yml: " + e.getMessage());
            }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
    }

    public PlayerStats getStats(Player player) {
        return getStats(player.getUniqueId());
    }

    public PlayerStats getStats(UUID uuid) {
        return statsCache.computeIfAbsent(uuid, id -> {
            String path = id.toString();
            int wins = dataConfig.getInt(path + ".wins", 0);
            int losses = dataConfig.getInt(path + ".losses", 0);
            double totalWon = dataConfig.getDouble(path + ".totalWon", 0.0);
            double totalLost = dataConfig.getDouble(path + ".totalLost", 0.0);
            return new PlayerStats(wins, losses, totalWon, totalLost);
        });
    }

    public void recordWin(Player player, double amount) {
        PlayerStats stats = getStats(player);
        stats.wins++;
        stats.totalWon += amount;
    }

    public void recordLoss(Player player, double amount) {
        PlayerStats stats = getStats(player);
        stats.losses++;
        stats.totalLost += amount;
    }

    /** Admin override: set wins directly */
    public void setWins(UUID uuid, int wins) {
        getStats(uuid).wins = wins;
    }

    /** Admin override: set losses directly */
    public void setLosses(UUID uuid, int losses) {
        getStats(uuid).losses = losses;
    }

    /** Admin override: add wins */
    public void addWins(UUID uuid, int amount) {
        getStats(uuid).wins += amount;
    }

    /** Admin override: add losses */
    public void addLosses(UUID uuid, int amount) {
        getStats(uuid).losses += amount;
    }

    public void saveAll() {
        for (Map.Entry<UUID, PlayerStats> entry : statsCache.entrySet()) {
            String path = entry.getKey().toString();
            PlayerStats stats = entry.getValue();
            dataConfig.set(path + ".wins", stats.wins);
            dataConfig.set(path + ".losses", stats.losses);
            dataConfig.set(path + ".totalWon", stats.totalWon);
            dataConfig.set(path + ".totalLost", stats.totalLost);
        }
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save playerdata.yml: " + e.getMessage());
        }
    }

    public static class PlayerStats {
        public int wins;
        public int losses;
        public double totalWon;
        public double totalLost;

        public PlayerStats(int wins, int losses, double totalWon, double totalLost) {
            this.wins = wins;
            this.losses = losses;
            this.totalWon = totalWon;
            this.totalLost = totalLost;
        }

        public int getTotalGames() {
            return wins + losses;
        }

        public double getWinRate() {
            if (getTotalGames() == 0) return 0.0;
            return (double) wins / getTotalGames() * 100.0;
        }
    }
}

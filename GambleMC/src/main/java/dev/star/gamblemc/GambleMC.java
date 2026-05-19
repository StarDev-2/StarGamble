package dev.star.gamblemc;

import dev.star.gamblemc.command.GambleAdminCommand;
import dev.star.gamblemc.command.GambleCommand;
import dev.star.gamblemc.listener.GUIListener;
import dev.star.gamblemc.manager.EconomyManager;
import dev.star.gamblemc.manager.PlayerDataManager;
import dev.star.gamblemc.manager.SessionManager;
import org.bukkit.plugin.java.JavaPlugin;

public class GambleMC extends JavaPlugin {

    private static GambleMC instance;
    private EconomyManager economyManager;
    private PlayerDataManager playerDataManager;
    private SessionManager sessionManager;

    @Override
    public void onEnable() {
        instance = this;

        // Save default config
        saveDefaultConfig();

        // Initialize managers
        economyManager = new EconomyManager(this);
        playerDataManager = new PlayerDataManager(this);
        sessionManager = new SessionManager(this);

        // Setup Vault
        if (!economyManager.setupEconomy()) {
            getLogger().warning("Vault not found or no economy plugin detected! Economy features will be disabled.");
        } else {
            getLogger().info("Vault economy hooked successfully.");
        }

        // Register commands
        getCommand("gamble").setExecutor(new GambleCommand(this));
        getCommand("gambleadmin").setExecutor(new GambleAdminCommand(this));

        // Register listeners
        getServer().getPluginManager().registerEvents(new GUIListener(this), this);

        getLogger().info("GambleMC v" + getDescription().getVersion() + " enabled!");
    }

    @Override
    public void onDisable() {
        // Close all open sessions
        if (sessionManager != null) {
            sessionManager.closeAll();
        }
        if (playerDataManager != null) {
            playerDataManager.saveAll();
        }
        getLogger().info("GambleMC disabled.");
    }

    public static GambleMC getInstance() {
        return instance;
    }

    public EconomyManager getEconomyManager() {
        return economyManager;
    }

    public PlayerDataManager getPlayerDataManager() {
        return playerDataManager;
    }

    public SessionManager getSessionManager() {
        return sessionManager;
    }
}

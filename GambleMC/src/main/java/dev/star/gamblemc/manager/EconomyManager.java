package dev.star.gamblemc.manager;

import dev.star.gamblemc.GambleMC;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

public class EconomyManager {

    private final GambleMC plugin;
    private Economy economy;
    private boolean enabled = false;

    public EconomyManager(GambleMC plugin) {
        this.plugin = plugin;
    }

    public boolean setupEconomy() {
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = plugin.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) return false;
        economy = rsp.getProvider();
        enabled = economy != null;
        return enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public double getBalance(Player player) {
        if (!enabled) return Double.MAX_VALUE;
        return economy.getBalance(player);
    }

    public boolean hasEnough(Player player, double amount) {
        if (!enabled) return true;
        return economy.has(player, amount);
    }

    public void deposit(Player player, double amount) {
        if (!enabled) return;
        economy.depositPlayer(player, amount);
    }

    public void withdraw(Player player, double amount) {
        if (!enabled) return;
        economy.withdrawPlayer(player, amount);
    }

    /**
     * Attempt to place a bet. Returns false if not enough money.
     */
    public boolean placeBet(Player player, double amount) {
        if (!hasEnough(player, amount)) return false;
        withdraw(player, amount);
        return true;
    }

    public String formatAmount(double amount) {
        String symbol = plugin.getConfig().getString("currency-symbol", "$");
        if (amount == (long) amount) {
            return symbol + String.format("%,d", (long) amount);
        }
        return symbol + String.format("%,.2f", amount);
    }

    public Economy getEconomy() {
        return economy;
    }
}

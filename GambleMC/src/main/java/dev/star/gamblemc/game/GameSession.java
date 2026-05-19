package dev.star.gamblemc.game;

import dev.star.gamblemc.GambleMC;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;

public abstract class GameSession {

    protected final GambleMC plugin;
    protected final Player player;
    protected double bet;
    protected Inventory inventory;
    protected final List<BukkitTask> tasks = new ArrayList<>();
    protected boolean finished = false;

    public GameSession(GambleMC plugin, Player player, double bet) {
        this.plugin = plugin;
        this.player = player;
        this.bet = bet;
    }

    /** Build and open the GUI */
    public abstract void open();

    /** Handle a click at a given slot */
    public abstract void handleClick(int slot);

    /** Clean up tasks and close inventory */
    public void cleanup() {
        for (BukkitTask task : tasks) {
            if (!task.isCancelled()) task.cancel();
        }
        tasks.clear();
        finished = true;
    }

    public Inventory getInventory() {
        return inventory;
    }

    public Player getPlayer() {
        return player;
    }

    public double getBet() {
        return bet;
    }

    public boolean isFinished() {
        return finished;
    }

    /** Pay the player winnings */
    protected void payout(double amount) {
        plugin.getEconomyManager().deposit(player, amount);
        plugin.getPlayerDataManager().recordWin(player, amount - bet);
        player.sendMessage(plugin.getEconomyManager().formatAmount(amount));
    }

    /** Record a loss (bet was already deducted) */
    protected void recordLoss() {
        plugin.getPlayerDataManager().recordLoss(player, bet);
    }

    protected void scheduleTask(BukkitTask task) {
        tasks.add(task);
    }
}

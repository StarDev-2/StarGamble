package dev.star.gamblemc.gui;

import dev.star.gamblemc.GambleMC;
import dev.star.gamblemc.util.ItemUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class MainMenuGUI {

    // Game type slots
    public static final int COINFLIP_SLOT = 11;
    public static final int SLOTS_SLOT = 13;
    public static final int HIGHER_LOWER_SLOT = 15;
    public static final int BLACKJACK_SLOT = 22;
    public static final int STATS_SLOT = 31;
    public static final int CLOSE_SLOT = 49;

    public static Inventory build(GambleMC plugin, Player player) {
        Inventory inv = Bukkit.createInventory(null, 54,
                Component.text("🎰 GambleMC — Choose Your Game").color(NamedTextColor.GOLD));

        // Fill with black glass
        for (int i = 0; i < 54; i++) inv.setItem(i, ItemUtil.fillerBlack());

        // Decorative gold border (top and bottom rows)
        for (int i = 0; i <= 8; i++) inv.setItem(i, ItemUtil.make(Material.GOLD_BLOCK, " "));
        for (int i = 45; i <= 53; i++) inv.setItem(i, ItemUtil.make(Material.GOLD_BLOCK, " "));
        // Side columns
        for (int r = 1; r <= 4; r++) {
            inv.setItem(r * 9, ItemUtil.make(Material.YELLOW_STAINED_GLASS_PANE, " "));
            inv.setItem(r * 9 + 8, ItemUtil.make(Material.YELLOW_STAINED_GLASS_PANE, " "));
        }

        // ── COINFLIP ──
        inv.setItem(COINFLIP_SLOT, ItemUtil.make(Material.GOLD_NUGGET,
                "&6&l🪙 COIN FLIP",
                "",
                "&7Flip a coin and guess heads or tails!",
                "&7Win &a2x &7your bet.",
                "",
                "&eClick to play!"));

        // ── SLOTS ──
        inv.setItem(SLOTS_SLOT, ItemUtil.make(Material.LEVER,
                "&d&l🎰 SLOTS",
                "",
                "&74 spinning reels of fortune!",
                "&7Match symbols to win big.",
                "&7Jackpot: &b50x &7your bet!",
                "",
                "&eClick to play!"));

        // ── HIGHER OR LOWER ──
        inv.setItem(HIGHER_LOWER_SLOT, ItemUtil.make(Material.COMPARATOR,
                "&b&l📊 HIGHER OR LOWER",
                "",
                "&7Guess if the next card is",
                "&7higher or lower than the current!",
                "&7Survive " + plugin.getConfig().getInt("higher-lower.rounds", 5) + " rounds to win &a2x&7!",
                "",
                "&eClick to play!"));

        // ── BLACKJACK ──
        inv.setItem(BLACKJACK_SLOT, ItemUtil.make(Material.NETHER_STAR,
                "&c&l♠ BLACKJACK",
                "",
                "&7Classic Blackjack vs. the Dealer.",
                "&7Get closest to 21 without busting!",
                "&7Natural Blackjack: &a2.5x &7your bet.",
                "",
                "&eClick to play!"));

        // ── STATS ──
        var stats = plugin.getPlayerDataManager().getStats(player);
        double balance = plugin.getEconomyManager().getBalance(player);
        inv.setItem(STATS_SLOT, ItemUtil.make(Material.BOOK,
                "&a&lYour Stats",
                "&7Balance: &e" + plugin.getEconomyManager().formatAmount(balance),
                "&7Wins: &a" + stats.wins,
                "&7Losses: &c" + stats.losses,
                "&7Win Rate: &e" + String.format("%.1f", stats.getWinRate()) + "%",
                "&7Total Won: &a+" + plugin.getEconomyManager().formatAmount(stats.totalWon),
                "&7Total Lost: &c-" + plugin.getEconomyManager().formatAmount(stats.totalLost)));

        // ── CLOSE ──
        inv.setItem(CLOSE_SLOT, ItemUtil.make(Material.BARRIER,
                "&c&lClose",
                "&7Click to close the menu"));

        return inv;
    }
}

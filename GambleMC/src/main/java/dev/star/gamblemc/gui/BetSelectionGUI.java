package dev.star.gamblemc.gui;

import dev.star.gamblemc.GambleMC;
import dev.star.gamblemc.util.ItemUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.List;

public class BetSelectionGUI {

    public enum GameType {
        COINFLIP, SLOTS, HIGHER_LOWER, BLACKJACK
    }

    // Bet amount slots (positions in the GUI)
    public static final int[] BET_SLOTS = {10, 11, 12, 13, 14, 15, 16};
    public static final double[] BET_AMOUNTS = {10, 50, 100, 500, 1000, 5000, 10000};
    public static final int CUSTOM_BET_SLOT = 22; // player types in chat
    public static final int BACK_SLOT = 49;

    public static Inventory build(GambleMC plugin, Player player, GameType gameType) {
        double min = plugin.getConfig().getDouble("min-bet", 10.0);
        double max = plugin.getConfig().getDouble("max-bet", 100000.0);
        double balance = plugin.getEconomyManager().getBalance(player);

        String gameName = switch (gameType) {
            case COINFLIP -> "&6🪙 Coin Flip";
            case SLOTS -> "&d🎰 Slots";
            case HIGHER_LOWER -> "&b📊 Higher/Lower";
            case BLACKJACK -> "&c♠ Blackjack";
        };

        Inventory inv = Bukkit.createInventory(null, 54,
                Component.text("💰 Place Your Bet — " + net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
                        .legacyAmpersand().serialize(ItemUtil.color(gameName)).replaceAll("§.", "")));

        for (int i = 0; i < 54; i++) inv.setItem(i, ItemUtil.fillerBlack());

        // Header info
        inv.setItem(4, ItemUtil.make(Material.GOLD_NUGGET,
                "&6Select Your Bet",
                "&7Balance: &e" + plugin.getEconomyManager().formatAmount(balance),
                "&7Min: &e" + plugin.getEconomyManager().formatAmount(min),
                "&7Max: &e" + plugin.getEconomyManager().formatAmount(max)));

        // Preset bets
        Material[] mats = {
                Material.IRON_NUGGET,
                Material.IRON_INGOT,
                Material.GOLD_INGOT,
                Material.GOLD_BLOCK,
                Material.DIAMOND,
                Material.DIAMOND_BLOCK,
                Material.NETHERITE_INGOT
        };

        for (int i = 0; i < BET_AMOUNTS.length; i++) {
            double amount = BET_AMOUNTS[i];
            boolean canAfford = balance >= amount && amount >= min && amount <= max;
            boolean outOfRange = amount < min || amount > max;

            String status = outOfRange ? "&8(Out of range)" : canAfford ? "&aClick to select" : "&cNot enough money";
            Material mat = canAfford ? mats[i] : Material.RED_STAINED_GLASS_PANE;

            inv.setItem(BET_SLOTS[i], ItemUtil.make(mat,
                    (canAfford ? "&e" : "&8") + plugin.getEconomyManager().formatAmount(amount),
                    "&7Bet amount",
                    status));
        }

        // Custom bet
        inv.setItem(CUSTOM_BET_SLOT, ItemUtil.make(Material.WRITABLE_BOOK,
                "&a&l✏ Custom Bet",
                "&7Click to type a custom bet amount",
                "&7Range: &e" + plugin.getEconomyManager().formatAmount(min) +
                        " &7- &e" + plugin.getEconomyManager().formatAmount(max)));

        // Back button
        inv.setItem(BACK_SLOT, ItemUtil.make(Material.ARROW,
                "&c&l← Back",
                "&7Return to game selection"));

        return inv;
    }
}

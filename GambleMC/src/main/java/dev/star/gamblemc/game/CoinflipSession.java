package dev.star.gamblemc.game;

import dev.star.gamblemc.GambleMC;
import dev.star.gamblemc.util.ItemUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.Random;

public class CoinflipSession extends GameSession {

    private static final int[] COIN_SLOTS = {10, 11, 12, 13, 14, 15, 16};
    private static final int HEADS_SLOT = 13; // center
    private static final int PICK_HEADS = 39;
    private static final int PICK_TAILS = 41;
    private static final int BET_INFO_SLOT = 4;

    private boolean playerChoseHeads = false;
    private boolean animating = false;
    private boolean resultShown = false;
    private int animTick = 0;
    private final Random random = new Random();

    // Items
    private final ItemStack HEADS_ITEM = ItemUtil.make(Material.PLAYER_HEAD, "&6&lHEADS", "&7The golden coin face");
    private final ItemStack TAILS_ITEM = ItemUtil.make(Material.FEATHER, "&f&lTAILS", "&7The feather side");
    private final ItemStack GLASS_GOLD = ItemUtil.make(Material.GOLD_BLOCK, " ");
    private final ItemStack GLASS_WHITE = ItemUtil.make(Material.WHITE_STAINED_GLASS_PANE, " ");
    private final ItemStack FILLER = ItemUtil.filler();

    public CoinflipSession(GambleMC plugin, Player player, double bet) {
        super(plugin, player, bet);
    }

    @Override
    public void open() {
        inventory = Bukkit.createInventory(null, 54,
                Component.text("⚡ Coin Flip — Bet: " + plugin.getEconomyManager().formatAmount(bet)).color(NamedTextColor.GOLD));

        // Fill with filler
        for (int i = 0; i < 54; i++) inventory.setItem(i, FILLER);

        // Bet info
        inventory.setItem(BET_INFO_SLOT, ItemUtil.make(Material.GOLD_NUGGET,
                "&6Your Bet: " + plugin.getEconomyManager().formatAmount(bet),
                "&7Win: &a" + plugin.getEconomyManager().formatAmount(bet * 2),
                "&7Pick a side below!"));

        // Coin display area (top rows) — starts as heads
        setCoinDisplay(true);

        // Choice buttons
        inventory.setItem(PICK_HEADS, ItemUtil.make(Material.PLAYER_HEAD,
                "&6&l[ HEADS ]",
                "&7Click to bet on HEADS",
                "&e→ Win 2x your bet!"));
        inventory.setItem(PICK_TAILS, ItemUtil.make(Material.FEATHER,
                "&f&l[ TAILS ]",
                "&7Click to bet on TAILS",
                "&e→ Win 2x your bet!"));

        // Decorative gold border
        inventory.setItem(40, ItemUtil.make(Material.GOLD_BLOCK, " "));

        player.openInventory(inventory);
    }

    private void setCoinDisplay(boolean showHeads) {
        ItemStack coin = showHeads ? HEADS_ITEM : TAILS_ITEM;
        ItemStack border = showHeads
                ? ItemUtil.make(Material.YELLOW_STAINED_GLASS_PANE, " ")
                : ItemUtil.make(Material.WHITE_STAINED_GLASS_PANE, " ");

        // Clear row 1 and 2 (slots 0-26)
        for (int i = 0; i <= 26; i++) inventory.setItem(i, FILLER);

        // Border glow around center
        int[] borderSlots = {3, 4, 5, 12, 14, 21, 22, 23};
        for (int s : borderSlots) inventory.setItem(s, border);

        // Center coin
        inventory.setItem(13, coin);
        // Side decorations
        inventory.setItem(11, ItemUtil.make(Material.GOLD_NUGGET, " "));
        inventory.setItem(15, ItemUtil.make(Material.GOLD_NUGGET, " "));
    }

    @Override
    public void handleClick(int slot) {
        if (animating || resultShown) return;

        if (slot == PICK_HEADS) {
            playerChoseHeads = true;
            startFlip();
        } else if (slot == PICK_TAILS) {
            playerChoseHeads = false;
            startFlip();
        }
    }

    private void startFlip() {
        animating = true;
        animTick = 0;

        // Determine result now (random)
        boolean resultIsHeads = random.nextBoolean();

        // Status label
        inventory.setItem(BET_INFO_SLOT, ItemUtil.make(Material.CLOCK,
                "&e&lFlipping...",
                "&7You chose: " + (playerChoseHeads ? "&6Heads" : "&fTails")));

        // Hide choice buttons while animating
        inventory.setItem(PICK_HEADS, FILLER);
        inventory.setItem(PICK_TAILS, FILLER);

        int totalTicks = 60; // 3 seconds of animation
        BukkitTask flipTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            animTick++;
            boolean showHeads = (animTick % 2 == 0);
            setCoinDisplay(showHeads);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 0.8f + (animTick * 0.02f));

            if (animTick >= totalTicks) {
                // Show final result
                setCoinDisplay(resultIsHeads);
                showResult(resultIsHeads);
                animating = false;
                resultShown = true;
            }
        }, 2L, 2L);

        tasks.add(flipTask);
    }

    private void showResult(boolean landedHeads) {
        boolean playerWon = (playerChoseHeads == landedHeads);
        String coinName = landedHeads ? "&6Heads" : "&fTails";
        String choiceName = playerChoseHeads ? "&6Heads" : "&fTails";

        if (playerWon) {
            double winAmount = bet * 2.0;
            payout(winAmount);
            inventory.setItem(BET_INFO_SLOT, ItemUtil.make(Material.DIAMOND,
                    "&a&l✔ YOU WIN!",
                    "&7Coin: " + coinName,
                    "&7You chose: " + choiceName,
                    "&aWon: &2+" + plugin.getEconomyManager().formatAmount(winAmount - bet)));
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        } else {
            recordLoss();
            inventory.setItem(BET_INFO_SLOT, ItemUtil.make(Material.BARRIER,
                    "&c&l✘ YOU LOSE!",
                    "&7Coin: " + coinName,
                    "&7You chose: " + choiceName,
                    "&cLost: &4-" + plugin.getEconomyManager().formatAmount(bet)));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 0.8f);
        }

        // Close button
        inventory.setItem(49, ItemUtil.make(Material.RED_BED, "&c&lClose", "&7Click to exit"));

        // Auto-close after 5 seconds
        BukkitTask closeTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            plugin.getSessionManager().removeSession(player);
            player.closeInventory();
        }, 100L);
        tasks.add(closeTask);
    }
}

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

import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class SlotsSession extends GameSession {

    /*
     * 54-slot chest layout (6 rows x 9 cols):
     *
     * Row 0 (0-8):   border
     * Row 1 (9-17):  [border][col0_r0][border][col1_r0][border][col2_r0][border][col3_r0][border]
     * Row 2 (18-26): [border][col0_r1][border][col1_r1][border][col2_r1][border][col3_r1][border]
     * Row 3 (27-35): [border][col0_r2][border][col1_r2][border][col2_r2][border][col3_r2][border]
     * Row 4 (36-44): border/buttons
     * Row 5 (45-53): border/info
     *
     * 4 columns × 3 rows of visible reel slots = 12 spinning items
     * Column slots: col0=[10,19,28], col1=[12,21,30], col2=[14,23,32], col3=[16,25,34]
     */

    // Reel column slot indices (row top→bottom)
    private static final int[][] REEL_SLOTS = {
            {10, 19, 28},
            {12, 21, 30},
            {14, 23, 32},
            {16, 25, 34}
    };

    private static final int SPIN_BUTTON = 49;
    private static final int BET_SLOT = 4;

    // Symbol definitions: [material, display name, multiplier key]
    public enum Symbol {
        SEVEN(Material.DIAMOND_BLOCK, "&b&l✦ 777 ✦", 50.0),
        BAR(Material.GOLD_BLOCK, "&6&l▬ BAR ▬", 10.0),
        CHERRY(Material.RED_WOOL, "&c&l❤ CHERRY", 5.0),
        LEMON(Material.YELLOW_WOOL, "&e&l✿ LEMON", 3.0),
        WATERMELON(Material.GREEN_WOOL, "&a&l⬟ MELON", 2.0),
        BELL(Material.ORANGE_WOOL, "&6&l♪ BELL", 1.5);

        public final Material mat;
        public final String label;
        public final double multiplier;

        Symbol(Material mat, String label, double mult) {
            this.mat = mat;
            this.label = label;
            this.multiplier = mult;
        }
    }

    // Weighted reel pool (more commons)
    private static final Symbol[] REEL_POOL = {
            Symbol.SEVEN,
            Symbol.BAR, Symbol.BAR,
            Symbol.CHERRY, Symbol.CHERRY, Symbol.CHERRY,
            Symbol.LEMON, Symbol.LEMON, Symbol.LEMON, Symbol.LEMON,
            Symbol.WATERMELON, Symbol.WATERMELON, Symbol.WATERMELON, Symbol.WATERMELON, Symbol.WATERMELON,
            Symbol.BELL, Symbol.BELL, Symbol.BELL, Symbol.BELL, Symbol.BELL, Symbol.BELL
    };

    private final Random random = new Random();
    private boolean spinning = false;
    private boolean hasSpun = false;

    // Final symbols for each column (determined at spin start)
    private final Symbol[] finalSymbols = new Symbol[4];

    // Current display symbol for each column (for animation)
    private final Symbol[] displaySymbols = new Symbol[4];
    private final int[] spinTicksRemaining = {0, 0, 0, 0};

    public SlotsSession(GambleMC plugin, Player player, double bet) {
        super(plugin, player, bet);
        for (int i = 0; i < 4; i++) displaySymbols[i] = randomSymbol();
    }

    @Override
    public void open() {
        inventory = Bukkit.createInventory(null, 54,
                Component.text("🎰 Slots — Bet: " + plugin.getEconomyManager().formatAmount(bet)).color(NamedTextColor.DARK_PURPLE));

        // Fill all with dark border
        for (int i = 0; i < 54; i++) inventory.setItem(i, ItemUtil.fillerBlack());

        // Draw separators between columns (odd columns: 9,11,13,15,17 in each reel row)
        int[] separatorCols = {9, 11, 13, 15, 17};
        for (int row = 1; row <= 3; row++) {
            for (int col : separatorCols) {
                inventory.setItem(row * 9 + col, ItemUtil.make(Material.PURPLE_STAINED_GLASS_PANE, " "));
            }
        }

        // Draw initial reel state
        renderReels();

        // Bet info
        inventory.setItem(BET_SLOT, ItemUtil.make(Material.GOLD_NUGGET,
                "&6Bet: " + plugin.getEconomyManager().formatAmount(bet),
                "&7Match 4-of-a-kind to win!",
                "&7Match 3-of-a-kind for partial win.",
                "",
                "&e✦ 4x SEVEN = " + Symbol.SEVEN.multiplier + "x",
                "&6▬ 4x BAR = " + Symbol.BAR.multiplier + "x",
                "&c❤ 4x CHERRY = " + Symbol.CHERRY.multiplier + "x",
                "&e✿ 4x LEMON = " + Symbol.LEMON.multiplier + "x",
                "&a⬟ 4x MELON = " + Symbol.WATERMELON.multiplier + "x",
                "&6♪ 4x BELL = " + Symbol.BELL.multiplier + "x"));

        // Spin button
        inventory.setItem(SPIN_BUTTON, ItemUtil.make(Material.LEVER,
                "&a&l▶ SPIN! ◀",
                "&7Click to spin the reels!",
                "&eBet: " + plugin.getEconomyManager().formatAmount(bet)));

        // Payout guide slot
        inventory.setItem(45, ItemUtil.make(Material.BOOK, "&e&lPayout Guide",
                "&73-of-a-kind → 0.5x multiplier",
                "&74-of-a-kind → full multiplier"));

        player.openInventory(inventory);
    }

    private void renderReels() {
        for (int col = 0; col < 4; col++) {
            Symbol sym = displaySymbols[col];
            // Middle row = the "payline" — show symbol clearly
            ItemStack item = ItemUtil.make(sym.mat, sym.label, "&7Column " + (col + 1));
            ItemStack dimItem = ItemUtil.make(sym.mat, "&8" + net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
                    .legacyAmpersand().serialize(ItemUtil.color(sym.label)).replaceAll("§.", ""), "&8Spinning...");

            // Top and bottom rows slightly dimmed
            inventory.setItem(REEL_SLOTS[col][0], dimItem);
            inventory.setItem(REEL_SLOTS[col][1], item);  // center = payline
            inventory.setItem(REEL_SLOTS[col][2], dimItem);
        }
    }

    @Override
    public void handleClick(int slot) {
        if (slot == SPIN_BUTTON && !spinning && !hasSpun) {
            startSpin();
        } else if (slot == 49 && hasSpun) {
            plugin.getSessionManager().removeSession(player);
            player.closeInventory();
        }
    }

    private void startSpin() {
        spinning = true;
        hasSpun = true;

        // Determine final results now
        for (int i = 0; i < 4; i++) finalSymbols[i] = randomSymbol();

        // Set spin durations per column (stagger stop times)
        spinTicksRemaining[0] = 30;
        spinTicksRemaining[1] = 45;
        spinTicksRemaining[2] = 60;
        spinTicksRemaining[3] = 75;

        // Gray out spin button
        inventory.setItem(SPIN_BUTTON, ItemUtil.make(Material.BARRIER, "&c&lSpinning...", "&7Please wait..."));

        // Info
        inventory.setItem(BET_SLOT, ItemUtil.make(Material.CLOCK, "&e&lSpinning!", "&7Good luck!"));

        BukkitTask spinTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            boolean anyStillSpinning = false;

            for (int col = 0; col < 4; col++) {
                if (spinTicksRemaining[col] > 0) {
                    anyStillSpinning = true;
                    spinTicksRemaining[col]--;
                    displaySymbols[col] = randomSymbol();
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 0.3f, 1.0f + (col * 0.15f));
                } else {
                    // Column has stopped — show final symbol
                    displaySymbols[col] = finalSymbols[col];
                }
            }

            renderReels();

            if (!anyStillSpinning) {
                // All stopped
                for (BukkitTask t : tasks) t.cancel();
                tasks.clear();
                spinning = false;
                evaluateResult();
            }
        }, 1L, 3L);

        tasks.add(spinTask);
    }

    private Symbol randomSymbol() {
        return REEL_POOL[random.nextInt(REEL_POOL.length)];
    }

    private void evaluateResult() {
        // Count matching symbols
        java.util.Map<Symbol, Integer> counts = new java.util.HashMap<>();
        for (Symbol s : finalSymbols) counts.merge(s, 1, Integer::sum);

        int bestMatch = 0;
        Symbol bestSymbol = null;
        for (java.util.Map.Entry<Symbol, Integer> e : counts.entrySet()) {
            if (e.getValue() > bestMatch) {
                bestMatch = e.getValue();
                bestSymbol = e.getKey();
            }
        }

        double multiplier = 0.0;
        String resultMsg;

        if (bestMatch == 4) {
            // 4-of-a-kind jackpot!
            multiplier = bestSymbol.multiplier;
            resultMsg = "&a&l★ JACKPOT! 4-of-a-Kind! ★";
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
        } else if (bestMatch == 3) {
            // 3-of-a-kind — half multiplier
            multiplier = bestSymbol.multiplier * 0.5;
            resultMsg = "&e&l3-of-a-Kind!";
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        } else {
            resultMsg = "&c&lNo Match — Better luck next time!";
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 0.8f);
        }

        if (multiplier > 0.0) {
            double winAmount = bet * multiplier;
            payout(winAmount);
            inventory.setItem(BET_SLOT, ItemUtil.make(Material.DIAMOND,
                    resultMsg,
                    "&7Multiplier: &e" + multiplier + "x",
                    "&aWon: &2+" + plugin.getEconomyManager().formatAmount(winAmount)));
        } else {
            recordLoss();
            inventory.setItem(BET_SLOT, ItemUtil.make(Material.BARRIER,
                    resultMsg,
                    "&cLost: &4-" + plugin.getEconomyManager().formatAmount(bet)));
        }

        // Highlight winning columns
        if (bestMatch >= 3 && bestSymbol != null) {
            Symbol winSym = bestSymbol;
            for (int col = 0; col < 4; col++) {
                if (finalSymbols[col] == winSym) {
                    // Light up the payline slot
                    inventory.setItem(REEL_SLOTS[col][1], ItemUtil.make(
                            finalSymbols[col].mat,
                            "&a&l★ " + net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
                                    .legacyAmpersand().serialize(ItemUtil.color(finalSymbols[col].label)).replaceAll("§.", "") + " ★",
                            "&aWINNER!"
                    ));
                }
            }
        }

        inventory.setItem(SPIN_BUTTON, ItemUtil.make(Material.RED_BED, "&c&lClose", "&7Click to exit"));

        // Auto-close after 6s
        BukkitTask closeTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            plugin.getSessionManager().removeSession(player);
            player.closeInventory();
        }, 120L);
        tasks.add(closeTask);
    }
}

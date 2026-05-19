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

public class HigherLowerSession extends GameSession {

    /*
     * Layout:
     *
     * Row 0 (0-8):   progress / history cards (previous rounds)
     * Row 1 (9-17):  [filler] [current card big] [filler]
     * Row 2 (18-26): [filler] [current card] [filler]  <- visual
     * Row 3 (27-35): [filler] [?? next card ??] [filler]
     * Row 4 (36-44): [HIGHER button] [filler] [LOWER button]
     * Row 5 (45-53): [bet info] [round info] [streak]
     *
     * HIGHER: slot 38
     * LOWER:  slot 42
     * Current card: slot 13
     * Next card (hidden): slot 31
     * Bet info: slot 47
     * Round counter: slot 49
     */

    private static final int HIGHER_SLOT = 38;
    private static final int LOWER_SLOT = 42;
    private static final int CURRENT_CARD_SLOT = 13;
    private static final int NEXT_CARD_SLOT = 31;
    private static final int BET_INFO_SLOT = 47;
    private static final int ROUND_SLOT = 49;
    private static final int STREAK_SLOT = 51;
    private static final int[] HISTORY_SLOTS = {0, 1, 2, 3, 4, 5, 6, 7, 8};

    private final int maxRounds;
    private int currentRound = 0;
    private int currentCard;
    private int nextCard;
    private int streak = 0;
    private boolean waitingForReveal = false;
    private boolean playerGuessedHigher;

    private final int[] history; // stores past cards
    private final Random random = new Random();

    public HigherLowerSession(GambleMC plugin, Player player, double bet) {
        super(plugin, player, bet);
        this.maxRounds = plugin.getConfig().getInt("higher-lower.rounds", 5);
        this.history = new int[maxRounds];
        this.currentCard = randomCard();
        this.nextCard = randomCard();
    }

    private int randomCard() {
        return random.nextInt(13) + 1; // 1-13 (Ace-King)
    }

    private String cardName(int value) {
        return switch (value) {
            case 1 -> "ACE";
            case 11 -> "JACK";
            case 12 -> "QUEEN";
            case 13 -> "KING";
            default -> String.valueOf(value);
        };
    }

    private Material cardMaterial(int value) {
        // Color gradient: low = blue/green, high = red/orange
        if (value <= 2) return Material.BLUE_WOOL;
        if (value <= 4) return Material.CYAN_WOOL;
        if (value <= 6) return Material.GREEN_WOOL;
        if (value <= 8) return Material.LIME_WOOL;
        if (value <= 10) return Material.YELLOW_WOOL;
        if (value <= 11) return Material.ORANGE_WOOL;
        return Material.RED_WOOL;
    }

    @Override
    public void open() {
        inventory = Bukkit.createInventory(null, 54,
                Component.text("📊 Higher or Lower — Round 1/" + maxRounds).color(NamedTextColor.AQUA));

        buildLayout();
        player.openInventory(inventory);
    }

    private void buildLayout() {
        // Fill all with filler
        for (int i = 0; i < 54; i++) inventory.setItem(i, ItemUtil.filler());

        // Render history row
        renderHistory();

        // Current card
        renderCurrentCard();

        // Hidden next card
        inventory.setItem(NEXT_CARD_SLOT, ItemUtil.make(Material.PURPLE_WOOL,
                "&5&l?  ?  ?",
                "&7The next card is hidden",
                "&7Is it HIGHER or LOWER than " + cardName(currentCard) + "?"));

        // Action buttons
        inventory.setItem(HIGHER_SLOT, ItemUtil.make(Material.LIME_WOOL,
                "&a&l▲ HIGHER",
                "&7Guess the next card is higher",
                "&7than &e" + cardName(currentCard)));
        inventory.setItem(LOWER_SLOT, ItemUtil.make(Material.RED_WOOL,
                "&c&l▼ LOWER",
                "&7Guess the next card is lower",
                "&7than &e" + cardName(currentCard)));

        // Middle decoration
        inventory.setItem(40, ItemUtil.make(Material.COMPARATOR,
                "&7vs.",
                "&7Higher or Lower?"));

        // Info
        renderInfo();
    }

    private void renderCurrentCard() {
        Material mat = cardMaterial(currentCard);
        String name = cardName(currentCard);
        inventory.setItem(CURRENT_CARD_SLOT, ItemUtil.make(mat,
                "&f&l" + name,
                "&7Current card: &e" + name,
                "&7Value: &e" + currentCard + "&7/13",
                "",
                "&7Is the next card higher or lower?"));

        // Visual border around current card
        int[] border = {9, 10, 12, 14, 15, 16, 17, 18, 26};
        for (int s : border) {
            inventory.setItem(s, ItemUtil.make(mat, " "));
        }
        inventory.setItem(11, ItemUtil.make(mat, " "));
    }

    private void renderHistory() {
        for (int i = 0; i < Math.min(currentRound, maxRounds); i++) {
            int val = history[i];
            inventory.setItem(HISTORY_SLOTS[i], ItemUtil.make(cardMaterial(val),
                    "&7" + cardName(val),
                    "&8Round " + (i + 1)));
        }
        // Fill remaining history slots with filler
        for (int i = currentRound; i < maxRounds && i < HISTORY_SLOTS.length; i++) {
            inventory.setItem(HISTORY_SLOTS[i], ItemUtil.make(Material.LIGHT_GRAY_STAINED_GLASS_PANE,
                    "&8Round " + (i + 1)));
        }
    }

    private void renderInfo() {
        inventory.setItem(BET_INFO_SLOT, ItemUtil.make(Material.GOLD_NUGGET,
                "&6Bet: " + plugin.getEconomyManager().formatAmount(bet),
                "&7Win: &a" + plugin.getEconomyManager().formatAmount(bet * 2)));

        inventory.setItem(ROUND_SLOT, ItemUtil.make(Material.CLOCK,
                "&eRound &6" + (currentRound + 1) + "&e/&6" + maxRounds,
                "&7Correct all " + maxRounds + " to win!"));

        inventory.setItem(STREAK_SLOT, ItemUtil.make(Material.BLAZE_ROD,
                "&6Streak: &e" + streak + "/" + maxRounds,
                streak >= 3 ? "&aOn fire! 🔥" : "&7Keep going!"));
    }

    @Override
    public void handleClick(int slot) {
        if (waitingForReveal || finished) return;

        if (slot == HIGHER_SLOT) {
            playerGuessedHigher = true;
            revealNextCard();
        } else if (slot == LOWER_SLOT) {
            playerGuessedHigher = false;
            revealNextCard();
        } else if (slot == 49 && finished) {
            plugin.getSessionManager().removeSession(player);
            player.closeInventory();
        }
    }

    private void revealNextCard() {
        waitingForReveal = true;

        // Hide buttons during reveal
        inventory.setItem(HIGHER_SLOT, ItemUtil.filler());
        inventory.setItem(LOWER_SLOT, ItemUtil.filler());

        // Brief animation — flash the hidden card
        BukkitTask revealTask = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            int tick = 0;
            boolean toggle = false;

            @Override
            public void run() {
                tick++;
                toggle = !toggle;

                if (tick <= 6) {
                    // Flash animation
                    inventory.setItem(NEXT_CARD_SLOT, toggle
                            ? ItemUtil.make(Material.PURPLE_WOOL, "&5&l? ? ?", "&7Revealing...")
                            : ItemUtil.make(Material.MAGENTA_WOOL, "&d&l? ? ?", "&7Revealing..."));
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.3f, 1.0f + tick * 0.1f);
                } else {
                    // Cancel and show result
                    for (BukkitTask t : tasks) t.cancel();
                    tasks.clear();
                    showRevealResult();
                }
            }
        }, 0L, 5L);

        tasks.add(revealTask);
    }

    private void showRevealResult() {
        boolean wasHigher = (nextCard > currentCard);
        boolean wasEqual = (nextCard == currentCard);

        boolean correct;
        String resultStr;

        if (wasEqual) {
            // Tie = wrong (house edge)
            correct = false;
            resultStr = "&e&lTIE! House wins ties.";
        } else if (playerGuessedHigher && wasHigher) {
            correct = true;
            resultStr = "&a&l✔ CORRECT! It was Higher!";
        } else if (!playerGuessedHigher && !wasHigher) {
            correct = true;
            resultStr = "&a&l✔ CORRECT! It was Lower!";
        } else {
            correct = false;
            resultStr = "&c&l✘ WRONG! " + (wasHigher ? "It was Higher!" : "It was Lower!");
        }

        // Show the actual card
        inventory.setItem(NEXT_CARD_SLOT, ItemUtil.make(cardMaterial(nextCard),
                (correct ? "&a" : "&c") + "&l" + cardName(nextCard),
                "&7The next card was: &e" + cardName(nextCard),
                "",
                resultStr));

        player.playSound(player.getLocation(),
                correct ? Sound.ENTITY_PLAYER_LEVELUP : Sound.ENTITY_VILLAGER_NO,
                1.0f, correct ? 1.0f : 0.8f);

        if (correct) {
            streak++;
            history[currentRound] = currentCard;
            currentRound++;

            if (currentRound >= maxRounds) {
                // Won!
                BukkitTask winTask = Bukkit.getScheduler().runTaskLater(plugin, this::showWin, 30L);
                tasks.add(winTask);
            } else {
                // Next round
                BukkitTask nextTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    currentCard = nextCard;
                    nextCard = randomCard();
                    waitingForReveal = false;

                    // Update title/header
                    player.closeInventory();
                    inventory = Bukkit.createInventory(null, 54,
                            Component.text("📊 Higher or Lower — Round " + (currentRound + 1) + "/" + maxRounds).color(NamedTextColor.AQUA));
                    buildLayout();
                    player.openInventory(inventory);
                }, 35L);
                tasks.add(nextTask);
            }
        } else {
            // Lost
            BukkitTask loseTask = Bukkit.getScheduler().runTaskLater(plugin, this::showLoss, 30L);
            tasks.add(loseTask);
        }
    }

    private void showWin() {
        double winAmount = bet * 2.0;
        payout(winAmount);
        finished = true;

        // Update info area
        clearButtons();
        inventory.setItem(BET_INFO_SLOT, ItemUtil.make(Material.DIAMOND,
                "&a&l🎉 YOU WIN!",
                "&7Completed all " + maxRounds + " rounds!",
                "&aWon: &2+" + plugin.getEconomyManager().formatAmount(winAmount - bet)));
        inventory.setItem(49, ItemUtil.make(Material.RED_BED, "&c&lClose", "&7Click to exit"));
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);

        BukkitTask closeTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            plugin.getSessionManager().removeSession(player);
            player.closeInventory();
        }, 120L);
        tasks.add(closeTask);
    }

    private void showLoss() {
        recordLoss();
        finished = true;

        clearButtons();
        inventory.setItem(BET_INFO_SLOT, ItemUtil.make(Material.BARRIER,
                "&c&lYOU LOSE!",
                "&7Made it " + currentRound + "/" + maxRounds + " rounds.",
                "&cLost: &4-" + plugin.getEconomyManager().formatAmount(bet)));
        inventory.setItem(49, ItemUtil.make(Material.RED_BED, "&c&lClose", "&7Click to exit"));

        BukkitTask closeTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            plugin.getSessionManager().removeSession(player);
            player.closeInventory();
        }, 120L);
        tasks.add(closeTask);
    }

    private void clearButtons() {
        inventory.setItem(HIGHER_SLOT, ItemUtil.filler());
        inventory.setItem(LOWER_SLOT, ItemUtil.filler());
        inventory.setItem(40, ItemUtil.filler());
    }
}

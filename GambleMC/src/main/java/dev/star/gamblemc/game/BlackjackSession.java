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

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class BlackjackSession extends GameSession {

    /*
     * Layout (54 slots):
     *
     * Row 0: DEALER label + dealer cards (left→right from slot 1)
     * Row 1: dealer cards overflow + score
     * Row 2: separator
     * Row 3: PLAYER label + player cards
     * Row 4: player cards overflow + score
     * Row 5: HIT | STAND | DOUBLE DOWN | BET INFO
     *
     * Dealer cards: slots 1,2,3,4,5,6,7  (max 7 visible)
     * Player cards: slots 28,29,30,31,32,33,34
     * Dealer score: slot 8
     * Player score: slot 35
     * HIT: slot 45
     * STAND: slot 49
     * DOUBLE: slot 53
     */

    private static final int[] DEALER_CARD_SLOTS = {1, 2, 3, 4, 5, 6, 7};
    private static final int[] PLAYER_CARD_SLOTS = {28, 29, 30, 31, 32, 33, 34};
    private static final int DEALER_SCORE_SLOT = 8;
    private static final int PLAYER_SCORE_SLOT = 35;
    private static final int HIT_SLOT = 45;
    private static final int STAND_SLOT = 49;
    private static final int DOUBLE_SLOT = 53;
    private static final int BET_INFO_SLOT = 40;

    private final List<Integer> playerHand = new ArrayList<>();
    private final List<Integer> dealerHand = new ArrayList<>();
    private final Random random = new Random();

    private boolean gameOver = false;
    private boolean playerTurn = true;
    private boolean doubled = false;

    public BlackjackSession(GambleMC plugin, Player player, double bet) {
        super(plugin, player, bet);
    }

    @Override
    public void open() {
        inventory = Bukkit.createInventory(null, 54,
                Component.text("♠ Blackjack — Bet: " + plugin.getEconomyManager().formatAmount(bet)).color(NamedTextColor.RED));

        buildBaseLayout();

        // Deal initial cards
        playerHand.add(drawCard());
        dealerHand.add(drawCard());
        playerHand.add(drawCard());
        dealerHand.add(drawCard()); // dealer's second card hidden

        renderHands(true);
        checkNaturalBlackjack();

        player.openInventory(inventory);
    }

    private void buildBaseLayout() {
        for (int i = 0; i < 54; i++) inventory.setItem(i, ItemUtil.fillerBlack());

        // Dealer label row
        inventory.setItem(0, ItemUtil.make(Material.RED_WOOL, "&c&l🂠 DEALER", "&7Dealer's hand"));

        // Separator row (slots 18-26)
        for (int i = 18; i <= 26; i++)
            inventory.setItem(i, ItemUtil.make(Material.GREEN_STAINED_GLASS_PANE, " "));

        // Player label
        inventory.setItem(27, ItemUtil.make(Material.LIME_WOOL, "&a&l♟ YOUR HAND", "&7Your cards"));

        // Bet info
        inventory.setItem(BET_INFO_SLOT, ItemUtil.make(Material.GOLD_NUGGET,
                "&6Bet: " + plugin.getEconomyManager().formatAmount(bet),
                "&7Blackjack pays 2.5x",
                "&7Normal win pays 2x"));

        renderActionButtons();
    }

    private void renderActionButtons() {
        if (!playerTurn || gameOver) return;

        inventory.setItem(HIT_SLOT, ItemUtil.make(Material.LIME_TERRACOTTA,
                "&a&l[ HIT ]",
                "&7Draw another card"));
        inventory.setItem(STAND_SLOT, ItemUtil.make(Material.RED_TERRACOTTA,
                "&c&l[ STAND ]",
                "&7Keep your current hand"));

        // Double down only on first action (2 cards) and if can afford
        if (playerHand.size() == 2 && plugin.getEconomyManager().hasEnough(player, bet)) {
            inventory.setItem(DOUBLE_SLOT, ItemUtil.make(Material.YELLOW_TERRACOTTA,
                    "&e&l[ DOUBLE DOWN ]",
                    "&7Double bet, draw one card only",
                    "&eExtra bet: " + plugin.getEconomyManager().formatAmount(bet)));
        } else {
            inventory.setItem(DOUBLE_SLOT, ItemUtil.fillerBlack());
        }
    }

    private void renderHands(boolean hideDealer) {
        // Render dealer's hand
        for (int i = 0; i < DEALER_CARD_SLOTS.length; i++) {
            if (i < dealerHand.size()) {
                if (i == 1 && hideDealer) {
                    // Hide second dealer card
                    inventory.setItem(DEALER_CARD_SLOTS[i], ItemUtil.make(Material.PURPLE_WOOL,
                            "&5&l[ HIDDEN ]", "&7Dealer's secret card"));
                } else {
                    inventory.setItem(DEALER_CARD_SLOTS[i], cardItem(dealerHand.get(i)));
                }
            } else {
                inventory.setItem(DEALER_CARD_SLOTS[i], ItemUtil.fillerBlack());
            }
        }

        // Dealer score
        int visibleDealerScore = hideDealer ? dealerHand.get(0) : handValue(dealerHand);
        String dealerScoreStr = hideDealer ? "&7Showing: &e" + cardDisplayValue(dealerHand.get(0)) : "&7Total: &e" + visibleDealerScore;
        inventory.setItem(DEALER_SCORE_SLOT, ItemUtil.make(Material.PAPER,
                "&c&lDealer Score", dealerScoreStr));

        // Render player's hand
        for (int i = 0; i < PLAYER_CARD_SLOTS.length; i++) {
            if (i < playerHand.size()) {
                inventory.setItem(PLAYER_CARD_SLOTS[i], cardItem(playerHand.get(i)));
            } else {
                inventory.setItem(PLAYER_CARD_SLOTS[i], ItemUtil.fillerBlack());
            }
        }

        // Player score
        int playerScore = handValue(playerHand);
        String scoreColor = playerScore > 21 ? "&c" : playerScore == 21 ? "&a&l" : "&e";
        String bustStr = playerScore > 21 ? " &c&lBUST!" : "";
        inventory.setItem(PLAYER_SCORE_SLOT, ItemUtil.make(Material.PAPER,
                "&a&lYour Score",
                "&7Total: " + scoreColor + playerScore + bustStr));
    }

    private ItemStack cardItem(int value) {
        boolean isAce = (value == 11);
        return ItemUtil.numberTotem(value, isAce);
    }

    private int drawCard() {
        // Card values: 2-10 plus face cards (10) and Ace (11)
        int[] possible = {2, 3, 4, 5, 6, 7, 8, 9, 10, 10, 10, 10, 11};
        return possible[random.nextInt(possible.length)];
    }

    private int handValue(List<Integer> hand) {
        int total = 0;
        int aces = 0;
        for (int card : hand) {
            if (card == 11) aces++;
            total += card;
        }
        while (total > 21 && aces > 0) {
            total -= 10;
            aces--;
        }
        return total;
    }

    private String cardDisplayValue(int v) {
        if (v == 11) return "ACE";
        if (v == 10) return "10/J/Q/K";
        return String.valueOf(v);
    }

    private void checkNaturalBlackjack() {
        if (handValue(playerHand) == 21 && playerHand.size() == 2) {
            // Natural Blackjack!
            playerTurn = false;
            gameOver = true;
            renderHands(false);

            double winAmount = bet * 2.5;
            payout(winAmount);

            inventory.setItem(BET_INFO_SLOT, ItemUtil.make(Material.NETHER_STAR,
                    "&6&l★ BLACKJACK! ★",
                    "&aYou got a natural 21!",
                    "&aWon: &2+" + plugin.getEconomyManager().formatAmount(winAmount - bet)));
            clearActionButtons();
            inventory.setItem(49, ItemUtil.make(Material.RED_BED, "&c&lClose", "&7Click to exit"));
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.2f);
            scheduleAutoClose();
        }
    }

    @Override
    public void handleClick(int slot) {
        if (gameOver) {
            if (slot == 49) {
                plugin.getSessionManager().removeSession(player);
                player.closeInventory();
            }
            return;
        }

        if (!playerTurn) return;

        if (slot == HIT_SLOT) {
            playerHand.add(drawCard());
            player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1.0f, 1.0f);
            renderHands(true);
            renderActionButtons();

            if (handValue(playerHand) > 21) {
                // Bust
                playerTurn = false;
                gameOver = true;
                renderHands(false);
                recordLoss();
                inventory.setItem(BET_INFO_SLOT, ItemUtil.make(Material.BARRIER,
                        "&c&lBUST! You lose.",
                        "&7Your total exceeded 21.",
                        "&cLost: &4-" + plugin.getEconomyManager().formatAmount(bet)));
                clearActionButtons();
                inventory.setItem(49, ItemUtil.make(Material.RED_BED, "&c&lClose", "&7Click to exit"));
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 0.7f);
                scheduleAutoClose();
            } else if (handValue(playerHand) == 21) {
                // Auto-stand on 21
                handleStand();
            }

        } else if (slot == STAND_SLOT) {
            handleStand();

        } else if (slot == DOUBLE_SLOT && playerHand.size() == 2) {
            if (!plugin.getEconomyManager().placeBet(player, bet)) {
                player.sendMessage("§cNot enough money to double down!");
                return;
            }
            doubled = true;
            bet *= 2;
            playerHand.add(drawCard());
            player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1.0f, 1.2f);

            // Update bet display
            inventory.setItem(BET_INFO_SLOT, ItemUtil.make(Material.GOLD_BLOCK,
                    "&6&lDOUBLED DOWN!",
                    "&7New bet: " + plugin.getEconomyManager().formatAmount(bet)));

            renderHands(true);

            if (handValue(playerHand) > 21) {
                playerTurn = false;
                gameOver = true;
                renderHands(false);
                recordLoss();
                inventory.setItem(BET_INFO_SLOT, ItemUtil.make(Material.BARRIER,
                        "&c&lBUST on Double Down!",
                        "&cLost: &4-" + plugin.getEconomyManager().formatAmount(bet)));
                clearActionButtons();
                inventory.setItem(49, ItemUtil.make(Material.RED_BED, "&c&lClose", "&7Click to exit"));
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 0.7f);
                scheduleAutoClose();
            } else {
                handleStand();
            }
        }
    }

    private void handleStand() {
        playerTurn = false;
        clearActionButtons();

        // Animate dealer drawing cards
        BukkitTask dealerTask = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            int step = 0;

            @Override
            public void run() {
                if (step == 0) {
                    // Reveal hidden card
                    renderHands(false);
                    player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 0.8f, 0.9f);
                } else {
                    // Dealer hits on < 17 (or soft 17)
                    int dealerTotal = handValue(dealerHand);
                    if (dealerTotal < 17) {
                        dealerHand.add(drawCard());
                        renderHands(false);
                        player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 0.8f, 1.0f);
                    } else {
                        // Dealer stands — evaluate
                        for (BukkitTask t : tasks) t.cancel();
                        tasks.clear();
                        evaluateFinal();
                        return;
                    }
                }
                step++;
            }
        }, 0L, 25L);

        tasks.add(dealerTask);
    }

    private void evaluateFinal() {
        int playerScore = handValue(playerHand);
        int dealerScore = handValue(dealerHand);
        gameOver = true;

        String result;
        boolean won = false;
        boolean push = false;

        if (dealerScore > 21) {
            won = true;
            result = "&a&lDealer Busted! You Win!";
        } else if (playerScore > dealerScore) {
            won = true;
            result = "&a&lYou Win!";
        } else if (playerScore == dealerScore) {
            push = true;
            result = "&e&lPush! Tie game.";
        } else {
            result = "&c&lDealer Wins.";
        }

        if (won) {
            double winAmount = bet * 2.0;
            payout(winAmount);
            inventory.setItem(BET_INFO_SLOT, ItemUtil.make(Material.DIAMOND,
                    result,
                    "&7Player: &e" + playerScore + " | Dealer: &e" + dealerScore,
                    "&aWon: &2+" + plugin.getEconomyManager().formatAmount(winAmount - bet)));
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        } else if (push) {
            plugin.getEconomyManager().deposit(player, bet); // Return bet
            inventory.setItem(BET_INFO_SLOT, ItemUtil.make(Material.YELLOW_WOOL,
                    result,
                    "&7Player: &e" + playerScore + " | Dealer: &e" + dealerScore,
                    "&7Your bet has been returned."));
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
        } else {
            recordLoss();
            inventory.setItem(BET_INFO_SLOT, ItemUtil.make(Material.BARRIER,
                    result,
                    "&7Player: &e" + playerScore + " | Dealer: &e" + dealerScore,
                    "&cLost: &4-" + plugin.getEconomyManager().formatAmount(bet)));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 0.8f);
        }

        inventory.setItem(49, ItemUtil.make(Material.RED_BED, "&c&lClose", "&7Click to exit"));
        scheduleAutoClose();
    }

    private void clearActionButtons() {
        inventory.setItem(HIT_SLOT, ItemUtil.fillerBlack());
        inventory.setItem(STAND_SLOT, ItemUtil.fillerBlack());
        inventory.setItem(DOUBLE_SLOT, ItemUtil.fillerBlack());
    }

    private void scheduleAutoClose() {
        BukkitTask closeTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!isFinished()) {
                plugin.getSessionManager().removeSession(player);
                player.closeInventory();
            }
        }, 140L);
        tasks.add(closeTask);
    }
}

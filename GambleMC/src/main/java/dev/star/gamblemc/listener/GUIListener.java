package dev.star.gamblemc.listener;

import dev.star.gamblemc.GambleMC;
import dev.star.gamblemc.game.*;
import dev.star.gamblemc.gui.BetSelectionGUI;
import dev.star.gamblemc.gui.MainMenuGUI;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GUIListener implements Listener {

    private final GambleMC plugin;
    // Players waiting to type a custom bet amount in chat
    private final Map<UUID, BetSelectionGUI.GameType> awaitingCustomBet = new HashMap<>();
    // Track which game type a player is selecting a bet for
    private final Map<UUID, BetSelectionGUI.GameType> betSelectionGame = new HashMap<>();

    public GUIListener(GambleMC plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getCurrentItem() == null) return;

        event.setCancelled(true); // Always cancel to prevent item theft

        String title = PlainTextComponentSerializer.plainText().serialize(event.getView().title());
        int slot = event.getRawSlot();

        // ── Main Menu ──
        if (title.startsWith("🎰 GambleMC")) {
            handleMainMenu(player, slot);
            return;
        }

        // ── Bet Selection ──
        if (title.startsWith("💰 Place Your Bet")) {
            handleBetSelection(player, slot);
            return;
        }

        // ── Active game session ──
        GameSession session = plugin.getSessionManager().getSession(player);
        if (session != null && event.getInventory().equals(session.getInventory())) {
            if (slot == 49 && session.isFinished()) {
                plugin.getSessionManager().removeSession(player);
                player.closeInventory();
            } else if (!session.isFinished()) {
                session.handleClick(slot);
            }
        }
    }

    private void handleMainMenu(Player player, int slot) {
        switch (slot) {
            case MainMenuGUI.COINFLIP_SLOT -> openBetSelection(player, BetSelectionGUI.GameType.COINFLIP);
            case MainMenuGUI.SLOTS_SLOT -> openBetSelection(player, BetSelectionGUI.GameType.SLOTS);
            case MainMenuGUI.HIGHER_LOWER_SLOT -> openBetSelection(player, BetSelectionGUI.GameType.HIGHER_LOWER);
            case MainMenuGUI.BLACKJACK_SLOT -> openBetSelection(player, BetSelectionGUI.GameType.BLACKJACK);
            case MainMenuGUI.CLOSE_SLOT -> player.closeInventory();
        }
    }

    private void openBetSelection(Player player, BetSelectionGUI.GameType type) {
        betSelectionGame.put(player.getUniqueId(), type);
        player.openInventory(BetSelectionGUI.build(plugin, player, type));
    }

    private void handleBetSelection(Player player, int slot) {
        BetSelectionGUI.GameType type = betSelectionGame.get(player.getUniqueId());
        if (type == null) return;

        if (slot == BetSelectionGUI.BACK_SLOT) {
            player.openInventory(MainMenuGUI.build(plugin, player));
            return;
        }

        if (slot == BetSelectionGUI.CUSTOM_BET_SLOT) {
            awaitingCustomBet.put(player.getUniqueId(), type);
            player.closeInventory();
            player.sendMessage("§6[GambleMC] §eType your bet amount in chat:");
            return;
        }

        // Check preset bet slots
        for (int i = 0; i < BetSelectionGUI.BET_SLOTS.length; i++) {
            if (slot == BetSelectionGUI.BET_SLOTS[i]) {
                double amount = BetSelectionGUI.BET_AMOUNTS[i];
                startGame(player, type, amount);
                return;
            }
        }
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        BetSelectionGUI.GameType type = awaitingCustomBet.get(player.getUniqueId());
        if (type == null) return;

        event.setCancelled(true);
        awaitingCustomBet.remove(player.getUniqueId());

        String input = event.getMessage().trim();
        double amount;
        try {
            amount = Double.parseDouble(input);
        } catch (NumberFormatException e) {
            player.sendMessage("§c[GambleMC] Invalid amount. Please enter a number.");
            return;
        }

        // Validate on main thread
        plugin.getServer().getScheduler().runTask(plugin, () -> startGame(player, type, amount));
    }

    private void startGame(Player player, BetSelectionGUI.GameType type, double amount) {
        double min = plugin.getConfig().getDouble("min-bet", 10.0);
        double max = plugin.getConfig().getDouble("max-bet", 100000.0);

        if (amount < min) {
            player.sendMessage("§c[GambleMC] Minimum bet is §e" + plugin.getEconomyManager().formatAmount(min));
            return;
        }
        if (amount > max) {
            player.sendMessage("§c[GambleMC] Maximum bet is §e" + plugin.getEconomyManager().formatAmount(max));
            return;
        }
        if (!plugin.getEconomyManager().hasEnough(player, amount)) {
            player.sendMessage("§c[GambleMC] You don't have enough money to bet §e" + plugin.getEconomyManager().formatAmount(amount));
            return;
        }
        if (plugin.getSessionManager().hasSession(player)) {
            player.sendMessage("§c[GambleMC] You already have an active game session!");
            return;
        }
        if (plugin.getSessionManager().isOnCooldown(player)) {
            long remaining = plugin.getSessionManager().getRemainingCooldown(player);
            player.sendMessage("§c[GambleMC] Please wait §e" + remaining + "s §cbefore gambling again.");
            return;
        }

        // Deduct bet
        if (!plugin.getEconomyManager().placeBet(player, amount)) {
            player.sendMessage("§c[GambleMC] Transaction failed. Please try again.");
            return;
        }

        // Create and start game
        GameSession session = switch (type) {
            case COINFLIP -> new CoinflipSession(plugin, player, amount);
            case SLOTS -> new SlotsSession(plugin, player, amount);
            case HIGHER_LOWER -> new HigherLowerSession(plugin, player, amount);
            case BLACKJACK -> new BlackjackSession(plugin, player, amount);
        };

        plugin.getSessionManager().addSession(player, session);
        betSelectionGame.remove(player.getUniqueId());
        session.open();
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;

        // If they close a game session mid-game without finishing, forfeit bet
        GameSession session = plugin.getSessionManager().getSession(player);
        if (session != null && !session.isFinished()) {
            // They closed mid-game — forfeit
            plugin.getPlayerDataManager().recordLoss(player, session.getBet());
            plugin.getSessionManager().removeSession(player);
            player.sendMessage("§c[GambleMC] You closed the game early. Your bet was forfeited.");
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        GameSession session = plugin.getSessionManager().getSession(player);
        if (session != null) {
            plugin.getPlayerDataManager().recordLoss(player, session.getBet());
            plugin.getSessionManager().removeSession(player);
        }
        awaitingCustomBet.remove(player.getUniqueId());
        betSelectionGame.remove(player.getUniqueId());
    }
}

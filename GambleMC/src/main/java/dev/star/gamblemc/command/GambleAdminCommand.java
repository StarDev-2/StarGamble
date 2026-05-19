package dev.star.gamblemc.command;

import dev.star.gamblemc.GambleMC;
import dev.star.gamblemc.manager.PlayerDataManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class GambleAdminCommand implements CommandExecutor, TabCompleter {

    private final GambleMC plugin;

    // Sub-commands
    private static final List<String> SUBCOMMANDS = List.of(
            "reload",
            "stats",
            "setwins",
            "setlosses",
            "addwins",
            "addlosses",
            "resetstats",
            "give",
            "take",
            "closesession",
            "setcooldown",
            "info"
    );

    public GambleAdminCommand(GambleMC plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {

        if (!sender.hasPermission("gamblemc.admin")) {
            sender.sendMessage("§c[GambleMC] You don't have admin permission.");
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {

            // /gambleadmin reload
            case "reload" -> {
                plugin.reloadConfig();
                sender.sendMessage("§a[GambleMC] Config reloaded.");
            }

            // /gambleadmin info
            case "info" -> {
                sender.sendMessage("§6=== GambleMC Info ===");
                sender.sendMessage("§7Version: §e" + plugin.getDescription().getVersion());
                sender.sendMessage("§7Economy: §e" + (plugin.getEconomyManager().isEnabled() ? "Vault Connected" : "Disabled"));
                sender.sendMessage("§7Active sessions: §e" + plugin.getSessionManager().getAllSessions().size());
                sender.sendMessage("§7Min bet: §e" + plugin.getEconomyManager().formatAmount(plugin.getConfig().getDouble("min-bet", 10)));
                sender.sendMessage("§7Max bet: §e" + plugin.getEconomyManager().formatAmount(plugin.getConfig().getDouble("max-bet", 100000)));
                sender.sendMessage("§7Cooldown: §e" + plugin.getConfig().getInt("cooldown-seconds", 5) + "s");
            }

            // /gambleadmin stats <player>
            case "stats" -> {
                if (args.length < 2) { sender.sendMessage("§cUsage: /gambleadmin stats <player>"); return true; }
                OfflinePlayer target = getOfflinePlayer(args[1]);
                if (target == null) { sender.sendMessage("§cPlayer not found."); return true; }
                PlayerDataManager.PlayerStats stats = plugin.getPlayerDataManager().getStats(target.getUniqueId());
                sender.sendMessage("§6=== Stats: " + target.getName() + " ===");
                sender.sendMessage("§7Wins: §a" + stats.wins);
                sender.sendMessage("§7Losses: §c" + stats.losses);
                sender.sendMessage("§7Total games: §e" + stats.getTotalGames());
                sender.sendMessage("§7Win rate: §e" + String.format("%.1f", stats.getWinRate()) + "%");
                sender.sendMessage("§7Total won: §a+" + plugin.getEconomyManager().formatAmount(stats.totalWon));
                sender.sendMessage("§7Total lost: §c-" + plugin.getEconomyManager().formatAmount(stats.totalLost));
            }

            // /gambleadmin setwins <player> <amount>
            case "setwins" -> {
                if (args.length < 3) { sender.sendMessage("§cUsage: /gambleadmin setwins <player> <amount>"); return true; }
                OfflinePlayer target = getOfflinePlayer(args[1]);
                if (target == null) { sender.sendMessage("§cPlayer not found."); return true; }
                int amount = parseIntSafe(args[2]);
                if (amount < 0) { sender.sendMessage("§cAmount must be 0 or greater."); return true; }
                plugin.getPlayerDataManager().setWins(target.getUniqueId(), amount);
                plugin.getPlayerDataManager().saveAll();
                sender.sendMessage("§a[GambleMC] Set §e" + target.getName() + "§a's wins to §e" + amount);
            }

            // /gambleadmin setlosses <player> <amount>
            case "setlosses" -> {
                if (args.length < 3) { sender.sendMessage("§cUsage: /gambleadmin setlosses <player> <amount>"); return true; }
                OfflinePlayer target = getOfflinePlayer(args[1]);
                if (target == null) { sender.sendMessage("§cPlayer not found."); return true; }
                int amount = parseIntSafe(args[2]);
                if (amount < 0) { sender.sendMessage("§cAmount must be 0 or greater."); return true; }
                plugin.getPlayerDataManager().setLosses(target.getUniqueId(), amount);
                plugin.getPlayerDataManager().saveAll();
                sender.sendMessage("§a[GambleMC] Set §e" + target.getName() + "§a's losses to §e" + amount);
            }

            // /gambleadmin addwins <player> <amount>
            case "addwins" -> {
                if (args.length < 3) { sender.sendMessage("§cUsage: /gambleadmin addwins <player> <amount>"); return true; }
                OfflinePlayer target = getOfflinePlayer(args[1]);
                if (target == null) { sender.sendMessage("§cPlayer not found."); return true; }
                int amount = parseIntSafe(args[2]);
                plugin.getPlayerDataManager().addWins(target.getUniqueId(), amount);
                plugin.getPlayerDataManager().saveAll();
                sender.sendMessage("§a[GambleMC] Added §e" + amount + " §awins to §e" + target.getName());
            }

            // /gambleadmin addlosses <player> <amount>
            case "addlosses" -> {
                if (args.length < 3) { sender.sendMessage("§cUsage: /gambleadmin addlosses <player> <amount>"); return true; }
                OfflinePlayer target = getOfflinePlayer(args[1]);
                if (target == null) { sender.sendMessage("§cPlayer not found."); return true; }
                int amount = parseIntSafe(args[2]);
                plugin.getPlayerDataManager().addLosses(target.getUniqueId(), amount);
                plugin.getPlayerDataManager().saveAll();
                sender.sendMessage("§a[GambleMC] Added §e" + amount + " §closses to §e" + target.getName());
            }

            // /gambleadmin resetstats <player>
            case "resetstats" -> {
                if (args.length < 2) { sender.sendMessage("§cUsage: /gambleadmin resetstats <player>"); return true; }
                OfflinePlayer target = getOfflinePlayer(args[1]);
                if (target == null) { sender.sendMessage("§cPlayer not found."); return true; }
                UUID uuid = target.getUniqueId();
                plugin.getPlayerDataManager().setWins(uuid, 0);
                plugin.getPlayerDataManager().setLosses(uuid, 0);
                plugin.getPlayerDataManager().saveAll();
                sender.sendMessage("§a[GambleMC] Reset §e" + target.getName() + "§a's stats.");
            }

            // /gambleadmin give <player> <amount>  (give money)
            case "give" -> {
                if (args.length < 3) { sender.sendMessage("§cUsage: /gambleadmin give <player> <amount>"); return true; }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) { sender.sendMessage("§cPlayer not online."); return true; }
                double amount = parseDoubleSafe(args[2]);
                if (amount <= 0) { sender.sendMessage("§cAmount must be positive."); return true; }
                plugin.getEconomyManager().deposit(target, amount);
                sender.sendMessage("§a[GambleMC] Gave §e" + plugin.getEconomyManager().formatAmount(amount) + " §ato §e" + target.getName());
                target.sendMessage("§6[GambleMC] An admin gave you §e" + plugin.getEconomyManager().formatAmount(amount) + "§6.");
            }

            // /gambleadmin take <player> <amount>  (take money)
            case "take" -> {
                if (args.length < 3) { sender.sendMessage("§cUsage: /gambleadmin take <player> <amount>"); return true; }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) { sender.sendMessage("§cPlayer not online."); return true; }
                double amount = parseDoubleSafe(args[2]);
                if (amount <= 0) { sender.sendMessage("§cAmount must be positive."); return true; }
                plugin.getEconomyManager().withdraw(target, amount);
                sender.sendMessage("§a[GambleMC] Took §e" + plugin.getEconomyManager().formatAmount(amount) + " §afrom §e" + target.getName());
                target.sendMessage("§c[GambleMC] An admin took §e" + plugin.getEconomyManager().formatAmount(amount) + "§c from you.");
            }

            // /gambleadmin closesession <player>
            case "closesession" -> {
                if (args.length < 2) { sender.sendMessage("§cUsage: /gambleadmin closesession <player>"); return true; }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) { sender.sendMessage("§cPlayer not online."); return true; }
                if (!plugin.getSessionManager().hasSession(target)) {
                    sender.sendMessage("§c" + target.getName() + " has no active session.");
                    return true;
                }
                plugin.getSessionManager().removeSession(target);
                target.closeInventory();
                target.sendMessage("§c[GambleMC] Your gambling session was closed by an admin.");
                sender.sendMessage("§a[GambleMC] Closed §e" + target.getName() + "§a's session.");
            }

            // /gambleadmin setcooldown <seconds>
            case "setcooldown" -> {
                if (args.length < 2) { sender.sendMessage("§cUsage: /gambleadmin setcooldown <seconds>"); return true; }
                int secs = parseIntSafe(args[1]);
                if (secs < 0) { sender.sendMessage("§cSeconds must be 0 or greater."); return true; }
                plugin.getConfig().set("cooldown-seconds", secs);
                plugin.saveConfig();
                sender.sendMessage("§a[GambleMC] Cooldown set to §e" + secs + "s§a.");
            }

            default -> sendHelp(sender);
        }

        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§6=== GambleMC Admin Commands ===");
        sender.sendMessage("§e/gambleadmin reload §7- Reload config");
        sender.sendMessage("§e/gambleadmin info §7- Plugin info & status");
        sender.sendMessage("§e/gambleadmin stats <player> §7- View player stats");
        sender.sendMessage("§e/gambleadmin setwins <player> <n> §7- Set player wins");
        sender.sendMessage("§e/gambleadmin setlosses <player> <n> §7- Set player losses");
        sender.sendMessage("§e/gambleadmin addwins <player> <n> §7- Add wins to player");
        sender.sendMessage("§e/gambleadmin addlosses <player> <n> §7- Add losses to player");
        sender.sendMessage("§e/gambleadmin resetstats <player> §7- Reset player stats");
        sender.sendMessage("§e/gambleadmin give <player> <amount> §7- Give money (online)");
        sender.sendMessage("§e/gambleadmin take <player> <amount> §7- Take money (online)");
        sender.sendMessage("§e/gambleadmin closesession <player> §7- Force-close game");
        sender.sendMessage("§e/gambleadmin setcooldown <seconds> §7- Change cooldown");
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("gamblemc.admin")) return List.of();

        if (args.length == 1) {
            return SUBCOMMANDS.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .toList();
        }

        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            List<String> playerCmds = List.of("stats", "setwins", "setlosses", "addwins", "addlosses",
                    "resetstats", "give", "take", "closesession");
            if (playerCmds.contains(sub)) {
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(n -> n.toLowerCase().startsWith(args[1].toLowerCase()))
                        .toList();
            }
        }

        return List.of();
    }

    @Nullable
    private OfflinePlayer getOfflinePlayer(String name) {
        // Check online first
        Player online = Bukkit.getPlayer(name);
        if (online != null) return online;
        // Fall back to offline lookup by name
        @SuppressWarnings("deprecation")
        OfflinePlayer offline = Bukkit.getOfflinePlayer(name);
        return offline.hasPlayedBefore() || offline.isOnline() ? offline : null;
    }

    private int parseIntSafe(String s) {
        try { return Integer.parseInt(s); } catch (NumberFormatException e) { return -1; }
    }

    private double parseDoubleSafe(String s) {
        try { return Double.parseDouble(s); } catch (NumberFormatException e) { return -1; }
    }
}

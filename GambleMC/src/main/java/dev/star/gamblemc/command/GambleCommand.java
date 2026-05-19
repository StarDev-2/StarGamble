package dev.star.gamblemc.command;

import dev.star.gamblemc.GambleMC;
import dev.star.gamblemc.gui.MainMenuGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class GambleCommand implements CommandExecutor {

    private final GambleMC plugin;

    public GambleCommand(GambleMC plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cThis command can only be used by players.");
            return true;
        }

        if (!player.hasPermission("gamblemc.use")) {
            player.sendMessage("§c[GambleMC] You don't have permission to gamble.");
            return true;
        }

        if (plugin.getSessionManager().hasSession(player)) {
            player.sendMessage("§c[GambleMC] You already have an active game session!");
            return true;
        }

        if (plugin.getSessionManager().isOnCooldown(player)) {
            long remaining = plugin.getSessionManager().getRemainingCooldown(player);
            player.sendMessage("§c[GambleMC] Please wait §e" + remaining + "s §cbefore gambling again.");
            return true;
        }

        player.openInventory(MainMenuGUI.build(plugin, player));
        return true;
    }
}

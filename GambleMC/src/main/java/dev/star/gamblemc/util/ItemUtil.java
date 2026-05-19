package dev.star.gamblemc.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ItemUtil {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    public static Component color(String text) {
        // Support legacy & codes
        return LegacyComponentSerializer.legacyAmpersand().deserialize(text);
    }

    public static ItemStack make(Material material, String name) {
        return make(material, name, 1);
    }

    public static ItemStack make(Material material, String name, int amount) {
        ItemStack item = new ItemStack(material, amount);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(color(name));
            meta.setCustomModelData(0);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack make(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(color(name));
            meta.lore(Arrays.stream(lore).map(ItemUtil::color).collect(Collectors.toList()));
            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack make(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(color(name));
            meta.lore(lore.stream().map(ItemUtil::color).collect(Collectors.toList()));
            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack glass(String name) {
        return make(Material.GRAY_STAINED_GLASS_PANE, name);
    }

    public static ItemStack filler() {
        return make(Material.GRAY_STAINED_GLASS_PANE, " ");
    }

    public static ItemStack fillerBlack() {
        return make(Material.BLACK_STAINED_GLASS_PANE, " ");
    }

    public static ItemStack fillerColor(Material mat) {
        return make(mat, " ");
    }

    /** Colored number totem for Blackjack */
    public static ItemStack numberTotem(int value, boolean isAce) {
        String label = isAce ? "&6&lACE &e(1 or 11)" : "&f&l" + value;
        Material mat = totemColorForValue(value);
        return make(mat, label, "&7Card value: &e" + (isAce ? "1 / 11" : value));
    }

    private static Material totemColorForValue(int v) {
        return switch (v) {
            case 1, 11 -> Material.GOLD_BLOCK;   // Ace
            case 2 -> Material.WHITE_WOOL;
            case 3 -> Material.LIGHT_GRAY_WOOL;
            case 4 -> Material.GRAY_WOOL;
            case 5 -> Material.RED_WOOL;
            case 6 -> Material.ORANGE_WOOL;
            case 7 -> Material.YELLOW_WOOL;
            case 8 -> Material.LIME_WOOL;
            case 9 -> Material.CYAN_WOOL;
            case 10 -> Material.BLUE_WOOL;
            default -> Material.PURPLE_WOOL;
        };
    }

    /** Block for higher-or-lower (colored wool) */
    public static ItemStack numberBlock(int value) {
        Material mat = switch (value % 5) {
            case 0 -> Material.RED_WOOL;
            case 1 -> Material.ORANGE_WOOL;
            case 2 -> Material.YELLOW_WOOL;
            case 3 -> Material.GREEN_WOOL;
            default -> Material.CYAN_WOOL;
        };
        return make(mat, "&f&lCard: &e" + value, "&7Range: 1-13");
    }

    public static ItemStack unknownBlock() {
        return make(Material.PURPLE_WOOL, "&5&l? ? ?", "&7The hidden card");
    }
}

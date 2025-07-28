package br.com._0xf.gui;


import br.com._0xf.Main;
import br.com._0xf.utils.CustomHead;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class ShopGui {

    private final String menuTitle = ChatColor.DARK_GRAY + "Loja - Categorias";
    private final String productTitlePrefix = ChatColor.DARK_GRAY + "Loja - ";
    private final CustomHead customHeadAPI = new CustomHead();

    // Mapeia jogador -> categoria atual aberta
    private final Map<UUID, String> openedCategory = new HashMap<>();

    private final Main main;
    private final ConfirmationGui confirmationGui;

    public ShopGui(Main main, ConfirmationGui confirmationGui) {
        this.main = main;
        this.confirmationGui = confirmationGui;
    }

    public void openMainMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 45, menuTitle);

        ConfigurationSection categories = main.getConfig().getConfigurationSection("categories");
        if (categories == null) return;

        for (String key : categories.getKeys(false)) {
            ConfigurationSection section = categories.getConfigurationSection(key);
            if (section == null) continue;

            int slot = section.getInt("slot", 0);
            int id = section.getInt("id", 1);
            int data = section.getInt("data", 0);
            String name = ChatColor.translateAlternateColorCodes('&', section.getString("name", key));
            List<String> lore = section.getStringList("lore");

            Material mat = Material.matchMaterial(String.valueOf(id));
            if (mat == null) mat = Material.STONE;

            ItemStack item = new ItemStack(mat, 1, (short) data);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(name);
                meta.setLore(formatLore(lore));
                item.setItemMeta(meta);
            }

            inv.setItem(slot, item);
        }

        inv.setItem(44, customHeadAPI.create(
                ChatColor.GREEN + "Histórico de Compras",
                "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNGFhZGZiOGJhZDc0NzAzZWExYjE1Yjc4YTY0MTQ0YzBlZWU1MDAxNjhhNmM4Y2MyYTcxNjk4OTY0OTc2MzUzYyJ9fX0=",
                Arrays.asList(
                        ChatColor.GRAY + "Clique para ver seu histórico de compras.",
                        ChatColor.GRAY + "Você pode consultar pagamentos anteriores."

        )));
        player.openInventory(inv);
    }

    public void openProductsMenu(Player player, String categoryKey) {
        ConfigurationSection categories = main.getConfig().getConfigurationSection("categories");
        if (categories == null) return;

        ConfigurationSection category = categories.getConfigurationSection(categoryKey);
        if (category == null) return;

        ConfigurationSection products = category.getConfigurationSection("products");
        if (products == null) return;

        String title = productTitlePrefix + ChatColor.stripColor(category.getString("name", categoryKey).replace("&", "§"));
        Inventory inv = Bukkit.createInventory(null, 54, title);

        for (String prodKey : products.getKeys(false)) {
            ConfigurationSection product = products.getConfigurationSection(prodKey);
            if (product == null) continue;

            int slot = product.getInt("slot", 0);
            int id = product.getInt("id", 1);
            int data = product.getInt("data", 0);
            double price = product.getDouble("price", 0.0);

            String name = ChatColor.translateAlternateColorCodes('&', product.getString("name", prodKey));
            List<String> lore = product.getStringList("lore");

            Material mat = Material.matchMaterial(String.valueOf(id));
            if (mat == null) mat = Material.STONE;

            ItemStack item = new ItemStack(mat, 1, (short) data);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(name);
                meta.setLore(formatLore(lore, price));
                item.setItemMeta(meta);
            }

            inv.setItem(slot, item);
        }

        openedCategory.put(player.getUniqueId(), categoryKey);
        player.openInventory(inv);
    }

    private List<String> formatLore(List<String> lore) {
        return formatLore(lore, -1);
    }

    public void handleCategoryClick(Player player, ItemStack item) {
        if (!item.hasItemMeta()) return;
        String displayName = ChatColor.stripColor(item.getItemMeta().getDisplayName());

        ConfigurationSection categories = main.getConfig().getConfigurationSection("categories");
        if (categories == null) return;

        for (String key : categories.getKeys(false)) {
            ConfigurationSection section = categories.getConfigurationSection(key);
            if (section == null) continue;

            String name = ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', section.getString("name")));
            if (displayName.equalsIgnoreCase(name)) {
                this.openProductsMenu(player, key);
                break;
            }
        }
    }

    public void handleProductClick(Player player, ItemStack item) {
        if (!item.hasItemMeta()) return;

        String catKey = openedCategory.getOrDefault(player.getUniqueId(), null);
        if (catKey == null) return;

        ConfigurationSection productSection = main.getConfig().getConfigurationSection("categories." + catKey + ".products");
        if (productSection == null) return;

        String clickedName = ChatColor.stripColor(item.getItemMeta().getDisplayName());

        for (String prodKey : productSection.getKeys(false)) {
            ConfigurationSection prod = productSection.getConfigurationSection(prodKey);
            if (prod == null) continue;

            String name = ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', prod.getString("name")));
            if (clickedName.equalsIgnoreCase(name)) {
                double price = prod.getDouble("price", 0.0);
                confirmationGui.open(player, prodKey, price);
                break;
            }
        }
    }

    private List<String> formatLore(List<String> lore, double price) {
        List<String> result = new ArrayList<>();
        for (String line : lore) {
            line = ChatColor.translateAlternateColorCodes('&', line);
            if (price >= 0) {
                line = line.replace("<value>", String.format("%.2f", price));
            }
            result.add(line);
        }
        return result;
    }

}

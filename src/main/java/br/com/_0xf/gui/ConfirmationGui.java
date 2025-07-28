package br.com._0xf.gui;

import br.com._0xf.Main;
import br.com._0xf.pix.PixManager;
import br.com._0xf.utils.CustomHead;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class ConfirmationGui {

    static CustomHead customHeadAPI = new CustomHead();
    private static final int SIZE = 27;
    private final ItemStack cancelItem = customHeadAPI.create("&cCancelar Compra", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYWMxNDYwMGFjZTUwNjk1YzdjOWJjZjA5ZTQyYWZkOWY1M2M5ZTIwZGFhMTUyNGM5NWRiNDE5N2RkMzExNjQxMiJ9fX0=");
    private final ItemStack buyItem = customHeadAPI.create("&aConfirmar Compra", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMzgzYTg4YjU5M2RhYjdlNjIxOGI3OWY1ZDk1YzQ0YmI3ZWExNzQyN2M4ZTZjOGNmNmJiNTFjZDJiYTZlY2UyYSJ9fX0=");
    private final Random random = new Random();
    private final Map<UUID, data> pendents = new HashMap<>();
    private final PixManager pixManager;


    public ConfirmationGui(Main main) {
        this.pixManager = main.getPix(); // usa a mesma instância global
    }

    public void open(Player p, String product, double price) {
        Inventory inv = Bukkit.createInventory(null, SIZE, ChatColor.DARK_GRAY + "Confirme a Compra");

        for (int i = 0; i < SIZE; i++) {
            inv.setItem(i, cancelItem);
        }

        int greenSlot = random.nextInt(SIZE);
        inv.setItem(greenSlot, buyItem);

        pendents.put(p.getUniqueId(), new data(product, price));
        p.openInventory(inv);
    }

        public void handleClick(InventoryClickEvent event) {
            Player p = (Player) event.getWhoClicked();
            Inventory inv = event.getInventory();

            if (!inv.getTitle().equals(ChatColor.DARK_GRAY  + "Confirme a Compra")) return;

            event.setCancelled(true);

            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || !clicked.hasItemMeta()) return;

            String name = ChatColor.stripColor(clicked.getItemMeta().getDisplayName());

            if (name.equalsIgnoreCase("Confirmar Compra")) {
                if(!isInvFull(p)) {
                    p.sendMessage(ChatColor.RED + "Seu inventário está cheio. Você precisa de um espaço vazio para receber o item.");
                    p.closeInventory();
                    return;
                }

                data buy = pendents.get(p.getUniqueId());

                if (buy != null) {
                    pixManager.createPixPayment(p, buy.product, buy.price);
                    p.closeInventory();
                } else {
                    p.sendMessage(ChatColor.RED + "Nenhuma compra pendente encontrada.");
                    p.closeInventory();
                }

            } else if (name.equalsIgnoreCase("Cancelar Compra")) {
                pendents.remove(p.getUniqueId());
                p.closeInventory();
                // player.sendMessage(ChatColor.RED + "Compra cancelada.");
            }
        }

    private boolean isInvFull(Player p){
        Inventory inv = p.getInventory();
        for (ItemStack item: inv.getContents()) {
            if(item == null) {
                return true;
            }
        }

        return false;
    }

    private static class data {
        final String product;
        final double price;

        public data(String product, double price) {
            this.product = product;
            this.price = price;
        }
    }

}

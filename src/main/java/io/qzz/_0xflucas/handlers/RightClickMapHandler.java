package io.qzz._0xflucas.handlers;

import org.bukkit.Material;
import org.bukkit.entity.ItemFrame;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;

public class RightClickMapHandler implements Listener {

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent e) {
        ItemStack item = e.getPlayer().getInventory().getItemInHand();

        if (e.getRightClicked() instanceof ItemFrame) {
            if (item != null && item.getType() == Material.MAP) {
                if (item.getItemMeta().getDisplayName().equals("§aPagamento PIX")) {
                    e.setCancelled(true);
                }
            }
        }
    }

    @EventHandler
    public void onDropQRCode(PlayerDropItemEvent e) {
        ItemStack item = e.getItemDrop().getItemStack();
        if (item != null && item.getType() == Material.MAP) {
            if (item.getItemMeta().getDisplayName().equals("§aPagamento PIX")) {
                e.setCancelled(true);
            }
        }
    }

}

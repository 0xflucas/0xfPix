package io.qzz._0xflucas.handlers;

import io.qzz._0xflucas.PixPlugin;
import io.qzz._0xflucas.gui.ConfirmationGui;
import io.qzz._0xflucas.gui.HistoryGui;
import io.qzz._0xflucas.gui.ShopGui;
import io.qzz._0xflucas.models.Payment;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Date;

public class InvClickHandler implements Listener {

    private final PixPlugin plugin;
    private final ShopGui shopGui;
    private final ConfirmationGui confirmationGui;
    private final HistoryGui historyGui;

    private final String menuTitle = ChatColor.DARK_GRAY + "Loja - Categorias";
    private final String productTitlePrefix = ChatColor.DARK_GRAY + "Loja - ";
    private final String confirmationTitle = ChatColor.DARK_GRAY + "Confirme a Compra";
    private final String historyTitle = ChatColor.DARK_GRAY + "Histórico de Pagamentos";

    public InvClickHandler(PixPlugin plugin, ShopGui shopGui, ConfirmationGui confirmationGui, HistoryGui historyGui) {
        this.plugin = plugin;
        this.shopGui = shopGui;
        this.confirmationGui = confirmationGui;
        this.historyGui = historyGui;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player) e.getWhoClicked();

        ItemStack clicked = e.getCurrentItem();
        if (clicked == null) return;
        if (clicked.getType() == Material.AIR) return;

        String title = safeGetTitle(e);
        if (title == null) title = "";

        if (title.equals(confirmationTitle)) {
            e.setCancelled(true);
            confirmationGui.handleClick(e);
            return;
        }

        if (title.equals(menuTitle)) {
            e.setCancelled(true);

            ItemMeta meta = clicked.getItemMeta();
            if (meta != null && meta.hasDisplayName()) {
                String name = meta.getDisplayName();

                if (name.equals(ChatColor.GREEN + "Histórico de Compras")) {
                    historyGui.openHistoryMenu(p);
                    return;
                }
            }

            shopGui.handleCategoryClick(p, clicked);
            return;
        }

        if (title.startsWith(productTitlePrefix)) {
            e.setCancelled(true);

            ItemMeta meta = clicked.getItemMeta();
            if (meta != null && meta.hasDisplayName()) {
                String name = meta.getDisplayName();

                if (name.equals(ChatColor.WHITE + "Voltar")) {
                    shopGui.openMainMenu(p);
                    return;
                }
            }

            shopGui.handleProductClick(p, clicked);
            return;
        }

        if (title.equals(historyTitle)) {
            e.setCancelled(true);

            ItemMeta meta = clicked.getItemMeta();
            if (meta == null || !meta.hasDisplayName()) return;

            String name = meta.getDisplayName();

            if (name.equals(ChatColor.WHITE + "Voltar")) {
                shopGui.openMainMenu(p);
                return;
            }

            if (name.startsWith(ChatColor.GRAY + "#")) {
                String paymentId = name.replace(ChatColor.GRAY + "#", "");

                // 1) tenta do cache/DB
                Payment payment = plugin.getPaymentRepository().findById(paymentId);
                if (payment != null) {
                    // exibindo detalhes
                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy 'às' HH:mm");

                    p.sendMessage("");
                    p.sendMessage(ChatColor.GREEN + "   ▸ Detalhes do Pagamento ◂   ");
                    p.sendMessage("");
                    p.sendMessage(ChatColor.GRAY + "ID:      " + ChatColor.WHITE + payment.getId());
                    p.sendMessage(ChatColor.GRAY + "Produto: " + ChatColor.WHITE + payment.getItem());
                    p.sendMessage(ChatColor.GRAY + "Valor:   " + ChatColor.WHITE + "R$ "
                            + String.format("%.2f", payment.getAmount()));
                    p.sendMessage(ChatColor.GRAY + "Status:  " + (payment.getStatus().equalsIgnoreCase("paid")
                            ? ChatColor.GREEN + "Pago"
                            : ChatColor.RED + "Expirado"));
                    p.sendMessage(ChatColor.GRAY + "Data:    " + ChatColor.WHITE
                            + sdf.format(new Date(payment.getCreatedAt())));

                    return;
                }

                plugin.getPix().checkPaymentStatus(paymentId, p, true);
            }
        }
    }

    private String safeGetTitle(InventoryClickEvent e) {
        try {
            return e.getView().getTitle();
        } catch (NoSuchMethodError | NoClassDefFoundError err) {
            try {
                Method m = e.getInventory().getClass().getMethod("getTitle");
                Object t = m.invoke(e.getInventory());
                return t != null ? t.toString() : null;
            } catch (Exception ex) {
                try {
                    Method m2 = e.getView().getClass().getMethod("getTitle");
                    Object t2 = m2.invoke(e.getView());
                    return t2 != null ? t2.toString() : null;
                } catch (Exception ignored) {
                }
            }
        } catch (Throwable th) {
            try {
                Method m = e.getView().getClass().getMethod("getTitle");
                Object t = m.invoke(e.getView());
                return t != null ? t.toString() : null;
            } catch (Exception ignored) {
            }
        }
        return null;
    }
}

package br.com._0xf.handlers;

import br.com._0xf.Main;
import br.com._0xf.gui.ConfirmationGui;
import br.com._0xf.gui.HistoryGui;
import br.com._0xf.gui.ShopGui;
import br.com._0xf.models.Payment;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.SimpleDateFormat;
import java.util.Date;

public class InvClickHandler implements Listener {

    private final Main plugin;
    private final ShopGui shopGui;
    private final ConfirmationGui confirmationGui;
    private final HistoryGui historyGui;

    private final String menuTitle           = ChatColor.DARK_GRAY + "Loja - Categorias";
    private final String productTitlePrefix  = ChatColor.DARK_GRAY + "Loja - ";
    private final String confirmationTitle   = ChatColor.DARK_GRAY + "Confirme a Compra";
    private final String historyTitle        = ChatColor.DARK_GRAY + "Histórico de Pagamentos";

    public InvClickHandler(Main plugin, ShopGui shopGui, ConfirmationGui confirmationGui, HistoryGui historyGui) {
        this.plugin          = plugin;
        this.shopGui         = shopGui;
        this.confirmationGui = confirmationGui;
        this.historyGui      = historyGui;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        // 1) Verifica se é um player
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player player = (Player) e.getWhoClicked();

        // 2) Protege contra slots vazios ou cursor
        ItemStack clicked = e.getCurrentItem();
        if (clicked == null) return;
        if (clicked.getType() == Material.AIR) return;

        // 3) Guarda o título para comparar
        String title = e.getView().getTitle();

        // 4) Handler de confirmação
        if (title.equals(confirmationTitle)) {
            e.setCancelled(true);
            confirmationGui.handleClick(e);
            return;
        }

        // 5) Handler do menu principal (categorias + histórico)
        if (title.equals(menuTitle)) {
            e.setCancelled(true);

            // 5a) Checa meta e nome do item
            ItemMeta meta = clicked.getItemMeta();
            if (meta != null && meta.hasDisplayName()) {
                String name = meta.getDisplayName();

                // 5b) Se for o ícone de Histórico
                if (name.equals(ChatColor.GREEN + "Histórico de Compras")) {
                    historyGui.openHistoryMenu(player);
                    return;
                }
            }

            // 5c) Senão, trata clique em categoria
            shopGui.handleCategoryClick(player, clicked);
            return;
        }

        // 6) Handler do menu de produtos
        if (title.startsWith(productTitlePrefix)) {
            e.setCancelled(true);
            shopGui.handleProductClick(player, clicked);
            return;
        }

        // 7) Handler do menu de histórico
        if (title.equals(historyTitle)) {
            e.setCancelled(true);

            ItemMeta meta = clicked.getItemMeta();
            if (meta == null || !meta.hasDisplayName()) return;

            String name = meta.getDisplayName();
            if (name.startsWith(ChatColor.GRAY + "#")) {
                String paymentId = name.replace(ChatColor.GRAY + "#", "");

                // 1) tenta do cache/DB
                Payment paid = plugin.getPaymentRepository().findById(paymentId);
                if (paid != null) {
                    // exibindo detalhes
                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy 'às' HH:mm");


                    player.sendMessage("");
                    player.sendMessage(ChatColor.GREEN + "   ▸ Detalhes do Pagamento ◂   ");
                    player.sendMessage("");
                    player.sendMessage(ChatColor.GRAY   + "ID:      " + ChatColor.WHITE  + paid.getId());
                    player.sendMessage(ChatColor.GRAY   + "Produto: " + ChatColor.WHITE  + paid.getItem());
                    player.sendMessage(ChatColor.GRAY   + "Valor:   " + ChatColor.GREEN  + "R$ " + String.format("%.2f", paid.getAmount()));
                    player.sendMessage(ChatColor.GRAY   + "Status:  " + (paid.getStatus().equalsIgnoreCase("paid")
                            ? ChatColor.GREEN + "Pago"
                            : ChatColor.RED   + "Pendente"));
                    player.sendMessage(ChatColor.GRAY   + "Data:    " + ChatColor.WHITE  + sdf.format(new Date(paid.getCreatedAt())));

                    return;
                }

                // 2) se não encontrou no DB (é pendente), chame a consulta na API
                plugin.getPix().consultarStatusPagamento(paymentId, player, true);
            }
        }
    }
}

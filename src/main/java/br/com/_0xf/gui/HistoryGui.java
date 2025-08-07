package br.com._0xf.gui;

import br.com._0xf.Main;
import br.com._0xf.database.repository.PaymentRepository;
import br.com._0xf.models.Payment;
import br.com._0xf.utils.CustomHead;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class HistoryGui {

    private final Main main;
    private final PaymentRepository database;
    private final String title = ChatColor.DARK_GRAY + "Histórico de Pagamentos";
    private final CustomHead customHeadAPI = new CustomHead();
    private final String HEAD_PAID;
    private final String HEAD_EXPIRED;
    private final String HEAD_PENDING;

    public HistoryGui(Main main) {
        this.HEAD_PAID = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNGY1YjQwOTQxN2ZmMmU3NzdkNWU4M2NlY2QyODU2ZDJjY2M2OGRmZmRlZjk0MDE1NGU5NDVhY2U2ZDljY2MifX19";
        this.HEAD_EXPIRED = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMmY0ZjI1MTc1MmM3MzE3YmIyZmI2MjhlNGMzN2M4MmM2MDcxOWQ5MDk5ODUxNDZiMjYxODMyMTUyYTMwYWRhMiJ9fX0=";
        this.HEAD_PENDING = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNjVlZDYwMzVhNTEyYzYzZmY1MzEyNTczZjk1MTFiMzE0M2NlN2Q3YWFiYTIyMzQ1ZGZmNDM5NzM2ZDUxYzFjIn19fQ==";
        this.main = main;
        this.database = main.getPaymentRepository();
    }

    public void openHistoryMenu(Player player) {
        List<Payment> list = database.getAll(player.getUniqueId());
        Inventory inv = Bukkit.createInventory(null, (9 * 6), title);

        int slot = 0;
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy - HH:mm");

        for (Payment product : list) {
            if (slot >= 54) break; // prevenir overflow

            String headBase64;

            if ("paid".equalsIgnoreCase(product.getStatus()) || "approved".equalsIgnoreCase(product.getStatus())) {
                headBase64 = HEAD_PAID;   //
            } else if ("expired".equalsIgnoreCase(product.getStatus())) {
                headBase64 = HEAD_EXPIRED; //
            } else {
                headBase64 = HEAD_PENDING; //
            }

            ItemStack item = customHeadAPI.create("§7#" + product.getId(), headBase64,
                    Arrays.asList(
                            "",
                            ChatColor.GRAY + "Produto: " + ChatColor.WHITE + product.getItem(),
                            ChatColor.GRAY + "Valor: " + ChatColor.WHITE + "R$ " + String.format("%.2f", product.getAmount()),
                            ChatColor.GRAY + "Status: " +
                                    (product.getStatus().equalsIgnoreCase("paid")
                                            ? ChatColor.GREEN + "Pago"
                                            : ChatColor.RED + "Expirado"),
                            ChatColor.GRAY + "Data: " + ChatColor.WHITE + sdf.format(new Date(product.getCreatedAt())),
                            "",
                            ChatColor.GRAY + "▶ Clique para detalhes"
                    ));

            ItemMeta meta = item.getItemMeta();
            item.setItemMeta(meta);
            inv.setItem(slot++, item);
        }

        Map<String, String> pendents = main.getPix().payments;
        for (Map.Entry<String, String> entry : pendents.entrySet()) {
            if (!entry.getKey().equalsIgnoreCase(player.getName())) continue;

            String product = main.getPix().productByPayment.get(entry.getValue());
            Double price = main.getPix().priceByPayment.get(entry.getValue());
            Long createdAt = main.getPix().timestampByPayment.get(entry.getValue()); // veja abaixo como populá-lo

            if (product == null || price == null) continue;
            ItemStack item = customHeadAPI.create("§7#" + entry.getValue(), HEAD_PENDING,
                    Arrays.asList(
                            "",
                            ChatColor.GRAY + "Produto: " + ChatColor.WHITE + product,
                            ChatColor.GRAY + "Valor: " + ChatColor.WHITE + "R$ " + String.format("%.2f", price),
                            ChatColor.GRAY + "Status: " + ChatColor.YELLOW + "Pendente",
                            ChatColor.GRAY + "Data: " + ChatColor.WHITE + sdf.format(new Date(createdAt)),
                            "",
                            ChatColor.GRAY + "▶ Clique para detalhes"
                    ));


            ItemMeta meta = item.getItemMeta();
            item.setItemMeta(meta);
            if (slot < 54) inv.setItem(slot++, item);
        }

        ItemStack backButton = new ItemStack(Material.ARROW);
        ItemMeta backMeta = backButton.getItemMeta();
        if (backMeta != null) {
            backMeta.setDisplayName(ChatColor.WHITE + "Voltar");
            backButton.setItemMeta(backMeta);
        }

        inv.setItem(49, backButton);

        player.openInventory(inv);
    }
}

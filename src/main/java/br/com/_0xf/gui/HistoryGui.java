package br.com._0xf.gui;

import br.com._0xf.Main;
import br.com._0xf.database.repository.PaymentRepository;
import br.com._0xf.models.Payment;
import br.com._0xf.utils.CustomHead;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
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

    public HistoryGui(Main main) {
        this.main = main;
        this.database = main.getPaymentRepository(); // ou como você acessar a instância
    }

    public void openHistoryMenu(Player player) {
        List<Payment> list = database.getAll(player.getUniqueId());
        Inventory inv = Bukkit.createInventory(null, (9 * 3), title);

        int slot = 0;
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy - HH:mm");

        for (Payment product : list) {
            if (slot >= 54) break; // prevenir overflow

            ItemStack item = customHeadAPI.create("§7#" + product.getId(),
                    "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOWNhOWNmZWVjMGM2ZjJkMGMxYjI5NWJhZjJkZmZlMDAwZDRiOWYzNzI1ODI3ZjFkZDY3ZWI0NmFjMmFhZDY1NiJ9fX0=",
                    Arrays.asList(
                            "",
                            ChatColor.GRAY + "Produto: " + ChatColor.WHITE + product.getItem(),
                            ChatColor.GRAY + "Valor: " + ChatColor.GREEN + "R$ " + String.format("%.2f", product.getAmount()),
                            ChatColor.GRAY + "Status: " + ChatColor.GREEN + (product.getStatus().equalsIgnoreCase("PAID") ? "Pago" : "Pendente"),
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
            ItemStack item = customHeadAPI.create("§7#" + entry.getValue(),
                    "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvODk4N2MwMDNlNTEyMDRhNmE4ZDZkYTcxMzUzZTBjOWM1MTY2YWVjMDNlZWQ5MzU3ZjcyZjM0ZmFhYThlOTNlYiJ9fX0=",
                    Arrays.asList(
                            "",
                            ChatColor.GRAY + "Produto: " + ChatColor.WHITE + product,
                            ChatColor.GRAY + "Valor: " + ChatColor.GREEN + "R$ " + String.format("%.2f", price),
                            ChatColor.GRAY + "Status: " + ChatColor.RED + "Pendente",
                            ChatColor.GRAY + "Data: " + ChatColor.WHITE + sdf.format(new Date(createdAt)),
                            "",
                            ChatColor.GRAY + "▶ Clique para detalhes"
                    ));


            ItemMeta meta = item.getItemMeta();
            item.setItemMeta(meta);
            if (slot < 54) inv.setItem(slot++, item);
        }

        player.openInventory(inv);
    }
}

package br.com._0xf.commands;

import br.com._0xf.Main;
import br.com._0xf.gui.ShopGui;
import br.com._0xf.pix.PixManager;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ShopCommand implements CommandExecutor {

    private final ShopGui gui;
    private final PixManager pixManager;

    public ShopCommand(ShopGui gui, Main main) {
        this.gui = gui;
        this.pixManager = main.getPix();
    }

    @Override
    public boolean onCommand(
        CommandSender sender,
        Command command,
        String label,
        String[] args
    ) {

        if (!(sender instanceof Player)) {
            sender.sendMessage("Comando apenas para jogadores.");
            return true;
        }

        Player p = (Player) sender;


        if (args.length == 0) {
            if(!p.hasPermission("0xfpix.use")) {
                p.sendMessage("§cVocê não tem permissão para usar este comando.");
                return true;
            }

            gui.openMainMenu(p);
            return true;
        }

        if(args[0].equalsIgnoreCase("reload") || args[0].equalsIgnoreCase("rl")) {
            if(!p.hasPermission("0xfpix.admin")) {
                p.sendMessage("§cVocê não tem permissão para recarregar a configuração.");
                return true;
            }

            Main.getInstance().reloadConfig();
            p.sendMessage("§aConfiguração recarregada com sucesso.");
            return true;
        }

        if(args[0].equals("status")) {
            if(!p.hasPermission("0xfpix.use")) {
                p.sendMessage("§cVocê não tem permissão para verificar o status do pagamento.");
                return true;
            }

            String paymentIdStatus = pixManager.getLastPayment(p.getName());
            if (paymentIdStatus == null) {
                p.sendMessage("§cNenhum pagamento pendente encontrado.");
                return true;
            }

            pixManager.checkPaymentStatus(paymentIdStatus, p, true);
            return true;
        }

        p.sendMessage(ChatColor.RED + "Por favor, use: /" + label + " [status|reload]");
        return true;
    }
}

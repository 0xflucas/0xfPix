package br.com._0xf;

import br.com._0xf.commands.ShopCommand;
import br.com._0xf.database.dao.PaymentDAO;
import br.com._0xf.database.repository.PaymentRepository;
import br.com._0xf.gui.ConfirmationGui;
import br.com._0xf.gui.HistoryGui;
import br.com._0xf.gui.ShopGui;
import br.com._0xf.handlers.InvClickHandler;
import br.com._0xf.handlers.RightClickMapHandler;
import br.com._0xf.models.Category;
import br.com._0xf.pix.PixManager;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.LinkedHashMap;
import java.util.Map;

public class Main extends JavaPlugin {

    public static final Map<String, Category> categories = new LinkedHashMap<>();
    private ConfirmationGui confirmationGui;
    private ShopGui shopGui;
    private HistoryGui historyGui;
    private PixManager pixManager;
    private PaymentRepository paymentRepository;
    private PaymentDAO paymentDAO;

    @Override
    public void onEnable() {

        setupConfig();
            this.paymentDAO = new PaymentDAO(
                    getConfig().getString("mysql.host"),
                    getConfig().getInt("mysql.port"),
                    getConfig().getString("mysql.database"),
                    getConfig().getString("mysql.username"),
                    getConfig().getString("mysql.password"),
                    this
            );

        try {
            this.paymentDAO.connect();
            Bukkit.getConsoleSender().sendMessage("");
            Bukkit.getConsoleSender().sendMessage("§b[0xfPix] Banco de dados §f'" + this.getConfig().getString("mysql.database") + "' §bconectado com sucesso.");
        } catch (Exception e) {
//            e.printStackTrace();
            Bukkit.getConsoleSender().sendMessage("§4§lErro ao conectar ao banco de dados: " + e.getMessage());
            Bukkit.getConsoleSender().sendMessage("§c[0xfPix] Erro ao conectar ao banco de dados. Verifique as configurações.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        this.paymentRepository = new PaymentRepository(this.getDatabaseManager());
        this.pixManager = new PixManager(this, this.paymentRepository);
        confirmationGui = new ConfirmationGui(this);
        shopGui = new ShopGui(this, confirmationGui);
        historyGui = new HistoryGui(this);

        setupEvents();
        setupCommands();
        setupMessages();
        setupProducts();
    }

    private PaymentDAO getDatabaseManager() {
        return paymentDAO;
    }

    @Override
    public void onDisable() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            pixManager.removeQRCodeItem(p);
        }

        if (paymentDAO != null) {
            paymentDAO.disconnect();
            Bukkit.getConsoleSender().sendMessage("§c[0xfPix] Banco de dados desconectado.");
        }
    }

    void setupEvents() {
        this.getServer().getPluginManager().registerEvents(new InvClickHandler(this, shopGui, confirmationGui, historyGui), this);
        this.getServer().getPluginManager().registerEvents(new RightClickMapHandler(), this);
//        this.getServer().getPluginManager().registerEvents(new DropQRCodeHandler(), this);
//        this.getServer().getPluginManager().registerEvents(new QRCodeMoveHandler(), this);
    }

    void setupCommands() {
        this.getCommand("shop").setExecutor(new ShopCommand(shopGui, this));
    }

    void setupConfig() {
        this.saveDefaultConfig();
        this.getConfig().options().copyDefaults(true);
    }

    void setupProducts() {
        ConfigurationSection categoriesSection = getConfig().getConfigurationSection("categories");
        if (categoriesSection != null) {
            for (String categorieId : categoriesSection.getKeys(false)) {
                Category category = new Category(categorieId, categoriesSection.getConfigurationSection(categorieId));
                categories.put(categorieId, category);
            }
        }

        Bukkit.getConsoleSender().sendMessage("§aCategorias e produtos carregados: §7" + categories.size());
    }

    void setupMessages() {
        Bukkit.getConsoleSender().sendMessage("");
        Bukkit.getConsoleSender().sendMessage("");
        Bukkit.getConsoleSender().sendMessage("§7==============================================================");
        Bukkit.getConsoleSender().sendMessage("§b=>[0xfPix] §f" + this.getDescription().getName() + " " + this.getDescription().getVersion() + " plugin habilitado com sucesso!!!");
        Bukkit.getConsoleSender().sendMessage("§7==============================================================");
        Bukkit.getConsoleSender().sendMessage("");
    }

    public Main getInstance() {
        return Main.getPlugin(Main.class);
    }

    public PixManager getPix() {
        return pixManager;
    }

    public PaymentRepository getPaymentRepository() {
        return paymentRepository;
    }
}
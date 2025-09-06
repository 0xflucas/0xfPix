package io.qzz._0xflucas;

import io.qzz._0xflucas.commands.ShopCommand;
import io.qzz._0xflucas.database.dao.PaymentDAO;
import io.qzz._0xflucas.database.repository.PaymentRepository;
import io.qzz._0xflucas.gui.ConfirmationGui;
import io.qzz._0xflucas.gui.HistoryGui;
import io.qzz._0xflucas.gui.ShopGui;
import io.qzz._0xflucas.handlers.InvClickHandler;
import io.qzz._0xflucas.handlers.RightClickMapHandler;
import io.qzz._0xflucas.models.Category;
import io.qzz._0xflucas.pix.PixManager;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.LinkedHashMap;
import java.util.Map;

public class PixPlugin extends JavaPlugin {

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
                this);

        try {
            this.paymentDAO.connect();
            Bukkit.getConsoleSender().sendMessage("");
            Bukkit.getConsoleSender().sendMessage("§b[0xfPix] Banco de dados §f'"
                    + this.getConfig().getString("mysql.database") + "' §bconectado com sucesso.");
        } catch (Exception e) {
            // e.printStackTrace();
            Bukkit.getConsoleSender().sendMessage("§4§lErro ao conectar ao banco de dados: " + e.getMessage());
            Bukkit.getConsoleSender()
                    .sendMessage("§c[0xfPix] Erro ao conectar ao banco de dados. Verifique as configurações.");
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
        this.getServer().getPluginManager()
                .registerEvents(new InvClickHandler(this, shopGui, confirmationGui, historyGui), this);
        this.getServer().getPluginManager().registerEvents(new RightClickMapHandler(), this);
        // this.getServer().getPluginManager().registerEvents(new DropQRCodeHandler(),
        // this);
        // this.getServer().getPluginManager().registerEvents(new QRCodeMoveHandler(),
        // this);
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
            for (String categoryId : categoriesSection.getKeys(false)) {
                Category category = new Category(categoryId, categoriesSection.getConfigurationSection(categoryId));
                categories.put(categoryId, category);
            }
        }

        Bukkit.getConsoleSender().sendMessage("§bCategorias carregadas: §f" + categories.size());
    }

    void setupMessages() {
        Bukkit.getConsoleSender().sendMessage("");
        Bukkit.getConsoleSender().sendMessage("");
        Bukkit.getConsoleSender().sendMessage("§7==============================================================");
        Bukkit.getConsoleSender().sendMessage("§b=>[0xfPix] §f" + this.getDescription().getName() + " "
                + this.getDescription().getVersion() + " plugin habilitado com sucesso!!!");
        Bukkit.getConsoleSender().sendMessage("§7==============================================================");
        Bukkit.getConsoleSender().sendMessage("");
    }

    public static PixPlugin getInstance() {
        return PixPlugin.getPlugin(PixPlugin.class);
    }

    public PixManager getPix() {
        return pixManager;
    }

    public PaymentRepository getPaymentRepository() {
        return paymentRepository;
    }
}
package io.qzz._0xflucas.models;

import org.bukkit.configuration.ConfigurationSection;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Category {
    private final String id;
    private final String name;
    private final int slot, itemId, data;
    private final List<String> lore;
    private final Map<String, Product> products = new LinkedHashMap<>();

    public Category(String id, ConfigurationSection section) {
        this.id = id;
        this.name = section.getString("name");
        this.slot = section.getInt("slot");
        this.itemId = section.getInt("id");
        this.data = section.getInt("data");
        this.lore = section.getStringList("lore");

        ConfigurationSection productsSec = section.getConfigurationSection("products");
        if (productsSec != null) {
            for (String prodId : productsSec.getKeys(false)) {
                products.put(prodId, new Product(prodId, productsSec.getConfigurationSection(prodId)));
            }
        }
    }

    public String getId() { return id; }
    public String getNome() { return name; }
    public int getSlot() { return slot; }
    public int getItemId() { return itemId; }
    public int getData() { return data; }
    public List<String> getLore() { return lore; }
    public Map<String, Product> getProducts() { return products; }
}

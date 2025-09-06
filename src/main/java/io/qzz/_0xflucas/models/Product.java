package io.qzz._0xflucas.models;

import org.bukkit.configuration.ConfigurationSection;

import java.util.List;

public class Product {

    private final String id;
    private final String name;
    private final double price;
    private final int slot, itemId, data;
    private final List<String> lore;
    private final List<String> commands;

    public Product(String id, ConfigurationSection section) {
        this.id = id;
        this.name = section.getString("name");
        this.price = section.getDouble("price");
        this.slot = section.getInt("slot");
        this.itemId = section.getInt("id");
        this.data = section.getInt("data");
        this.lore = section.getStringList("lore");
        this.commands = section.getStringList("commands");
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public double getPrice() { return price; }
    public int getSlot() { return slot; }
    public int getItemId() { return itemId; }
    public int getData() { return data; }
    public List<String> getLore() { return lore; }
    public List<String> getCommands() { return commands; }
}
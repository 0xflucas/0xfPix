package io.qzz._0xflucas.utils;

import java.lang.reflect.Field;
import java.util.List;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

public class CustomHead {

    public CustomHead() {}
    public ItemStack create(String nome, String base64) {
        return create(nome, base64, null);
    }

    public ItemStack create(String nome, String base64, List<String> lore) {
        ItemStack skull = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
        if (!(skull.getItemMeta() instanceof SkullMeta)) return skull;

        SkullMeta meta = (SkullMeta) skull.getItemMeta();

        if (nome != null && !nome.isEmpty()) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', nome));
        }

        if (lore != null) {
            meta.setLore(lore);
        }

        try {
            Class<?> profileClass = Class.forName("com.mojang.authlib.GameProfile");
            Object profile = profileClass.getConstructor(UUID.class, String.class)
                    .newInstance(UUID.randomUUID(), null);

            Class<?> propertyClass = Class.forName("com.mojang.authlib.properties.Property");
            Object textureProperty = propertyClass
                    .getConstructor(String.class, String.class)
                    .newInstance("textures", base64);

            Object propertyMap = profileClass.getMethod("getProperties").invoke(profile);
            propertyMap.getClass()
                    .getMethod("put", Object.class, Object.class)
                    .invoke(propertyMap, "textures", textureProperty);

            Field profileField = meta.getClass().getDeclaredField("profile");
            profileField.setAccessible(true);
            profileField.set(meta, profile);
        } catch (Exception ignored) {
            return skull;
        }

        skull.setItemMeta(meta);
        return skull;
    }
}
package br.com._0xf.qrcode;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.map.MapView;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

public class QRCodeMapItem {

    public static ItemStack createQRCodeMap(String base64QR, String price, String itemComprando, String tempoPagamento) {
        try {
            // Remove o prefixo base64 se existir
            String cleanBase64 = base64QR.replace("data:image/png;base64,", "");

            // Decodifica a imagem base64 para bytes
            byte[] imageBytes = Base64.getDecoder().decode(cleanBase64);

            // Lê os bytes em BufferedImage
            BufferedImage originalImage = ImageIO.read(new ByteArrayInputStream(imageBytes));

            // Redimensiona a imagem para 128x128 (resolução do mapa no MC 1.8)
            BufferedImage resizedImage = resizeImage(originalImage, 128, 128);

            // Cria o item mapa
            ItemStack mapItem = new ItemStack(Material.MAP);

            // Pega o mundo padrão (primeiro da lista)
            World world = Bukkit.getWorlds().get(0);

            // Cria uma nova visão de mapa
            MapView mapView = Bukkit.createMap(world);

            // Remove renderizadores padrões
            mapView.getRenderers().clear();

            // Adiciona nosso renderizador customizado para mostrar o QR Code
            mapView.addRenderer(new QRCodeMapRenderer(resizedImage));

            // Associa o mapa ao item pelo id
            mapItem.setDurability((short) mapView.getId());

            ItemMeta meta = mapItem.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("§aPagamento PIX");
                meta.setLore(Arrays.asList("§7Escaneie o QR Code", "§7para completar o pagamento."));

                meta.setDisplayName("§aPagamento PIX");

                List<String> lore = new ArrayList<>();
                lore.add("§7Escaneie o QR Code");
                lore.add("§7para completar o pagamento.");
                lore.add("§7");
                lore.add("§7Valor: §a" + price);
                lore.add("§7Produto: §a" + itemComprando);
                lore.add("§7Você tem até 10 minutos para realizar o pagamento");
                lore.add("§7Após o pagamento, aguarde a confirmação");

                meta.setLore(lore);
                mapItem.setItemMeta(meta);
            }

            return mapItem;
        } catch (Exception e) {
            // Retorna um item de erro (barreira)
            ItemStack errorItem = new ItemStack(Material.BARRIER);
            ItemMeta errorMeta = errorItem.getItemMeta();

            if (errorMeta != null) {
                errorMeta.setDisplayName("§cErro ao gerar QR Code");
                errorMeta.setLore(Arrays.asList("§7Não foi possível gerar o QR Code.", "§7Contate um administrador."));
                errorItem.setItemMeta(errorMeta);
            }


            return errorItem;
        }
    }

    // Função para redimensionar BufferedImage para largura e altura definidas
    private static BufferedImage resizeImage(BufferedImage originalImage, int width, int height) {
        Image tmp = originalImage.getScaledInstance(width, height, Image.SCALE_SMOOTH);
        BufferedImage resized = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        Graphics2D g2d = resized.createGraphics();
        g2d.drawImage(tmp, 0, 0, null);
        g2d.dispose();

        return resized;
    }
}

package io.qzz._0xflucas.qrcode;
import org.bukkit.entity.Player;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;

import java.awt.image.BufferedImage;

public class QRCodeMapRenderer extends MapRenderer {
    private final BufferedImage qrImage;
    private boolean rendered = false;

    public QRCodeMapRenderer(BufferedImage qrImage) {
        this.qrImage = qrImage;
    }

    @Override
    public void render(MapView mapView, MapCanvas mapCanvas, Player player) {
        if (rendered) return; // desenha só uma vez
        mapCanvas.drawImage(0, 0, qrImage);
        rendered = true;
    }
}
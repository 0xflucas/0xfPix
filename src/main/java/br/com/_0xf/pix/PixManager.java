package br.com._0xf.pix;

import br.com._0xf.Main;
import br.com._0xf.database.repository.PaymentRepository;
import br.com._0xf.models.Payment;
import br.com._0xf.qrcode.QRCodeMapItem;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.*;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitTask;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.bukkit.Bukkit.getScheduler;

public class PixManager {

    private final Main main;
    private final OkHttpClient httpClient = new OkHttpClient();
    private final String token;

    public final Map<String, String> payments = new ConcurrentHashMap<>();            // playerName → paymentId
    public final Map<String, String> statusPayment = new ConcurrentHashMap<>();     // paymentId → status
    public final Map<String, String> productByPayment = new ConcurrentHashMap<>();// paymentId → produto
    public final Map<String, Double> priceByPayment = new ConcurrentHashMap<>(); // paymentId → valor
    private final Set<String> processedPayment = ConcurrentHashMap.newKeySet();// paymentId já concluído
    private final Map<String, BukkitTask> taskByPayment = new ConcurrentHashMap<>(); // paymentId → task
    public final Map<String, Long> timestampByPayment = new ConcurrentHashMap<>();
    private final PaymentRepository paymentRepository;
    private final Set<String> wPayments = ConcurrentHashMap.newKeySet();


    public PixManager(Main main, PaymentRepository paymentRepository) {
        this.main = main;
        this.paymentRepository = paymentRepository;
        this.token = main.getConfig().getString("mercado-pago.token");
    }

    public void createPixPayment(Player p, String product, double price) {
        String playerName = p.getName();
        if (payments.containsKey(playerName)) {
            getScheduler().runTask(main, () ->
                    p.sendMessage("§cVocê já tem um pagamento PIX pendente.")
            );
            return;
        }

        // Monta body JSON
        JsonObject payer = new JsonObject();
        payer.addProperty("email", playerName.toLowerCase() + "@0xf.dev"); // damienvat@0xf.dev

        JsonObject body = new JsonObject();
        body.addProperty("transaction_amount", price);
        body.addProperty("description", "Compra de " + product + " com PIX");
        body.add("payer", payer);
        body.addProperty("payment_method_id", "pix");

        RequestBody requestBody = RequestBody.create(
                Objects.requireNonNull(MediaType.parse("application/json")),
                body.toString()
        );

        Request request = new Request.Builder()
                .url("https://api.mercadopago.com/v1/payments")
                .addHeader("Authorization", "Bearer " + token)
                .addHeader("Content-Type", "application/json")
                .addHeader("X-Idempotency-Key", UUID.randomUUID().toString())
                .post(requestBody)
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                main.getLogger().warning("Erro ao criar pagamento PIX: " + e.getMessage());
                getScheduler().runTask(main, () ->
                        p.sendMessage("§cErro interno. Por favor, contate um administrador. #1")
                );
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (ResponseBody respBody = response.body()) {
                    if (!response.isSuccessful()) {
                        String erro = respBody != null ? respBody.string() : "desconhecido";
                        main.getLogger().warning("Erro ao criar PIX: " + erro);
                        getScheduler().runTask(main, () ->
                                p.sendMessage("§cErro interno. Por favor, contate um administrador. #2")
                        );
                        return;
                    }

                    if (respBody == null) {
                        main.getLogger().warning("Resposta vazia ao criar PIX.");
                        getScheduler().runTask(main, () ->
                                p.sendMessage("§cErro interno. Por favor, contate um administrador. #3")
                        );
                        return;
                    }

                    JsonObject obj = new JsonParser().parse(respBody.string()).getAsJsonObject();
                    String paymentId = obj.get("id").getAsString();
                    String status = obj.get("status").getAsString();
                    JsonObject tx = obj.getAsJsonObject("point_of_interaction")
                            .getAsJsonObject("transaction_data");
                    String qrCode = tx.get("qr_code").getAsString();
                    String qrBase64 = tx.get("qr_code_base64").getAsString();

                    // Envia QR ao jogador
                    getScheduler().runTask(main, () -> {
                        p.sendMessage("§aPagamento PIX criado! ID: §e" + paymentId);
                        p.sendMessage("§7QRCode:");
                        p.sendMessage("§f" + qrCode);
                        p.getInventory().addItem(
                                QRCodeMapItem.createQRCodeMap(
                                        qrBase64,
                                        "R$ " + String.format("%.2f", price),
                                        product)
                        );

                        payments.put(playerName, paymentId);
                        statusPayment.put(paymentId, status);
                        productByPayment.put(paymentId, product);
                        priceByPayment.put(paymentId, price);
                        timestampByPayment.put(paymentId, System.currentTimeMillis());

                        p.sendMessage("§aQRCode gerado com sucesso! Verifique seu inventário.");
                        p.sendMessage("§aO pagamento será verificado automaticamente assim que for efetuado.");
                        p.sendMessage("§7Você tem até §a" + main.getConfig().getInt("pix.expire_minutes", 10) + " minutos §7para concluir o pagamento antes que ele expire.");

                    });

                    startAutoCheck(playerName, paymentId);
                }
            }
        });
    }

    public void checkPaymentStatus(String paymentId, Player p, boolean sendMessage) {
        if (processedPayment.contains(paymentId)) {
            getScheduler().runTask(main, () ->
                    p.sendMessage("§aPagamento já foi processado.")
            );
            return;
        }

        String local = statusPayment.get(paymentId);
        if ("approved".equals(local)) {
            processPayment(paymentId, p);
            return;
        }

        Request req = new Request.Builder()
                .url("https://api.mercadopago.com/v1/payments/" + paymentId)
                .addHeader("Authorization", "Bearer " + token)
                .build();

        httpClient.newCall(req).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                main.getLogger().warning("Erro ao consultar status: " + e.getMessage());
                getScheduler().runTask(main, () ->
                        p.sendMessage("§cErro ao consultar status.")
                );
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (ResponseBody rb = response.body()) {
                    if (response.code() == 404) {
                        getScheduler().runTask(main, () ->
                                p.sendMessage("§cPagamento não encontrado.")
                        );
                        return;
                    }
                    if (!response.isSuccessful()) {
                        String msg = rb != null ? rb.string() : "desconhecido";
                        main.getLogger().warning("Consulta falhou: " + msg);
                        getScheduler().runTask(main, () ->
                                p.sendMessage("§cFalha na consulta.")
                        );
                        return;
                    }

                    if (rb == null) {
                        main.getLogger().warning("Resposta vazia ao consultar status.");
                        getScheduler().runTask(main, () ->
                                p.sendMessage("§cErro interno.")
                        );
                        return;
                    }

                    String status = new JsonParser().parse(rb.string())
                            .getAsJsonObject()
                            .get("status").getAsString();
                    statusPayment.put(paymentId, status);

                    if ("approved".equals(status)) {
                        processPayment(paymentId, p);
                    } else {
                        if (sendMessage) {
                            // Pegando dados do cache em memória
                            String product = productByPayment.get(paymentId);
                            Double price = priceByPayment.get(paymentId);
                            String currentStatus = statusPayment.get(paymentId);
                            Long createdAtMillis = timestampByPayment.get(paymentId);

                            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy 'às' HH:mm");

                            p.sendMessage("");
                            p.sendMessage(ChatColor.GREEN + "   ▸ Detalhes do Pagamento ◂   ");
                            p.sendMessage("");
                            p.sendMessage(ChatColor.GRAY   + "ID:      " + ChatColor.WHITE  + paymentId);
                            p.sendMessage(ChatColor.GRAY   + "Produto: " + ChatColor.WHITE  + (product != null ? product : "Desconhecido"));
                            p.sendMessage(ChatColor.GRAY   + "Valor:   " + ChatColor.GREEN  + "R$ " + (price != null ? String.format("%.2f", price) : "0.00"));

                            String textStatus;
                            ChatColor statusColor;

                            if ("paid".equalsIgnoreCase(currentStatus) || "approved".equalsIgnoreCase(currentStatus)) {
                                textStatus = "Pago";
                                statusColor = ChatColor.GREEN;
                            } else if ("pending".equalsIgnoreCase(currentStatus)) {
                                textStatus = "Pendente";
                                statusColor = ChatColor.RED;
                            } else {
                                textStatus = currentStatus != null ? currentStatus.toUpperCase() : "Desconhecido";
                                statusColor = ChatColor.RED;
                            }

                            p.sendMessage(ChatColor.GRAY + "Status:  " + statusColor + textStatus);

                            String formatedDate = createdAtMillis != null
                                    ? sdf.format(new Date(createdAtMillis))
                                    : "Desconhecida";

                            p.sendMessage(ChatColor.GRAY   + "Data:    " + ChatColor.WHITE  + formatedDate);
                            p.sendMessage("");

                            p.sendMessage("§c" + ("pending".equalsIgnoreCase(currentStatus)
                                    ? "Seu pagamento está pendente. Por favor, aguarde a confirmação."
                                    : "Pagamento aprovado!"));
                        }
                    }
                }
            }
        });
    }

    private void processPayment(String paymentId, Player p) {
        processedPayment.add(paymentId);

        // Cancela a tarefa agendada
        BukkitTask task = taskByPayment.remove(paymentId);
        if (task != null) task.cancel();

        // Recupera dados
        String product = productByPayment.get(paymentId);
        Double price = priceByPayment.get(paymentId);
        if (product == null || price == null) {
            main.getLogger().warning("[PIX] Dados faltando p/ " + paymentId);
            getScheduler().runTask(main, () ->
                    p.sendMessage("§cErro interno. Por favor, contate um administrador. #4")
            );
            return;
        }

        // Persiste no banco
        Payment pmt = new Payment(
                paymentId,
                p.getUniqueId(),
                product,
                price,
                "paid",
                System.currentTimeMillis()
        );

        paymentRepository.save(pmt);
        // Entrega e limpa memória
        getScheduler().runTask(main, () -> {
            runProductCommands(p, product);
            productByPayment.remove(paymentId);
            priceByPayment.remove(paymentId);
            statusPayment.remove(paymentId);
            payments.remove(p.getName());
            timestampByPayment.remove(paymentId);
            removeQRCodeItem(p);
            p.sendMessage("§aPagamento confirmado e entregue!");

        });
    }

    private void startAutoCheck(String playerName, String paymentId) {
        final long interval = 20L * 10; // 10 segundos em ticks

        int expireMin = main.getConfig().getInt("pix.expire_minutes", 10);
        int warnMin = main.getConfig().getInt("pix.warn_before_expire_minutes", 1);

        final long timeout = expireMin * 60 * 1000L;
        final long warningTime = timeout - (warnMin * 60 * 1000L);
        final long start = System.currentTimeMillis();

        BukkitTask task = getScheduler().runTaskTimerAsynchronously(main, () -> {
            final long elapsed = System.currentTimeMillis() - start;
            Player p = Bukkit.getPlayerExact(playerName);

            if (p != null && p.isOnline()) {

                // ⚠ AVISO CONFIGURÁVEL (só 1 vez)
                if (elapsed >= warningTime && !wPayments.contains(paymentId)) {
                    wPayments.add(paymentId);

                    getScheduler().runTask(main, () -> {
                        long remainingMillis = timeout - elapsed;
                        long remainingMinutes = remainingMillis / 1000 / 60;
                        long remainingSeconds = (remainingMillis / 1000) % 60;

                        String timeMsg;
                        if (remainingMinutes > 0) {
                            timeMsg = remainingMinutes + " minuto" + (remainingMinutes > 1 ? "s" : "");
                        } else {
                            timeMsg = remainingSeconds + " segundo" + (remainingSeconds != 1 ? "s" : "");
                        }

                        p.sendMessage("");
                        p.sendMessage("§cFaltam menos de " + timeMsg + " para o pagamento expirar!");
                        p.sendMessage("§7Finalize o pagamento o quanto antes para receber o produto.");
                    });
                }

                // ⏰ EXPIRAÇÃO
                if (elapsed >= timeout) {
                    getScheduler().runTask(main, () -> {
                        p.sendMessage("");
                        p.sendMessage("§cTempo esgotado. Pagamento expirado!");
                        p.sendMessage("§7O código PIX gerado não foi pago dentro do tempo limite.");
                        p.sendMessage("§7Você pode tentar novamente usando: §a/shop§7.");
                    });
                    removeQRCodeItem(p);

                    statusPayment.remove(paymentId);
                    payments.remove(playerName);
                    wPayments.remove(paymentId);

                    BukkitTask t = taskByPayment.remove(paymentId);
                    if (t != null) t.cancel();
                    return;
                }

                // 🔁 VERIFICA STATUS
                String status = statusPayment.getOrDefault(paymentId, "pending");
                if ("approved".equals(status)) {
                    processPayment(paymentId, p);
                } else {
                    checkPaymentStatus(paymentId, p, false);
                }
            }
        }, 250L, interval);

        taskByPayment.put(paymentId, task);
    }

    private void runProductCommands(Player player, String produto) {
        for (String cat : main.getConfig().getConfigurationSection("categories").getKeys(false)) {
            String path = "categories." + cat + ".products." + produto + ".commands";
            for (String cmd : main.getConfig().getStringList(path)) {
                Bukkit.dispatchCommand(
                        Bukkit.getConsoleSender(),
                        cmd.replace("{player}", player.getName())

                );
            }
        }
    }

    public void removeQRCodeItem(Player player) {
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack it = player.getInventory().getItem(i);
            if (it != null && it.getType() == Material.MAP) {
                ItemMeta m = it.getItemMeta();
                if (m != null && m.hasDisplayName() && m.getDisplayName().contains("PIX")) {
                    player.getInventory().setItem(i, null);
                }
            }
        }
        player.updateInventory();
    }

    public String getLastPayment(String playerName) {
        return payments.get(playerName);
    }
}

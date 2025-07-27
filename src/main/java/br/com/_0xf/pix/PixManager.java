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
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.bukkit.Bukkit.getScheduler;

public class PixManager {

    private final Main main;
    private final OkHttpClient httpClient = new OkHttpClient();
    private final String token;

    // Caches em memória
    public final Map<String, String> pagamentos = new ConcurrentHashMap<>();            // playerName → paymentId
    public final Map<String, String> statusPagamentos = new ConcurrentHashMap<>();     // paymentId → status
    public final Map<String, String> produtosPorPagamento = new ConcurrentHashMap<>();// paymentId → produto
    public final Map<String, Double> valoresPorPagamento = new ConcurrentHashMap<>(); // paymentId → valor
    private final Set<String> pagamentosProcessados = ConcurrentHashMap.newKeySet();// paymentId já concluído
    private final Map<String, BukkitTask> tasksPorPagamento = new ConcurrentHashMap<>(); // paymentId → task
    public final Map<String, Long> timestampsPorPagamento = new ConcurrentHashMap<>();


    private final PaymentRepository paymentRepository;

    public PixManager(Main main, PaymentRepository paymentRepository) {
        this.main = main;
        this.paymentRepository = paymentRepository;
        this.token = main.getConfig().getString("mercado-pago.token");
    }

    public void criarPagamentoPIX(Player player, String produto, double valor) {
        String playerName = player.getName();
        if (pagamentos.containsKey(playerName)) {
            getScheduler().runTask(main, () ->
                    player.sendMessage("§cVocê já tem um pagamento PIX pendente.")
            );
            return;
        }

        // Monta body JSON
        JsonObject payer = new JsonObject();
        payer.addProperty("email", playerName + "@mc-craft.com");

        JsonObject body = new JsonObject();
        body.addProperty("transaction_amount", valor);
        body.addProperty("description", "Compra de " + produto + " com PIX");
        body.add("payer", payer);
        body.addProperty("payment_method_id", "pix");

        RequestBody requestBody = RequestBody.create(
                MediaType.parse("application/json"),
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
                        player.sendMessage("§cErro ao criar pagamento PIX.")
                );
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (ResponseBody respBody = response.body()) {
                    if (!response.isSuccessful()) {
                        String erro = respBody != null ? respBody.string() : "desconhecido";
                        main.getLogger().warning("Erro ao criar PIX: " + erro);
                        getScheduler().runTask(main, () ->
                                player.sendMessage("§cFalha: " + erro)
                        );
                        return;
                    }

                    if (respBody == null) {
                        main.getLogger().warning("Resposta vazia ao criar PIX.");
                        getScheduler().runTask(main, () ->
                                player.sendMessage("§cErro interno.")
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

                    // Armazena em memória
                    pagamentos.put(playerName, paymentId);
                    statusPagamentos.put(paymentId, status);
                    produtosPorPagamento.put(paymentId, produto);
                    valoresPorPagamento.put(paymentId, valor);
                    timestampsPorPagamento.put(paymentId, System.currentTimeMillis());


                    // Envia QR ao jogador
                    getScheduler().runTask(main, () -> {
                        player.sendMessage("§aPagamento PIX criado! ID: §e" + paymentId);
                        player.sendMessage("§7QRCode:");
                        player.sendMessage("§f" + qrCode);
                        player.getInventory().addItem(
                                QRCodeMapItem.createQRCodeMap(
                                        qrBase64,
                                        "R$ " + String.format("%.2f", valor),
                                        produto, "10 minutos"
                                )
                        );

                        player.sendMessage("§aQRCode gerado com sucesso! Verifique seu inventário.");
                        player.sendMessage("§aO pagamento será verificado automaticamente assim que for efetuado.");
                        player.sendMessage("§aVocê tem até 10 minutos para concluir o pagamento antes que ele expire.");

                    });

                    // Inicia monitoramento
                    iniciarVerificacaoAutomatica(playerName, paymentId);
                }
            }
        });
    }

    public void consultarStatusPagamento(String paymentId, Player player, boolean sendMessage) {
        if (pagamentosProcessados.contains(paymentId)) {
            getScheduler().runTask(main, () ->
                    player.sendMessage("§aPagamento já foi processado.")
            );
            return;
        }

        String local = statusPagamentos.get(paymentId);
        if ("approved".equals(local)) {
            processarPagamento(paymentId, player);
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
                        player.sendMessage("§cErro ao consultar status.")
                );
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (ResponseBody rb = response.body()) {
                    if (response.code() == 404) {
                        getScheduler().runTask(main, () ->
                                player.sendMessage("§cPagamento não encontrado.")
                        );
                        return;
                    }
                    if (!response.isSuccessful()) {
                        String msg = rb != null ? rb.string() : "desconhecido";
                        main.getLogger().warning("Consulta falhou: " + msg);
                        getScheduler().runTask(main, () ->
                                player.sendMessage("§cFalha na consulta.")
                        );
                        return;
                    }

                    if (rb == null) {
                        main.getLogger().warning("Resposta vazia ao consultar status.");
                        getScheduler().runTask(main, () ->
                                player.sendMessage("§cErro interno.")
                        );
                        return;
                    }

                    String status = new JsonParser().parse(rb.string())
                            .getAsJsonObject()
                            .get("status").getAsString();
                    statusPagamentos.put(paymentId, status);

                    if ("approved".equals(status)) {
                        processarPagamento(paymentId, player);
                    } else {
                        if (sendMessage) {
                            // Pegando dados do cache em memória
                            String produto = produtosPorPagamento.get(paymentId);
                            Double valor = valoresPorPagamento.get(paymentId);
                            String statusAtual = statusPagamentos.get(paymentId);
                            Long createdAtMillis = timestampsPorPagamento.get(paymentId);

                            // Formata a data, cuidado para ter o SimpleDateFormat definido
                            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy 'às' HH:mm");

                            player.sendMessage("");
                            player.sendMessage(ChatColor.GREEN + "   ▸ Detalhes do Pagamento ◂   ");
                            player.sendMessage("");
                            player.sendMessage(ChatColor.GRAY   + "ID:      " + ChatColor.WHITE  + paymentId);
                            player.sendMessage(ChatColor.GRAY   + "Produto: " + ChatColor.WHITE  + (produto != null ? produto : "Desconhecido"));
                            player.sendMessage(ChatColor.GRAY   + "Valor:   " + ChatColor.GREEN  + "R$ " + (valor != null ? String.format("%.2f", valor) : "0.00"));

                            String statusTexto;
                            ChatColor corStatus;

                            if ("paid".equalsIgnoreCase(statusAtual) || "approved".equalsIgnoreCase(statusAtual)) {
                                statusTexto = "Pago";
                                corStatus = ChatColor.GREEN;
                            } else if ("pending".equalsIgnoreCase(statusAtual)) {
                                statusTexto = "Pendente";
                                corStatus = ChatColor.RED;
                            } else {
                                statusTexto = statusAtual != null ? statusAtual.toUpperCase() : "Desconhecido";
                                corStatus = ChatColor.RED;
                            }

                            player.sendMessage(ChatColor.GRAY + "Status:  " + corStatus + statusTexto);

                            String dataFormatada = createdAtMillis != null
                                    ? sdf.format(new Date(createdAtMillis))
                                    : "Desconhecida";

                            player.sendMessage(ChatColor.GRAY   + "Data:    " + ChatColor.WHITE  + dataFormatada);
                            player.sendMessage("");

                            player.sendMessage("§c" + ("pending".equalsIgnoreCase(statusAtual)
                                    ? "Seu pagamento está pendente. Por favor, aguarde a confirmação."
                                    : "Pagamento aprovado!"));
                        }
                    }
                }
            }
        });
    }

    private void processarPagamento(String paymentId, Player player) {
        pagamentosProcessados.add(paymentId);

        // Cancela a tarefa agendada
        BukkitTask task = tasksPorPagamento.remove(paymentId);
        if (task != null) task.cancel();

        // Recupera dados
        String produto = produtosPorPagamento.get(paymentId);
        Double valor = valoresPorPagamento.get(paymentId);
        if (produto == null || valor == null) {
            main.getLogger().warning("[PIX] Dados faltando p/ " + paymentId);
            getScheduler().runTask(main, () ->
                    player.sendMessage("§cErro interno. Conta'te admin.")
            );
            return;
        }

        // Persiste no banco
        Payment pmt = new Payment(
                paymentId,
                player.getUniqueId(),
                produto,
                valor,
                "paid",
                System.currentTimeMillis()
        );
        paymentRepository.save(pmt);

        // Entrega e limpa memória
        getScheduler().runTask(main, () -> {
            executarComandosConfigurados(player, produto);
            produtosPorPagamento.remove(paymentId);
            valoresPorPagamento.remove(paymentId);
            statusPagamentos.remove(paymentId);
            pagamentos.remove(player.getName());
            timestampsPorPagamento.remove(paymentId);
            removeQRCodeItem(player);
            player.sendMessage("§aPagamento confirmado e entregue!");
        });
    }

    private void iniciarVerificacaoAutomatica(String playerName, String paymentId) {
        final long interval = 20L * 10;
        final long timeout = 60_000L;
        final long start = System.currentTimeMillis();

        BukkitTask task = getScheduler().runTaskTimerAsynchronously(main, () -> {
            if (System.currentTimeMillis() - start >= timeout) {
                Player p = Bukkit.getPlayerExact(playerName);
                if (p != null && p.isOnline()) {
                    getScheduler().runTask(main, () ->
                            p.sendMessage("§cTempo esgotado. Pagamento expirou.")
                    );
                    removeQRCodeItem(p);
                }
                statusPagamentos.remove(paymentId);
                pagamentos.remove(playerName);
                tasksPorPagamento.remove(paymentId).cancel();
                return;
            }

            String status = statusPagamentos.getOrDefault(paymentId, "pending");
            Player p = Bukkit.getPlayerExact(playerName);
            if ("approved".equals(status)) {
                if (p != null && p.isOnline()) {
                    processarPagamento(paymentId, p);
                }
            } else {
                if (p != null && p.isOnline()) {
//                    Bukkit.getConsoleSender().sendMessage("§eVerificando PIX do player " + p.getName() + " Status: " + status.toUpperCase() + " - ID: " + paymentId);
//                    p.sendMessage("§eVerificando PIX… Status: " + status.toUpperCase());
                    consultarStatusPagamento(paymentId, p, false);
                }
            }
        }, 250L, interval);

        tasksPorPagamento.put(paymentId, task);
    }

    private void executarComandosConfigurados(Player player, String produto) {
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
        return pagamentos.get(playerName);
    }
}

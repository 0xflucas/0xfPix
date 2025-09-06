package io.qzz._0xflucas.database.repository;

import io.qzz._0xflucas.database.dao.PaymentDAO;
import io.qzz._0xflucas.models.Payment;
import org.bukkit.Bukkit;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class PaymentRepository {

    private final PaymentDAO dbManager;
    private final Map<String, Payment> cache = new  ConcurrentHashMap<>();

    public PaymentRepository(PaymentDAO dbManager) {
        this.dbManager = dbManager;
    }

    public void save(Payment payment) {
        dbManager.savePayment(payment);
        cache.put(payment.getId(), payment); // atualiza cache
    }

    /*
    public List<Payment> getAllByPlayer(UUID uuid) {
        return dbManager.getPaymentsByUUID(uuid);
    }
    */

    public List<Payment> getAllPaid(UUID uuid) {
        List<Payment> payments = dbManager.getPaymentsByUUID(uuid);
        if (payments == null) return Collections.emptyList();

        return payments.stream()
                .filter(p -> p != null && p.getStatus() != null && p.getStatus().equalsIgnoreCase("paid"))
                .filter(p -> p.getStatus() != null)
                .collect(Collectors.toList());
    }

    public List<Payment> getAll(UUID uuid) {
        List<Payment> payments = dbManager.getPaymentsByUUID(uuid);
        if (payments == null) return Collections.emptyList();

        return payments.stream()
//                .filter(p -> p != null && p.getStatus() != null && p.getStatus().equalsIgnoreCase("paid"))
                .filter(p -> p != null && p.getStatus() != null)
                .collect(Collectors.toList());
    }

    public Payment findById(String id) {
        // 1) tenta cache
        Payment cached = cache.get(id);
        if (cached != null) return cached;

        // 2) busca no banco via dbManager
        Payment fromDb = dbManager.findPaymentById(id);
        if (fromDb != null) {
            cache.put(id, fromDb);
            Bukkit.getConsoleSender().sendMessage("§a[0xfPix] Pagamento encontrado no DB: " + fromDb.getId() + " - " + fromDb.getItem());
            Bukkit.getConsoleSender().sendMessage("§a[0xfPix] Status: " + fromDb.getStatus() + " - Valor: R$ " + String.format("%.2f", fromDb.getAmount()));
        }

        return fromDb;
    }
}
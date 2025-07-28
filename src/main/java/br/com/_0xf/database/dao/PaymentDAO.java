package br.com._0xf.database.dao;

import br.com._0xf.Main;
import br.com._0xf.models.Payment;
import org.bukkit.Bukkit;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PaymentDAO {

    private final String host, database, username, password;
    private final int port;
    private Connection connection;
    private final Main main;

    public PaymentDAO(String host, int port, String database, String username, String password, Main main) {
        this.main = main;
        this.host = host;
        this.port = port;
        this.database = database;
        this.username = username;
        this.password = password;
    }

    public void connect() throws SQLException {
        if (connection != null && !connection.isClosed()) return;

        String url = "jdbc:mysql://" + host + ":" + port + "/" + database +
                "?useSSL=false&allowPublicKeyRetrieval=true&useUnicode=true&characterEncoding=UTF-8&serverTimezone=America/Sao_Paulo";

        connection = DriverManager.getConnection(url, username, password);

        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS payments (" +
                            "id VARCHAR(36) PRIMARY KEY, " +
                            "uuid VARCHAR(36) NOT NULL, " +
                            "item VARCHAR(255) NOT NULL, " +
                            "amount DOUBLE NOT NULL, " +
                            "status VARCHAR(20) NOT NULL, " +
                            "created_at BIGINT NOT NULL" +
                            ")"
            );
        }
    }

    public void disconnect() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void savePayment(Payment payment) {
        if (!isConnected()) {
            Bukkit.getConsoleSender().sendMessage("§c[0xfPix] Falha ao salvar pagamento: conexão MySQL indisponível.");
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(main, () -> {
            try {
                String query = "REPLACE INTO payments (id, uuid, item, amount, status, created_at) VALUES (?, ?, ?, ?, ?, ?)";
                try (PreparedStatement ps = connection.prepareStatement(query)) {
                    ps.setString(1, payment.getId());
                    ps.setString(2, payment.getUuid().toString());
                    ps.setString(3, payment.getItem());
                    ps.setDouble(4, payment.getAmount());
                    ps.setString(5, payment.getStatus());
                    ps.setLong(6, payment.getCreatedAt());
                    ps.executeUpdate();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    public List<Payment> getPaymentsByUUID(UUID uuid) {
        List<Payment> list = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT * FROM payments WHERE uuid = ? ORDER BY created_at DESC")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Payment(
                            rs.getString("id"),
                            uuid,
                            rs.getString("item"),
                            rs.getDouble("amount"),
                            rs.getString("status"),
                            rs.getLong("created_at")
                    ));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public Payment findPaymentById(String id) {
        try (PreparedStatement ps = connection.prepareStatement("SELECT * FROM payments WHERE id = ?")) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Payment(
                            rs.getString("id"),
                            UUID.fromString(rs.getString("uuid")),
                            rs.getString("item"),
                            rs.getDouble("amount"),
                            rs.getString("status"),
                            rs.getLong("created_at")
                    );
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }


    public boolean isConnected() {
        try {
            return connection != null && !connection.isClosed() && connection.isValid(3);
        } catch (SQLException e) {
            return false;
        }
    }

    public Connection getConnection() {
        return connection;
    }
}

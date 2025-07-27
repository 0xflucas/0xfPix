package br.com._0xf.models;

import java.util.UUID;

public class Payment {
    
    private String id;
    private UUID uuid;
    private String item;
    private double amount;
    private String status;
    private long createdAt;  // em milissegundos desde Epoch

    public Payment(String id, UUID uuid, String item, double amount, String status, long createdAt) {
        this.id = id;
        this.uuid = uuid;
        this.item = item;
        this.amount = amount;
        this.status = status;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public String getItem() {
        return item;
    }

    public void setItem(String item) {
        this.item = item;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
}

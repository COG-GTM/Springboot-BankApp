package com.example.bankapp.model;

import io.quarkus.mongodb.panache.PanacheMongoEntity;
import io.quarkus.mongodb.panache.common.MongoEntity;
import org.bson.types.ObjectId;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@MongoEntity(collection = "transactions")
public class Transaction extends PanacheMongoEntity {

    public BigDecimal amount;
    public String type;
    public LocalDateTime timestamp;
    public ObjectId accountId;

    public Transaction() {
    }

    public Transaction(BigDecimal amount, String type, LocalDateTime timestamp, ObjectId accountId) {
        this.amount = amount;
        this.type = type;
        this.timestamp = timestamp;
        this.accountId = accountId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isCredit() {
        return type != null && (type.contains("Transfer In") || type.equals("Deposit"));
    }

    public ObjectId getAccountId() {
        return accountId;
    }

    public void setAccountId(ObjectId accountId) {
        this.accountId = accountId;
    }
}

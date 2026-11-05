package com.example.bankapp.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Record of a transfer request that has already been applied, keyed by the
 * client supplied idempotency key. Used to make /transfer safe to retry.
 */
@Entity
@Table(name = "processed_transfer")
public class ProcessedTransfer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 64)
    private String idempotencyKey;

    @Column(name = "from_username", nullable = false)
    private String fromUsername;

    @Column(name = "processed_at", nullable = false)
    private LocalDateTime processedAt;

    public ProcessedTransfer() {
    }

    public ProcessedTransfer(String idempotencyKey, String fromUsername, LocalDateTime processedAt) {
        this.idempotencyKey = idempotencyKey;
        this.fromUsername = fromUsername;
        this.processedAt = processedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public String getFromUsername() {
        return fromUsername;
    }

    public void setFromUsername(String fromUsername) {
        this.fromUsername = fromUsername;
    }

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(LocalDateTime processedAt) {
        this.processedAt = processedAt;
    }
}

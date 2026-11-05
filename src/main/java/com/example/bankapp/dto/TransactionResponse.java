package com.example.bankapp.dto;

import com.example.bankapp.model.StoredValueTransaction;

import java.math.BigDecimal;
import java.time.Instant;

public record TransactionResponse(Long transactionId, String type, BigDecimal amount, BigDecimal balanceAfter,
                                  String idempotencyKey, Instant createdAt) {

    public static TransactionResponse from(StoredValueTransaction txn) {
        return new TransactionResponse(
                txn.getId(),
                txn.getType().name(),
                txn.getAmount(),
                txn.getBalanceAfter(),
                txn.getIdempotencyKey(),
                txn.getCreatedAt());
    }
}

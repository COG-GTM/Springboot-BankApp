package com.example.bankapp.dto;

import com.example.bankapp.model.StoredValueCard;
import com.example.bankapp.model.StoredValueTransaction;

import java.math.BigDecimal;
import java.time.Instant;

public record RedemptionResponse(Long transactionId, String cardToken, BigDecimal amountRedeemed,
                                 BigDecimal remainingBalance, String currency, String status,
                                 String idempotencyKey, Instant redeemedAt, boolean replayed) {

    public static RedemptionResponse from(StoredValueTransaction txn, StoredValueCard card, boolean replayed) {
        return new RedemptionResponse(
                txn.getId(),
                card.getCardToken(),
                txn.getAmount(),
                txn.getBalanceAfter(),
                card.getCurrency(),
                card.getStatus().name(),
                txn.getIdempotencyKey(),
                txn.getCreatedAt(),
                replayed);
    }
}

package com.example.bankapp.dto;

import com.example.bankapp.model.StoredValueCard;

import java.math.BigDecimal;
import java.time.Instant;

public record BalanceResponse(String cardToken, String currency, BigDecimal balance, String status,
                              Instant expiresAt, Instant asOf) {

    public static BalanceResponse from(StoredValueCard card, Instant asOf) {
        return new BalanceResponse(
                card.getCardToken(),
                card.getCurrency(),
                card.getBalance(),
                card.getStatus().name(),
                card.getExpiresAt(),
                asOf);
    }
}

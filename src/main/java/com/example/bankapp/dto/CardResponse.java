package com.example.bankapp.dto;

import com.example.bankapp.model.StoredValueCard;

import java.math.BigDecimal;
import java.time.Instant;

public record CardResponse(String cardToken, String currency, BigDecimal initialAmount, BigDecimal balance,
                           String status, Instant issuedAt, Instant expiresAt, Disclosure disclosure) {

    public static CardResponse from(StoredValueCard card) {
        return new CardResponse(
                card.getCardToken(),
                card.getCurrency(),
                card.getInitialAmount(),
                card.getBalance(),
                card.getStatus().name(),
                card.getIssuedAt(),
                card.getExpiresAt(),
                Disclosure.forCard(card));
    }
}

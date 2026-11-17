package com.example.bankapp.dto;

import com.example.bankapp.model.StoredValueCard;

import java.time.Instant;

/**
 * Consumer disclosure returned with every card representation. Fees are not assessed on
 * stored value issued through this API, and the expiry (if any) is always surfaced.
 */
public record Disclosure(Instant expiresAt, boolean feesAssessed, String feePolicy, String expiryPolicy) {

    public static final String FEE_POLICY = "No purchase, dormancy, inactivity or service fees are assessed on this card.";
    public static final String NO_EXPIRY_POLICY = "Funds on this card do not expire.";

    public static Disclosure forCard(StoredValueCard card) {
        String expiryPolicy = card.getExpiresAt() == null
                ? NO_EXPIRY_POLICY
                : "Funds on this card may not be redeemed on or after " + card.getExpiresAt() + ".";
        return new Disclosure(card.getExpiresAt(), false, FEE_POLICY, expiryPolicy);
    }
}

package com.example.bankapp.storedvalue;

import com.example.bankapp.exception.CardExpiredException;
import com.example.bankapp.exception.CardNotFoundException;
import com.example.bankapp.exception.IdempotencyKeyConflictException;
import com.example.bankapp.exception.InsufficientCardBalanceException;
import com.example.bankapp.exception.StoredValueException;
import com.example.bankapp.model.StoredValueCard;
import com.example.bankapp.model.StoredValueCardStatus;
import com.example.bankapp.model.StoredValueTransaction;
import com.example.bankapp.model.StoredValueTransactionType;
import com.example.bankapp.repository.StoredValueCardRepository;
import com.example.bankapp.repository.StoredValueTransactionRepository;
import com.example.bankapp.service.StoredValueService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StoredValueServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    private static final String TOKEN = "8f1a0e5c-0000-4000-8000-00000000abcd";

    @Mock
    private StoredValueCardRepository cardRepository;

    @Mock
    private StoredValueTransactionRepository transactionRepository;

    private StoredValueService service;

    @BeforeEach
    void setUp() {
        service = new StoredValueService(cardRepository, transactionRepository,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private StoredValueCard card(BigDecimal balance, Instant expiresAt) {
        StoredValueCard card = new StoredValueCard(TOKEN, "1234567890123456", "USD",
                new BigDecimal("100.00"), expiresAt, NOW.minusSeconds(3600));
        card.setId(1L);
        card.setBalance(balance);
        return card;
    }

    @Test
    void issueCardStartsWithFullBalanceAndIssueLedgerEntry() {
        when(cardRepository.save(any(StoredValueCard.class))).thenAnswer(inv -> inv.getArgument(0));
        when(transactionRepository.save(any(StoredValueTransaction.class))).thenAnswer(inv -> inv.getArgument(0));

        StoredValueCard issued = service.issueCard(new BigDecimal("25.00"), "usd", NOW.plusSeconds(86400));

        assertThat(issued.getBalance()).isEqualByComparingTo("25.00");
        assertThat(issued.getCurrency()).isEqualTo("USD");
        assertThat(issued.getStatus()).isEqualTo(StoredValueCardStatus.ACTIVE);
        assertThat(issued.getCardToken()).isNotBlank();
        assertThat(issued.getCardReference()).hasSize(16);
        verify(transactionRepository).save(any(StoredValueTransaction.class));
    }

    @Test
    void issuedTokenIsMaskedForLogging() {
        assertThat(StoredValueCard.maskToken(TOKEN)).isEqualTo("****abcd");
        assertThat(StoredValueCard.maskToken(null)).isEqualTo("****");
    }

    @Test
    void issueCardRejectsNonPositiveAmount() {
        assertThatThrownBy(() -> service.issueCard(new BigDecimal("0.00"), "USD", null))
                .isInstanceOf(StoredValueException.class)
                .hasMessageContaining("greater than zero");
    }

    @Test
    void issueCardRejectsAmountAboveTheCap() {
        assertThatThrownBy(() -> service.issueCard(new BigDecimal("10000.01"), "USD", null))
                .isInstanceOf(StoredValueException.class)
                .hasMessageContaining("must not exceed 10000.00");
    }

    @Test
    void issueCardRejectsExpiryInThePast() {
        assertThatThrownBy(() -> service.issueCard(new BigDecimal("10.00"), "USD", NOW.minusSeconds(1)))
                .isInstanceOf(StoredValueException.class)
                .hasMessageContaining("expiresAt must be in the future");
    }

    @Test
    void redeemRejectsExpiredCard() {
        when(cardRepository.findByCardTokenForUpdate(TOKEN))
                .thenReturn(Optional.of(card(new BigDecimal("100.00"), NOW.minusSeconds(1))));
        when(transactionRepository.findByCardIdAndIdempotencyKey(1L, "key-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.redeem(TOKEN, new BigDecimal("10.00"), "key-1"))
                .isInstanceOf(CardExpiredException.class);
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void redeemRejectsOverRedemption() {
        when(cardRepository.findByCardTokenForUpdate(TOKEN))
                .thenReturn(Optional.of(card(new BigDecimal("10.00"), null)));
        when(transactionRepository.findByCardIdAndIdempotencyKey(1L, "key-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.redeem(TOKEN, new BigDecimal("10.01"), "key-1"))
                .isInstanceOf(InsufficientCardBalanceException.class);
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void fullRedemptionDepletesCard() {
        StoredValueCard card = card(new BigDecimal("40.00"), null);
        when(cardRepository.findByCardTokenForUpdate(TOKEN)).thenReturn(Optional.of(card));
        when(transactionRepository.findByCardIdAndIdempotencyKey(1L, "key-1")).thenReturn(Optional.empty());
        when(transactionRepository.save(any(StoredValueTransaction.class))).thenAnswer(inv -> inv.getArgument(0));

        StoredValueService.Redemption redemption = service.redeem(TOKEN, new BigDecimal("40.00"), "key-1");

        assertThat(redemption.replayed()).isFalse();
        assertThat(card.getBalance()).isEqualByComparingTo("0.00");
        assertThat(card.getStatus()).isEqualTo(StoredValueCardStatus.DEPLETED);
        assertThat(redemption.transaction().getBalanceAfter()).isEqualByComparingTo("0.00");
    }

    @Test
    void replayingIdempotencyKeyReturnsOriginalWithoutMovingMoney() {
        StoredValueCard card = card(new BigDecimal("90.00"), null);
        StoredValueTransaction original = new StoredValueTransaction(card, StoredValueTransactionType.REDEMPTION,
                new BigDecimal("10.00"), new BigDecimal("90.00"), "key-1", NOW);
        when(cardRepository.findByCardTokenForUpdate(TOKEN)).thenReturn(Optional.of(card));
        when(transactionRepository.findByCardIdAndIdempotencyKey(1L, "key-1")).thenReturn(Optional.of(original));

        StoredValueService.Redemption redemption = service.redeem(TOKEN, new BigDecimal("10.00"), "key-1");

        assertThat(redemption.replayed()).isTrue();
        assertThat(card.getBalance()).isEqualByComparingTo("90.00");
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void replayOnAnExpiredCardStillReportsExpiredStatus() {
        StoredValueCard card = card(new BigDecimal("90.00"), NOW.minusSeconds(1));
        StoredValueTransaction original = new StoredValueTransaction(card, StoredValueTransactionType.REDEMPTION,
                new BigDecimal("10.00"), new BigDecimal("90.00"), "key-1", NOW);
        when(cardRepository.findByCardTokenForUpdate(TOKEN)).thenReturn(Optional.of(card));
        when(transactionRepository.findByCardIdAndIdempotencyKey(1L, "key-1")).thenReturn(Optional.of(original));

        StoredValueService.Redemption redemption = service.redeem(TOKEN, new BigDecimal("10.00"), "key-1");

        assertThat(redemption.replayed()).isTrue();
        assertThat(redemption.card().getStatus()).isEqualTo(StoredValueCardStatus.EXPIRED);
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void reusingIdempotencyKeyWithDifferentAmountIsRejected() {
        StoredValueCard card = card(new BigDecimal("90.00"), null);
        StoredValueTransaction original = new StoredValueTransaction(card, StoredValueTransactionType.REDEMPTION,
                new BigDecimal("10.00"), new BigDecimal("90.00"), "key-1", NOW);
        when(cardRepository.findByCardTokenForUpdate(TOKEN)).thenReturn(Optional.of(card));
        when(transactionRepository.findByCardIdAndIdempotencyKey(1L, "key-1")).thenReturn(Optional.of(original));

        assertThatThrownBy(() -> service.redeem(TOKEN, new BigDecimal("25.00"), "key-1"))
                .isInstanceOf(IdempotencyKeyConflictException.class);
    }

    @Test
    void unknownTokenIsNotFound() {
        when(cardRepository.findByCardToken("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getCard("missing")).isInstanceOf(CardNotFoundException.class);
    }

    @Test
    void balanceInquiryReportsExpiredStatusOncePastExpiry() {
        when(cardRepository.findByCardToken(TOKEN))
                .thenReturn(Optional.of(card(new BigDecimal("100.00"), NOW.minusSeconds(1))));

        assertThat(service.getCard(TOKEN).getStatus()).isEqualTo(StoredValueCardStatus.EXPIRED);
    }
}

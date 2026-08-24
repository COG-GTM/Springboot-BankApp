package com.example.bankapp.service;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Stored-value (gift card) ledger. All monetary state changes go through {@link #redeem}, which
 * takes a row-level lock on the card so concurrent redemptions cannot double-spend the balance.
 */
@Service
public class StoredValueService {

    private static final Logger log = LoggerFactory.getLogger(StoredValueService.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final StoredValueCardRepository cardRepository;
    private final StoredValueTransactionRepository transactionRepository;
    private final Clock clock;

    @Autowired
    public StoredValueService(StoredValueCardRepository cardRepository,
                              StoredValueTransactionRepository transactionRepository) {
        this(cardRepository, transactionRepository, Clock.systemUTC());
    }

    public StoredValueService(StoredValueCardRepository cardRepository,
                              StoredValueTransactionRepository transactionRepository,
                              Clock clock) {
        this.cardRepository = cardRepository;
        this.transactionRepository = transactionRepository;
        this.clock = clock;
    }

    @Transactional
    public StoredValueCard issueCard(BigDecimal amount, String currency, Instant expiresAt) {
        Instant now = clock.instant();
        requirePositiveMinorUnits(amount);
        if (expiresAt != null && !expiresAt.isAfter(now)) {
            throw new StoredValueException("INVALID_EXPIRY", HttpStatus.BAD_REQUEST,
                    "expiresAt must be in the future");
        }

        StoredValueCard card = new StoredValueCard(
                UUID.randomUUID().toString(),
                generateCardReference(),
                currency.toUpperCase(),
                amount.setScale(2, RoundingMode.UNNECESSARY),
                expiresAt,
                now);
        cardRepository.save(card);

        transactionRepository.save(new StoredValueTransaction(
                card, StoredValueTransactionType.ISSUE, card.getInitialAmount(), card.getBalance(), null, now));

        log.info("Issued stored-value card token={} currency={} amount={}",
                card.maskedToken(), card.getCurrency(), card.getInitialAmount());
        return card;
    }

    @Transactional(readOnly = true)
    public StoredValueCard getCard(String cardToken) {
        StoredValueCard card = cardRepository.findByCardToken(cardToken).orElseThrow(CardNotFoundException::new);
        card.applyExpiry(clock.instant());
        return card;
    }

    @Transactional(readOnly = true)
    public List<StoredValueTransaction> getTransactions(String cardToken) {
        StoredValueCard card = cardRepository.findByCardToken(cardToken).orElseThrow(CardNotFoundException::new);
        return transactionRepository.findByCardIdOrderByIdAsc(card.getId());
    }

    /**
     * Redeems part or all of a card balance. Idempotent per (card, Idempotency-Key): replaying the
     * same key with the same amount returns the original ledger entry without moving money.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Redemption redeem(String cardToken, BigDecimal amount, String idempotencyKey) {
        Instant now = clock.instant();
        requirePositiveMinorUnits(amount);
        StoredValueCard card = cardRepository.findByCardTokenForUpdate(cardToken)
                .orElseThrow(CardNotFoundException::new);

        StoredValueTransaction replay = transactionRepository
                .findByCardIdAndIdempotencyKey(card.getId(), idempotencyKey).orElse(null);
        if (replay != null) {
            if (replay.getAmount().compareTo(amount) != 0) {
                throw new IdempotencyKeyConflictException();
            }
            log.info("Replayed redemption for card token={} key={}", card.maskedToken(), idempotencyKey);
            return new Redemption(replay, card, true);
        }

        card.applyExpiry(now);
        if (card.getStatus() == StoredValueCardStatus.EXPIRED) {
            throw new CardExpiredException();
        }
        if (card.getBalance().compareTo(amount) < 0) {
            throw new InsufficientCardBalanceException(card.getBalance());
        }

        card.setBalance(card.getBalance().subtract(amount));
        if (card.getBalance().compareTo(BigDecimal.ZERO) == 0) {
            card.setStatus(StoredValueCardStatus.DEPLETED);
        }
        cardRepository.save(card);

        StoredValueTransaction txn = transactionRepository.save(new StoredValueTransaction(
                card, StoredValueTransactionType.REDEMPTION, amount, card.getBalance(), idempotencyKey, now));

        log.info("Redeemed {} from stored-value card token={} remainingBalance={}",
                amount, card.maskedToken(), card.getBalance());
        return new Redemption(txn, card, false);
    }

    private static void requirePositiveMinorUnits(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new StoredValueException("INVALID_AMOUNT", HttpStatus.BAD_REQUEST,
                    "amount must be greater than zero");
        }
        if (amount.stripTrailingZeros().scale() > 2) {
            throw new StoredValueException("INVALID_AMOUNT", HttpStatus.BAD_REQUEST,
                    "amount must have at most 2 decimal places");
        }
    }

    private static String generateCardReference() {
        StringBuilder reference = new StringBuilder(16);
        for (int i = 0; i < 16; i++) {
            reference.append(RANDOM.nextInt(10));
        }
        return reference.toString();
    }

    public record Redemption(StoredValueTransaction transaction, StoredValueCard card, boolean replayed) {
    }
}

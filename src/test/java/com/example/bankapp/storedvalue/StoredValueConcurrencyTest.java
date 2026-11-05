package com.example.bankapp.storedvalue;

import com.example.bankapp.exception.StoredValueException;
import com.example.bankapp.model.StoredValueCard;
import com.example.bankapp.model.StoredValueCardStatus;
import com.example.bankapp.repository.StoredValueCardRepository;
import com.example.bankapp.repository.StoredValueTransactionRepository;
import com.example.bankapp.service.StoredValueService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves the no-double-spend invariant: concurrent redeems against one card are serialised by the
 * pessimistic row lock, so exactly the funded number of redemptions succeed.
 */
@SpringBootTest
class StoredValueConcurrencyTest {

    private static final int THREADS = 20;
    private static final BigDecimal REDEEM_AMOUNT = new BigDecimal("10.00");

    @Autowired
    private StoredValueService storedValueService;

    @Autowired
    private StoredValueCardRepository cardRepository;

    @Autowired
    private StoredValueTransactionRepository transactionRepository;

    @Test
    void concurrentRedemptionsCannotOverdrawTheCard() throws Exception {
        StoredValueCard card = storedValueService.issueCard(new BigDecimal("100.00"), "USD", null);
        String token = card.getCardToken();

        AtomicInteger succeeded = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();
        ConcurrentLinkedQueue<Throwable> unexpected = new ConcurrentLinkedQueue<>();
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(THREADS);
        ExecutorService pool = Executors.newFixedThreadPool(THREADS);

        for (int i = 0; i < THREADS; i++) {
            final String key = "concurrent-" + i;
            pool.submit(() -> {
                try {
                    start.await();
                    storedValueService.redeem(token, REDEEM_AMOUNT, key);
                    succeeded.incrementAndGet();
                } catch (StoredValueException ex) {
                    rejected.incrementAndGet();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } catch (RuntimeException ex) {
                    unexpected.add(ex);
                } finally {
                    done.countDown();
                }
            });
        }

        start.countDown();
        assertThat(done.await(60, TimeUnit.SECONDS)).isTrue();
        pool.shutdownNow();

        StoredValueCard reloaded = cardRepository.findByCardToken(token).orElseThrow();
        assertThat(List.copyOf(unexpected)).isEmpty();
        assertThat(succeeded.get()).isEqualTo(10);
        assertThat(rejected.get()).isEqualTo(THREADS - 10);
        assertThat(reloaded.getBalance()).isEqualByComparingTo("0.00");
        assertThat(reloaded.getStatus()).isEqualTo(StoredValueCardStatus.DEPLETED);
        assertThat(transactionRepository.findByCardIdOrderByIdAsc(reloaded.getId())).hasSize(11);
    }

    @Test
    void concurrentReplaysOfTheSameIdempotencyKeyDebitOnce() throws Exception {
        StoredValueCard card = storedValueService.issueCard(new BigDecimal("100.00"), "USD", null);
        String token = card.getCardToken();
        String key = "single-key";

        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(THREADS);
        ExecutorService pool = Executors.newFixedThreadPool(THREADS);

        for (int i = 0; i < THREADS; i++) {
            pool.submit(() -> {
                try {
                    start.await();
                    storedValueService.redeem(token, REDEEM_AMOUNT, key);
                } catch (StoredValueException ex) {
                    // idempotent replay never throws; any other failure is caught by the assertions below
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }

        start.countDown();
        assertThat(done.await(60, TimeUnit.SECONDS)).isTrue();
        pool.shutdownNow();

        StoredValueCard reloaded = cardRepository.findByCardToken(token).orElseThrow();
        assertThat(reloaded.getBalance()).isEqualByComparingTo("90.00");
        assertThat(transactionRepository.findByCardIdOrderByIdAsc(reloaded.getId())).hasSize(2);
    }
}

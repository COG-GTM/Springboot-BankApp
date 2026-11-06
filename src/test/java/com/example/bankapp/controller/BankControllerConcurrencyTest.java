package com.example.bankapp.controller;

import com.example.bankapp.repository.AccountRepository;
import com.example.bankapp.repository.TransactionRepository;
import com.example.bankapp.service.AccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Drives the real HTTP endpoints so the requests go through open-in-view, where the controller's
 * Account stays managed for the whole request.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class BankControllerConcurrencyTest {

    @LocalServerPort
    private int port;

    @Autowired
    private AccountService accountService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    private final HttpClient http = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build();

    @BeforeEach
    void cleanDatabase() {
        transactionRepository.deleteAll();
        accountRepository.deleteAll();
    }

    @Test
    void concurrentWithdrawRequestsCannotOverdraw() throws Exception {
        accountService.registerAccount("mallory", "pw12345");
        String session = login("mallory", "pw12345");
        post(session, "/deposit", "amount=100");

        fireConcurrently(20, i -> post(session, "/withdraw", "amount=100"));

        assertThat(balanceOf("mallory")).isEqualByComparingTo("0.00");
        assertThat(transactionRepository.findByAccountId(idOf("mallory")))
                .filteredOn(t -> t.getType().equals("Withdrawal"))
                .hasSize(1);
    }

    @Test
    void concurrentTransferRequestsCannotSpendTheSameBalanceTwice() throws Exception {
        accountService.registerAccount("mallory", "pw12345");
        for (int i = 0; i < 10; i++) {
            accountService.registerAccount("accomplice" + i, "pw12345");
        }
        String session = login("mallory", "pw12345");
        post(session, "/deposit", "amount=100");

        fireConcurrently(10, i -> post(session, "/transfer", "toUsername=accomplice" + i + "&amount=100"));

        assertThat(balanceOf("mallory")).isEqualByComparingTo("0.00");
        BigDecimal received = BigDecimal.ZERO;
        for (int i = 0; i < 10; i++) {
            received = received.add(balanceOf("accomplice" + i));
        }
        assertThat(received).isEqualByComparingTo("100.00");
    }

    private interface Request {
        void send(int index) throws Exception;
    }

    private void fireConcurrently(int count, Request request) {
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            int index = i;
            futures.add(CompletableFuture.runAsync(() -> {
                try {
                    request.send(index);
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            }));
        }
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    }

    private String login(String username, String password) throws Exception {
        HttpResponse<Void> response = http.send(form(null, "/login", "username=" + username + "&password=" + password),
                HttpResponse.BodyHandlers.discarding());
        assertThat(response.headers().firstValue("Location")).hasValueSatisfying(l -> assertThat(l).endsWith("/dashboard"));
        return response.headers().allValues("Set-Cookie").stream()
                .filter(c -> c.startsWith("JSESSIONID="))
                .map(c -> c.substring(0, c.indexOf(';')))
                .findFirst()
                .orElseThrow();
    }

    private void post(String session, String path, String body) throws Exception {
        http.send(form(session, path, body), HttpResponse.BodyHandlers.discarding());
    }

    private HttpRequest form(String session, String path, String body) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body));
        if (session != null) {
            builder.header("Cookie", session);
        }
        return builder.build();
    }

    private Long idOf(String username) {
        return accountRepository.findByUsername(username).orElseThrow().getId();
    }

    private BigDecimal balanceOf(String username) {
        return accountRepository.findByUsername(username).orElseThrow().getBalance();
    }
}

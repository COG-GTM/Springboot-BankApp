package com.example.bankapp.storedvalue;

import com.example.bankapp.model.StoredValueCard;
import com.example.bankapp.repository.StoredValueCardRepository;
import com.example.bankapp.service.AccountService;
import com.example.bankapp.service.StoredValueService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class StoredValueApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccountService accountService;

    @Autowired
    private StoredValueService storedValueService;

    @Autowired
    private StoredValueCardRepository cardRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private String username;
    private final String password = "Partner@123";

    @BeforeEach
    void registerPartnerAccount() {
        username = "partner-" + UUID.randomUUID();
        accountService.registerAccount(username, password);
    }

    private RequestPostProcessor partner() {
        return httpBasic(username, password);
    }

    private String issueCard(String body) throws Exception {
        String response = mockMvc.perform(post("/api/v1/stored-value/cards")
                        .with(partner())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("cardToken").asText();
    }

    @Test
    void issueBalanceRedeemAndLedgerFlow() throws Exception {
        String token = issueCard("{\"amount\":50.00,\"currency\":\"USD\"}");

        mockMvc.perform(get("/api/v1/stored-value/cards/{token}/balance", token).with(partner()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(50.00))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.currency").value("USD"));

        mockMvc.perform(post("/api/v1/stored-value/cards/{token}/redeem", token)
                        .with(partner())
                        .header("Idempotency-Key", "redeem-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":20.00}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amountRedeemed").value(20.00))
                .andExpect(jsonPath("$.remainingBalance").value(30.00))
                .andExpect(jsonPath("$.replayed").value(false));

        mockMvc.perform(post("/api/v1/stored-value/cards/{token}/redeem", token)
                        .with(partner())
                        .header("Idempotency-Key", "redeem-2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":30.00}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.remainingBalance").value(0.00))
                .andExpect(jsonPath("$.status").value("DEPLETED"));

        String ledger = mockMvc.perform(get("/api/v1/stored-value/cards/{token}/transactions", token).with(partner()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].type").value("ISSUE"))
                .andExpect(jsonPath("$[1].type").value("REDEMPTION"))
                .andExpect(jsonPath("$[2].balanceAfter").value(0.00))
                .andReturn().getResponse().getContentAsString();
        assertThat(ledger).doesNotContain("cardReference");
    }

    @Test
    void issueResponseCarriesExpiryAndFeeDisclosure() throws Exception {
        String expiry = Instant.now().plusSeconds(86400).toString();
        mockMvc.perform(post("/api/v1/stored-value/cards")
                        .with(partner())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":15.00,\"currency\":\"GBP\",\"expiresAt\":\"" + expiry + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.disclosure.feesAssessed").value(false))
                .andExpect(jsonPath("$.disclosure.feePolicy").isNotEmpty())
                .andExpect(jsonPath("$.disclosure.expiryPolicy").isNotEmpty())
                .andExpect(jsonPath("$.expiresAt").isNotEmpty());
    }

    @Test
    void redemptionIsIdempotentPerKey() throws Exception {
        String token = issueCard("{\"amount\":40.00,\"currency\":\"USD\"}");
        String key = "partner-order-" + UUID.randomUUID();

        for (int attempt = 0; attempt < 2; attempt++) {
            mockMvc.perform(post("/api/v1/stored-value/cards/{token}/redeem", token)
                            .with(partner())
                            .header("Idempotency-Key", key)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"amount\":10.00}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.remainingBalance").value(30.00))
                    .andExpect(jsonPath("$.replayed").value(attempt == 1));
        }

        mockMvc.perform(get("/api/v1/stored-value/cards/{token}/transactions", token).with(partner()))
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void reusingIdempotencyKeyWithDifferentAmountConflicts() throws Exception {
        String token = issueCard("{\"amount\":40.00,\"currency\":\"USD\"}");
        String key = "partner-order-" + UUID.randomUUID();

        mockMvc.perform(post("/api/v1/stored-value/cards/{token}/redeem", token)
                        .with(partner())
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":10.00}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/stored-value/cards/{token}/redeem", token)
                        .with(partner())
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":11.00}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("IDEMPOTENCY_KEY_CONFLICT"));
    }

    @Test
    void overRedemptionIsRejected() throws Exception {
        String token = issueCard("{\"amount\":5.00,\"currency\":\"USD\"}");

        mockMvc.perform(post("/api/v1/stored-value/cards/{token}/redeem", token)
                        .with(partner())
                        .header("Idempotency-Key", "over-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":5.01}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INSUFFICIENT_BALANCE"));

        mockMvc.perform(get("/api/v1/stored-value/cards/{token}/balance", token).with(partner()))
                .andExpect(jsonPath("$.balance").value(5.00));
    }

    @Test
    void expiredCardCannotBeRedeemedAndReportsExpiredBalance() throws Exception {
        StoredValueCard card = storedValueService.issueCard(new BigDecimal("30.00"), "USD",
                Instant.now().plusSeconds(3600));
        StoredValueCard stored = cardRepository.findByCardToken(card.getCardToken()).orElseThrow();
        stored.setExpiresAt(Instant.now().minusSeconds(60));
        cardRepository.saveAndFlush(stored);

        mockMvc.perform(post("/api/v1/stored-value/cards/{token}/redeem", card.getCardToken())
                        .with(partner())
                        .header("Idempotency-Key", "expired-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":1.00}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CARD_EXPIRED"));

        mockMvc.perform(get("/api/v1/stored-value/cards/{token}/balance", card.getCardToken()).with(partner()))
                .andExpect(jsonPath("$.status").value("EXPIRED"));
    }

    @Test
    void validationFailuresReturnMeaningfulErrors() throws Exception {
        mockMvc.perform(post("/api/v1/stored-value/cards")
                        .with(partner())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":-5.00,\"currency\":\"usd\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.details.length()").value(2));

        String token = issueCard("{\"amount\":10.00,\"currency\":\"USD\"}");

        mockMvc.perform(post("/api/v1/stored-value/cards/{token}/redeem", token)
                        .with(partner())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":1.00}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MISSING_HEADER"));
    }

    @Test
    void unknownTokenReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/stored-value/cards/{token}/balance", UUID.randomUUID().toString())
                        .with(partner()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CARD_NOT_FOUND"));
    }

    @Test
    void apiRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/stored-value/cards/{token}/balance", UUID.randomUUID().toString()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.details").isArray())
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    void unsupportedMethodIsAClientErrorNotAServerError() throws Exception {
        mockMvc.perform(post("/api/v1/stored-value/cards/{token}/balance", UUID.randomUUID().toString())
                        .with(partner()))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void unsupportedContentTypeIsAClientError() throws Exception {
        mockMvc.perform(post("/api/v1/stored-value/cards")
                        .with(partner())
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("amount=10"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.code").value("UNSUPPORTED_MEDIA_TYPE"));
    }

    @Test
    void oversizedIdempotencyKeyIsRejectedAsAClientError() throws Exception {
        String token = issueCard("{\"amount\":10.00,\"currency\":\"USD\"}");

        mockMvc.perform(post("/api/v1/stored-value/cards/{token}/redeem", token)
                        .with(partner())
                        .header("Idempotency-Key", "k".repeat(129))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":1.00}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void wrongCredentialsReturnJsonUnauthorizedRatherThanLoginRedirect() throws Exception {
        mockMvc.perform(get("/api/v1/stored-value/cards/{token}/balance", UUID.randomUUID().toString())
                        .with(httpBasic(username, "not-the-password")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void responsesNeverExposeTheCardReference() throws Exception {
        String response = mockMvc.perform(post("/api/v1/stored-value/cards")
                        .with(partner())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":12.00,\"currency\":\"USD\"}"))
                .andReturn().getResponse().getContentAsString();

        JsonNode json = objectMapper.readTree(response);
        assertThat(json.has("cardReference")).isFalse();
        assertThat(json.get("cardToken").asText()).isNotBlank();
    }
}

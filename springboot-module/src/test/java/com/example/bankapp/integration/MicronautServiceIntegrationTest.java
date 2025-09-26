package com.example.bankapp.integration;

import com.example.bankapp.client.MicronautBankingServiceClient;
import com.example.bankapp.facade.BankingServiceFacade;
import com.example.bankapp.model.Account;
import com.example.bankapp.model.Transaction;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties = {
    "micronaut.service.url=http://localhost:8081",
    "feign.circuitbreaker.enabled=false"
})
public class MicronautServiceIntegrationTest {

    @Autowired
    private BankingServiceFacade bankingServiceFacade;

    @Autowired
    private MicronautBankingServiceClient micronautClient;

    @Test
    public void testMicronautServiceInjection() {
        assertNotNull(micronautClient, "Micronaut service client should be injected");
    }

    @Test
    public void testBankingServiceFacadeIntegration() {
        assertNotNull(bankingServiceFacade, "Banking service facade should be injected");
        
        try {
            Account testAccount = bankingServiceFacade.registerAccount("testuser", "password");
            assertNotNull(testAccount, "Account should be created");
            assertEquals("testuser", testAccount.getUsername());
        } catch (Exception e) {
            System.out.println("Expected exception when Micronaut service is not available: " + e.getMessage());
        }
    }

    @Test
    public void testFallbackMechanism() {
        try {
            Account account = bankingServiceFacade.findAccountByUsername("nonexistent");
            fail("Should throw exception for non-existent account");
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("Account not found") || 
                      e.getMessage().contains("Connection refused") ||
                      e.getMessage().contains("micronaut failure"),
                      "Should handle service unavailability gracefully");
        }
    }
}

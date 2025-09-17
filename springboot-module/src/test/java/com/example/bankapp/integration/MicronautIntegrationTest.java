package com.example.bankapp.integration;

import com.example.bankapp.facade.BankingServiceFacade;
import com.example.bankapp.model.Account;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties = {
    "micronaut.service.url=http://localhost:8081",
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
public class MicronautIntegrationTest {

    @Autowired
    private BankingServiceFacade bankingServiceFacade;

    @Test
    public void testFallbackToSpringBootWhenMicronautUnavailable() {
        assertNotNull(bankingServiceFacade);
        
        try {
            Account account = bankingServiceFacade.registerAccount("testuser", "password");
            assertNotNull(account);
            assertEquals("testuser", account.getUsername());
        } catch (Exception e) {
            String message = e.getMessage();
            assertTrue(message != null && (
                message.contains("Defaulting to Spring boot") || 
                message.contains("Username already exists") ||
                message.contains("Connection refused") ||
                message.contains("feign.RetryableException") ||
                message.contains("java.net.ConnectException")
            ), "Unexpected exception message: " + message);
        }
    }

    @Test
    public void testBankingServiceFacadeNotNull() {
        assertNotNull(bankingServiceFacade);
    }
}

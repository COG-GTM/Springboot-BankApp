package com.example.bankapp.integration;

import com.example.bankapp.facade.BankingServiceFacade;
import com.example.bankapp.model.Account;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties = {
    "micronaut.service.url=http://localhost:8081"
})
public class MicronautIntegrationTest {

    @Autowired
    private BankingServiceFacade bankingServiceFacade;

    @Test
    public void testMicronautServiceCommunication() {
        String testUsername = "testuser" + System.currentTimeMillis();
        
        Account account = bankingServiceFacade.registerAccount(testUsername, "password");
        assertNotNull(account);
        assertEquals(testUsername, account.getUsername());
        
        bankingServiceFacade.deposit(account, new BigDecimal("100.00"));
        
        Account retrievedAccount = bankingServiceFacade.findAccountByUsername(testUsername);
        assertEquals(new BigDecimal("100.00"), retrievedAccount.getBalance());
    }
}

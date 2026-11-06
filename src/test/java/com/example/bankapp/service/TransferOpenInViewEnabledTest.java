package com.example.bankapp.service;

import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "spring.jpa.open-in-view=true")
class TransferOpenInViewEnabledTest extends AbstractTransferIntegrationTest {
}

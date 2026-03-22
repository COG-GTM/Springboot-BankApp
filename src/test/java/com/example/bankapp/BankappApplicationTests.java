package com.example.bankapp;

import com.example.bankapp.config.SecurityConfig;
import com.example.bankapp.controller.BankController;
import com.example.bankapp.repository.AccountRepository;
import com.example.bankapp.repository.TransactionRepository;
import com.example.bankapp.service.AccountService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class BankappApplicationTests {

	@Autowired
	private ApplicationContext applicationContext;

	@Test
	void contextLoads() {
	}

	@Test
	void main_runsWithoutError() {
		assertDoesNotThrow(() -> BankappApplication.main(new String[]{}));
	}

	@Test
	void accountServiceBeanExists() {
		assertNotNull(applicationContext.getBean(AccountService.class));
	}

	@Test
	void accountRepositoryBeanExists() {
		assertNotNull(applicationContext.getBean(AccountRepository.class));
	}

	@Test
	void transactionRepositoryBeanExists() {
		assertNotNull(applicationContext.getBean(TransactionRepository.class));
	}

	@Test
	void bankControllerBeanExists() {
		assertNotNull(applicationContext.getBean(BankController.class));
	}

	@Test
	void securityConfigBeanExists() {
		assertNotNull(applicationContext.getBean(SecurityConfig.class));
	}
}

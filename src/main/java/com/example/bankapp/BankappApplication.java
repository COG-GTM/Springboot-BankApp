package com.example.bankapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class BankappApplication {

	public static void main(String[] args) {
		SpringApplication.run(BankappApplication.class, args);
	}

}

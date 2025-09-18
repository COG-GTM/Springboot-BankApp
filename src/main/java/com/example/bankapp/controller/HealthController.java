package com.example.bankapp.controller;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;

@Controller("/health")
public class HealthController {

    @Get
    public String health() {
        return "Micronaut BankApp is running!";
    }
}

package com.example.bankapp.repository;

import com.example.bankapp.model.Account;
import io.quarkus.mongodb.panache.PanacheMongoRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;

@ApplicationScoped
public class AccountRepository implements PanacheMongoRepository<Account> {

    public Optional<Account> findByUsername(String username) {
        return find("username", username).firstResultOptional();
    }
}

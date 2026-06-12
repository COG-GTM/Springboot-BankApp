package com.example.bankapp.repository;

import com.example.bankapp.model.Transaction;
import io.quarkus.mongodb.panache.PanacheMongoRepository;
import jakarta.enterprise.context.ApplicationScoped;
import org.bson.types.ObjectId;

import java.util.List;

@ApplicationScoped
public class TransactionRepository implements PanacheMongoRepository<Transaction> {

    public List<Transaction> findByAccountId(ObjectId accountId) {
        return list("accountId", accountId);
    }
}

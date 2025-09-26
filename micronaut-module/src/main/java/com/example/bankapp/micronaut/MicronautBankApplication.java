package com.example.bankapp.micronaut;

import com.example.bankapp.model.Account;
import com.example.bankapp.model.Transaction;
import io.micronaut.runtime.Micronaut;
import io.micronaut.serde.annotation.SerdeImport;

@SerdeImport(Account.class)
@SerdeImport(Transaction.class)
public class MicronautBankApplication {

    public static void main(String[] args) {
        Micronaut.run(MicronautBankApplication.class, args);
    }
}

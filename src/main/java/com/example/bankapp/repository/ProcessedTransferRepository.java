package com.example.bankapp.repository;

import com.example.bankapp.model.ProcessedTransfer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedTransferRepository extends JpaRepository<ProcessedTransfer, Long> {
    boolean existsByIdempotencyKey(String idempotencyKey);
}

package com.example.bankapp.repository;

import com.example.bankapp.model.StoredValueTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StoredValueTransactionRepository extends JpaRepository<StoredValueTransaction, Long> {

    List<StoredValueTransaction> findByCardIdOrderByIdAsc(Long cardId);

    Optional<StoredValueTransaction> findByCardIdAndIdempotencyKey(Long cardId, String idempotencyKey);
}

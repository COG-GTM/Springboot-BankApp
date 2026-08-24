package com.example.bankapp.repository;

import com.example.bankapp.model.StoredValueCard;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface StoredValueCardRepository extends JpaRepository<StoredValueCard, Long> {

    Optional<StoredValueCard> findByCardToken(String cardToken);

    /**
     * Row-level lock used by redemption so concurrent redeems of the same card serialise
     * instead of double-spending the balance.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from StoredValueCard c where c.cardToken = :cardToken")
    Optional<StoredValueCard> findByCardTokenForUpdate(@Param("cardToken") String cardToken);
}

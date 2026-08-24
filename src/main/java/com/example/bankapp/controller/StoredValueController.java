package com.example.bankapp.controller;

import com.example.bankapp.dto.BalanceResponse;
import com.example.bankapp.dto.CardResponse;
import com.example.bankapp.dto.IssueCardRequest;
import com.example.bankapp.dto.RedeemRequest;
import com.example.bankapp.dto.RedemptionResponse;
import com.example.bankapp.dto.TransactionResponse;
import com.example.bankapp.model.StoredValueCard;
import com.example.bankapp.service.StoredValueService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1/stored-value/cards")
@Validated
public class StoredValueController {

    private final StoredValueService storedValueService;

    @Autowired
    public StoredValueController(StoredValueService storedValueService) {
        this.storedValueService = storedValueService;
    }

    @PostMapping
    public ResponseEntity<CardResponse> issueCard(@Valid @RequestBody IssueCardRequest request) {
        StoredValueCard card = storedValueService.issueCard(request.amount(), request.currency(), request.expiresAt());
        return ResponseEntity.status(HttpStatus.CREATED).body(CardResponse.from(card));
    }

    @GetMapping("/{token}/balance")
    public BalanceResponse getBalance(@PathVariable String token) {
        return BalanceResponse.from(storedValueService.getCard(token), Instant.now());
    }

    @PostMapping("/{token}/redeem")
    public RedemptionResponse redeem(@PathVariable String token,
                                     @RequestHeader(name = "Idempotency-Key")
                                     @NotBlank(message = "Idempotency-Key header is required")
                                     @Size(max = 128, message = "Idempotency-Key must be at most 128 characters")
                                     String idempotencyKey,
                                     @Valid @RequestBody RedeemRequest request) {
        StoredValueService.Redemption redemption =
                storedValueService.redeem(token, request.amount(), idempotencyKey);
        return RedemptionResponse.from(redemption.transaction(), redemption.card(), redemption.replayed());
    }

    @GetMapping("/{token}/transactions")
    public List<TransactionResponse> getTransactions(@PathVariable String token) {
        return storedValueService.getTransactions(token).stream()
                .map(TransactionResponse::from)
                .toList();
    }
}

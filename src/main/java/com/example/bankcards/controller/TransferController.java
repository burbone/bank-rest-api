package com.example.bankcards.controller;

import com.example.bankcards.dto.TransactionDto;
import com.example.bankcards.dto.TransferRequest;
import com.example.bankcards.entity.Transaction;
import com.example.bankcards.service.CardService;
import com.example.bankcards.service.TransferService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transfers")
@RequiredArgsConstructor
@PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
public class TransferController {

    private final TransferService transferService;
    private final CardService cardService;

    @PostMapping
    public ResponseEntity<TransactionDto> transfer(@Valid @RequestBody TransferRequest request) {
        Transaction transaction = transferService.transferBetweenMyCards(
                request.getFromCardId(),
                request.getToCardId(),
                request.getAmount()
        );
        return ResponseEntity.ok(convertToDto(transaction));
    }

    @GetMapping("/my")
    public ResponseEntity<List<TransactionDto>> getMyTransactions() {
        List<TransactionDto> transactions = transferService.getMyTransactions()
                .stream()
                .map(this::convertToDto)
                .toList();
        return ResponseEntity.ok(transactions);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TransactionDto> getTransaction(@PathVariable Long id) {
        Transaction transaction = transferService.getTransactionById(id);
        return ResponseEntity.ok(convertToDto(transaction));
    }

    private TransactionDto convertToDto(Transaction transaction) {
        TransactionDto dto = new TransactionDto();
        dto.setId(transaction.getId());
        dto.setFromCardMasked(cardService.getMaskedCardNumber(transaction.getFromCard()));
        dto.setToCardMasked(cardService.getMaskedCardNumber(transaction.getToCard()));
        dto.setAmount(transaction.getAmount());
        dto.setTransactionDate(transaction.getTransactionDate());
        dto.setStatus(transaction.getStatus().name());
        dto.setDescription(transaction.getDescription());
        return dto;
    }
}

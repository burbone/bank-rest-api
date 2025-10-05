package com.example.bankcards.service;

import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.Transaction;
import com.example.bankcards.entity.User;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.TransactionRepository;
import com.example.bankcards.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TransferService {

    private final CardRepository cardRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    @Transactional
    public Transaction transferBetweenMyCards(Long fromCardId, Long toCardId, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Transfer amount must be positive");
        }

        User currentUser = getCurrentUser();

        Card fromCard = cardRepository.findById(fromCardId)
                .orElseThrow(() -> new RuntimeException("Source card not found"));

        Card toCard = cardRepository.findById(toCardId)
                .orElseThrow(() -> new RuntimeException("Destination card not found"));

        if (!fromCard.getOwner().getId().equals(currentUser.getId()) ||
                !toCard.getOwner().getId().equals(currentUser.getId())) {
            throw new RuntimeException("You can only transfer between your own cards");
        }

        if (fromCardId.equals(toCardId)) {
            throw new RuntimeException("Cannot transfer to the same card");
        }

        if (fromCard.getStatus() != Card.CardStatus.ACTIVE) {
            throw new RuntimeException("Source card is not active");
        }

        if (toCard.getStatus() != Card.CardStatus.ACTIVE) {
            throw new RuntimeException("Destination card is not active");
        }

        if (fromCard.getBalance().compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient balance");
        }

        fromCard.setBalance(fromCard.getBalance().subtract(amount));
        toCard.setBalance(toCard.getBalance().add(amount));

        cardRepository.save(fromCard);
        cardRepository.save(toCard);

        Transaction transaction = new Transaction();
        transaction.setFromCard(fromCard);
        transaction.setToCard(toCard);
        transaction.setAmount(amount);
        transaction.setStatus(Transaction.TransactionStatus.COMPLETED);
        transaction.setDescription("Transfer between own cards");

        return transactionRepository.save(transaction);
    }

    public List<Transaction> getMyTransactions() {
        User currentUser = getCurrentUser();

        List<Card> myCards = cardRepository.findByOwner(currentUser, null).getContent();

        if (myCards.isEmpty()) {
            return List.of();
        }

        return myCards.stream()
                .flatMap(card -> transactionRepository
                        .findByFromCardIdOrToCardId(card.getId(), card.getId())
                        .stream())
                .distinct()
                .toList();
    }

    public Transaction getTransactionById(Long id) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));

        User currentUser = getCurrentUser();

        if (!transaction.getFromCard().getOwner().getId().equals(currentUser.getId()) &&
                !transaction.getToCard().getOwner().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Access denied");
        }

        return transaction;
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
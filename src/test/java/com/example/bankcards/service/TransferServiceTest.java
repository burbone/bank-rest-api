package com.example.bankcards.service;

import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.Transaction;
import com.example.bankcards.entity.User;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.TransactionRepository;
import com.example.bankcards.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransferServiceTest {

    @Mock
    private CardRepository cardRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private TransferService transferService;

    private User testUser;
    private Card fromCard;
    private Card toCard;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setRole(User.Role.USER);

        fromCard = new Card();
        fromCard.setId(1L);
        fromCard.setBalance(new BigDecimal("1000.00"));
        fromCard.setStatus(Card.CardStatus.ACTIVE);
        fromCard.setOwner(testUser);
        fromCard.setExpireDate(LocalDate.now().plusYears(1));

        toCard = new Card();
        toCard.setId(2L);
        toCard.setBalance(new BigDecimal("500.00"));
        toCard.setStatus(Card.CardStatus.ACTIVE);
        toCard.setOwner(testUser);
        toCard.setExpireDate(LocalDate.now().plusYears(1));

        SecurityContextHolder.setContext(securityContext);
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        lenient().when(authentication.getName()).thenReturn("testuser");
        lenient().when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
    }

    @Test
    void transferBetweenMyCards_Success() {
        BigDecimal amount = new BigDecimal("100.00");

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(cardRepository.findById(1L)).thenReturn(Optional.of(fromCard));
        when(cardRepository.findById(2L)).thenReturn(Optional.of(toCard));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(i -> i.getArguments()[0]);

        Transaction result = transferService.transferBetweenMyCards(1L, 2L, amount);

        assertEquals(new BigDecimal("900.00"), fromCard.getBalance());
        assertEquals(new BigDecimal("600.00"), toCard.getBalance());
        assertEquals(amount, result.getAmount());
        assertEquals(Transaction.TransactionStatus.COMPLETED, result.getStatus());

        verify(cardRepository, times(2)).save(any(Card.class));
        verify(transactionRepository, times(1)).save(any(Transaction.class));
    }

    @Test
    void transferBetweenMyCards_InsufficientBalance() {
        BigDecimal amount = new BigDecimal("2000.00");

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(cardRepository.findById(1L)).thenReturn(Optional.of(fromCard));
        when(cardRepository.findById(2L)).thenReturn(Optional.of(toCard));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            transferService.transferBetweenMyCards(1L, 2L, amount);
        });

        assertEquals("Insufficient balance", exception.getMessage());
        verify(cardRepository, never()).save(any(Card.class));
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void transferBetweenMyCards_SameCard() {
        BigDecimal amount = new BigDecimal("100.00");

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        lenient().when(cardRepository.findById(1L)).thenReturn(Optional.of(fromCard));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            transferService.transferBetweenMyCards(1L, 1L, amount);
        });

        assertEquals("Cannot transfer to the same card", exception.getMessage());
    }

    @Test
    void transferBetweenMyCards_NegativeAmount() {
        BigDecimal amount = new BigDecimal("-100.00");

        lenient().when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            transferService.transferBetweenMyCards(1L, 2L, amount);
        });

        assertEquals("Transfer amount must be positive", exception.getMessage());
    }

    @Test
    void transferBetweenMyCards_BlockedSourceCard() {
        BigDecimal amount = new BigDecimal("100.00");
        fromCard.setStatus(Card.CardStatus.BLOCKED);

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(cardRepository.findById(1L)).thenReturn(Optional.of(fromCard));
        when(cardRepository.findById(2L)).thenReturn(Optional.of(toCard));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            transferService.transferBetweenMyCards(1L, 2L, amount);
        });

        assertEquals("Source card is not active", exception.getMessage());
    }

    @Test
    void transferBetweenMyCards_CardNotFound() {
        BigDecimal amount = new BigDecimal("100.00");

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(cardRepository.findById(1L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            transferService.transferBetweenMyCards(1L, 2L, amount);
        });

        assertEquals("Source card not found", exception.getMessage());
    }
}
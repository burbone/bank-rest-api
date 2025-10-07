package com.example.bankcards.service;

import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.User;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.util.EncryptionUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CardServiceTest {

    @Mock
    private CardRepository cardRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EncryptionUtil encryptionUtil;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private CardService cardService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setRole(User.Role.USER);
    }

    private void setupSecurityContext() {
        SecurityContextHolder.setContext(securityContext);
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        lenient().when(authentication.getName()).thenReturn("testuser");
        lenient().when(authentication.getAuthorities()).thenReturn(
                (Collection) Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );
        lenient().when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
    }

    @Test
    void createCard_Success() {
        setupSecurityContext();

        String cardNumber = "1111222233334444";
        String cardHolder = "Ivan Ivanov";
        LocalDate expireDate = LocalDate.now().plusYears(3);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(encryptionUtil.encrypt(cardNumber)).thenReturn("encrypted123");
        when(cardRepository.save(any(Card.class))).thenAnswer(i -> i.getArguments()[0]);

        Card result = cardService.createCard(cardNumber, cardHolder, expireDate, 1L);

        assertNotNull(result);
        assertEquals("encrypted123", result.getCardNumberEncrypted());
        assertEquals(cardHolder, result.getCardHolder());
        assertEquals(Card.CardStatus.ACTIVE, result.getStatus());
        assertEquals(BigDecimal.ZERO, result.getBalance());

        verify(cardRepository, times(1)).save(any(Card.class));
    }

    @Test
    void createCard_InvalidCardNumber() {
        String invalidCardNumber = "123";

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            cardService.createCard(invalidCardNumber, "Ivan Ivanov", LocalDate.now().plusYears(1), 1L);
        });

        assertEquals("Invalid card number format", exception.getMessage());
        verify(cardRepository, never()).save(any(Card.class));
    }

    @Test
    void createCard_ExpiredDate() {
        String cardNumber = "1111222233334444";
        LocalDate pastDate = LocalDate.now().minusDays(1);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            cardService.createCard(cardNumber, "Ivan Ivanov", pastDate, 1L);
        });

        assertEquals("Card expiration date is in the past", exception.getMessage());
    }

    @Test
    void blockCard_Success() {
        setupSecurityContext();

        Card card = new Card();
        card.setId(1L);
        card.setStatus(Card.CardStatus.ACTIVE);
        card.setOwner(testUser);

        when(cardRepository.findById(1L)).thenReturn(Optional.of(card));
        when(cardRepository.save(any(Card.class))).thenAnswer(i -> i.getArguments()[0]);

        Card result = cardService.blockCard(1L);

        assertEquals(Card.CardStatus.BLOCKED, result.getStatus());
        verify(cardRepository, times(1)).save(card);
    }
}
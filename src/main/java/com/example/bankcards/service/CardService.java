package com.example.bankcards.service;

import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.User;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.util.CardMaskingUtil;
import com.example.bankcards.util.EncryptionUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class CardService {

    private final CardRepository cardRepository;
    private final UserRepository userRepository;
    private final EncryptionUtil encryptionUtil;

    @Transactional
    public Card createCard(String cardNumber, String cardHolder, LocalDate expireDate, Long userId) {
        if (!CardMaskingUtil.isValidCardNumber(cardNumber)) {
            throw new RuntimeException("Invalid card number format");
        }

        User owner = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (expireDate.isBefore(LocalDate.now())) {
            throw new RuntimeException("Card expiration date is in the past");
        }

        Card card = new Card();
        card.setCardNumberEncrypted(encryptionUtil.encrypt(cardNumber));
        card.setCardHolder(cardHolder);
        card.setExpireDate(expireDate);
        card.setStatus(Card.CardStatus.ACTIVE);
        card.setBalance(BigDecimal.ZERO);
        card.setOwner(owner);

        return cardRepository.save(card);
    }

    public Page<Card> getMyCards(Pageable pageable) {
        User currentUser = getCurrentUser();
        return cardRepository.findByOwner(currentUser, pageable);
    }

    public Page<Card> getAllCards(Pageable pageable) {
        return cardRepository.findAll(pageable);
    }

    public Card getCardById(Long id) {
        Card card = cardRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Card not found"));

        User currentUser = getCurrentUser();
        if (!isAdmin() && !card.getOwner().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Access denied");
        }

        return card;
    }

    @Transactional
    public Card blockCard(Long id) {
        Card card = getCardById(id);

        if (card.getStatus() == Card.CardStatus.EXPIRED) {
            throw new RuntimeException("Cannot block expired card");
        }

        card.setStatus(Card.CardStatus.BLOCKED);
        return cardRepository.save(card);
    }

    @Transactional
    public Card activateCard(Long id) {
        Card card = getCardById(id);

        if (card.getExpireDate().isBefore(LocalDate.now())) {
            card.setStatus(Card.CardStatus.EXPIRED);
            throw new RuntimeException("Card has expired");
        }

        card.setStatus(Card.CardStatus.ACTIVE);
        return cardRepository.save(card);
    }

    @Transactional
    public void deleteCard(Long id) {
        Card card = cardRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Card not found"));

        if (card.getBalance().compareTo(BigDecimal.ZERO) > 0) {
            throw new RuntimeException("Cannot delete card with positive balance");
        }

        cardRepository.delete(card);
    }

    public String getMaskedCardNumber(Card card) {
        String decryptedNumber = encryptionUtil.decrypt(card.getCardNumberEncrypted());
        return CardMaskingUtil.maskCardNumber(decryptedNumber);
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private boolean isAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }
}
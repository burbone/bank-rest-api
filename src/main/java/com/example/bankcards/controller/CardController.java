package com.example.bankcards.controller;

import com.example.bankcards.dto.CardCreateRequest;
import com.example.bankcards.dto.CardDto;
import com.example.bankcards.entity.Card;
import com.example.bankcards.service.CardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cards")
@RequiredArgsConstructor
@Tag(name = "Cards", description = "Card management operations")
@SecurityRequirement(name = "Bearer Authentication")
public class CardController {

    private final CardService cardService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create new card", description = "Creates a new card for specified user (ADMIN only)")
    public ResponseEntity<CardDto> createCard(@Valid @RequestBody CardCreateRequest request) {
        Card card = cardService.createCard(
                request.getCardNumber(),
                request.getCardHolder(),
                request.getExpireDate(),
                request.getOwnerId()
        );
        return ResponseEntity.ok(convertToDto(card));
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    @Operation(summary = "Get cards", description = "Returns paginated list of current user's cards")
    public ResponseEntity<Page<CardDto>> getMyCards(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<CardDto> cards = cardService.getMyCards(pageable)
                .map(this::convertToDto);
        return ResponseEntity.ok(cards);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all cards", description = "Returns list of all cards (ADMIN only)")
    public ResponseEntity<Page<CardDto>> getAllCards(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<CardDto> cards = cardService.getAllCards(pageable)
                .map(this::convertToDto);
        return ResponseEntity.ok(cards);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    @Operation(summary = "Get card by ID", description = "Returns card details (owner or ADMIN only)")
    public ResponseEntity<CardDto> getCard(@PathVariable Long id) {
        Card card = cardService.getCardById(id);
        return ResponseEntity.ok(convertToDto(card));
    }

    @PutMapping("/{id}/block")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    @Operation(summary = "Block card", description = "Blocks the card (owner or ADMIN)")
    public ResponseEntity<CardDto> blockCard(@PathVariable Long id) {
        Card card = cardService.blockCard(id);
        return ResponseEntity.ok(convertToDto(card));
    }

    @PutMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Activate card", description = "Activates blocked card (ADMIN only)")
    public ResponseEntity<CardDto> activateCard(@PathVariable Long id) {
        Card card = cardService.activateCard(id);
        return ResponseEntity.ok(convertToDto(card));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete card", description = "Deletes card (ADMIN only)")
    public ResponseEntity<Void> deleteCard(@PathVariable Long id) {
        cardService.deleteCard(id);
        return ResponseEntity.noContent().build();
    }

    private CardDto convertToDto(Card card) {
        CardDto dto = new CardDto();
        dto.setId(card.getId());
        dto.setMaskedCardNumber(cardService.getMaskedCardNumber(card));
        dto.setCardHolder(card.getCardHolder());
        dto.setExpireDate(card.getExpireDate());
        dto.setStatus(card.getStatus().name());
        dto.setBalance(card.getBalance());
        dto.setOwnerId(card.getOwner().getId());
        dto.setOwnerUsername(card.getOwner().getUsername());
        return dto;
    }
}
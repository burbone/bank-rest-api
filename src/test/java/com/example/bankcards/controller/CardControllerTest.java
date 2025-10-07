package com.example.bankcards.controller;

import com.example.bankcards.dto.CardCreateRequest;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.User;
import com.example.bankcards.service.CardService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class CardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CardService cardService;

    @Test
    @WithMockUser(roles = "ADMIN")
    void createCard_ShouldReturnCardDto_WhenRequestIsValid() throws Exception {
        CardCreateRequest request = new CardCreateRequest(
                "1234567890123456",
                "Ivan Ivanov",
                LocalDate.now().plusYears(2),
                1L
        );

        User owner = new User();
        owner.setId(1L);
        owner.setUsername("testuser");

        Card card = new Card();
        card.setId(1L);
        card.setCardNumberEncrypted("encrypted");
        card.setCardHolder("Ivan Ivanov");
        card.setExpireDate(LocalDate.now().plusYears(2));
        card.setStatus(Card.CardStatus.ACTIVE);
        card.setBalance(BigDecimal.ZERO);
        card.setOwner(owner);

        when(cardService.createCard(anyString(), anyString(), any(LocalDate.class), anyLong()))
                .thenReturn(card);
        when(cardService.getMaskedCardNumber(any(Card.class)))
                .thenReturn("************3456");

        mockMvc.perform(post("/api/cards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.cardHolder").value("Ivan Ivanov"))
                .andExpect(jsonPath("$.maskedCardNumber").value("************3456"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createCard_ShouldReturnBadRequest_WhenCardNumberIsInvalid() throws Exception {
        String jsonRequest = "{\"cardNumber\":\"123\",\"cardHolder\":\"Ivan Ivanov\",\"expireDate\":\"2027-12-31\",\"ownerId\":1}";

        mockMvc.perform(post("/api/cards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "USER")
    void getMyCards_ShouldReturnPageOfCards() throws Exception {
        User owner = new User();
        owner.setId(1L);
        owner.setUsername("testuser");

        Card card = new Card();
        card.setId(1L);
        card.setCardHolder("Ivan Ivanov");
        card.setExpireDate(LocalDate.now().plusYears(2));
        card.setStatus(Card.CardStatus.ACTIVE);
        card.setBalance(BigDecimal.valueOf(1000));
        card.setOwner(owner);

        Page<Card> page = new PageImpl<>(List.of(card), PageRequest.of(0, 10), 1);

        when(cardService.getMyCards(any())).thenReturn(page);
        when(cardService.getMaskedCardNumber(any(Card.class))).thenReturn("************3456");

        mockMvc.perform(get("/api/cards/my")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].cardHolder").value("Ivan Ivanov"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAllCards_ShouldReturnPageOfCards() throws Exception {
        User owner = new User();
        owner.setId(1L);
        owner.setUsername("testuser");

        Card card = new Card();
        card.setId(2L);
        card.setCardHolder("Ivan Ivanov");
        card.setExpireDate(LocalDate.now().plusYears(2));
        card.setStatus(Card.CardStatus.ACTIVE);
        card.setBalance(BigDecimal.valueOf(2000));
        card.setOwner(owner);

        Page<Card> page = new PageImpl<>(List.of(card), PageRequest.of(0, 10), 1);

        when(cardService.getAllCards(any())).thenReturn(page);
        when(cardService.getMaskedCardNumber(card)).thenReturn("************9999");

        mockMvc.perform(get("/api/cards")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(2))
                .andExpect(jsonPath("$.content[0].cardHolder").value("Ivan Ivanov"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void getCard_ShouldReturnCard_WhenCardExists() throws Exception {
        User owner = new User();
        owner.setId(1L);
        owner.setUsername("testuser");

        Card card = new Card();
        card.setId(1L);
        card.setCardHolder("Ivan Ivanov");
        card.setExpireDate(LocalDate.now().plusYears(2));
        card.setStatus(Card.CardStatus.ACTIVE);
        card.setBalance(BigDecimal.valueOf(1000));
        card.setOwner(owner);

        when(cardService.getCardById(1L)).thenReturn(card);
        when(cardService.getMaskedCardNumber(any(Card.class))).thenReturn("************3456");

        mockMvc.perform(get("/api/cards/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.cardHolder").value("Ivan Ivanov"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void blockCard_ShouldReturnBlockedCard() throws Exception {
        User owner = new User();
        owner.setId(1L);
        owner.setUsername("testuser");

        Card card = new Card();
        card.setId(1L);
        card.setCardHolder("Ivan Ivanov");
        card.setExpireDate(LocalDate.now().plusYears(2));
        card.setStatus(Card.CardStatus.BLOCKED);
        card.setBalance(BigDecimal.valueOf(1000));
        card.setOwner(owner);

        when(cardService.blockCard(1L)).thenReturn(card);
        when(cardService.getMaskedCardNumber(any(Card.class))).thenReturn("************3456");

        mockMvc.perform(put("/api/cards/1/block"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("BLOCKED"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void activateCard_ShouldReturnActivatedCard() throws Exception {
        User owner = new User();
        owner.setId(1L);
        owner.setUsername("testuser");

        Card card = new Card();
        card.setId(1L);
        card.setCardHolder("Ivan Ivanov");
        card.setExpireDate(LocalDate.now().plusYears(2));
        card.setStatus(Card.CardStatus.ACTIVE);
        card.setBalance(BigDecimal.valueOf(1000));
        card.setOwner(owner);

        when(cardService.activateCard(1L)).thenReturn(card);
        when(cardService.getMaskedCardNumber(any(Card.class))).thenReturn("************3456");

        mockMvc.perform(put("/api/cards/1/activate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteCard_ShouldReturnNoContent() throws Exception {
        mockMvc.perform(delete("/api/cards/1"))
                .andExpect(status().isNoContent());
    }
}
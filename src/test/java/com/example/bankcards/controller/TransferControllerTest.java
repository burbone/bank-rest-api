package com.example.bankcards.controller;

import com.example.bankcards.dto.TransferRequest;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.Transaction;
import com.example.bankcards.entity.User;
import com.example.bankcards.service.CardService;
import com.example.bankcards.service.TransferService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class TransferControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TransferService transferService;

    @MockBean
    private CardService cardService;

    @Test
    @WithMockUser(roles = "USER")
    void transfer_ShouldReturnTransactionDto_WhenRequestIsValid() throws Exception {
        TransferRequest request = new TransferRequest(1L, 2L, BigDecimal.valueOf(100));

        User owner = new User();
        owner.setId(1L);
        owner.setUsername("testuser");

        Card fromCard = new Card();
        fromCard.setId(1L);
        fromCard.setCardHolder("Ivan Ivanov");
        fromCard.setExpireDate(LocalDate.now().plusYears(2));
        fromCard.setStatus(Card.CardStatus.ACTIVE);
        fromCard.setBalance(BigDecimal.valueOf(1000));
        fromCard.setOwner(owner);

        Card toCard = new Card();
        toCard.setId(2L);
        toCard.setCardHolder("Ivan Ivanov");
        toCard.setExpireDate(LocalDate.now().plusYears(2));
        toCard.setStatus(Card.CardStatus.ACTIVE);
        toCard.setBalance(BigDecimal.valueOf(500));
        toCard.setOwner(owner);

        Transaction transaction = new Transaction();
        transaction.setId(1L);
        transaction.setFromCard(fromCard);
        transaction.setToCard(toCard);
        transaction.setAmount(BigDecimal.valueOf(100));
        transaction.setTransactionDate(LocalDateTime.now());
        transaction.setStatus(Transaction.TransactionStatus.COMPLETED);
        transaction.setDescription("Transfer between own cards");

        when(transferService.transferBetweenMyCards(anyLong(), anyLong(), any(BigDecimal.class)))
                .thenReturn(transaction);
        when(cardService.getMaskedCardNumber(fromCard)).thenReturn("************1111");
        when(cardService.getMaskedCardNumber(toCard)).thenReturn("************2222");

        mockMvc.perform(post("/api/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.amount").value(100))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void transfer_ShouldReturnBadRequest_WhenAmountIsZero() throws Exception {
        String jsonRequest = "{\"fromCardId\":1,\"toCardId\":2,\"amount\":0}";

        mockMvc.perform(post("/api/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "USER")
    void transfer_ShouldReturnBadRequest_WhenAmountIsNegative() throws Exception {
        String jsonRequest = "{\"fromCardId\":1,\"toCardId\":2,\"amount\":-50}";

        mockMvc.perform(post("/api/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "USER")
    void getMyTransactions_ShouldReturnListOfTransactions() throws Exception {
        User owner = new User();
        owner.setId(1L);
        owner.setUsername("testuser");

        Card fromCard = new Card();
        fromCard.setId(1L);
        fromCard.setCardHolder("Ivan Ivanov");
        fromCard.setOwner(owner);

        Card toCard = new Card();
        toCard.setId(2L);
        toCard.setCardHolder("Ivan Ivanov");
        toCard.setOwner(owner);

        Transaction transaction = new Transaction();
        transaction.setId(1L);
        transaction.setFromCard(fromCard);
        transaction.setToCard(toCard);
        transaction.setAmount(BigDecimal.valueOf(100));
        transaction.setTransactionDate(LocalDateTime.now());
        transaction.setStatus(Transaction.TransactionStatus.COMPLETED);

        when(transferService.getMyTransactions()).thenReturn(List.of(transaction));
        when(cardService.getMaskedCardNumber(fromCard)).thenReturn("************1111");
        when(cardService.getMaskedCardNumber(toCard)).thenReturn("************2222");

        mockMvc.perform(get("/api/transfers/my"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].amount").value(100))
                .andExpect(jsonPath("$[0].status").value("COMPLETED"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void getTransaction_ShouldReturnTransaction_WhenTransactionExists() throws Exception {
        User owner = new User();
        owner.setId(1L);
        owner.setUsername("testuser");

        Card fromCard = new Card();
        fromCard.setId(1L);
        fromCard.setCardHolder("Ivan Ivanov");
        fromCard.setOwner(owner);

        Card toCard = new Card();
        toCard.setId(2L);
        toCard.setCardHolder("Ivan Ivanov");
        toCard.setOwner(owner);

        Transaction transaction = new Transaction();
        transaction.setId(1L);
        transaction.setFromCard(fromCard);
        transaction.setToCard(toCard);
        transaction.setAmount(BigDecimal.valueOf(100));
        transaction.setTransactionDate(LocalDateTime.now());
        transaction.setStatus(Transaction.TransactionStatus.COMPLETED);

        when(transferService.getTransactionById(1L)).thenReturn(transaction);
        when(cardService.getMaskedCardNumber(fromCard)).thenReturn("************1111");
        when(cardService.getMaskedCardNumber(toCard)).thenReturn("************2222");

        mockMvc.perform(get("/api/transfers/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.amount").value(100))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void transfer_ShouldWork_ForAdminRole() throws Exception {
        TransferRequest request = new TransferRequest(1L, 2L, BigDecimal.valueOf(50));

        User owner = new User();
        owner.setId(1L);
        owner.setUsername("admin");

        Card fromCard = new Card();
        fromCard.setId(1L);
        fromCard.setOwner(owner);

        Card toCard = new Card();
        toCard.setId(2L);
        toCard.setOwner(owner);

        Transaction transaction = new Transaction();
        transaction.setId(2L);
        transaction.setFromCard(fromCard);
        transaction.setToCard(toCard);
        transaction.setAmount(BigDecimal.valueOf(50));
        transaction.setTransactionDate(LocalDateTime.now());
        transaction.setStatus(Transaction.TransactionStatus.COMPLETED);

        when(transferService.transferBetweenMyCards(anyLong(), anyLong(), any(BigDecimal.class)))
                .thenReturn(transaction);
        when(cardService.getMaskedCardNumber(fromCard)).thenReturn("************3333");
        when(cardService.getMaskedCardNumber(toCard)).thenReturn("************4444");

        mockMvc.perform(post("/api/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.amount").value(50));
    }
}
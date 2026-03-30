package com.example.bankcards.service;

import com.example.bankcards.dto.common.PageResponse;
import com.example.bankcards.dto.transfer.CreateTransferRequest;
import com.example.bankcards.dto.transfer.TransferResponse;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.Transfer;
import com.example.bankcards.entity.User;
import com.example.bankcards.entity.enums.CardStatus;
import com.example.bankcards.exception.ConflictException;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.TransferRepository;
import com.example.bankcards.util.CardViewHelper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransferServiceImplTest {

    @Mock
    private CardRepository cardRepository;

    @Mock
    private TransferRepository transferRepository;

    @Mock
    private CardViewHelper cardViewHelper;

    @InjectMocks
    private TransferServiceImpl transferService;

    @Test
    void createTransferShouldMoveMoneyBetweenOwnedCards() {
        Card fromCard = createCard(10L, "**** **** **** 1111", "100.00", CardStatus.ACTIVE, LocalDate.now().plusYears(1));
        Card toCard = createCard(20L, "**** **** **** 2222", "40.00", CardStatus.ACTIVE, LocalDate.now().plusYears(1));

        CreateTransferRequest request = new CreateTransferRequest(
                10L,
                20L,
                new BigDecimal("25")
        );

        when(cardRepository.findAllByOwnerIdAndIdInForUpdate(7L, Set.of(10L, 20L)))
                .thenReturn(List.of(fromCard, toCard));
        when(cardRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(transferRepository.save(any(Transfer.class))).thenAnswer(invocation -> {
            Transfer transfer = invocation.getArgument(0);
            transfer.setId(99L);
            transfer.setCreatedAt(LocalDateTime.of(2026, 3, 26, 16, 30));
            return transfer;
        });
        when(cardViewHelper.resolveStatus(fromCard)).thenReturn(CardStatus.ACTIVE);
        when(cardViewHelper.resolveStatus(toCard)).thenReturn(CardStatus.ACTIVE);
        when(cardViewHelper.maskCardNumber(fromCard)).thenReturn("**** **** **** 1111");
        when(cardViewHelper.maskCardNumber(toCard)).thenReturn("**** **** **** 2222");

        TransferResponse response = transferService.createTransfer(7L, request);

        assertThat(fromCard.getBalance()).isEqualByComparingTo("75.00");
        assertThat(toCard.getBalance()).isEqualByComparingTo("65.00");

        ArgumentCaptor<Transfer> transferCaptor = ArgumentCaptor.forClass(Transfer.class);
        verify(transferRepository).save(transferCaptor.capture());
        Transfer savedTransfer = transferCaptor.getValue();

        assertThat(savedTransfer.getFromCard()).isEqualTo(fromCard);
        assertThat(savedTransfer.getToCard()).isEqualTo(toCard);
        assertThat(savedTransfer.getAmount()).isEqualByComparingTo("25.00");

        assertThat(response.id()).isEqualTo(99L);
        assertThat(response.fromCardId()).isEqualTo(10L);
        assertThat(response.toCardId()).isEqualTo(20L);
        assertThat(response.fromCardMaskedNumber()).isEqualTo("**** **** **** 1111");
        assertThat(response.toCardMaskedNumber()).isEqualTo("**** **** **** 2222");
        assertThat(response.amount()).isEqualByComparingTo("25.00");
    }

    @Test
    void createTransferShouldRejectSameCardTransfer() {
        CreateTransferRequest request = new CreateTransferRequest(
                10L,
                10L,
                new BigDecimal("10.00")
        );

        assertThatThrownBy(() -> transferService.createTransfer(7L, request))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Transfer between the same card is not allowed");

        verifyNoInteractions(cardRepository, transferRepository);
    }

    @Test
    void createTransferShouldRejectInsufficientBalance() {
        Card fromCard = createCard(10L, "**** **** **** 1111", "10.00", CardStatus.ACTIVE, LocalDate.now().plusYears(1));
        Card toCard = createCard(20L, "**** **** **** 2222", "40.00", CardStatus.ACTIVE, LocalDate.now().plusYears(1));

        CreateTransferRequest request = new CreateTransferRequest(
                10L,
                20L,
                new BigDecimal("25.00")
        );

        when(cardRepository.findAllByOwnerIdAndIdInForUpdate(7L, Set.of(10L, 20L)))
                .thenReturn(List.of(fromCard, toCard));
        when(cardViewHelper.resolveStatus(fromCard)).thenReturn(CardStatus.ACTIVE);
        when(cardViewHelper.resolveStatus(toCard)).thenReturn(CardStatus.ACTIVE);

        assertThatThrownBy(() -> transferService.createTransfer(7L, request))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Insufficient balance");
    }

    @Test
    void getCurrentUserTransfersShouldReturnPagedHistory() {
        Card fromCard = createCard(10L, "**** **** **** 1111", "100.00", CardStatus.ACTIVE, LocalDate.now().plusYears(1));
        Card toCard = createCard(20L, "**** **** **** 2222", "40.00", CardStatus.ACTIVE, LocalDate.now().plusYears(1));

        Transfer transfer = new Transfer();
        transfer.setId(1L);
        transfer.setFromCard(fromCard);
        transfer.setToCard(toCard);
        transfer.setAmount(new BigDecimal("15.00"));
        transfer.setCreatedAt(LocalDateTime.of(2026, 3, 26, 16, 40));

        when(cardRepository.findByIdAndOwnerId(10L, 7L)).thenReturn(Optional.of(fromCard));
        when(transferRepository.findAllByOwnerId(eq(7L), eq(10L), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(transfer), PageRequest.of(0, 10), 1));
        when(cardViewHelper.maskCardNumber(fromCard)).thenReturn("**** **** **** 1111");
        when(cardViewHelper.maskCardNumber(toCard)).thenReturn("**** **** **** 2222");

        PageResponse<TransferResponse> response = transferService.getCurrentUserTransfers(7L, 10L, PageRequest.of(0, 10));

        assertThat(response.totalElements()).isEqualTo(1);
        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).amount()).isEqualByComparingTo("15.00");
        assertThat(response.content().get(0).fromCardMaskedNumber()).isEqualTo("**** **** **** 1111");
    }

    private Card createCard(
            Long cardId,
            String maskedNumber,
            String balance,
            CardStatus status,
            LocalDate expirationDate
    ) {
        User owner = new User();
        owner.setId(7L);
        owner.setUsername("user");
        owner.setEmail("user@bankcards.local");

        Card card = new Card();
        card.setId(cardId);
        card.setOwner(owner);
        card.setCardNumberMasked(maskedNumber);
        card.setBalance(new BigDecimal(balance));
        card.setStatus(status);
        card.setExpirationDate(expirationDate);
        card.setBlockRequested(false);
        return card;
    }
}

package com.example.bankcards.service;

import com.example.bankcards.dto.card.CardResponse;
import com.example.bankcards.dto.card.CreateCardRequest;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.User;
import com.example.bankcards.entity.enums.CardStatus;
import com.example.bankcards.exception.ConflictException;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.util.CardNumberCodec;
import com.example.bankcards.util.CardViewHelper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CardServiceImplTest {

    @Mock
    private CardRepository cardRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CardNumberCodec cardNumberCodec;

    @Mock
    private CardViewHelper cardViewHelper;

    @InjectMocks
    private CardServiceImpl cardService;

    @Test
    void createCardShouldEncryptAndPersistNewCard() {
        User owner = new User();
        owner.setId(1L);
        owner.setUsername("user");
        owner.setEmail("user@bankcards.local");
        owner.setEnabled(true);

        CreateCardRequest request = new CreateCardRequest(
                1L,
                LocalDate.now().plusYears(3),
                new BigDecimal("1500")
        );

        when(userRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(owner));
        when(cardNumberCodec.generateCardNumber()).thenReturn("4000001234567899");
        when(cardNumberCodec.hash("4000001234567899")).thenReturn("hash");
        when(cardRepository.existsByCardNumberHash("hash")).thenReturn(false);
        when(cardNumberCodec.encrypt("4000001234567899")).thenReturn("encrypted");
        when(cardRepository.save(any(Card.class))).thenAnswer(invocation -> {
            Card card = invocation.getArgument(0);
            card.setId(10L);
            return card;
        });
        when(cardNumberCodec.mask("4000001234567899")).thenReturn("**** **** **** 7899");
        when(cardViewHelper.resolveStatus(any(Card.class))).thenReturn(CardStatus.ACTIVE);
        when(cardViewHelper.maskCardNumber(any(Card.class))).thenReturn("**** **** **** 7899");
        when(cardViewHelper.resolveBlockRequested(any(Card.class))).thenReturn(false);

        CardResponse response = cardService.createCard(request);

        ArgumentCaptor<Card> cardCaptor = ArgumentCaptor.forClass(Card.class);
        verify(cardRepository).save(cardCaptor.capture());
        Card savedCard = cardCaptor.getValue();

        assertThat(savedCard.getOwner()).isEqualTo(owner);
        assertThat(savedCard.getCardNumberEncrypted()).isEqualTo("encrypted");
        assertThat(savedCard.getCardNumberHash()).isEqualTo("hash");
        assertThat(savedCard.getStatus()).isEqualTo(CardStatus.ACTIVE);
        assertThat(savedCard.getBlockRequested()).isFalse();
        assertThat(savedCard.getBalance()).isEqualByComparingTo("1500.00");

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.maskedCardNumber()).isEqualTo("**** **** **** 7899");
        assertThat(response.ownerId()).isEqualTo(1L);
        assertThat(response.status()).isEqualTo("ACTIVE");
        assertThat(response.balance()).isEqualByComparingTo("1500.00");
    }

    @Test
    void requestBlockShouldMarkCardAsPending() {
        User owner = new User();
        owner.setId(7L);
        owner.setUsername("user");
        owner.setEmail("user@bankcards.local");

        Card card = new Card();
        card.setId(9L);
        card.setOwner(owner);
        card.setStatus(CardStatus.ACTIVE);
        card.setBlockRequested(false);
        card.setCardNumberEncrypted("encrypted");
        card.setBalance(new BigDecimal("200.00"));
        card.setExpirationDate(LocalDate.now().plusYears(1));

        when(cardRepository.findByIdAndOwnerId(9L, 7L)).thenReturn(Optional.of(card));
        when(cardRepository.save(card)).thenReturn(card);
        when(cardViewHelper.isExpired(card)).thenReturn(false);
        when(cardViewHelper.resolveStatus(card)).thenReturn(CardStatus.ACTIVE);
        when(cardViewHelper.maskCardNumber(card)).thenReturn("**** **** **** 7899");
        when(cardViewHelper.resolveBlockRequested(card)).thenReturn(true);

        CardResponse response = cardService.requestBlock(7L, 9L);

        assertThat(card.getBlockRequested()).isTrue();
        assertThat(response.blockRequested()).isTrue();
        assertThat(response.status()).isEqualTo("ACTIVE");
    }

    @Test
    void activateCardShouldRejectExpiredCard() {
        User owner = new User();
        owner.setId(1L);

        Card card = new Card();
        card.setId(5L);
        card.setOwner(owner);
        card.setStatus(CardStatus.EXPIRED);
        card.setCardNumberEncrypted("encrypted");
        card.setExpirationDate(LocalDate.now().minusDays(1));

        when(cardRepository.findById(5L)).thenReturn(Optional.of(card));
        when(cardViewHelper.isExpired(card)).thenReturn(true);

        assertThatThrownBy(() -> cardService.activateCard(5L))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Expired card cannot be changed");
    }
}

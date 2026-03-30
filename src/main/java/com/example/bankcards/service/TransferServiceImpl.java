package com.example.bankcards.service;

import com.example.bankcards.dto.common.PageResponse;
import com.example.bankcards.dto.transfer.CreateTransferRequest;
import com.example.bankcards.dto.transfer.TransferResponse;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.Transfer;
import com.example.bankcards.entity.enums.CardStatus;
import com.example.bankcards.exception.ConflictException;
import com.example.bankcards.exception.ResourceNotFoundException;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.TransferRepository;
import com.example.bankcards.util.CardViewHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TransferServiceImpl implements TransferService {

    private final CardRepository cardRepository;
    private final TransferRepository transferRepository;
    private final CardViewHelper cardViewHelper;

    @Override
    @Transactional
    public TransferResponse createTransfer(Long userId, CreateTransferRequest request) {
        if (request.fromCardId().equals(request.toCardId())) {
            throw new ConflictException("Transfer between the same card is not allowed");
        }

        BigDecimal amount = normalizeAmount(request.amount());

        List<Card> cards = cardRepository.findAllByOwnerIdAndIdInForUpdate(
                userId,
                Set.of(request.fromCardId(), request.toCardId())
        );

        if (cards.size() != 2) {
            throw new ResourceNotFoundException("One or both cards not found");
        }

        Map<Long, Card> cardsById = cards.stream()
                .collect(Collectors.toMap(Card::getId, Function.identity()));

        Card fromCard = cardsById.get(request.fromCardId());
        Card toCard = cardsById.get(request.toCardId());

        if (fromCard == null || toCard == null) {
            throw new ResourceNotFoundException("One or both cards not found");
        }

        assertTransferAllowed(fromCard, "Source card");
        assertTransferAllowed(toCard, "Destination card");

        if (fromCard.getBalance().compareTo(amount) < 0) {
            throw new ConflictException("Insufficient balance");
        }

        fromCard.setBalance(fromCard.getBalance().subtract(amount).setScale(2, RoundingMode.HALF_UP));
        toCard.setBalance(toCard.getBalance().add(amount).setScale(2, RoundingMode.HALF_UP));

        cardRepository.saveAll(cards);

        Transfer transfer = new Transfer();
        transfer.setFromCard(fromCard);
        transfer.setToCard(toCard);
        transfer.setAmount(amount);

        return toResponse(transferRepository.save(transfer));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<TransferResponse> getCurrentUserTransfers(Long userId, Long cardId, Pageable pageable) {
        if (cardId != null) {
            cardRepository.findByIdAndOwnerId(cardId, userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Card not found"));
        }

        Page<TransferResponse> page = transferRepository.findAllByOwnerId(userId, cardId, pageable)
                .map(this::toResponse);

        return PageResponse.from(page);
    }

    private void assertTransferAllowed(Card card, String cardLabel) {
        CardStatus status = cardViewHelper.resolveStatus(card);

        if (status == CardStatus.EXPIRED) {
            throw new ConflictException(cardLabel + " is expired");
        }

        if (status == CardStatus.BLOCKED) {
            throw new ConflictException(cardLabel + " is blocked");
        }
    }

    private TransferResponse toResponse(Transfer transfer) {
        return new TransferResponse(
                transfer.getId(),
                transfer.getFromCard().getId(),
                cardViewHelper.maskCardNumber(transfer.getFromCard()),
                transfer.getToCard().getId(),
                cardViewHelper.maskCardNumber(transfer.getToCard()),
                transfer.getAmount(),
                transfer.getCreatedAt()
        );
    }

    private BigDecimal normalizeAmount(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP);
    }
}

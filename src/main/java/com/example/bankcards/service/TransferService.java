package com.example.bankcards.service;

import com.example.bankcards.dto.common.PageResponse;
import com.example.bankcards.dto.transfer.CreateTransferRequest;
import com.example.bankcards.dto.transfer.TransferResponse;
import org.springframework.data.domain.Pageable;

public interface TransferService {

    TransferResponse createTransfer(Long userId, CreateTransferRequest request);

    PageResponse<TransferResponse> getCurrentUserTransfers(Long userId, Long cardId, Pageable pageable);
}

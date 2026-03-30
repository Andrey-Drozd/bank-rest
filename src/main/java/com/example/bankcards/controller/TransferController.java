package com.example.bankcards.controller;

import com.example.bankcards.dto.common.PageResponse;
import com.example.bankcards.dto.transfer.CreateTransferRequest;
import com.example.bankcards.dto.transfer.TransferResponse;
import com.example.bankcards.security.UserPrincipal;
import com.example.bankcards.service.TransferService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/transfers")
@RequiredArgsConstructor
public class TransferController {

    private final TransferService transferService;

    @PostMapping("/me")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public TransferResponse createTransfer(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateTransferRequest request
    ) {
        return transferService.createTransfer(principal.id(), request);
    }

    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public PageResponse<TransferResponse> getMyTransfers(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) @Positive Long cardId,
            @RequestParam(defaultValue = "0") @PositiveOrZero int page,
            @RequestParam(defaultValue = "10") @Positive @Max(100) int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return transferService.getCurrentUserTransfers(principal.id(), cardId, pageable);
    }
}

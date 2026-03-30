package com.example.bankcards.entity;

import com.example.bankcards.entity.enums.CardStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "cards",
        indexes = {
                @Index(name = "idx_cards_owner_id", columnList = "owner_id"),
                @Index(name = "idx_cards_status", columnList = "status"),
                @Index(name = "idx_cards_owner_status", columnList = "owner_id,status")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class Card {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(name = "card_number_encrypted", nullable = false, length = 512)
    private String cardNumberEncrypted;

    @Column(name = "card_number_hash", nullable = false, unique = true, length = 128)
    private String cardNumberHash;

    @Column(name = "card_number_masked", length = 19)
    private String cardNumberMasked;

    @Column(name = "expiration_date", nullable = false)
    private LocalDate expirationDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private CardStatus status = CardStatus.ACTIVE;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(name = "block_requested", nullable = false)
    private Boolean blockRequested = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Version
    @Column(nullable = false)
    private Long version = 0L;

    @OneToMany(mappedBy = "fromCard", fetch = FetchType.LAZY)
    private List<Transfer> outgoingTransfers = new ArrayList<>();

    @OneToMany(mappedBy = "toCard", fetch = FetchType.LAZY)
    private List<Transfer> incomingTransfers = new ArrayList<>();

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

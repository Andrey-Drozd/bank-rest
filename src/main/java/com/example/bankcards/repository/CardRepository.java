package com.example.bankcards.repository;

import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.enums.CardStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface CardRepository extends JpaRepository<Card, Long>, JpaSpecificationExecutor<Card> {

    boolean existsByCardNumberHash(String cardNumberHash);

    Page<Card> findAllByOwnerId(Long ownerId, Pageable pageable);

    Optional<Card> findByIdAndOwnerId(Long id, Long ownerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select card
            from Card card
            where card.owner.id = :ownerId
              and card.id in :cardIds
            order by card.id asc
            """)
    List<Card> findAllByOwnerIdAndIdInForUpdate(@Param("ownerId") Long ownerId, @Param("cardIds") Collection<Long> cardIds);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update Card card
            set card.status = :expiredStatus,
                card.blockRequested = false
            where card.status <> :expiredStatus
              and card.expirationDate < :today
            """)
    int markExpiredCards(@Param("today") LocalDate today, @Param("expiredStatus") CardStatus expiredStatus);
}

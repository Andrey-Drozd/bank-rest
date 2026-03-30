package com.example.bankcards.repository;

import com.example.bankcards.entity.Transfer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TransferRepository extends JpaRepository<Transfer, Long> {

    @EntityGraph(attributePaths = {"fromCard", "toCard"})
    @Query("""
            select transfer
            from Transfer transfer
            join transfer.fromCard fromCard
            join transfer.toCard toCard
            where fromCard.owner.id = :ownerId
              and toCard.owner.id = :ownerId
              and (:cardId is null or fromCard.id = :cardId or toCard.id = :cardId)
            """)
    Page<Transfer> findAllByOwnerId(@Param("ownerId") Long ownerId, @Param("cardId") Long cardId, Pageable pageable);
}

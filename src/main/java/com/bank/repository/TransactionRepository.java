package com.bank.repository;

import com.bank.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

// public interface TransactionRepository extends JpaRepository<Transaction, Long> {
// }

public interface TransactionRepository extends JpaRepository<Transaction, Long>,
        JpaSpecificationExecutor<Transaction> {
                List<Transaction> findByAccountIdAndActiveTrue(Long accountId);
                Page<Transaction> findByAccountIdAndActiveTrue(Long accountId, Pageable pageable);
                Page<Transaction> findByAccountOwnerIdAndActiveTrue(Long ownerId, Pageable pageable);

                long countByAccountIdAndCreatedAtAfterAndActiveTrue(Long accountId, LocalDateTime after);

    List<Transaction> findByAccountIdAndCreatedAtBetweenOrderByCreatedAtAsc(
            Long accountId, LocalDateTime from, LocalDateTime to);

    List<Transaction> findByCreatedAtBetweenOrderByCreatedAtAsc(LocalDateTime from, LocalDateTime to);

    @Query("SELECT COUNT(t), COALESCE(SUM(t.amount), 0), COALESCE(AVG(t.amount), 0), " +
           "COALESCE(MAX(t.amount), 0), COALESCE(MIN(t.amount), 0) " +
           "FROM Transaction t " +
           "WHERE t.createdAt BETWEEN :from AND :to " +
           "AND (:ownerId IS NULL OR t.account.owner.id = :ownerId)")
    List<Object[]> aggregateStats(@Param("from") LocalDateTime from,
                             @Param("to") LocalDateTime to,
                             @Param("ownerId") Long ownerId);
}
package com.banking.core.repository;

import com.banking.core.entity.Transaction;
import com.banking.core.entity.TransactionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Transfer işlem veri erişim katmanı.
 */
@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    /**
     * Belirli bir hesabın tüm işlem geçmişini sayfalı olarak döner.
     * Hem gönderilen hem alınan işlemler dahil edilir.
     */
    @Query("""
            SELECT t FROM Transaction t
            WHERE (t.fromAccount.id = :accountId OR t.toAccount.id = :accountId)
            AND (:status IS NULL OR t.status = :status)
            ORDER BY t.createdAt DESC
            """)
    Page<Transaction> findByAccountIdAndStatus(
            @Param("accountId") UUID accountId,
            @Param("status") TransactionStatus status,
            Pageable pageable
    );

    /**
     * Kullanıcıya ait tüm hesapların işlem geçmişini sayfalı döner.
     */
    @Query("""
            SELECT t FROM Transaction t
            WHERE t.fromAccount.owner.id = :userId OR t.toAccount.owner.id = :userId
            ORDER BY t.createdAt DESC
            """)
    Page<Transaction> findAllByUserId(@Param("userId") UUID userId, Pageable pageable);
}

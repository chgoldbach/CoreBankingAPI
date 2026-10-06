package com.banking.core.repository;

import com.banking.core.entity.Account;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Hesap veri erişim katmanı.
 *
 * findByIdWithPessimisticLock: Para transferi sırasında hesabı kilitlemek için
 * kullanılır. PESSIMISTIC_WRITE → veritabanı düzeyinde SELECT FOR UPDATE oluşturur.
 * Bu sayede aynı anda başka bir transaction aynı hesabı okuyup güncelleyemez.
 */
@Repository
public interface AccountRepository extends JpaRepository<Account, UUID> {

    /**
     * Hesabı veritabanı düzeyinde kilitleyerek getirir (SELECT FOR UPDATE).
     * Yalnızca aktif @Transactional bloğu içinde çağrılmalıdır.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Account a WHERE a.id = :id")
    Optional<Account> findByIdWithPessimisticLock(@Param("id") UUID id);

    /**
     * Kullanıcıya ait tüm aktif hesapları döner.
     */
    @Query("SELECT a FROM Account a WHERE a.owner.id = :ownerId AND a.active = true ORDER BY a.createdAt DESC")
    List<Account> findActiveByOwnerId(@Param("ownerId") UUID ownerId);

    /**
     * Hesap numarasına göre hesap arar.
     */
    Optional<Account> findByAccountNumber(String accountNumber);

    /**
     * Hesap numarasının mevcut olup olmadığını kontrol eder.
     */
    boolean existsByAccountNumber(String accountNumber);
}

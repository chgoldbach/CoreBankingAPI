package com.banking.core.repository;

import com.banking.core.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Kullanıcı veri erişim katmanı.
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * E-posta adresine göre kullanıcı arar.
     * Hem login hem de kayıt sırasında kullanılır.
     */
    Optional<User> findByEmail(String email);

    /**
     * E-posta adresinin sistemde kayıtlı olup olmadığını kontrol eder.
     * Kayıt işleminde duplicate kontrolü için.
     */
    boolean existsByEmail(String email);
}

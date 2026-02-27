// src/main/java/com/reservas/repository/UserRepository.java
package com.reservas.repository;

import com.reservas.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByRefreshToken(String refreshToken);
    Optional<User> findByPasswordResetToken(String passwordResetToken);
    boolean existsByEmail(String email);
}
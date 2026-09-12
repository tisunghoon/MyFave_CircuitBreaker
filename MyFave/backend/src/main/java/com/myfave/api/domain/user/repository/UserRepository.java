package com.myfave.api.domain.user.repository;

import com.myfave.api.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByNameAndPhone(String name, String phone);
    Optional<User> findByEmailAndPhone(String email, String phone);
    boolean existsByEmail(String email);
    boolean existsByNickname(String nickname);
    boolean existsByPhone(String phone);
    Optional<User> findBySocialProviderId(String socialProviderId);
}

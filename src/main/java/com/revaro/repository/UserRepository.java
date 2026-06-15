package com.revaro.repository;

import com.revaro.entity.User;
import com.revaro.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByUsernameIgnoreCase(String username);

    boolean existsByEmail(String email);

    List<User> findByRole(Role role);

    List<User> findByUsernameContainingIgnoreCase(String username);
}

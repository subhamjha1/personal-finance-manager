package com.syfe.pfm.repository;

import com.syfe.pfm.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistence access for {@link User}. */
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);
}

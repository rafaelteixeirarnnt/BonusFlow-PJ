package com.bonusflowpj.repository;

import com.bonusflowpj.domain.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    @Override
    @EntityGraph(attributePaths = "professional")
    java.util.List<User> findAll();

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByProfessionalId(Long professionalId);

    Optional<User> findByEmailIgnoreCase(String email);

    @EntityGraph(attributePaths = "professional")
    Optional<User> findWithProfessionalByEmailIgnoreCase(String email);

    @EntityGraph(attributePaths = "professional")
    Optional<User> findWithProfessionalById(Long id);
}

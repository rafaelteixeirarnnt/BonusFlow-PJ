package com.bonusflowpj.repository;

import com.bonusflowpj.domain.Professional;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProfessionalRepository extends JpaRepository<Professional, Long> {

    Optional<Professional> findByEmailIgnoreCase(String email);
}

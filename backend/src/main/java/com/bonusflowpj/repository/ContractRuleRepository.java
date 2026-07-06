package com.bonusflowpj.repository;

import com.bonusflowpj.domain.AbsenceType;
import com.bonusflowpj.domain.ContractRule;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContractRuleRepository extends JpaRepository<ContractRule, Long> {

    @Override
    @EntityGraph(attributePaths = "professional")
    List<ContractRule> findAll();

    Optional<ContractRule> findByProfessionalIdAndAbsenceType(Long professionalId, AbsenceType absenceType);
}

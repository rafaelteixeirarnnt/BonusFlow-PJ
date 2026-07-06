package com.bonusflowpj.repository;

import com.bonusflowpj.domain.AbsenceRequest;
import com.bonusflowpj.domain.AbsenceStatus;
import com.bonusflowpj.domain.AbsenceType;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AbsenceRequestRepository extends JpaRepository<AbsenceRequest, Long> {

    @Override
    @EntityGraph(attributePaths = {"professional", "createdBy"})
    List<AbsenceRequest> findAll();

    @EntityGraph(attributePaths = {"professional", "createdBy"})
    Optional<AbsenceRequest> findWithAssociationsById(Long id);

    @Query("""
        select coalesce(sum(a.requestedDays), 0)
        from AbsenceRequest a
        where a.professional.id = :professionalId
          and a.absenceType = :absenceType
          and a.status = :status
        """)
    int sumRequestedDaysByProfessionalTypeAndStatus(
        @Param("professionalId") Long professionalId,
        @Param("absenceType") AbsenceType absenceType,
        @Param("status") AbsenceStatus status
    );

    boolean existsByProfessionalIdAndStatusInAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
        Long professionalId,
        Collection<AbsenceStatus> statuses,
        LocalDate endDate,
        LocalDate startDate
    );

    @Query("""
        select a
        from AbsenceRequest a
        join fetch a.professional
        join fetch a.createdBy
        where (:professionalId is null or a.professional.id = :professionalId)
          and (:absenceType is null or a.absenceType = :absenceType)
          and a.startDate <= :monthEnd
          and a.endDate >= :monthStart
        order by a.startDate desc
        """)
    List<AbsenceRequest> report(
        @Param("monthStart") LocalDate monthStart,
        @Param("monthEnd") LocalDate monthEnd,
        @Param("professionalId") Long professionalId,
        @Param("absenceType") AbsenceType absenceType
    );
}

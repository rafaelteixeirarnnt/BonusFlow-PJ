package com.bonusflowpj.repository;

import com.bonusflowpj.domain.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;

public interface UserRepository extends JpaRepository<User, Long> {

    @Override
    @EntityGraph(attributePaths = {"professional"})
    java.util.List<User> findAll();

    @Query("select distinct u from User u left join fetch u.contacts left join fetch u.professional order by u.id asc")
    List<User> findAllForListing();

    @Query(value = "select u.id from User u order by u.id asc", countQuery = "select count(u) from User u")
    Page<Long> findIdsForListing(Pageable pageable);

    @Query("select distinct u from User u left join fetch u.contacts left join fetch u.professional where u.id in :ids")
    List<User> findAllForListingByIdIn(@Param("ids") List<Long> ids);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByProfessionalId(Long professionalId);

    boolean existsByProfessionalIdAndActiveTrue(Long professionalId);

    Optional<User> findByEmailIgnoreCase(String email);

    Optional<User> findByCpf(String cpf);

    @EntityGraph(attributePaths = "professional")
    Optional<User> findWithProfessionalByEmailIgnoreCase(String email);

    @EntityGraph(attributePaths = "professional")
    Optional<User> findWithProfessionalById(Long id);
}

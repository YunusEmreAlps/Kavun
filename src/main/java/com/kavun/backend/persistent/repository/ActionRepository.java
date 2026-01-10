package com.kavun.backend.persistent.repository;

import com.kavun.backend.persistent.domain.user.Action;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.stereotype.Repository;

@Repository
@RepositoryRestResource(exported = false)
public interface ActionRepository extends JpaRepository<Action, Long> {
    Optional<Action> findByCode(String code);

    boolean existsByCode(String code);
}

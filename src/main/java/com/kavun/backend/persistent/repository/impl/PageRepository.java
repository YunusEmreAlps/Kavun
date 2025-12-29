package com.kavun.backend.persistent.repository.impl;

import com.kavun.backend.persistent.domain.user.Page;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.stereotype.Repository;

@Repository
@RepositoryRestResource(exported = false)
public interface PageRepository extends JpaRepository<Page, Long> {
    Optional<Page> findByCode(String code);

    boolean existsByCode(String code);
}

package com.kavun.backend.persistent.repository.impl;

import com.kavun.backend.persistent.domain.user.PageAction;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.stereotype.Repository;

@Repository
@RepositoryRestResource(exported = false)
public interface PageActionRepository extends JpaRepository<PageAction, Long> {}

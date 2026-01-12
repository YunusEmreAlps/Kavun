package com.kavun.backend.persistent.repository;

import com.kavun.backend.persistent.domain.user.Action;

import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.stereotype.Repository;

@Repository
@RepositoryRestResource(exported = false)
public interface ActionRepository extends BaseRepository<Action> {}

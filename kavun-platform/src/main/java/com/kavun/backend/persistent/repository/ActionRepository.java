package com.kavun.backend.persistent.repository;

import com.kavun.backend.persistent.domain.user.Action;

import java.util.Optional;

import org.springframework.stereotype.Repository;

@Repository
public interface ActionRepository extends BaseRepository<Action> {

    // Includes soft-deleted actions, so a deleted action is not recreated.
    Optional<Action> findByCode(String code);
}

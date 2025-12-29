package com.kavun.backend.service.user;

import com.kavun.backend.persistent.domain.user.Action;

import jakarta.validation.Valid;

import java.util.List;
import java.util.Optional;

/**
* Action service to provide implementation for the definitions about an action.
*
* @author Yunus Emre Alpu
* @version 1.0
* @since 1.0
*/
public interface ActionService {
    Action save(@Valid Action action);
    Optional<Action> findById(Long id);
    List<Action> findAll();
    void deleteById(Long id);
    Optional<Action> findByCode(String code);
    boolean existsByCode(String code);
}

package com.kavun.backend.service.user;

import com.kavun.backend.persistent.domain.user.PageAction;

import jakarta.validation.Valid;

import java.util.List;
import java.util.Optional;

/**
* PageAction service to provide implementation for the definitions about a page action.
*
* @author Yunus Emre Alpu
* @version 1.0
* @since 1.0
*/
public interface PageActionService {

    PageAction save(@Valid PageAction pageAction);
    Optional<PageAction> findById(Long id);
    List<PageAction> findAll();
    void deleteById(Long id);
}

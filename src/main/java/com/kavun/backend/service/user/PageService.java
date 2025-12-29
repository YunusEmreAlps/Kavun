package com.kavun.backend.service.user;

import com.kavun.backend.persistent.domain.user.Page;

import jakarta.validation.Valid;

import java.util.List;
import java.util.Optional;

/**
* Page service to provide implementation for the definitions about a page.
*
* @author Yunus Emre Alpu
* @version 1.0
* @since 1.0
*/
public interface PageService {

    Page save(@Valid Page page);
    Optional<Page> findById(Long id);
    List<Page> findAll();
    void deleteById(Long id);
    Optional<Page> findByCode(String code);
    boolean existsByCode(String code);
}

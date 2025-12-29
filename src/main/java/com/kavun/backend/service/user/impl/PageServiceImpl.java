package com.kavun.backend.service.user.impl;

import com.kavun.backend.persistent.domain.user.Page;
import com.kavun.backend.persistent.repository.impl.PageRepository;
import com.kavun.backend.service.user.PageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PageServiceImpl implements PageService {
    private final PageRepository pageRepository;

    @Override
    @Transactional
    public Page save(Page page) {
        return pageRepository.save(page);
    }

    @Override
    public Optional<Page> findById(Long id) {
        return pageRepository.findById(id);
    }

    @Override
    public List<Page> findAll() {
        return pageRepository.findAll();
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        pageRepository.deleteById(id);
    }

    @Override
    public Optional<Page> findByCode(String code) {
        return pageRepository.findByCode(code);
    }

    @Override
    public boolean existsByCode(String code) {
        return pageRepository.existsByCode(code);
    }
}

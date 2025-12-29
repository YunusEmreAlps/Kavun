package com.kavun.backend.service.user.impl;

import com.kavun.backend.persistent.domain.user.PageAction;
import com.kavun.backend.persistent.repository.impl.PageActionRepository;
import com.kavun.backend.service.user.PageActionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PageActionServiceImpl implements PageActionService {
    private final PageActionRepository pageActionRepository;

    @Override
    @Transactional
    public PageAction save(PageAction pageAction) {
        return pageActionRepository.save(pageAction);
    }

    @Override
    public Optional<PageAction> findById(Long id) {
        return pageActionRepository.findById(id);
    }

    @Override
    public List<PageAction> findAll() {
        return pageActionRepository.findAll();
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        pageActionRepository.deleteById(id);
    }
}

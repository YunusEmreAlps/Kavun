package com.kavun.backend.service.user.impl;

import com.kavun.backend.persistent.domain.user.Action;
import com.kavun.backend.persistent.repository.ActionRepository;
import com.kavun.backend.service.user.ActionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ActionServiceImpl implements ActionService {
    private final ActionRepository actionRepository;

    @Override
    @Transactional
    public Action save(Action action) {
        return actionRepository.save(action);
    }

    @Override
    public Optional<Action> findById(Long id) {
        return actionRepository.findById(id);
    }

    @Override
    public List<Action> findAll() {
        return actionRepository.findAll();
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        actionRepository.deleteById(id);
    }

    @Override
    public Optional<Action> findByCode(String code) {
        return actionRepository.findByCode(code);
    }

    @Override
    public boolean existsByCode(String code) {
        return actionRepository.existsByCode(code);
    }
}

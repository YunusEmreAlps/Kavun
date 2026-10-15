package com.kavun.backend.service.user;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.kavun.backend.persistent.domain.user.Action;
import com.kavun.backend.persistent.repository.ActionRepository;
import com.kavun.backend.service.AbstractService;
import com.kavun.shared.dto.ActionDto;
import com.kavun.shared.dto.mapper.ActionMapper;
import com.kavun.shared.request.ActionRequest;


import java.util.Set;

/**
 * Action service to provide implementation for the definitions about an action.
 *
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 1.0
 */
@Service
@Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
public class ActionService
        extends AbstractService<ActionRequest, Action, ActionDto, ActionRepository, ActionMapper> {

    public ActionService(ActionMapper mapper, ActionRepository repository) {
        super(mapper, repository);
    }

    @Override
    protected Set<String> filterableFields() {
        return Set.of("code", "name", "type", "createdAt");
    }
}

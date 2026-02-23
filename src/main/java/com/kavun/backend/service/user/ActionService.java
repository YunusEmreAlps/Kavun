package com.kavun.backend.service.user;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.kavun.backend.persistent.domain.user.Action;
import com.kavun.backend.persistent.repository.ActionRepository;
import com.kavun.backend.persistent.specification.ActionSpecification;
import com.kavun.backend.service.AbstractService;
import com.kavun.shared.dto.ActionDto;
import com.kavun.shared.dto.mapper.ActionMapper;
import com.kavun.shared.request.ActionRequest;

import org.springframework.data.jpa.domain.Specification;

import java.util.Map;

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
        extends AbstractService<ActionRequest, Action, ActionDto, ActionRepository, ActionMapper, ActionSpecification> {

    public ActionService(ActionMapper mapper, ActionRepository repository, ActionSpecification specification) {
        super(mapper, repository, specification);
    }

    public Specification<Action> search(Map<String, Object> paramaterMap) {
        return specification.search(paramaterMap);
    }

}

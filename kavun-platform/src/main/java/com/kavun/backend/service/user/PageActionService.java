package com.kavun.backend.service.user;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.kavun.backend.persistent.domain.user.PageAction;
import com.kavun.backend.persistent.repository.PageActionRepository;
import com.kavun.backend.service.AbstractService;
import com.kavun.shared.dto.PageActionDto;
import com.kavun.shared.dto.mapper.PageActionMapper;
import com.kavun.shared.request.PageActionRequest;


import java.util.Set;

/**
* PageAction service to provide implementation for the definitions about a page action.
*
* @author Yunus Emre Alpu
* @version 1.0
* @since 1.0
*/
@Service
@Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
public class PageActionService
        extends AbstractService<PageActionRequest, PageAction, PageActionDto, PageActionRepository, PageActionMapper> {

    public PageActionService(PageActionMapper mapper, PageActionRepository repository) {
        super(mapper, repository);
    }

    @Override
    protected Set<String> filterableFields() {
        return Set.of("page", "action", "apiEndpoint", "httpMethod", "label", "createdAt");
    }
}

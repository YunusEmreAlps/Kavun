package com.kavun.backend.service.user;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.kavun.backend.persistent.domain.user.WebPage;
import com.kavun.backend.persistent.repository.PageRepository;
import com.kavun.backend.service.AbstractService;
import com.kavun.shared.dto.PageDto;
import com.kavun.shared.dto.mapper.PageMapper;
import com.kavun.shared.request.PageRequest;


import java.util.Set;
/**
* Page service to provide implementation for the definitions about a page.
*
* @author Yunus Emre Alpu
* @version 1.0
* @since 1.0
*/
@Service
@Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
public class PageService
        extends AbstractService<PageRequest, WebPage, PageDto, PageRepository, PageMapper> {

    public PageService(PageMapper mapper, PageRepository repository) {
        super(mapper, repository);
    }

    @Override
    protected Set<String> filterableFields() {
        return Set.of(
            "code", "name", "url", "icon", "description", "parent", "navigable",
            "showInMenu", "createdAt");
    }
}

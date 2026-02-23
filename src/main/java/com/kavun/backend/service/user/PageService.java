package com.kavun.backend.service.user;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.kavun.backend.persistent.domain.user.WebPage;
import com.kavun.backend.persistent.repository.PageRepository;
import com.kavun.backend.persistent.specification.PageSpecification;
import com.kavun.backend.service.AbstractService;
import com.kavun.shared.dto.PageDto;
import com.kavun.shared.dto.mapper.PageMapper;
import com.kavun.shared.request.PageRequest;

import org.springframework.data.jpa.domain.Specification;

import java.util.Map;
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
        extends AbstractService<PageRequest, WebPage, PageDto, PageRepository, PageMapper, PageSpecification> {

    public PageService(PageMapper mapper, PageRepository repository, PageSpecification specification) {
        super(mapper, repository, specification);
    }

    public Specification<WebPage> search(Map<String, Object> paramaterMap) {
        return specification.search(paramaterMap);
    }

}

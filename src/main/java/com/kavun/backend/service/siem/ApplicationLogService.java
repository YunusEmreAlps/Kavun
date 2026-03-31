package com.kavun.backend.service.siem;

import java.util.Map;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.kavun.backend.persistent.domain.siem.ApplicationLog;
import com.kavun.backend.persistent.repository.ApplicationLogRepository;
import com.kavun.backend.persistent.specification.ApplicationLogSpecification;
import com.kavun.backend.service.AbstractService;
import com.kavun.shared.dto.ApplicationLogDto;
import com.kavun.shared.dto.mapper.ApplicationLogMapper;
import com.kavun.shared.request.ApplicationLogRequest;

/**
 * Application log service to provide implementation for the definitions about
 * an application log.
 *
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 1.0
 */
@Service
@Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
public class ApplicationLogService extends
        AbstractService<ApplicationLogRequest, ApplicationLog, ApplicationLogDto, ApplicationLogRepository, ApplicationLogMapper, ApplicationLogSpecification> {

    public ApplicationLogService(ApplicationLogMapper mapper, ApplicationLogRepository repository,
            ApplicationLogSpecification specification) {
        super(mapper, repository, specification);
    }

    public Specification<ApplicationLog> search(Map<String, Object> parameterMap) {
        return specification.search(parameterMap);
    }
}

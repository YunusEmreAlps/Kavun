package com.kavun.backend.persistent.repository;

import com.kavun.backend.persistent.domain.siem.ApplicationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(exported = false)
public interface ApplicationLogRepository extends JpaRepository<ApplicationLog, Long> {}

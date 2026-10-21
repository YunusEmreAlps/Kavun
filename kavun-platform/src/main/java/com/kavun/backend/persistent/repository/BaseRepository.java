package com.kavun.backend.persistent.repository;

import com.kavun.backend.persistent.domain.base.BaseEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface BaseRepository<T extends BaseEntity<Long>>
        extends JpaRepository<T, Long>, JpaSpecificationExecutor<T> {
}

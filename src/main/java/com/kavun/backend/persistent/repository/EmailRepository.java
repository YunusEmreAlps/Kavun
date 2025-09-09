package com.kavun.backend.persistent.repository;

import com.kavun.backend.persistent.domain.email.Email;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.NonNull;

import org.springframework.data.jpa.datatables.repository.DataTablesRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;
import org.springframework.stereotype.Repository;

@Repository
@RepositoryRestResource(exported = false)
public interface EmailRepository extends DataTablesRepository<Email, Long> {

    @Override
    @RestResource(exported = false)
    Optional<Email> findById(@NonNull Long id);

    Email findByPublicId(String publicId);

    @Query(nativeQuery = true, value = "select status, count(*) count from email group by status")
    List<Map<Boolean, Object>> statusGroupCount();
}

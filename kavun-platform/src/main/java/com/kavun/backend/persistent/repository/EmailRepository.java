package com.kavun.backend.persistent.repository;

import com.kavun.backend.persistent.domain.email.Email;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.NonNull;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface EmailRepository extends JpaRepository<Email, Long> {

    @Override
    Optional<Email> findById(@NonNull Long id);

    Email findByPublicId(String publicId);

    @Query(nativeQuery = true, value = "select status, count(*) count from email group by status")
    List<Map<Boolean, Object>> statusGroupCount();
}

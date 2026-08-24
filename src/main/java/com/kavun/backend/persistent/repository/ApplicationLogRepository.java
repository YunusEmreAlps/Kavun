package com.kavun.backend.persistent.repository;

import com.kavun.backend.persistent.domain.siem.ApplicationLog;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.transaction.annotation.Transactional;

@RepositoryRestResource(exported = false)
public interface ApplicationLogRepository extends BaseRepository<ApplicationLog> {

  // Retention cleanup - deletes SIEM log rows older than the configured cutoff (returns
  // count for logging). See ApplicationLogCleanupScheduler.
  @Modifying
  @Transactional
  @Query("DELETE FROM ApplicationLog l WHERE l.createdAt < :threshold")
  int deleteOlderThan(LocalDateTime threshold);
}

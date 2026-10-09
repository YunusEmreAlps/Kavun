package com.kavun.backend.persistent.repository;

import com.kavun.backend.persistent.domain.user.WebPage;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.stereotype.Repository;

/**
 * The PageRepository class exposes implementation from JpaRepository on Page
 * entity .
 *
 * @author Yunus Emre Alpu
 * @version 2.0
 * @since 1.0
 */
@Repository
@RepositoryRestResource(exported = false)
public interface PageRepository extends BaseRepository<WebPage> {
    WebPage findByCodeAndDeletedFalse(String code);

    WebPage findByUrlAndDeletedFalse(String url);

    @Query("SELECT p FROM WebPage p LEFT JOIN FETCH p.parent WHERE p.deleted = false ORDER BY p.orderIndex, p.id")
    List<WebPage> findAllActiveWithParent();
}

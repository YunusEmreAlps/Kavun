package com.kavun.backend.persistent.repository;

import com.kavun.backend.persistent.domain.user.WebPage;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
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
public interface PageRepository extends BaseRepository<WebPage> {
    WebPage findByCodeAndDeletedFalse(String code);

    // Includes soft-deleted pages, so a deleted page is not recreated.
    Optional<WebPage> findByCode(String code);

    WebPage findByUrlAndDeletedFalse(String url);

    @Query("SELECT p FROM WebPage p LEFT JOIN FETCH p.parent WHERE p.deleted = false ORDER BY p.orderIndex, p.id")
    List<WebPage> findAllActiveWithParent();
}

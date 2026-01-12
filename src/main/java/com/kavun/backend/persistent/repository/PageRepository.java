package com.kavun.backend.persistent.repository;

import com.kavun.backend.persistent.domain.user.WebPage;

import java.util.List;
import java.util.Optional;

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
    WebPage getReferenceById(Long id);

    Optional<WebPage> findByCode(String code);

    List<WebPage> findByParentIsNullAndDeletedFalseOrderByDisplayOrder();
    List<WebPage> findByParentIdAndDeletedFalseOrderByDisplayOrder(Long parentId);

    @Query("SELECT p FROM WebPage p LEFT JOIN FETCH p.pageActions WHERE p.id = :id AND p.deleted = false")
    Optional<WebPage> findByIdWithPageActions(Long id);
}

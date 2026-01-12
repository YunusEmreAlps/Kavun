package com.kavun.backend.persistent.repository;

import com.kavun.backend.persistent.domain.user.PageAction;
import com.kavun.enums.HttpMethod;

import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/**
 * The PageActionRepository class exposes implementation from JpaRepository on
 * PageAction
 * entity .
 *
 * @author Yunus Emre Alpu
 * @version 2.0
 * @since 1.0
 */
@Repository
@RepositoryRestResource(exported = false)
public interface PageActionRepository extends BaseRepository<PageAction> {

    List<PageAction> findByPageIdAndDeletedFalse(Long pageId);

    @Query("SELECT pa FROM PageAction pa JOIN FETCH pa.page JOIN FETCH pa.action WHERE pa.page.id = :pageId AND pa.deleted = false")
    List<PageAction> findByPageIdWithDetails(Long pageId);

    Optional<PageAction> findByPageIdAndActionIdAndDeletedFalse(Long pageId, Long actionId);

    Optional<PageAction> findByApiEndpointAndHttpMethodAndDeletedFalse(String apiEndpoint, HttpMethod httpMethod);

    @Query("SELECT pa FROM PageAction pa JOIN FETCH pa.action a JOIN FETCH pa.page p " +
            "WHERE p.url = :pageUrl AND a.code = :actionCode AND pa.deleted = false AND p.deleted = false")
    Optional<PageAction> findActionByPageUrlAndActionCode(String pageUrl, String actionCode);
}

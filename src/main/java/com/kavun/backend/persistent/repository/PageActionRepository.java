package com.kavun.backend.persistent.repository;

import com.kavun.backend.persistent.domain.user.PageAction;

import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;

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
public interface PageActionRepository extends BaseRepository<PageAction> {

    @Query("SELECT pa FROM PageAction pa JOIN FETCH pa.page JOIN FETCH pa.action WHERE pa.page.id = :pageId AND pa.deleted = false " +
           "ORDER BY pa.orderIndex, pa.id")
    List<PageAction> findByPageIdWithDetails(Long pageId);

    @Query("SELECT pa FROM PageAction pa JOIN FETCH pa.page p JOIN FETCH pa.action " +
           "WHERE pa.deleted = false AND p.deleted = false ORDER BY pa.orderIndex, pa.id")
    List<PageAction> findAllActiveWithDetails();

    @Query("SELECT pa FROM PageAction pa JOIN FETCH pa.action a JOIN FETCH pa.page p " +
           "WHERE p.code = :pageCode AND a.code = :actionCode AND pa.deleted = false AND p.deleted = false")
    Optional<PageAction> findActionByPageCodeAndActionCode(String pageCode, String actionCode);

    // Includes soft-deleted page actions, so a deleted page action is not recreated.
    Optional<PageAction> findByPageIdAndActionId(Long pageId, Long actionId);
}

package com.kavun.backend.persistent.repository;

import com.kavun.backend.persistent.domain.user.UserDevice;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for the User.
 *
 * @author Yunus Emre Alpu
 * @version 2.0
 * @since 1.0
 */
@Repository
@RepositoryRestResource(exported = false)
public interface UserDeviceRepository extends BaseRepository<UserDevice> {
    /**
     * Finds a user device by its device ID.
     *
     * @param deviceId the unique identifier of the device
     * @return an Optional containing the UserDevice if found, or empty if not found
     */
    Optional<UserDevice> findByDeviceId(String deviceId);
}

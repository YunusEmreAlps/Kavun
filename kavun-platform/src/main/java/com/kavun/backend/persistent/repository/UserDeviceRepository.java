package com.kavun.backend.persistent.repository;

import com.kavun.backend.persistent.domain.user.UserDevice;

import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for the User.
 *
 * @author Yunus Emre Alpu
 * @version 2.0
 * @since 1.0
 */
@Repository
public interface UserDeviceRepository extends BaseRepository<UserDevice> {
    /**
     * Finds a user's record of a device. A browser used by several users has one record per user.
     *
     * @param userId the user
     * @param deviceId the identifier the client sends for the device
     * @return the device record, if the user has signed in from it before
     */
    Optional<UserDevice> findByUserIdAndDeviceId(Long userId, String deviceId);

    // Device usage rate analytics for all user devices.
    @Query(value = """
            SELECT device_type, COUNT(*) AS count
            FROM user_devices
            WHERE deleted IS NULL OR deleted = false
            GROUP BY device_type
            """, nativeQuery = true)
    List<Object[]> countDeviceTypes();
}

package com.kavun.backend.persistent.repository;

import com.kavun.backend.persistent.domain.user.User;
import com.kavun.shared.dto.UserInfoDto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.NonNull;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.EntityGraph.EntityGraphType;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;
import org.springframework.stereotype.Repository;

/**
 * Repository for the User.
 *
 * @author Yunus Emre Alpu
 * @version 2.0
 * @since 1.0
 */
@Repository
@RepositoryRestResource(exported = false)
public interface UserRepository extends BaseRepository<User> {

    @NonNull
    @Override
    @RestResource(exported = false)
    @EntityGraph(type = EntityGraphType.FETCH, attributePaths = { "userRoles", "userRoles.role" })
    Optional<User> findById(@NonNull Long id);

    /**
     * Find user by email (excludes deleted users).
     *
     * @param email email used to search for user.
     * @return User found.
     */
    @EntityGraph(type = EntityGraphType.FETCH, attributePaths = { "userRoles", "userRoles.role" })
    User findByEmailAndDeletedFalse(String email);

    /**
     * Find user by email (includes deleted users - for admin purposes only).
     *
     * @param email email used to search for user.
     * @return User found.
     */
    @EntityGraph(type = EntityGraphType.FETCH, attributePaths = { "userRoles", "userRoles.role" })
    User findByEmail(String email);

    // Find user by phone (excludes deleted users).
    @EntityGraph(type = EntityGraphType.FETCH, attributePaths = { "userRoles", "userRoles.role" })
    User findByPhoneAndDeletedFalse(String phone);

    /**
     * Find user by username (excludes deleted users).
     *
     * @param username username used to search for user.
     * @return User found.
     */
    @EntityGraph(type = EntityGraphType.FETCH, attributePaths = { "userRoles", "userRoles.role" })
    User findByUsernameAndDeletedFalse(String username);

    /**
     * Find user by username (includes deleted users - for admin purposes only).
     *
     * @param username username used to search for user.
     * @return User found.
     */
    @EntityGraph(type = EntityGraphType.FETCH, attributePaths = { "userRoles", "userRoles.role" })
    User findByUsername(String username);

    /**
     * Check if user exists by username.
     *
     * @param username username to check if user exists.
     * @return True if user exists or false otherwise.
     */
    Boolean existsByUsernameOrderById(String username);

    /**
     * Check if user exists by username or email.
     *
     * @param username username to check if user exists.
     * @param email    email to check if user exists.
     * @return True if user exists or false otherwise.
     */
    @RestResource(exported = false)
    Boolean existsByUsernameAndEnabledTrueOrEmailAndEnabledTrueOrderById(
            String username, String email);

    // Check if username exists for a different user (for update validations).
    Boolean existsByUsernameAndIdNotAndDeletedFalse(String username, Long id);

    // Check if email exists for a different user (for update validations).
    Boolean existsByEmailAndIdNotAndDeletedFalse(String email, Long id);

    /**
     * Check if user exists by username and verificationToken.
     *
     * @param username          the username
     * @param verificationToken the verification token
     * @return if user exists with the given verification token
     */
    Boolean existsByUsernameAndVerificationTokenOrderById(String username, String verificationToken);

    Boolean existsByUsernameAndFailedLoginAttemptsGreaterThanOrderById(String username, int attempts);

    /**\n     * Find all users that failed to verify their email after a certain time.
     *
     * @param allowedDaysToVerify email verification allowed days.
     * @return List of users that failed to verify their email.
     */
    @RestResource(exported = false)
    List<User> findByEnabledFalseAndCreatedAtBefore(LocalDateTime allowedDaysToVerify);

    /**
     * Find user by verification token.
     *
     * @param verificationToken the verification token
     * @return the user
     */
    User findByVerificationToken(String verificationToken);

    /**
     * Get user information by UUID for display purposes
     */
    @Query("""
        SELECT new com.kavun.shared.dto.UserInfoDto(
            u.id,
            u.firstName,
            u.lastName
        )
        FROM User u
        WHERE u.id = :userId AND u.deleted = false
        """)
    Optional<UserInfoDto> findUserInfoById(@Param("userId") Long userId);

    /**
     * Get multiple user information by UUIDs for display purposes
     */
    @Query("""
        SELECT new com.kavun.shared.dto.UserInfoDto(
            u.id,
            u.firstName,
            u.lastName
        )
        FROM User u
        WHERE u.id IN :userIds AND u.deleted = false
        """)
    List<UserInfoDto> findUserInfoByIds(@Param("userIds") List<Long> userIds);
}

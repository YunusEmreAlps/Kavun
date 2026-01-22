package com.kavun.shared.request;

import com.kavun.enums.RoleType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.Set;

/**
 * User request DTO for creating and updating users.
 *
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 1.0
 */
@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class UserRequest extends BaseRequest {

    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String password;
    private String phoneNumber;
    private Boolean enabled;
    private Set<RoleType> roleTypes;
}

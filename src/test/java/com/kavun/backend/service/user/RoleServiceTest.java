package com.kavun.backend.service.user;

import com.kavun.backend.persistent.domain.user.Role;
import com.kavun.backend.persistent.domain.user.User;
import com.kavun.backend.persistent.repository.RoleRepository;
import com.kavun.backend.persistent.repository.UserRepository;
import com.kavun.backend.persistent.repository.UserRoleRepository;
import com.kavun.backend.persistent.specification.RoleSpecification;
import com.kavun.enums.RoleType;
import com.kavun.shared.dto.mapper.RoleMapper;
import com.kavun.shared.request.RoleRequest;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RoleServiceTest {

  @Mock private transient RoleRepository roleRepository;

  @Mock private transient UserRepository userRepository;

  @Mock private transient UserRoleRepository userRoleRepository;

  private transient RoleService roleService;

  private transient Role roleEntity;

  @BeforeEach
  void setUp() {
    roleService =
        new RoleService(
            Mappers.getMapper(RoleMapper.class),
            roleRepository,
            new RoleSpecification(),
            userRepository,
            userRoleRepository);
    roleEntity = new Role(RoleType.ROLE_USER);
    roleEntity.setId(1L);
  }

  @Test
  void createRole() {
    Mockito.when(roleRepository.save(ArgumentMatchers.any(Role.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    var request = RoleRequest.builder().name("ROLE_MANAGER").label("Manager").build();
    var created = roleService.create(request);

    Assertions.assertEquals("ROLE_MANAGER", created.getName());
  }

  @Test
  void getRoleByName() {
    Mockito.when(roleRepository.findByName(roleEntity.getName()))
        .thenReturn(Optional.of(roleEntity));

    Assertions.assertEquals(roleEntity, roleService.findByName(roleEntity.getName()));
  }

  @Test
  void getRoleByNameReturnsNullWhenMissing() {
    Mockito.when(roleRepository.findByName("ROLE_MISSING")).thenReturn(Optional.empty());

    Assertions.assertNull(roleService.findByName("ROLE_MISSING"));
  }

  @Test
  void deleteMarksRoleAsDeleted() {
    Mockito.when(roleRepository.findById(1L)).thenReturn(Optional.of(roleEntity));

    roleService.delete(1L);

    Assertions.assertTrue(roleEntity.isDeleted());
    Assertions.assertNotNull(roleEntity.getDeletedAt());
  }

  @Test
  void deleteAlreadyDeletedRoleThrows() {
    roleEntity.setDeleted(true);
    Mockito.when(roleRepository.findById(1L)).thenReturn(Optional.of(roleEntity));

    Assertions.assertThrows(IllegalStateException.class, () -> roleService.delete(1L));
  }

  @Test
  void assignRoleUserAlreadyHasThrows() {
    var user = new User();
    user.setId(7L);
    user.addUserRole(roleEntity);
    Mockito.when(roleRepository.findById(1L)).thenReturn(Optional.of(roleEntity));
    Mockito.when(userRepository.findById(7L)).thenReturn(Optional.of(user));

    Assertions.assertThrows(
        IllegalArgumentException.class, () -> roleService.assignRoleToUser(1L, 7L));
  }
}

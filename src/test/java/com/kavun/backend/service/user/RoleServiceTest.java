package com.kavun.backend.service.user;

import com.kavun.backend.persistent.domain.user.Role;
import com.kavun.backend.persistent.repository.RoleRepository;
import com.kavun.backend.service.user.impl.RoleServiceImpl;
import com.kavun.enums.RoleType;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RoleServiceTest {

  @InjectMocks private transient RoleServiceImpl roleService;

  @Mock private transient RoleRepository roleEntityRepository;

  private transient Role roleEntity;

  @BeforeEach
  void setUp() {
    roleEntity = new Role(RoleType.ROLE_USER);
  }

  @Test
  void saveRole() {
    Mockito.when(roleEntityRepository.save(roleEntity)).thenReturn(roleEntity);

    Role storedRoleDetails = roleService.save(this.roleEntity);
    Assertions.assertNotNull(storedRoleDetails);
  }

  @Test
  void getRoleByName() {
    Mockito.when(roleEntityRepository.findFirstByName(roleEntity.getName()))
        .thenReturn(Optional.of(roleEntity));

    Role storedRoleDetails = roleService.findByName(roleEntity.getName());
    Assertions.assertEquals(roleEntity, storedRoleDetails);
  }
}

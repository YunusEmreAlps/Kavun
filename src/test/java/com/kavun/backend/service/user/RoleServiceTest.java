package com.kavun.backend.service.user;

import com.kavun.backend.persistent.domain.user.Role;
import com.kavun.backend.persistent.repository.RoleRepository;
import com.kavun.backend.persistent.repository.UserRepository;
import com.kavun.backend.persistent.repository.UserRoleRepository;
import com.kavun.backend.persistent.specification.RoleSpecification;
import com.kavun.enums.RoleType;
import com.kavun.shared.dto.RoleDto;
import com.kavun.shared.dto.mapper.RoleMapper;
import com.kavun.shared.request.RoleRequest;
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

  @InjectMocks private transient RoleService roleService;

  @Mock private transient RoleMapper roleMapper;

  @Mock private transient RoleRepository roleEntityRepository;

  @Mock private transient RoleSpecification roleSpecification;

  @Mock private transient UserRepository userRepository;

  @Mock private transient UserRoleRepository userRoleRepository;

  private transient Role roleEntity;

  @BeforeEach
  void setUp() {
    roleEntity = new Role(RoleType.ROLE_USER);
  }

  @Test
  void createRole() {
    var request = RoleRequest.builder().name(roleEntity.getName()).build();
    var dto = RoleDto.builder().name(roleEntity.getName()).build();

    Mockito.when(roleMapper.toEntity(request)).thenReturn(roleEntity);
    Mockito.when(roleEntityRepository.save(roleEntity)).thenReturn(roleEntity);
    Mockito.when(roleMapper.toDto(roleEntity)).thenReturn(dto);

    RoleDto storedRoleDetails = roleService.create(request);
    Assertions.assertEquals(roleEntity.getName(), storedRoleDetails.getName());
  }

  @Test
  void getRoleByName() {
    Mockito.when(roleEntityRepository.findByName(roleEntity.getName()))
        .thenReturn(Optional.of(roleEntity));

    Role storedRoleDetails = roleService.findByName(roleEntity.getName());
    Assertions.assertEquals(roleEntity, storedRoleDetails);
  }
}

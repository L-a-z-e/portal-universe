package com.portal.universe.authservice.auth.service;

import com.portal.universe.authservice.auth.domain.RoleEntity;
import com.portal.universe.authservice.auth.domain.UserRole;
import com.portal.universe.authservice.auth.repository.RoleEntityRepository;
import com.portal.universe.authservice.auth.repository.UserRoleRepository;
import com.portal.universe.event.auth.RoleAssignedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RbacInitializationService 테스트")
class RbacInitializationServiceTest {

    @Mock
    private RoleEntityRepository roleEntityRepository;

    @Mock
    private UserRoleRepository userRoleRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private RbacInitializationService rbacInitializationService;

    private static final String USER_ID = "test-uuid";

    @Nested
    @DisplayName("initializeNewUser")
    class InitializeNewUser {

        @Test
        @DisplayName("should_assignRoleAndPublishEvent_when_newUser")
        void should_assignRoleAndPublishEvent_when_newUser() {
            // given
            when(userRoleRepository.findByUserId(USER_ID)).thenReturn(Collections.emptyList());

            RoleEntity userRole = RoleEntity.builder()
                    .roleKey("ROLE_USER")
                    .displayName("User")
                    .system(true)
                    .build();
            when(roleEntityRepository.findByRoleKey("ROLE_USER")).thenReturn(Optional.of(userRole));
            when(userRoleRepository.save(any(UserRole.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // when
            rbacInitializationService.initializeNewUser(USER_ID);

            // then
            verify(userRoleRepository).save(any(UserRole.class));
            verify(eventPublisher).publishEvent(any(RoleAssignedEvent.class));
        }

        @Test
        @DisplayName("should_skip_when_alreadyInitialized")
        void should_skip_when_alreadyInitialized() {
            // given
            UserRole existingRole = UserRole.builder()
                    .userId(USER_ID)
                    .role(RoleEntity.builder().roleKey("ROLE_USER").displayName("User").system(true).build())
                    .assignedBy("SYSTEM")
                    .build();
            when(userRoleRepository.findByUserId(USER_ID)).thenReturn(List.of(existingRole));

            // when
            rbacInitializationService.initializeNewUser(USER_ID);

            // then
            verify(userRoleRepository, never()).save(any(UserRole.class));
            verify(eventPublisher, never()).publishEvent(any());
        }

        @Test
        @DisplayName("should_throwException_when_roleNotFound")
        void should_throwException_when_roleNotFound() {
            // given
            when(userRoleRepository.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(roleEntityRepository.findByRoleKey("ROLE_USER")).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> rbacInitializationService.initializeNewUser(USER_ID))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("ROLE_USER not found");
        }
    }
}

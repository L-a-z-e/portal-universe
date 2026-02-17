package com.portal.universe.authservice.auth.service;

import com.portal.universe.authservice.auth.domain.*;
import com.portal.universe.authservice.auth.dto.rbac.AssignRoleRequest;
import com.portal.universe.authservice.auth.dto.rbac.RoleResponse;
import com.portal.universe.authservice.auth.dto.rbac.UserPermissionsResponse;
import com.portal.universe.authservice.auth.dto.rbac.UserRoleResponse;
import com.portal.universe.authservice.auth.repository.*;
import java.util.Collections;
import com.portal.universe.authservice.common.exception.AuthErrorCode;
import com.portal.universe.commonlibrary.exception.CustomBusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RbacService 테스트")
class RbacServiceTest {

    @Mock
    private RoleEntityRepository roleEntityRepository;

    @Mock
    private UserRoleRepository userRoleRepository;

    @Mock
    private RolePermissionRepository rolePermissionRepository;

    @Mock
    private MembershipTierPermissionRepository membershipTierPermissionRepository;

    @Mock
    private UserMembershipRepository userMembershipRepository;

    @Mock
    private AuthAuditLogRepository auditLogRepository;

    @Mock
    private RoleIncludeRepository roleIncludeRepository;

    @Mock
    private RoleHierarchyService roleHierarchyService;

    @InjectMocks
    private RbacService rbacService;

    private static final String USER_ID = "test-uuid";
    private static final String ADMIN_ID = "admin-uuid";

    private RoleEntity createRole(String roleKey, boolean system) {
        return RoleEntity.builder()
                .roleKey(roleKey)
                .displayName(roleKey)
                .system(system)
                .build();
    }

    private UserRole createUserRole(String userId, RoleEntity role) {
        return UserRole.builder()
                .userId(userId)
                .role(role)
                .assignedBy("SYSTEM")
                .build();
    }

    @Nested
    @DisplayName("getAllActiveRoles")
    class GetAllActiveRoles {

        @Test
        @DisplayName("should_returnActiveRoles_when_rolesExist")
        void should_returnActiveRoles_when_rolesExist() {
            // given
            RoleEntity role1 = createRole("ROLE_USER", true);
            RoleEntity role2 = createRole("ROLE_ADMIN", false);
            when(roleEntityRepository.findByActiveTrue()).thenReturn(List.of(role1, role2));
            when(roleIncludeRepository.findAllWithRoles()).thenReturn(Collections.emptyList());

            // when
            List<RoleResponse> result = rbacService.getAllActiveRoles();

            // then
            assertThat(result).hasSize(2);
        }
    }

    @Nested
    @DisplayName("getUserRoles")
    class GetUserRoles {

        @Test
        @DisplayName("should_returnNonExpiredRoles_when_userHasRoles")
        void should_returnNonExpiredRoles_when_userHasRoles() {
            // given
            RoleEntity role = createRole("ROLE_USER", true);
            UserRole userRole = createUserRole(USER_ID, role);
            when(userRoleRepository.findByUserIdWithRole(USER_ID)).thenReturn(List.of(userRole));

            // when
            List<UserRoleResponse> result = rbacService.getUserRoles(USER_ID);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).roleKey()).isEqualTo("ROLE_USER");
        }
    }

    @Nested
    @DisplayName("resolveUserPermissions")
    class ResolveUserPermissions {

        @Test
        @DisplayName("should_combineRoleAndMembershipPermissions_when_bothExist")
        void should_combineRoleAndMembershipPermissions_when_bothExist() {
            // given
            when(userRoleRepository.findActiveRoleKeysByUserId(USER_ID))
                    .thenReturn(List.of("ROLE_USER"));
            when(rolePermissionRepository.findPermissionKeysByRoleKeys(List.of("ROLE_USER")))
                    .thenReturn(List.of("blog:read", "blog:write"));

            MembershipTier tier = MembershipTier.builder()
                    .membershipGroup("user:shopping")
                    .tierKey("PREMIUM")
                    .displayName("Premium")
                    .sortOrder(1)
                    .build();
            UserMembership membership = UserMembership.builder()
                    .userId(USER_ID)
                    .membershipGroup("user:shopping")
                    .tier(tier)
                    .build();
            when(userMembershipRepository.findActiveByUserId(USER_ID))
                    .thenReturn(List.of(membership));
            when(membershipTierPermissionRepository.findPermissionKeysByGroupAndTier("user:shopping", "PREMIUM"))
                    .thenReturn(List.of("user:shopping:premium_access"));

            // when
            UserPermissionsResponse result = rbacService.resolveUserPermissions(USER_ID);

            // then
            assertThat(result.userId()).isEqualTo(USER_ID);
            assertThat(result.roles()).containsExactly("ROLE_USER");
            assertThat(result.permissions()).contains("blog:read", "blog:write", "user:shopping:premium_access");
            assertThat(result.memberships()).containsEntry("user:shopping", "PREMIUM");
        }

        @Test
        @DisplayName("should_returnEmptyPermissions_when_noRolesAndNoMemberships")
        void should_returnEmptyPermissions_when_noRolesAndNoMemberships() {
            // given
            when(userRoleRepository.findActiveRoleKeysByUserId(USER_ID))
                    .thenReturn(List.of());
            when(userMembershipRepository.findActiveByUserId(USER_ID))
                    .thenReturn(List.of());

            // when
            UserPermissionsResponse result = rbacService.resolveUserPermissions(USER_ID);

            // then
            assertThat(result.permissions()).isEmpty();
            assertThat(result.memberships()).isEmpty();
        }
    }

    @Nested
    @DisplayName("assignRole")
    class AssignRole {

        @Test
        @DisplayName("should_assignRole_when_roleExistsAndNotAlreadyAssigned")
        void should_assignRole_when_roleExistsAndNotAlreadyAssigned() {
            // given
            RoleEntity role = createRole("ROLE_SELLER", false);
            AssignRoleRequest request = new AssignRoleRequest(USER_ID, "ROLE_SELLER", null);

            when(roleEntityRepository.findByRoleKey("ROLE_SELLER")).thenReturn(Optional.of(role));
            when(userRoleRepository.findByUserIdWithRole(USER_ID)).thenReturn(List.of());
            when(userRoleRepository.save(any(UserRole.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // when
            UserRoleResponse result = rbacService.assignRole(request, ADMIN_ID);

            // then
            assertThat(result.roleKey()).isEqualTo("ROLE_SELLER");
            verify(auditLogRepository).save(any(AuthAuditLog.class));
        }

        @Test
        @DisplayName("should_throwException_when_roleNotFound")
        void should_throwException_when_roleNotFound() {
            // given
            AssignRoleRequest request = new AssignRoleRequest(USER_ID, "ROLE_UNKNOWN", null);
            when(roleEntityRepository.findByRoleKey("ROLE_UNKNOWN")).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> rbacService.assignRole(request, ADMIN_ID))
                    .isInstanceOf(CustomBusinessException.class)
                    .satisfies(ex -> {
                        CustomBusinessException cbe = (CustomBusinessException) ex;
                        assertThat(cbe.getErrorCode()).isEqualTo(AuthErrorCode.ROLE_NOT_FOUND);
                    });
        }

        @Test
        @DisplayName("should_throwException_when_roleAlreadyAssigned")
        void should_throwException_when_roleAlreadyAssigned() {
            // given
            RoleEntity role = createRole("ROLE_SELLER", false);
            UserRole existingUserRole = createUserRole(USER_ID, role);
            AssignRoleRequest request = new AssignRoleRequest(USER_ID, "ROLE_SELLER", null);

            when(roleEntityRepository.findByRoleKey("ROLE_SELLER")).thenReturn(Optional.of(role));
            when(userRoleRepository.findByUserIdWithRole(USER_ID)).thenReturn(List.of(existingUserRole));

            // when & then
            assertThatThrownBy(() -> rbacService.assignRole(request, ADMIN_ID))
                    .isInstanceOf(CustomBusinessException.class)
                    .satisfies(ex -> {
                        CustomBusinessException cbe = (CustomBusinessException) ex;
                        assertThat(cbe.getErrorCode()).isEqualTo(AuthErrorCode.ROLE_ALREADY_ASSIGNED);
                    });
        }
    }

    @Nested
    @DisplayName("revokeRole")
    class RevokeRole {

        @Test
        @DisplayName("should_revokeRole_when_roleAssigned")
        void should_revokeRole_when_roleAssigned() {
            // given
            RoleEntity role = createRole("ROLE_SELLER", false);
            UserRole userRole = createUserRole(USER_ID, role);

            when(roleEntityRepository.findByRoleKey("ROLE_SELLER")).thenReturn(Optional.of(role));
            when(userRoleRepository.findByUserIdWithRole(USER_ID)).thenReturn(List.of(userRole));

            // when
            rbacService.revokeRole(USER_ID, "ROLE_SELLER", ADMIN_ID);

            // then
            verify(userRoleRepository).delete(userRole);
            verify(auditLogRepository).save(any(AuthAuditLog.class));
        }

        @Test
        @DisplayName("should_throwException_when_systemRoleUser")
        void should_throwException_when_systemRoleUser() {
            // given
            RoleEntity role = createRole("ROLE_USER", true);
            when(roleEntityRepository.findByRoleKey("ROLE_USER")).thenReturn(Optional.of(role));

            // when & then
            assertThatThrownBy(() -> rbacService.revokeRole(USER_ID, "ROLE_USER", ADMIN_ID))
                    .isInstanceOf(CustomBusinessException.class)
                    .satisfies(ex -> {
                        CustomBusinessException cbe = (CustomBusinessException) ex;
                        assertThat(cbe.getErrorCode()).isEqualTo(AuthErrorCode.SYSTEM_ROLE_CANNOT_BE_MODIFIED);
                    });
        }

        @Test
        @DisplayName("should_throwException_when_roleNotAssigned")
        void should_throwException_when_roleNotAssigned() {
            // given
            RoleEntity role = createRole("ROLE_SELLER", false);
            when(roleEntityRepository.findByRoleKey("ROLE_SELLER")).thenReturn(Optional.of(role));
            when(userRoleRepository.findByUserIdWithRole(USER_ID)).thenReturn(List.of());

            // when & then
            assertThatThrownBy(() -> rbacService.revokeRole(USER_ID, "ROLE_SELLER", ADMIN_ID))
                    .isInstanceOf(CustomBusinessException.class)
                    .satisfies(ex -> {
                        CustomBusinessException cbe = (CustomBusinessException) ex;
                        assertThat(cbe.getErrorCode()).isEqualTo(AuthErrorCode.ROLE_NOT_ASSIGNED);
                    });
        }
    }
}

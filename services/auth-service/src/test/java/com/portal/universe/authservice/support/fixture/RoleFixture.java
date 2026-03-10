package com.portal.universe.authservice.support.fixture;

import com.portal.universe.authservice.auth.domain.PermissionEntity;
import com.portal.universe.authservice.auth.domain.RoleEntity;
import com.portal.universe.authservice.auth.domain.UserRole;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Role, Permission, UserRole 테스트 데이터 빌더.
 */
public final class RoleFixture {

    private RoleFixture() {}

    // ========== RoleEntity ==========

    public static RoleEntity createUserRole() {
        return roleBuilder().build();
    }

    public static RoleEntity createAdminRole() {
        return roleBuilder()
                .roleKey("ROLE_SUPER_ADMIN")
                .displayName("Super Admin")
                .system(true)
                .build();
    }

    public static RoleEntity createSellerRole() {
        return roleBuilder()
                .roleKey("ROLE_SELLER")
                .displayName("Seller")
                .serviceScope("shopping")
                .membershipGroup("seller:shopping")
                .build();
    }

    public static RoleEntityBuilder roleBuilder() {
        return new RoleEntityBuilder();
    }

    public static class RoleEntityBuilder {
        private Long id;
        private String roleKey = "ROLE_USER";
        private String displayName = "User";
        private String description = "Default user role";
        private String serviceScope;
        private String membershipGroup;
        private boolean system = false;

        public RoleEntityBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public RoleEntityBuilder roleKey(String roleKey) {
            this.roleKey = roleKey;
            return this;
        }

        public RoleEntityBuilder displayName(String displayName) {
            this.displayName = displayName;
            return this;
        }

        public RoleEntityBuilder description(String description) {
            this.description = description;
            return this;
        }

        public RoleEntityBuilder serviceScope(String serviceScope) {
            this.serviceScope = serviceScope;
            return this;
        }

        public RoleEntityBuilder membershipGroup(String membershipGroup) {
            this.membershipGroup = membershipGroup;
            return this;
        }

        public RoleEntityBuilder system(boolean system) {
            this.system = system;
            return this;
        }

        public RoleEntity build() {
            RoleEntity role = RoleEntity.builder()
                    .roleKey(roleKey)
                    .displayName(displayName)
                    .description(description)
                    .serviceScope(serviceScope)
                    .membershipGroup(membershipGroup)
                    .system(system)
                    .build();
            if (id != null) {
                ReflectionTestUtils.setField(role, "id", id);
            }
            return role;
        }
    }

    // ========== PermissionEntity ==========

    public static PermissionEntity createPermission() {
        return permissionBuilder().build();
    }

    public static PermissionEntityBuilder permissionBuilder() {
        return new PermissionEntityBuilder();
    }

    public static class PermissionEntityBuilder {
        private Long id;
        private String permissionKey = "blog:post:read";
        private String service = "blog";
        private String resource = "post";
        private String action = "read";
        private String description = "Read blog posts";

        public PermissionEntityBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public PermissionEntityBuilder permissionKey(String permissionKey) {
            this.permissionKey = permissionKey;
            return this;
        }

        public PermissionEntityBuilder service(String service) {
            this.service = service;
            return this;
        }

        public PermissionEntityBuilder resource(String resource) {
            this.resource = resource;
            return this;
        }

        public PermissionEntityBuilder action(String action) {
            this.action = action;
            return this;
        }

        public PermissionEntityBuilder description(String description) {
            this.description = description;
            return this;
        }

        public PermissionEntity build() {
            PermissionEntity permission = PermissionEntity.builder()
                    .permissionKey(permissionKey)
                    .service(service)
                    .resource(resource)
                    .action(action)
                    .description(description)
                    .build();
            if (id != null) {
                ReflectionTestUtils.setField(permission, "id", id);
            }
            return permission;
        }
    }

    // ========== UserRole ==========

    public static UserRole createUserRole(String userId, RoleEntity role) {
        return UserRole.builder()
                .userId(userId)
                .role(role)
                .assignedBy("system")
                .build();
    }
}

package com.portal.universe.authservice.auth.service;

import com.portal.universe.authservice.auth.domain.*;
import com.portal.universe.authservice.auth.repository.*;
import com.portal.universe.authservice.user.domain.Role;
import com.portal.universe.authservice.user.domain.User;
import com.portal.universe.authservice.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * RBAC 초기 데이터 시딩 및 기존 사용자 마이그레이션을 수행합니다.
 * 애플리케이션 시작 시 한 번만 실행되며, 이미 데이터가 존재하면 스킵합니다.
 */
@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class RbacDataMigrationRunner implements ApplicationRunner {

    private final RoleEntityRepository roleEntityRepository;
    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final UserRoleRepository userRoleRepository;
    private final MembershipTierRepository membershipTierRepository;
    private final UserMembershipRepository userMembershipRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (roleEntityRepository.count() > 0) {
            log.info("RBAC data already initialized, skipping migration");
            return;
        }

        log.info("Starting RBAC data migration...");

        seedRoles();
        seedPermissions();
        mapRolePermissions();
        seedMembershipTiers();
        migrateExistingUsers();

        log.info("RBAC data migration completed successfully");
    }

    private void seedRoles() {
        log.info("Seeding roles...");

        // Global roles
        RoleEntity userRole = roleEntityRepository.save(RoleEntity.builder()
                .roleKey("ROLE_USER")
                .displayName("User")
                .description("Default user role with basic access")
                .system(true)
                .build());

        RoleEntity superAdmin = roleEntityRepository.save(RoleEntity.builder()
                .roleKey("ROLE_SUPER_ADMIN")
                .displayName("Super Admin")
                .description("System-wide administrator with full access")
                .system(true)
                .build());

        // Shopping service roles
        roleEntityRepository.save(RoleEntity.builder()
                .roleKey("ROLE_SELLER")
                .displayName("Seller")
                .description("Seller who can manage own products")
                .serviceScope("shopping")
                .parentRole(userRole)
                .system(true)
                .build());

        roleEntityRepository.save(RoleEntity.builder()
                .roleKey("ROLE_SHOPPING_ADMIN")
                .displayName("Shopping Admin")
                .description("Shopping service administrator")
                .serviceScope("shopping")
                .parentRole(superAdmin)
                .system(true)
                .build());

        // Blog service roles
        roleEntityRepository.save(RoleEntity.builder()
                .roleKey("ROLE_BLOG_ADMIN")
                .displayName("Blog Admin")
                .description("Blog service administrator")
                .serviceScope("blog")
                .parentRole(superAdmin)
                .system(true)
                .build());

        log.info("Seeded 5 system roles");
    }

    private void seedPermissions() {
        log.info("Seeding permissions...");

        // Shopping permissions
        List<Map<String, String>> shoppingPermissions = List.of(
                Map.of("key", "shopping:product:create", "resource", "product", "action", "create", "desc", "Create products"),
                Map.of("key", "shopping:product:read", "resource", "product", "action", "read", "desc", "View products"),
                Map.of("key", "shopping:product:update", "resource", "product", "action", "update", "desc", "Update products"),
                Map.of("key", "shopping:product:delete", "resource", "product", "action", "delete", "desc", "Delete products"),
                Map.of("key", "shopping:order:read", "resource", "order", "action", "read", "desc", "View orders"),
                Map.of("key", "shopping:order:manage", "resource", "order", "action", "manage", "desc", "Manage all orders"),
                Map.of("key", "shopping:inventory:manage", "resource", "inventory", "action", "manage", "desc", "Manage inventory"),
                Map.of("key", "shopping:delivery:manage", "resource", "delivery", "action", "manage", "desc", "Manage deliveries"),
                Map.of("key", "shopping:coupon:manage", "resource", "coupon", "action", "manage", "desc", "Manage coupons"),
                Map.of("key", "shopping:timedeal:manage", "resource", "timedeal", "action", "manage", "desc", "Manage time deals"),
                Map.of("key", "shopping:timedeal:early_access", "resource", "timedeal", "action", "early_access", "desc", "Early access to time deals"),
                Map.of("key", "shopping:analytics:read", "resource", "analytics", "action", "read", "desc", "View shopping analytics")
        );

        for (Map<String, String> p : shoppingPermissions) {
            permissionRepository.save(PermissionEntity.builder()
                    .permissionKey(p.get("key"))
                    .service("shopping")
                    .resource(p.get("resource"))
                    .action(p.get("action"))
                    .description(p.get("desc"))
                    .build());
        }

        // Blog permissions
        List<Map<String, String>> blogPermissions = List.of(
                Map.of("key", "blog:post:create", "resource", "post", "action", "create", "desc", "Create blog posts"),
                Map.of("key", "blog:post:read", "resource", "post", "action", "read", "desc", "Read blog posts"),
                Map.of("key", "blog:post:update", "resource", "post", "action", "update", "desc", "Update blog posts"),
                Map.of("key", "blog:post:delete", "resource", "post", "action", "delete", "desc", "Delete blog posts"),
                Map.of("key", "blog:post:manage", "resource", "post", "action", "manage", "desc", "Manage all blog posts"),
                Map.of("key", "blog:comment:manage", "resource", "comment", "action", "manage", "desc", "Manage all comments"),
                Map.of("key", "blog:file:delete", "resource", "file", "action", "delete", "desc", "Delete uploaded files"),
                Map.of("key", "blog:analytics:read", "resource", "analytics", "action", "read", "desc", "View blog analytics")
        );

        for (Map<String, String> p : blogPermissions) {
            permissionRepository.save(PermissionEntity.builder()
                    .permissionKey(p.get("key"))
                    .service("blog")
                    .resource(p.get("resource"))
                    .action(p.get("action"))
                    .description(p.get("desc"))
                    .build());
        }

        // Auth permissions
        List<Map<String, String>> authPermissions = List.of(
                Map.of("key", "auth:role:manage", "resource", "role", "action", "manage", "desc", "Manage roles"),
                Map.of("key", "auth:permission:manage", "resource", "permission", "action", "manage", "desc", "Manage permissions"),
                Map.of("key", "auth:membership:manage", "resource", "membership", "action", "manage", "desc", "Manage memberships"),
                Map.of("key", "auth:user:manage", "resource", "user", "action", "manage", "desc", "Manage users"),
                Map.of("key", "auth:seller:approve", "resource", "seller", "action", "approve", "desc", "Approve seller applications"),
                Map.of("key", "auth:audit:read", "resource", "audit", "action", "read", "desc", "Read audit logs")
        );

        for (Map<String, String> p : authPermissions) {
            permissionRepository.save(PermissionEntity.builder()
                    .permissionKey(p.get("key"))
                    .service("auth")
                    .resource(p.get("resource"))
                    .action(p.get("action"))
                    .description(p.get("desc"))
                    .build());
        }

        log.info("Seeded {} permissions", shoppingPermissions.size() + blogPermissions.size() + authPermissions.size());
    }

    private void mapRolePermissions() {
        log.info("Mapping role-permission relationships...");

        // ROLE_USER: basic read permissions
        RoleEntity userRole = roleEntityRepository.findByRoleKey("ROLE_USER").orElseThrow();
        mapPermissions(userRole, List.of(
                "shopping:product:read",
                "shopping:order:read",
                "blog:post:create",
                "blog:post:read",
                "blog:post:update",
                "blog:post:delete"
        ));

        // ROLE_SELLER: product management + own analytics
        RoleEntity sellerRole = roleEntityRepository.findByRoleKey("ROLE_SELLER").orElseThrow();
        mapPermissions(sellerRole, List.of(
                "shopping:product:create",
                "shopping:product:read",
                "shopping:product:update",
                "shopping:product:delete",
                "shopping:order:read",
                "shopping:analytics:read"
        ));

        // ROLE_SHOPPING_ADMIN: all shopping permissions
        RoleEntity shoppingAdmin = roleEntityRepository.findByRoleKey("ROLE_SHOPPING_ADMIN").orElseThrow();
        mapPermissions(shoppingAdmin, List.of(
                "shopping:product:create",
                "shopping:product:read",
                "shopping:product:update",
                "shopping:product:delete",
                "shopping:order:read",
                "shopping:order:manage",
                "shopping:inventory:manage",
                "shopping:delivery:manage",
                "shopping:coupon:manage",
                "shopping:timedeal:manage",
                "shopping:analytics:read",
                "auth:seller:approve"
        ));

        // ROLE_BLOG_ADMIN: all blog permissions
        RoleEntity blogAdmin = roleEntityRepository.findByRoleKey("ROLE_BLOG_ADMIN").orElseThrow();
        mapPermissions(blogAdmin, List.of(
                "blog:post:create",
                "blog:post:read",
                "blog:post:update",
                "blog:post:delete",
                "blog:post:manage",
                "blog:comment:manage",
                "blog:file:delete",
                "blog:analytics:read"
        ));

        // ROLE_SUPER_ADMIN: all permissions (no explicit mapping needed, checked by wildcard)
        // But we map auth management permissions explicitly
        RoleEntity superAdmin = roleEntityRepository.findByRoleKey("ROLE_SUPER_ADMIN").orElseThrow();
        mapPermissions(superAdmin, List.of(
                "auth:role:manage",
                "auth:permission:manage",
                "auth:membership:manage",
                "auth:user:manage",
                "auth:seller:approve",
                "auth:audit:read"
        ));

        log.info("Role-permission mappings completed");
    }

    private void mapPermissions(RoleEntity role, List<String> permissionKeys) {
        for (String key : permissionKeys) {
            permissionRepository.findByPermissionKey(key).ifPresent(permission ->
                    rolePermissionRepository.save(new RolePermission(role, permission))
            );
        }
    }

    private void seedMembershipTiers() {
        log.info("Seeding membership tiers...");

        // Shopping tiers
        membershipTierRepository.save(MembershipTier.builder()
                .serviceName("shopping").tierKey("FREE").displayName("Free")
                .sortOrder(0).build());
        membershipTierRepository.save(MembershipTier.builder()
                .serviceName("shopping").tierKey("BASIC").displayName("Basic")
                .priceMonthly(new BigDecimal("4900")).priceYearly(new BigDecimal("49000"))
                .sortOrder(1).build());
        membershipTierRepository.save(MembershipTier.builder()
                .serviceName("shopping").tierKey("PREMIUM").displayName("Premium")
                .priceMonthly(new BigDecimal("9900")).priceYearly(new BigDecimal("99000"))
                .sortOrder(2).build());
        membershipTierRepository.save(MembershipTier.builder()
                .serviceName("shopping").tierKey("VIP").displayName("VIP")
                .priceMonthly(new BigDecimal("19900")).priceYearly(new BigDecimal("199000"))
                .sortOrder(3).build());

        // Blog tiers
        membershipTierRepository.save(MembershipTier.builder()
                .serviceName("blog").tierKey("FREE").displayName("Free")
                .sortOrder(0).build());
        membershipTierRepository.save(MembershipTier.builder()
                .serviceName("blog").tierKey("BASIC").displayName("Basic")
                .priceMonthly(new BigDecimal("2900")).priceYearly(new BigDecimal("29000"))
                .sortOrder(1).build());
        membershipTierRepository.save(MembershipTier.builder()
                .serviceName("blog").tierKey("PREMIUM").displayName("Premium")
                .priceMonthly(new BigDecimal("5900")).priceYearly(new BigDecimal("59000"))
                .sortOrder(2).build());
        membershipTierRepository.save(MembershipTier.builder()
                .serviceName("blog").tierKey("VIP").displayName("VIP")
                .priceMonthly(new BigDecimal("9900")).priceYearly(new BigDecimal("99000"))
                .sortOrder(3).build());

        log.info("Seeded 8 membership tiers (4 shopping + 4 blog)");
    }

    private void migrateExistingUsers() {
        log.info("Migrating existing users to RBAC...");

        RoleEntity userRoleEntity = roleEntityRepository.findByRoleKey("ROLE_USER").orElseThrow();
        RoleEntity superAdminEntity = roleEntityRepository.findByRoleKey("ROLE_SUPER_ADMIN").orElseThrow();
        MembershipTier shoppingFree = membershipTierRepository.findByServiceNameAndTierKey("shopping", "FREE").orElseThrow();
        MembershipTier blogFree = membershipTierRepository.findByServiceNameAndTierKey("blog", "FREE").orElseThrow();

        List<User> allUsers = userRepository.findAll();
        int migratedCount = 0;

        for (User user : allUsers) {
            String uuid = user.getUuid();

            // Skip if already migrated
            if (!userRoleRepository.findByUserId(uuid).isEmpty()) {
                continue;
            }

            // Assign ROLE_USER to all users
            userRoleRepository.save(UserRole.builder()
                    .userId(uuid)
                    .role(userRoleEntity)
                    .assignedBy("SYSTEM_MIGRATION")
                    .build());

            // If user was ADMIN, also assign ROLE_SUPER_ADMIN
            if (user.getRole() == Role.ADMIN) {
                userRoleRepository.save(UserRole.builder()
                        .userId(uuid)
                        .role(superAdminEntity)
                        .assignedBy("SYSTEM_MIGRATION")
                        .build());
            }

            // Create default FREE memberships
            if (!userMembershipRepository.existsByUserIdAndServiceName(uuid, "shopping")) {
                userMembershipRepository.save(UserMembership.builder()
                        .userId(uuid)
                        .serviceName("shopping")
                        .tier(shoppingFree)
                        .build());
            }

            if (!userMembershipRepository.existsByUserIdAndServiceName(uuid, "blog")) {
                userMembershipRepository.save(UserMembership.builder()
                        .userId(uuid)
                        .serviceName("blog")
                        .tier(blogFree)
                        .build());
            }

            migratedCount++;
        }

        log.info("Migrated {} existing users to RBAC system", migratedCount);
    }
}

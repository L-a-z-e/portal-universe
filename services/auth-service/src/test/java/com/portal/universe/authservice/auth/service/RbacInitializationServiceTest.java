package com.portal.universe.authservice.auth.service;

import com.portal.universe.authservice.auth.domain.MembershipTier;
import com.portal.universe.authservice.auth.domain.RoleEntity;
import com.portal.universe.authservice.auth.domain.UserMembership;
import com.portal.universe.authservice.auth.domain.UserRole;
import com.portal.universe.authservice.auth.repository.MembershipTierRepository;
import com.portal.universe.authservice.auth.repository.RoleEntityRepository;
import com.portal.universe.authservice.auth.repository.UserMembershipRepository;
import com.portal.universe.authservice.auth.repository.UserRoleRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
    private MembershipTierRepository membershipTierRepository;

    @Mock
    private UserMembershipRepository userMembershipRepository;

    @InjectMocks
    private RbacInitializationService rbacInitializationService;

    private static final String USER_ID = "test-uuid";

    @Nested
    @DisplayName("initializeNewUser")
    class InitializeNewUser {

        @Test
        @DisplayName("should_assignRoleAndMemberships_when_newUser")
        void should_assignRoleAndMemberships_when_newUser() {
            // given
            when(userRoleRepository.findByUserId(USER_ID)).thenReturn(Collections.emptyList());

            RoleEntity userRole = RoleEntity.builder()
                    .roleKey("ROLE_USER")
                    .displayName("User")
                    .system(true)
                    .build();
            when(roleEntityRepository.findByRoleKey("ROLE_USER")).thenReturn(Optional.of(userRole));
            when(userRoleRepository.save(any(UserRole.class))).thenAnswer(invocation -> invocation.getArgument(0));

            MembershipTier shoppingFree = MembershipTier.builder()
                    .serviceName("shopping")
                    .tierKey("FREE")
                    .displayName("Free")
                    .sortOrder(0)
                    .build();
            MembershipTier blogFree = MembershipTier.builder()
                    .serviceName("blog")
                    .tierKey("FREE")
                    .displayName("Free")
                    .sortOrder(0)
                    .build();

            when(userMembershipRepository.existsByUserIdAndServiceName(USER_ID, "shopping")).thenReturn(false);
            when(userMembershipRepository.existsByUserIdAndServiceName(USER_ID, "blog")).thenReturn(false);
            when(membershipTierRepository.findByServiceNameAndTierKey("shopping", "FREE"))
                    .thenReturn(Optional.of(shoppingFree));
            when(membershipTierRepository.findByServiceNameAndTierKey("blog", "FREE"))
                    .thenReturn(Optional.of(blogFree));
            when(userMembershipRepository.save(any(UserMembership.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // when
            rbacInitializationService.initializeNewUser(USER_ID);

            // then
            verify(userRoleRepository).save(any(UserRole.class));
            verify(userMembershipRepository, times(2)).save(any(UserMembership.class));
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
            verify(userMembershipRepository, never()).save(any(UserMembership.class));
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

        @Test
        @DisplayName("should_throwException_when_membershipTierNotFound")
        void should_throwException_when_membershipTierNotFound() {
            // given
            when(userRoleRepository.findByUserId(USER_ID)).thenReturn(Collections.emptyList());

            RoleEntity userRole = RoleEntity.builder()
                    .roleKey("ROLE_USER")
                    .displayName("User")
                    .system(true)
                    .build();
            when(roleEntityRepository.findByRoleKey("ROLE_USER")).thenReturn(Optional.of(userRole));
            when(userRoleRepository.save(any(UserRole.class))).thenAnswer(invocation -> invocation.getArgument(0));

            when(userMembershipRepository.existsByUserIdAndServiceName(USER_ID, "shopping")).thenReturn(false);
            when(membershipTierRepository.findByServiceNameAndTierKey("shopping", "FREE"))
                    .thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> rbacInitializationService.initializeNewUser(USER_ID))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("FREE not found");
        }

        @Test
        @DisplayName("should_skipMembershipCreation_when_alreadyExists")
        void should_skipMembershipCreation_when_alreadyExists() {
            // given
            when(userRoleRepository.findByUserId(USER_ID)).thenReturn(Collections.emptyList());

            RoleEntity userRole = RoleEntity.builder()
                    .roleKey("ROLE_USER")
                    .displayName("User")
                    .system(true)
                    .build();
            when(roleEntityRepository.findByRoleKey("ROLE_USER")).thenReturn(Optional.of(userRole));
            when(userRoleRepository.save(any(UserRole.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Both memberships already exist
            when(userMembershipRepository.existsByUserIdAndServiceName(USER_ID, "shopping")).thenReturn(true);
            when(userMembershipRepository.existsByUserIdAndServiceName(USER_ID, "blog")).thenReturn(true);

            // when
            rbacInitializationService.initializeNewUser(USER_ID);

            // then
            verify(userRoleRepository).save(any(UserRole.class));
            verify(userMembershipRepository, never()).save(any(UserMembership.class));
        }
    }
}

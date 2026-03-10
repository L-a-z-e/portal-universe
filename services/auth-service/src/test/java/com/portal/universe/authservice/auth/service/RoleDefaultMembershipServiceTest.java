package com.portal.universe.authservice.auth.service;

import com.portal.universe.authservice.auth.domain.RoleDefaultMembership;
import com.portal.universe.authservice.auth.dto.rbac.RoleDefaultMappingRequest;
import com.portal.universe.authservice.auth.dto.rbac.RoleDefaultMappingResponse;
import com.portal.universe.authservice.auth.repository.MembershipTierRepository;
import com.portal.universe.authservice.auth.repository.RoleDefaultMembershipRepository;
import com.portal.universe.authservice.auth.repository.RoleEntityRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RoleDefaultMembershipService 테스트")
class RoleDefaultMembershipServiceTest {

    @Mock
    private RoleDefaultMembershipRepository roleDefaultMembershipRepository;
    @Mock
    private RoleEntityRepository roleEntityRepository;
    @Mock
    private MembershipTierRepository membershipTierRepository;

    @InjectMocks
    private RoleDefaultMembershipService roleDefaultMembershipService;

    private RoleDefaultMembership createMapping(String roleKey, String group, String tierKey) {
        return RoleDefaultMembership.builder()
                .roleKey(roleKey)
                .membershipGroup(group)
                .defaultTierKey(tierKey)
                .build();
    }

    @Nested
    @DisplayName("getAllMappings")
    class GetAllMappings {

        @Test
        @DisplayName("should_returnAllMappings_when_exist")
        void should_returnAllMappings_when_exist() {
            // given
            List<RoleDefaultMembership> mappings = List.of(
                    createMapping("ROLE_USER", "user:blog", "FREE"),
                    createMapping("ROLE_USER", "user:shopping", "FREE")
            );
            when(roleDefaultMembershipRepository.findAll()).thenReturn(mappings);

            // when
            List<RoleDefaultMappingResponse> result = roleDefaultMembershipService.getAllMappings();

            // then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).roleKey()).isEqualTo("ROLE_USER");
        }
    }

    @Nested
    @DisplayName("getMappingsByRoleKey")
    class GetMappingsByRoleKey {

        @Test
        @DisplayName("should_returnMappings_when_roleKeyExists")
        void should_returnMappings_when_roleKeyExists() {
            // given
            when(roleDefaultMembershipRepository.findByRoleKey("ROLE_USER"))
                    .thenReturn(List.of(createMapping("ROLE_USER", "user:blog", "FREE")));

            // when
            List<RoleDefaultMappingResponse> result =
                    roleDefaultMembershipService.getMappingsByRoleKey("ROLE_USER");

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).membershipGroup()).isEqualTo("user:blog");
        }
    }

    @Nested
    @DisplayName("addMapping")
    class AddMapping {

        @Test
        @DisplayName("should_addMapping_when_validRequest")
        void should_addMapping_when_validRequest() {
            // given
            RoleDefaultMappingRequest request = new RoleDefaultMappingRequest(
                    "ROLE_USER", "user:blog", "FREE");

            when(roleEntityRepository.existsByRoleKey("ROLE_USER")).thenReturn(true);
            when(membershipTierRepository.existsByMembershipGroupAndTierKey("user:blog", "FREE"))
                    .thenReturn(true);
            when(roleDefaultMembershipRepository.existsByRoleKeyAndMembershipGroup("ROLE_USER", "user:blog"))
                    .thenReturn(false);
            RoleDefaultMembership saved = createMapping("ROLE_USER", "user:blog", "FREE");
            when(roleDefaultMembershipRepository.save(any(RoleDefaultMembership.class))).thenReturn(saved);

            // when
            RoleDefaultMappingResponse result = roleDefaultMembershipService.addMapping(request, "admin-uuid");

            // then
            assertThat(result.roleKey()).isEqualTo("ROLE_USER");
            assertThat(result.membershipGroup()).isEqualTo("user:blog");
            verify(roleDefaultMembershipRepository).save(any());
        }

        @Test
        @DisplayName("should_throwException_when_roleNotFound")
        void should_throwException_when_roleNotFound() {
            // given
            RoleDefaultMappingRequest request = new RoleDefaultMappingRequest(
                    "ROLE_UNKNOWN", "user:blog", "FREE");
            when(roleEntityRepository.existsByRoleKey("ROLE_UNKNOWN")).thenReturn(false);

            // when & then
            assertThatThrownBy(() -> roleDefaultMembershipService.addMapping(request, "admin-uuid"))
                    .isInstanceOf(CustomBusinessException.class)
                    .satisfies(ex -> {
                        CustomBusinessException cbe = (CustomBusinessException) ex;
                        assertThat(cbe.getErrorCode()).isEqualTo(AuthErrorCode.ROLE_NOT_FOUND);
                    });
        }

        @Test
        @DisplayName("should_throwException_when_invalidMembershipGroupFormat")
        void should_throwException_when_invalidMembershipGroupFormat() {
            // given
            RoleDefaultMappingRequest request = new RoleDefaultMappingRequest(
                    "ROLE_USER", "INVALID_FORMAT", "FREE");
            when(roleEntityRepository.existsByRoleKey("ROLE_USER")).thenReturn(true);

            // when & then
            assertThatThrownBy(() -> roleDefaultMembershipService.addMapping(request, "admin-uuid"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("should_throwException_when_tierNotFound")
        void should_throwException_when_tierNotFound() {
            // given
            RoleDefaultMappingRequest request = new RoleDefaultMappingRequest(
                    "ROLE_USER", "user:blog", "UNKNOWN_TIER");
            when(roleEntityRepository.existsByRoleKey("ROLE_USER")).thenReturn(true);
            when(membershipTierRepository.existsByMembershipGroupAndTierKey("user:blog", "UNKNOWN_TIER"))
                    .thenReturn(false);

            // when & then
            assertThatThrownBy(() -> roleDefaultMembershipService.addMapping(request, "admin-uuid"))
                    .isInstanceOf(CustomBusinessException.class)
                    .satisfies(ex -> {
                        CustomBusinessException cbe = (CustomBusinessException) ex;
                        assertThat(cbe.getErrorCode()).isEqualTo(AuthErrorCode.MEMBERSHIP_TIER_NOT_FOUND);
                    });
        }

        @Test
        @DisplayName("should_throwException_when_duplicateMapping")
        void should_throwException_when_duplicateMapping() {
            // given
            RoleDefaultMappingRequest request = new RoleDefaultMappingRequest(
                    "ROLE_USER", "user:blog", "FREE");
            when(roleEntityRepository.existsByRoleKey("ROLE_USER")).thenReturn(true);
            when(membershipTierRepository.existsByMembershipGroupAndTierKey("user:blog", "FREE"))
                    .thenReturn(true);
            when(roleDefaultMembershipRepository.existsByRoleKeyAndMembershipGroup("ROLE_USER", "user:blog"))
                    .thenReturn(true);

            // when & then
            assertThatThrownBy(() -> roleDefaultMembershipService.addMapping(request, "admin-uuid"))
                    .isInstanceOf(CustomBusinessException.class)
                    .satisfies(ex -> {
                        CustomBusinessException cbe = (CustomBusinessException) ex;
                        assertThat(cbe.getErrorCode()).isEqualTo(AuthErrorCode.ROLE_DEFAULT_MAPPING_ALREADY_EXISTS);
                    });
        }
    }

    @Nested
    @DisplayName("removeMapping")
    class RemoveMapping {

        @Test
        @DisplayName("should_removeMapping_when_exists")
        void should_removeMapping_when_exists() {
            // given
            when(roleDefaultMembershipRepository.existsByRoleKeyAndMembershipGroup("ROLE_USER", "user:blog"))
                    .thenReturn(true);

            // when
            roleDefaultMembershipService.removeMapping("ROLE_USER", "user:blog", "admin-uuid");

            // then
            verify(roleDefaultMembershipRepository)
                    .deleteByRoleKeyAndMembershipGroup("ROLE_USER", "user:blog");
        }

        @Test
        @DisplayName("should_throwException_when_mappingNotFound")
        void should_throwException_when_mappingNotFound() {
            // given
            when(roleDefaultMembershipRepository.existsByRoleKeyAndMembershipGroup("ROLE_USER", "user:blog"))
                    .thenReturn(false);

            // when & then
            assertThatThrownBy(() -> roleDefaultMembershipService.removeMapping(
                    "ROLE_USER", "user:blog", "admin-uuid"))
                    .isInstanceOf(CustomBusinessException.class)
                    .satisfies(ex -> {
                        CustomBusinessException cbe = (CustomBusinessException) ex;
                        assertThat(cbe.getErrorCode()).isEqualTo(AuthErrorCode.ROLE_DEFAULT_MAPPING_NOT_FOUND);
                    });
        }
    }
}

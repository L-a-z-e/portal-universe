package com.portal.universe.authservice.auth.service;

import com.portal.universe.authservice.auth.domain.*;
import com.portal.universe.authservice.auth.dto.membership.ChangeMembershipRequest;
import com.portal.universe.authservice.auth.dto.membership.MembershipResponse;
import com.portal.universe.authservice.auth.dto.membership.MembershipTierResponse;
import com.portal.universe.authservice.auth.repository.AuthAuditLogRepository;
import com.portal.universe.authservice.auth.repository.MembershipTierRepository;
import com.portal.universe.authservice.auth.repository.UserMembershipRepository;
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
@DisplayName("MembershipService 테스트")
class MembershipServiceTest {

    @Mock
    private UserMembershipRepository userMembershipRepository;

    @Mock
    private MembershipTierRepository membershipTierRepository;

    @Mock
    private AuthAuditLogRepository auditLogRepository;

    @InjectMocks
    private MembershipService membershipService;

    private static final String USER_ID = "test-uuid";

    private MembershipTier createTier(String serviceName, String tierKey) {
        return MembershipTier.builder()
                .membershipGroup(serviceName)
                .tierKey(tierKey)
                .displayName(tierKey)
                .sortOrder(0)
                .build();
    }

    private UserMembership createMembership(String serviceName, MembershipTier tier) {
        return UserMembership.builder()
                .userId(USER_ID)
                .membershipGroup(serviceName)
                .tier(tier)
                .build();
    }

    @Nested
    @DisplayName("getUserMemberships")
    class GetUserMemberships {

        @Test
        @DisplayName("should_returnMemberships_when_userHasMemberships")
        void should_returnMemberships_when_userHasMemberships() {
            // given
            MembershipTier tier = createTier("user:shopping", "FREE");
            UserMembership membership = createMembership("user:shopping", tier);
            when(userMembershipRepository.findByUserId(USER_ID)).thenReturn(List.of(membership));

            // when
            List<MembershipResponse> result = membershipService.getUserMemberships(USER_ID);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).membershipGroup()).isEqualTo("user:shopping");
            assertThat(result.get(0).tierKey()).isEqualTo("FREE");
        }

        @Test
        @DisplayName("should_returnEmptyList_when_userHasNoMemberships")
        void should_returnEmptyList_when_userHasNoMemberships() {
            // given
            when(userMembershipRepository.findByUserId(USER_ID)).thenReturn(List.of());

            // when
            List<MembershipResponse> result = membershipService.getUserMemberships(USER_ID);

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getUserMembership")
    class GetUserMembership {

        @Test
        @DisplayName("should_returnMembership_when_exists")
        void should_returnMembership_when_exists() {
            // given
            MembershipTier tier = createTier("user:shopping", "PREMIUM");
            UserMembership membership = createMembership("user:shopping", tier);
            when(userMembershipRepository.findByUserIdAndMembershipGroup(USER_ID, "user:shopping"))
                    .thenReturn(Optional.of(membership));

            // when
            MembershipResponse result = membershipService.getUserMembership(USER_ID, "user:shopping");

            // then
            assertThat(result.tierKey()).isEqualTo("PREMIUM");
        }

        @Test
        @DisplayName("should_throwException_when_membershipNotFound")
        void should_throwException_when_membershipNotFound() {
            // given
            when(userMembershipRepository.findByUserIdAndMembershipGroup(USER_ID, "user:shopping"))
                    .thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> membershipService.getUserMembership(USER_ID, "user:shopping"))
                    .isInstanceOf(CustomBusinessException.class)
                    .satisfies(ex -> {
                        CustomBusinessException cbe = (CustomBusinessException) ex;
                        assertThat(cbe.getErrorCode()).isEqualTo(AuthErrorCode.MEMBERSHIP_NOT_FOUND);
                    });
        }
    }

    @Nested
    @DisplayName("getServiceTiers")
    class GetServiceTiers {

        @Test
        @DisplayName("should_returnTiers_when_serviceExists")
        void should_returnTiers_when_serviceExists() {
            // given
            MembershipTier free = createTier("user:shopping", "FREE");
            MembershipTier premium = createTier("user:shopping", "PREMIUM");
            when(membershipTierRepository.findByMembershipGroupOrderBySortOrder("user:shopping"))
                    .thenReturn(List.of(free, premium));

            // when
            List<MembershipTierResponse> result = membershipService.getGroupTiers("user:shopping");

            // then
            assertThat(result).hasSize(2);
        }
    }

    @Nested
    @DisplayName("changeMembershipTier")
    class ChangeMembershipTier {

        @Test
        @DisplayName("should_changeTier_when_validRequest")
        void should_changeTier_when_validRequest() {
            // given
            MembershipTier freeTier = createTier("user:shopping", "FREE");
            MembershipTier premiumTier = createTier("user:shopping", "PREMIUM");
            UserMembership membership = createMembership("user:shopping", freeTier);

            ChangeMembershipRequest request = new ChangeMembershipRequest("user:shopping", "PREMIUM");
            when(membershipTierRepository.findByMembershipGroupAndTierKey("user:shopping", "PREMIUM"))
                    .thenReturn(Optional.of(premiumTier));
            when(userMembershipRepository.findByUserIdAndMembershipGroup(USER_ID, "user:shopping"))
                    .thenReturn(Optional.of(membership));

            // when
            MembershipResponse result = membershipService.changeMembershipTier(USER_ID, request);

            // then
            assertThat(result.tierKey()).isEqualTo("PREMIUM");
            verify(auditLogRepository).save(any(AuthAuditLog.class));
        }

        @Test
        @DisplayName("should_throwException_when_tierNotFound")
        void should_throwException_when_tierNotFound() {
            // given
            ChangeMembershipRequest request = new ChangeMembershipRequest("user:shopping", "PLATINUM");
            when(membershipTierRepository.findByMembershipGroupAndTierKey("user:shopping", "PLATINUM"))
                    .thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> membershipService.changeMembershipTier(USER_ID, request))
                    .isInstanceOf(CustomBusinessException.class)
                    .satisfies(ex -> {
                        CustomBusinessException cbe = (CustomBusinessException) ex;
                        assertThat(cbe.getErrorCode()).isEqualTo(AuthErrorCode.MEMBERSHIP_TIER_NOT_FOUND);
                    });
        }
    }

    @Nested
    @DisplayName("cancelMembership")
    class CancelMembership {

        @Test
        @DisplayName("should_cancelMembership_when_active")
        void should_cancelMembership_when_active() {
            // given
            MembershipTier premiumTier = createTier("user:shopping", "PREMIUM");
            MembershipTier freeTier = createTier("user:shopping", "FREE");
            UserMembership membership = createMembership("user:shopping", premiumTier);

            when(userMembershipRepository.findByUserIdAndMembershipGroup(USER_ID, "user:shopping"))
                    .thenReturn(Optional.of(membership));
            when(membershipTierRepository.findByMembershipGroupAndTierKey("user:shopping", "FREE"))
                    .thenReturn(Optional.of(freeTier));

            // when
            membershipService.cancelMembership(USER_ID, "user:shopping");

            // then
            assertThat(membership.getStatus()).isEqualTo(MembershipStatus.CANCELLED);
            verify(auditLogRepository).save(any(AuthAuditLog.class));
        }

        @Test
        @DisplayName("should_throwException_when_membershipNotActive")
        void should_throwException_when_membershipNotActive() {
            // given
            MembershipTier tier = createTier("user:shopping", "FREE");
            UserMembership membership = createMembership("user:shopping", tier);
            membership.cancel(); // already cancelled

            when(userMembershipRepository.findByUserIdAndMembershipGroup(USER_ID, "user:shopping"))
                    .thenReturn(Optional.of(membership));

            // when & then
            assertThatThrownBy(() -> membershipService.cancelMembership(USER_ID, "user:shopping"))
                    .isInstanceOf(CustomBusinessException.class)
                    .satisfies(ex -> {
                        CustomBusinessException cbe = (CustomBusinessException) ex;
                        assertThat(cbe.getErrorCode()).isEqualTo(AuthErrorCode.MEMBERSHIP_EXPIRED);
                    });
        }
    }

    @Nested
    @DisplayName("adminChangeMembershipTier")
    class AdminChangeMembershipTier {

        @Test
        @DisplayName("should_changeTier_when_membershipExists")
        void should_changeTier_when_membershipExists() {
            // given
            MembershipTier freeTier = createTier("user:shopping", "FREE");
            MembershipTier premiumTier = createTier("user:shopping", "PREMIUM");
            UserMembership membership = createMembership("user:shopping", freeTier);

            ChangeMembershipRequest request = new ChangeMembershipRequest("user:shopping", "PREMIUM");
            when(membershipTierRepository.findByMembershipGroupAndTierKey("user:shopping", "PREMIUM"))
                    .thenReturn(Optional.of(premiumTier));
            when(userMembershipRepository.findByUserIdAndMembershipGroup(USER_ID, "user:shopping"))
                    .thenReturn(Optional.of(membership));

            // when
            MembershipResponse result = membershipService.adminChangeMembershipTier(USER_ID, request, "admin-uuid");

            // then
            assertThat(result.tierKey()).isEqualTo("PREMIUM");
        }

        @Test
        @DisplayName("should_createDefaultMembership_when_membershipNotExists")
        void should_createDefaultMembership_when_membershipNotExists() {
            // given
            MembershipTier premiumTier = createTier("user:shopping", "PREMIUM");
            ChangeMembershipRequest request = new ChangeMembershipRequest("user:shopping", "PREMIUM");

            when(membershipTierRepository.findByMembershipGroupAndTierKey("user:shopping", "PREMIUM"))
                    .thenReturn(Optional.of(premiumTier));
            when(userMembershipRepository.findByUserIdAndMembershipGroup(USER_ID, "user:shopping"))
                    .thenReturn(Optional.empty());
            when(userMembershipRepository.save(any(UserMembership.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // when
            MembershipResponse result = membershipService.adminChangeMembershipTier(USER_ID, request, "admin-uuid");

            // then
            assertThat(result.tierKey()).isEqualTo("PREMIUM");
            verify(userMembershipRepository).save(any(UserMembership.class));
        }
    }
}

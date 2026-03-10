package com.portal.universe.authservice.support.fixture;

import com.portal.universe.authservice.auth.domain.MembershipGroupConstants;
import com.portal.universe.authservice.auth.domain.MembershipTier;
import com.portal.universe.authservice.auth.domain.UserMembership;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * MembershipTier + UserMembership 테스트 데이터 빌더.
 */
public final class MembershipFixture {

    private MembershipFixture() {}

    // ========== MembershipTier ==========

    public static MembershipTier createFreeTier() {
        return tierBuilder().build();
    }

    public static MembershipTier createPremiumTier() {
        return tierBuilder()
                .tierKey("PREMIUM")
                .displayName("Premium")
                .priceMonthly(new BigDecimal("9900"))
                .priceYearly(new BigDecimal("99000"))
                .sortOrder(2)
                .build();
    }

    public static MembershipTierBuilder tierBuilder() {
        return new MembershipTierBuilder();
    }

    public static class MembershipTierBuilder {
        private Long id;
        private String membershipGroup = MembershipGroupConstants.USER_BLOG;
        private String tierKey = "FREE";
        private String displayName = "Free";
        private BigDecimal priceMonthly = BigDecimal.ZERO;
        private BigDecimal priceYearly = BigDecimal.ZERO;
        private int sortOrder = 1;

        public MembershipTierBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public MembershipTierBuilder membershipGroup(String membershipGroup) {
            this.membershipGroup = membershipGroup;
            return this;
        }

        public MembershipTierBuilder tierKey(String tierKey) {
            this.tierKey = tierKey;
            return this;
        }

        public MembershipTierBuilder displayName(String displayName) {
            this.displayName = displayName;
            return this;
        }

        public MembershipTierBuilder priceMonthly(BigDecimal priceMonthly) {
            this.priceMonthly = priceMonthly;
            return this;
        }

        public MembershipTierBuilder priceYearly(BigDecimal priceYearly) {
            this.priceYearly = priceYearly;
            return this;
        }

        public MembershipTierBuilder sortOrder(int sortOrder) {
            this.sortOrder = sortOrder;
            return this;
        }

        public MembershipTier build() {
            MembershipTier tier = MembershipTier.builder()
                    .membershipGroup(membershipGroup)
                    .tierKey(tierKey)
                    .displayName(displayName)
                    .priceMonthly(priceMonthly)
                    .priceYearly(priceYearly)
                    .sortOrder(sortOrder)
                    .build();
            if (id != null) {
                ReflectionTestUtils.setField(tier, "id", id);
            }
            return tier;
        }
    }

    // ========== UserMembership ==========

    public static UserMembership createActiveMembership(String userId, MembershipTier tier) {
        return membershipBuilder()
                .userId(userId)
                .membershipGroup(tier.getMembershipGroup())
                .tier(tier)
                .build();
    }

    public static UserMembershipBuilder membershipBuilder() {
        return new UserMembershipBuilder();
    }

    public static class UserMembershipBuilder {
        private Long id;
        private String userId = UserFixture.DEFAULT_UUID;
        private String membershipGroup = MembershipGroupConstants.USER_BLOG;
        private MembershipTier tier;
        private Instant expiresAt = Instant.now().plus(365, ChronoUnit.DAYS);
        private boolean autoRenew = true;

        public UserMembershipBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public UserMembershipBuilder userId(String userId) {
            this.userId = userId;
            return this;
        }

        public UserMembershipBuilder membershipGroup(String membershipGroup) {
            this.membershipGroup = membershipGroup;
            return this;
        }

        public UserMembershipBuilder tier(MembershipTier tier) {
            this.tier = tier;
            return this;
        }

        public UserMembershipBuilder expiresAt(Instant expiresAt) {
            this.expiresAt = expiresAt;
            return this;
        }

        public UserMembershipBuilder autoRenew(boolean autoRenew) {
            this.autoRenew = autoRenew;
            return this;
        }

        public UserMembership build() {
            MembershipTier effectiveTier = tier != null ? tier : MembershipFixture.createFreeTier();
            UserMembership membership = UserMembership.builder()
                    .userId(userId)
                    .membershipGroup(membershipGroup)
                    .tier(effectiveTier)
                    .expiresAt(expiresAt)
                    .autoRenew(autoRenew)
                    .build();
            if (id != null) {
                ReflectionTestUtils.setField(membership, "id", id);
            }
            return membership;
        }
    }
}

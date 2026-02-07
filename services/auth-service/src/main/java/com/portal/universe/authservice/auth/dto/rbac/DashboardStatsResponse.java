package com.portal.universe.authservice.auth.dto.rbac;

import java.util.List;
import java.util.Map;

public record DashboardStatsResponse(
        UserStats users,
        RoleStats roles,
        MembershipStats memberships,
        SellerStats sellers,
        List<RecentActivityItem> recentActivity
) {
    public record UserStats(long total, Map<String, Long> byStatus) {}

    public record RoleStats(int total, int systemCount, List<RoleAssignmentCount> assignments) {}

    public record RoleAssignmentCount(String roleKey, String displayName, long userCount) {}

    public record MembershipStats(List<GroupStats> groups) {}

    public record GroupStats(String group, long activeCount, List<TierCount> tiers) {}

    public record TierCount(String tierKey, String displayName, long count) {}

    public record SellerStats(long pending, long approved, long rejected) {}

    public record RecentActivityItem(
            String eventType, String targetUserId, String actorUserId,
            String details, String createdAt
    ) {}
}

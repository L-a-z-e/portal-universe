// admin-frontend/src/dto/admin.ts
export type { PageResponse } from '@portal/design-types';

export interface RoleResponse {
  id: number;
  roleKey: string;
  displayName: string;
  description: string;
  serviceScope: string | null;
  membershipGroup: string | null;
  parentRoleKey: string | null;
  system: boolean;
  active: boolean;
}

export interface RoleDetailResponse {
  id: number;
  roleKey: string;
  displayName: string;
  description: string;
  serviceScope: string | null;
  membershipGroup: string | null;
  parentRoleKey: string | null;
  system: boolean;
  active: boolean;
  createdAt: string;
  updatedAt: string;
  permissions: PermissionResponse[];
}

export interface PermissionResponse {
  id: number;
  permissionKey: string;
  service: string;
  resource: string;
  action: string;
  description: string;
  active: boolean;
}

export interface CreateRoleRequest {
  roleKey: string;
  displayName: string;
  description?: string;
  serviceScope?: string;
  membershipGroup?: string;
  parentRoleKey?: string;
}

export interface UpdateRoleRequest {
  displayName: string;
  description?: string;
}

export interface UserRole {
  id: number;
  roleKey: string;
  displayName: string;
  assignedBy: string;
  assignedAt: string;
  expiresAt: string | null;
}

export interface UserPermissions {
  userId: string;
  roles: string[];
  permissions: string[];
  memberships: Record<string, unknown>;
}

export interface MembershipTierResponse {
  id: number;
  membershipGroup: string;
  tierKey: string;
  displayName: string;
  priceMonthly: number | null;
  priceYearly: number | null;
  sortOrder: number;
}

export interface MembershipResponse {
  id: number;
  userId: string;
  membershipGroup: string;
  tierKey: string;
  tierDisplayName: string;
  status: 'ACTIVE' | 'CANCELLED' | 'EXPIRED';
  autoRenew: boolean;
  startedAt: string | null;
  expiresAt: string | null;
  createdAt: string;
}

export interface SellerApplication {
  id: number;
  userId: string;
  businessName: string;
  businessNumber: string;
  reason: string;
  status: 'PENDING' | 'APPROVED' | 'REJECTED';
  reviewedBy: string | null;
  reviewComment: string | null;
  reviewedAt: string | null;
  createdAt: string;
}

export interface AuditLog {
  id: number;
  eventType: string;
  actorUserId: string;
  targetUserId: string;
  details: string;
  ipAddress: string | null;
  createdAt: string;
}

export interface AdminUserSummary {
  uuid: string;
  email: string;
  username: string | null;
  nickname: string | null;
  profileImageUrl: string | null;
  status: 'ACTIVE' | 'DORMANT' | 'BANNED' | 'WITHDRAWAL_PENDING';
  createdAt: string;
  lastLoginAt: string | null;
}

export interface ApiResponse<T> {
  success: boolean;
  data: T;
  error?: { code: string; message: string };
}

// Dashboard Stats

export interface DashboardStats {
  users: UserStats;
  roles: RoleStats;
  memberships: MembershipStats;
  sellers: SellerStats;
  recentActivity: RecentActivityItem[];
}

export interface UserStats {
  total: number;
  byStatus: Record<string, number>;
}

export interface RoleStats {
  total: number;
  systemCount: number;
  assignments: RoleAssignmentCount[];
}

export interface RoleAssignmentCount {
  roleKey: string;
  displayName: string;
  userCount: number;
}

export interface MembershipStats {
  groups: GroupStats[];
}

export interface GroupStats {
  group: string;
  activeCount: number;
  tiers: TierCount[];
}

export interface TierCount {
  tierKey: string;
  displayName: string;
  count: number;
}

export interface SellerStats {
  pending: number;
  approved: number;
  rejected: number;
}

export interface RecentActivityItem {
  eventType: string;
  targetUserId: string;
  actorUserId: string;
  details: string;
  createdAt: string;
}

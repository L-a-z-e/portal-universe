// admin-frontend/src/api/admin.ts
import apiClient from './index';
import type {
  AdminUserSummary,
  ApiResponse,
  RoleResponse,
  RoleDetailResponse,
  PermissionResponse,
  CreateRoleRequest,
  UpdateRoleRequest,
  UserRole,
  UserPermissions,
  MembershipTierResponse,
  MembershipResponse,
  UpdateMembershipTierRequest,
  SellerApplication,
  AuditLog,
  DashboardStats,
  PageResponse,
} from '@/dto/admin';

// ==================== 경로 상수 ====================

const ADMIN_RBAC_BASE = '/api/v1/admin/rbac';
const ADMIN_MEMBERSHIP_BASE = '/api/v1/admin/memberships';
const ADMIN_SELLER_BASE = '/api/v1/admin/seller';

// === RBAC ===

export async function searchUsers(query: string, page: number, size: number): Promise<PageResponse<AdminUserSummary>> {
  const params = new URLSearchParams({ query, page: String(page), size: String(size) });
  const res = await apiClient.get<ApiResponse<PageResponse<AdminUserSummary>>>(
    `${ADMIN_RBAC_BASE}/users?${params}`,
  );
  return res.data.data;
}

export async function fetchRoles(): Promise<RoleResponse[]> {
  const res = await apiClient.get<ApiResponse<RoleResponse[]>>(`${ADMIN_RBAC_BASE}/roles`);
  return res.data.data;
}

export async function fetchRoleDetail(roleKey: string): Promise<RoleDetailResponse> {
  const res = await apiClient.get<ApiResponse<RoleDetailResponse>>(`${ADMIN_RBAC_BASE}/roles/${roleKey}`);
  return res.data.data;
}

export async function createRole(data: CreateRoleRequest): Promise<RoleResponse> {
  const res = await apiClient.post<ApiResponse<RoleResponse>>(`${ADMIN_RBAC_BASE}/roles`, data);
  return res.data.data;
}

export async function updateRole(roleKey: string, data: UpdateRoleRequest): Promise<RoleResponse> {
  const res = await apiClient.put<ApiResponse<RoleResponse>>(`${ADMIN_RBAC_BASE}/roles/${roleKey}`, data);
  return res.data.data;
}

export async function toggleRoleActive(roleKey: string, active: boolean): Promise<RoleResponse> {
  const res = await apiClient.patch<ApiResponse<RoleResponse>>(
    `${ADMIN_RBAC_BASE}/roles/${roleKey}/status?active=${active}`,
  );
  return res.data.data;
}

export async function fetchRolePermissions(roleKey: string): Promise<PermissionResponse[]> {
  const res = await apiClient.get<ApiResponse<PermissionResponse[]>>(
    `${ADMIN_RBAC_BASE}/roles/${roleKey}/permissions`,
  );
  return res.data.data;
}

export async function assignPermission(roleKey: string, permissionKey: string): Promise<void> {
  await apiClient.post(`${ADMIN_RBAC_BASE}/roles/${roleKey}/permissions?permissionKey=${permissionKey}`);
}

export async function removePermission(roleKey: string, permissionKey: string): Promise<void> {
  await apiClient.delete(`${ADMIN_RBAC_BASE}/roles/${roleKey}/permissions/${permissionKey}`);
}

export async function fetchPermissions(): Promise<PermissionResponse[]> {
  const res = await apiClient.get<ApiResponse<PermissionResponse[]>>(`${ADMIN_RBAC_BASE}/permissions`);
  return res.data.data;
}

export async function fetchUserRoles(userId: string): Promise<UserRole[]> {
  const res = await apiClient.get<ApiResponse<UserRole[]>>(`${ADMIN_RBAC_BASE}/users/${userId}/roles`);
  return res.data.data;
}

export async function fetchUserPermissions(userId: string): Promise<UserPermissions> {
  const res = await apiClient.get<ApiResponse<UserPermissions>>(`${ADMIN_RBAC_BASE}/users/${userId}/permissions`);
  return res.data.data;
}

export async function assignRole(userId: string, roleKey: string): Promise<void> {
  await apiClient.post(`${ADMIN_RBAC_BASE}/roles/assign`, { userId, roleKey });
}

export async function revokeRole(userId: string, roleKey: string): Promise<void> {
  await apiClient.delete(`${ADMIN_RBAC_BASE}/users/${userId}/roles/${roleKey}`);
}

// === Membership ===

export async function fetchMembershipGroups(): Promise<string[]> {
  const res = await apiClient.get<ApiResponse<string[]>>(`${ADMIN_MEMBERSHIP_BASE}/groups`);
  return res.data.data;
}

export async function fetchMembershipTiers(group: string): Promise<MembershipTierResponse[]> {
  const res = await apiClient.get<ApiResponse<MembershipTierResponse[]>>(`/api/v1/memberships/tiers/${group}`);
  return res.data.data;
}

export async function fetchUserMemberships(userId: string): Promise<MembershipResponse[]> {
  const res = await apiClient.get<ApiResponse<MembershipResponse[]>>(`${ADMIN_MEMBERSHIP_BASE}/users/${userId}`);
  return res.data.data;
}

export async function changeUserMembership(userId: string, membershipGroup: string, tierKey: string): Promise<void> {
  await apiClient.put(`${ADMIN_MEMBERSHIP_BASE}/users/${userId}`, { membershipGroup, tierKey });
}

export async function updateMembershipTier(
  tierId: number,
  data: UpdateMembershipTierRequest,
): Promise<MembershipTierResponse> {
  const res = await apiClient.put<ApiResponse<MembershipTierResponse>>(
    `${ADMIN_MEMBERSHIP_BASE}/tiers/${tierId}`, data,
  );
  return res.data.data;
}

// === Seller ===

export async function fetchPendingSellerApplications(size = 50): Promise<PageResponse<SellerApplication>> {
  const res = await apiClient.get<ApiResponse<PageResponse<SellerApplication>>>(`${ADMIN_SELLER_BASE}/applications/pending?size=${size}`);
  return res.data.data;
}

export async function reviewSellerApplication(id: number, approved: boolean, reviewComment: string): Promise<void> {
  await apiClient.post(`${ADMIN_SELLER_BASE}/applications/${id}/review`, { approved, reviewComment });
}

// === Dashboard ===

export async function fetchDashboardStats(): Promise<DashboardStats> {
  const res = await apiClient.get<ApiResponse<DashboardStats>>(`${ADMIN_RBAC_BASE}/dashboard`);
  return res.data.data;
}

// === Audit ===

export async function fetchAuditLogs(page: number, size: number, userId?: string): Promise<PageResponse<AuditLog>> {
  const params = new URLSearchParams({ page: String(page), size: String(size) });
  const url = userId
    ? `${ADMIN_RBAC_BASE}/users/${userId}/audit?${params}`
    : `${ADMIN_RBAC_BASE}/audit?${params}`;
  const res = await apiClient.get<ApiResponse<PageResponse<AuditLog>>>(url);
  return res.data.data;
}

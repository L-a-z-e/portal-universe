// blog-frontend/src/stores/followStore.ts

import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import * as followApi from '@/api/follow';
import type { FollowListResponse } from '@/dto/follow';

export const useFollowStore = defineStore('follow', () => {
  // ==================== State ====================
  const followingIds = ref<string[]>([]);
  const followingIdsLoaded = ref(false);
  const loading = ref(false);
  const error = ref<Error | null>(null);

  // 팔로워/팔로잉 목록 캐시
  const followersCache = ref<Map<string, FollowListResponse>>(new Map());
  const followingsCache = ref<Map<string, FollowListResponse>>(new Map());

  // ==================== Getters ====================
  const isFollowing = computed(() => (uuid: string) => {
    return followingIds.value.includes(uuid);
  });

  const followingCount = computed(() => followingIds.value.length);

  // ==================== Actions ====================

  /**
   * 내 팔로잉 ID 목록 로드
   */
  async function loadFollowingIds() {
    if (followingIdsLoaded.value) return;

    loading.value = true;
    error.value = null;

    try {
      const response = await followApi.getMyFollowingIds();
      followingIds.value = response.followingIds;
      followingIdsLoaded.value = true;
    } catch (e) {
      error.value = e as Error;
      console.error('Failed to load following ids:', e);
    } finally {
      loading.value = false;
    }
  }

  /**
   * 팔로우 토글
   */
  async function toggleFollow(username: string, targetUuid: string) {
    loading.value = true;
    error.value = null;

    try {
      const response = await followApi.toggleFollow(username);

      // 로컬 상태 업데이트
      if (response.following) {
        if (!followingIds.value.includes(targetUuid)) {
          followingIds.value.push(targetUuid);
        }
      } else {
        followingIds.value = followingIds.value.filter(id => id !== targetUuid);
      }

      // 캐시 무효화
      followersCache.value.delete(username);
      followingsCache.value.delete(username);

      return response;
    } catch (e) {
      error.value = e as Error;
      throw e;
    } finally {
      loading.value = false;
    }
  }

  /**
   * 팔로워 목록 조회
   */
  async function getFollowers(username: string, page: number = 0, size: number = 20) {
    const cacheKey = `${username}-${page}-${size}`;
    if (followersCache.value.has(cacheKey)) {
      return followersCache.value.get(cacheKey)!;
    }

    loading.value = true;
    error.value = null;

    try {
      const response = await followApi.getFollowers(username, page, size);
      followersCache.value.set(cacheKey, response);
      return response;
    } catch (e) {
      error.value = e as Error;
      throw e;
    } finally {
      loading.value = false;
    }
  }

  /**
   * 팔로잉 목록 조회
   */
  async function getFollowings(username: string, page: number = 0, size: number = 20) {
    const cacheKey = `${username}-${page}-${size}`;
    if (followingsCache.value.has(cacheKey)) {
      return followingsCache.value.get(cacheKey)!;
    }

    loading.value = true;
    error.value = null;

    try {
      const response = await followApi.getFollowings(username, page, size);
      followingsCache.value.set(cacheKey, response);
      return response;
    } catch (e) {
      error.value = e as Error;
      throw e;
    } finally {
      loading.value = false;
    }
  }

  /**
   * 팔로우 상태 확인
   */
  async function checkFollowStatus(username: string) {
    try {
      const response = await followApi.getFollowStatus(username);
      return response.isFollowing;
    } catch (e) {
      console.error('Failed to check follow status:', e);
      return false;
    }
  }

  /**
   * 캐시 초기화
   */
  function clearCache() {
    followersCache.value.clear();
    followingsCache.value.clear();
  }

  /**
   * 상태 리셋 (로그아웃 시)
   */
  function reset() {
    followingIds.value = [];
    followingIdsLoaded.value = false;
    loading.value = false;
    error.value = null;
    clearCache();
  }

  return {
    // State
    followingIds,
    followingIdsLoaded,
    loading,
    error,

    // Getters
    isFollowing,
    followingCount,

    // Actions
    loadFollowingIds,
    toggleFollow,
    getFollowers,
    getFollowings,
    checkFollowStatus,
    clearCache,
    reset,
  };
});

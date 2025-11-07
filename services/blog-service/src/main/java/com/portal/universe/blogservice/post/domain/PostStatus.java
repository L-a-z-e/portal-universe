package com.portal.universe.blogservice.post.domain;

/**
 * 게시물 상태 - PRD Phase 1: 기본 상태 관리
 */
public enum PostStatus {
    DRAFT,      // 초안
    PUBLISHED,  // 발행됨
    ARCHIVED    // 보관됨 (PRD Phase 2에서 활용)
}
package com.portal.universe.blogservice.post.domain;

public enum PostSortType {
    CREATED_AT,     // 생성일순
    PUBLISHED_AT,   // 발행일순 (기본)
    VIEW_COUNT,     // 조회수순 (인기순)
    LIKE_COUNT,     // 좋아요순 (Phase 2)
    TITLE          // 제목순 (검색 결과용)
}
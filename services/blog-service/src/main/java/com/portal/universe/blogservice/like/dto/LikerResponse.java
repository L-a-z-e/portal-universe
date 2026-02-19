package com.portal.universe.blogservice.like.dto;

import com.portal.universe.blogservice.like.domain.Like;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * 좋아요한 사용자 정보 응답 DTO
 * @param userId 사용자 ID
 * @param userName 사용자 Username (핸들)
 * @param nickname 사용자 닉네임 (표시용)
 * @param likedAt 좋아요 생성 시간
 */
@Builder
public record LikerResponse(
        String userId,
        String userName,
        String nickname,
        LocalDateTime likedAt
) {
    public static LikerResponse from(Like like) {
        return LikerResponse.builder()
                .userId(like.getUserId())
                .userName(like.getUserName())
                .nickname(like.getNickname())
                .likedAt(like.getCreatedAt())
                .build();
    }
}

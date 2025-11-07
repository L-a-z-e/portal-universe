package com.portal.universe.blogservice.post.dto;

import com.portal.universe.blogservice.post.domain.PostStatus;

public record PostStatusChangeRequest(
        PostStatus newStatus
) {}
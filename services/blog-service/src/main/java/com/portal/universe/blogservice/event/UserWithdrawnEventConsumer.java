package com.portal.universe.blogservice.event;

import com.portal.universe.blogservice.comment.domain.Comment;
import com.portal.universe.blogservice.comment.repository.CommentRepository;
import com.portal.universe.blogservice.like.repository.LikeRepository;
import com.portal.universe.blogservice.post.domain.Post;
import com.portal.universe.blogservice.post.repository.PostRepository;
import com.portal.universe.event.auth.AuthTopics;
import com.portal.universe.event.auth.UserWithdrawnEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 회원 탈퇴 이벤트를 수신하여 블로그 데이터를 정리한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserWithdrawnEventConsumer {

    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final LikeRepository likeRepository;

    @KafkaListener(topics = AuthTopics.USER_WITHDRAWN, groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "avroKafkaListenerContainerFactory")
    public void handleUserWithdrawn(UserWithdrawnEvent event) {
        String userId = event.getUserId().toString();
        log.info("Received UserWithdrawnEvent: userId={}", userId);

        // 1. Post soft delete
        List<Post> posts = postRepository.findByAuthorId(userId);
        if (!posts.isEmpty()) {
            posts.forEach(Post::markDeleted);
            postRepository.saveAll(posts);
            log.info("Soft deleted {} posts for userId={}", posts.size(), userId);
        }

        // 2. Comment soft delete
        List<Comment> comments = commentRepository.findByAuthorIdAndIsDeletedFalseOrderByCreatedAtDesc(userId);
        if (!comments.isEmpty()) {
            comments.forEach(Comment::delete);
            commentRepository.saveAll(comments);
            log.info("Soft deleted {} comments for userId={}", comments.size(), userId);
        }

        // 3. Like hard delete
        likeRepository.deleteByUserId(userId);
        log.info("Deleted likes for userId={}", userId);

        log.info("Completed user withdrawal cleanup for userId={}", userId);
    }
}

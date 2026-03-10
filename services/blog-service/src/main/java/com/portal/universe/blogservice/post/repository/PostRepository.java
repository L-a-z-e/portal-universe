package com.portal.universe.blogservice.post.repository;

import com.portal.universe.blogservice.post.domain.Post;
import com.portal.universe.blogservice.post.domain.PostStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface PostRepository extends MongoRepository<Post, String>, PostRepositoryCustom {

    List<Post> findByProductId(String productId);

    Page<Post> findByStatusOrderByPublishedAtDesc(PostStatus status, Pageable pageable);

    Page<Post> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<Post> findByAuthorIdOrderByCreatedAtDesc(String authorId, Pageable pageable);

    Page<Post> findByAuthorIdAndStatusOrderByCreatedAtDesc(String authorId, PostStatus status, Pageable pageable);

    Page<Post> findByCategoryAndStatusOrderByPublishedAtDesc(String category, PostStatus status, Pageable pageable);

    Page<Post> findByTagsInAndStatusOrderByPublishedAtDesc(List<String> tags, PostStatus status, Pageable pageable);

    @Query("{ $text: { $search: ?0 }, status: ?1 }")
    Page<Post> findByTextSearchAndStatus(String searchText, PostStatus status, Pageable pageable);

    @Query("{ _id: ?0, $or: [ { status: 'PUBLISHED' }, { authorId: ?1 } ] }")
    Optional<Post> findByIdAndViewableBy(String postId, String userId);

    Page<Post> findByStatusOrderByViewCountDescPublishedAtDesc(PostStatus status, Pageable pageable);

    Page<Post> findByStatusAndPublishedAtAfterOrderByPublishedAtDesc(
            PostStatus status, Instant since, Pageable pageable);

    @Query("{ $or: [ { category: ?0 }, { tags: { $in: ?1 } } ], status: ?2, _id: { $ne: ?3 } }")
    List<Post> findRelatedPosts(String category, List<String> tags, PostStatus status, String excludePostId);

    long countByAuthorIdAndStatus(String authorId, PostStatus status);

    long countByCategoryAndStatus(String category, PostStatus status);

    long countByStatus(PostStatus status);

    @Query(value = "{ status: ?0 }", fields = "{ category: 1 }")
    List<String> findDistinctCategoriesByStatus(PostStatus status);

    List<Post> findByAuthorId(String authorId);

    Page<Post> findByAuthorIdInAndStatusOrderByPublishedAtDesc(
            List<String> authorIds, PostStatus status, Pageable pageable);

    Optional<Post> findFirstByStatusAndPublishedAtLessThanOrderByPublishedAtDesc(
            PostStatus status, Instant publishedAt);

    Optional<Post> findFirstByStatusAndPublishedAtGreaterThanOrderByPublishedAtAsc(
            PostStatus status, Instant publishedAt);

    Optional<Post> findFirstByAuthorIdAndStatusAndPublishedAtLessThanOrderByPublishedAtDesc(
            String authorId, PostStatus status, Instant publishedAt);

    Optional<Post> findFirstByAuthorIdAndStatusAndPublishedAtGreaterThanOrderByPublishedAtAsc(
            String authorId, PostStatus status, Instant publishedAt);

    Optional<Post> findFirstByCategoryAndStatusAndPublishedAtLessThanOrderByPublishedAtDesc(
            String category, PostStatus status, Instant publishedAt);

    Optional<Post> findFirstByCategoryAndStatusAndPublishedAtGreaterThanOrderByPublishedAtAsc(
            String category, PostStatus status, Instant publishedAt);
}

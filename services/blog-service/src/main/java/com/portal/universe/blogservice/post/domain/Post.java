package com.portal.universe.blogservice.post.domain;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.index.TextIndexed;
import org.springframework.data.mongodb.core.mapping.Document;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

/**
 * 블로그 게시물 Entity - 기본 블로그 기능 중심으로 개선
 * 기존 productId는 선택적 기능으로 유지 (하위 호환성)
 */
@Document(collection = "posts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Post {

    @Id
    private String id;

    /**
     * 게시물 제목 (SEO 최적화를 위한 텍스트 인덱스)
     */
    @TextIndexed(weight = 2.0f)
    @NotBlank(message = "제목은 필수입니다")
    @Size(max = 200, message = "제목은 200자를 초과할 수 없습니다")
    private String title;

    /**
     * 게시물 내용 (검색 최적화)
     */
    @TextIndexed
    @NotBlank(message = "내용은 필수입니다")
    private String content;

    /**
     * 게시물 요약 (목록 표시용, 자동 생성 또는 수동 입력)
     */
    @Size(max = 500, message = "요약은 500자를 초과할 수 없습니다")
    private String summary;

    /**
     * 작성자 ID (JWT에서 추출)
     */
    @Indexed
    @NotBlank(message = "작성자는 필수입니다")
    private String authorId;

    /**
     * 작성자 이름 (표시용)
     */
    private String authorName;

    /**
     * 게시물 상태 - PRD Phase 1: 기본 발행 관리
     */
    @Indexed
    private PostStatus status = PostStatus.DRAFT;

    /**
     * 태그 목록 - PRD: 분류 및 검색 기능
     */
    @Indexed
    private Set<String> tags = new HashSet<>();

    /**
     * 카테고리 - PRD: 콘텐츠 허브 구조
     */
    @Indexed
    private String category;

    /**
     * 조회수 - PRD: 참여도 측정
     */
    private Long viewCount = 0L;

    /**
     * 좋아요 수 - PRD Phase 2: 참여 기능
     */
    private Long likeCount = 0L;

    /**
     * 댓글 수 - PRD Phase 3: 트렌딩 점수 계산용
     */
    private Long commentCount = 0L;

    /**
     * 발행일시 (공개 게시물)
     */
    @Indexed
    private LocalDateTime publishedAt;

    /**
     * SEO 메타 정보 - PRD Phase 1: SEO 최적화
     */
    @Size(max = 160, message = "메타 설명은 160자를 초과할 수 없습니다")
    private String metaDescription;

    /**
     * 썸네일 이미지 URL - PRD Phase 2: 멀티미디어
     */
    private String thumbnailUrl;

    /**
     * 첨부 이미지 URL 목록
     * - Toast UI Editor 또는 별도 이미지 업로드로 추가된 이미지들
     * - S3 URL 목록
     * - 본문 내 이미지와 별도로 관리 가능
     */
    private List<String> images = new ArrayList<>();

    /**
     * 연관 상품 ID (선택적 기능으로 유지)
     * PRD Phase 3: 수익화 기능에서 활용 가능
     */
    private String productId;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @Builder
    public Post(String title, String content, String summary, String authorId,
                String authorName, PostStatus status, Set<String> tags, String category,
                String metaDescription, String thumbnailUrl, List<String> images, String productId) {
        this.title = title;
        this.content = content;
        this.summary = summary != null ? summary : generateSummary(content);
        this.authorId = authorId;
        this.authorName = authorName;
        this.status = status != null ? status : PostStatus.DRAFT;
        this.tags = tags != null ? new HashSet<>(tags) : new HashSet<>();
        this.category = category;
        this.metaDescription = metaDescription != null ? metaDescription : generateMetaDescription(content);
        this.thumbnailUrl = thumbnailUrl;
        this.images = images != null ? new ArrayList<>(images) : new ArrayList<>();
        this.productId = productId; // 선택적 유지
    }

    /**
     * 기본 게시물 수정
     */
    public void update (String title, String content, String summary, Set<String> tags,
                        String category, String metaDescription, String thumbnailUrl, List<String> images) {
        this.title = title;
        this.content = content;
        this.summary = summary != null ? summary : generateSummary(content);
        this.tags = tags != null ? new HashSet<>(tags) : new HashSet<>();
        this.category = category;
        this.metaDescription = metaDescription != null ? metaDescription : generateMetaDescription(content);
        this.thumbnailUrl = thumbnailUrl;
        this.images = images != null ? new ArrayList<>(images) : new ArrayList<>();
    }

    /**
     * 게시물 발행 - PRD Phase 1: 기본 상태 관리
     */
    public void publish() {
        this.status = PostStatus.PUBLISHED;
        this.publishedAt = LocalDateTime.now();
    }

    /**
     * 초안으로 변경
     */
    public void unpublish() {
        this.status = PostStatus.DRAFT;
        this.publishedAt = null;
    }

    /**
     * 조회수 증가 - PRD: 참여도 측정
     */
    public void incrementViewCount() {
        this.viewCount++;
    }

    /**
     * 좋아요 수 증가 - PRD Phase 2
     */
    public void incrementLikeCount() {
        this.likeCount++;
    }

    /**
     * 좋아요 수 감소
     */
    public void decrementLikeCount() {
        if (this.likeCount > 0) {
            this.likeCount--;
        }
    }

    /**
     * 댓글 수 증가 - PRD Phase 3
     */
    public void incrementCommentCount() {
        this.commentCount++;
    }

    /**
     * 댓글 수 감소
     */
    public void decrementCommentCount() {
        if (this.commentCount > 0) {
            this.commentCount--;
        }
    }

    /**
     * 이미지 추가
     */
    public void addImage(String imageUrl) {
        if (imageUrl != null && !imageUrl.trim().isEmpty()) {
            this.images.add(imageUrl);
        }
    }

    /**
     * 이미지 제거
     */
    public void removeImage(String imageUrl) {
        this.images.remove(imageUrl);
    }

    /**
     * 모든 이미지 제거
     */
    public void clearImages() {
        this.images.clear();
    }

    /**
     * 썸네일이 없을 경우 첫 번째 이미지를 썸네일로 자동 설정
     */
    public void setDefaultThumbnailIfNeeded() {
        if ((this.thumbnailUrl == null || this.thumbnailUrl.isEmpty())
                && !this.images.isEmpty()) {
            this.thumbnailUrl = this.images.get(0);
        }
    }

    /**
     * 내용에서 요약 자동 생성 (PRD: AI 보조 기능 대비)
     */
    private String generateSummary(String content) {
        return truncateContent(content, 200);
    }

    private String generateMetaDescription(String content) {
        return truncateContent(content, 160);
    }

    private String truncateContent(String content, int maxLength) {
        if (content == null || content.isEmpty()) return "";
        String clean = content.replaceAll("<[^>]*>", "");
        return clean.length() > maxLength ? clean.substring(0, maxLength) + "..." : clean;
    }

    /**
     * 발행 상태 확인
     */
    public boolean isPublished() {
        return PostStatus.PUBLISHED.equals(this.status);
    }

    /**
     * 조회 가능 여부 (발행된 글 또는 작성자 본인)
     */
    public boolean isViewableBy(String userId) {
        return isPublished() || this.authorId.equals(userId);
    }
}
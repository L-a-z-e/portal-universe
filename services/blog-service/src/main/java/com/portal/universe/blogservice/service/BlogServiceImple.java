package com.portal.universe.blogservice.service;

import com.portal.universe.blogservice.domain.Post;
import com.portal.universe.blogservice.dto.PostCreateRequest;
import com.portal.universe.blogservice.dto.PostResponse;
import com.portal.universe.blogservice.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BlogServiceImple implements BlogService{

    private final PostRepository postRepository;

    @Override
    public PostResponse createPost(PostCreateRequest request, String authorId) {
        // 1. DTO => Document(Entity) 객체로 변환
        Post newPost = Post.builder()
                .title(request.title())
                .content(request.content())
                .authorId(authorId)
                .build();

        // 2. Repository를 통해 DB에 저장
        Post savedPost = postRepository.save(newPost);

        // 3. 저장된 Document 객체를 다시 응답용 DTO로 변환하여 반환
        return convertToResponse(savedPost);
    }

    @Override
    public List<PostResponse> getAllPosts() {
        List<Post> posts = postRepository.findAll();

        return posts.stream()
                .map(this::convertToResponse)
                .toList();
    }

    private PostResponse convertToResponse(Post post) {
        return new PostResponse(
                post.getId(),
                post.getTitle(),
                post.getContent(),
                post.getAuthorId(),
                post.getCreatedAt(),
                post.getUpdatedAt()
        );
    }

    @Override
    public PostResponse getPostById(String postId) {
        Post post = postRepository.findById(postId).orElseThrow(() -> new RuntimeException("Post not found with id: " + postId));

        return convertToResponse(post);
    }
}

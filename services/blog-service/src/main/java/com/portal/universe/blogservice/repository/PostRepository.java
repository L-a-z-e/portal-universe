package com.portal.universe.blogservice.repository;

import com.portal.universe.blogservice.domain.Post;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface PostRepository extends MongoRepository<Post,String> {
    List<Post> findByProductId(String productId);
}

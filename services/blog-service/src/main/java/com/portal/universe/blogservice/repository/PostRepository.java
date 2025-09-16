package com.portal.universe.blogservice.repository;

import com.portal.universe.blogservice.domain.Post;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface PostRepository extends MongoRepository<Post,String> {

}

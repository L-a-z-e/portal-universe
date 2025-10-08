package com.portal.universe.blogservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@EnableMongoAuditing
@ComponentScan(basePackages = "com.portal.universe")
public class BlogServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(BlogServiceApplication.class, args);
    }

}

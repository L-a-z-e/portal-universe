package com.portal.universe.configservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.EnableConfigServer;

import java.nio.file.Paths;

@SpringBootApplication
@EnableConfigServer
public class ConfigServiceApplication {

    public static void main(String[] args) {
        System.out.println("### Current Working Directory: " + Paths.get("").toAbsolutePath().toString());
        SpringApplication.run(ConfigServiceApplication.class, args);
    }

}

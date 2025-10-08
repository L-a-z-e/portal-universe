package com.portal.universe.shoppingservice;

import com.portal.universe.shoppingservice.config.FeignClientConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@EnableFeignClients(defaultConfiguration = FeignClientConfig.class)
@ComponentScan(basePackages = { "com.portal.universe.shoppingservice", "com.portal.universe.commonlibrary" })
public class ShoppingServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(ShoppingServiceApplication.class, args);
    }
}

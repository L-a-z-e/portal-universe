package com.portal.universe.shoppingsellerservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {
        "com.portal.universe.shoppingsellerservice",
        "com.portal.universe.commonlibrary"
})
@EnableFeignClients
@EnableScheduling
public class ShoppingSellerServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShoppingSellerServiceApplication.class, args);
    }
}

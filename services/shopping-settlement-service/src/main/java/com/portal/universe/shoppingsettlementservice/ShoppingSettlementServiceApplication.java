package com.portal.universe.shoppingsettlementservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {
        "com.portal.universe.shoppingsettlementservice",
        "com.portal.universe.commonlibrary"
})
@EnableScheduling
public class ShoppingSettlementServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShoppingSettlementServiceApplication.class, args);
    }
}

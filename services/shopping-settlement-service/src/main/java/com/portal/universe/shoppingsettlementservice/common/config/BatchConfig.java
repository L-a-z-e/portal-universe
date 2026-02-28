package com.portal.universe.shoppingsettlementservice.common.config;

import com.portal.universe.shoppingsettlementservice.batch.SettlementBatchProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(SettlementBatchProperties.class)
public class BatchConfig {
}

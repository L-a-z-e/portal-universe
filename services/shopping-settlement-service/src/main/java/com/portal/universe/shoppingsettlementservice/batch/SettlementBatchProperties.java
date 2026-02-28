package com.portal.universe.shoppingsettlementservice.batch;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "settlement.batch")
public class SettlementBatchProperties {

    private int chunkSize = 100;
    private int gridSize = 4;
    private int retryLimit = 3;
    private int skipLimit = 10;
    private int corePoolSize = 4;
    private boolean scheduledEnabled = false;
}

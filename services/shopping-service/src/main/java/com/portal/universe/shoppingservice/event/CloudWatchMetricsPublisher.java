package com.portal.universe.shoppingservice.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.cloudwatch.model.Dimension;
import software.amazon.awssdk.services.cloudwatch.model.MetricDatum;
import software.amazon.awssdk.services.cloudwatch.model.PutMetricDataRequest;
import software.amazon.awssdk.services.cloudwatch.model.StandardUnit;

import java.math.BigDecimal;
import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class CloudWatchMetricsPublisher {

    private static final String NAMESPACE = "PortalUniverse/Shopping";

    private final CloudWatchClient cloudWatchClient;

    @Async
    public void publishOrderSuccess(String orderNumber, BigDecimal totalAmount) {
        try {
            Instant now = Instant.now();

            MetricDatum countMetric = MetricDatum.builder()
                    .metricName("OrderSagaSuccess")
                    .value(1.0)
                    .unit(StandardUnit.COUNT)
                    .timestamp(now)
                    .dimensions(serviceDimension())
                    .build();

            MetricDatum amountMetric = MetricDatum.builder()
                    .metricName("OrderTotalAmount")
                    .value(totalAmount.doubleValue())
                    .unit(StandardUnit.NONE)
                    .timestamp(now)
                    .dimensions(serviceDimension())
                    .build();

            cloudWatchClient.putMetricData(PutMetricDataRequest.builder()
                    .namespace(NAMESPACE)
                    .metricData(countMetric, amountMetric)
                    .build());

            log.debug("CloudWatch metrics published: OrderSagaSuccess, order={}, amount={}",
                    orderNumber, totalAmount);
        } catch (Exception e) {
            log.warn("CloudWatch metrics publish failed (non-blocking): order={}, error={}",
                    orderNumber, e.getMessage());
        }
    }

    @Async
    public void publishOrderFailure(String orderNumber, String failedStep) {
        try {
            MetricDatum failureMetric = MetricDatum.builder()
                    .metricName("OrderSagaFailure")
                    .value(1.0)
                    .unit(StandardUnit.COUNT)
                    .timestamp(Instant.now())
                    .dimensions(
                            serviceDimension(),
                            Dimension.builder()
                                    .name("FailedStep")
                                    .value(failedStep)
                                    .build()
                    )
                    .build();

            cloudWatchClient.putMetricData(PutMetricDataRequest.builder()
                    .namespace(NAMESPACE)
                    .metricData(failureMetric)
                    .build());

            log.debug("CloudWatch metrics published: OrderSagaFailure, order={}, step={}",
                    orderNumber, failedStep);
        } catch (Exception e) {
            log.warn("CloudWatch metrics publish failed (non-blocking): order={}, error={}",
                    orderNumber, e.getMessage());
        }
    }

    @Async
    public void publishCompensation(String orderNumber, int attemptCount) {
        try {
            MetricDatum compensationMetric = MetricDatum.builder()
                    .metricName("OrderSagaCompensation")
                    .value(1.0)
                    .unit(StandardUnit.COUNT)
                    .timestamp(Instant.now())
                    .dimensions(serviceDimension())
                    .build();

            cloudWatchClient.putMetricData(PutMetricDataRequest.builder()
                    .namespace(NAMESPACE)
                    .metricData(compensationMetric)
                    .build());

            log.debug("CloudWatch metrics published: OrderSagaCompensation, order={}, attempt={}",
                    orderNumber, attemptCount);
        } catch (Exception e) {
            log.warn("CloudWatch metrics publish failed (non-blocking): order={}, error={}",
                    orderNumber, e.getMessage());
        }
    }

    private Dimension serviceDimension() {
        return Dimension.builder()
                .name("Service")
                .value("shopping-service")
                .build();
    }
}

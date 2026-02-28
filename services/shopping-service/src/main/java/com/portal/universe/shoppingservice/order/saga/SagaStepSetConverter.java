package com.portal.universe.shoppingservice.order.saga;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Set&lt;SagaStep&gt; ↔ DB VARCHAR 자동 변환기입니다.
 * DB에는 "RESERVE_INVENTORY,PROCESS_PAYMENT" 형태로 저장됩니다.
 */
@Converter
public class SagaStepSetConverter implements AttributeConverter<Set<SagaStep>, String> {

    @Override
    public String convertToDatabaseColumn(Set<SagaStep> steps) {
        if (steps == null || steps.isEmpty()) {
            return "";
        }
        return steps.stream()
                .map(SagaStep::name)
                .collect(Collectors.joining(","));
    }

    @Override
    public Set<SagaStep> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return EnumSet.noneOf(SagaStep.class);
        }
        return Arrays.stream(dbData.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(SagaStep::valueOf)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(SagaStep.class)));
    }
}

package com.portal.universe.authservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Kafka를 통해 발행될 사용자 등록 이벤트 DTO(Data Transfer Object)입니다.
 * @deprecated common-library의 {@link com.portal.universe.common.event.UserSignedUpEvent}로 대체되었습니다. 현재 사용되지 않습니다.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Deprecated
public class UserRegisteredEvent {
    private String email;
    private String username;
}
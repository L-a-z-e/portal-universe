package com.portal.universe.authservice.auth.dto;

import com.portal.universe.authservice.password.config.PasswordPolicyProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * 비밀번호 정책 응답 DTO입니다.
 * 프론트엔드에서 비밀번호 요구사항을 사용자에게 안내할 때 사용합니다.
 */
public record PasswordPolicyResponse(
        int minLength,
        int maxLength,
        List<String> requirements
) {
    public static PasswordPolicyResponse from(PasswordPolicyProperties props) {
        List<String> requirements = new ArrayList<>();

        requirements.add(props.getMinLength() + "자 이상 " + props.getMaxLength() + "자 이하");

        if (props.isRequireUppercase()) {
            requirements.add("영문 대문자 1자 이상 포함");
        }
        if (props.isRequireLowercase()) {
            requirements.add("영문 소문자 1자 이상 포함");
        }
        if (props.isRequireDigit()) {
            requirements.add("숫자 1자 이상 포함");
        }
        if (props.isRequireSpecialChar()) {
            requirements.add("특수문자 1자 이상 포함 (" + props.getSpecialChars() + ")");
        }
        if (props.isPreventSequential()) {
            requirements.add("연속된 문자/숫자 사용 불가 (예: abc, 123)");
        }
        if (props.isPreventUserInfo()) {
            requirements.add("이메일, 사용자명 등 개인정보 포함 불가");
        }
        if (props.getHistoryCount() > 0) {
            requirements.add("최근 " + props.getHistoryCount() + "개 비밀번호 재사용 불가");
        }

        return new PasswordPolicyResponse(props.getMinLength(), props.getMaxLength(), requirements);
    }
}

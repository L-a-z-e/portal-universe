package com.portal.universe.commonlibrary.security.sql;

import java.util.regex.Pattern;

/**
 * SQL Injection 방어를 위한 유틸리티 클래스입니다.
 * JPA/MyBatis가 기본적으로 Prepared Statement를 통해 방어하지만,
 * 동적 쿼리나 정렬 필드 같은 특수 케이스에 추가 검증 레이어를 제공합니다.
 */
public class SqlInjectionUtils {

    /**
     * SQL Injection 위험 패턴을 감지하는 정규식 목록
     */
    private static final Pattern[] SQL_INJECTION_PATTERNS = {
            // SQL 주석
            Pattern.compile("--", Pattern.CASE_INSENSITIVE),
            Pattern.compile("/\\*", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\*/", Pattern.CASE_INSENSITIVE),
            // Union 기반 공격
            Pattern.compile("\\bunion\\b.*\\bselect\\b", Pattern.CASE_INSENSITIVE),
            // 세미콜론을 통한 다중 쿼리
            Pattern.compile(";\\s*(drop|delete|update|insert|create|alter)", Pattern.CASE_INSENSITIVE),
            // 위험한 SQL 키워드 조합
            Pattern.compile("\\b(exec|execute)\\b\\s*\\(", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bxp_cmdshell\\b", Pattern.CASE_INSENSITIVE),
            // OR 1=1, AND 1=1 같은 항상 참인 조건
            Pattern.compile("(\\bor\\b|\\band\\b)\\s+['\"]?\\d+['\"]?\\s*=\\s*['\"]?\\d+['\"]?", Pattern.CASE_INSENSITIVE),
            // 문자열 연결을 통한 우회
            Pattern.compile("\\bchar\\s*\\(", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bconcat\\s*\\(", Pattern.CASE_INSENSITIVE),
            // 시간 기반 Blind SQL Injection
            Pattern.compile("\\b(sleep|benchmark|waitfor)\\b\\s*\\(", Pattern.CASE_INSENSITIVE),
            // Information Schema 접근
            Pattern.compile("\\binformation_schema\\b", Pattern.CASE_INSENSITIVE),
            // SQL 함수를 이용한 공격
            Pattern.compile("\\bload_file\\s*\\(", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\binto\\s+outfile\\b", Pattern.CASE_INSENSITIVE)
    };

    /**
     * 정렬 필드에 사용 가능한 안전한 문자만 허용 (알파벳, 숫자, 언더스코어, 점)
     */
    private static final Pattern SAFE_FIELD_PATTERN = Pattern.compile("^[a-zA-Z0-9_.]+$");

    private SqlInjectionUtils() {
        throw new AssertionError("Utility class should not be instantiated");
    }

    /**
     * 입력값에 SQL Injection 공격 패턴이 포함되어 있는지 검사합니다.
     *
     * @param input 검사할 문자열
     * @return SQL Injection 패턴이 감지되면 true, 아니면 false
     */
    public static boolean containsSqlInjection(String input) {
        if (input == null || input.isEmpty()) {
            return false;
        }

        String normalizedInput = normalize(input);

        for (Pattern pattern : SQL_INJECTION_PATTERNS) {
            if (pattern.matcher(normalizedInput).find()) {
                return true;
            }
        }

        return false;
    }

    /**
     * 입력값이 안전한지 검사합니다.
     * SQL Injection 패턴이 없으면 true를 반환합니다.
     *
     * @param input 검사할 문자열
     * @return 안전하면 true, 위험하면 false
     */
    public static boolean isSafe(String input) {
        return !containsSqlInjection(input);
    }

    /**
     * 입력값을 정규화합니다.
     * 여러 개의 공백을 하나로 합치고, 앞뒤 공백을 제거합니다.
     *
     * @param input 원본 문자열
     * @return 정규화된 문자열
     */
    public static String normalize(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        // 여러 공백을 하나로
        String normalized = input.replaceAll("\\s+", " ");

        // 앞뒤 공백 제거
        return normalized.trim();
    }

    /**
     * 정렬 필드명(sort field)이 안전한지 검증합니다.
     * ORDER BY 절에 동적으로 필드명을 넣을 때 사용합니다.
     *
     * @param fieldName 필드명
     * @return 안전한 형식이면 true, 아니면 false
     */
    public static boolean isSafeSortField(String fieldName) {
        if (fieldName == null || fieldName.isEmpty()) {
            return false;
        }

        // 알파벳, 숫자, 언더스코어, 점만 허용
        return SAFE_FIELD_PATTERN.matcher(fieldName).matches();
    }

    /**
     * 정렬 방향(sort direction)이 안전한지 검증합니다.
     * ORDER BY 절에 동적으로 방향을 넣을 때 사용합니다.
     *
     * @param direction 정렬 방향 ("ASC" 또는 "DESC")
     * @return ASC 또는 DESC이면 true, 아니면 false
     */
    public static boolean isSafeSortDirection(String direction) {
        if (direction == null || direction.isEmpty()) {
            return false;
        }

        String normalized = direction.trim().toUpperCase();
        return "ASC".equals(normalized) || "DESC".equals(normalized);
    }
}

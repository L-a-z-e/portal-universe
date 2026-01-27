package com.portal.universe.authservice.auth.service;

/**
 * 로그인 시도를 추적하고 계정 잠금을 관리하는 서비스 인터페이스입니다.
 * Redis를 사용하여 IP 또는 IP:username 조합 기반으로 실패 횟수를 추적합니다.
 */
public interface LoginAttemptService {

    /**
     * 로그인 실패를 기록합니다.
     * 실패 횟수가 임계값을 초과하면 자동으로 잠금 처리됩니다.
     *
     * @param key 추적할 키 (IP 또는 IP:username)
     */
    void recordFailure(String key);

    /**
     * 로그인 성공 시 실패 기록을 초기화합니다.
     *
     * @param key 추적할 키 (IP 또는 IP:username)
     */
    void recordSuccess(String key);

    /**
     * 해당 키가 잠금 상태인지 확인합니다.
     *
     * @param key 확인할 키 (IP 또는 IP:username)
     * @return 잠금 상태이면 true, 그렇지 않으면 false
     */
    boolean isBlocked(String key);

    /**
     * 현재 실패 횟수를 조회합니다.
     *
     * @param key 확인할 키 (IP 또는 IP:username)
     * @return 현재 실패 횟수 (기록이 없으면 0)
     */
    int getAttemptCount(String key);

    /**
     * 잠금 해제까지 남은 시간(초)을 조회합니다.
     *
     * @param key 확인할 키 (IP 또는 IP:username)
     * @return 남은 잠금 시간(초), 잠금 상태가 아니면 0
     */
    long getRemainingLockTime(String key);
}

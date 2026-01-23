package com.portal.universe.authservice.repository;

import com.portal.universe.authservice.domain.PasswordHistory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PasswordHistoryRepository extends JpaRepository<PasswordHistory, Long> {

    /**
     * 특정 사용자의 최근 N개 비밀번호 히스토리를 조회합니다.
     * 가장 최근 것부터 내림차순으로 정렬됩니다.
     *
     * @param userId 사용자 ID
     * @param pageable 페이징 정보 (크기 제한)
     * @return 비밀번호 히스토리 목록
     */
    @Query("SELECT ph FROM PasswordHistory ph WHERE ph.userId = :userId ORDER BY ph.createdAt DESC")
    List<PasswordHistory> findRecentByUserId(@Param("userId") Long userId, Pageable pageable);

    /**
     * 특정 사용자의 모든 비밀번호 히스토리를 삭제합니다.
     * 회원 탈퇴 시 사용됩니다.
     *
     * @param userId 사용자 ID
     */
    void deleteByUserId(Long userId);
}

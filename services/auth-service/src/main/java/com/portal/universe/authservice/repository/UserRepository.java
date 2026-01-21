package com.portal.universe.authservice.repository;

import com.portal.universe.authservice.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * User 엔티티에 대한 데이터 접근을 처리하는 Spring Data JPA 리포지토리입니다.
 */
public interface UserRepository extends JpaRepository<User, Long> {
    /**
     * 주어진 이메일 주소로 사용자를 조회합니다.
     * @param email 조회할 사용자의 이메일
     * @return 해당 이메일을 가진 사용자를 담은 Optional 객체. 존재하지 않으면 Optional.empty()를 반환합니다.
     */
    Optional<User> findByEmail(String email);

    /**
     * 주어진 UUID로 사용자를 조회합니다.
     * @param uuid 조회할 사용자의 UUID
     * @return 해당 UUID를 가진 사용자를 담은 Optional 객체. 존재하지 않으면 Optional.empty()를 반환합니다.
     */
    Optional<User> findByUuid(String uuid);
}
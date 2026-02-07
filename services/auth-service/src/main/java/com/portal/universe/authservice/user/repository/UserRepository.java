package com.portal.universe.authservice.user.repository;

import com.portal.universe.authservice.user.domain.User;
import com.portal.universe.authservice.user.domain.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
     * 주어진 이메일 주소로 사용자를 프로필과 함께 조회합니다.
     * @param email 조회할 사용자의 이메일
     * @return 해당 이메일을 가진 사용자를 담은 Optional 객체. 존재하지 않으면 Optional.empty()를 반환합니다.
     */
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.profile WHERE u.email = :email")
    Optional<User> findByEmailWithProfile(@Param("email") String email);

    /**
     * 주어진 UUID로 사용자를 조회합니다.
     * @param uuid 조회할 사용자의 UUID
     * @return 해당 UUID를 가진 사용자를 담은 Optional 객체. 존재하지 않으면 Optional.empty()를 반환합니다.
     */
    Optional<User> findByUuid(String uuid);

    /**
     * 주어진 UUID로 사용자를 프로필과 함께 조회합니다.
     * @param uuid 조회할 사용자의 UUID
     * @return 해당 UUID를 가진 사용자를 담은 Optional 객체. 존재하지 않으면 Optional.empty()를 반환합니다.
     */
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.profile WHERE u.uuid = :uuid")
    Optional<User> findByUuidWithProfile(@Param("uuid") String uuid);

    /**
     * 주어진 username으로 사용자를 조회합니다.
     * @param username 조회할 사용자의 username
     * @return 해당 username을 가진 사용자를 담은 Optional 객체. 존재하지 않으면 Optional.empty()를 반환합니다.
     */
    @Query("SELECT u FROM User u JOIN FETCH u.profile p WHERE p.username = :username")
    Optional<User> findByUsername(@Param("username") String username);

    /**
     * 주어진 username이 존재하는지 확인합니다.
     * @param username 확인할 username
     * @return username이 존재하면 true, 아니면 false
     */
    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END FROM User u JOIN u.profile p WHERE p.username = :username")
    boolean existsByUsername(@Param("username") String username);

    long countByStatus(UserStatus status);

    @EntityGraph(attributePaths = {"profile"})
    @Query("""
            SELECT u FROM User u LEFT JOIN u.profile p
            WHERE LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%'))
               OR LOWER(p.username) LIKE LOWER(CONCAT('%', :query, '%'))
               OR LOWER(p.nickname) LIKE LOWER(CONCAT('%', :query, '%'))
            """)
    Page<User> searchByQuery(@Param("query") String query, Pageable pageable);

    @EntityGraph(attributePaths = {"profile"})
    Page<User> findAllBy(Pageable pageable);
}
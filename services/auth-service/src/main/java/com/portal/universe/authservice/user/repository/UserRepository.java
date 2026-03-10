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

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.profile WHERE u.email = :email")
    Optional<User> findByEmailWithProfile(@Param("email") String email);

    Optional<User> findByUuid(String uuid);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.profile WHERE u.uuid = :uuid")
    Optional<User> findByUuidWithProfile(@Param("uuid") String uuid);

    @Query("SELECT u FROM User u JOIN FETCH u.profile p WHERE p.username = :username")
    Optional<User> findByUsername(@Param("username") String username);

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

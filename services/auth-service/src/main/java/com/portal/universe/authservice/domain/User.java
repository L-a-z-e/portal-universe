package com.portal.universe.authservice.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 사용자 정보를 나타내는 JPA 엔티티 클래스입니다.
 */
@Entity
@Table(name = "users") // 데이터베이스의 'users' 테이블과 매핑
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA 프록시 생성을 위한 기본 생성자
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // ID 자동 생성 전략 (MySQL의 AUTO_INCREMENT)
    private Long id;

    @Column(nullable = false, unique = true) // null 불가, 유니크 제약 조건
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING) // Enum 이름을 문자열로 저장
    @Column(nullable = false)
    private Role role;

    /**
     * 새로운 사용자를 생성하는 생성자입니다.
     * @param email 사용자 이메일
     * @param password 암호화된 비밀번호
     * @param name 사용자 이름
     * @param role 사용자 역할
     */
    public User(String email, String password, String name, Role role) {
        this.email = email;
        this.password = password;
        this.name = name;
        this.role = role;
    }
}
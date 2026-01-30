package com.portal.universe.authservice.auth.service;

import com.portal.universe.authservice.auth.repository.UserRoleRepository;
import com.portal.universe.authservice.user.domain.User;
import com.portal.universe.authservice.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Spring Security의 UserDetailsService를 구현한 클래스입니다.
 * 사용자 이름(본 서비스에서는 이메일)을 기반으로 사용자 정보를 데이터베이스에서 조회하여
 * Spring Security가 이해할 수 있는 UserDetails 객체로 변환하는 역할을 합니다.
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;

    /**
     * 주어진 이메일(username)로 사용자를 찾아 UserDetails 객체를 반환합니다.
     * RBAC 테이블 기반으로 복수 authority를 지원합니다.
     *
     * @param email Spring Security가 전달하는 사용자 식별자 (로그인 시 입력한 username)
     * @return Spring Security가 사용할 사용자 상세 정보 객체
     * @throws UsernameNotFoundException 해당 이메일을 가진 사용자가 없을 경우 발생
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        // RBAC 테이블 기반 복수 authority 조회
        List<String> roleKeys = userRoleRepository.findActiveRoleKeysByUserId(user.getUuid());
        List<SimpleGrantedAuthority> authorities = roleKeys.stream()
                .map(SimpleGrantedAuthority::new)
                .toList();

        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                authorities
        );
    }
}

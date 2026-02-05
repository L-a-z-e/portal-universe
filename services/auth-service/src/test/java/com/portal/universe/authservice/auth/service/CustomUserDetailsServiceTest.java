package com.portal.universe.authservice.auth.service;

import com.portal.universe.authservice.auth.repository.UserRoleRepository;
import com.portal.universe.authservice.user.domain.User;
import com.portal.universe.authservice.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CustomUserDetailsService 테스트")
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserRoleRepository userRoleRepository;

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    private static final String EMAIL = "test@example.com";
    private static final String PASSWORD = "encodedPassword";
    private static final String UUID = "test-uuid-1234";

    private User createTestUser() {
        User user = new User(EMAIL, PASSWORD);
        try {
            var uuidField = User.class.getDeclaredField("uuid");
            uuidField.setAccessible(true);
            uuidField.set(user, UUID);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return user;
    }

    @Nested
    @DisplayName("loadUserByUsername")
    class LoadUserByUsername {

        @Test
        @DisplayName("should_returnUserDetails_when_userExistsWithRoles")
        void should_returnUserDetails_when_userExistsWithRoles() {
            // given
            User user = createTestUser();
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
            when(userRoleRepository.findActiveRoleKeysByUserId(UUID))
                    .thenReturn(List.of("ROLE_USER", "ROLE_ADMIN"));

            // when
            UserDetails userDetails = customUserDetailsService.loadUserByUsername(EMAIL);

            // then
            assertThat(userDetails.getUsername()).isEqualTo(EMAIL);
            assertThat(userDetails.getPassword()).isEqualTo(PASSWORD);
            assertThat(userDetails.getAuthorities()).hasSize(2);
            assertThat(userDetails.getAuthorities())
                    .extracting(auth -> auth.getAuthority())
                    .containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN");
        }

        @Test
        @DisplayName("should_returnUserDetailsWithEmptyAuthorities_when_userHasNoRoles")
        void should_returnUserDetailsWithEmptyAuthorities_when_userHasNoRoles() {
            // given
            User user = createTestUser();
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
            when(userRoleRepository.findActiveRoleKeysByUserId(UUID))
                    .thenReturn(List.of());

            // when
            UserDetails userDetails = customUserDetailsService.loadUserByUsername(EMAIL);

            // then
            assertThat(userDetails.getUsername()).isEqualTo(EMAIL);
            assertThat(userDetails.getAuthorities()).isEmpty();
        }

        @Test
        @DisplayName("should_throwUsernameNotFoundException_when_userNotFound")
        void should_throwUsernameNotFoundException_when_userNotFound() {
            // given
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername(EMAIL))
                    .isInstanceOf(UsernameNotFoundException.class)
                    .hasMessageContaining(EMAIL);
        }
    }
}

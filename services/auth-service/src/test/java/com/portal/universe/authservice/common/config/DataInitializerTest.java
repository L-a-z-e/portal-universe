package com.portal.universe.authservice.common.config;

import com.portal.universe.authservice.auth.domain.RoleEntity;
import com.portal.universe.authservice.auth.domain.UserRole;
import com.portal.universe.authservice.auth.repository.RoleEntityRepository;
import com.portal.universe.authservice.auth.repository.UserRoleRepository;
import com.portal.universe.authservice.auth.service.RbacInitializationService;
import com.portal.universe.authservice.user.domain.User;
import com.portal.universe.authservice.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DataInitializer 테스트")
class DataInitializerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RbacInitializationService rbacInitializationService;

    @Mock
    private RoleEntityRepository roleEntityRepository;

    @Mock
    private UserRoleRepository userRoleRepository;

    @InjectMocks
    private DataInitializer dataInitializer;

    @Nested
    @DisplayName("initData")
    class InitData {

        @Test
        @DisplayName("should_createTestUsers_when_theyDontExist")
        void should_createTestUsers_when_theyDontExist() throws Exception {
            // given
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());
            when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.empty());
            when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
                User user = invocation.getArgument(0);
                try {
                    var idField = User.class.getDeclaredField("id");
                    idField.setAccessible(true);
                    idField.set(user, 1L);
                    var uuidField = User.class.getDeclaredField("uuid");
                    uuidField.setAccessible(true);
                    uuidField.set(user, "generated-uuid");
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                return user;
            });

            RoleEntity adminRole = RoleEntity.builder()
                    .roleKey("ROLE_SUPER_ADMIN")
                    .displayName("Super Admin")
                    .system(true)
                    .build();
            when(roleEntityRepository.findByRoleKey("ROLE_SUPER_ADMIN"))
                    .thenReturn(Optional.of(adminRole));
            when(userRoleRepository.save(any(UserRole.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // when
            CommandLineRunner runner = dataInitializer.initData();
            runner.run();

            // then
            verify(userRepository, times(2)).save(any(User.class));
            verify(rbacInitializationService, times(2)).initializeNewUser(anyString());
            verify(userRoleRepository).save(any(UserRole.class)); // ROLE_SUPER_ADMIN for admin user
        }

        @Test
        @DisplayName("should_skipUserCreation_when_usersAlreadyExist")
        void should_skipUserCreation_when_usersAlreadyExist() throws Exception {
            // given
            User existingUser = new User("test@example.com", "encodedPassword");
            User existingAdmin = new User("admin@example.com", "encodedPassword");

            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(existingUser));
            when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(existingAdmin));

            // when
            CommandLineRunner runner = dataInitializer.initData();
            runner.run();

            // then
            verify(userRepository, never()).save(any(User.class));
            verify(rbacInitializationService, never()).initializeNewUser(anyString());
        }

        @Test
        @DisplayName("should_returnNonNullRunner_when_initDataCalled")
        void should_returnNonNullRunner_when_initDataCalled() {
            // when
            CommandLineRunner runner = dataInitializer.initData();

            // then
            assertThat(runner).isNotNull();
        }
    }
}

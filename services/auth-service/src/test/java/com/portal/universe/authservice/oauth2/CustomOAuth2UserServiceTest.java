package com.portal.universe.authservice.oauth2;

import com.portal.universe.authservice.auth.repository.UserRoleRepository;
import com.portal.universe.authservice.auth.service.RbacInitializationService;
import com.portal.universe.authservice.oauth2.domain.SocialAccount;
import com.portal.universe.authservice.oauth2.domain.SocialProvider;
import com.portal.universe.authservice.oauth2.repository.SocialAccountRepository;
import com.portal.universe.authservice.user.domain.User;
import com.portal.universe.authservice.user.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CustomOAuth2UserService Test")
class CustomOAuth2UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private SocialAccountRepository socialAccountRepository;

    @Mock
    private UserRoleRepository userRoleRepository;

    @Mock
    private RbacInitializationService rbacInitializationService;

    @Spy
    @InjectMocks
    private CustomOAuth2UserService customOAuth2UserService;

    private static final String USER_UUID = "test-uuid-123";
    private static final String USER_EMAIL = "test@gmail.com";
    private static final String PROVIDER_ID = "google-provider-id-123";

    @Nested
    @DisplayName("loadUser - 기존 소셜 계정 존재")
    class ExistingSocialAccount {

        @Test
        @DisplayName("기존 소셜 계정이 있으면 해당 사용자를 반환한다")
        void should_returnExistingUser_when_socialAccountExists() throws Exception {
            // given
            User existingUser = createUserWithUuid(USER_EMAIL, USER_UUID);

            SocialAccount socialAccount = new SocialAccount(existingUser, SocialProvider.GOOGLE, PROVIDER_ID);

            Map<String, Object> attributes = Map.of(
                    "sub", PROVIDER_ID,
                    "email", USER_EMAIL,
                    "name", "Test User",
                    "picture", "https://example.com/photo.jpg"
            );

            when(socialAccountRepository.findByProviderAndProviderId(SocialProvider.GOOGLE, PROVIDER_ID))
                    .thenReturn(Optional.of(socialAccount));

            // when - processOAuth2User 내부 로직 검증을 위해 리플렉션 사용
            var method = CustomOAuth2UserService.class.getDeclaredMethod(
                    "processOAuth2User", String.class, OAuth2UserInfo.class);
            method.setAccessible(true);

            OAuth2UserInfo userInfo = new GoogleOAuth2UserInfo(attributes);
            User result = (User) method.invoke(customOAuth2UserService, "google", userInfo);

            // then
            assertThat(result).isEqualTo(existingUser);
            assertThat(result.getUuid()).isEqualTo(USER_UUID);
            verify(userRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("loadUser - 이메일 매칭으로 계정 연동")
    class EmailMatchLinkAccount {

        @Test
        @DisplayName("동일 이메일의 기존 사용자가 있으면 소셜 계정을 연동한다")
        void should_linkSocialAccount_when_emailMatchesDifferentProvider() throws Exception {
            // given
            User existingUser = createUserWithUuid(USER_EMAIL, USER_UUID);
            existingUser.getSocialAccounts(); // initialize list

            Map<String, Object> attributes = Map.of(
                    "sub", PROVIDER_ID,
                    "email", USER_EMAIL,
                    "name", "Test User",
                    "picture", "https://example.com/photo.jpg"
            );

            when(socialAccountRepository.findByProviderAndProviderId(SocialProvider.GOOGLE, PROVIDER_ID))
                    .thenReturn(Optional.empty());
            when(userRepository.findByEmail(USER_EMAIL))
                    .thenReturn(Optional.of(existingUser));
            when(userRepository.save(existingUser)).thenReturn(existingUser);

            // when
            var method = CustomOAuth2UserService.class.getDeclaredMethod(
                    "processOAuth2User", String.class, OAuth2UserInfo.class);
            method.setAccessible(true);

            OAuth2UserInfo userInfo = new GoogleOAuth2UserInfo(attributes);
            User result = (User) method.invoke(customOAuth2UserService, "google", userInfo);

            // then
            assertThat(result).isEqualTo(existingUser);
            verify(userRepository).save(existingUser);
            assertThat(existingUser.getSocialAccounts()).hasSize(1);
            assertThat(existingUser.getSocialAccounts().get(0).getProvider()).isEqualTo(SocialProvider.GOOGLE);
        }
    }

    @Nested
    @DisplayName("loadUser - 신규 사용자 생성")
    class NewUserCreation {

        @Test
        @DisplayName("신규 사용자이면 User + SocialAccount를 생성하고 RBAC를 초기화한다")
        void should_createNewUserWithSocialAccountAndRbac_when_newUser() throws Exception {
            // given
            Map<String, Object> attributes = Map.of(
                    "sub", PROVIDER_ID,
                    "email", USER_EMAIL,
                    "name", "New User",
                    "picture", "https://example.com/photo.jpg"
            );

            when(socialAccountRepository.findByProviderAndProviderId(SocialProvider.GOOGLE, PROVIDER_ID))
                    .thenReturn(Optional.empty());
            when(userRepository.findByEmail(USER_EMAIL))
                    .thenReturn(Optional.empty());

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            when(userRepository.save(userCaptor.capture())).thenAnswer(invocation -> {
                User user = invocation.getArgument(0);
                setFieldValue(user, "uuid", USER_UUID);
                return user;
            });

            // when
            var method = CustomOAuth2UserService.class.getDeclaredMethod(
                    "processOAuth2User", String.class, OAuth2UserInfo.class);
            method.setAccessible(true);

            OAuth2UserInfo userInfo = new GoogleOAuth2UserInfo(attributes);
            User result = (User) method.invoke(customOAuth2UserService, "google", userInfo);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getEmail()).isEqualTo(USER_EMAIL);
            assertThat(result.getPassword()).isNull();
            assertThat(result.getProfile()).isNotNull();
            assertThat(result.getProfile().getNickname()).isEqualTo("New User");
            assertThat(result.getSocialAccounts()).hasSize(1);

            verify(rbacInitializationService).initializeNewUser(USER_UUID);
        }

        @Test
        @DisplayName("이름이 null이면 providerId 기반 닉네임을 생성한다")
        void should_generateNickname_when_nameIsNull() throws Exception {
            // given
            String providerId = "12345678abcdef";
            Map<String, Object> attributes = new HashMap<>();
            attributes.put("sub", providerId);
            attributes.put("email", USER_EMAIL);
            attributes.put("name", null);
            attributes.put("picture", null);

            when(socialAccountRepository.findByProviderAndProviderId(SocialProvider.GOOGLE, providerId))
                    .thenReturn(Optional.empty());
            when(userRepository.findByEmail(USER_EMAIL))
                    .thenReturn(Optional.empty());
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
                User user = invocation.getArgument(0);
                setFieldValue(user, "uuid", USER_UUID);
                return user;
            });

            // when
            var method = CustomOAuth2UserService.class.getDeclaredMethod(
                    "processOAuth2User", String.class, OAuth2UserInfo.class);
            method.setAccessible(true);

            OAuth2UserInfo userInfo = new GoogleOAuth2UserInfo(attributes);
            User result = (User) method.invoke(customOAuth2UserService, "google", userInfo);

            // then
            assertThat(result.getProfile().getNickname()).isEqualTo("User_12345678");
        }
    }

    @Nested
    @DisplayName("Provider별 처리 - Google")
    class GoogleProvider {

        @Test
        @DisplayName("should_handleGoogleProvider_when_newUser")
        void should_handleGoogleProvider_when_newUser() throws Exception {
            // given
            Map<String, Object> attributes = Map.of(
                    "sub", PROVIDER_ID,
                    "email", USER_EMAIL,
                    "name", "Google User",
                    "picture", "https://lh3.googleusercontent.com/photo.jpg"
            );

            when(socialAccountRepository.findByProviderAndProviderId(SocialProvider.GOOGLE, PROVIDER_ID))
                    .thenReturn(Optional.empty());
            when(userRepository.findByEmail(USER_EMAIL))
                    .thenReturn(Optional.empty());
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
                User user = invocation.getArgument(0);
                setFieldValue(user, "uuid", USER_UUID);
                return user;
            });

            // when
            var method = CustomOAuth2UserService.class.getDeclaredMethod(
                    "processOAuth2User", String.class, OAuth2UserInfo.class);
            method.setAccessible(true);

            OAuth2UserInfo userInfo = new GoogleOAuth2UserInfo(attributes);
            User result = (User) method.invoke(customOAuth2UserService, "google", userInfo);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getEmail()).isEqualTo(USER_EMAIL);
            assertThat(result.getSocialAccounts()).hasSize(1);
            assertThat(result.getSocialAccounts().get(0).getProvider()).isEqualTo(SocialProvider.GOOGLE);
            assertThat(result.getProfile().getNickname()).isEqualTo("Google User");
        }
    }

    @Nested
    @DisplayName("Provider별 처리 - Kakao")
    class KakaoProvider {

        @Test
        @DisplayName("should_handleKakaoProvider_when_newUser")
        void should_handleKakaoProvider_when_newUser() throws Exception {
            // given - Kakao 중첩 구조 attributes
            Map<String, Object> profile = new HashMap<>();
            profile.put("nickname", "카카오사용자");
            profile.put("profile_image_url", "https://k.kakaocdn.net/photo.jpg");

            Map<String, Object> kakaoAccount = new HashMap<>();
            kakaoAccount.put("email", USER_EMAIL);
            kakaoAccount.put("profile", profile);

            Map<String, Object> attributes = new HashMap<>();
            attributes.put("id", 1234567890L);
            attributes.put("kakao_account", kakaoAccount);

            String kakaoProviderId = "1234567890";

            when(socialAccountRepository.findByProviderAndProviderId(SocialProvider.KAKAO, kakaoProviderId))
                    .thenReturn(Optional.empty());
            when(userRepository.findByEmail(USER_EMAIL))
                    .thenReturn(Optional.empty());
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
                User user = invocation.getArgument(0);
                setFieldValue(user, "uuid", USER_UUID);
                return user;
            });

            // when
            var method = CustomOAuth2UserService.class.getDeclaredMethod(
                    "processOAuth2User", String.class, OAuth2UserInfo.class);
            method.setAccessible(true);

            OAuth2UserInfo userInfo = new KakaoOAuth2UserInfo(attributes);
            User result = (User) method.invoke(customOAuth2UserService, "kakao", userInfo);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getEmail()).isEqualTo(USER_EMAIL);
            assertThat(result.getSocialAccounts()).hasSize(1);
            assertThat(result.getSocialAccounts().get(0).getProvider()).isEqualTo(SocialProvider.KAKAO);
            assertThat(result.getProfile().getNickname()).isEqualTo("카카오사용자");
            verify(rbacInitializationService).initializeNewUser(USER_UUID);
        }
    }

    @Nested
    @DisplayName("Provider별 처리 - Naver")
    class NaverProvider {

        @Test
        @DisplayName("should_handleNaverProvider_when_newUser")
        void should_handleNaverProvider_when_newUser() throws Exception {
            // given - Naver response wrapper 구조
            Map<String, Object> naverResponse = new HashMap<>();
            naverResponse.put("id", "naver-id-123");
            naverResponse.put("email", USER_EMAIL);
            naverResponse.put("name", "네이버사용자");
            naverResponse.put("profile_image", "https://phinf.pstatic.net/photo.jpg");

            Map<String, Object> attributes = new HashMap<>();
            attributes.put("response", naverResponse);

            when(socialAccountRepository.findByProviderAndProviderId(SocialProvider.NAVER, "naver-id-123"))
                    .thenReturn(Optional.empty());
            when(userRepository.findByEmail(USER_EMAIL))
                    .thenReturn(Optional.empty());
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
                User user = invocation.getArgument(0);
                setFieldValue(user, "uuid", USER_UUID);
                return user;
            });

            // when
            var method = CustomOAuth2UserService.class.getDeclaredMethod(
                    "processOAuth2User", String.class, OAuth2UserInfo.class);
            method.setAccessible(true);

            OAuth2UserInfo userInfo = new NaverOAuth2UserInfo(attributes);
            User result = (User) method.invoke(customOAuth2UserService, "naver", userInfo);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getEmail()).isEqualTo(USER_EMAIL);
            assertThat(result.getSocialAccounts()).hasSize(1);
            assertThat(result.getSocialAccounts().get(0).getProvider()).isEqualTo(SocialProvider.NAVER);
            assertThat(result.getProfile().getNickname()).isEqualTo("네이버사용자");
        }
    }

    @Nested
    @DisplayName("CustomOAuth2User 역할 매핑")
    class RoleMapping {

        @Test
        @DisplayName("should_mapRolesToCustomOAuth2User_when_rolesProvided")
        void should_mapRolesToCustomOAuth2User_when_rolesProvided() throws Exception {
            // given
            User user = createUserWithUuid(USER_EMAIL, USER_UUID);
            Map<String, Object> attributes = Map.of("sub", PROVIDER_ID, "email", USER_EMAIL);
            List<String> roleKeys = List.of("ROLE_USER", "ROLE_BLOG_ADMIN", "ROLE_SUPER_ADMIN");

            // when
            CustomOAuth2User oAuth2User = new CustomOAuth2User(user, attributes, "sub", roleKeys);

            // then
            assertThat(oAuth2User.getAuthorities()).hasSize(3);
            assertThat(oAuth2User.getAuthorities())
                    .extracting("authority")
                    .containsExactly("ROLE_USER", "ROLE_BLOG_ADMIN", "ROLE_SUPER_ADMIN");
            assertThat(oAuth2User.getUuid()).isEqualTo(USER_UUID);
            assertThat(oAuth2User.getEmail()).isEqualTo(USER_EMAIL);
        }
    }

    // ========== Helper Methods ==========

    private User createUserWithUuid(String email, String uuid) throws Exception {
        User user = new User(email, null);
        setFieldValue(user, "uuid", uuid);
        return user;
    }

    private void setFieldValue(Object target, String fieldName, Object value) throws Exception {
        Field field = findField(target.getClass(), fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private Field findField(Class<?> clazz, String fieldName) {
        Class<?> current = clazz;
        while (current != null) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            }
        }
        throw new RuntimeException("Field not found: " + fieldName);
    }
}

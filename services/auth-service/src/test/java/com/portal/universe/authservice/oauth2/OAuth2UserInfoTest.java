package com.portal.universe.authservice.oauth2;

import com.portal.universe.authservice.oauth2.domain.SocialProvider;
import org.junit.jupiter.api.*;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("OAuth2UserInfo Test")
class OAuth2UserInfoTest {

    @Nested
    @DisplayName("OAuth2UserInfoFactory")
    class Factory {

        @Test
        @DisplayName("google registrationId로 GoogleOAuth2UserInfo를 생성한다")
        void should_createGoogleUserInfo_when_registrationIdIsGoogle() {
            // given
            Map<String, Object> attributes = Map.of("sub", "123");

            // when
            OAuth2UserInfo userInfo = OAuth2UserInfoFactory.getOAuth2UserInfo("google", attributes);

            // then
            assertThat(userInfo).isInstanceOf(GoogleOAuth2UserInfo.class);
        }

        @Test
        @DisplayName("naver registrationId로 NaverOAuth2UserInfo를 생성한다")
        void should_createNaverUserInfo_when_registrationIdIsNaver() {
            // given
            Map<String, Object> attributes = Map.of("response", Map.of("id", "123"));

            // when
            OAuth2UserInfo userInfo = OAuth2UserInfoFactory.getOAuth2UserInfo("naver", attributes);

            // then
            assertThat(userInfo).isInstanceOf(NaverOAuth2UserInfo.class);
        }

        @Test
        @DisplayName("kakao registrationId로 KakaoOAuth2UserInfo를 생성한다")
        void should_createKakaoUserInfo_when_registrationIdIsKakao() {
            // given
            Map<String, Object> attributes = Map.of("id", 123456L);

            // when
            OAuth2UserInfo userInfo = OAuth2UserInfoFactory.getOAuth2UserInfo("kakao", attributes);

            // then
            assertThat(userInfo).isInstanceOf(KakaoOAuth2UserInfo.class);
        }

        @Test
        @DisplayName("대소문자에 관계없이 provider를 인식한다")
        void should_recognizeProvider_when_caseInsensitive() {
            // given
            Map<String, Object> attributes = Map.of("sub", "123");

            // when
            OAuth2UserInfo userInfo = OAuth2UserInfoFactory.getOAuth2UserInfo("GOOGLE", attributes);

            // then
            assertThat(userInfo).isInstanceOf(GoogleOAuth2UserInfo.class);
        }

        @Test
        @DisplayName("지원하지 않는 provider이면 IllegalArgumentException을 발생시킨다")
        void should_throwException_when_unsupportedProvider() {
            // given
            Map<String, Object> attributes = Map.of("id", "123");

            // when & then
            assertThatThrownBy(() -> OAuth2UserInfoFactory.getOAuth2UserInfo("facebook", attributes))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("GoogleOAuth2UserInfo")
    class Google {

        @Test
        @DisplayName("Google 속성에서 사용자 정보를 올바르게 파싱한다")
        void should_parseUserInfo_when_googleAttributes() {
            // given
            Map<String, Object> attributes = Map.of(
                    "sub", "google-123456",
                    "email", "user@gmail.com",
                    "name", "홍길동",
                    "picture", "https://lh3.googleusercontent.com/photo.jpg"
            );

            // when
            GoogleOAuth2UserInfo userInfo = new GoogleOAuth2UserInfo(attributes);

            // then
            assertThat(userInfo.getId()).isEqualTo("google-123456");
            assertThat(userInfo.getEmail()).isEqualTo("user@gmail.com");
            assertThat(userInfo.getName()).isEqualTo("홍길동");
            assertThat(userInfo.getImageUrl()).isEqualTo("https://lh3.googleusercontent.com/photo.jpg");
        }

        @Test
        @DisplayName("속성이 없으면 null을 반환한다")
        void should_returnNull_when_attributesMissing() {
            // given
            Map<String, Object> attributes = new HashMap<>();

            // when
            GoogleOAuth2UserInfo userInfo = new GoogleOAuth2UserInfo(attributes);

            // then
            assertThat(userInfo.getId()).isNull();
            assertThat(userInfo.getEmail()).isNull();
            assertThat(userInfo.getName()).isNull();
            assertThat(userInfo.getImageUrl()).isNull();
        }

        @Test
        @DisplayName("getAttributes로 원본 attributes를 반환한다")
        void should_returnOriginalAttributes() {
            // given
            Map<String, Object> attributes = Map.of("sub", "123", "email", "test@test.com");

            // when
            GoogleOAuth2UserInfo userInfo = new GoogleOAuth2UserInfo(attributes);

            // then
            assertThat(userInfo.getAttributes()).isEqualTo(attributes);
        }
    }

    @Nested
    @DisplayName("KakaoOAuth2UserInfo")
    class Kakao {

        @Test
        @DisplayName("Kakao 중첩 속성에서 사용자 정보를 올바르게 파싱한다")
        void should_parseUserInfo_when_kakaoNestedAttributes() {
            // given
            Map<String, Object> profile = Map.of(
                    "nickname", "카카오닉네임",
                    "profile_image_url", "https://k.kakaocdn.net/profile.jpg",
                    "thumbnail_image_url", "https://k.kakaocdn.net/thumb.jpg"
            );
            Map<String, Object> kakaoAccount = Map.of(
                    "email", "user@kakao.com",
                    "profile", profile
            );
            Map<String, Object> attributes = Map.of(
                    "id", 1234567890L,
                    "kakao_account", kakaoAccount
            );

            // when
            KakaoOAuth2UserInfo userInfo = new KakaoOAuth2UserInfo(attributes);

            // then
            assertThat(userInfo.getId()).isEqualTo("1234567890");
            assertThat(userInfo.getEmail()).isEqualTo("user@kakao.com");
            assertThat(userInfo.getName()).isEqualTo("카카오닉네임");
            assertThat(userInfo.getImageUrl()).isEqualTo("https://k.kakaocdn.net/profile.jpg");
        }

        @Test
        @DisplayName("Kakao id가 Long 타입이면 String으로 변환한다")
        void should_convertIdToString_when_idIsLong() {
            // given
            Map<String, Object> attributes = new HashMap<>();
            attributes.put("id", 9876543210L);

            // when
            KakaoOAuth2UserInfo userInfo = new KakaoOAuth2UserInfo(attributes);

            // then
            assertThat(userInfo.getId()).isEqualTo("9876543210");
        }

        @Test
        @DisplayName("profile_image_url이 없으면 thumbnail_image_url을 사용한다")
        void should_useThumbnailUrl_when_profileImageUrlIsMissing() {
            // given
            Map<String, Object> profile = new HashMap<>();
            profile.put("nickname", "닉네임");
            profile.put("profile_image_url", null);
            profile.put("thumbnail_image_url", "https://k.kakaocdn.net/thumb.jpg");

            Map<String, Object> kakaoAccount = Map.of(
                    "email", "user@kakao.com",
                    "profile", profile
            );
            Map<String, Object> attributes = Map.of(
                    "id", 123L,
                    "kakao_account", kakaoAccount
            );

            // when
            KakaoOAuth2UserInfo userInfo = new KakaoOAuth2UserInfo(attributes);

            // then
            assertThat(userInfo.getImageUrl()).isEqualTo("https://k.kakaocdn.net/thumb.jpg");
        }

        @Test
        @DisplayName("kakao_account가 없으면 null을 반환한다")
        void should_returnNull_when_kakaoAccountIsMissing() {
            // given
            Map<String, Object> attributes = new HashMap<>();
            attributes.put("id", 123L);

            // when
            KakaoOAuth2UserInfo userInfo = new KakaoOAuth2UserInfo(attributes);

            // then
            assertThat(userInfo.getEmail()).isNull();
            assertThat(userInfo.getName()).isNull();
            assertThat(userInfo.getImageUrl()).isNull();
        }

        @Test
        @DisplayName("profile이 없으면 name과 imageUrl은 null을 반환한다")
        void should_returnNull_when_profileIsMissing() {
            // given
            Map<String, Object> kakaoAccount = new HashMap<>();
            kakaoAccount.put("email", "user@kakao.com");

            Map<String, Object> attributes = Map.of(
                    "id", 123L,
                    "kakao_account", kakaoAccount
            );

            // when
            KakaoOAuth2UserInfo userInfo = new KakaoOAuth2UserInfo(attributes);

            // then
            assertThat(userInfo.getEmail()).isEqualTo("user@kakao.com");
            assertThat(userInfo.getName()).isNull();
            assertThat(userInfo.getImageUrl()).isNull();
        }

        @Test
        @DisplayName("id가 null이면 null을 반환한다")
        void should_returnNull_when_idIsNull() {
            // given
            Map<String, Object> attributes = new HashMap<>();
            attributes.put("id", null);

            // when
            KakaoOAuth2UserInfo userInfo = new KakaoOAuth2UserInfo(attributes);

            // then
            assertThat(userInfo.getId()).isNull();
        }
    }

    @Nested
    @DisplayName("NaverOAuth2UserInfo")
    class Naver {

        @Test
        @DisplayName("Naver response 래핑에서 사용자 정보를 올바르게 파싱한다")
        void should_parseUserInfo_when_naverResponseWrapped() {
            // given
            Map<String, Object> responseMap = Map.of(
                    "id", "naver-123456",
                    "email", "user@naver.com",
                    "name", "홍길동",
                    "nickname", "길동이",
                    "profile_image", "https://phinf.pstatic.net/profile.jpg"
            );
            Map<String, Object> attributes = Map.of("response", responseMap);

            // when
            NaverOAuth2UserInfo userInfo = new NaverOAuth2UserInfo(attributes);

            // then
            assertThat(userInfo.getId()).isEqualTo("naver-123456");
            assertThat(userInfo.getEmail()).isEqualTo("user@naver.com");
            assertThat(userInfo.getName()).isEqualTo("홍길동");
            assertThat(userInfo.getImageUrl()).isEqualTo("https://phinf.pstatic.net/profile.jpg");
        }

        @Test
        @DisplayName("name이 없으면 nickname을 사용한다")
        void should_useNickname_when_nameIsMissing() {
            // given
            Map<String, Object> responseMap = new HashMap<>();
            responseMap.put("id", "naver-123");
            responseMap.put("email", "user@naver.com");
            responseMap.put("name", null);
            responseMap.put("nickname", "네이버닉네임");

            Map<String, Object> attributes = Map.of("response", responseMap);

            // when
            NaverOAuth2UserInfo userInfo = new NaverOAuth2UserInfo(attributes);

            // then
            assertThat(userInfo.getName()).isEqualTo("네이버닉네임");
        }

        @Test
        @DisplayName("response가 없으면 모든 값이 null이다")
        void should_returnNull_when_responseIsMissing() {
            // given
            Map<String, Object> attributes = new HashMap<>();

            // when
            NaverOAuth2UserInfo userInfo = new NaverOAuth2UserInfo(attributes);

            // then
            assertThat(userInfo.getId()).isNull();
            assertThat(userInfo.getEmail()).isNull();
            assertThat(userInfo.getName()).isNull();
            assertThat(userInfo.getImageUrl()).isNull();
        }

        @Test
        @DisplayName("name과 nickname이 모두 있으면 name을 우선한다")
        void should_preferName_when_bothNameAndNicknameExist() {
            // given
            Map<String, Object> responseMap = Map.of(
                    "id", "naver-123",
                    "email", "user@naver.com",
                    "name", "실명",
                    "nickname", "닉네임"
            );
            Map<String, Object> attributes = Map.of("response", responseMap);

            // when
            NaverOAuth2UserInfo userInfo = new NaverOAuth2UserInfo(attributes);

            // then
            assertThat(userInfo.getName()).isEqualTo("실명");
        }
    }
}

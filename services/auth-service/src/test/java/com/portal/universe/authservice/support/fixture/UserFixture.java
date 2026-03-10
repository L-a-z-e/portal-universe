package com.portal.universe.authservice.support.fixture;

import com.portal.universe.authservice.oauth2.domain.SocialAccount;
import com.portal.universe.authservice.oauth2.domain.SocialProvider;
import com.portal.universe.authservice.user.domain.User;
import com.portal.universe.authservice.user.domain.UserProfile;
import com.portal.universe.authservice.user.domain.UserStatus;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * User + UserProfile 테스트 데이터 빌더.
 * ReflectionTestUtils 사용을 이 클래스 내부로 격리합니다.
 */
public final class UserFixture {

    public static final String DEFAULT_UUID = "550e8400-e29b-41d4-a716-446655440000";
    public static final String DEFAULT_EMAIL = "test@example.com";
    public static final String DEFAULT_PASSWORD = "encodedPassword";
    public static final String DEFAULT_NICKNAME = "testNickname";
    public static final String DEFAULT_REAL_NAME = "Test User";

    private UserFixture() {}

    public static User create() {
        return builder().build();
    }

    public static UserBuilder builder() {
        return new UserBuilder();
    }

    public static class UserBuilder {
        private Long id;
        private String uuid = DEFAULT_UUID;
        private String email = DEFAULT_EMAIL;
        private String password = DEFAULT_PASSWORD;
        private UserStatus status = UserStatus.ACTIVE;
        private String nickname = DEFAULT_NICKNAME;
        private String realName = DEFAULT_REAL_NAME;
        private String username;
        private String phoneNumber;
        private String bio;
        private String profileImageUrl;
        private boolean marketingAgree = false;
        private boolean withProfile = true;

        public UserBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public UserBuilder uuid(String uuid) {
            this.uuid = uuid;
            return this;
        }

        public UserBuilder email(String email) {
            this.email = email;
            return this;
        }

        public UserBuilder password(String password) {
            this.password = password;
            return this;
        }

        public UserBuilder status(UserStatus status) {
            this.status = status;
            return this;
        }

        public UserBuilder nickname(String nickname) {
            this.nickname = nickname;
            return this;
        }

        public UserBuilder realName(String realName) {
            this.realName = realName;
            return this;
        }

        public UserBuilder username(String username) {
            this.username = username;
            return this;
        }

        public UserBuilder phoneNumber(String phoneNumber) {
            this.phoneNumber = phoneNumber;
            return this;
        }

        public UserBuilder bio(String bio) {
            this.bio = bio;
            return this;
        }

        public UserBuilder profileImageUrl(String profileImageUrl) {
            this.profileImageUrl = profileImageUrl;
            return this;
        }

        public UserBuilder marketingAgree(boolean marketingAgree) {
            this.marketingAgree = marketingAgree;
            return this;
        }

        public UserBuilder withoutProfile() {
            this.withProfile = false;
            return this;
        }

        public User build() {
            User user = new User(email, password);
            ReflectionTestUtils.setField(user, "uuid", uuid);
            if (id != null) {
                ReflectionTestUtils.setField(user, "id", id);
            }
            if (status != UserStatus.ACTIVE) {
                ReflectionTestUtils.setField(user, "status", status);
            }
            if (withProfile) {
                UserProfile profile = new UserProfile(user, nickname, realName, marketingAgree);
                if (username != null) {
                    profile.setUsername(username);
                }
                if (phoneNumber != null) {
                    profile.updatePhoneNumber(phoneNumber);
                }
                if (bio != null) {
                    profile.updateBio(bio);
                }
                if (profileImageUrl != null) {
                    profile.updateProfileImageUrl(profileImageUrl);
                }
                user.setProfile(profile);
            }
            return user;
        }
    }

    /**
     * 소셜 로그인 사용자를 생성합니다 (password=null, SocialAccount 연결).
     */
    public static User createSocialUser() {
        return createSocialUser(SocialProvider.GOOGLE, "google-oauth2-id-123");
    }

    public static User createSocialUser(SocialProvider provider, String providerId) {
        User user = builder().password(null).build();
        user.getSocialAccounts().add(new SocialAccount(user, provider, providerId));
        return user;
    }

    /**
     * 비활성 상태의 사용자를 생성합니다.
     */
    public static User createBannedUser() {
        return builder().status(UserStatus.BANNED).build();
    }

    public static User createDormantUser() {
        return builder().status(UserStatus.DORMANT).build();
    }

    public static User createWithdrawalPendingUser() {
        return builder().status(UserStatus.WITHDRAWAL_PENDING).build();
    }
}

package com.portal.universe.authservice.oauth2;

import com.portal.universe.authservice.domain.Role;
import com.portal.universe.authservice.domain.SocialAccount;
import com.portal.universe.authservice.domain.SocialProvider;
import com.portal.universe.authservice.domain.User;
import com.portal.universe.authservice.domain.UserProfile;
import com.portal.universe.authservice.repository.SocialAccountRepository;
import com.portal.universe.authservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * OAuth2 소셜 로그인 후 사용자 정보를 처리하는 서비스입니다.
 * 소셜 로그인 제공자로부터 사용자 정보를 받아
 * 기존 사용자 조회 또는 신규 사용자 생성을 처리합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final SocialAccountRepository socialAccountRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        String userNameAttributeName = userRequest.getClientRegistration()
                .getProviderDetails()
                .getUserInfoEndpoint()
                .getUserNameAttributeName();

        log.debug("OAuth2 로그인 시도 - provider: {}, userNameAttribute: {}", registrationId, userNameAttributeName);

        OAuth2UserInfo userInfo = OAuth2UserInfoFactory.getOAuth2UserInfo(registrationId, oAuth2User.getAttributes());

        User user = processOAuth2User(registrationId, userInfo);

        return new CustomOAuth2User(user, oAuth2User.getAttributes(), userNameAttributeName);
    }

    /**
     * OAuth2 사용자 정보를 처리하여 User 엔티티를 반환합니다.
     * 1. 기존 소셜 계정이 있으면 해당 사용자 반환
     * 2. 동일 이메일의 사용자가 있으면 소셜 계정 연동
     * 3. 신규 사용자면 User + SocialAccount 생성
     */
    private User processOAuth2User(String registrationId, OAuth2UserInfo userInfo) {
        SocialProvider provider = SocialProvider.valueOf(registrationId.toUpperCase());
        String providerId = userInfo.getId();
        String email = userInfo.getEmail();

        // 1. 기존 소셜 계정으로 가입된 사용자 확인
        Optional<SocialAccount> existingSocialAccount = socialAccountRepository
                .findByProviderAndProviderId(provider, providerId);

        if (existingSocialAccount.isPresent()) {
            log.debug("기존 소셜 계정으로 로그인 - provider: {}, email: {}", provider, email);
            return existingSocialAccount.get().getUser();
        }

        // 2. 동일 이메일로 가입된 사용자가 있는지 확인
        Optional<User> existingUser = userRepository.findByEmail(email);

        if (existingUser.isPresent()) {
            // 기존 사용자에게 소셜 계정 연동
            User user = existingUser.get();
            log.debug("기존 사용자에 소셜 계정 연동 - provider: {}, email: {}", provider, email);
            linkSocialAccount(user, provider, providerId);
            return user;
        }

        // 3. 신규 사용자 생성
        log.debug("신규 소셜 사용자 생성 - provider: {}, email: {}", provider, email);
        return createNewUser(provider, providerId, userInfo);
    }

    /**
     * 기존 사용자에게 새로운 소셜 계정을 연동합니다.
     */
    private void linkSocialAccount(User user, SocialProvider provider, String providerId) {
        SocialAccount socialAccount = new SocialAccount(user, provider, providerId);
        user.getSocialAccounts().add(socialAccount);
        userRepository.save(user);
    }

    /**
     * 신규 사용자와 소셜 계정, 프로필을 생성합니다.
     */
    private User createNewUser(SocialProvider provider, String providerId, OAuth2UserInfo userInfo) {
        // User 생성 (password는 null - 소셜 로그인 사용자)
        User user = new User(userInfo.getEmail(), null, Role.USER);

        // UserProfile 생성
        String nickname = userInfo.getName() != null ? userInfo.getName() : "User_" + providerId.substring(0, 8);
        UserProfile profile = new UserProfile(user, nickname, userInfo.getImageUrl());
        user.setProfile(profile);

        // SocialAccount 생성
        SocialAccount socialAccount = new SocialAccount(user, provider, providerId);
        user.getSocialAccounts().add(socialAccount);

        return userRepository.save(user);
    }
}

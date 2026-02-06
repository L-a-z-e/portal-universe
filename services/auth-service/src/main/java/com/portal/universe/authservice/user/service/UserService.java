package com.portal.universe.authservice.user.service;

import com.portal.universe.authservice.common.exception.AuthErrorCode;
import com.portal.universe.authservice.user.dto.SignupCommand;
import com.portal.universe.authservice.user.dto.UserProfileResponse;
import com.portal.universe.authservice.user.domain.User;
import com.portal.universe.authservice.user.domain.UserProfile;
import com.portal.universe.authservice.follow.repository.FollowRepository;
import com.portal.universe.authservice.password.PasswordValidator;
import com.portal.universe.authservice.password.ValidationResult;
import com.portal.universe.authservice.password.domain.PasswordHistory;
import com.portal.universe.authservice.password.repository.PasswordHistoryRepository;
import com.portal.universe.authservice.user.repository.UserRepository;
import com.portal.universe.authservice.auth.service.RbacInitializationService;
import com.portal.universe.event.auth.UserSignedUpEvent;
import com.portal.universe.commonlibrary.exception.CustomBusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final FollowRepository followRepository;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;
    private final PasswordValidator passwordValidator;
    private final PasswordHistoryRepository passwordHistoryRepository;
    private final RbacInitializationService rbacInitializationService;

    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-z0-9_]{3,20}$");

    /**
     * 일반 이메일 회원가입 처리
     */
    @Transactional
    public Long registerUser(SignupCommand command) {
        // 1. 이메일 중복 확인
        if (userRepository.findByEmail(command.email()).isPresent()) {
            throw new CustomBusinessException(AuthErrorCode.EMAIL_ALREADY_EXISTS);
        }

        // 2. 비밀번호 정책 검증
        ValidationResult validationResult = passwordValidator.validate(command.password());
        if (!validationResult.isValid()) {
            String allErrors = String.join("; ", validationResult.getErrors());
            throw new CustomBusinessException(AuthErrorCode.PASSWORD_TOO_WEAK, allErrors);
        }

        // 3. 비밀번호 암호화
        String encodedPassword = passwordEncoder.encode(command.password());

        // 4. User 엔티티 생성
        User newUser = new User(command.email(), encodedPassword);

        // 5. UserProfile 엔티티 생성 및 연결
        UserProfile profile = new UserProfile(
                newUser,
                command.nickname(),
                command.realName(),
                command.marketingAgree()
        );
        newUser.setProfile(profile);

        // 6. 저장 (Cascade로 인해 Profile도 함께 저장)
        User savedUser = userRepository.save(newUser);

        // 7. 비밀번호 히스토리 저장
        savePasswordHistory(savedUser.getId(), encodedPassword);

        // 8. RBAC 초기화 (ROLE_USER + FREE 멤버십)
        rbacInitializationService.initializeNewUser(savedUser.getUuid());

        // 9. 이벤트 발행 (트랜잭션 커밋 후 Kafka로 전송됨)
        UserSignedUpEvent event = new UserSignedUpEvent(
                savedUser.getUuid(),
                savedUser.getEmail(),
                savedUser.getProfile().getNickname()
        );
        eventPublisher.publishEvent(event);

        return savedUser.getId();
    }

    /**
     * Username으로 사용자 프로필 조회 (공개)
     */
    public UserProfileResponse getProfileByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new CustomBusinessException(AuthErrorCode.USER_NOT_FOUND));

        FollowCounts counts = getFollowCounts(user);
        return UserProfileResponse.from(user, counts.followerCount(), counts.followingCount());
    }

    /**
     * 내 프로필 조회 (UUID 기반)
     */
    public UserProfileResponse getMyProfileByUuid(String uuid) {
        User user = findUserByUuidOrThrow(uuid);
        FollowCounts counts = getFollowCounts(user);

        return UserProfileResponse.from(user, counts.followerCount(), counts.followingCount());
    }

    /**
     * 프로필 수정 (UUID 기반)
     */
    @Transactional
    public UserProfileResponse updateProfileByUuid(String uuid, String nickname, String bio,
                                                   String profileImageUrl, String website) {
        User user = findUserByUuidOrThrow(uuid);
        user.getProfile().updateProfile(nickname, bio, profileImageUrl, website);

        FollowCounts counts = getFollowCounts(user);
        return UserProfileResponse.from(user, counts.followerCount(), counts.followingCount());
    }

    /**
     * Username 설정 (UUID 기반, 최초 1회)
     */
    @Transactional
    public UserProfileResponse setUsernameByUuid(String uuid, String username) {
        if (!USERNAME_PATTERN.matcher(username).matches()) {
            throw new CustomBusinessException(AuthErrorCode.INVALID_USERNAME_FORMAT);
        }

        User user = findUserByUuidOrThrow(uuid);

        if (user.getProfile().getUsername() != null) {
            throw new CustomBusinessException(AuthErrorCode.USERNAME_ALREADY_SET);
        }

        if (userRepository.existsByUsername(username)) {
            throw new CustomBusinessException(AuthErrorCode.USERNAME_ALREADY_EXISTS);
        }

        user.getProfile().setUsername(username);

        FollowCounts counts = getFollowCounts(user);
        return UserProfileResponse.from(user, counts.followerCount(), counts.followingCount());
    }

    /**
     * Username 중복 확인
     */
    public boolean checkUsernameAvailability(String username) {
        // Username 형식 검증
        if (!USERNAME_PATTERN.matcher(username).matches()) {
            throw new CustomBusinessException(AuthErrorCode.INVALID_USERNAME_FORMAT);
        }

        return !userRepository.existsByUsername(username);
    }

    /**
     * 비밀번호 변경 (UUID 기반)
     */
    @Transactional
    public void changePasswordByUuid(String uuid, String currentPassword, String newPassword) {
        User user = findUserByUuidOrThrow(uuid);
        doChangePassword(user, currentPassword, newPassword);
    }

    private void doChangePassword(User user, String currentPassword, String newPassword) {
        if (user.isSocialUser()) {
            throw new CustomBusinessException(AuthErrorCode.SOCIAL_USER_CANNOT_CHANGE_PASSWORD);
        }

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new CustomBusinessException(AuthErrorCode.INVALID_CURRENT_PASSWORD);
        }

        ValidationResult validationResult = passwordValidator.validate(newPassword, user);
        if (!validationResult.isValid()) {
            String allErrors = String.join("; ", validationResult.getErrors());
            throw new CustomBusinessException(AuthErrorCode.PASSWORD_TOO_WEAK, allErrors);
        }

        String encodedPassword = passwordEncoder.encode(newPassword);
        user.changePassword(encodedPassword);

        savePasswordHistory(user.getId(), encodedPassword);
    }

    // ==================== Private Helper Methods ====================

    private User findUserByUuidOrThrow(String uuid) {
        return userRepository.findByUuid(uuid)
                .orElseThrow(() -> new CustomBusinessException(AuthErrorCode.USER_NOT_FOUND));
    }

    /**
     * 사용자의 팔로워/팔로잉 카운트를 담은 DTO를 생성합니다.
     *
     * <p>팔로우 카운트 조회 로직이 여러 메서드에서 반복되어 추출했습니다.
     * 나중에 캐싱이나 최적화가 필요할 때 이 메서드만 수정하면 됩니다.</p>
     *
     * @param user 조회 대상 사용자
     * @return 팔로워 수와 팔로잉 수를 담은 레코드
     */
    private FollowCounts getFollowCounts(User user) {
        int followerCount = (int) followRepository.countByFollowing(user);
        int followingCount = (int) followRepository.countByFollower(user);
        return new FollowCounts(followerCount, followingCount);
    }

    /**
     * 팔로워/팔로잉 카운트를 담는 불변 데이터 클래스
     */
    private record FollowCounts(int followerCount, int followingCount) {}

    /**
     * 비밀번호 히스토리를 저장합니다.
     * 정책에 따라 최근 N개의 비밀번호만 유지합니다.
     */
    private void savePasswordHistory(Long userId, String encodedPassword) {
        PasswordHistory history = PasswordHistory.create(userId, encodedPassword);
        passwordHistoryRepository.save(history);
    }
}

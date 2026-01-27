package com.portal.universe.authservice.user.service;

import com.portal.universe.authservice.common.exception.AuthErrorCode;
import com.portal.universe.authservice.user.dto.UserProfileResponse;
import com.portal.universe.authservice.user.domain.Role;
import com.portal.universe.authservice.user.domain.User;
import com.portal.universe.authservice.user.domain.UserProfile;
import com.portal.universe.authservice.follow.repository.FollowRepository;
import com.portal.universe.authservice.password.PasswordValidator;
import com.portal.universe.authservice.password.ValidationResult;
import com.portal.universe.authservice.password.domain.PasswordHistory;
import com.portal.universe.authservice.password.repository.PasswordHistoryRepository;
import com.portal.universe.authservice.user.repository.UserRepository;
import com.portal.universe.common.event.UserSignedUpEvent;
import com.portal.universe.commonlibrary.exception.CustomBusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
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
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final PasswordValidator passwordValidator;
    private final PasswordHistoryRepository passwordHistoryRepository;

    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-z0-9_]{3,20}$");

    /**
     * 회원가입 요청 DTO
     * (Controller와 공유하거나 별도로 분리할 수 있음. 현재는 Service 내부에 정의하거나 별도 패키지로 분리 권장)
     */
    public record SignupCommand(String email, String password, String nickname, String realName, boolean marketingAgree) {}

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
            throw new CustomBusinessException(AuthErrorCode.PASSWORD_TOO_WEAK,
                    validationResult.getFirstError());
        }

        // 3. 비밀번호 암호화
        String encodedPassword = passwordEncoder.encode(command.password());

        // 4. User 엔티티 생성
        User newUser = new User(command.email(), encodedPassword, Role.USER);

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

        // 8. 이벤트 발행
        // 주의: 트랜잭션 커밋 후 발행하는 것이 안전함 (TransactionalEventListener 고려 가능)
        // 현재는 단순성을 위해 직접 발행
        UserSignedUpEvent event = new UserSignedUpEvent(
                savedUser.getUuid(), // UUID 사용
                savedUser.getEmail(),
                savedUser.getProfile().getNickname() // Name 대신 Nickname 사용
        );
        kafkaTemplate.send("user-signup", event);

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
     * 내 프로필 조회
     */
    public UserProfileResponse getMyProfile(Long userId) {
        User user = findUserByIdOrThrow(userId);
        FollowCounts counts = getFollowCounts(user);

        return UserProfileResponse.from(user, counts.followerCount(), counts.followingCount());
    }

    /**
     * 프로필 수정
     */
    @Transactional
    public UserProfileResponse updateProfile(Long userId, String nickname, String bio,
                                            String profileImageUrl, String website) {
        User user = findUserByIdOrThrow(userId);
        user.getProfile().updateProfile(nickname, bio, profileImageUrl, website);

        FollowCounts counts = getFollowCounts(user);
        return UserProfileResponse.from(user, counts.followerCount(), counts.followingCount());
    }

    /**
     * Username 설정 (최초 1회)
     */
    @Transactional
    public UserProfileResponse setUsername(Long userId, String username) {
        // Username 형식 검증
        if (!USERNAME_PATTERN.matcher(username).matches()) {
            throw new CustomBusinessException(AuthErrorCode.INVALID_USERNAME_FORMAT);
        }

        User user = findUserByIdOrThrow(userId);

        // 이미 설정된 경우
        if (user.getProfile().getUsername() != null) {
            throw new CustomBusinessException(AuthErrorCode.USERNAME_ALREADY_SET);
        }

        // 중복 확인
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
     * 비밀번호 변경
     */
    @Transactional
    public void changePassword(Long userId, String currentPassword, String newPassword) {
        User user = findUserByIdOrThrow(userId);

        // 1. 소셜 로그인 사용자 확인
        if (user.isSocialUser()) {
            throw new CustomBusinessException(AuthErrorCode.SOCIAL_USER_CANNOT_CHANGE_PASSWORD);
        }

        // 2. 현재 비밀번호 확인
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new CustomBusinessException(AuthErrorCode.INVALID_CURRENT_PASSWORD);
        }

        // 3. 새 비밀번호 정책 검증 (사용자 정보 포함 + 이전 비밀번호 재사용 체크)
        ValidationResult validationResult = passwordValidator.validate(newPassword, user);
        if (!validationResult.isValid()) {
            throw new CustomBusinessException(AuthErrorCode.PASSWORD_TOO_WEAK,
                    validationResult.getFirstError());
        }

        // 4. 비밀번호 변경
        String encodedPassword = passwordEncoder.encode(newPassword);
        user.changePassword(encodedPassword);

        // 5. 비밀번호 히스토리 저장
        savePasswordHistory(userId, encodedPassword);
    }

    // ==================== Private Helper Methods ====================

    /**
     * ID로 사용자를 조회하고, 없으면 예외를 발생시킵니다.
     *
     * <p>이 메서드는 반복되는 "조회 후 예외 처리" 패턴을 추출한 것입니다.
     * 여러 메서드에서 동일한 로직이 중복되면 버그 수정이나 변경 시
     * 모든 위치를 찾아 수정해야 하는 문제가 발생합니다.</p>
     *
     * @param userId 조회할 사용자 ID
     * @return 조회된 User 엔티티
     * @throws CustomBusinessException 사용자를 찾을 수 없는 경우
     */
    private User findUserByIdOrThrow(Long userId) {
        return userRepository.findById(userId)
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

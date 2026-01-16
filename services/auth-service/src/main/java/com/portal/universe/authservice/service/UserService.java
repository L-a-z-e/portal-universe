package com.portal.universe.authservice.service;

import com.portal.universe.authservice.domain.Role;
import com.portal.universe.authservice.domain.User;
import com.portal.universe.authservice.domain.UserProfile;
import com.portal.universe.authservice.exception.AuthErrorCode;
import com.portal.universe.authservice.repository.UserRepository;
import com.portal.universe.common.event.UserSignedUpEvent;
import com.portal.universe.commonlibrary.exception.CustomBusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final KafkaTemplate<String, Object> kafkaTemplate;

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

        // 2. 비밀번호 암호화
        String encodedPassword = passwordEncoder.encode(command.password());

        // 3. User 엔티티 생성
        User newUser = new User(command.email(), encodedPassword, Role.USER);

        // 4. UserProfile 엔티티 생성 및 연결
        UserProfile profile = new UserProfile(
                newUser,
                command.nickname(),
                command.realName(),
                command.marketingAgree()
        );
        newUser.setProfile(profile);

        // 5. 저장 (Cascade로 인해 Profile도 함께 저장)
        User savedUser = userRepository.save(newUser);

        // 6. 이벤트 발행
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
}

package com.portal.universe.authservice.follow.service;

import com.portal.universe.authservice.common.exception.AuthErrorCode;
import com.portal.universe.authservice.follow.domain.Follow;
import com.portal.universe.authservice.follow.dto.FollowResponse;
import com.portal.universe.authservice.follow.dto.FollowStatusResponse;
import com.portal.universe.authservice.follow.dto.FollowingIdsResponse;
import com.portal.universe.authservice.follow.repository.FollowRepository;
import com.portal.universe.authservice.user.domain.User;
import com.portal.universe.authservice.user.domain.UserProfile;
import com.portal.universe.authservice.user.repository.UserRepository;
import com.portal.universe.commonlibrary.exception.CustomBusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FollowService 테스트")
class FollowServiceTest {

    @Mock
    private FollowRepository followRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private FollowService followService;

    private User createUser(Long id, String uuid, String email, String username) {
        User user = new User(email, "password");
        try {
            var idField = User.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(user, id);
            var uuidField = User.class.getDeclaredField("uuid");
            uuidField.setAccessible(true);
            uuidField.set(user, uuid);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        UserProfile profile = new UserProfile(user, "nick-" + id, "Real Name", false);
        if (username != null) {
            profile.setUsername(username);
        }
        user.setProfile(profile);
        return user;
    }

    @Nested
    @DisplayName("toggleFollow - ID based")
    class ToggleFollowById {

        @Test
        @DisplayName("should_follow_when_notYetFollowing")
        void should_follow_when_notYetFollowing() {
            // given
            User currentUser = createUser(1L, "uuid-1", "user1@test.com", "user1");
            User targetUser = createUser(2L, "uuid-2", "user2@test.com", "user2");

            when(userRepository.findById(1L)).thenReturn(Optional.of(currentUser));
            when(userRepository.findByUsername("user2")).thenReturn(Optional.of(targetUser));
            when(followRepository.existsByFollowerAndFollowing(currentUser, targetUser)).thenReturn(false);
            when(followRepository.save(any(Follow.class))).thenReturn(new Follow(currentUser, targetUser));
            when(followRepository.countByFollowing(targetUser)).thenReturn(1L);
            when(followRepository.countByFollower(targetUser)).thenReturn(0L);

            // when
            FollowResponse result = followService.toggleFollow(1L, "user2");

            // then
            assertThat(result.following()).isTrue();
            verify(followRepository).save(any(Follow.class));
        }

        @Test
        @DisplayName("should_unfollow_when_alreadyFollowing")
        void should_unfollow_when_alreadyFollowing() {
            // given
            User currentUser = createUser(1L, "uuid-1", "user1@test.com", "user1");
            User targetUser = createUser(2L, "uuid-2", "user2@test.com", "user2");
            Follow follow = new Follow(currentUser, targetUser);

            when(userRepository.findById(1L)).thenReturn(Optional.of(currentUser));
            when(userRepository.findByUsername("user2")).thenReturn(Optional.of(targetUser));
            when(followRepository.existsByFollowerAndFollowing(currentUser, targetUser)).thenReturn(true);
            when(followRepository.findByFollowerAndFollowing(currentUser, targetUser))
                    .thenReturn(Optional.of(follow));
            when(followRepository.countByFollowing(targetUser)).thenReturn(0L);
            when(followRepository.countByFollower(targetUser)).thenReturn(0L);

            // when
            FollowResponse result = followService.toggleFollow(1L, "user2");

            // then
            assertThat(result.following()).isFalse();
            verify(followRepository).delete(follow);
        }

        @Test
        @DisplayName("should_throwException_when_followYourself")
        void should_throwException_when_followYourself() {
            // given
            User user = createUser(1L, "uuid-1", "user1@test.com", "user1");

            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(userRepository.findByUsername("user1")).thenReturn(Optional.of(user));

            // when & then
            assertThatThrownBy(() -> followService.toggleFollow(1L, "user1"))
                    .isInstanceOf(CustomBusinessException.class)
                    .satisfies(ex -> {
                        CustomBusinessException cbe = (CustomBusinessException) ex;
                        assertThat(cbe.getErrorCode()).isEqualTo(AuthErrorCode.CANNOT_FOLLOW_YOURSELF);
                    });
        }

        @Test
        @DisplayName("should_throwException_when_targetUserNotFound")
        void should_throwException_when_targetUserNotFound() {
            // given
            User currentUser = createUser(1L, "uuid-1", "user1@test.com", "user1");
            when(userRepository.findById(1L)).thenReturn(Optional.of(currentUser));
            when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> followService.toggleFollow(1L, "unknown"))
                    .isInstanceOf(CustomBusinessException.class)
                    .satisfies(ex -> {
                        CustomBusinessException cbe = (CustomBusinessException) ex;
                        assertThat(cbe.getErrorCode()).isEqualTo(AuthErrorCode.FOLLOW_USER_NOT_FOUND);
                    });
        }
    }

    @Nested
    @DisplayName("toggleFollowByUuid")
    class ToggleFollowByUuid {

        @Test
        @DisplayName("should_follow_when_calledWithUuid")
        void should_follow_when_calledWithUuid() {
            // given
            User currentUser = createUser(1L, "uuid-1", "user1@test.com", "user1");
            User targetUser = createUser(2L, "uuid-2", "user2@test.com", "user2");

            when(userRepository.findByUuid("uuid-1")).thenReturn(Optional.of(currentUser));
            when(userRepository.findByUsername("user2")).thenReturn(Optional.of(targetUser));
            when(followRepository.existsByFollowerAndFollowing(currentUser, targetUser)).thenReturn(false);
            when(followRepository.save(any(Follow.class))).thenReturn(new Follow(currentUser, targetUser));
            when(followRepository.countByFollowing(targetUser)).thenReturn(1L);
            when(followRepository.countByFollower(targetUser)).thenReturn(0L);

            // when
            FollowResponse result = followService.toggleFollowByUuid("uuid-1", "user2");

            // then
            assertThat(result.following()).isTrue();
        }
    }

    @Nested
    @DisplayName("getFollowStatus")
    class GetFollowStatus {

        @Test
        @DisplayName("should_returnFollowing_when_isFollowing")
        void should_returnFollowing_when_isFollowing() {
            // given
            User currentUser = createUser(1L, "uuid-1", "user1@test.com", "user1");
            User targetUser = createUser(2L, "uuid-2", "user2@test.com", "user2");

            when(userRepository.findById(1L)).thenReturn(Optional.of(currentUser));
            when(userRepository.findByUsername("user2")).thenReturn(Optional.of(targetUser));
            when(followRepository.existsByFollowerAndFollowing(currentUser, targetUser)).thenReturn(true);

            // when
            FollowStatusResponse result = followService.getFollowStatus(1L, "user2");

            // then
            assertThat(result.isFollowing()).isTrue();
        }

        @Test
        @DisplayName("should_returnNotFollowing_when_notFollowing")
        void should_returnNotFollowing_when_notFollowing() {
            // given
            User currentUser = createUser(1L, "uuid-1", "user1@test.com", "user1");
            User targetUser = createUser(2L, "uuid-2", "user2@test.com", "user2");

            when(userRepository.findById(1L)).thenReturn(Optional.of(currentUser));
            when(userRepository.findByUsername("user2")).thenReturn(Optional.of(targetUser));
            when(followRepository.existsByFollowerAndFollowing(currentUser, targetUser)).thenReturn(false);

            // when
            FollowStatusResponse result = followService.getFollowStatus(1L, "user2");

            // then
            assertThat(result.isFollowing()).isFalse();
        }
    }

    @Nested
    @DisplayName("getMyFollowingIds")
    class GetMyFollowingIds {

        @Test
        @DisplayName("should_returnFollowingIds_when_userHasFollowings")
        void should_returnFollowingIds_when_userHasFollowings() {
            // given
            when(followRepository.findFollowingUuidsByFollowerId(1L))
                    .thenReturn(List.of("uuid-2", "uuid-3"));

            // when
            FollowingIdsResponse result = followService.getMyFollowingIds(1L);

            // then
            assertThat(result.followingIds()).containsExactly("uuid-2", "uuid-3");
        }
    }

    @Nested
    @DisplayName("getFollowCounts")
    class GetFollowCounts {

        @Test
        @DisplayName("should_returnCounts_when_usernameExists")
        void should_returnCounts_when_usernameExists() {
            // given
            User user = createUser(1L, "uuid-1", "user@test.com", "user1");
            when(userRepository.findByUsername("user1")).thenReturn(Optional.of(user));
            when(followRepository.countByFollowing(user)).thenReturn(10L);
            when(followRepository.countByFollower(user)).thenReturn(5L);

            // when
            int[] counts = followService.getFollowCounts("user1");

            // then
            assertThat(counts[0]).isEqualTo(10);
            assertThat(counts[1]).isEqualTo(5);
        }

        @Test
        @DisplayName("should_throwException_when_usernameNotFound")
        void should_throwException_when_usernameNotFound() {
            // given
            when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> followService.getFollowCounts("unknown"))
                    .isInstanceOf(CustomBusinessException.class)
                    .satisfies(ex -> {
                        CustomBusinessException cbe = (CustomBusinessException) ex;
                        assertThat(cbe.getErrorCode()).isEqualTo(AuthErrorCode.FOLLOW_USER_NOT_FOUND);
                    });
        }
    }
}

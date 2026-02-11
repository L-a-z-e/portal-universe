package com.portal.universe.authservice.follow.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portal.universe.authservice.follow.dto.FollowListResponse;
import com.portal.universe.authservice.follow.dto.FollowResponse;
import com.portal.universe.authservice.follow.dto.FollowStatusResponse;
import com.portal.universe.authservice.follow.dto.FollowUserResponse;
import com.portal.universe.authservice.follow.dto.FollowingIdsResponse;
import com.portal.universe.authservice.follow.service.FollowService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FollowController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FollowController Unit Test")
class FollowControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    FollowService followService;

    private static final String USER_UUID = "test-user-uuid";
    private static final String BASE_URL = "/api/v1/users";

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(USER_UUID, null, Collections.emptyList())
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("POST /api/v1/users/{username}/follow")
    class ToggleFollow {

        @Test
        @DisplayName("should_returnFollowResponse_when_followToggled")
        void should_returnFollowResponse_when_followToggled() throws Exception {
            // given
            FollowResponse response = new FollowResponse(true, 11, 5);
            when(followService.toggleFollowByUuid(USER_UUID, "targetuser")).thenReturn(response);

            // when & then
            mockMvc.perform(post(BASE_URL + "/targetuser/follow"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.following").value(true))
                    .andExpect(jsonPath("$.data.followerCount").value(11))
                    .andExpect(jsonPath("$.data.followingCount").value(5));
        }

        @Test
        @DisplayName("should_returnUnfollowResponse_when_alreadyFollowing")
        void should_returnUnfollowResponse_when_alreadyFollowing() throws Exception {
            // given
            FollowResponse response = new FollowResponse(false, 9, 5);
            when(followService.toggleFollowByUuid(USER_UUID, "targetuser")).thenReturn(response);

            // when & then
            mockMvc.perform(post(BASE_URL + "/targetuser/follow"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.following").value(false))
                    .andExpect(jsonPath("$.data.followerCount").value(9));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/users/{username}/followers")
    class GetFollowers {

        @Test
        @DisplayName("should_returnFollowerList_when_requested")
        void should_returnFollowerList_when_requested() throws Exception {
            // given
            FollowUserResponse user = new FollowUserResponse(
                    "follower-uuid", "follower1", "Follower", "https://img.test.com/pic.jpg", "Bio"
            );
            FollowListResponse response = new FollowListResponse(
                    List.of(user), 0, 20, 1, 1, false
            );
            when(followService.getFollowers("targetuser", 0, 20)).thenReturn(response);

            // when & then
            mockMvc.perform(get(BASE_URL + "/targetuser/followers")
                            .param("page", "0")
                            .param("size", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.users").isArray())
                    .andExpect(jsonPath("$.data.users[0].username").value("follower1"))
                    .andExpect(jsonPath("$.data.totalElements").value(1));
        }

        @Test
        @DisplayName("should_returnEmptyList_when_noFollowers")
        void should_returnEmptyList_when_noFollowers() throws Exception {
            // given
            FollowListResponse response = new FollowListResponse(
                    List.of(), 0, 20, 0, 0, false
            );
            when(followService.getFollowers("lonelyuser", 0, 20)).thenReturn(response);

            // when & then
            mockMvc.perform(get(BASE_URL + "/lonelyuser/followers"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.users").isEmpty())
                    .andExpect(jsonPath("$.data.totalElements").value(0));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/users/{username}/following")
    class GetFollowings {

        @Test
        @DisplayName("should_returnFollowingList_when_requested")
        void should_returnFollowingList_when_requested() throws Exception {
            // given
            FollowUserResponse user = new FollowUserResponse(
                    "following-uuid", "following1", "Following", "https://img.test.com/pic.jpg", "Bio"
            );
            FollowListResponse response = new FollowListResponse(
                    List.of(user), 0, 20, 1, 1, false
            );
            when(followService.getFollowings("testuser", 0, 20)).thenReturn(response);

            // when & then
            mockMvc.perform(get(BASE_URL + "/testuser/following")
                            .param("page", "0")
                            .param("size", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.users[0].username").value("following1"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/users/me/following/ids")
    class GetMyFollowingIds {

        @Test
        @DisplayName("should_returnFollowingIds_when_authenticated")
        void should_returnFollowingIds_when_authenticated() throws Exception {
            // given
            FollowingIdsResponse response = new FollowingIdsResponse(
                    List.of("uuid-1", "uuid-2", "uuid-3")
            );
            when(followService.getMyFollowingIdsByUuid(USER_UUID)).thenReturn(response);

            // when & then
            mockMvc.perform(get(BASE_URL + "/me/following/ids"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.followingIds").isArray())
                    .andExpect(jsonPath("$.data.followingIds.length()").value(3));
        }

        @Test
        @DisplayName("should_returnEmptyList_when_notFollowingAnyone")
        void should_returnEmptyList_when_notFollowingAnyone() throws Exception {
            // given
            FollowingIdsResponse response = new FollowingIdsResponse(List.of());
            when(followService.getMyFollowingIdsByUuid(USER_UUID)).thenReturn(response);

            // when & then
            mockMvc.perform(get(BASE_URL + "/me/following/ids"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.followingIds").isEmpty());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/users/{username}/follow/status")
    class GetFollowStatus {

        @Test
        @DisplayName("should_returnTrue_when_following")
        void should_returnTrue_when_following() throws Exception {
            // given
            FollowStatusResponse response = new FollowStatusResponse(true);
            when(followService.getFollowStatusByUuid(USER_UUID, "targetuser")).thenReturn(response);

            // when & then
            mockMvc.perform(get(BASE_URL + "/targetuser/follow/status"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.isFollowing").value(true));
        }

        @Test
        @DisplayName("should_returnFalse_when_notFollowing")
        void should_returnFalse_when_notFollowing() throws Exception {
            // given
            FollowStatusResponse response = new FollowStatusResponse(false);
            when(followService.getFollowStatusByUuid(USER_UUID, "stranger")).thenReturn(response);

            // when & then
            mockMvc.perform(get(BASE_URL + "/stranger/follow/status"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.isFollowing").value(false));
        }
    }
}

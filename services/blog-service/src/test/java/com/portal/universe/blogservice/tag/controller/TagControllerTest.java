package com.portal.universe.blogservice.tag.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portal.universe.blogservice.tag.dto.TagCreateRequest;
import com.portal.universe.blogservice.tag.dto.TagResponse;
import com.portal.universe.blogservice.tag.dto.TagStatsResponse;
import com.portal.universe.blogservice.tag.service.TagService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TagController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("TagController 테스트")
class TagControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TagService tagService;

    @Test
    @DisplayName("POST /tags - should_createTag_when_validRequest")
    void should_createTag_when_validRequest() throws Exception {
        // given
        TagCreateRequest request = new TagCreateRequest("Java", "Java programming language");
        TagResponse response = new TagResponse(
            "tag-1",
            "Java",
            10L,
            "Java programming language",
            LocalDateTime.now(),
            LocalDateTime.now()
        );
        given(tagService.createTag(any(TagCreateRequest.class))).willReturn(response);

        // when & then
        mockMvc.perform(post("/tags")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.id").value("tag-1"))
            .andExpect(jsonPath("$.data.name").value("Java"));

        verify(tagService).createTag(any(TagCreateRequest.class));
    }

    @Test
    @DisplayName("POST /tags - should_returnBadRequest_when_nameIsBlank")
    void should_returnBadRequest_when_nameIsBlank() throws Exception {
        // given
        TagCreateRequest request = new TagCreateRequest("", "Description");

        // when & then
        mockMvc.perform(post("/tags")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /tags - should_returnAllTags")
    void should_returnAllTags() throws Exception {
        // given
        TagResponse response = new TagResponse(
            "tag-1",
            "Java",
            10L,
            "Java programming language",
            LocalDateTime.now(),
            LocalDateTime.now()
        );
        given(tagService.getAllTags()).willReturn(List.of(response));

        // when & then
        mockMvc.perform(get("/tags"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data[0].id").value("tag-1"))
            .andExpect(jsonPath("$.data[0].name").value("Java"));

        verify(tagService).getAllTags();
    }

    @Test
    @DisplayName("GET /tags/{tagName} - should_returnTag")
    void should_returnTag() throws Exception {
        // given
        TagResponse response = new TagResponse(
            "tag-1",
            "Java",
            10L,
            "Java programming language",
            LocalDateTime.now(),
            LocalDateTime.now()
        );
        given(tagService.getTagByName("Java")).willReturn(response);

        // when & then
        mockMvc.perform(get("/tags/Java"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.name").value("Java"));

        verify(tagService).getTagByName("Java");
    }

    @Test
    @DisplayName("GET /tags/popular - should_returnPopularTags")
    void should_returnPopularTags() throws Exception {
        // given
        TagStatsResponse stats = new TagStatsResponse("Java", 50L, 10000L);
        given(tagService.getPopularTags(10)).willReturn(List.of(stats));

        // when & then
        mockMvc.perform(get("/tags/popular")
                .param("limit", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data[0].name").value("Java"))
            .andExpect(jsonPath("$.data[0].postCount").value(50));

        verify(tagService).getPopularTags(10);
    }

    @Test
    @DisplayName("GET /tags/search - should_returnSearchResults")
    void should_returnSearchResults() throws Exception {
        // given
        TagResponse response = new TagResponse(
            "tag-1",
            "Java",
            10L,
            "Java programming language",
            LocalDateTime.now(),
            LocalDateTime.now()
        );
        given(tagService.searchTags("Jav", 5)).willReturn(List.of(response));

        // when & then
        mockMvc.perform(get("/tags/search")
                .param("q", "Jav")
                .param("limit", "5"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data[0].name").value("Java"));

        verify(tagService).searchTags("Jav", 5);
    }

    @Test
    @DisplayName("PATCH /tags/{tagName}/description - should_updateDescription")
    void should_updateDescription() throws Exception {
        // given
        TagResponse response = new TagResponse(
            "tag-1",
            "Java",
            10L,
            "Updated description",
            LocalDateTime.now(),
            LocalDateTime.now()
        );
        given(tagService.updateTagDescription("Java", "Updated description")).willReturn(response);

        // when & then
        mockMvc.perform(patch("/tags/Java/description")
                .param("description", "Updated description"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.description").value("Updated description"));

        verify(tagService).updateTagDescription("Java", "Updated description");
    }

    @Test
    @DisplayName("DELETE /tags/{tagName} - should_deleteTag")
    void should_deleteTag() throws Exception {
        // when & then
        mockMvc.perform(delete("/tags/Java"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").doesNotExist());

        verify(tagService).deleteTag("Java");
    }
}

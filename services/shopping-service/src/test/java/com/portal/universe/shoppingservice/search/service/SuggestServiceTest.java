package com.portal.universe.shoppingservice.search.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SuggestServiceTest {

    @Mock
    private ElasticsearchClient esClient;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ZSetOperations<String, Object> zSetOperations;

    @Mock
    private ListOperations<String, Object> listOperations;

    @InjectMocks
    private SuggestService suggestService;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        lenient().when(redisTemplate.opsForList()).thenReturn(listOperations);
    }

    @Test
    @DisplayName("should_returnEmptyList_when_keywordTooShort")
    void should_returnEmptyList_when_keywordTooShort() {
        // when
        List<String> result = suggestService.suggest("a", 5);

        // then
        assertThat(result).isEmpty();
        verifyNoInteractions(esClient);
    }

    @Test
    @DisplayName("should_returnEmptyList_when_keywordIsNull")
    void should_returnEmptyList_when_keywordIsNull() {
        // when
        List<String> result = suggestService.suggest(null, 5);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("should_returnEmptyList_when_searchFails")
    @SuppressWarnings("unchecked")
    void should_returnEmptyList_when_searchFails() throws IOException {
        // given
        when(esClient.search(any(co.elastic.clients.elasticsearch.core.SearchRequest.class), eq(Void.class)))
                .thenThrow(new IOException("Connection refused"));

        // when
        List<String> result = suggestService.suggest("laptop", 5);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("should_returnPopularKeywords_when_exists")
    void should_returnPopularKeywords_when_exists() {
        // given
        Set<Object> keywords = new LinkedHashSet<>();
        keywords.add("laptop");
        keywords.add("phone");
        keywords.add("tablet");
        when(zSetOperations.reverseRange("search:popular", 0, 9)).thenReturn(keywords);

        // when
        List<String> result = suggestService.getPopularKeywords(10);

        // then
        assertThat(result).containsExactly("laptop", "phone", "tablet");
    }

    @Test
    @DisplayName("should_incrementSearchCount_when_called")
    void should_incrementSearchCount_when_called() {
        // when
        suggestService.incrementSearchCount("laptop");

        // then
        verify(zSetOperations).incrementScore("search:popular", "laptop", 1);
    }

    @Test
    @DisplayName("should_returnRecentKeywords_when_exists")
    void should_returnRecentKeywords_when_exists() {
        // given
        List<Object> keywords = List.of("laptop", "phone");
        when(listOperations.range("search:recent:user-1", 0, 4)).thenReturn(keywords);

        // when
        List<String> result = suggestService.getRecentKeywords("user-1", 5);

        // then
        assertThat(result).containsExactly("laptop", "phone");
    }

    @Test
    @DisplayName("should_addRecentKeyword_when_called")
    void should_addRecentKeyword_when_called() {
        // when
        suggestService.addRecentKeyword("user-1", "laptop");

        // then
        String key = "search:recent:user-1";
        verify(listOperations).remove(key, 0, "laptop");
        verify(listOperations).leftPush(key, "laptop");
        verify(listOperations).trim(key, 0, 19);
        verify(zSetOperations).incrementScore("search:popular", "laptop", 1);
    }

    @Test
    @DisplayName("should_deleteRecentKeyword_when_called")
    void should_deleteRecentKeyword_when_called() {
        // when
        suggestService.deleteRecentKeyword("user-1", "laptop");

        // then
        verify(listOperations).remove("search:recent:user-1", 0, "laptop");
    }

    @Test
    @DisplayName("should_clearRecentKeywords_when_called")
    void should_clearRecentKeywords_when_called() {
        // when
        suggestService.clearRecentKeywords("user-1");

        // then
        verify(redisTemplate).delete("search:recent:user-1");
    }
}

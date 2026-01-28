package com.portal.universe.shoppingservice.search.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.search.CompletionSuggestOption;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class SuggestService {

    private static final String INDEX_NAME = "products";
    private static final String POPULAR_KEY = "search:popular";
    private static final String RECENT_KEY_PREFIX = "search:recent:";

    private final ElasticsearchClient esClient;
    private final RedisTemplate<String, Object> redisTemplate;

    public List<String> suggest(String keyword, int size) {
        if (keyword == null || keyword.length() < 2) {
            return List.of();
        }

        try {
            SearchRequest request = SearchRequest.of(s -> s
                    .index(INDEX_NAME)
                    .suggest(su -> su
                            .suggesters("product-suggest", sg -> sg
                                    .prefix(keyword)
                                    .completion(c -> c
                                            .field("name.suggest")
                                            .size(size)
                                            .skipDuplicates(true)
                                            .fuzzy(f -> f.fuzziness("AUTO"))
                                    )
                            )
                    )
            );

            var response = esClient.search(request, Void.class);

            List<String> suggestions = new ArrayList<>();
            if (response.suggest() != null && response.suggest().get("product-suggest") != null) {
                for (var suggestion : response.suggest().get("product-suggest")) {
                    if (suggestion.isCompletion()) {
                        for (CompletionSuggestOption<Void> option : suggestion.completion().options()) {
                            suggestions.add(option.text());
                        }
                    }
                }
            }

            return suggestions;

        } catch (IOException e) {
            log.error("Failed to get suggestions for keyword: {}", keyword, e);
            return List.of();
        }
    }

    public List<String> getPopularKeywords(int size) {
        Set<Object> keywords = redisTemplate.opsForZSet().reverseRange(POPULAR_KEY, 0, size - 1);
        if (keywords == null) {
            return List.of();
        }
        return keywords.stream()
                .map(Object::toString)
                .toList();
    }

    public void incrementSearchCount(String keyword) {
        redisTemplate.opsForZSet().incrementScore(POPULAR_KEY, keyword, 1);
    }

    public List<String> getRecentKeywords(String userId, int size) {
        String key = RECENT_KEY_PREFIX + userId;
        List<Object> keywords = redisTemplate.opsForList().range(key, 0, size - 1);
        if (keywords == null) {
            return List.of();
        }
        return keywords.stream()
                .map(Object::toString)
                .toList();
    }

    public void addRecentKeyword(String userId, String keyword) {
        String key = RECENT_KEY_PREFIX + userId;
        // Remove existing duplicate
        redisTemplate.opsForList().remove(key, 0, keyword);
        // Add to front
        redisTemplate.opsForList().leftPush(key, keyword);
        // Keep only last 20
        redisTemplate.opsForList().trim(key, 0, 19);

        // Also increment popular count
        incrementSearchCount(keyword);
    }

    public void deleteRecentKeyword(String userId, String keyword) {
        String key = RECENT_KEY_PREFIX + userId;
        redisTemplate.opsForList().remove(key, 0, keyword);
    }

    public void clearRecentKeywords(String userId) {
        String key = RECENT_KEY_PREFIX + userId;
        redisTemplate.delete(key);
    }
}

package com.portal.universe.blogservice.support.fixture;

import com.portal.universe.blogservice.series.domain.Series;

import java.util.ArrayList;
import java.util.List;

public final class SeriesFixture {

    private SeriesFixture() {}

    public static Series create() {
        return builder().build();
    }

    public static SeriesBuilder builder() {
        return new SeriesBuilder();
    }

    public static class SeriesBuilder {
        private String id = "series-1";
        private String name = "Test Series";
        private String description = "Test series description";
        private String authorId = "user-1";
        private String authorUsername = "testuser";
        private String authorNickname = "TestUser";
        private String thumbnailUrl;
        private List<String> postIds = new ArrayList<>();

        public SeriesBuilder id(String id) { this.id = id; return this; }
        public SeriesBuilder name(String name) { this.name = name; return this; }
        public SeriesBuilder description(String description) { this.description = description; return this; }
        public SeriesBuilder authorId(String authorId) { this.authorId = authorId; return this; }
        public SeriesBuilder authorUsername(String authorUsername) { this.authorUsername = authorUsername; return this; }
        public SeriesBuilder authorNickname(String authorNickname) { this.authorNickname = authorNickname; return this; }
        public SeriesBuilder thumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; return this; }
        public SeriesBuilder postIds(List<String> postIds) { this.postIds = new ArrayList<>(postIds); return this; }

        public Series build() {
            return Series.builder()
                    .id(id)
                    .name(name)
                    .description(description)
                    .authorId(authorId)
                    .authorUsername(authorUsername)
                    .authorNickname(authorNickname)
                    .thumbnailUrl(thumbnailUrl)
                    .postIds(new ArrayList<>(postIds))
                    .build();
        }
    }
}

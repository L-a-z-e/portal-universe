package com.portal.universe.blogservice.support.fixture;

import com.portal.universe.blogservice.tag.domain.Tag;

public final class TagFixture {

    private TagFixture() {}

    public static Tag create() {
        return builder().build();
    }

    public static TagBuilder builder() {
        return new TagBuilder();
    }

    public static class TagBuilder {
        private String id = "tag-1";
        private String name = "java";
        private Long postCount = 0L;
        private String description;

        public TagBuilder id(String id) { this.id = id; return this; }
        public TagBuilder name(String name) { this.name = name; return this; }
        public TagBuilder postCount(Long postCount) { this.postCount = postCount; return this; }
        public TagBuilder description(String description) { this.description = description; return this; }

        public Tag build() {
            return Tag.builder()
                    .id(id)
                    .name(name)
                    .postCount(postCount)
                    .description(description)
                    .build();
        }
    }
}

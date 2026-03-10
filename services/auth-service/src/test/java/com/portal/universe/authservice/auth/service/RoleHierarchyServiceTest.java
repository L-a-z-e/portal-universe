package com.portal.universe.authservice.auth.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("RoleHierarchyService 테스트")
class RoleHierarchyServiceTest {

    @Mock
    private RoleHierarchyCacheComponent cacheComponent;

    @InjectMocks
    private RoleHierarchyService roleHierarchyService;

    /**
     * DAG 그래프:
     * SUPER_ADMIN → [SHOPPING_ADMIN, BLOG_ADMIN]
     * SHOPPING_ADMIN → [ROLE_USER]
     * BLOG_ADMIN → [ROLE_USER]
     * ROLE_USER → []
     */
    private Map<String, List<String>> createSampleGraph() {
        return Map.of(
                "ROLE_SUPER_ADMIN", List.of("ROLE_SHOPPING_ADMIN", "ROLE_BLOG_ADMIN"),
                "ROLE_SHOPPING_ADMIN", List.of("ROLE_USER"),
                "ROLE_BLOG_ADMIN", List.of("ROLE_USER"),
                "ROLE_USER", List.of()
        );
    }

    @Nested
    @DisplayName("resolveEffectiveRoles")
    class ResolveEffectiveRoles {

        @Test
        @DisplayName("should_resolveAllDescendants_when_superAdmin")
        void should_resolveAllDescendants_when_superAdmin() {
            // given
            when(cacheComponent.getHierarchyGraph()).thenReturn(createSampleGraph());

            // when
            List<String> result = roleHierarchyService.resolveEffectiveRoles(List.of("ROLE_SUPER_ADMIN"));

            // then
            assertThat(result).containsExactlyInAnyOrder(
                    "ROLE_SUPER_ADMIN", "ROLE_SHOPPING_ADMIN", "ROLE_BLOG_ADMIN", "ROLE_USER"
            );
        }

        @Test
        @DisplayName("should_returnSingleRole_when_leafRole")
        void should_returnSingleRole_when_leafRole() {
            // given
            when(cacheComponent.getHierarchyGraph()).thenReturn(createSampleGraph());

            // when
            List<String> result = roleHierarchyService.resolveEffectiveRoles(List.of("ROLE_USER"));

            // then
            assertThat(result).containsExactly("ROLE_USER");
        }

        @Test
        @DisplayName("should_deduplicateRoles_when_multiplePathsToSameRole")
        void should_deduplicateRoles_when_multiplePathsToSameRole() {
            // given — SHOPPING_ADMIN과 BLOG_ADMIN 모두 ROLE_USER를 포함
            when(cacheComponent.getHierarchyGraph()).thenReturn(createSampleGraph());

            // when
            List<String> result = roleHierarchyService.resolveEffectiveRoles(
                    List.of("ROLE_SHOPPING_ADMIN", "ROLE_BLOG_ADMIN"));

            // then — ROLE_USER가 중복 없이 1번만
            assertThat(result).containsExactlyInAnyOrder(
                    "ROLE_SHOPPING_ADMIN", "ROLE_BLOG_ADMIN", "ROLE_USER"
            );
        }

        @Test
        @DisplayName("should_returnEmptyList_when_emptyInput")
        void should_returnEmptyList_when_emptyInput() {
            // given
            when(cacheComponent.getHierarchyGraph()).thenReturn(createSampleGraph());

            // when
            List<String> result = roleHierarchyService.resolveEffectiveRoles(List.of());

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should_returnInputOnly_when_roleNotInGraph")
        void should_returnInputOnly_when_roleNotInGraph() {
            // given
            when(cacheComponent.getHierarchyGraph()).thenReturn(createSampleGraph());

            // when
            List<String> result = roleHierarchyService.resolveEffectiveRoles(List.of("ROLE_UNKNOWN"));

            // then
            assertThat(result).containsExactly("ROLE_UNKNOWN");
        }
    }

    @Nested
    @DisplayName("wouldCreateCycle")
    class WouldCreateCycle {

        @Test
        @DisplayName("should_returnTrue_when_addingIncludeCreatesCycle")
        void should_returnTrue_when_addingIncludeCreatesCycle() {
            // given — ROLE_USER → ROLE_SUPER_ADMIN 추가하면 cycle
            // SUPER_ADMIN → SHOPPING_ADMIN → ROLE_USER → (SUPER_ADMIN?)
            when(cacheComponent.getHierarchyGraph()).thenReturn(createSampleGraph());

            // when — candidateIncludeKey(ROLE_SUPER_ADMIN)에서 BFS로 roleKey(ROLE_USER)에 도달 가능?
            boolean result = roleHierarchyService.wouldCreateCycle("ROLE_USER", "ROLE_SUPER_ADMIN");

            // then — SUPER_ADMIN → ... → USER 도달 가능 = cycle
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("should_returnFalse_when_noPathExists")
        void should_returnFalse_when_noPathExists() {
            // given — ROLE_SUPER_ADMIN에 ROLE_USER 추가 (이미 간접 포함)
            when(cacheComponent.getHierarchyGraph()).thenReturn(createSampleGraph());

            // when — ROLE_USER에서 BFS로 ROLE_SUPER_ADMIN에 도달 불가
            boolean result = roleHierarchyService.wouldCreateCycle("ROLE_SUPER_ADMIN", "ROLE_USER");

            // then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("should_returnTrue_when_selfReference")
        void should_returnTrue_when_selfReference() {
            // given
            when(cacheComponent.getHierarchyGraph()).thenReturn(createSampleGraph());

            // when — ROLE_USER에 ROLE_USER 추가 (자기참조)
            boolean result = roleHierarchyService.wouldCreateCycle("ROLE_USER", "ROLE_USER");

            // then
            assertThat(result).isTrue();
        }
    }

    @Nested
    @DisplayName("getHierarchyGraph")
    class GetHierarchyGraph {

        @Test
        @DisplayName("should_delegateToCache_when_called")
        void should_delegateToCache_when_called() {
            // given
            Map<String, List<String>> graph = createSampleGraph();
            when(cacheComponent.getHierarchyGraph()).thenReturn(graph);

            // when
            Map<String, List<String>> result = roleHierarchyService.getHierarchyGraph();

            // then
            assertThat(result).isEqualTo(graph);
        }
    }

    @Nested
    @DisplayName("evictHierarchyCache")
    class EvictHierarchyCache {

        @Test
        @DisplayName("should_delegateToCache_when_called")
        void should_delegateToCache_when_called() {
            // when
            roleHierarchyService.evictHierarchyCache();

            // then
            verify(cacheComponent).evictHierarchyCache();
        }
    }
}

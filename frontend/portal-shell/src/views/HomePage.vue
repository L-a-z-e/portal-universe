<script setup lang="ts">
import { Button, Badge } from '@portal/design-vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '../store/auth';
import DashboardPage from './DashboardPage.vue';

const router = useRouter();
const authStore = useAuthStore();

const services = [
  {
    id: 1,
    title: 'Blog',
    desc: '생각을 기록하고 공유하세요',
    icon: '✍️',
    link: '/blog',
    gradient: 'from-brand-primary to-brand-primaryHover'
  },
  {
    id: 2,
    title: 'Shopping',
    desc: '원하는 상품을 찾아보세요',
    icon: '🛒',
    link: '/shopping',
    gradient: 'from-status-info to-status-info'
  },
  {
    id: 3,
    title: 'Notes',
    desc: '메모와 아이디어를 정리하세요',
    icon: '📝',
    link: '#',
    gradient: 'from-brand-primary to-status-info'
  },
  {
    id: 4,
    title: 'Gallery',
    desc: '소중한 순간을 저장하세요',
    icon: '📸',
    link: '#',
    gradient: 'from-status-info to-brand-primary'
  }
];

const features = [
  {
    id: 1,
    title: '통합 인증',
    desc: '하나의 계정으로 모든 서비스 이용',
    icon: '🔐'
  },
  {
    id: 2,
    title: '실시간 동기화',
    desc: '모든 디바이스에서 즉시 동기화',
    icon: '🔄'
  },
  {
    id: 3,
    title: '안전한 저장',
    desc: '엔드투엔드 암호화로 데이터 보호',
    icon: '🛡️'
  }
];

const posts = [
  {
    id: 1,
    title: 'Vue 3 Composition API 완벽 가이드',
    excerpt: 'Vue 3의 핵심 기능인 Composition API를 실전 예제와 함께 자세히 알아봅니다...',
    date: '2025-10-20',
    readTime: '5분',
    tags: ['Vue', 'Frontend'],
    gradient: 'from-brand-primary to-brand-primaryHover'
  },
  {
    id: 2,
    title: 'Design System 구축하기',
    excerpt: '확장 가능하고 유지보수하기 쉬운 디자인 시스템을 만드는 방법을 단계별로...',
    date: '2025-10-18',
    readTime: '8분',
    tags: ['Design', 'UI/UX'],
    gradient: 'from-status-info to-status-info'
  },
  {
    id: 3,
    title: 'Micro Frontend 아키텍처',
    excerpt: 'Module Federation을 활용한 마이크로 프론트엔드 구축 실전 가이드...',
    date: '2025-10-15',
    readTime: '12분',
    tags: ['Architecture'],
    gradient: 'from-brand-primary to-status-info'
  }
];

function handleStartClick() {
  if (authStore.isAuthenticated) {
    router.push('/blog');
  } else {
    authStore.requestLogin();
  }
}
</script>

<template>
  <!-- Show Dashboard for authenticated users -->
  <DashboardPage v-if="authStore.isAuthenticated" />

  <!-- Marketing page for guests -->
  <div v-else class="bg-bg-page text-text-body">

    <!-- Hero Section - Linear style dark gradient -->
    <section class="relative overflow-hidden bg-gradient-to-b from-bg-page via-bg-card to-bg-page">
      <!-- Grid pattern overlay -->
      <div class="absolute inset-0 bg-[url('data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iNjAiIGhlaWdodD0iNjAiIHhtbG5zPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwL3N2ZyI+PGRlZnM+PHBhdHRlcm4gaWQ9ImdyaWQiIHdpZHRoPSI2MCIgaGVpZ2h0PSI2MCIgcGF0dGVyblVuaXRzPSJ1c2VyU3BhY2VPblVzZSI+PHBhdGggZD0iTSAxMCAwIEwgMCAwIDAgMTAiIGZpbGw9Im5vbmUiIHN0cm9rZT0id2hpdGUiIHN0cm9rZS1vcGFjaXR5PSIwLjAzIiBzdHJva2Utd2lkdGg9IjEiLz48L3BhdHRlcm4+PC9kZWZzPjxyZWN0IHdpZHRoPSIxMDAlIiBoZWlnaHQ9IjEwMCUiIGZpbGw9InVybCgjZ3JpZCkiLz48L3N2Zz4=')] opacity-50"></div>
      <!-- Glow effect -->
      <div class="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-[600px] h-[600px] bg-brand-primary/10 rounded-full blur-[120px]"></div>

      <div class="relative max-w-7xl mx-auto px-4 py-24 md:py-32">
        <div class="max-w-3xl">
          <div class="inline-flex items-center gap-2 px-3 py-1.5 rounded-full bg-brand-primary/10 border border-brand-primary/20 mb-8">
            <span class="w-1.5 h-1.5 bg-brand-primary rounded-full animate-pulse"></span>
            <span class="text-sm text-brand-primary font-medium">Now with Linear-style dark mode</span>
          </div>

          <h1 class="text-5xl md:text-6xl font-bold text-text-heading mb-6 leading-tight">
            Your Digital
            <span class="block bg-gradient-to-r from-brand-primary to-brand-primaryHover bg-clip-text text-transparent">
              Universe
            </span>
          </h1>

          <p class="text-lg text-text-body mb-10 leading-relaxed">
            모든 것이 연결된 당신만의 공간.
            <span class="text-text-heading font-medium">블로그, 쇼핑, 메모</span> 등
            다양한 서비스를 하나의 포털에서 경험하세요.
          </p>

          <div class="flex flex-wrap gap-3">
            <Button variant="primary" size="lg" @click="handleStartClick">
              {{ authStore.isAuthenticated ? '블로그 시작하기' : '시작하기' }} →
            </Button>

            <Button variant="secondary" size="lg">
              라이브 데모
            </Button>
          </div>
        </div>
      </div>
    </section>

    <!-- Services - Linear style cards -->
    <section class="max-w-7xl mx-auto px-4 py-16">
      <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        <div
            v-for="service in services"
            :key="service.id"
            @click="() => service.link !== '#' && router.push(service.link)"
            class="group relative overflow-hidden rounded-xl p-6 bg-bg-card border border-border-default hover:border-brand-primary/50 transition-all duration-normal cursor-pointer"
        >
          <div class="relative z-10">
            <div class="text-4xl mb-4">{{ service.icon }}</div>
            <h3 class="text-lg font-semibold text-text-heading mb-1">{{ service.title }}</h3>
            <p class="text-text-meta text-sm">{{ service.desc }}</p>
          </div>
          <div class="absolute inset-0 bg-brand-primary/5 opacity-0 group-hover:opacity-100 transition-opacity"></div>
        </div>
      </div>
    </section>

    <!-- Features - Linear style -->
    <section class="max-w-7xl mx-auto px-4 py-20">
      <div class="text-center mb-12">
        <div class="inline-block px-3 py-1 rounded-full bg-brand-primary/10 text-brand-primary font-medium text-sm mb-4">
          WHY PORTAL UNIVERSE
        </div>
        <h2 class="text-3xl md:text-4xl font-bold text-text-heading mb-4">
          모든 것이 연결됩니다
        </h2>
        <p class="text-lg text-text-meta max-w-2xl mx-auto">
          강력한 통합 기능으로 더 효율적인 디지털 라이프를 경험하세요
        </p>
      </div>

      <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
        <div
            v-for="feature in features"
            :key="feature.id"
            class="group p-6 rounded-xl bg-bg-card border border-border-default hover:border-brand-primary/30 transition-all duration-normal"
        >
          <div class="text-3xl mb-4">{{ feature.icon }}</div>
          <h3 class="text-lg font-semibold text-text-heading mb-2">{{ feature.title }}</h3>
          <p class="text-text-meta text-sm">{{ feature.desc }}</p>
        </div>
      </div>
    </section>

    <!-- Recent Posts - Linear style -->
    <section class="max-w-7xl mx-auto px-4 py-20">
      <div class="flex items-end justify-between mb-8">
        <div>
          <div class="inline-block px-3 py-1 rounded-full bg-brand-primary/10 text-brand-primary font-medium text-sm mb-4">
            LATEST UPDATES
          </div>
          <h2 class="text-3xl font-bold text-text-heading">최근 업데이트</h2>
        </div>
        <Button variant="secondary" @click="router.push('/blog')">
          전체 보기 →
        </Button>
      </div>

      <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
        <article
            v-for="post in posts"
            :key="post.id"
            class="group bg-bg-card rounded-xl overflow-hidden border border-border-default hover:border-brand-primary/30 transition-all duration-normal cursor-pointer"
        >
          <div class="h-40 bg-gradient-to-br from-bg-elevated to-bg-card flex items-center justify-center text-5xl">
            📄
          </div>

          <div class="p-5">
            <div class="flex items-center gap-2 mb-3">
              <Badge
                  v-for="tag in post.tags"
                  :key="tag"
                  variant="primary"
                  size="sm"
              >
                {{ tag }}
              </Badge>
            </div>

            <h3 class="text-base font-semibold text-text-heading mb-2 group-hover:text-brand-primary transition-colors">
              {{ post.title }}
            </h3>

            <p class="text-text-meta text-sm mb-4 line-clamp-2">
              {{ post.excerpt }}
            </p>

            <div class="flex items-center justify-between text-xs text-text-meta">
              <span>{{ post.date }}</span>
              <span>{{ post.readTime }} 읽기</span>
            </div>
          </div>
        </article>
      </div>
    </section>

    <!-- CTA Section - Linear style gradient -->
    <section class="relative overflow-hidden py-20">
      <div class="absolute inset-0 bg-gradient-to-r from-brand-primary to-brand-primaryHover"></div>
      <div class="absolute inset-0 bg-[url('data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iNjAiIGhlaWdodD0iNjAiIHhtbG5zPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwL3N2ZyI+PGRlZnM+PHBhdHRlcm4gaWQ9ImdyaWQiIHdpZHRoPSI2MCIgaGVpZ2h0PSI2MCIgcGF0dGVyblVuaXRzPSJ1c2VyU3BhY2VPblVzZSI+PHBhdGggZD0iTSAxMCAwIEwgMCAwIDAgMTAiIGZpbGw9Im5vbmUiIHN0cm9rZT0id2hpdGUiIHN0cm9rZS1vcGFjaXR5PSIwLjEiIHN0cm9rZS13aWR0aD0iMSIvPjwvcGF0dGVybj48L2RlZnM+PHJlY3Qgd2lkdGg9IjEwMCUiIGhlaWdodD0iMTAwJSIgZmlsbD0idXJsKCNncmlkKSIvPjwvc3ZnPg==')] opacity-30"></div>
      <div class="relative max-w-4xl mx-auto px-4 text-center">
        <h2 class="text-3xl md:text-4xl font-bold text-white mb-4">
          지금 바로 시작하세요
        </h2>
        <p class="text-lg text-white/80 mb-8">
          무료로 시작하고, 언제든지 업그레이드할 수 있습니다
        </p>
        <div class="flex flex-wrap gap-3 justify-center">
          <Button variant="secondary" size="lg" @click="handleStartClick">
            {{ authStore.isAuthenticated ? '블로그 시작하기' : '무료로 시작하기' }}
          </Button>
          <Button variant="ghost" size="lg" class="text-white border border-white/20 hover:bg-white/20">
            가격 보기
          </Button>
        </div>
      </div>
    </section>

  </div>
</template>
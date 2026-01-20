<script setup lang="ts">
import { ref } from 'vue';
import { Button, Badge } from '@portal/design-system-vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '../store/auth';
import LoginModal from '../components/LoginModal.vue';

const router = useRouter();
const authStore = useAuthStore();
const showLoginModal = ref(false);

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
    link: '#',
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
    showLoginModal.value = true;
  }
}
</script>

<template>
  <div class="bg-[#08090a] text-[#b4b4b4] light:bg-white light:text-gray-600">

    <!-- Hero Section - Linear style dark gradient -->
    <section class="relative overflow-hidden bg-gradient-to-b from-[#08090a] via-[#0f1011] to-[#08090a] light:from-gray-50 light:via-white light:to-gray-50">
      <!-- Grid pattern overlay -->
      <div class="absolute inset-0 bg-[url('data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iNjAiIGhlaWdodD0iNjAiIHhtbG5zPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwL3N2ZyI+PGRlZnM+PHBhdHRlcm4gaWQ9ImdyaWQiIHdpZHRoPSI2MCIgaGVpZ2h0PSI2MCIgcGF0dGVyblVuaXRzPSJ1c2VyU3BhY2VPblVzZSI+PHBhdGggZD0iTSAxMCAwIEwgMCAwIDAgMTAiIGZpbGw9Im5vbmUiIHN0cm9rZT0id2hpdGUiIHN0cm9rZS1vcGFjaXR5PSIwLjAzIiBzdHJva2Utd2lkdGg9IjEiLz48L3BhdHRlcm4+PC9kZWZzPjxyZWN0IHdpZHRoPSIxMDAlIiBoZWlnaHQ9IjEwMCUiIGZpbGw9InVybCgjZ3JpZCkiLz48L3N2Zz4=')] opacity-50"></div>
      <!-- Glow effect -->
      <div class="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-[600px] h-[600px] bg-[#5e6ad2]/10 rounded-full blur-[120px]"></div>

      <div class="relative max-w-7xl mx-auto px-4 py-24 md:py-32">
        <div class="max-w-3xl">
          <div class="inline-flex items-center gap-2 px-3 py-1.5 rounded-full bg-[#5e6ad2]/10 border border-[#5e6ad2]/20 mb-8">
            <span class="w-1.5 h-1.5 bg-[#5e6ad2] rounded-full animate-pulse"></span>
            <span class="text-sm text-[#5e6ad2] font-medium">Now with Linear-style dark mode</span>
          </div>

          <h1 class="text-5xl md:text-6xl font-bold text-white mb-6 leading-tight light:text-gray-900">
            Your Digital
            <span class="block bg-gradient-to-r from-[#5e6ad2] to-[#818cf8] bg-clip-text text-transparent">
              Universe
            </span>
          </h1>

          <p class="text-lg text-[#b4b4b4] mb-10 leading-relaxed light:text-gray-600">
            모든 것이 연결된 당신만의 공간.
            <span class="text-white font-medium light:text-gray-900">블로그, 쇼핑, 메모</span> 등
            다양한 서비스를 하나의 포털에서 경험하세요.
          </p>

          <div class="flex flex-wrap gap-3">
            <button
                @click="handleStartClick"
                class="px-6 py-3 bg-white/90 text-[#08090a] font-semibold rounded-lg hover:bg-white transition-all duration-normal flex items-center gap-2 light:bg-[#5e6ad2] light:text-white light:hover:bg-[#4f5bc0]"
            >
              {{ authStore.isAuthenticated ? '블로그 시작하기' : '시작하기' }}
              <span>→</span>
            </button>

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
            class="group relative overflow-hidden rounded-xl p-6 bg-[#0f1011] border border-[#2a2a2a] hover:border-[#5e6ad2]/50 transition-all duration-normal cursor-pointer light:bg-white light:border-gray-200 light:hover:border-[#5e6ad2]/50"
        >
          <div class="relative z-10">
            <div class="text-4xl mb-4">{{ service.icon }}</div>
            <h3 class="text-lg font-semibold text-white mb-1 light:text-gray-900">{{ service.title }}</h3>
            <p class="text-[#6b6b6b] text-sm light:text-gray-500">{{ service.desc }}</p>
          </div>
          <div class="absolute inset-0 bg-[#5e6ad2]/5 opacity-0 group-hover:opacity-100 transition-opacity"></div>
        </div>
      </div>
    </section>

    <!-- Features - Linear style -->
    <section class="max-w-7xl mx-auto px-4 py-20">
      <div class="text-center mb-12">
        <div class="inline-block px-3 py-1 rounded-full bg-[#5e6ad2]/10 text-[#5e6ad2] font-medium text-sm mb-4">
          WHY PORTAL UNIVERSE
        </div>
        <h2 class="text-3xl md:text-4xl font-bold text-white mb-4 light:text-gray-900">
          모든 것이 연결됩니다
        </h2>
        <p class="text-lg text-[#6b6b6b] max-w-2xl mx-auto light:text-gray-500">
          강력한 통합 기능으로 더 효율적인 디지털 라이프를 경험하세요
        </p>
      </div>

      <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
        <div
            v-for="feature in features"
            :key="feature.id"
            class="group p-6 rounded-xl bg-[#0f1011] border border-[#2a2a2a] hover:border-[#5e6ad2]/30 transition-all duration-normal light:bg-gray-50 light:border-gray-200 light:hover:border-[#5e6ad2]/30"
        >
          <div class="text-3xl mb-4">{{ feature.icon }}</div>
          <h3 class="text-lg font-semibold text-white mb-2 light:text-gray-900">{{ feature.title }}</h3>
          <p class="text-[#6b6b6b] text-sm light:text-gray-500">{{ feature.desc }}</p>
        </div>
      </div>
    </section>

    <!-- Recent Posts - Linear style -->
    <section class="max-w-7xl mx-auto px-4 py-20">
      <div class="flex items-end justify-between mb-8">
        <div>
          <div class="inline-block px-3 py-1 rounded-full bg-[#5e6ad2]/10 text-[#5e6ad2] font-medium text-sm mb-4">
            LATEST UPDATES
          </div>
          <h2 class="text-3xl font-bold text-white light:text-gray-900">최근 업데이트</h2>
        </div>
        <Button variant="secondary" @click="router.push('/blog')">
          전체 보기 →
        </Button>
      </div>

      <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
        <article
            v-for="post in posts"
            :key="post.id"
            class="group bg-[#0f1011] rounded-xl overflow-hidden border border-[#2a2a2a] hover:border-[#5e6ad2]/30 transition-all duration-normal cursor-pointer light:bg-white light:border-gray-200 light:hover:border-[#5e6ad2]/30"
        >
          <div class="h-40 bg-gradient-to-br from-[#18191b] to-[#0f1011] flex items-center justify-center text-5xl light:from-gray-100 light:to-gray-50">
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

            <h3 class="text-base font-semibold text-white mb-2 group-hover:text-[#5e6ad2] transition-colors light:text-gray-900">
              {{ post.title }}
            </h3>

            <p class="text-[#6b6b6b] text-sm mb-4 line-clamp-2 light:text-gray-500">
              {{ post.excerpt }}
            </p>

            <div class="flex items-center justify-between text-xs text-[#6b6b6b] light:text-gray-400">
              <span>{{ post.date }}</span>
              <span>{{ post.readTime }} 읽기</span>
            </div>
          </div>
        </article>
      </div>
    </section>

    <!-- CTA Section - Linear style gradient -->
    <section class="relative overflow-hidden py-20">
      <div class="absolute inset-0 bg-gradient-to-r from-[#5e6ad2] to-[#818cf8]"></div>
      <div class="absolute inset-0 bg-[url('data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iNjAiIGhlaWdodD0iNjAiIHhtbG5zPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwL3N2ZyI+PGRlZnM+PHBhdHRlcm4gaWQ9ImdyaWQiIHdpZHRoPSI2MCIgaGVpZ2h0PSI2MCIgcGF0dGVyblVuaXRzPSJ1c2VyU3BhY2VPblVzZSI+PHBhdGggZD0iTSAxMCAwIEwgMCAwIDAgMTAiIGZpbGw9Im5vbmUiIHN0cm9rZT0id2hpdGUiIHN0cm9rZS1vcGFjaXR5PSIwLjEiIHN0cm9rZS13aWR0aD0iMSIvPjwvcGF0dGVybj48L2RlZnM+PHJlY3Qgd2lkdGg9IjEwMCUiIGhlaWdodD0iMTAwJSIgZmlsbD0idXJsKCNncmlkKSIvPjwvc3ZnPg==')] opacity-30"></div>
      <div class="relative max-w-4xl mx-auto px-4 text-center">
        <h2 class="text-3xl md:text-4xl font-bold text-white mb-4">
          지금 바로 시작하세요
        </h2>
        <p class="text-lg text-white/80 mb-8">
          무료로 시작하고, 언제든지 업그레이드할 수 있습니다
        </p>
        <div class="flex flex-wrap gap-3 justify-center">
          <button
              @click="handleStartClick"
              class="px-6 py-3 bg-white text-[#5e6ad2] font-semibold rounded-lg hover:bg-white/90 transition-all duration-normal"
          >
            {{ authStore.isAuthenticated ? '블로그 시작하기' : '무료로 시작하기' }}
          </button>
          <button class="px-6 py-3 bg-white/10 text-white font-semibold rounded-lg border border-white/20 hover:bg-white/20 transition-all duration-normal">
            가격 보기
          </button>
        </div>
      </div>
    </section>

    <!-- Login Modal -->
    <LoginModal v-model="showLoginModal" />
  </div>
</template>
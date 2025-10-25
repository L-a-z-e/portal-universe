<script setup lang="ts">
import { ref } from 'vue';
import { Button, Badge } from '@portal/design-system';
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
    gradient: 'from-brand-500 to-brand-600'
  },
  {
    id: 2,
    title: 'Shopping',
    desc: '원하는 상품을 찾아보세요',
    icon: '🛒',
    link: '#',
    gradient: 'from-accent-500 to-accent-600'
  },
  {
    id: 3,
    title: 'Notes',
    desc: '메모와 아이디어를 정리하세요',
    icon: '📝',
    link: '#',
    gradient: 'from-brand-400 to-accent-500'
  },
  {
    id: 4,
    title: 'Gallery',
    desc: '소중한 순간을 저장하세요',
    icon: '📸',
    link: '#',
    gradient: 'from-accent-400 to-pink-500'
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
    gradient: 'from-brand-500 to-brand-600'
  },
  {
    id: 2,
    title: 'Design System 구축하기',
    excerpt: '확장 가능하고 유지보수하기 쉬운 디자인 시스템을 만드는 방법을 단계별로...',
    date: '2025-10-18',
    readTime: '8분',
    tags: ['Design', 'UI/UX'],
    gradient: 'from-accent-500 to-accent-600'
  },
  {
    id: 3,
    title: 'Micro Frontend 아키텍처',
    excerpt: 'Module Federation을 활용한 마이크로 프론트엔드 구축 실전 가이드...',
    date: '2025-10-15',
    readTime: '12분',
    tags: ['Architecture'],
    gradient: 'from-brand-400 to-accent-500'
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
  <div class="bg-white dark:bg-gray-900">

    <!-- Hero Section -->
    <section class="relative overflow-hidden bg-gradient-to-br from-gray-900 via-brand-900 to-accent-900">
      <div class="absolute inset-0 bg-[url('image/svg+xml;base64,PHN2ZyB3aWR0aD0iNjAiIGhlaWdodD0iNjAiIHhtbG5zPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwL3N2ZyI+PGRlZnM+PHBhdHRlcm4gaWQ9ImdyaWQiIHdpZHRoPSI2MCIgaGVpZ2h0PSI2MCIgcGF0dGVyblVuaXRzPSJ1c2VyU3BhY2VPblVzZSI+PHBhdGggZD0iTSAxMCAwIEwgMCAwIDAgMTAiIGZpbGw9Im5vbmUiIHN0cm9rZT0id2hpdGUiIHN0cm9rZS1vcGFjaXR5PSIwLjAzIiBzdHJva2Utd2lkdGg9IjEiLz48L3BhdHRlcm4+PC9kZWZzPjxyZWN0IHdpZHRoPSIxMDAlIiBoZWlnaHQ9IjEwMCUiIGZpbGw9InVybCgjZ3JpZCkiLz48L3N2Zz4=')] opacity-40"></div>

      <div class="relative max-w-7xl mx-auto px-4 py-24 md:py-32">
        <div class="max-w-3xl">
          <div class="inline-flex items-center gap-2 px-4 py-2 rounded-full bg-white/10 backdrop-blur-sm border border-white/20 mb-8">
            <span class="w-2 h-2 bg-emerald-400 rounded-full animate-pulse"></span>
            <span class="text-sm text-white/90 font-medium">New: Dark mode is here</span>
          </div>

          <h1 class="text-5xl md:text-7xl font-bold text-white mb-6 leading-tight">
            Your Digital
            <span class="block bg-gradient-to-r from-brand-400 via-accent-400 to-pink-400 bg-clip-text text-transparent">
              Universe
            </span>
          </h1>

          <p class="text-xl text-slate-300 mb-10 leading-relaxed">
            모든 것이 연결된 당신만의 공간.
            <span class="text-white font-semibold">블로그, 쇼핑, 메모</span> 등
            다양한 서비스를 하나의 포털에서 경험하세요.
          </p>

          <div class="flex flex-wrap gap-4">
            <button
                @click="handleStartClick"
                class="px-8 py-4 bg-white text-brand-600 font-semibold rounded-lg hover:bg-gray-100 transition-all duration-200 shadow-xl hover:shadow-2xl flex items-center gap-2"
            >
              {{ authStore.isAuthenticated ? '블로그 시작하기' : '시작하기' }}
              <span>→</span>
            </button>

            <Button variant="outline" size="lg">
              라이브 데모
            </Button>
          </div>
        </div>
      </div>

      <div class="absolute bottom-0 left-0 right-0">
        <svg viewBox="0 0 1440 80" class="w-full h-20 md:h-32" preserveAspectRatio="none">
          <path d="M0,32 C240,80 480,80 720,32 C960,-16 1200,-16 1440,32 L1440,80 L0,80 Z" fill="white" class="dark:fill-gray-900"/>
        </svg>
      </div>
    </section>

    <!-- Services -->
    <section class="max-w-7xl mx-auto px-4 -mt-8 relative z-10">
      <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        <div
            v-for="service in services"
            :key="service.id"
            @click="() => service.link !== '#' && router.push(service.link)"
            :class="[
            'group relative overflow-hidden rounded-2xl p-8',
            'bg-gradient-to-br shadow-lg hover:shadow-2xl',
            'transform hover:-translate-y-2 transition-all duration-300 cursor-pointer',
            service.gradient
          ]"
        >
          <div class="relative z-10">
            <div class="text-5xl mb-4">{{ service.icon }}</div>
            <h3 class="text-2xl font-bold text-white mb-2">{{ service.title }}</h3>
            <p class="text-white/90 text-sm">{{ service.desc }}</p>
          </div>
          <div class="absolute inset-0 bg-white/5 opacity-0 group-hover:opacity-100 transition-opacity"></div>
        </div>
      </div>
    </section>

    <!-- Features -->
    <section class="max-w-7xl mx-auto px-4 py-24">
      <div class="text-center mb-16">
        <div class="inline-block px-4 py-2 rounded-full bg-brand-50 dark:bg-brand-900/30 text-brand-600 dark:text-brand-400 font-semibold text-sm mb-4">
          WHY PORTAL UNIVERSE
        </div>
        <h2 class="text-4xl md:text-5xl font-bold text-gray-900 dark:text-gray-100 mb-4">
          모든 것이 연결됩니다
        </h2>
        <p class="text-xl text-gray-600 dark:text-gray-400 max-w-2xl mx-auto">
          강력한 통합 기능으로 더 효율적인 디지털 라이프를 경험하세요
        </p>
      </div>

      <div class="grid grid-cols-1 md:grid-cols-3 gap-8">
        <div
            v-for="feature in features"
            :key="feature.id"
            class="group p-8 rounded-2xl bg-gradient-to-br from-gray-50 to-gray-100 dark:from-gray-800 dark:to-gray-700 hover:from-brand-50 hover:to-accent-50 dark:hover:from-brand-900/50 dark:hover:to-accent-900/50 transition-all duration-300 hover:shadow-lg"
        >
          <div class="text-4xl mb-4">{{ feature.icon }}</div>
          <h3 class="text-xl font-bold text-gray-900 dark:text-gray-100 mb-2">{{ feature.title }}</h3>
          <p class="text-gray-600 dark:text-gray-400">{{ feature.desc }}</p>
        </div>
      </div>
    </section>

    <!-- Recent Posts -->
    <section class="max-w-7xl mx-auto px-4 py-24 bg-gradient-to-b from-white to-gray-50 dark:from-gray-900 dark:to-gray-800">
      <div class="flex items-end justify-between mb-12">
        <div>
          <div class="inline-block px-4 py-2 rounded-full bg-accent-50 dark:bg-accent-900/30 text-accent-600 dark:text-accent-400 font-semibold text-sm mb-4">
            LATEST UPDATES
          </div>
          <h2 class="text-4xl font-bold text-gray-900 dark:text-gray-100">최근 업데이트</h2>
        </div>
        <Button variant="secondary" @click="router.push('/blog')">
          전체 보기 →
        </Button>
      </div>

      <div class="grid grid-cols-1 md:grid-cols-3 gap-8">
        <article
            v-for="post in posts"
            :key="post.id"
            class="group bg-white dark:bg-gray-800 rounded-2xl overflow-hidden shadow-sm hover:shadow-xl transition-all duration-300 cursor-pointer"
        >
          <div :class="['h-48 bg-gradient-to-br flex items-center justify-center text-7xl', post.gradient]">
            📄
          </div>

          <div class="p-6">
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

            <h3 class="text-xl font-bold text-gray-900 dark:text-gray-100 mb-2 group-hover:text-brand-600 transition-colors">
              {{ post.title }}
            </h3>

            <p class="text-gray-600 dark:text-gray-400 mb-4 line-clamp-2">
              {{ post.excerpt }}
            </p>

            <div class="flex items-center justify-between text-sm text-gray-500 dark:text-gray-500">
              <span>{{ post.date }}</span>
              <span>{{ post.readTime }} 읽기</span>
            </div>
          </div>
        </article>
      </div>
    </section>

    <!-- CTA Section -->
    <section class="bg-gradient-to-br from-brand-600 to-accent-600 text-white py-24">
      <div class="max-w-4xl mx-auto px-4 text-center">
        <h2 class="text-4xl md:text-5xl font-bold mb-6">
          지금 바로 시작하세요
        </h2>
        <p class="text-xl text-brand-100 mb-10">
          무료로 시작하고, 언제든지 업그레이드할 수 있습니다
        </p>
        <div class="flex flex-wrap gap-4 justify-center">
          <button
              @click="handleStartClick"
              class="px-8 py-4 bg-white text-brand-600 font-semibold rounded-lg hover:bg-gray-100 transition-all duration-200 shadow-xl"
          >
            {{ authStore.isAuthenticated ? '블로그 시작하기' : '무료로 시작하기' }}
          </button>
          <Button variant="outline" size="lg">
            가격 보기
          </Button>
        </div>
      </div>
    </section>

    <!-- Login Modal -->
    <LoginModal v-model="showLoginModal" />
  </div>
</template>
<script setup lang="ts">
import { Button } from '@portal/design-vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '../store/auth';
import MaterialIcon from '../components/MaterialIcon.vue';

const router = useRouter();
const authStore = useAuthStore();

const services = [
  {
    title: 'Blog',
    desc: '컨텐츠 관리 및 출판 시스템',
    icon: 'article',
    link: '/blog',
    color: 'teal',
  },
  {
    title: 'Shopping',
    desc: '커머스 주문 및 재고 관리',
    icon: 'storefront',
    link: '/shopping',
    color: 'orange',
  },
  {
    title: 'Prism',
    desc: 'AI 에이전트 오케스트레이션',
    icon: 'smart_toy',
    link: '/prism',
    color: 'indigo',
  },
  {
    title: 'Drive',
    desc: '안전한 클라우드 스토리지',
    icon: 'cloud_upload',
    link: '/drive',
    color: 'sky',
  },
];

const features = [
  {
    title: '통합 인증',
    desc: '하나의 계정으로 모든 서비스를 이용할 수 있습니다',
    icon: 'lock',
  },
  {
    title: '실시간 동기화',
    desc: '모든 서비스의 데이터가 즉시 동기화됩니다',
    icon: 'sync',
  },
  {
    title: '안전한 저장',
    desc: '엔드투엔드 암호화로 데이터를 보호합니다',
    icon: 'shield',
  },
];

function handleStartClick() {
  if (authStore.isAuthenticated) {
    router.push('/dashboard');
  } else {
    authStore.requestLogin('/dashboard');
  }
}
</script>

<template>
  <div class="bg-bg-page text-text-body">

    <!-- Hero Section -->
    <section class="relative pt-20 pb-24 md:pt-32 md:pb-36 overflow-hidden">
      <!-- Grid pattern -->
      <div class="absolute inset-0 bg-[linear-gradient(to_right,rgba(255,255,255,0.04)_1px,transparent_1px),linear-gradient(to_bottom,rgba(255,255,255,0.04)_1px,transparent_1px)] bg-[size:40px_40px] [mask-image:linear-gradient(to_bottom,transparent,black_10%,black_90%,transparent)] opacity-30"></div>
      <!-- Glow -->
      <div class="absolute top-[-100px] left-1/2 -translate-x-1/2 w-[1000px] h-[600px] bg-[radial-gradient(circle_at_center,rgba(157,78,221,0.15)_0%,transparent_60%)] blur-3xl opacity-50"></div>

      <div class="relative max-w-5xl mx-auto px-6 text-center flex flex-col items-center">
        <!-- Badge -->
        <div class="inline-flex items-center gap-1.5 px-3 py-1 rounded-full border border-border-default bg-bg-card/50 text-xs text-brand-primary mb-8">
          <span class="relative flex h-2 w-2">
            <span class="animate-ping absolute inline-flex h-full w-full rounded-full bg-brand-primary opacity-75"></span>
            <span class="relative inline-flex rounded-full h-2 w-2 bg-brand-primary"></span>
          </span>
          Unified Digital Workspace
        </div>

        <h1 class="text-5xl md:text-7xl font-bold tracking-tight text-text-heading mb-8 leading-[1.1]">
          Your Digital<br />
          <span class="bg-gradient-to-r from-brand-primary to-brand-primaryHover bg-clip-text text-transparent">Universe</span>
        </h1>

        <p class="text-lg md:text-xl text-text-meta max-w-3xl mx-auto mb-12 leading-relaxed">
          모든 것이 연결된 당신만의 공간.
          <span class="text-text-heading">Blog, Shopping, AI Orchestration</span> 등
          다양한 서비스를 하나의 포털에서 경험하세요.
        </p>

        <div class="flex flex-col sm:flex-row items-center justify-center gap-4">
          <button
            @click="handleStartClick"
            class="h-12 px-8 rounded-full bg-text-heading text-bg-page font-semibold hover:opacity-90 transition-opacity flex items-center gap-2 group text-base cursor-pointer"
          >
            {{ authStore.isAuthenticated ? '대시보드로 이동' : '시작하기' }}
            <MaterialIcon name="arrow_forward" :size="20" class="transition-transform group-hover:translate-x-0.5" />
          </button>
          <Button variant="secondary" size="lg" class="rounded-full" @click="router.push('/blog')">
            둘러보기
          </Button>
        </div>
      </div>
    </section>

    <!-- Services Section -->
    <section class="py-32 border-t border-border-default bg-bg-card/30">
      <div class="max-w-7xl mx-auto px-6">
        <div class="text-center mb-20">
          <h2 class="text-3xl md:text-4xl font-semibold text-text-heading mb-4">통합된 생태계</h2>
          <p class="text-text-meta text-base md:text-lg max-w-2xl mx-auto">팀이 필요로 하는 모든 도구가 준비되어 있습니다.</p>
        </div>

        <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
          <div
            v-for="service in services"
            :key="service.title"
            @click="router.push(service.link)"
            class="service-card group cursor-pointer h-60 flex flex-col justify-between"
            :class="`hover-glow-${service.color}`"
          >
            <div>
              <div
                class="w-14 h-14 rounded-xl flex items-center justify-center mb-6 group-hover:scale-110 transition-transform"
                :class="{
                  'bg-teal-500/10 text-teal-400': service.color === 'teal',
                  'bg-orange-500/10 text-orange-400': service.color === 'orange',
                  'bg-indigo-500/10 text-indigo-400': service.color === 'indigo',
                  'bg-sky-500/10 text-sky-400': service.color === 'sky',
                }"
              >
                <MaterialIcon :name="service.icon" :size="28" />
              </div>
              <h3 class="text-xl font-medium text-text-heading mb-2">{{ service.title }}</h3>
            </div>
            <p class="text-base text-text-muted">{{ service.desc }}</p>
          </div>
        </div>
      </div>
    </section>

    <!-- Features Section -->
    <section class="py-32 relative overflow-hidden">
      <div class="max-w-5xl mx-auto px-6">
        <div class="mb-20 text-center">
          <h2 class="text-3xl md:text-4xl font-semibold text-text-heading mb-6">연결된 경험</h2>
          <p class="text-text-meta text-lg max-w-2xl mx-auto">
            데이터가 사일로에 갇히지 않습니다. 모든 서비스가 유기적으로 연결되어 끊김 없는 워크플로우를 제공합니다.
          </p>
        </div>

        <div class="grid grid-cols-1 md:grid-cols-3 gap-6">
          <div
            v-for="feature in features"
            :key="feature.title"
            class="p-8 rounded-2xl bg-bg-card border border-border-default hover:border-brand-primary/30 transition-all group"
          >
            <div class="w-12 h-12 rounded-xl bg-brand-primary/10 flex items-center justify-center mb-6 text-brand-primary group-hover:scale-110 transition-transform">
              <MaterialIcon :name="feature.icon" :size="24" />
            </div>
            <h3 class="text-lg font-medium text-text-heading mb-3">{{ feature.title }}</h3>
            <p class="text-text-meta text-sm leading-relaxed">{{ feature.desc }}</p>
          </div>
        </div>
      </div>
    </section>

  </div>
</template>

<style scoped>
.service-card {
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.06);
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;
  padding: 2rem;
  border-radius: 1rem;
}

.service-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 1px;
  background: transparent;
  transition: background 0.3s ease;
  box-shadow: 0 0 15px transparent;
}

.service-card:hover {
  background: rgba(255, 255, 255, 0.06);
  transform: translateY(-4px);
}

.hover-glow-teal:hover::before { background: #14b8a6; box-shadow: 0 1px 15px rgba(20, 184, 166, 0.5); }
.hover-glow-orange:hover::before { background: #f97316; box-shadow: 0 1px 15px rgba(249, 115, 22, 0.5); }
.hover-glow-indigo:hover::before { background: #6366f1; box-shadow: 0 1px 15px rgba(99, 102, 241, 0.5); }
.hover-glow-sky:hover::before { background: #0ea5e9; box-shadow: 0 1px 15px rgba(14, 165, 233, 0.5); }

/* Light mode adjustments */
:global(html.light) .service-card {
  background: rgba(0, 0, 0, 0.02);
  border-color: rgba(0, 0, 0, 0.08);
}
:global(html.light) .service-card:hover {
  background: rgba(0, 0, 0, 0.04);
}
</style>

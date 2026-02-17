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
    desc: '생각을 글로, 글을 세상으로',
    icon: 'article',
    link: '/blog',
    color: 'teal',
  },
  {
    title: 'Shopping',
    desc: '발견하고, 비교하고, 구매하세요',
    icon: 'storefront',
    link: '/shopping',
    color: 'orange',
  },
  {
    title: 'Prism',
    desc: 'AI와 대화 한 번으로 복잡한 일을 해결',
    icon: 'smart_toy',
    link: '/prism',
    color: 'indigo',
  },
  {
    title: 'Drive',
    desc: '소중한 파일, 어디서든 안전하게',
    icon: 'cloud_upload',
    link: '/drive',
    color: 'sky',
  },
];

const features = [
  {
    title: '한 번의 로그인',
    desc: '하나의 계정으로 모든 서비스를 자유롭게 넘나드세요',
    icon: 'passkey',
  },
  {
    title: '끊김 없는 흐름',
    desc: '서비스 사이를 자연스럽게 이동하세요. 컨텍스트가 따라갑니다',
    icon: 'swap_horiz',
  },
  {
    title: '나에게 맞춘 공간',
    desc: '대시보드, 테마, 알림까지 나만의 방식으로 설정하세요',
    icon: 'tune',
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
          All-in-One Digital Life
        </div>

        <h1 class="text-5xl md:text-7xl font-bold tracking-tight text-text-heading mb-8 leading-[1.1]">
          Your Digital<br />
          <span class="bg-gradient-to-r from-brand-primary to-brand-primaryHover bg-clip-text text-transparent">Universe</span>
        </h1>

        <p class="text-lg md:text-xl text-text-meta max-w-2xl mx-auto mb-12 leading-relaxed">
          글을 쓰고, 쇼핑하고, AI와 대화하고, 파일을 보관하세요.<br />
          흩어진 디지털 일상을 <span class="text-text-heading">하나의 공간</span>으로 연결합니다.
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

    <!-- Browser Mockup Section -->
    <section class="relative z-10 -mt-8 pb-20 md:pb-32">
      <div class="max-w-7xl mx-auto px-6 perspective-container">
        <div class="tilt-card relative rounded-2xl overflow-hidden border border-border-default bg-[#0e0f11] shadow-2xl mx-auto max-w-[1300px]">
          <!-- Title bar -->
          <div class="h-10 bg-[#1a1b1e] border-b border-[rgba(255,255,255,0.06)] flex items-center px-6 gap-2">
            <div class="flex gap-2">
              <div class="w-3 h-3 rounded-full bg-[#f87171]"></div>
              <div class="w-3 h-3 rounded-full bg-[#fbbf24]"></div>
              <div class="w-3 h-3 rounded-full bg-[#4ade80]"></div>
            </div>
            <div class="flex-1 text-center text-xs text-text-muted font-mono">portal-universe.app</div>
          </div>
          <!-- Content -->
          <div class="bg-[#08090a] p-8 min-h-[500px] md:min-h-[700px] relative overflow-hidden">
            <div class="flex h-full gap-8">
              <!-- Sidebar mock -->
              <div class="w-20 flex-shrink-0 border-r border-[rgba(255,255,255,0.04)] flex-col items-center py-6 gap-6 hidden md:flex">
                <div class="w-10 h-10 rounded-lg bg-brand-primary/20 flex items-center justify-center">
                  <MaterialIcon name="grid_view" :size="20" class="text-brand-primary" />
                </div>
                <div class="w-8 h-8 rounded-lg bg-white/5"></div>
                <div class="w-8 h-8 rounded-lg bg-white/5"></div>
                <div class="w-8 h-8 rounded-lg bg-white/5"></div>
                <div class="mt-auto w-8 h-8 rounded-full bg-gradient-to-tr from-gray-700 to-gray-600"></div>
              </div>
              <!-- Main content mock -->
              <div class="flex-1 flex flex-col h-full">
                <!-- Top bar mock -->
                <div class="flex justify-between items-center mb-8 pr-0 md:pr-8 pt-2">
                  <div class="h-10 w-48 md:w-64 bg-white/5 rounded-lg"></div>
                  <div class="flex gap-3">
                    <div class="h-10 w-10 bg-white/5 rounded-lg"></div>
                    <div class="h-10 w-24 md:w-32 bg-brand-primary rounded-lg shadow-lg shadow-brand-primary/20"></div>
                  </div>
                </div>
                <!-- Stats cards mock -->
                <div class="grid grid-cols-2 md:grid-cols-4 gap-4 md:gap-6 mb-8 pr-0 md:pr-8">
                  <div class="h-32 bg-[#0f1011] border border-[rgba(255,255,255,0.06)] rounded-xl p-4 flex flex-col justify-between">
                    <div class="h-8 w-8 rounded bg-teal-500/10"></div>
                    <div class="h-2 w-1/2 bg-white/10 rounded"></div>
                  </div>
                  <div class="h-32 bg-[#0f1011] border border-[rgba(255,255,255,0.06)] rounded-xl p-4 flex flex-col justify-between">
                    <div class="h-8 w-8 rounded bg-orange-500/10"></div>
                    <div class="h-2 w-1/2 bg-white/10 rounded"></div>
                  </div>
                  <div class="h-32 bg-[#0f1011] border border-[rgba(255,255,255,0.06)] rounded-xl p-4 flex flex-col justify-between">
                    <div class="h-8 w-8 rounded bg-indigo-500/10"></div>
                    <div class="h-2 w-1/2 bg-white/10 rounded"></div>
                  </div>
                  <div class="h-32 bg-[#0f1011] border border-[rgba(255,255,255,0.06)] rounded-xl p-4 flex flex-col justify-between">
                    <div class="h-8 w-8 rounded bg-sky-500/10"></div>
                    <div class="h-2 w-1/2 bg-white/10 rounded"></div>
                  </div>
                </div>
                <!-- Main panels mock -->
                <div class="flex flex-col md:flex-row gap-6 md:gap-8 h-full min-h-[200px] md:min-h-[300px] pr-0 md:pr-8 pb-8">
                  <div class="flex-1 bg-[#0f1011] border border-[rgba(255,255,255,0.06)] rounded-xl p-6 relative overflow-hidden group">
                    <div class="absolute inset-0 bg-gradient-to-tr from-brand-primary/5 via-transparent to-transparent opacity-50 group-hover:opacity-100 transition-opacity"></div>
                    <div class="h-4 w-1/3 bg-white/10 rounded mb-4"></div>
                    <div class="space-y-3">
                      <div class="h-12 w-full bg-white/5 rounded border border-white/5"></div>
                      <div class="h-12 w-full bg-white/5 rounded border border-white/5"></div>
                      <div class="h-12 w-full bg-white/5 rounded border border-white/5"></div>
                    </div>
                  </div>
                  <div class="w-full md:w-1/3 bg-[#0f1011] border border-[rgba(255,255,255,0.06)] rounded-xl p-6">
                    <div class="h-4 w-1/2 bg-white/10 rounded mb-6"></div>
                    <div class="flex justify-center py-8">
                      <div class="w-24 h-24 md:w-32 md:h-32 rounded-full border-4 border-brand-primary/20 border-t-brand-primary animate-spin"></div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
            <!-- Bottom fade -->
            <div class="absolute inset-0 bg-gradient-to-t from-[#08090a] to-transparent opacity-60 pointer-events-none"></div>
          </div>
        </div>
        <!-- Glow under card -->
        <div class="absolute top-1/2 left-1/2 -translate-x-1/2 translate-y-32 w-4/5 h-32 bg-brand-primary blur-[120px] opacity-25 -z-10"></div>
      </div>
    </section>

    <!-- Services Section -->
    <section class="py-32 border-t border-border-default bg-bg-card/30">
      <div class="max-w-7xl mx-auto px-6">
        <div class="text-center mb-20">
          <h2 class="text-3xl md:text-4xl font-semibold text-text-heading mb-4">하나의 공간, 무한한 가능성</h2>
          <p class="text-text-meta text-base md:text-lg max-w-2xl mx-auto">일상의 모든 디지털 활동이 여기에 있습니다.</p>
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
          <h2 class="text-3xl md:text-4xl font-semibold text-text-heading mb-6">왜 Portal Universe인가?</h2>
          <p class="text-text-meta text-lg max-w-xl mx-auto">
            여러 앱을 오가는 대신, 하나의 공간에서 모든 걸 해결하세요.
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

/* Perspective browser mockup */
.perspective-container {
  perspective: 2000px;
}

.tilt-card {
  transform: rotateX(20deg) scale(0.95);
  transform-style: preserve-3d;
  box-shadow: 0 50px 100px -20px rgba(157, 78, 221, 0.25);
  transition: transform 0.5s ease;
}

.tilt-card:hover {
  transform: rotateX(10deg) scale(0.98) translateY(-10px);
}

/* Light mode adjustments */
:global(html.light) .service-card {
  background: rgba(0, 0, 0, 0.02);
  border-color: rgba(0, 0, 0, 0.08);
}
:global(html.light) .service-card:hover {
  background: rgba(0, 0, 0, 0.04);
}
</style>

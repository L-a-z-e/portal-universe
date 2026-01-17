#!/bin/bash

echo "🧹 Storybook 재설치 (design-system, npm workspace 방식)"

cd /Users/laze/Laze/Project/portal-universe/frontend

# frontend 루트에서 Storybook 제거
npm uninstall storybook @storybook/vue3-vite @storybook/addon-essentials @storybook/addon-a11y @chromatic-com/storybook
rm -rf .storybook

# design-system에 Storybook 설치 (workspace 지정)
echo "🎨 Storybook 설치 중 (design-system workspace)..."
cd design-system
npx storybook@latest init --type vue3 --yes

# addon-vitest 제거
npm uninstall @storybook/addon-vitest

# 필수 의존성 추가
npm install -D @vitejs/plugin-vue

cat > .storybook/main.ts << 'MAINEOF'
import type { StorybookConfig } from '@storybook/vue3-vite';
import vue from '@vitejs/plugin-vue';
import { mergeConfig } from 'vite';

const config: StorybookConfig = {
  stories: [
    "../src/**/*.mdx",
    "../src/**/*.stories.@(js|jsx|mjs|ts|tsx|vue)"
  ],
  addons: [
    '@storybook/addon-essentials',
    '@storybook/addon-a11y',
  ],
  framework: {
    name: '@storybook/vue3-vite',
    options: {}
  },
  viteFinal: async (config) => {
    return mergeConfig(config, {
      plugins: [vue()],
      css: {
        postcss: './postcss.config.js'
      }
    });
  }
};

export default config;
MAINEOF

cat > .storybook/preview.ts << 'PREVIEWEOF'
import type { Preview } from '@storybook/vue3';
import '../src/index.css';

const preview: Preview = {
  parameters: {
    controls: {
      matchers: {
        color: /(background|color)$/i,
        date: /Date$/i,
      },
    },
  },
  globalTypes: {
    theme: {
      name: 'Theme',
      description: 'Global theme for components',
      defaultValue: 'light',
      toolbar: {
        icon: 'circlehollow',
        items: [
          { value: 'light', title: 'Light' },
          { value: 'dark', title: 'Dark' }
        ],
      },
    },
  },
  decorators: [
    (story, context) => {
      const theme = context.globals.theme || 'light';
      document.documentElement.className = theme === 'dark' ? 'dark' : '';
      return story();
    },
  ],
};

export default preview;
PREVIEWEOF

echo "✅ Storybook 설치 완료!"
echo "🚀 실행: npm run storybook -w design-system"
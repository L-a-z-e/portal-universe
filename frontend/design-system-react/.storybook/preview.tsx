import type { Preview } from '@storybook/react';
import { themes } from 'storybook/theming';
import '../src/styles/index.css';

const portalDarkTheme = {
  ...themes.dark,

  // Brand
  brandTitle: 'Portal Design System (React)',
  brandUrl: '/',

  // UI
  appBg: '#08090a',
  appContentBg: '#0e0f10',
  appPreviewBg: '#08090a',
  appBorderColor: '#26282b',
  appBorderRadius: 6,

  // Typography
  fontBase: '"Inter Variable", "Inter", -apple-system, BlinkMacSystemFont, sans-serif',
  fontCode: '"JetBrains Mono", "Fira Code", monospace',

  // Text colors
  textColor: '#f7f8f8',
  textInverseColor: '#08090a',
  textMutedColor: '#8a8f98',

  // Toolbar
  barTextColor: '#8a8f98',
  barHoverColor: '#f7f8f8',
  barSelectedColor: '#5e6ad2',
  barBg: '#0e0f10',

  // Form colors
  buttonBg: '#5e6ad2',
  buttonBorder: '#5e6ad2',
  booleanBg: '#1b1c1e',
  booleanSelectedBg: '#5e6ad2',
  inputBg: '#1b1c1e',
  inputBorder: '#26282b',
  inputTextColor: '#f7f8f8',
  inputBorderRadius: 4,

  // Brand colors
  colorPrimary: '#5e6ad2',
  colorSecondary: '#5e6ad2',
};

const preview: Preview = {
  parameters: {
    actions: { argTypesRegex: '^on[A-Z].*' },
    controls: {
      matchers: {
        color: /(background|color)$/i,
        date: /Date$/,
      },
    },
    backgrounds: {
      disable: true,
    },
    docs: {
      theme: portalDarkTheme,
    },
  },
  globalTypes: {
    theme: {
      name: 'Theme',
      description: 'Global theme for components',
      defaultValue: 'dark',
      toolbar: {
        icon: 'circlehollow',
        items: [
          { value: 'dark', title: 'Dark (Default)' },
          { value: 'light', title: 'Light' },
        ],
        showName: true,
        dynamicTitle: true,
      },
    },
    service: {
      name: 'Service',
      description: 'Service theme variant',
      defaultValue: 'portal',
      toolbar: {
        icon: 'globe',
        items: [
          { value: 'portal', title: 'Portal (Linear)' },
          { value: 'blog', title: 'Blog (Green)' },
          { value: 'shopping', title: 'Shopping (Orange)' },
        ],
        showName: true,
        dynamicTitle: true,
      },
    },
  },
  decorators: [
    (Story, context) => {
      const theme = context.globals.theme || 'dark';
      const service = context.globals.service || 'portal';

      if (typeof document !== 'undefined') {
        document.documentElement.setAttribute('data-theme', theme);
        document.documentElement.setAttribute('data-service', service);

        if (theme === 'dark') {
          document.documentElement.classList.add('dark');
        } else {
          document.documentElement.classList.remove('dark');
        }
      }

      return (
        <div className="p-6 min-h-screen bg-bg-page text-text-body transition-all duration-normal ease-linear-ease">
          <Story />
        </div>
      );
    },
  ],
};

export default preview;

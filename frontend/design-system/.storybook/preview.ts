import type { Preview } from '@storybook/vue3';
import '../src/styles/index.css';

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
      disable: true, // Let the theme handle backgrounds
    },
    docs: {
      theme: {
        base: 'dark',
      },
    },
  },
  globalTypes: {
    theme: {
      name: 'Theme',
      description: 'Global theme for components',
      defaultValue: 'dark', // Linear-inspired: dark is default
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
    (story, context) => {
      const theme = context.globals.theme || 'dark';
      const service = context.globals.service || 'portal';

      return {
        setup() {
          if (typeof document !== 'undefined') {
            document.documentElement.setAttribute('data-theme', theme);
            document.documentElement.setAttribute('data-service', service);

            if (theme === 'dark') {
              document.documentElement.classList.add('dark');
            } else {
              document.documentElement.classList.remove('dark');
            }
          }
          return {};
        },
        template: `
          <div class="p-6 min-h-screen bg-bg-page text-text-body transition-all duration-normal ease-linear-ease">
            <story />
          </div>
        `,
      };
    },
  ],
};

export default preview;

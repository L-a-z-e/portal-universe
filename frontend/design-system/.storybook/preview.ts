import type { Preview } from '@storybook/vue3';
import '../src/styles/index.css';
import '../src/styles/themes/blog.css';
import '../src/styles/themes/shopping.css';

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
      default: 'light',
      values: [
        { name: 'light', value: '#F8F9FA' },
        { name: 'dark', value: '#0F1419' },
      ],
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
          { value: 'dark', title: 'Dark' },
        ],
        showName: true,
      },
    },
    service: {
      name: 'Service',
      description: 'Service theme variant',
      defaultValue: 'portal',
      toolbar: {
        icon: 'globe',
        items: [
          { value: 'portal', title: 'Portal' },
          { value: 'blog', title: 'Blog' },
          { value: 'shopping', title: 'Shopping' },
        ],
        showName: true,
      },
    },
  },
  decorators: [
    (story, context) => {
      const theme = context.globals.theme || 'light';
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
          <div class="p-4 min-h-screen bg-bg-page text-text-body transition-colors duration-200">
            <story />
          </div>
        `,
      };
    },
  ],
};

export default preview;

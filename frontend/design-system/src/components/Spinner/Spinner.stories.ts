import type { Meta, StoryObj } from '@storybook/vue3';
import { Spinner } from './index';

const meta: Meta<typeof Spinner> = {
  title: 'Components/Feedback/Spinner',
  component: Spinner,
  tags: ['autodocs'],
  argTypes: {
    size: {
      control: 'select',
      options: ['xs', 'sm', 'md', 'lg', 'xl'],
    },
    color: {
      control: 'select',
      options: ['primary', 'current', 'white'],
    },
  },
};

export default meta;
type Story = StoryObj<typeof Spinner>;

export const Default: Story = {
  args: {
    size: 'md',
    color: 'primary',
  },
};

export const Sizes: Story = {
  render: () => ({
    components: { Spinner },
    template: `
      <div class="flex items-center gap-6">
        <div class="text-center">
          <Spinner size="xs" />
          <p class="text-xs text-text-muted mt-2">xs</p>
        </div>
        <div class="text-center">
          <Spinner size="sm" />
          <p class="text-xs text-text-muted mt-2">sm</p>
        </div>
        <div class="text-center">
          <Spinner size="md" />
          <p class="text-xs text-text-muted mt-2">md</p>
        </div>
        <div class="text-center">
          <Spinner size="lg" />
          <p class="text-xs text-text-muted mt-2">lg</p>
        </div>
        <div class="text-center">
          <Spinner size="xl" />
          <p class="text-xs text-text-muted mt-2">xl</p>
        </div>
      </div>
    `,
  }),
};

export const Colors: Story = {
  render: () => ({
    components: { Spinner },
    template: `
      <div class="flex items-center gap-8">
        <div class="text-center">
          <Spinner color="primary" size="lg" />
          <p class="text-xs text-text-muted mt-2">primary</p>
        </div>
        <div class="text-center text-blue-500">
          <Spinner color="current" size="lg" />
          <p class="text-xs text-text-muted mt-2">current</p>
        </div>
        <div class="text-center bg-gray-800 p-4 rounded">
          <Spinner color="white" size="lg" />
          <p class="text-xs text-gray-300 mt-2">white</p>
        </div>
      </div>
    `,
  }),
};

export const WithText: Story = {
  render: () => ({
    components: { Spinner },
    template: `
      <div class="flex items-center gap-3">
        <Spinner size="sm" />
        <span class="text-text-body">Loading...</span>
      </div>
    `,
  }),
};

export const InButton: Story = {
  render: () => ({
    components: { Spinner },
    template: `
      <button class="inline-flex items-center gap-2 px-4 py-2 bg-brand-600 text-white rounded-lg">
        <Spinner size="sm" color="white" />
        <span>Processing...</span>
      </button>
    `,
  }),
};

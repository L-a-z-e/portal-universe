import type { Meta, StoryObj } from '@storybook/vue3';
import { Skeleton } from './index';

const meta: Meta<typeof Skeleton> = {
  title: 'Components/Feedback/Skeleton',
  component: Skeleton,
  tags: ['autodocs'],
  argTypes: {
    variant: {
      control: 'select',
      options: ['text', 'circular', 'rectangular', 'rounded'],
    },
    animation: {
      control: 'select',
      options: ['pulse', 'wave', 'none'],
    },
    lines: {
      control: { type: 'number', min: 1, max: 10 },
    },
  },
};

export default meta;
type Story = StoryObj<typeof Skeleton>;

export const Text: Story = {
  args: {
    variant: 'text',
    width: '200px',
  },
};

export const MultilineText: Story = {
  args: {
    variant: 'text',
    lines: 3,
    width: '100%',
  },
};

export const Circular: Story = {
  args: {
    variant: 'circular',
    width: '64px',
    height: '64px',
  },
};

export const Rectangular: Story = {
  args: {
    variant: 'rectangular',
    width: '100%',
    height: '200px',
  },
};

export const Rounded: Story = {
  args: {
    variant: 'rounded',
    width: '100%',
    height: '150px',
  },
};

export const Animations: Story = {
  render: () => ({
    components: { Skeleton },
    template: `
      <div class="flex flex-col gap-6">
        <div>
          <h3 class="text-sm font-medium text-text-muted mb-2">Pulse (default)</h3>
          <Skeleton animation="pulse" width="200px" />
        </div>
        <div>
          <h3 class="text-sm font-medium text-text-muted mb-2">Wave</h3>
          <Skeleton animation="wave" width="200px" />
        </div>
        <div>
          <h3 class="text-sm font-medium text-text-muted mb-2">None</h3>
          <Skeleton animation="none" width="200px" />
        </div>
      </div>
    `,
  }),
};

export const CardSkeleton: Story = {
  render: () => ({
    components: { Skeleton },
    template: `
      <div class="max-w-sm p-4 border border-border-default rounded-lg">
        <Skeleton variant="rectangular" height="200px" class="mb-4" />
        <Skeleton width="70%" class="mb-2" />
        <Skeleton width="100%" class="mb-2" />
        <Skeleton width="40%" />
      </div>
    `,
  }),
};

export const ProfileSkeleton: Story = {
  render: () => ({
    components: { Skeleton },
    template: `
      <div class="flex items-center gap-4">
        <Skeleton variant="circular" width="48px" height="48px" />
        <div class="flex-1">
          <Skeleton width="120px" class="mb-2" />
          <Skeleton width="200px" />
        </div>
      </div>
    `,
  }),
};

export const ListSkeleton: Story = {
  render: () => ({
    components: { Skeleton },
    template: `
      <div class="space-y-4">
        <div v-for="i in 3" :key="i" class="flex items-center gap-3">
          <Skeleton variant="circular" width="40px" height="40px" />
          <div class="flex-1">
            <Skeleton width="60%" class="mb-1" />
            <Skeleton width="40%" height="0.75rem" />
          </div>
        </div>
      </div>
    `,
  }),
};

import type { Meta, StoryObj } from '@storybook/vue3';
import { Stack } from './index';

const meta: Meta<typeof Stack> = {
  title: 'Components/Layout/Stack',
  component: Stack,
  tags: ['autodocs'],
  argTypes: {
    direction: {
      control: 'select',
      options: ['horizontal', 'vertical'],
    },
    gap: {
      control: 'select',
      options: ['none', 'xs', 'sm', 'md', 'lg', 'xl', '2xl'],
    },
    align: {
      control: 'select',
      options: ['start', 'center', 'end', 'stretch', 'baseline'],
    },
    justify: {
      control: 'select',
      options: ['start', 'center', 'end', 'between', 'around', 'evenly'],
    },
    wrap: {
      control: 'boolean',
    },
  },
};

export default meta;
type Story = StoryObj<typeof Stack>;

const Box = `<div class="w-16 h-16 bg-brand-500 rounded flex items-center justify-center text-white font-bold">Box</div>`;

export const Vertical: Story = {
  render: () => ({
    components: { Stack },
    template: `
      <Stack direction="vertical" gap="md">
        <div class="w-full h-12 bg-brand-500 rounded flex items-center justify-center text-white">1</div>
        <div class="w-full h-12 bg-brand-500 rounded flex items-center justify-center text-white">2</div>
        <div class="w-full h-12 bg-brand-500 rounded flex items-center justify-center text-white">3</div>
      </Stack>
    `,
  }),
};

export const Horizontal: Story = {
  render: () => ({
    components: { Stack },
    template: `
      <Stack direction="horizontal" gap="md">
        <div class="w-16 h-16 bg-brand-500 rounded flex items-center justify-center text-white">1</div>
        <div class="w-16 h-16 bg-brand-500 rounded flex items-center justify-center text-white">2</div>
        <div class="w-16 h-16 bg-brand-500 rounded flex items-center justify-center text-white">3</div>
      </Stack>
    `,
  }),
};

export const GapSizes: Story = {
  render: () => ({
    components: { Stack },
    template: `
      <div class="space-y-8">
        <div>
          <h3 class="text-sm font-medium text-text-muted mb-2">Gap: xs</h3>
          <Stack direction="horizontal" gap="xs">
            <div class="w-12 h-12 bg-brand-500 rounded" />
            <div class="w-12 h-12 bg-brand-500 rounded" />
            <div class="w-12 h-12 bg-brand-500 rounded" />
          </Stack>
        </div>
        <div>
          <h3 class="text-sm font-medium text-text-muted mb-2">Gap: md</h3>
          <Stack direction="horizontal" gap="md">
            <div class="w-12 h-12 bg-brand-500 rounded" />
            <div class="w-12 h-12 bg-brand-500 rounded" />
            <div class="w-12 h-12 bg-brand-500 rounded" />
          </Stack>
        </div>
        <div>
          <h3 class="text-sm font-medium text-text-muted mb-2">Gap: xl</h3>
          <Stack direction="horizontal" gap="xl">
            <div class="w-12 h-12 bg-brand-500 rounded" />
            <div class="w-12 h-12 bg-brand-500 rounded" />
            <div class="w-12 h-12 bg-brand-500 rounded" />
          </Stack>
        </div>
      </div>
    `,
  }),
};

export const Alignment: Story = {
  render: () => ({
    components: { Stack },
    template: `
      <div class="space-y-8">
        <div>
          <h3 class="text-sm font-medium text-text-muted mb-2">Align: start</h3>
          <Stack direction="horizontal" gap="md" align="start" class="h-24 border border-dashed border-gray-300 p-2">
            <div class="w-12 h-8 bg-brand-500 rounded" />
            <div class="w-12 h-12 bg-brand-500 rounded" />
            <div class="w-12 h-16 bg-brand-500 rounded" />
          </Stack>
        </div>
        <div>
          <h3 class="text-sm font-medium text-text-muted mb-2">Align: center</h3>
          <Stack direction="horizontal" gap="md" align="center" class="h-24 border border-dashed border-gray-300 p-2">
            <div class="w-12 h-8 bg-brand-500 rounded" />
            <div class="w-12 h-12 bg-brand-500 rounded" />
            <div class="w-12 h-16 bg-brand-500 rounded" />
          </Stack>
        </div>
        <div>
          <h3 class="text-sm font-medium text-text-muted mb-2">Align: end</h3>
          <Stack direction="horizontal" gap="md" align="end" class="h-24 border border-dashed border-gray-300 p-2">
            <div class="w-12 h-8 bg-brand-500 rounded" />
            <div class="w-12 h-12 bg-brand-500 rounded" />
            <div class="w-12 h-16 bg-brand-500 rounded" />
          </Stack>
        </div>
      </div>
    `,
  }),
};

export const Justify: Story = {
  render: () => ({
    components: { Stack },
    template: `
      <div class="space-y-6">
        <div>
          <h3 class="text-sm font-medium text-text-muted mb-2">Justify: between</h3>
          <Stack direction="horizontal" gap="md" justify="between" class="border border-dashed border-gray-300 p-2">
            <div class="w-12 h-12 bg-brand-500 rounded" />
            <div class="w-12 h-12 bg-brand-500 rounded" />
            <div class="w-12 h-12 bg-brand-500 rounded" />
          </Stack>
        </div>
        <div>
          <h3 class="text-sm font-medium text-text-muted mb-2">Justify: around</h3>
          <Stack direction="horizontal" gap="md" justify="around" class="border border-dashed border-gray-300 p-2">
            <div class="w-12 h-12 bg-brand-500 rounded" />
            <div class="w-12 h-12 bg-brand-500 rounded" />
            <div class="w-12 h-12 bg-brand-500 rounded" />
          </Stack>
        </div>
        <div>
          <h3 class="text-sm font-medium text-text-muted mb-2">Justify: evenly</h3>
          <Stack direction="horizontal" gap="md" justify="evenly" class="border border-dashed border-gray-300 p-2">
            <div class="w-12 h-12 bg-brand-500 rounded" />
            <div class="w-12 h-12 bg-brand-500 rounded" />
            <div class="w-12 h-12 bg-brand-500 rounded" />
          </Stack>
        </div>
      </div>
    `,
  }),
};

export const Wrap: Story = {
  render: () => ({
    components: { Stack },
    template: `
      <Stack direction="horizontal" gap="md" wrap class="max-w-xs border border-dashed border-gray-300 p-2">
        <div class="w-16 h-16 bg-brand-500 rounded" />
        <div class="w-16 h-16 bg-brand-500 rounded" />
        <div class="w-16 h-16 bg-brand-500 rounded" />
        <div class="w-16 h-16 bg-brand-500 rounded" />
        <div class="w-16 h-16 bg-brand-500 rounded" />
      </Stack>
    `,
  }),
};

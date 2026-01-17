import type { Meta, StoryObj } from '@storybook/vue3';
import { Divider } from './index';

const meta: Meta<typeof Divider> = {
  title: 'Components/Layout/Divider',
  component: Divider,
  tags: ['autodocs'],
  argTypes: {
    orientation: {
      control: 'select',
      options: ['horizontal', 'vertical'],
    },
    variant: {
      control: 'select',
      options: ['solid', 'dashed', 'dotted'],
    },
    color: {
      control: 'select',
      options: ['default', 'muted', 'strong'],
    },
    spacing: {
      control: 'select',
      options: ['none', 'sm', 'md', 'lg'],
    },
  },
};

export default meta;
type Story = StoryObj<typeof Divider>;

export const Default: Story = {
  render: () => ({
    components: { Divider },
    template: `
      <div>
        <p class="text-text-body">Content above</p>
        <Divider />
        <p class="text-text-body">Content below</p>
      </div>
    `,
  }),
};

export const Variants: Story = {
  render: () => ({
    components: { Divider },
    template: `
      <div class="space-y-8">
        <div>
          <h3 class="text-sm font-medium text-text-muted mb-2">Solid</h3>
          <Divider variant="solid" />
        </div>
        <div>
          <h3 class="text-sm font-medium text-text-muted mb-2">Dashed</h3>
          <Divider variant="dashed" />
        </div>
        <div>
          <h3 class="text-sm font-medium text-text-muted mb-2">Dotted</h3>
          <Divider variant="dotted" />
        </div>
      </div>
    `,
  }),
};

export const Colors: Story = {
  render: () => ({
    components: { Divider },
    template: `
      <div class="space-y-8">
        <div>
          <h3 class="text-sm font-medium text-text-muted mb-2">Default</h3>
          <Divider color="default" />
        </div>
        <div>
          <h3 class="text-sm font-medium text-text-muted mb-2">Muted</h3>
          <Divider color="muted" />
        </div>
        <div>
          <h3 class="text-sm font-medium text-text-muted mb-2">Strong</h3>
          <Divider color="strong" />
        </div>
      </div>
    `,
  }),
};

export const WithLabel: Story = {
  render: () => ({
    components: { Divider },
    template: `
      <div>
        <p class="text-text-body">Content above</p>
        <Divider label="OR" />
        <p class="text-text-body">Content below</p>
      </div>
    `,
  }),
};

export const Vertical: Story = {
  render: () => ({
    components: { Divider },
    template: `
      <div class="flex items-center gap-0 h-16">
        <span class="text-text-body px-4">Left</span>
        <Divider orientation="vertical" spacing="none" />
        <span class="text-text-body px-4">Center</span>
        <Divider orientation="vertical" spacing="none" />
        <span class="text-text-body px-4">Right</span>
      </div>
    `,
  }),
};

export const Spacing: Story = {
  render: () => ({
    components: { Divider },
    template: `
      <div class="space-y-8">
        <div class="border border-dashed border-gray-300 p-2">
          <p class="text-sm text-text-muted">spacing: none</p>
          <Divider spacing="none" />
          <p class="text-sm text-text-muted">After divider</p>
        </div>
        <div class="border border-dashed border-gray-300 p-2">
          <p class="text-sm text-text-muted">spacing: sm</p>
          <Divider spacing="sm" />
          <p class="text-sm text-text-muted">After divider</p>
        </div>
        <div class="border border-dashed border-gray-300 p-2">
          <p class="text-sm text-text-muted">spacing: md</p>
          <Divider spacing="md" />
          <p class="text-sm text-text-muted">After divider</p>
        </div>
        <div class="border border-dashed border-gray-300 p-2">
          <p class="text-sm text-text-muted">spacing: lg</p>
          <Divider spacing="lg" />
          <p class="text-sm text-text-muted">After divider</p>
        </div>
      </div>
    `,
  }),
};
